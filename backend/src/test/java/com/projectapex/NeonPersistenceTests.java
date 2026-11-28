package com.projectapex;

import com.projectapex.config.NeonDataSourceConfig;
import com.projectapex.model.*;
import com.projectapex.service.FirebaseService;
import com.projectapex.service.NeonPersistenceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class NeonPersistenceTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private NeonPersistenceService neonPersistenceService;

    @Autowired
    private FirebaseService firebaseService;

    @Test
    @DisplayName("NeonDataSourceConfig parses Neon postgresql://, postgres://, jdbc:postgresql:// and falls back to H2")
    void testNeonUrlParsingAndFallback() {
        // 1. Standard Neon postgresql:// URL with URL-encoded credentials & sslmode=require
        NeonDataSourceConfig.ResolvedDataSourceSpec neonSpec = NeonDataSourceConfig.resolveSpec(
                "postgresql://neondb_owner:npg_Abc%40123+xyz@ep-cool-pooler.ap-southeast-1.aws.neon.tech/neondb?sslmode=require",
                null,
                null
        );
        assertTrue(neonSpec.postgreSql());
        assertEquals(NeonDataSourceConfig.ENGINE_NEON_POSTGRES, neonSpec.databaseEngine());
        assertEquals("org.postgresql.Driver", neonSpec.driverClassName());
        assertEquals("jdbc:postgresql://ep-cool-pooler.ap-southeast-1.aws.neon.tech:5432/neondb?sslmode=require", neonSpec.jdbcUrl());
        assertEquals("neondb_owner", neonSpec.username());
        assertEquals("npg_Abc@123+xyz", neonSpec.password());

        // 2. postgres:// URL without sslmode -> appends ?sslmode=require and preserves custom port
        NeonDataSourceConfig.ResolvedDataSourceSpec postgresSchemeSpec = NeonDataSourceConfig.resolveSpec(
                "postgres://apexuser:secret123@db.neon.tech:5433/apexdb",
                null,
                null
        );
        assertTrue(postgresSchemeSpec.postgreSql());
        assertEquals("jdbc:postgresql://db.neon.tech:5433/apexdb?sslmode=require", postgresSchemeSpec.jdbcUrl());
        assertEquals("apexuser", postgresSchemeSpec.username());
        assertEquals("secret123", postgresSchemeSpec.password());

        // 3. postgresql:// with existing query param but missing sslmode -> appends &sslmode=require
        NeonDataSourceConfig.ResolvedDataSourceSpec extraQuerySpec = NeonDataSourceConfig.resolveSpec(
                "postgresql://apexuser:pass@db.neon.tech/apexdb?connect_timeout=10",
                null,
                null
        );
        assertEquals("jdbc:postgresql://db.neon.tech:5432/apexdb?connect_timeout=10&sslmode=require", extraQuerySpec.jdbcUrl());

        // 4. Direct jdbc:postgresql:// URL with PGUSER / PGPASSWORD
        NeonDataSourceConfig.ResolvedDataSourceSpec jdbcSpec = NeonDataSourceConfig.resolveSpec(
                "jdbc:postgresql://ep-direct.aws.neon.tech:5432/neondb?sslmode=require",
                "pg_user",
                "pg_pass"
        );
        assertTrue(jdbcSpec.postgreSql());
        assertEquals("jdbc:postgresql://ep-direct.aws.neon.tech:5432/neondb?sslmode=require", jdbcSpec.jdbcUrl());
        assertEquals("pg_user", jdbcSpec.username());
        assertEquals("pg_pass", jdbcSpec.password());

        // 5. Missing URL -> embedded H2 PostgreSQL-Mode fallback
        NeonDataSourceConfig.ResolvedDataSourceSpec h2Spec = NeonDataSourceConfig.resolveSpec("", null, null);
        assertFalse(h2Spec.postgreSql());
        assertEquals(NeonDataSourceConfig.ENGINE_H2_POSTGRES_MODE, h2Spec.databaseEngine());
        assertEquals("org.h2.Driver", h2Spec.driverClassName());
        assertEquals(NeonDataSourceConfig.H2_POSTGRES_MODE_URL, h2Spec.jdbcUrl());
        assertEquals("sa", h2Spec.username());
        assertEquals("", h2Spec.password());
    }

    @Test
    @DisplayName("GET /api/v1/health executes SELECT 1 and reports live DB status and entity counts")
    void testHealthEndpointReportsDatabaseAndCounts() {
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "/api/v1/health",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(Boolean.TRUE, response.getBody().get("success"));

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        assertNotNull(data);
        assertEquals("UP", data.get("status"));
        assertEquals("project-apex-backend", data.get("service"));
        assertEquals("Render", data.get("hosting"));
        assertEquals(NeonDataSourceConfig.ENGINE_H2_POSTGRES_MODE, data.get("database"));
        assertEquals(Boolean.TRUE, data.get("dbConnected"));
        assertTrue(((Number) data.get("usersCount")).longValue() >= 3L);
        assertTrue(((Number) data.get("venuesCount")).longValue() >= 4L);
        assertNotNull(data.get("timestamp"));
    }

    @Test
    @DisplayName("All 13 relational tables persist, hydrate accurately, and enforce ACID unique slot locks")
    void testAllTablesPersistAndHydrateAndAcidSlotLocks() {
        // 1. User + OwnerProfile + Venue + Resource + ScheduleRule + DateOverride
        UserRecord owner = new UserRecord("usr_persist_owner_01", "persist.owner@apex.dev", "Persist Owner", "+91 98100 00001", "ROLE_OWNER");
        owner.setOwnerProfile(new OwnerProfile("Persist Sports LLP", "Thane West", "27ZZZZZ0000Z1Z5", "ZZZZZ0000Z"));
        owner.setPreferredSports(List.of("CRICKET_TURF", "BADMINTON"));
        firebaseService.getUsers().put(owner.getUid(), owner);

        VenueRecord venue = new VenueRecord("venue_persist_01", owner.getUid(), "Persist Arena", "Kolshet", "thane");
        venue.setSportTypes(List.of("CRICKET_TURF"));
        firebaseService.getVenues().put(venue.getVenueId(), venue);

        ResourceRecord resource = new ResourceRecord("res_persist_01", venue.getVenueId(), owner.getUid(), "Persist Turf 1", List.of("CRICKET_TURF"), 110000);
        firebaseService.getResources().put(resource.getResourceId(), resource);

        ScheduleRuleRecord rule = new ScheduleRuleRecord(resource.getResourceId(), venue.getVenueId(), owner.getUid());
        firebaseService.getScheduleRules().put(resource.getResourceId(), rule);

        DateOverrideRecord dateOverride = new DateOverrideRecord(resource.getResourceId(), venue.getVenueId(), "2026-12-20");
        dateOverride.setSlotPriceOverrides(Map.of("19:00", 150000L));
        firebaseService.getDateOverrides().put(dateOverride.getOverrideId(), dateOverride);

        // 2. Discount, Booking, Payment, Webhook, OTP, Facility, AvailabilityDoc, Transaction, AuditLog
        DiscountRecord discount = new DiscountRecord("disc_persist_01", owner.getUid(), "Persist 10% Off", "PROMO_CODE", "PERSIST10", 10.0, "PERCENTAGE");
        firebaseService.getDiscounts().put(discount.getDiscountId(), discount);

        BookingRecord booking = new BookingRecord();
        booking.setBookingId("bk_persist_01");
        booking.setBookingCode("ARENA-PRST1");
        booking.setUserId(owner.getUid());
        booking.setOwnerId(owner.getUid());
        booking.setVenueId(venue.getVenueId());
        booking.setResourceId(resource.getResourceId());
        booking.setBookingDate("2026-12-20");
        booking.setSlots(List.of(new BookingRecord.BookedSlotItem("19:00", "20:00", 150000L)));
        booking.setSubtotalPaise(150000L);
        booking.setFinalPayablePaise(150000L);
        booking.setStatus("CONFIRMED");
        firebaseService.getBookings().put(booking.getBookingId(), booking);

        PaymentRecord payment = new PaymentRecord("pay_persist_01", booking.getBookingId(), owner.getUid(), owner.getUid(), "order_p1", "pay_p1", 150000L);
        firebaseService.getPayments().put(payment.getPaymentId(), payment);

        WebhookEventRecord webhook = new WebhookEventRecord("evt_persist_01", "payment.captured", "PROCESSED");
        firebaseService.getWebhookEvents().put(webhook.getEventId(), webhook);

        OtpSession otp = new OtpSession();
        otp.setSessionId("otp_persist_01");
        otp.setEmail("persist.owner@apex.dev");
        otp.setPhone("+91 98100 00001");
        otp.setPurpose("LOGIN");
        otp.setLoginOtp("123456");
        otp.setCreatedAt(System.currentTimeMillis());
        otp.setExpiresAt(System.currentTimeMillis() + 300_000L);
        firebaseService.getOtpVerifications().put(otp.getSessionId(), otp);

        FacilityRecord fac = new FacilityRecord();
        fac.setCourtId("court_persist_01");
        fac.setVenueId(venue.getVenueId());
        fac.setName("Legacy Persist Pitch");
        fac.setSport("cricket");
        fac.setBaseHourlyRate(1200);
        fac.setPeakHourlyRate(1600);
        firebaseService.getCourts().put(fac.getCourtId(), fac);

        AvailabilityDoc avail = new AvailabilityDoc(fac.getCourtId(), venue.getVenueId(), "2026-12-21",
                Map.of("18:00", new SlotInfo("18:00", 1200, false, 0, "Standard")));
        firebaseService.getAvailability().put(avail.getDocId(), avail);

        TransactionRecord tx = new TransactionRecord("tx_persist_01", booking.getBookingId(), owner.getUid(), "PAYMENT", 1500, "Razorpay", "pay_p1");
        firebaseService.getTransactions().put(tx.getTxId(), tx);

        AuditLog audit = new AuditLog("audit_persist_01", "PERSISTENCE_TEST", owner.getUid(), "booking", booking.getBookingId(), Map.of("env", "test"));
        firebaseService.getAuditLogs().add(audit);

        // Verify SQL hydration reads everything back accurately
        assertTrue(neonPersistenceService.findAllUsers().stream().anyMatch(u -> "usr_persist_owner_01".equals(u.getUid()) && u.getOwnerProfile() != null));
        assertTrue(neonPersistenceService.findAllVenues().stream().anyMatch(v -> "venue_persist_01".equals(v.getVenueId())));
        assertTrue(neonPersistenceService.findAllResources().stream().anyMatch(r -> "res_persist_01".equals(r.getResourceId())));
        assertTrue(neonPersistenceService.findAllScheduleRules().stream().anyMatch(sr -> "res_persist_01".equals(sr.getResourceId())));
        assertTrue(neonPersistenceService.findAllDateOverrides().stream().anyMatch(d -> "res_persist_01_2026-12-20".equals(d.getOverrideId())));
        assertTrue(neonPersistenceService.findAllDiscounts().stream().anyMatch(d -> "disc_persist_01".equals(d.getDiscountId())));
        assertTrue(neonPersistenceService.findAllBookings().stream().anyMatch(b -> "bk_persist_01".equals(b.getBookingId()) && "ARENA-PRST1".equals(b.getBookingCode())));
        assertTrue(neonPersistenceService.findAllPayments().stream().anyMatch(p -> "pay_persist_01".equals(p.getPaymentId())));
        assertTrue(neonPersistenceService.findAllWebhookEvents().stream().anyMatch(e -> "evt_persist_01".equals(e.getEventId())));
        assertTrue(neonPersistenceService.findAllOtpSessions().stream().anyMatch(s -> "otp_persist_01".equals(s.getSessionId())));
        assertTrue(neonPersistenceService.findAllFacilities().stream().anyMatch(f -> "court_persist_01".equals(f.getCourtId()) && f.getBaseHourlyRate() == 1200));
        assertTrue(neonPersistenceService.findAllAvailabilityDocs().stream().anyMatch(a -> "court_persist_01_2026-12-21".equals(a.getDocId())));
        assertTrue(neonPersistenceService.findAllTransactions().stream().anyMatch(t -> "tx_persist_01".equals(t.getTxId()) && t.getAmount() == 1500));
        assertTrue(neonPersistenceService.findAllAuditLogs().stream().anyMatch(a -> "audit_persist_01".equals(a.getLogId())));

        // 3. Verify ACID uq_apex_slot_lock enforcement across direct DB calls
        String lockDate = "2026-12-25";
        SlotLockRecord lockUserA = new SlotLockRecord(resource.getResourceId(), venue.getVenueId(), lockDate, "20:00", "21:00", "bk_lock_a", "user_A", 600_000L);
        neonPersistenceService.saveSlotLock(lockUserA);

        SlotLockRecord lockUserB = new SlotLockRecord(resource.getResourceId(), venue.getVenueId(), lockDate, "20:00", "21:00", "bk_lock_b", "user_B", 600_000L);
        assertThrows(IllegalStateException.class, () -> neonPersistenceService.saveSlotLock(lockUserB));

        // Release User A's lock -> User B can now acquire
        neonPersistenceService.deleteSlotLock(lockUserA.getLockId(), lockUserA);
        assertDoesNotThrow(() -> neonPersistenceService.saveSlotLock(lockUserB));

        // Expired lock replacement -> User C can acquire after User B's lock expires
        lockUserB.setHoldExpiresAt(System.currentTimeMillis() - 10_000L);
        neonPersistenceService.saveSlotLock(lockUserB);

        SlotLockRecord lockUserC = new SlotLockRecord(resource.getResourceId(), venue.getVenueId(), lockDate, "20:00", "21:00", "bk_lock_c", "user_C", 600_000L);
        assertDoesNotThrow(() -> neonPersistenceService.saveSlotLock(lockUserC));

        // Clean up test lock row
        neonPersistenceService.deleteSlotLock(lockUserC.getLockId(), lockUserC);
    }
}

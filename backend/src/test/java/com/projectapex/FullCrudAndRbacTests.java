package com.projectapex;

import com.projectapex.dto.request.*;
import com.projectapex.service.NeonPersistenceService;
import com.projectapex.service.RazorpayService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FullCrudAndRbacTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private NeonPersistenceService neonPersistenceService;

    @Autowired
    private RazorpayService razorpayService;

    private HttpHeaders bearerHeaders(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(token);
        return h;
    }

    @Test
    @DisplayName("RBAC & Full CRUD: Owner/Player registration, Venue+Facility CRUD, Booking Reschedule/Cancel/Delete & Cross-Tenant Isolation")
    @SuppressWarnings("unchecked")
    void testEndToEndCrudAndRbacIsolation() {
        String suffix = String.valueOf(System.currentTimeMillis() % 100000);

        // 1. Register Owner A with role="venue_owner" (verifies JwtAuthenticationFilter maps ROLE_OWNER properly)
        RegisterRequest ownerAReq = new RegisterRequest();
        ownerAReq.setDisplayName("QA Owner Alpha");
        ownerAReq.setEmail("qa.owner.a." + suffix + "@apex.dev");
        ownerAReq.setPhone("+91 98210 " + suffix);
        ownerAReq.setPassword("OwnerPass@123");
        ownerAReq.setRole("venue_owner");
        ownerAReq.setCity("Thane");
        ownerAReq.setFacilityName("QA Alpha Turf " + suffix);
        ownerAReq.setArea("Wagle Estate");
        ownerAReq.setSports(List.of("cricket", "pickleball"));
        ownerAReq.setStartingPrice(1100);

        ResponseEntity<Map<String, Object>> regOwnerAResp = restTemplate.exchange(
                "/api/v1/auth/register", HttpMethod.POST, new HttpEntity<>(ownerAReq), new ParameterizedTypeReference<>() {});
        assertEquals(HttpStatus.OK, regOwnerAResp.getStatusCode());
        Map<String, Object> ownerAData = (Map<String, Object>) regOwnerAResp.getBody().get("data");
        String ownerAToken = (String) ownerAData.get("token");
        Map<String, Object> ownerAUser = (Map<String, Object>) ownerAData.get("user");
        assertNotNull(ownerAUser.get("venueId"));
        assertNotEquals("venue_thane_01", ownerAUser.get("venueId"));
        assertFalse(ownerAUser.containsKey("passwordHash"), "Password hash must be @JsonIgnore");
        String ownerAVenueId = (String) ownerAUser.get("venueId");

        // Duplicate registration with same email/phone must fail with 400
        ResponseEntity<Map<String, Object>> dupResp = restTemplate.exchange(
                "/api/v1/auth/register", HttpMethod.POST, new HttpEntity<>(ownerAReq), new ParameterizedTypeReference<>() {});
        assertEquals(HttpStatus.BAD_REQUEST, dupResp.getStatusCode());

        // 2. Owner A creates, updates, and deletes a Venue
        CreateVenueRequest newVenueReq = new CreateVenueRequest();
        newVenueReq.setName("QA Temp Arena " + suffix);
        newVenueReq.setCity("thane");
        newVenueReq.setLocality("Majiwada");
        newVenueReq.setMinSlotPricePaise(90000);
        newVenueReq.setSportTypes(List.of("BADMINTON"));

        ResponseEntity<Map<String, Object>> createVenueResp = restTemplate.exchange(
                "/api/v1/owner/venues", HttpMethod.POST,
                new HttpEntity<>(newVenueReq, bearerHeaders(ownerAToken)),
                new ParameterizedTypeReference<>() {});
        assertEquals(HttpStatus.OK, createVenueResp.getStatusCode());
        Map<String, Object> createdVenue = (Map<String, Object>) createVenueResp.getBody().get("data");
        String tempVenueId = (String) createdVenue.get("venueId");

        // 3. Owner A adds a new Court/Facility on their venue, updates it
        CreateFacilityRequest facReq = new CreateFacilityRequest();
        facReq.setVenueId(ownerAVenueId);
        facReq.setName("QA Pickleball Court " + suffix);
        facReq.setSport("pickleball");
        facReq.setCapacity(4);
        facReq.setBaseHourlyRate(800);
        facReq.setPeakHourlyRate(1000);

        ResponseEntity<Map<String, Object>> addFacResp = restTemplate.exchange(
                "/api/v1/facilities", HttpMethod.POST,
                new HttpEntity<>(facReq, bearerHeaders(ownerAToken)),
                new ParameterizedTypeReference<>() {});
        assertEquals(HttpStatus.CREATED, addFacResp.getStatusCode());
        Map<String, Object> facData = (Map<String, Object>) addFacResp.getBody().get("data");
        String courtId = (String) facData.get("courtId");

        facReq.setBaseHourlyRate(850);
        facReq.setName("QA Pickleball Pro " + suffix);
        ResponseEntity<Map<String, Object>> updFacResp = restTemplate.exchange(
                "/api/v1/facilities/" + courtId, HttpMethod.PUT,
                new HttpEntity<>(facReq, bearerHeaders(ownerAToken)),
                new ParameterizedTypeReference<>() {});
        assertEquals(HttpStatus.OK, updFacResp.getStatusCode());
        assertEquals(850, ((Number) ((Map<String, Object>) updFacResp.getBody().get("data")).get("baseHourlyRate")).intValue());

        // 4. Register Player P1 and Player P2
        RegisterRequest p1Req = new RegisterRequest();
        p1Req.setDisplayName("QA Player One");
        p1Req.setEmail("qa.p1." + suffix + "@apex.dev");
        p1Req.setPhone("+91 98310 " + suffix);
        p1Req.setPassword("PlayerPass@123");
        p1Req.setRole("player");
        p1Req.setCity("Mumbai");

        ResponseEntity<Map<String, Object>> p1Resp = restTemplate.exchange(
                "/api/v1/auth/register", HttpMethod.POST, new HttpEntity<>(p1Req), new ParameterizedTypeReference<>() {});
        assertEquals(HttpStatus.OK, p1Resp.getStatusCode());
        Map<String, Object> p1Data = (Map<String, Object>) p1Resp.getBody().get("data");
        String p1Token = (String) p1Data.get("token");

        RegisterRequest p2Req = new RegisterRequest();
        p2Req.setDisplayName("QA Player Two");
        p2Req.setEmail("qa.p2." + suffix + "@apex.dev");
        p2Req.setPhone("+91 98320 " + suffix);
        p2Req.setPassword("PlayerPass@123");
        p2Req.setRole("player");
        p2Req.setCity("Thane");

        ResponseEntity<Map<String, Object>> p2Resp = restTemplate.exchange(
                "/api/v1/auth/register", HttpMethod.POST, new HttpEntity<>(p2Req), new ParameterizedTypeReference<>() {});
        String p2Token = (String) ((Map<String, Object>) p2Resp.getBody().get("data")).get("token");

        // Player P1 MUST get 403 on owner/gate/facility endpoints
        ResponseEntity<String> playerToOwnerForbidden = restTemplate.exchange(
                "/api/v1/owner/venues", HttpMethod.GET, new HttpEntity<>(bearerHeaders(p1Token)), String.class);
        assertEquals(HttpStatus.FORBIDDEN, playerToOwnerForbidden.getStatusCode());

        ResponseEntity<String> playerToFacForbidden = restTemplate.exchange(
                "/api/v1/facilities", HttpMethod.POST, new HttpEntity<>(facReq, bearerHeaders(p1Token)), String.class);
        assertEquals(HttpStatus.FORBIDDEN, playerToFacForbidden.getStatusCode());

        // Player P1 updates their profile via PUT /api/v1/users/me
        RegisterProfileRequest profUpd = new RegisterProfileRequest();
        profUpd.setFullName("QA Player One Updated");
        profUpd.setCity("Navi Mumbai");
        ResponseEntity<Map<String, Object>> updProfResp = restTemplate.exchange(
                "/api/v1/users/me", HttpMethod.PUT, new HttpEntity<>(profUpd, bearerHeaders(p1Token)), new ParameterizedTypeReference<>() {});
        assertEquals(HttpStatus.OK, updProfResp.getStatusCode());
        assertEquals("Navi Mumbai", ((Map<String, Object>) updProfResp.getBody().get("data")).get("city"));

        // 5. Player P1 initiates & verifies booking via /bookings/initiate + /verify-payment
        String matchDate = LocalDate.now().plusDays(3).toString();
        InitiateBookingRequest initReq = new InitiateBookingRequest();
        initReq.setVenueId(ownerAVenueId);
        initReq.setResourceId(courtId);
        initReq.setBookingDate(matchDate);
        initReq.setSlotStartTimes(List.of("18:00"));
        initReq.setSportType("PICKLEBALL");

        ResponseEntity<Map<String, Object>> initResp = restTemplate.exchange(
                "/api/v1/bookings/initiate", HttpMethod.POST, new HttpEntity<>(initReq, bearerHeaders(p1Token)), new ParameterizedTypeReference<>() {});
        assertEquals(HttpStatus.OK, initResp.getStatusCode());
        Map<String, Object> initData = (Map<String, Object>) initResp.getBody().get("data");
        String bookingId = (String) initData.get("bookingId");
        String orderId = (String) initData.get("razorpayOrderId");

        // Player P2 trying to book same slot must get 409 CONFLICT
        ResponseEntity<Map<String, Object>> conflictResp = restTemplate.exchange(
                "/api/v1/bookings/initiate", HttpMethod.POST, new HttpEntity<>(initReq, bearerHeaders(p2Token)), new ParameterizedTypeReference<>() {});
        assertEquals(HttpStatus.CONFLICT, conflictResp.getStatusCode());

        // Player P1 confirms payment
        String payId = "pay_qa_" + suffix;
        VerifyPaymentRequest verReq = new VerifyPaymentRequest(orderId, payId, razorpayService.computeSignature(orderId, payId));
        ResponseEntity<Map<String, Object>> verResp = restTemplate.exchange(
                "/api/v1/bookings/" + bookingId + "/verify-payment", HttpMethod.POST,
                new HttpEntity<>(verReq, bearerHeaders(p1Token)), new ParameterizedTypeReference<>() {});
        assertEquals(HttpStatus.OK, verResp.getStatusCode());
        Map<String, Object> confirmedBooking = (Map<String, Object>) verResp.getBody().get("data");
        assertEquals("CONFIRMED", confirmedBooking.get("status"));

        // Player P2 CANNOT view/update/delete Player P1's booking (403 Forbidden)
        ResponseEntity<String> idorGet = restTemplate.exchange(
                "/api/v1/bookings/" + bookingId, HttpMethod.GET, new HttpEntity<>(bearerHeaders(p2Token)), String.class);
        assertEquals(HttpStatus.FORBIDDEN, idorGet.getStatusCode());

        // 6. Player P1 reschedules booking from 18:00 -> 19:00 via PUT /api/v1/bookings/{id}
        UpdateBookingRequest reschedReq = new UpdateBookingRequest();
        reschedReq.setBookingDate(matchDate);
        reschedReq.setSlotTime("19:00");
        reschedReq.setNotes("Rescheduled by QA");
        ResponseEntity<Map<String, Object>> reschedResp = restTemplate.exchange(
                "/api/v1/bookings/" + bookingId, HttpMethod.PUT,
                new HttpEntity<>(reschedReq, bearerHeaders(p1Token)), new ParameterizedTypeReference<>() {});
        assertEquals(HttpStatus.OK, reschedResp.getStatusCode());
        assertEquals("Rescheduled by QA", ((Map<String, Object>) reschedResp.getBody().get("data")).get("notes"));

        // Original 18:00 lock is freed so Player P2 can now acquire 18:00!
        ResponseEntity<Map<String, Object>> p2BookedOldSlot = restTemplate.exchange(
                "/api/v1/bookings/initiate", HttpMethod.POST, new HttpEntity<>(initReq, bearerHeaders(p2Token)), new ParameterizedTypeReference<>() {});
        assertEquals(HttpStatus.OK, p2BookedOldSlot.getStatusCode());
        String p2BookingId = (String) ((Map<String, Object>) p2BookedOldSlot.getBody().get("data")).get("bookingId");

        // 7. Player P1 cancels (>24h Tier 1 -> finalPayablePaise - 5000L fee deducted)
        ResponseEntity<Map<String, Object>> cancelResp = restTemplate.exchange(
                "/api/v1/bookings/" + bookingId + "/cancel", HttpMethod.POST,
                new HttpEntity<>(new CancelBookingRequest("QA Cancel Tier 1"), bearerHeaders(p1Token)),
                new ParameterizedTypeReference<>() {});
        assertEquals(HttpStatus.OK, cancelResp.getStatusCode());
        Map<String, Object> cancelledData = (Map<String, Object>) cancelResp.getBody().get("data");
        long paidPaise = ((Number) cancelledData.get("finalPayablePaise")).longValue();
        long refundPaise = ((Number) cancelledData.get("refundAmountPaise")).longValue();
        assertEquals(Math.max(0L, paidPaise - 5000L), refundPaise, "Tier 1 cancellation must deduct ₹50 (5000 paise) fee");

        // 8. Delete bookings + delete facility + delete venue + delete test users so DB stays clean
        assertEquals(HttpStatus.OK, restTemplate.exchange(
                "/api/v1/bookings/" + bookingId, HttpMethod.DELETE, new HttpEntity<>(bearerHeaders(p1Token)), String.class).getStatusCode());
        assertEquals(HttpStatus.OK, restTemplate.exchange(
                "/api/v1/bookings/" + p2BookingId, HttpMethod.DELETE, new HttpEntity<>(bearerHeaders(p2Token)), String.class).getStatusCode());
        assertEquals(HttpStatus.OK, restTemplate.exchange(
                "/api/v1/facilities/" + courtId, HttpMethod.DELETE, new HttpEntity<>(bearerHeaders(ownerAToken)), String.class).getStatusCode());
        assertEquals(HttpStatus.OK, restTemplate.exchange(
                "/api/v1/owner/venues/" + tempVenueId, HttpMethod.DELETE, new HttpEntity<>(bearerHeaders(ownerAToken)), String.class).getStatusCode());
        assertEquals(HttpStatus.OK, restTemplate.exchange(
                "/api/v1/owner/venues/" + ownerAVenueId, HttpMethod.DELETE, new HttpEntity<>(bearerHeaders(ownerAToken)), String.class).getStatusCode());
        assertEquals(HttpStatus.OK, restTemplate.exchange(
                "/api/v1/users/me", HttpMethod.DELETE, new HttpEntity<>(bearerHeaders(p1Token)), String.class).getStatusCode());
        assertEquals(HttpStatus.OK, restTemplate.exchange(
                "/api/v1/users/me", HttpMethod.DELETE, new HttpEntity<>(bearerHeaders(p2Token)), String.class).getStatusCode());
        assertEquals(HttpStatus.OK, restTemplate.exchange(
                "/api/v1/users/me", HttpMethod.DELETE, new HttpEntity<>(bearerHeaders(ownerAToken)), String.class).getStatusCode());

        assertTrue(neonPersistenceService.findAllBookings().stream().noneMatch(b -> bookingId.equals(b.getBookingId())));
    }
}

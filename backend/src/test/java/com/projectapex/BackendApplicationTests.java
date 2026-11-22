package com.projectapex;

import com.projectapex.dto.request.*;
import com.projectapex.dto.response.AuthResponse;
import com.projectapex.dto.response.RazorpayOrderResponse;
import com.projectapex.model.*;
import com.projectapex.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class BackendApplicationTests {

    @Autowired
    private FirebaseService firebaseService;

    @Autowired
    private OtpService otpService;

    @Autowired
    private AuthService authService;

    @Autowired
    private VenueService venueService;

    @Autowired
    private ConcurrencyService concurrencyService;

    @Autowired
    private RazorpayService razorpayService;

    @Autowired
    private BookingService bookingService;

    @BeforeEach
    void setUp() {
        firebaseService.init();
    }

    @Test
    @DisplayName("Workflow 1: Player Registration with Dual Mobile (+91) & Email OTP")
    void testPlayerRegistrationWithDualOtp() {
        String phone = "+91 98111 22334";
        String email = "virat.patil@sports.in";
        String name = "Virat Patil";

        // 1. Dispatch Dual OTPs
        OtpSession otpSession = otpService.sendRegistrationOtp(phone, email, name);
        assertNotNull(otpSession.getSessionId());
        assertEquals(6, otpSession.getPhoneOtp().length());
        assertEquals(6, otpSession.getEmailOtp().length());

        // 2. Reject wrong OTPs
        assertThrows(IllegalArgumentException.class, () ->
                otpService.verifyRegistrationOtp(otpSession.getSessionId(), "000000", otpSession.getEmailOtp()));

        // 3. Complete Atomic Registration with correct OTPs
        VerifyRegistrationOtpRequest req = new VerifyRegistrationOtpRequest();
        req.setSessionId(otpSession.getSessionId());
        req.setPhoneOtp(otpSession.getPhoneOtp());
        req.setEmailOtp(otpSession.getEmailOtp());
        req.setDisplayName(name);
        req.setPhone(phone);
        req.setEmail(email);
        req.setRole("player");
        req.setCity("Thane");

        AuthResponse authRes = authService.verifyAndRegister(req);
        assertNotNull(authRes.getToken());
        assertNotNull(authRes.getUser());
        assertEquals(100, authRes.getUser().getLoyaltyPoints());
        assertTrue(authRes.getUser().getIsPhoneVerified());
        assertTrue(authRes.getUser().getIsEmailVerified());
        assertEquals("player", authRes.getUser().getRole());
    }

    @Test
    @DisplayName("Workflow 2 & 3: Facility Owner Registration & Adding New Facility with Capacity & Specs")
    void testFacilityOwnerRegistrationAndAddFacility() {
        String phone = "+91 98222 33445";
        String email = "sachin@stadiumsports.com";
        String name = "Sachin Rane";

        OtpSession otpSession = otpService.sendRegistrationOtp(phone, email, name);
        VerifyRegistrationOtpRequest req = new VerifyRegistrationOtpRequest();
        req.setSessionId(otpSession.getSessionId());
        req.setPhoneOtp(otpSession.getPhoneOtp());
        req.setEmailOtp(otpSession.getEmailOtp());
        req.setDisplayName(name);
        req.setPhone(phone);
        req.setEmail(email);
        req.setRole("venue_owner");
        req.setCity("Thane");
        req.setFacilityName("Rane Mega Turf Complex");
        req.setArea("Majiwada");
        req.setSports(List.of("cricket", "football"));
        req.setStartingPrice(1300);

        AuthResponse authRes = authService.verifyAndRegister(req);
        assertEquals("venue_owner", authRes.getUser().getRole());
        assertNotNull(authRes.getUser().getVenueId());

        // Owner adds a new customized facility pitch
        CreateFacilityRequest facilityReq = new CreateFacilityRequest();
        facilityReq.setVenueId(authRes.getUser().getVenueId());
        facilityReq.setName("Championship Box Cricket Arena 2");
        facilityReq.setSport("cricket");
        facilityReq.setFacilityType("Box Cricket");
        facilityReq.setCapacity(18); // Max 18 players
        facilityReq.setSurface("15mm High-Density Monofilament Turf");
        facilityReq.setDimensions("120ft x 70ft");
        facilityReq.setBaseHourlyRate(1350);
        facilityReq.setPeakHourlyRate(1800);
        facilityReq.setOddHourDiscountPct(35);
        facilityReq.setHasFloodlights(true);
        facilityReq.setAmenities(List.of("Floodlights", "Padded Boundaries", "Player Dugouts"));

        FacilityRecord createdFacility = venueService.createFacility(authRes.getUser().getUid(), facilityReq);
        assertNotNull(createdFacility.getCourtId());
        assertEquals(18, createdFacility.getCapacity());
        assertEquals("Box Cricket", createdFacility.getFacilityType());
        assertEquals(1350, createdFacility.getBaseHourlyRate());
    }

    @Test
    @DisplayName("Workflow 4: 5-Minute Atomic Concurrency Slot Reservation Lease")
    void testConcurrencyLockingAndRaceConditionPrevention() {
        String courtId = "court_thane_01_crk";
        String date = "2026-11-28";
        String slot = "19:00";
        String playerA = "usr_player_a";
        String playerB = "usr_player_b";

        // Player A acquires 5-min lease
        ConcurrencyLock lockA = concurrencyService.acquireLock(courtId, date, slot, playerA);
        assertEquals("HELD", lockA.getStatus());
        assertEquals(playerA, lockA.getUserId());

        // Player B tries to acquire same slot -> Double booking conflict
        assertThrows(IllegalStateException.class, () ->
                concurrencyService.acquireLock(courtId, date, slot, playerB));

        // Player A releases lock
        concurrencyService.releaseLock(courtId, date, slot, playerA);

        // Player B now acquires the slot
        ConcurrencyLock lockB = concurrencyService.acquireLock(courtId, date, slot, playerB);
        assertEquals(playerB, lockB.getUserId());
    }

    @Test
    @DisplayName("Workflow 5: Razorpay Order Creation, HMAC SHA256 Signature Verification & Atomic Booking")
    void testRazorpayOrderAndAtomicBookingCommit() {
        String courtId = "court_thane_01_crk";
        String date = "2026-11-29";
        String slot = "18:00";
        String userId = "usr_rohan_deshmukh";

        // Acquire lock first
        concurrencyService.acquireLock(courtId, date, slot, userId);

        // 1. Create Razorpay Order
        String orderId = razorpayService.createOrder(1220, "rcpt_test_101");
        assertNotNull(orderId);
        assertTrue(orderId.startsWith("order_"));

        // 2. Compute authentic HMAC SHA256 signature
        String paymentId = "pay_test_987654";
        String signature = razorpayService.computeSignature(orderId, paymentId);
        assertTrue(razorpayService.verifySignature(orderId, paymentId, signature));

        // 3. Reject invalid signature
        assertFalse(razorpayService.verifySignature(orderId, paymentId, "invalid_tampered_signature"));

        // 4. Commit Booking
        BookingRecord booking = bookingService.commitBooking(
                userId,
                courtId,
                date,
                slot,
                orderId,
                paymentId,
                signature,
                "UPI"
        );

        assertNotNull(booking.getBookingId());
        assertEquals("CONFIRMED", booking.getStatus());
        assertTrue(booking.getQrPassToken().startsWith("APEX-PASS-"));

        // 5. Strict User Isolation: User has exactly 1 booking
        List<BookingRecord> myPasses = bookingService.getPlayerBookings(userId);
        assertEquals(1, myPasses.size());
        assertEquals(booking.getBookingId(), myPasses.get(0).getBookingId());

        // Another user has 0 bookings
        List<BookingRecord> otherPasses = bookingService.getPlayerBookings("usr_random_other_player");
        assertEquals(0, otherPasses.size());

        // 6. Gate QR Pass Scanner
        Map<String, Object> gateCheck = bookingService.verifyGatePass(booking.getQrPassToken(), "owner_01");
        assertTrue((Boolean) gateCheck.get("success"));
        assertFalse((Boolean) gateCheck.get("alreadyCheckedIn"));

        // Duplicate scan check
        Map<String, Object> duplicateCheck = bookingService.verifyGatePass(booking.getQrPassToken(), "owner_01");
        assertTrue((Boolean) duplicateCheck.get("alreadyCheckedIn"));
    }

    @Test
    @DisplayName("Workflow 6: 3-Tier Cancellation & Automated Refund Engine")
    void testThreeTierCancellation() {
        String courtId = "court_thane_01_fb";
        String date = "2026-11-30";
        String slot = "15:00";
        String userId = "usr_cancel_test_player";

        concurrencyService.acquireLock(courtId, date, slot, userId);
        String orderId = razorpayService.createOrder(1400, "rcpt_cancel_1");
        String paymentId = "pay_cancel_1";
        String signature = razorpayService.computeSignature(orderId, paymentId);

        BookingRecord booking = bookingService.commitBooking(userId, courtId, date, slot, orderId, paymentId, signature, "UPI");
        assertEquals("CONFIRMED", booking.getStatus());

        // Cancel booking
        BookingRecord cancelled = bookingService.cancelBooking(booking.getBookingId(), userId);
        assertEquals("CANCELLED", cancelled.getStatus());
        assertNotNull(cancelled.getRefundAmount());
        assertTrue(cancelled.getRefundAmount() > 0);

        // Cancelled QR Pass must be rejected at gate
        assertThrows(IllegalStateException.class, () ->
                bookingService.verifyGatePass(cancelled.getQrPassToken(), "owner_01"));
    }
}

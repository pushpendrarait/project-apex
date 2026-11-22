package com.projectapex;

import com.projectapex.dto.request.CancelBookingRequest;
import com.projectapex.dto.request.InitiateBookingRequest;
import com.projectapex.dto.request.RegisterOwnerRequest;
import com.projectapex.dto.request.RegisterProfileRequest;
import com.projectapex.dto.request.VerifyPaymentRequest;
import com.projectapex.dto.response.AuthResponse;
import com.projectapex.model.*;
import com.projectapex.service.*;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TechSpecWorkflowsTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private VenueService venueService;

    @Autowired
    private CalendarAvailabilityService availabilityService;

    @Autowired
    private PricingAndDiscountService pricingService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private RazorpayService razorpayService;

    @Autowired
    private WebhookService webhookService;

    @Autowired
    private FirebaseService firebaseService;

    @Test
    @DisplayName("TechSpec Phase 2: Player & Facility Owner Onboarding & Sport Filtering")
    void testPhase2OnboardingAndDiscovery() {
        // 1. Player Onboarding
        RegisterProfileRequest playerReq = new RegisterProfileRequest();
        playerReq.setFullName("Shubman Gill");
        playerReq.setPhone("+91 98765 43210");
        playerReq.setEmail("shubman.gill@sports.in");
        playerReq.setCity("Thane");
        playerReq.setPreferredSports(List.of("CRICKET_TURF", "BADMINTON"));
        playerReq.setPassword("Password@123");

        AuthResponse playerRes = authService.registerProfile(playerReq);
        assertNotNull(playerRes.getToken());
        assertEquals("ROLE_PLAYER", playerRes.getUser().getRole());
        assertEquals(100, playerRes.getUser().getLoyaltyPoints());

        // 2. Owner Onboarding
        RegisterOwnerRequest ownerReq = new RegisterOwnerRequest();
        ownerReq.setOwnerName("Ashwin Ravichandran");
        ownerReq.setBusinessName("Spin Kings Arena");
        ownerReq.setPhone("+91 99887 76655");
        ownerReq.setEmail("ashwin@spinkings.com");
        ownerReq.setCity("Mumbai");
        ownerReq.setBusinessAddress("BKC Sports Complex, Bandra East");
        ownerReq.setGstNumber("27ABCDE1234F1Z5");
        ownerReq.setPassword("OwnerSecret@123");

        AuthResponse ownerRes = authService.registerOwner(ownerReq);
        assertNotNull(ownerRes.getToken());
        assertEquals("ROLE_OWNER", ownerRes.getUser().getRole());
        assertNotNull(ownerRes.getUser().getOwnerProfile());
        assertEquals("APPROVED", ownerRes.getUser().getOwnerProfile().getVerificationStatus());

        // 3. Discovery Filter by Sport and City
        List<VenueRecord> thaneCricket = venueService.searchVenues("thane", null, "CRICKET_TURF", null, null, null, null, 0, 10);
        assertFalse(thaneCricket.isEmpty());
        assertTrue(thaneCricket.stream().allMatch(v -> "thane".equalsIgnoreCase(v.getCity())));
    }

    @Test
    @DisplayName("TechSpec Phase 3: Dynamic Slot Generation, Paise Pricing, Auto-Discounts & Atomic Slot Lock")
    void testPhase3DynamicSlotsAndLocking() {
        String resourceId = "res_thane_crk_01";
        String date = LocalDate.now().plusDays(2).toString();

        // 1. Zero empty-slot pre-generation: Dynamic generation from rules + overrides
        List<SlotAvailability> slots = availabilityService.getGeneratedSlots(resourceId, date);
        assertNotNull(slots);
        assertFalse(slots.isEmpty());
        assertTrue(slots.size() >= 14, "Standard operating day from 06:00 to 23:00 has at least 14 1-hr slots");

        // Verify paise price values
        assertTrue(slots.stream().allMatch(s -> s.getOriginalPricePaise() > 0));

        // 2. Pricing Quote & Auto-Discounts
        // Bulk slots auto discount (2+ slots)
        PricingAndDiscountService.PriceQuote quote = pricingService.calculateQuote(
                "venue_thane_01", resourceId, date, List.of("19:00", "20:00"), "TURF20", "user_test_01");

        assertNotNull(quote);
        assertEquals(2, quote.getItemizedSlots().size());
        assertTrue(quote.getDiscountAmountPaise() > 0, "Discount should be applied");
        assertTrue(quote.getFinalPayablePaise() >= 100L, "Razorpay minimum amount constraint (>= 100 paise)");

        // 3. Atomic Slot Hold (10-minute lease)
        InitiateBookingRequest initReq = new InitiateBookingRequest();
        initReq.setVenueId("venue_thane_01");
        initReq.setResourceId(resourceId);
        initReq.setBookingDate(date);
        initReq.setSlotStartTimes(List.of("19:00", "20:00"));
        initReq.setSportType("CRICKET_TURF");
        initReq.setPromoCode("TURF20");

        Map<String, Object> bookingHold = bookingService.initiateBooking(initReq, "user_player_A");
        assertNotNull(bookingHold.get("bookingId"));
        assertNotNull(bookingHold.get("razorpayOrderId"));
        long holdExpiresAt = (Long) bookingHold.get("holdExpiresAt");
        assertTrue(holdExpiresAt > System.currentTimeMillis());

        // 4. Double-Booking Prevention: Player B must be rejected with Conflict
        assertThrows(IllegalStateException.class, () ->
                bookingService.initiateBooking(initReq, "user_player_B"),
                "Concurrent booking on held slot must be rejected");
    }

    @Test
    @DisplayName("TechSpec Phase 4: Razorpay Payment Verification, Webhook with Raw String, & Refunds")
    void testPhase4PaymentWebhookAndRefund() {
        String resourceId = "res_thane_fb_01";
        String date = LocalDate.now().plusDays(3).toString();

        InitiateBookingRequest initReq = new InitiateBookingRequest();
        initReq.setVenueId("venue_thane_01");
        initReq.setResourceId(resourceId);
        initReq.setBookingDate(date);
        initReq.setSlotStartTimes(List.of("18:00"));
        initReq.setSportType("FOOTBALL_TURF");

        Map<String, Object> initiated = bookingService.initiateBooking(initReq, "user_payment_test");
        String bookingId = (String) initiated.get("bookingId");
        String orderId = (String) initiated.get("razorpayOrderId");
        String paymentId = "pay_test_" + System.currentTimeMillis();
        String signature = razorpayService.computeSignature(orderId, paymentId);

        // 1. Synchronous Signature Verification & Confirmation
        VerifyPaymentRequest verifyReq = new VerifyPaymentRequest(orderId, paymentId, signature);
        BookingRecord confirmed = bookingService.verifyPayment(bookingId, verifyReq);

        assertEquals("CONFIRMED", confirmed.getStatus());
        assertNotNull(confirmed.getBookingCode());
        assertTrue(confirmed.getBookingCode().startsWith("ARENA-"));

        // 2. Asynchronous Webhook Raw Body HMAC Signature Processing & Idempotency
        String rawWebhookPayload = "{"
                + "\"event\":\"payment.captured\","
                + "\"payload\":{"
                + "  \"payment\":{\"entity\":{\"id\":\"" + paymentId + "\",\"order_id\":\"" + orderId + "\",\"notes\":{\"bookingId\":\"" + bookingId + "\"}}}"
                + "}}";

        String webhookSig = razorpayService.computeWebhookSignature(rawWebhookPayload);
        String eventId = "evt_" + System.currentTimeMillis();

        boolean webhookResult = webhookService.processWebhook(rawWebhookPayload, webhookSig, eventId);
        assertTrue(webhookResult);

        // Duplicate event ID idempotency
        boolean duplicateResult = webhookService.processWebhook(rawWebhookPayload, webhookSig, eventId);
        assertTrue(duplicateResult, "Duplicate webhook event must be acknowledged safely");

        // 3. Player Cancellation & Refund Calculation
        CancelBookingRequest cancelReq = new CancelBookingRequest("Match rescheduled");
        BookingRecord cancelled = bookingService.cancelBookingByPlayer(bookingId, "user_payment_test", cancelReq.getReason());
        assertEquals("CANCELLED_BY_USER", cancelled.getStatus());
        assertNotNull(cancelled.getRefundAmountPaise());
        assertTrue(cancelled.getRefundAmountPaise() > 0);
    }
}

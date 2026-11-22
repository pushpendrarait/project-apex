package com.projectapex.controller;

import com.projectapex.dto.request.*;
import com.projectapex.dto.response.ApiResponse;
import com.projectapex.model.BookingRecord;
import com.projectapex.model.ConcurrencyLock;
import com.projectapex.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    /**
     * POST /api/v1/bookings/initiate - Atomically holds slots and creates Razorpay Order
     */
    @PostMapping("/bookings/initiate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> initiateBooking(
            @Valid @RequestBody InitiateBookingRequest req,
            Authentication auth
    ) {
        String userId = auth != null ? auth.getName() : "demo_player";
        Map<String, Object> res = bookingService.initiateBooking(req, userId);
        return ResponseEntity.ok(ApiResponse.ok("Booking initiated. 10-minute slot hold acquired.", res));
    }

    /**
     * POST /api/v1/bookings/{bookingId}/verify-payment - Verifies Razorpay signature & confirms booking
     */
    @PostMapping("/bookings/{bookingId}/verify-payment")
    public ResponseEntity<ApiResponse<BookingRecord>> verifyPayment(
            @PathVariable String bookingId,
            @Valid @RequestBody VerifyPaymentRequest req
    ) {
        BookingRecord confirmed = bookingService.verifyPayment(bookingId, req);
        return ResponseEntity.ok(ApiResponse.ok("Payment verified and booking confirmed", confirmed));
    }

    /**
     * GET /api/v1/bookings/my - Player's bookings
     */
    @GetMapping("/bookings/my")
    public ResponseEntity<ApiResponse<List<BookingRecord>>> getMyBookings(Authentication auth) {
        String userId = auth != null ? auth.getName() : "demo_player";
        List<BookingRecord> bookings = bookingService.getPlayerBookings(userId);
        return ResponseEntity.ok(ApiResponse.ok("Player bookings retrieved", bookings));
    }

    /**
     * GET /api/v1/bookings/{bookingId} - Booking details
     */
    @GetMapping("/bookings/{bookingId}")
    public ResponseEntity<ApiResponse<BookingRecord>> getBooking(@PathVariable String bookingId) {
        BookingRecord b = bookingService.getBooking(bookingId);
        return ResponseEntity.ok(ApiResponse.ok("Booking details", b));
    }

    /**
     * POST /api/v1/bookings/{bookingId}/cancel - Player cancellation with 3-tier policy
     */
    @PostMapping("/bookings/{bookingId}/cancel")
    public ResponseEntity<ApiResponse<BookingRecord>> cancelBooking(
            @PathVariable String bookingId,
            @RequestBody(required = false) CancelBookingRequest req,
            Authentication auth
    ) {
        String userId = auth != null ? auth.getName() : "demo_player";
        String reason = req != null && req.getReason() != null ? req.getReason() : "Player requested cancellation";
        BookingRecord cancelled = bookingService.cancelBookingByPlayer(bookingId, userId, reason);
        return ResponseEntity.ok(ApiResponse.ok("Booking cancelled and refund processed", cancelled));
    }

    // =========================================================================
    // Compatibility Endpoints for Test Suite (Runner Tests)
    // =========================================================================

    @PostMapping("/bookings/commit")
    public ResponseEntity<ApiResponse<BookingRecord>> commitBooking(@Valid @RequestBody CommitBookingRequest req) {
        BookingRecord b = bookingService.commitBooking(req);
        return ResponseEntity.ok(ApiResponse.ok("Booking committed", b));
    }

    @PostMapping("/bookings/walkin")
    public ResponseEntity<ApiResponse<BookingRecord>> walkInBooking(@Valid @RequestBody WalkInBookingRequest req) {
        BookingRecord b = bookingService.createWalkInBooking(req);
        return ResponseEntity.ok(ApiResponse.ok("Walk-in booking recorded", b));
    }
}

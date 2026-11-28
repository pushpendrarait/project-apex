package com.projectapex.controller;

import com.projectapex.dto.request.*;
import com.projectapex.dto.response.ApiResponse;
import com.projectapex.model.BookingRecord;
import com.projectapex.model.ConcurrencyLock;
import com.projectapex.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
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

    private String requireUserId(Authentication auth) {
        if (auth == null || auth.getName() == null || "anonymousUser".equals(auth.getName())) {
            throw new AccessDeniedException("Authentication required");
        }
        return auth.getName();
    }

    private boolean isAdmin(Authentication auth) {
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    private boolean isOwnerOrAdmin(Authentication auth) {
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_OWNER".equals(a.getAuthority()) || "ROLE_ADMIN".equals(a.getAuthority()));
    }

    /**
     * POST /api/v1/bookings/initiate - Atomically holds slots and creates Razorpay Order
     */
    @PostMapping("/bookings/initiate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> initiateBooking(
            @Valid @RequestBody InitiateBookingRequest req,
            Authentication auth
    ) {
        String userId = requireUserId(auth);
        Map<String, Object> res = bookingService.initiateBooking(req, userId);
        return ResponseEntity.ok(ApiResponse.ok("Booking initiated. 10-minute slot hold acquired.", res));
    }

    /**
     * POST /api/v1/bookings/{bookingId}/verify-payment - Verifies Razorpay signature & confirms booking
     */
    @PostMapping("/bookings/{bookingId}/verify-payment")
    public ResponseEntity<ApiResponse<BookingRecord>> verifyPayment(
            @PathVariable String bookingId,
            @Valid @RequestBody VerifyPaymentRequest req,
            Authentication auth
    ) {
        String callerUid = requireUserId(auth);
        BookingRecord existing = bookingService.getBooking(bookingId);
        if (!callerUid.equals(existing.getUserId()) && !isAdmin(auth)) {
            throw new AccessDeniedException("Unauthorized to verify payment for another user's booking");
        }
        BookingRecord confirmed = bookingService.verifyPayment(bookingId, req);
        return ResponseEntity.ok(ApiResponse.ok("Payment verified and booking confirmed", confirmed));
    }

    /**
     * GET /api/v1/bookings/my or /api/v1/bookings/my-passes - Player's bookings
     */
    @GetMapping({"/bookings/my", "/bookings/my-passes"})
    public ResponseEntity<ApiResponse<List<BookingRecord>>> getMyBookings(Authentication auth) {
        String userId = requireUserId(auth);
        List<BookingRecord> bookings = bookingService.getPlayerBookings(userId);
        return ResponseEntity.ok(ApiResponse.ok("Player bookings retrieved", bookings));
    }

    /**
     * GET /api/v1/bookings/{bookingId} - Booking details (Strict user/owner isolation)
     */
    @GetMapping("/bookings/{bookingId}")
    public ResponseEntity<ApiResponse<BookingRecord>> getBooking(@PathVariable String bookingId, Authentication auth) {
        String callerUid = requireUserId(auth);
        BookingRecord b = bookingService.getBooking(bookingId);
        boolean isOwner = b.getOwnerId() != null && callerUid.equals(b.getOwnerId());
        if (!callerUid.equals(b.getUserId()) && !isOwner && !isAdmin(auth)) {
            throw new AccessDeniedException("Unauthorized to view another user's booking");
        }
        return ResponseEntity.ok(ApiResponse.ok("Booking details", b));
    }

    /**
     * PUT / PATCH /api/v1/bookings/{bookingId} - Reschedule / update booking details atomically
     */
    @RequestMapping(value = "/bookings/{bookingId}", method = {RequestMethod.PUT, RequestMethod.PATCH})
    public ResponseEntity<ApiResponse<BookingRecord>> updateBooking(
            @PathVariable String bookingId,
            @RequestBody UpdateBookingRequest req,
            Authentication auth
    ) {
        String userId = requireUserId(auth);
        BookingRecord updated = bookingService.updateBookingByPlayer(bookingId, userId, req, isAdmin(auth));
        return ResponseEntity.ok(ApiResponse.ok("Booking updated and rescheduled successfully", updated));
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
        String userId = requireUserId(auth);
        String reason = req != null && req.getReason() != null ? req.getReason() : "Player requested cancellation";
        BookingRecord cancelled = bookingService.cancelBookingByPlayer(bookingId, userId, reason);
        return ResponseEntity.ok(ApiResponse.ok("Booking cancelled and refund processed", cancelled));
    }

    /**
     * DELETE /api/v1/bookings/{bookingId} - Delete booking & release any active slot locks
     */
    @DeleteMapping("/bookings/{bookingId}")
    public ResponseEntity<ApiResponse<Void>> deleteBooking(@PathVariable String bookingId, Authentication auth) {
        String actorId = requireUserId(auth);
        bookingService.deleteBooking(bookingId, actorId, isAdmin(auth));
        return ResponseEntity.ok(ApiResponse.ok("Booking deleted and slot lock released", null));
    }

    // =========================================================================
    // Compatibility Endpoints for Test Suite & Web Checkout
    // =========================================================================

    @PostMapping("/bookings/commit")
    public ResponseEntity<ApiResponse<BookingRecord>> commitBooking(
            @Valid @RequestBody CommitBookingRequest req,
            Authentication auth) {
        String userId = requireUserId(auth);
        BookingRecord b = bookingService.commitBooking(req, userId);
        return ResponseEntity.ok(ApiResponse.ok("Booking committed", b));
    }

    @PostMapping("/bookings/walkin")
    public ResponseEntity<ApiResponse<BookingRecord>> walkInBooking(
            @Valid @RequestBody WalkInBookingRequest req,
            Authentication auth) {
        String ownerId = requireUserId(auth);
        if (!isOwnerOrAdmin(auth)) {
            throw new AccessDeniedException("Facility Owner role required for walk-in reservations");
        }
        BookingRecord b = bookingService.createWalkInBooking(
                isAdmin(auth) ? "admin" : ownerId,
                req.getCourtId(),
                req.getDate(),
                req.getSlot(),
                req.getCustomerName(),
                req.getCustomerPhone(),
                req.getAmountCollected()
        );
        return ResponseEntity.ok(ApiResponse.ok("Walk-in booking recorded", b));
    }
}

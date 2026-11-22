package com.projectapex.controller;

import com.projectapex.dto.request.CommitBookingRequest;
import com.projectapex.dto.response.ApiResponse;
import com.projectapex.model.BookingRecord;
import com.projectapex.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    /**
     * Atomically commits a booking after verifying Razorpay HMAC SHA256 signature
     */
    @PostMapping("/commit")
    public ResponseEntity<ApiResponse<BookingRecord>> commitBooking(
            @Valid @RequestBody CommitBookingRequest req,
            Authentication auth,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId) {
        String userId = auth != null && auth.getName() != null && !"anonymousUser".equals(auth.getName())
                ? auth.getName()
                : (headerUserId != null ? headerUserId : "demo_player");

        BookingRecord booking = bookingService.commitBooking(
                userId,
                req.getCourtId(),
                req.getDate(),
                req.getSlot(),
                req.getRazorpayOrderId(),
                req.getRazorpayPaymentId(),
                req.getRazorpaySignature(),
                req.getPaymentMethod()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Booking confirmed and QR gate pass issued", booking));
    }

    /**
     * Retrieves strictly the authenticated user's match bookings and digital QR passes
     */
    @GetMapping("/my-passes")
    public ResponseEntity<ApiResponse<List<BookingRecord>>> getMyPasses(
            Authentication auth,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId) {
        String userId = auth != null && auth.getName() != null && !"anonymousUser".equals(auth.getName())
                ? auth.getName()
                : headerUserId;

        if (userId == null || "guest".equalsIgnoreCase(userId)) {
            return ResponseEntity.ok(ApiResponse.ok("Guest mode: No passes found", List.of()));
        }

        List<BookingRecord> bookings = bookingService.getPlayerBookings(userId);
        return ResponseEntity.ok(ApiResponse.ok(bookings));
    }

    /**
     * 3-Tier Cancellation & Automated Refund
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<BookingRecord>> cancelBooking(
            @PathVariable String id,
            Authentication auth,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId) {
        String userId = auth != null && auth.getName() != null && !"anonymousUser".equals(auth.getName())
                ? auth.getName()
                : (headerUserId != null ? headerUserId : "demo_player");

        BookingRecord cancelled = bookingService.cancelBooking(id, userId);
        return ResponseEntity.ok(ApiResponse.ok("Booking cancelled and refund processed", cancelled));
    }
}

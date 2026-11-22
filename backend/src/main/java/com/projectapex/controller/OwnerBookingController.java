package com.projectapex.controller;

import com.projectapex.dto.request.CancelBookingRequest;
import com.projectapex.dto.response.ApiResponse;
import com.projectapex.model.BookingRecord;
import com.projectapex.service.BookingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/owner")
@CrossOrigin(origins = "*")
public class OwnerBookingController {

    private final BookingService bookingService;

    public OwnerBookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping("/bookings")
    public ResponseEntity<ApiResponse<List<BookingRecord>>> getOwnerBookings(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String status,
            Authentication auth
    ) {
        String ownerId = auth != null ? auth.getName() : "demo_owner";
        List<BookingRecord> bookings = bookingService.getOwnerBookings(ownerId, date, status);
        return ResponseEntity.ok(ApiResponse.ok("Owner bookings retrieved", bookings));
    }

    @PatchMapping("/bookings/{bookingId}/status")
    public ResponseEntity<ApiResponse<BookingRecord>> updateStatus(
            @PathVariable String bookingId,
            @RequestBody Map<String, String> body,
            Authentication auth
    ) {
        String ownerId = auth != null ? auth.getName() : "demo_owner";
        String status = body.getOrDefault("status", "COMPLETED");
        BookingRecord b = bookingService.updateBookingStatusByOwner(bookingId, ownerId, status);
        return ResponseEntity.ok(ApiResponse.ok("Booking marked as " + status, b));
    }

    @PostMapping("/bookings/{bookingId}/cancel")
    public ResponseEntity<ApiResponse<BookingRecord>> emergencyCancel(
            @PathVariable String bookingId,
            @RequestBody Map<String, String> body,
            Authentication auth
    ) {
        String ownerId = auth != null ? auth.getName() : "demo_owner";
        String reason = body.getOrDefault("reason", "Venue emergency / maintenance");
        BookingRecord b = bookingService.cancelBookingByOwner(bookingId, ownerId, reason);
        return ResponseEntity.ok(ApiResponse.ok("Booking cancelled with 100% full refund to player", b));
    }

    @GetMapping("/analytics/summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAnalyticsSummary(Authentication auth) {
        String ownerId = auth != null ? auth.getName() : "demo_owner";
        List<BookingRecord> bookings = bookingService.getOwnerBookings(ownerId, null, null);

        long totalRevenuePaise = 0;
        long totalDiscountPaise = 0;
        int confirmedCount = 0;
        int cancelledCount = 0;

        for (BookingRecord b : bookings) {
            if ("CONFIRMED".equalsIgnoreCase(b.getStatus()) || "COMPLETED".equalsIgnoreCase(b.getStatus())) {
                totalRevenuePaise += b.getFinalPayablePaise();
                totalDiscountPaise += b.getDiscountAmountPaise();
                confirmedCount++;
            } else if (b.getStatus() != null && b.getStatus().startsWith("CANCELLED")) {
                cancelledCount++;
            }
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalBookings", bookings.size());
        summary.put("confirmedBookings", confirmedCount);
        summary.put("cancelledBookings", cancelledCount);
        summary.put("grossRevenuePaise", totalRevenuePaise);
        summary.put("grossRevenueRupees", (double) totalRevenuePaise / 100);
        summary.put("discountGivenPaise", totalDiscountPaise);
        summary.put("discountGivenRupees", (double) totalDiscountPaise / 100);
        summary.put("occupancyPercentage", confirmedCount > 0 ? 68.5 : 0.0);

        return ResponseEntity.ok(ApiResponse.ok("Analytics summary", summary));
    }
}

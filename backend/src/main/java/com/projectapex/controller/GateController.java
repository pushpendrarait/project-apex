package com.projectapex.controller;

import com.projectapex.dto.request.VerifyGatePassRequest;
import com.projectapex.dto.request.WalkInBookingRequest;
import com.projectapex.dto.response.ApiResponse;
import com.projectapex.model.BookingRecord;
import com.projectapex.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/gate")
public class GateController {

    private final BookingService bookingService;

    public GateController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    /**
     * Verifies digital scannable QR Pass Token at stadium / turf turnstiles
     */
    @PostMapping("/verify-pass")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyGatePass(
            @Valid @RequestBody VerifyGatePassRequest req,
            Authentication auth,
            @RequestHeader(value = "X-Owner-Id", required = false) String headerOwnerId) {
        String ownerId = auth != null && auth.getName() != null && !"anonymousUser".equals(auth.getName())
                ? auth.getName()
                : (headerOwnerId != null ? headerOwnerId : "demo_owner");

        Map<String, Object> result = bookingService.verifyGatePass(req.getQrPassToken(), ownerId);
        return ResponseEntity.ok(ApiResponse.ok("Gate token processed", result));
    }

    /**
     * Counter Walk-In / Phone Reservation with Cash Settlement
     */
    @PostMapping("/walk-in")
    public ResponseEntity<ApiResponse<BookingRecord>> createWalkInBooking(
            @Valid @RequestBody WalkInBookingRequest req,
            Authentication auth,
            @RequestHeader(value = "X-Owner-Id", required = false) String headerOwnerId) {
        String ownerId = auth != null && auth.getName() != null && !"anonymousUser".equals(auth.getName())
                ? auth.getName()
                : (headerOwnerId != null ? headerOwnerId : "demo_owner");

        BookingRecord booking = bookingService.createWalkInBooking(
                ownerId,
                req.getCourtId(),
                req.getDate(),
                req.getSlot(),
                req.getCustomerName(),
                req.getCustomerPhone(),
                req.getAmountCollected()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Walk-in booking created and cash collected", booking));
    }
}

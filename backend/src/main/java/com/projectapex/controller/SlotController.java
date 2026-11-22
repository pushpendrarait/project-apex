package com.projectapex.controller;

import com.projectapex.dto.request.AcquireLockRequest;
import com.projectapex.dto.response.ApiResponse;
import com.projectapex.model.AvailabilityDoc;
import com.projectapex.model.ConcurrencyLock;
import com.projectapex.service.ConcurrencyService;
import com.projectapex.service.VenueService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/slots")
public class SlotController {

    private final VenueService venueService;
    private final ConcurrencyService concurrencyService;

    public SlotController(VenueService venueService, ConcurrencyService concurrencyService) {
        this.venueService = venueService;
        this.concurrencyService = concurrencyService;
    }

    /**
     * Retrieves Consolidated Daily Document for a court (1 read per court per day)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<AvailabilityDoc>> getSlots(
            @RequestParam String courtId,
            @RequestParam(defaultValue = "2026-11-28") String date) {
        AvailabilityDoc doc = venueService.getDailyAvailability(courtId, date);
        return ResponseEntity.ok(ApiResponse.ok(doc));
    }

    /**
     * Acquires 5-minute atomic reservation check-and-set lease
     */
    @PostMapping("/lock")
    public ResponseEntity<ApiResponse<ConcurrencyLock>> acquireLock(
            @Valid @RequestBody AcquireLockRequest req,
            Authentication auth,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId) {
        String userId = auth != null && auth.getName() != null && !"anonymousUser".equals(auth.getName())
                ? auth.getName()
                : (headerUserId != null ? headerUserId : "demo_player");

        ConcurrencyLock lock = concurrencyService.acquireLock(req.getCourtId(), req.getDate(), req.getSlot(), userId);
        return ResponseEntity.ok(ApiResponse.ok("5-minute concurrency lease acquired successfully", lock));
    }

    /**
     * Releases reservation lock
     */
    @DeleteMapping("/lock")
    public ResponseEntity<ApiResponse<Void>> releaseLock(
            @RequestParam String courtId,
            @RequestParam String date,
            @RequestParam String slot,
            Authentication auth,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId) {
        String userId = auth != null && auth.getName() != null && !"anonymousUser".equals(auth.getName())
                ? auth.getName()
                : (headerUserId != null ? headerUserId : "demo_player");

        concurrencyService.releaseLock(courtId, date, slot, userId);
        return ResponseEntity.ok(ApiResponse.ok("Lock released successfully", null));
    }
}

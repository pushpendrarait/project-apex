package com.projectapex.controller;

import com.projectapex.dto.request.CreateFacilityRequest;
import com.projectapex.dto.response.ApiResponse;
import com.projectapex.model.FacilityRecord;
import com.projectapex.service.VenueService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/facilities")
public class FacilityController {

    private final VenueService venueService;

    public FacilityController(VenueService venueService) {
        this.venueService = venueService;
    }

    private String requireOwnerId(Authentication auth) {
        if (auth == null || auth.getName() == null || "anonymousUser".equals(auth.getName())) {
            throw new AccessDeniedException("Facility Owner authentication required");
        }
        return auth.getName();
    }

    private boolean isAdmin(Authentication auth) {
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    /**
     * Adds a new sports facility / pitch / court with capacity, surface, dimensions, and amenities
     */
    @PostMapping
    public ResponseEntity<ApiResponse<FacilityRecord>> addFacility(
            @Valid @RequestBody CreateFacilityRequest req,
            Authentication auth) {
        String ownerId = isAdmin(auth) ? "admin" : requireOwnerId(auth);
        FacilityRecord facility = venueService.createFacility(ownerId, req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Facility created and published successfully", facility));
    }

    @GetMapping("/{courtId}")
    public ResponseEntity<ApiResponse<FacilityRecord>> getFacility(@PathVariable String courtId) {
        return ResponseEntity.ok(ApiResponse.ok("Facility details retrieved", venueService.getFacility(courtId)));
    }

    @PutMapping("/{courtId}")
    public ResponseEntity<ApiResponse<FacilityRecord>> updateFacility(
            @PathVariable String courtId,
            @RequestBody CreateFacilityRequest req,
            Authentication auth) {
        String ownerId = requireOwnerId(auth);
        FacilityRecord updated = venueService.updateFacility(courtId, ownerId, req, isAdmin(auth));
        return ResponseEntity.ok(ApiResponse.ok("Facility updated successfully", updated));
    }

    @DeleteMapping("/{courtId}")
    public ResponseEntity<ApiResponse<Void>> deleteFacility(
            @PathVariable String courtId,
            Authentication auth) {
        String ownerId = requireOwnerId(auth);
        venueService.deleteFacility(courtId, ownerId, isAdmin(auth));
        return ResponseEntity.ok(ApiResponse.ok("Facility deleted successfully", null));
    }
}

package com.projectapex.controller;

import com.projectapex.dto.request.CreateFacilityRequest;
import com.projectapex.dto.response.ApiResponse;
import com.projectapex.model.FacilityRecord;
import com.projectapex.service.VenueService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/facilities")
public class FacilityController {

    private final VenueService venueService;

    public FacilityController(VenueService venueService) {
        this.venueService = venueService;
    }

    /**
     * Adds a new sports facility / pitch / court with capacity, surface, dimensions, and amenities
     */
    @PostMapping
    public ResponseEntity<ApiResponse<FacilityRecord>> addFacility(
            @Valid @RequestBody CreateFacilityRequest req,
            Authentication auth,
            @RequestHeader(value = "X-Owner-Id", required = false) String headerOwnerId) {
        String ownerId = auth != null && auth.getName() != null && !"anonymousUser".equals(auth.getName())
                ? auth.getName()
                : (headerOwnerId != null ? headerOwnerId : "demo_owner");

        FacilityRecord facility = venueService.createFacility(ownerId, req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Facility created and published successfully", facility));
    }
}

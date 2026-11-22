package com.projectapex.controller;

import com.projectapex.dto.request.CreateResourceRequest;
import com.projectapex.dto.request.CreateVenueRequest;
import com.projectapex.dto.request.DateOverrideRequest;
import com.projectapex.dto.response.ApiResponse;
import com.projectapex.model.*;
import com.projectapex.service.VenueService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/owner")
@CrossOrigin(origins = "*")
public class OwnerVenueController {

    private final VenueService venueService;

    public OwnerVenueController(VenueService venueService) {
        this.venueService = venueService;
    }

    @PostMapping("/venues")
    public ResponseEntity<ApiResponse<VenueRecord>> createVenue(
            @Valid @RequestBody CreateVenueRequest req,
            Authentication auth
    ) {
        String ownerId = auth != null ? auth.getName() : "demo_owner";
        VenueRecord venue = venueService.createVenue(ownerId, req);
        return ResponseEntity.ok(ApiResponse.ok("Venue created successfully", venue));
    }

    @GetMapping("/venues")
    public ResponseEntity<ApiResponse<List<VenueRecord>>> getOwnerVenues(Authentication auth) {
        String ownerId = auth != null ? auth.getName() : "demo_owner";
        List<VenueRecord> venues = venueService.getOwnerVenues(ownerId);
        return ResponseEntity.ok(ApiResponse.ok("Owner venues retrieved", venues));
    }

    @PutMapping("/venues/{venueId}")
    public ResponseEntity<ApiResponse<VenueRecord>> updateVenue(
            @PathVariable String venueId,
            @Valid @RequestBody CreateVenueRequest req,
            Authentication auth
    ) {
        String ownerId = auth != null ? auth.getName() : "demo_owner";
        VenueRecord updated = venueService.updateVenue(venueId, ownerId, req);
        return ResponseEntity.ok(ApiResponse.ok("Venue updated successfully", updated));
    }

    @PostMapping("/venues/{venueId}/resources")
    public ResponseEntity<ApiResponse<ResourceRecord>> addResource(
            @PathVariable String venueId,
            @Valid @RequestBody CreateResourceRequest req,
            Authentication auth
    ) {
        String ownerId = auth != null ? auth.getName() : "demo_owner";
        ResourceRecord r = venueService.createResource(ownerId, venueId, req);
        return ResponseEntity.ok(ApiResponse.ok("Resource added to venue", r));
    }

    @PutMapping("/resources/{resourceId}")
    public ResponseEntity<ApiResponse<ResourceRecord>> updateResource(
            @PathVariable String resourceId,
            @Valid @RequestBody CreateResourceRequest req,
            Authentication auth
    ) {
        String ownerId = auth != null ? auth.getName() : "demo_owner";
        ResourceRecord r = venueService.updateResource(resourceId, ownerId, req);
        return ResponseEntity.ok(ApiResponse.ok("Resource updated", r));
    }

    @PutMapping("/resources/{resourceId}/schedule")
    public ResponseEntity<ApiResponse<ScheduleRuleRecord>> updateSchedule(
            @PathVariable String resourceId,
            @RequestBody ScheduleRuleRecord schedule,
            Authentication auth
    ) {
        String ownerId = auth != null ? auth.getName() : "demo_owner";
        ScheduleRuleRecord rule = venueService.updateScheduleRule(resourceId, ownerId, schedule);
        return ResponseEntity.ok(ApiResponse.ok("Weekly schedule updated", rule));
    }

    @PutMapping("/resources/{resourceId}/overrides/{date}")
    public ResponseEntity<ApiResponse<DateOverrideRecord>> updateDateOverride(
            @PathVariable String resourceId,
            @PathVariable String date,
            @RequestBody DateOverrideRequest req,
            Authentication auth
    ) {
        String ownerId = auth != null ? auth.getName() : "demo_owner";
        DateOverrideRecord override = venueService.updateDateOverride(resourceId, ownerId, date, req);
        return ResponseEntity.ok(ApiResponse.ok("Date override saved", override));
    }
}

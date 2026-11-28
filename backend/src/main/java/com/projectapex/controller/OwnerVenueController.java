package com.projectapex.controller;

import com.projectapex.dto.request.CreateResourceRequest;
import com.projectapex.dto.request.CreateVenueRequest;
import com.projectapex.dto.request.DateOverrideRequest;
import com.projectapex.dto.response.ApiResponse;
import com.projectapex.model.*;
import com.projectapex.service.VenueService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
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

    private String requireOwnerId(Authentication auth) {
        if (auth == null || auth.getName() == null || "anonymousUser".equals(auth.getName())) {
            throw new AccessDeniedException("Owner authentication required");
        }
        return auth.getName();
    }

    private boolean isAdmin(Authentication auth) {
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    @PostMapping("/venues")
    public ResponseEntity<ApiResponse<VenueRecord>> createVenue(
            @Valid @RequestBody CreateVenueRequest req,
            Authentication auth
    ) {
        String ownerId = requireOwnerId(auth);
        VenueRecord venue = venueService.createVenue(ownerId, req);
        return ResponseEntity.ok(ApiResponse.ok("Venue created successfully", venue));
    }

    @GetMapping("/venues")
    public ResponseEntity<ApiResponse<List<VenueRecord>>> getOwnerVenues(Authentication auth) {
        String ownerId = requireOwnerId(auth);
        List<VenueRecord> venues = venueService.getOwnerVenues(isAdmin(auth) ? "admin" : ownerId);
        return ResponseEntity.ok(ApiResponse.ok("Owner venues retrieved", venues));
    }

    @PutMapping("/venues/{venueId}")
    public ResponseEntity<ApiResponse<VenueRecord>> updateVenue(
            @PathVariable String venueId,
            @Valid @RequestBody CreateVenueRequest req,
            Authentication auth
    ) {
        String ownerId = requireOwnerId(auth);
        VenueRecord updated = venueService.updateVenue(venueId, isAdmin(auth) ? "admin" : ownerId, req);
        return ResponseEntity.ok(ApiResponse.ok("Venue updated successfully", updated));
    }

    @DeleteMapping("/venues/{venueId}")
    public ResponseEntity<ApiResponse<Void>> deleteVenue(
            @PathVariable String venueId,
            Authentication auth
    ) {
        String ownerId = requireOwnerId(auth);
        venueService.deleteVenue(venueId, ownerId, isAdmin(auth));
        return ResponseEntity.ok(ApiResponse.ok("Venue deleted successfully", null));
    }

    @PostMapping("/venues/{venueId}/resources")
    public ResponseEntity<ApiResponse<ResourceRecord>> addResource(
            @PathVariable String venueId,
            @Valid @RequestBody CreateResourceRequest req,
            Authentication auth
    ) {
        String ownerId = requireOwnerId(auth);
        ResourceRecord r = venueService.createResource(isAdmin(auth) ? "admin" : ownerId, venueId, req);
        return ResponseEntity.ok(ApiResponse.ok("Resource added to venue", r));
    }

    @PutMapping("/resources/{resourceId}")
    public ResponseEntity<ApiResponse<ResourceRecord>> updateResource(
            @PathVariable String resourceId,
            @Valid @RequestBody CreateResourceRequest req,
            Authentication auth
    ) {
        String ownerId = requireOwnerId(auth);
        ResourceRecord r = venueService.updateResource(resourceId, isAdmin(auth) ? "admin" : ownerId, req);
        return ResponseEntity.ok(ApiResponse.ok("Resource updated", r));
    }

    @DeleteMapping("/resources/{resourceId}")
    public ResponseEntity<ApiResponse<Void>> deleteResource(
            @PathVariable String resourceId,
            Authentication auth
    ) {
        String ownerId = requireOwnerId(auth);
        venueService.deleteResource(resourceId, ownerId, isAdmin(auth));
        return ResponseEntity.ok(ApiResponse.ok("Resource deleted successfully", null));
    }

    @PutMapping("/resources/{resourceId}/schedule")
    public ResponseEntity<ApiResponse<ScheduleRuleRecord>> updateSchedule(
            @PathVariable String resourceId,
            @RequestBody ScheduleRuleRecord schedule,
            Authentication auth
    ) {
        String ownerId = requireOwnerId(auth);
        ScheduleRuleRecord rule = venueService.updateScheduleRule(resourceId, isAdmin(auth) ? "admin" : ownerId, schedule);
        return ResponseEntity.ok(ApiResponse.ok("Weekly schedule updated", rule));
    }

    @PutMapping("/resources/{resourceId}/overrides/{date}")
    public ResponseEntity<ApiResponse<DateOverrideRecord>> updateDateOverride(
            @PathVariable String resourceId,
            @PathVariable String date,
            @RequestBody DateOverrideRequest req,
            Authentication auth
    ) {
        String ownerId = requireOwnerId(auth);
        DateOverrideRecord override = venueService.updateDateOverride(resourceId, isAdmin(auth) ? "admin" : ownerId, date, req);
        return ResponseEntity.ok(ApiResponse.ok("Date override saved", override));
    }
}

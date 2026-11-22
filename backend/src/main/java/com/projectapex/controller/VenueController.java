package com.projectapex.controller;

import com.projectapex.dto.response.ApiResponse;
import com.projectapex.model.FacilityRecord;
import com.projectapex.model.VenueRecord;
import com.projectapex.service.VenueService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/venues")
public class VenueController {

    private final VenueService venueService;

    public VenueController(VenueService venueService) {
        this.venueService = venueService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<VenueRecord>>> getVenues(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String sport) {
        List<VenueRecord> venues = venueService.getVenues(city, sport);
        return ResponseEntity.ok(ApiResponse.ok(venues));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VenueRecord>> getVenueById(@PathVariable String id) {
        VenueRecord venue = venueService.getVenueById(id);
        if (venue == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Venue not found: " + id));
        }
        return ResponseEntity.ok(ApiResponse.ok(venue));
    }

    @GetMapping("/{id}/courts")
    public ResponseEntity<ApiResponse<List<FacilityRecord>>> getCourtsForVenue(@PathVariable String id) {
        List<FacilityRecord> courts = venueService.getCourtsForVenue(id);
        return ResponseEntity.ok(ApiResponse.ok(courts));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<VenueRecord>> createVenue(@RequestBody VenueRecord venue) {
        VenueRecord created = venueService.createVenue(venue);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Venue created successfully", created));
    }
}

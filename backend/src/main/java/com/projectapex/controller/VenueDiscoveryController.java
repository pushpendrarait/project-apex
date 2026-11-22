package com.projectapex.controller;

import com.projectapex.dto.response.ApiResponse;
import com.projectapex.model.ResourceRecord;
import com.projectapex.model.SlotAvailability;
import com.projectapex.model.SportType;
import com.projectapex.model.VenueRecord;
import com.projectapex.service.CalendarAvailabilityService;
import com.projectapex.service.PricingAndDiscountService;
import com.projectapex.service.VenueService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class VenueDiscoveryController {

    private final VenueService venueService;
    private final CalendarAvailabilityService availabilityService;
    private final PricingAndDiscountService pricingService;

    public VenueDiscoveryController(VenueService venueService,
                                    CalendarAvailabilityService availabilityService,
                                    PricingAndDiscountService pricingService) {
        this.venueService = venueService;
        this.availabilityService = availabilityService;
        this.pricingService = pricingService;
    }

    /**
     * GET /api/v1/sports - List all active sport categories
     */
    @GetMapping("/sports")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getSports() {
        List<Map<String, Object>> sports = new ArrayList<>();
        for (SportType st : SportType.values()) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", st.name());
            item.put("name", st.getDisplayName());
            item.put("category", st.getCategory());
            item.put("icon", st.getIcon());
            item.put("isTurf", st.isTurf());
            sports.add(item);
        }
        return ResponseEntity.ok(ApiResponse.ok("Active sports categories", sports));
    }

    /**
     * GET /api/v1/venues - Search/Filter sports venues
     */
    @GetMapping("/venues")
    public ResponseEntity<ApiResponse<List<VenueRecord>>> getVenues(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String locality,
            @RequestParam(required = false) String sportType,
            @RequestParam(required = false) Long minPrice,
            @RequestParam(required = false) Long maxPrice,
            @RequestParam(required = false) Boolean hasActiveOffers,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        List<VenueRecord> venues = venueService.searchVenues(city, locality, sportType, minPrice, maxPrice, hasActiveOffers, q, page, size);
        return ResponseEntity.ok(ApiResponse.ok("Venues retrieved", venues));
    }

    /**
     * GET /api/v1/venues/{venueId} - Venue details with playable units (resources)
     */
    @GetMapping("/venues/{venueId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getVenueDetails(@PathVariable String venueId) {
        VenueRecord venue = venueService.getVenueById(venueId);
        List<ResourceRecord> resources = venueService.getResourcesForVenue(venueId);

        Map<String, Object> data = new HashMap<>();
        data.put("venue", venue);
        data.put("resources", resources);
        return ResponseEntity.ok(ApiResponse.ok("Venue details", data));
    }

    @GetMapping("/venues/{venueId}/courts")
    public ResponseEntity<ApiResponse<List<com.projectapex.model.FacilityRecord>>> getCourtsForVenue(@PathVariable String venueId) {
        List<com.projectapex.model.FacilityRecord> courts = venueService.getCourtsForVenue(venueId);
        return ResponseEntity.ok(ApiResponse.ok(courts));
    }

    /**
     * GET /api/v1/availability - Query generated slots with live status & dynamic prices
     */
    @GetMapping("/availability")
    public ResponseEntity<ApiResponse<List<SlotAvailability>>> getAvailability(
            @RequestParam String resourceId,
            @RequestParam String date
    ) {
        List<SlotAvailability> slots = availabilityService.getGeneratedSlots(resourceId, date);
        return ResponseEntity.ok(ApiResponse.ok("Slot availability", slots));
    }

    /**
     * POST /api/v1/pricing/quote - Preview price breakdown and validate promo code
     */
    @PostMapping("/pricing/quote")
    public ResponseEntity<ApiResponse<PricingAndDiscountService.PriceQuote>> getQuote(
            @RequestBody Map<String, Object> body,
            Authentication auth
    ) {
        String venueId = (String) body.get("venueId");
        String resourceId = (String) body.get("resourceId");
        String date = (String) body.get("date");
        String promoCode = (String) body.get("promoCode");
        List<String> slotTimes = (List<String>) body.get("slots");
        String userId = auth != null ? auth.getName() : "demo_player";

        PricingAndDiscountService.PriceQuote quote = pricingService.calculateQuote(
                venueId, resourceId, date, slotTimes != null ? slotTimes : Collections.emptyList(), promoCode, userId);

        return ResponseEntity.ok(ApiResponse.ok("Price quote generated", quote));
    }
}

package com.projectapex.controller;

import com.projectapex.dto.response.ApiResponse;
import com.projectapex.model.ResourceRecord;
import com.projectapex.model.SlotAvailability;
import com.projectapex.model.VenueRecord;
import com.projectapex.service.CalendarAvailabilityService;
import com.projectapex.service.FirebaseService;
import com.projectapex.service.VenueService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/v1/owner")
@CrossOrigin(origins = "*")
public class OwnerCalendarController {

    private final CalendarAvailabilityService availabilityService;
    private final VenueService venueService;
    private final FirebaseService firebaseService;

    public OwnerCalendarController(CalendarAvailabilityService availabilityService,
                                   VenueService venueService,
                                   FirebaseService firebaseService) {
        this.availabilityService = availabilityService;
        this.venueService = venueService;
        this.firebaseService = firebaseService;
    }

    /**
     * GET /api/v1/owner/calendar - Master Calendar View across Date Range
     */
    @GetMapping("/calendar")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMasterCalendar(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String venueId,
            @RequestParam(required = false) String resourceId,
            Authentication auth
    ) {
        String ownerId = auth != null ? auth.getName() : "demo_owner";

        LocalDate start = (startDate != null && !startDate.isEmpty()) ? LocalDate.parse(startDate) : LocalDate.now();
        LocalDate end = (endDate != null && !endDate.isEmpty()) ? LocalDate.parse(endDate) : start.plusDays(6);

        // Find applicable resources for this owner
        List<ResourceRecord> targetResources = new ArrayList<>();
        if (resourceId != null && !resourceId.isEmpty()) {
            ResourceRecord r = firebaseService.getResources().get(resourceId);
            if (r != null && (ownerId.equals(r.getOwnerId()) || "admin".equalsIgnoreCase(ownerId))) {
                targetResources.add(r);
            }
        } else if (venueId != null && !venueId.isEmpty()) {
            targetResources.addAll(venueService.getResourcesForVenue(venueId));
        } else {
            List<VenueRecord> ownerVenues = venueService.getOwnerVenues(ownerId);
            for (VenueRecord v : ownerVenues) {
                targetResources.addAll(venueService.getResourcesForVenue(v.getVenueId()));
            }
        }

        Map<String, Object> calendarData = new HashMap<>();
        List<Map<String, Object>> days = new ArrayList<>();

        LocalDate curr = start;
        while (!curr.isAfter(end)) {
            String dateStr = curr.toString();
            Map<String, Object> dayInfo = new HashMap<>();
            dayInfo.put("date", dateStr);
            dayInfo.put("dayOfWeek", curr.getDayOfWeek().name());

            List<Map<String, Object>> resourceSlots = new ArrayList<>();
            for (ResourceRecord r : targetResources) {
                List<SlotAvailability> slots = availabilityService.getGeneratedSlots(r.getResourceId(), dateStr);
                Map<String, Object> rMap = new HashMap<>();
                rMap.put("resourceId", r.getResourceId());
                rMap.put("resourceName", r.getName());
                rMap.put("venueId", r.getVenueId());
                rMap.put("slots", slots);
                resourceSlots.add(rMap);
            }
            dayInfo.put("resources", resourceSlots);
            days.add(dayInfo);
            curr = curr.plusDays(1);
        }

        calendarData.put("startDate", start.toString());
        calendarData.put("endDate", end.toString());
        calendarData.put("days", days);

        return ResponseEntity.ok(ApiResponse.ok("Master calendar retrieved", calendarData));
    }
}

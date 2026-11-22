package com.projectapex.service;

import com.projectapex.dto.request.CreateFacilityRequest;
import com.projectapex.model.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class VenueService {

    private final FirebaseService firebaseService;
    private final PricingService pricingService;

    public VenueService(FirebaseService firebaseService, PricingService pricingService) {
        this.firebaseService = firebaseService;
        this.pricingService = pricingService;
    }

    public List<VenueRecord> getVenues(String city, String sport) {
        return firebaseService.getVenues().values().stream()
                .filter(v -> city == null || "all".equalsIgnoreCase(city) || city.equalsIgnoreCase(v.getCity()))
                .filter(v -> sport == null || "all".equalsIgnoreCase(sport) || (v.getSports() != null && v.getSports().contains(sport.toLowerCase())))
                .sorted((a, b) -> Double.compare(b.getRating(), a.getRating()))
                .toList();
    }

    public VenueRecord getVenueById(String venueId) {
        return firebaseService.getVenues().get(venueId);
    }

    public List<FacilityRecord> getCourtsForVenue(String venueId) {
        return firebaseService.getCourts().values().stream()
                .filter(c -> venueId.equals(c.getVenueId()))
                .toList();
    }

    public synchronized VenueRecord createVenue(VenueRecord venue) {
        if (venue.getVenueId() == null) {
            String slug = venue.getName().toLowerCase().replaceAll("[^a-z0-9]", "_");
            venue.setVenueId("venue_" + slug + "_" + (100 + (int)(Math.random() * 900)));
        }
        venue.setCreatedAt(System.currentTimeMillis());
        venue.setStatus("ACTIVE");
        if (venue.getRating() == null) venue.setRating(5.0);
        if (venue.getReviewsCount() == null) venue.setReviewsCount(1);
        firebaseService.getVenues().put(venue.getVenueId(), venue);
        return venue;
    }

    /**
     * Adds a new sports facility / court with granular capacity, surface, and dimensions
     */
    public synchronized FacilityRecord createFacility(String ownerId, CreateFacilityRequest req) {
        VenueRecord venue = firebaseService.getVenues().get(req.getVenueId());
        if (venue == null) {
            throw new IllegalArgumentException("Venue ID not found: " + req.getVenueId());
        }

        String courtSlug = req.getName().toLowerCase().replaceAll("[^a-z0-9]", "_");
        String courtId = "court_" + courtSlug + "_" + (100 + (int)(Math.random() * 900));

        FacilityRecord facility = new FacilityRecord();
        facility.setCourtId(courtId);
        facility.setVenueId(req.getVenueId());
        facility.setName(req.getName());
        facility.setSport(req.getSport().toLowerCase());
        facility.setFacilityType(req.getFacilityType());
        facility.setCapacity(req.getCapacity());
        facility.setSurface(req.getSurface() != null ? req.getSurface() : "Professional Synthetic Turf");
        facility.setDimensions(req.getDimensions() != null ? req.getDimensions() : "Standard Turf Dimensions");
        facility.setBaseHourlyRate(req.getBaseHourlyRate());
        facility.setPeakHourlyRate(req.getPeakHourlyRate());
        facility.setOddHourDiscountPct(req.getOddHourDiscountPct());
        facility.setHasFloodlights(req.getHasFloodlights());
        facility.setAmenities(req.getAmenities() != null ? req.getAmenities() : List.of("Floodlights", "Changing Rooms"));
        facility.setStatus("ACTIVE");
        facility.setCreatedAt(System.currentTimeMillis());

        firebaseService.getCourts().put(courtId, facility);

        // Update venue's sports list if new
        if (venue.getSports() != null && !venue.getSports().contains(facility.getSport())) {
            List<String> updatedSports = new ArrayList<>(venue.getSports());
            updatedSports.add(facility.getSport());
            venue.setSports(updatedSports);
        }

        // Generate initial availability for today and upcoming dates
        String today = "2026-11-28";
        getDailyAvailability(courtId, today);

        return facility;
    }

    /**
     * Generates / Retrieves Consolidated Daily Document (1 read per court per day)
     */
    public synchronized AvailabilityDoc getDailyAvailability(String courtId, String date) {
        String docId = courtId + "_" + date;
        AvailabilityDoc existing = firebaseService.getAvailability().get(docId);
        if (existing != null) {
            return existing;
        }

        FacilityRecord facility = firebaseService.getCourts().get(courtId);
        int baseRate = facility != null ? facility.getBaseHourlyRate() : 1000;
        int oddDiscount = facility != null ? facility.getOddHourDiscountPct() : 35;

        Map<String, SlotInfo> slots = new LinkedHashMap<>();
        for (int h = 6; h <= 23; h++) {
            String slotTime = String.format("%02d:00", h);
            PricingService.PricingResult p = pricingService.calculatePrice(facility, slotTime, null);
            SlotInfo slot = new SlotInfo(slotTime, p.baseRate, p.isOddHour, p.discountPct, p.bandName);
            slot.setEffectivePrice(p.total - p.convenienceFee); // slot price without convenience fee
            slots.put(slotTime, slot);
        }

        AvailabilityDoc newDoc = new AvailabilityDoc(courtId, facility != null ? facility.getVenueId() : "", date, slots);
        firebaseService.getAvailability().put(docId, newDoc);
        return newDoc;
    }
}

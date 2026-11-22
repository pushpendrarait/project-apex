package com.projectapex.service;

import com.projectapex.dto.request.CreateFacilityRequest;
import com.projectapex.dto.request.CreateResourceRequest;
import com.projectapex.dto.request.CreateVenueRequest;
import com.projectapex.dto.request.DateOverrideRequest;
import com.projectapex.model.*;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class VenueService {

    private final FirebaseService firebaseService;

    public VenueService(FirebaseService firebaseService) {
        this.firebaseService = firebaseService;
    }

    /**
     * Discovery & Search for Sports Venues with Filtering & Pagination
     */
    @Cacheable(value = "venues", key = "#city + '-' + #sportType + '-' + #q + '-' + #page")
    public List<VenueRecord> searchVenues(String city, String locality, String sportType,
                                          Long minPricePaise, Long maxPricePaise,
                                          Boolean hasActiveOffers, String q, int page, int size) {
        return firebaseService.getVenues().values().stream()
                .filter(v -> "ACTIVE".equalsIgnoreCase(v.getStatus()))
                .filter(v -> city == null || "all".equalsIgnoreCase(city) || city.equalsIgnoreCase(v.getCity()))
                .filter(v -> locality == null || locality.equalsIgnoreCase(v.getLocality()))
                .filter(v -> {
                    if (sportType == null || "all".equalsIgnoreCase(sportType)) return true;
                    String target = sportType.toUpperCase();
                    return v.getSportTypes().stream().anyMatch(st -> st.equalsIgnoreCase(target) || st.contains(target));
                })
                .filter(v -> minPricePaise == null || v.getMinSlotPricePaise() >= minPricePaise)
                .filter(v -> maxPricePaise == null || v.getMinSlotPricePaise() <= maxPricePaise)
                .filter(v -> hasActiveOffers == null || !hasActiveOffers || v.isHasActiveDiscounts())
                .filter(v -> {
                    if (q == null || q.trim().isEmpty()) return true;
                    String term = q.trim().toLowerCase();
                    return v.getName().toLowerCase().contains(term) ||
                            v.getLocality().toLowerCase().contains(term) ||
                            v.getCity().toLowerCase().contains(term) ||
                            v.getSportTypes().stream().anyMatch(s -> s.toLowerCase().contains(term));
                })
                .sorted(Comparator.comparingDouble(VenueRecord::getRating).reversed())
                .skip((long) page * size)
                .limit(size)
                .collect(Collectors.toList());
    }

    public List<VenueRecord> getVenues(String city, String sport) {
        return searchVenues(city, null, sport, null, null, null, null, 0, 50);
    }

    public VenueRecord getVenueById(String venueId) {
        VenueRecord v = firebaseService.getVenues().get(venueId);
        if (v == null) {
            throw new IllegalArgumentException("Venue not found: " + venueId);
        }
        return v;
    }

    public List<ResourceRecord> getResourcesForVenue(String venueId) {
        return firebaseService.getResources().values().stream()
                .filter(r -> venueId.equals(r.getVenueId()) && r.isActive())
                .collect(Collectors.toList());
    }

    public List<FacilityRecord> getCourtsForVenue(String venueId) {
        return firebaseService.getCourts().values().stream()
                .filter(c -> venueId.equals(c.getVenueId()))
                .toList();
    }

    @CacheEvict(value = "venues", allEntries = true)
    public synchronized VenueRecord createVenue(String ownerId, CreateVenueRequest req) {
        String slug = req.getName().toLowerCase().replaceAll("[^a-z0-9]", "_");
        String venueId = "venue_" + slug + "_" + (100 + (int) (Math.random() * 900));

        VenueRecord v = new VenueRecord(venueId, ownerId, req.getName(), req.getLocality(), req.getCity());
        v.setDescription(req.getDescription());
        v.setAddressLine(req.getAddressLine());
        v.setState(req.getState());
        v.setPincode(req.getPincode());
        v.setLatitude(req.getLatitude());
        v.setLongitude(req.getLongitude());
        v.setContactPhone(req.getContactPhone());
        v.setAmenities(req.getAmenities());
        v.setSportTypes(req.getSportTypes());
        v.setMinSlotPricePaise(req.getMinSlotPricePaise());
        v.setImageUrls(req.getImageUrls());
        if (req.getCancellationPolicy() != null && !req.getCancellationPolicy().isEmpty()) {
            v.setCancellationPolicy(req.getCancellationPolicy());
        }
        v.setStatus("ACTIVE");
        v.setCreatedAt(System.currentTimeMillis());
        v.setUpdatedAt(System.currentTimeMillis());

        firebaseService.getVenues().put(venueId, v);
        return v;
    }

    @CacheEvict(value = "venues", allEntries = true)
    public synchronized VenueRecord updateVenue(String venueId, String ownerId, CreateVenueRequest req) {
        VenueRecord v = getVenueById(venueId);
        if (!v.getOwnerId().equals(ownerId) && !"admin".equalsIgnoreCase(ownerId)) {
            throw new IllegalStateException("Unauthorized to modify this venue.");
        }

        v.setName(req.getName());
        v.setDescription(req.getDescription());
        v.setAddressLine(req.getAddressLine());
        v.setLocality(req.getLocality());
        v.setCity(req.getCity());
        v.setAmenities(req.getAmenities());
        v.setSportTypes(req.getSportTypes());
        v.setMinSlotPricePaise(req.getMinSlotPricePaise());
        v.setImageUrls(req.getImageUrls());
        if (req.getCancellationPolicy() != null) {
            v.setCancellationPolicy(req.getCancellationPolicy());
        }
        v.setUpdatedAt(System.currentTimeMillis());
        return v;
    }

    @CacheEvict(value = "resources", allEntries = true)
    public synchronized ResourceRecord createResource(String ownerId, String venueId, CreateResourceRequest req) {
        VenueRecord venue = getVenueById(venueId);
        if (!venue.getOwnerId().equals(ownerId) && !"admin".equalsIgnoreCase(ownerId)) {
            throw new IllegalStateException("Unauthorized to add resource to this venue.");
        }

        String resSlug = req.getName().toLowerCase().replaceAll("[^a-z0-9]", "_");
        String resourceId = "res_" + resSlug + "_" + (100 + (int) (Math.random() * 900));

        ResourceRecord r = new ResourceRecord(resourceId, venueId, ownerId, req.getName(), req.getSportTypes(), req.getBasePricePaise());
        r.setSlotDurationMinutes(req.getSlotDurationMinutes());
        r.setWeekendPricePaise(req.getWeekendPricePaise());
        r.setPeakHourPricePaise(req.getPeakHourPricePaise());
        r.setMaxPlayers(req.getMaxPlayers());
        r.setSurfaceType(req.getSurfaceType());
        r.setIndoor(req.isIndoor());
        r.setActive(true);
        r.setCreatedAt(System.currentTimeMillis());

        firebaseService.getResources().put(resourceId, r);

        // Attach default schedule rule
        ScheduleRuleRecord rule = new ScheduleRuleRecord(resourceId, venueId, ownerId);
        firebaseService.getScheduleRules().put(resourceId, rule);

        // Update venue's sports union
        Set<String> sportsSet = new HashSet<>(venue.getSportTypes());
        sportsSet.addAll(req.getSportTypes());
        venue.setSportTypes(new ArrayList<>(sportsSet));

        return r;
    }

    public synchronized ResourceRecord updateResource(String resourceId, String ownerId, CreateResourceRequest req) {
        ResourceRecord r = firebaseService.getResources().get(resourceId);
        if (r == null) {
            throw new IllegalArgumentException("Resource not found: " + resourceId);
        }
        if (!r.getOwnerId().equals(ownerId) && !"admin".equalsIgnoreCase(ownerId)) {
            throw new IllegalStateException("Unauthorized to update this resource.");
        }

        r.setName(req.getName());
        r.setSportTypes(req.getSportTypes());
        r.setSlotDurationMinutes(req.getSlotDurationMinutes());
        r.setBasePricePaise(req.getBasePricePaise());
        r.setWeekendPricePaise(req.getWeekendPricePaise());
        r.setPeakHourPricePaise(req.getPeakHourPricePaise());
        r.setMaxPlayers(req.getMaxPlayers());
        r.setSurfaceType(req.getSurfaceType());
        r.setIndoor(req.isIndoor());
        r.setUpdatedAt(System.currentTimeMillis());
        return r;
    }

    public synchronized ScheduleRuleRecord updateScheduleRule(String resourceId, String ownerId, ScheduleRuleRecord updated) {
        ResourceRecord r = firebaseService.getResources().get(resourceId);
        if (r == null) {
            throw new IllegalArgumentException("Resource not found: " + resourceId);
        }
        if (!r.getOwnerId().equals(ownerId) && !"admin".equalsIgnoreCase(ownerId)) {
            throw new IllegalStateException("Unauthorized");
        }

        updated.setResourceId(resourceId);
        updated.setVenueId(r.getVenueId());
        updated.setOwnerId(ownerId);
        updated.setUpdatedAt(System.currentTimeMillis());
        firebaseService.getScheduleRules().put(resourceId, updated);
        return updated;
    }

    public synchronized DateOverrideRecord updateDateOverride(String resourceId, String ownerId, String date, DateOverrideRequest req) {
        ResourceRecord r = firebaseService.getResources().get(resourceId);
        if (r == null) {
            throw new IllegalArgumentException("Resource not found: " + resourceId);
        }
        if (!r.getOwnerId().equals(ownerId) && !"admin".equalsIgnoreCase(ownerId)) {
            throw new IllegalStateException("Unauthorized");
        }

        String overrideId = resourceId + "_" + date;
        DateOverrideRecord rec = new DateOverrideRecord(resourceId, r.getVenueId(), date);
        rec.setClosedFullDay(req.isClosedFullDay());
        rec.setClosureReason(req.getClosureReason());
        rec.setCustomOpenTime(req.getCustomOpenTime());
        rec.setCustomCloseTime(req.getCustomCloseTime());
        rec.setBlockedSlots(req.getBlockedSlots());
        rec.setSlotPriceOverrides(req.getSlotPriceOverrides());
        rec.setUpdatedAt(System.currentTimeMillis());

        firebaseService.getDateOverrides().put(overrideId, rec);
        return rec;
    }

    public List<VenueRecord> getOwnerVenues(String ownerId) {
        return firebaseService.getVenues().values().stream()
                .filter(v -> ownerId.equals(v.getOwnerId()) || "admin".equalsIgnoreCase(ownerId))
                .sorted(Comparator.comparing(VenueRecord::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    // Compatibility methods
    public synchronized VenueRecord createVenue(VenueRecord venue) {
        if (venue.getVenueId() == null) {
            String slug = venue.getName().toLowerCase().replaceAll("[^a-z0-9]", "_");
            venue.setVenueId("venue_" + slug + "_" + (100 + (int) (Math.random() * 900)));
        }
        venue.setCreatedAt(System.currentTimeMillis());
        venue.setStatus("ACTIVE");
        firebaseService.getVenues().put(venue.getVenueId(), venue);
        return venue;
    }

    public synchronized FacilityRecord createFacility(String ownerId, CreateFacilityRequest req) {
        VenueRecord venue = firebaseService.getVenues().get(req.getVenueId());
        if (venue == null) {
            throw new IllegalArgumentException("Venue ID not found: " + req.getVenueId());
        }

        String courtSlug = req.getName().toLowerCase().replaceAll("[^a-z0-9]", "_");
        String courtId = "court_" + courtSlug + "_" + (100 + (int) (Math.random() * 900));

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
        return facility;
    }

    public synchronized AvailabilityDoc getDailyAvailability(String courtId, String date) {
        String docId = courtId + "_" + date;
        AvailabilityDoc doc = firebaseService.getAvailability().get(docId);
        if (doc != null) return doc;

        FacilityRecord court = firebaseService.getCourts().get(courtId);
        if (court == null) {
            throw new IllegalArgumentException("Court not found: " + courtId);
        }

        doc = new AvailabilityDoc();
        doc.setDocId(docId);
        doc.setCourtId(courtId);
        doc.setVenueId(court.getVenueId());
        doc.setDate(date);

        Map<String, SlotInfo> slots = new HashMap<>();
        for (int h = 6; h < 23; h++) {
            String slotTime = String.format("%02d:00", h);
            SlotInfo slot = new SlotInfo(slotTime, court.getBaseHourlyRate(), false, 0, "Standard");
            slots.put(slotTime, slot);
        }
        doc.setSlots(slots);
        firebaseService.getAvailability().put(docId, doc);
        return doc;
    }
}

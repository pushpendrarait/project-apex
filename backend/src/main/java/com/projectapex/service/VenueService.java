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
        firebaseService.persistVenue(v);
        return v;
    }

    @CacheEvict(value = {"venues", "resources"}, allEntries = true)
    public synchronized boolean deleteVenue(String venueId, String ownerId, boolean isAdmin) {
        VenueRecord v = getVenueById(venueId);
        if (!v.getOwnerId().equals(ownerId) && !isAdmin && !"admin".equalsIgnoreCase(ownerId)) {
            throw new IllegalStateException("Unauthorized to delete this venue.");
        }
        String todayIst = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Kolkata")).toString();
        boolean hasActiveBooking = firebaseService.getBookings().values().stream()
                .anyMatch(b -> venueId.equals(b.getVenueId())
                        && ("CONFIRMED".equalsIgnoreCase(b.getStatus())
                            || "PENDING_PAYMENT".equalsIgnoreCase(b.getStatus())
                            || "CHECKED_IN".equalsIgnoreCase(b.getStatus()))
                        && (b.getBookingDate() == null || b.getBookingDate().compareTo(todayIst) >= 0));
        if (hasActiveBooking) {
            throw new IllegalStateException("Conflict: Cannot delete venue while active or upcoming bookings exist.");
        }

        List<String> resIds = firebaseService.getResources().values().stream()
                .filter(r -> venueId.equals(r.getVenueId()))
                .map(ResourceRecord::getResourceId)
                .toList();
        for (String rid : resIds) {
            firebaseService.getResources().remove(rid);
            firebaseService.getScheduleRules().remove(rid);
        }

        List<String> courtIds = firebaseService.getCourts().values().stream()
                .filter(c -> venueId.equals(c.getVenueId()))
                .map(FacilityRecord::getCourtId)
                .toList();
        for (String cid : courtIds) {
            firebaseService.getCourts().remove(cid);
        }

        firebaseService.getVenues().remove(venueId);
        firebaseService.getAuditLogs().add(new AuditLog(
                "audit_ven_del_" + System.currentTimeMillis(), "VENUE_DELETED", ownerId, "venue", venueId,
                Map.of("name", v.getName())));
        return true;
    }

    @CacheEvict(value = {"venues", "resources"}, allEntries = true)
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
        venue.setUpdatedAt(System.currentTimeMillis());
        firebaseService.persistVenue(venue);

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
        firebaseService.persistResource(r);
        return r;
    }

    @CacheEvict(value = {"venues", "resources"}, allEntries = true)
    public synchronized boolean deleteResource(String resourceId, String ownerId, boolean isAdmin) {
        ResourceRecord r = firebaseService.getResources().get(resourceId);
        if (r == null) {
            throw new IllegalArgumentException("Resource not found: " + resourceId);
        }
        if (!r.getOwnerId().equals(ownerId) && !isAdmin && !"admin".equalsIgnoreCase(ownerId)) {
            throw new IllegalStateException("Unauthorized to delete this resource.");
        }
        boolean hasActiveBooking = firebaseService.getBookings().values().stream()
                .anyMatch(b -> resourceId.equals(b.getResourceId())
                        && ("CONFIRMED".equalsIgnoreCase(b.getStatus())
                            || "PENDING_PAYMENT".equalsIgnoreCase(b.getStatus())
                            || "CHECKED_IN".equalsIgnoreCase(b.getStatus())));
        if (hasActiveBooking) {
            throw new IllegalStateException("Conflict: Cannot delete resource with active bookings.");
        }
        firebaseService.getResources().remove(resourceId);
        firebaseService.getScheduleRules().remove(resourceId);
        return true;
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

    @CacheEvict(value = {"venues", "resources"}, allEntries = true)
    public synchronized FacilityRecord createFacility(String ownerId, CreateFacilityRequest req) {
        VenueRecord venue = firebaseService.getVenues().get(req.getVenueId());
        if (venue == null) {
            throw new IllegalArgumentException("Venue ID not found: " + req.getVenueId());
        }
        if (ownerId != null && !"admin".equalsIgnoreCase(ownerId) && !ownerId.equals(venue.getOwnerId())) {
            throw new IllegalStateException("Unauthorized to add facility to this venue.");
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

        // Keep ResourceRecord + ScheduleRuleRecord synced for /api/v1/availability & booking engine
        ResourceRecord res = new ResourceRecord(
                courtId,
                venue.getVenueId(),
                venue.getOwnerId(),
                facility.getName(),
                List.of(facility.getSport().toUpperCase()),
                (long) facility.getBaseHourlyRate() * 100L
        );
        res.setPeakHourPricePaise((long) facility.getPeakHourlyRate() * 100L);
        res.setMaxPlayers(facility.getCapacity());
        res.setSurfaceType(facility.getSurface());
        res.setIndoor(facility.getFacilityType() != null && facility.getFacilityType().toLowerCase().contains("indoor"));
        firebaseService.getResources().put(res.getResourceId(), res);
        firebaseService.getScheduleRules().put(res.getResourceId(), new ScheduleRuleRecord(res.getResourceId(), venue.getVenueId(), venue.getOwnerId()));

        firebaseService.getAuditLogs().add(new AuditLog(
                "audit_fac_add_" + System.currentTimeMillis(), "FACILITY_CREATED", ownerId, "facility", courtId,
                Map.of("venueId", venue.getVenueId(), "name", facility.getName(), "baseRate", facility.getBaseHourlyRate())));
        return facility;
    }

    public FacilityRecord getFacility(String courtId) {
        FacilityRecord f = firebaseService.getCourts().get(courtId);
        if (f == null) {
            throw new IllegalArgumentException("Facility not found: " + courtId);
        }
        return f;
    }

    @CacheEvict(value = {"venues", "resources"}, allEntries = true)
    public synchronized FacilityRecord updateFacility(String courtId, String ownerId, CreateFacilityRequest req, boolean isAdmin) {
        FacilityRecord f = getFacility(courtId);
        VenueRecord venue = firebaseService.getVenues().get(f.getVenueId());
        if (venue != null && !venue.getOwnerId().equals(ownerId) && !isAdmin && !"admin".equalsIgnoreCase(ownerId)) {
            throw new IllegalStateException("Unauthorized to update this facility.");
        }

        if (req.getName() != null) f.setName(req.getName());
        if (req.getSport() != null) f.setSport(req.getSport().toLowerCase());
        if (req.getFacilityType() != null) f.setFacilityType(req.getFacilityType());
        if (req.getCapacity() != null) f.setCapacity(req.getCapacity());
        if (req.getSurface() != null) f.setSurface(req.getSurface());
        if (req.getDimensions() != null) f.setDimensions(req.getDimensions());
        if (req.getBaseHourlyRate() != null) f.setBaseHourlyRate(req.getBaseHourlyRate());
        if (req.getPeakHourlyRate() != null) f.setPeakHourlyRate(req.getPeakHourlyRate());
        if (req.getOddHourDiscountPct() != null) f.setOddHourDiscountPct(req.getOddHourDiscountPct());
        if (req.getHasFloodlights() != null) f.setHasFloodlights(req.getHasFloodlights());
        if (req.getAmenities() != null) f.setAmenities(req.getAmenities());
        firebaseService.persistFacility(f);

        ResourceRecord res = firebaseService.getResources().get(courtId);
        if (res != null) {
            res.setName(f.getName());
            res.setSportTypes(List.of(f.getSport().toUpperCase()));
            res.setBasePricePaise((long) f.getBaseHourlyRate() * 100L);
            res.setPeakHourPricePaise((long) f.getPeakHourlyRate() * 100L);
            res.setMaxPlayers(f.getCapacity());
            res.setSurfaceType(f.getSurface());
            res.setUpdatedAt(System.currentTimeMillis());
            firebaseService.persistResource(res);
        }

        firebaseService.getAuditLogs().add(new AuditLog(
                "audit_fac_upd_" + System.currentTimeMillis(), "FACILITY_UPDATED", ownerId, "facility", courtId,
                Map.of("name", f.getName(), "baseRate", f.getBaseHourlyRate())));
        return f;
    }

    @CacheEvict(value = {"venues", "resources"}, allEntries = true)
    public synchronized boolean deleteFacility(String courtId, String ownerId, boolean isAdmin) {
        FacilityRecord f = getFacility(courtId);
        VenueRecord venue = firebaseService.getVenues().get(f.getVenueId());
        if (venue != null && !venue.getOwnerId().equals(ownerId) && !isAdmin && !"admin".equalsIgnoreCase(ownerId)) {
            throw new IllegalStateException("Unauthorized to delete this facility.");
        }
        boolean hasActiveBooking = firebaseService.getBookings().values().stream()
                .anyMatch(b -> courtId.equals(b.getResourceId())
                        && ("CONFIRMED".equalsIgnoreCase(b.getStatus())
                            || "PENDING_PAYMENT".equalsIgnoreCase(b.getStatus())
                            || "CHECKED_IN".equalsIgnoreCase(b.getStatus())));
        if (hasActiveBooking) {
            throw new IllegalStateException("Conflict: Cannot delete facility while active bookings exist.");
        }

        firebaseService.getCourts().remove(courtId);
        if (firebaseService.getResources().containsKey(courtId)) {
            firebaseService.getResources().remove(courtId);
            firebaseService.getScheduleRules().remove(courtId);
        }
        firebaseService.getAuditLogs().add(new AuditLog(
                "audit_fac_del_" + System.currentTimeMillis(), "FACILITY_DELETED", ownerId, "facility", courtId,
                Map.of("venueId", f.getVenueId())));
        return true;
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

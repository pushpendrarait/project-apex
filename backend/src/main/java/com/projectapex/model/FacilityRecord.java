package com.projectapex.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class FacilityRecord {
    private String courtId;
    private String venueId;
    private String name;
    private String sport; // "cricket", "football", "badminton", etc.
    private String facilityType; // "Box Cricket", "Turf Football", "Indoor Badminton"
    private Integer capacity; // Max player capacity (e.g. 16 for cricket, 10 for football, 4 for badminton)
    private String surface; // "15mm Monofilament Synthetic Turf", "BWF Approved Teakwood"
    private String dimensions; // "115ft x 65ft"
    private Integer baseHourlyRate;
    private Integer peakHourlyRate;
    private Integer oddHourDiscountPct;
    private Boolean hasFloodlights;
    private List<String> amenities;
    private String status; // "ACTIVE"
    private Long createdAt;

    public FacilityRecord() {}

    public String getCourtId() { return courtId; }
    public void setCourtId(String courtId) { this.courtId = courtId; }

    public String getVenueId() { return venueId; }
    public void setVenueId(String venueId) { this.venueId = venueId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSport() { return sport; }
    public void setSport(String sport) { this.sport = sport; }

    public String getFacilityType() { return facilityType; }
    public void setFacilityType(String facilityType) { this.facilityType = facilityType; }

    public Integer getCapacity() { return capacity != null ? capacity : 10; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public String getSurface() { return surface; }
    public void setSurface(String surface) { this.surface = surface; }

    public String getDimensions() { return dimensions; }
    public void setDimensions(String dimensions) { this.dimensions = dimensions; }

    public Integer getBaseHourlyRate() { return baseHourlyRate != null ? baseHourlyRate : 1000; }
    public void setBaseHourlyRate(Integer baseHourlyRate) { this.baseHourlyRate = baseHourlyRate; }

    public Integer getPeakHourlyRate() { return peakHourlyRate != null ? peakHourlyRate : 1400; }
    public void setPeakHourlyRate(Integer peakHourlyRate) { this.peakHourlyRate = peakHourlyRate; }

    public Integer getOddHourDiscountPct() { return oddHourDiscountPct != null ? oddHourDiscountPct : 30; }
    public void setOddHourDiscountPct(Integer oddHourDiscountPct) { this.oddHourDiscountPct = oddHourDiscountPct; }

    public Boolean getHasFloodlights() { return hasFloodlights != null ? hasFloodlights : true; }
    public void setHasFloodlights(Boolean hasFloodlights) { this.hasFloodlights = hasFloodlights; }

    public List<String> getAmenities() { return amenities; }
    public void setAmenities(List<String> amenities) { this.amenities = amenities; }

    public String getStatus() { return status != null ? status : "ACTIVE"; }
    public void setStatus(String status) { this.status = status; }

    public Long getCreatedAt() { return createdAt; }
    public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }
}

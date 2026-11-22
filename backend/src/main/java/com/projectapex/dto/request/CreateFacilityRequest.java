package com.projectapex.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class CreateFacilityRequest {
    @NotBlank(message = "Venue ID is required")
    private String venueId;

    @NotBlank(message = "Facility name is required")
    private String name;

    @NotBlank(message = "Sport / Facility Type is required (e.g. cricket, football, badminton)")
    private String sport;

    private String facilityType;

    @NotNull(message = "Player capacity is required")
    @Min(value = 1, message = "Capacity must be at least 1 player")
    private Integer capacity;

    private String surface;
    private String dimensions;

    @NotNull(message = "Base hourly rate is required")
    @Min(value = 200, message = "Base rate must be at least ₹200")
    private Integer baseHourlyRate;

    private Integer peakHourlyRate;
    private Integer oddHourDiscountPct;
    private Boolean hasFloodlights;
    private List<String> amenities;

    public CreateFacilityRequest() {}

    public String getVenueId() { return venueId; }
    public void setVenueId(String venueId) { this.venueId = venueId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSport() { return sport; }
    public void setSport(String sport) { this.sport = sport; }

    public String getFacilityType() { return facilityType != null ? facilityType : sport; }
    public void setFacilityType(String facilityType) { this.facilityType = facilityType; }

    public Integer getCapacity() { return capacity != null ? capacity : 10; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public String getSurface() { return surface; }
    public void setSurface(String surface) { this.surface = surface; }

    public String getDimensions() { return dimensions; }
    public void setDimensions(String dimensions) { this.dimensions = dimensions; }

    public Integer getBaseHourlyRate() { return baseHourlyRate; }
    public void setBaseHourlyRate(Integer baseHourlyRate) { this.baseHourlyRate = baseHourlyRate; }

    public Integer getPeakHourlyRate() { return peakHourlyRate != null ? peakHourlyRate : (int)(baseHourlyRate * 1.3); }
    public void setPeakHourlyRate(Integer peakHourlyRate) { this.peakHourlyRate = peakHourlyRate; }

    public Integer getOddHourDiscountPct() { return oddHourDiscountPct != null ? oddHourDiscountPct : 30; }
    public void setOddHourDiscountPct(Integer oddHourDiscountPct) { this.oddHourDiscountPct = oddHourDiscountPct; }

    public Boolean getHasFloodlights() { return hasFloodlights != null ? hasFloodlights : true; }
    public void setHasFloodlights(Boolean hasFloodlights) { this.hasFloodlights = hasFloodlights; }

    public List<String> getAmenities() { return amenities; }
    public void setAmenities(List<String> amenities) { this.amenities = amenities; }
}

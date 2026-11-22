package com.projectapex.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.ArrayList;
import java.util.List;

public class InitiateBookingRequest {
    @NotBlank(message = "Venue ID is required")
    private String venueId;

    @NotBlank(message = "Resource ID is required")
    private String resourceId;

    @NotBlank(message = "Booking Date is required (YYYY-MM-DD)")
    private String bookingDate;

    @NotEmpty(message = "At least one slot start time is required")
    private List<String> slotStartTimes = new ArrayList<>();

    private String sportType;
    private String promoCode;

    public InitiateBookingRequest() {
    }

    public String getVenueId() {
        return venueId;
    }

    public void setVenueId(String venueId) {
        this.venueId = venueId;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(String bookingDate) {
        this.bookingDate = bookingDate;
    }

    public List<String> getSlotStartTimes() {
        return slotStartTimes;
    }

    public void setSlotStartTimes(List<String> slotStartTimes) {
        this.slotStartTimes = slotStartTimes != null ? slotStartTimes : new ArrayList<>();
    }

    public String getSportType() {
        return sportType;
    }

    public void setSportType(String sportType) {
        this.sportType = sportType;
    }

    public String getPromoCode() {
        return promoCode;
    }

    public void setPromoCode(String promoCode) {
        this.promoCode = promoCode;
    }
}

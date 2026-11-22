package com.projectapex.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SlotInfo {
    private String slotTime; // "14:00"
    private String status; // "AVAILABLE", "HELD", "BOOKED"
    private Integer basePrice;
    private Integer effectivePrice;
    private Boolean isOddHour;
    private Integer discountPct;
    private String bandName;
    private String bookingId;
    private String userId;

    public SlotInfo() {}

    public SlotInfo(String slotTime, Integer basePrice, boolean isOddHour, int discountPct, String bandName) {
        this.slotTime = slotTime;
        this.status = "AVAILABLE";
        this.basePrice = basePrice;
        this.isOddHour = isOddHour;
        this.discountPct = discountPct;
        this.bandName = bandName;
        this.effectivePrice = isOddHour ? Math.round(basePrice * (1.0f - (discountPct / 100.0f))) : basePrice;
    }

    public String getSlotTime() { return slotTime; }
    public void setSlotTime(String slotTime) { this.slotTime = slotTime; }

    public String getStatus() { return status != null ? status : "AVAILABLE"; }
    public void setStatus(String status) { this.status = status; }

    public Integer getBasePrice() { return basePrice; }
    public void setBasePrice(Integer basePrice) { this.basePrice = basePrice; }

    public Integer getEffectivePrice() { return effectivePrice; }
    public void setEffectivePrice(Integer effectivePrice) { this.effectivePrice = effectivePrice; }

    public Boolean getIsOddHour() { return isOddHour; }
    public void setIsOddHour(Boolean isOddHour) { this.isOddHour = isOddHour; }

    public Integer getDiscountPct() { return discountPct; }
    public void setDiscountPct(Integer discountPct) { this.discountPct = discountPct; }

    public String getBandName() { return bandName; }
    public void setBandName(String bandName) { this.bandName = bandName; }

    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
}

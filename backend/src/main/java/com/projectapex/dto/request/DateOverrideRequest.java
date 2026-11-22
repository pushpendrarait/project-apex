package com.projectapex.dto.request;

import com.projectapex.model.DateOverrideRecord;

import java.util.HashMap;
import java.util.Map;

public class DateOverrideRequest {
    private boolean isClosedFullDay = false;
    private String closureReason;
    private String customOpenTime;
    private String customCloseTime;
    private Map<String, DateOverrideRecord.BlockedSlotInfo> blockedSlots = new HashMap<>();
    private Map<String, Long> slotPriceOverrides = new HashMap<>();

    public DateOverrideRequest() {
    }

    public boolean isClosedFullDay() {
        return isClosedFullDay;
    }

    public void setClosedFullDay(boolean closedFullDay) {
        isClosedFullDay = closedFullDay;
    }

    public String getClosureReason() {
        return closureReason;
    }

    public void setClosureReason(String closureReason) {
        this.closureReason = closureReason;
    }

    public String getCustomOpenTime() {
        return customOpenTime;
    }

    public void setCustomOpenTime(String customOpenTime) {
        this.customOpenTime = customOpenTime;
    }

    public String getCustomCloseTime() {
        return customCloseTime;
    }

    public void setCustomCloseTime(String customCloseTime) {
        this.customCloseTime = customCloseTime;
    }

    public Map<String, DateOverrideRecord.BlockedSlotInfo> getBlockedSlots() {
        return blockedSlots;
    }

    public void setBlockedSlots(Map<String, DateOverrideRecord.BlockedSlotInfo> blockedSlots) {
        this.blockedSlots = blockedSlots != null ? blockedSlots : new HashMap<>();
    }

    public Map<String, Long> getSlotPriceOverrides() {
        return slotPriceOverrides;
    }

    public void setSlotPriceOverrides(Map<String, Long> slotPriceOverrides) {
        this.slotPriceOverrides = slotPriceOverrides != null ? slotPriceOverrides : new HashMap<>();
    }
}

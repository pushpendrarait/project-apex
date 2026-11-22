package com.projectapex.model;

import java.util.HashMap;
import java.util.Map;

public class DateOverrideRecord {
    private String overrideId; // deterministic: {resourceId}_{YYYY-MM-DD}
    private String resourceId;
    private String venueId;
    private String date; // YYYY-MM-DD
    private boolean isClosedFullDay = false;
    private String closureReason;
    private String customOpenTime;
    private String customCloseTime;
    private Map<String, BlockedSlotInfo> blockedSlots = new HashMap<>(); // Key: "HH:mm"
    private Map<String, Long> slotPriceOverrides = new HashMap<>(); // Key: "HH:mm" -> pricePaise
    private long updatedAt = System.currentTimeMillis();

    public DateOverrideRecord() {
    }

    public DateOverrideRecord(String resourceId, String venueId, String date) {
        this.resourceId = resourceId;
        this.venueId = venueId;
        this.date = date;
        this.overrideId = resourceId + "_" + date;
    }

    public String getOverrideId() {
        return overrideId;
    }

    public void setOverrideId(String overrideId) {
        this.overrideId = overrideId;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getVenueId() {
        return venueId;
    }

    public void setVenueId(String venueId) {
        this.venueId = venueId;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
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

    public Map<String, BlockedSlotInfo> getBlockedSlots() {
        return blockedSlots;
    }

    public void setBlockedSlots(Map<String, BlockedSlotInfo> blockedSlots) {
        this.blockedSlots = blockedSlots != null ? blockedSlots : new HashMap<>();
    }

    public Map<String, Long> getSlotPriceOverrides() {
        return slotPriceOverrides;
    }

    public void setSlotPriceOverrides(Map<String, Long> slotPriceOverrides) {
        this.slotPriceOverrides = slotPriceOverrides != null ? slotPriceOverrides : new HashMap<>();
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public static class BlockedSlotInfo {
        private String type = "MAINTENANCE"; // MAINTENANCE, OFFLINE_BOOKING
        private String reason;
        private String offlineCustomerName;
        private String offlineCustomerPhone;
        private Long offlineAmountPaise;

        public BlockedSlotInfo() {
        }

        public BlockedSlotInfo(String type, String reason) {
            this.type = type;
            this.reason = reason;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }

        public String getOfflineCustomerName() {
            return offlineCustomerName;
        }

        public void setOfflineCustomerName(String offlineCustomerName) {
            this.offlineCustomerName = offlineCustomerName;
        }

        public String getOfflineCustomerPhone() {
            return offlineCustomerPhone;
        }

        public void setOfflineCustomerPhone(String offlineCustomerPhone) {
            this.offlineCustomerPhone = offlineCustomerPhone;
        }

        public Long getOfflineAmountPaise() {
            return offlineAmountPaise;
        }

        public void setOfflineAmountPaise(Long offlineAmountPaise) {
            this.offlineAmountPaise = offlineAmountPaise;
        }
    }
}

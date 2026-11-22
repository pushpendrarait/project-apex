package com.projectapex.model;

public class SlotAvailability {
    private String resourceId;
    private String date; // YYYY-MM-DD
    private String startTime; // HH:mm
    private String endTime; // HH:mm
    private long originalPricePaise;
    private long finalPricePaise;
    private String status = "AVAILABLE"; // AVAILABLE, HELD, BOOKED, BLOCKED, PAST
    private boolean isPeak = false;
    private boolean isWeekend = false;
    private String discountInfo;
    private String blockedType; // MAINTENANCE, OFFLINE_BOOKING
    private String blockedReason;

    public SlotAvailability() {
    }

    public SlotAvailability(String resourceId, String date, String startTime, String endTime, long originalPricePaise, long finalPricePaise, String status) {
        this.resourceId = resourceId;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.originalPricePaise = originalPricePaise;
        this.finalPricePaise = finalPricePaise;
        this.status = status;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public long getOriginalPricePaise() {
        return originalPricePaise;
    }

    public void setOriginalPricePaise(long originalPricePaise) {
        this.originalPricePaise = originalPricePaise;
    }

    public long getFinalPricePaise() {
        return finalPricePaise;
    }

    public void setFinalPricePaise(long finalPricePaise) {
        this.finalPricePaise = finalPricePaise;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isPeak() {
        return isPeak;
    }

    public void setPeak(boolean peak) {
        isPeak = peak;
    }

    public boolean isWeekend() {
        return isWeekend;
    }

    public void setWeekend(boolean weekend) {
        isWeekend = weekend;
    }

    public String getDiscountInfo() {
        return discountInfo;
    }

    public void setDiscountInfo(String discountInfo) {
        this.discountInfo = discountInfo;
    }

    public String getBlockedType() {
        return blockedType;
    }

    public void setBlockedType(String blockedType) {
        this.blockedType = blockedType;
    }

    public String getBlockedReason() {
        return blockedReason;
    }

    public void setBlockedReason(String blockedReason) {
        this.blockedReason = blockedReason;
    }
}

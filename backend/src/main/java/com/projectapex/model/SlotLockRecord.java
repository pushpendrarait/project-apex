package com.projectapex.model;

public class SlotLockRecord {
    private String lockId; // deterministic: {resourceId}_{YYYY-MM-DD}_{HHmm}
    private String resourceId;
    private String venueId;
    private String date; // YYYY-MM-DD
    private String startTime; // HH:mm
    private String endTime; // HH:mm
    private String status = "HELD"; // HELD, BOOKED
    private String bookingId;
    private String userId;
    private Long holdExpiresAt; // now + 10 mins (null when BOOKED)
    private long createdAt = System.currentTimeMillis();

    public SlotLockRecord() {
    }

    public SlotLockRecord(String resourceId, String venueId, String date, String startTime, String endTime, String bookingId, String userId, long holdDurationMs) {
        this.resourceId = resourceId;
        this.venueId = venueId;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.bookingId = bookingId;
        this.userId = userId;
        this.status = "HELD";
        this.holdExpiresAt = System.currentTimeMillis() + holdDurationMs;
        this.createdAt = System.currentTimeMillis();
        this.lockId = buildLockId(resourceId, date, startTime);
    }

    public static String buildLockId(String resourceId, String date, String startTime) {
        String cleanTime = startTime.replace(":", "");
        return resourceId + "_" + date + "_" + cleanTime;
    }

    public boolean isExpired() {
        return "HELD".equalsIgnoreCase(status) && holdExpiresAt != null && System.currentTimeMillis() > holdExpiresAt;
    }

    public String getLockId() {
        return lockId;
    }

    public void setLockId(String lockId) {
        this.lockId = lockId;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Long getHoldExpiresAt() {
        return holdExpiresAt;
    }

    public void setHoldExpiresAt(Long holdExpiresAt) {
        this.holdExpiresAt = holdExpiresAt;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}

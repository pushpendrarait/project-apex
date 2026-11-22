package com.projectapex.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConcurrencyLock {
    private String lockKey; // courtId_date_slot
    private String courtId;
    private String date;
    private String slot;
    private String userId;
    private Long acquiredAt;
    private Long expiresAt; // acquiredAt + 300000 (5 mins)
    private String status; // "HELD", "COMMITTED", "RELEASED", "EXPIRED"
    private String bookingId;

    public ConcurrencyLock() {}

    public ConcurrencyLock(String courtId, String date, String slot, String userId, long ttlMs) {
        this.lockKey = courtId + "_" + date + "_" + slot;
        this.courtId = courtId;
        this.date = date;
        this.slot = slot;
        this.userId = userId;
        this.acquiredAt = System.currentTimeMillis();
        this.expiresAt = this.acquiredAt + ttlMs;
        this.status = "HELD";
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > (expiresAt != null ? expiresAt : 0);
    }

    public String getLockKey() { return lockKey; }
    public void setLockKey(String lockKey) { this.lockKey = lockKey; }

    public String getCourtId() { return courtId; }
    public void setCourtId(String courtId) { this.courtId = courtId; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getSlot() { return slot; }
    public void setSlot(String slot) { this.slot = slot; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public Long getAcquiredAt() { return acquiredAt; }
    public void setAcquiredAt(Long acquiredAt) { this.acquiredAt = acquiredAt; }

    public Long getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Long expiresAt) { this.expiresAt = expiresAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }
}

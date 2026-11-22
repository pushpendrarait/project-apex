package com.projectapex.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class BookingRecord {
    private String bookingId;
    private String userId;
    private String userName;
    private String userPhone;
    private String userEmail;
    private String venueId;
    private String venueName;
    private String courtId;
    private String courtName;
    private String sport;
    private String date;
    private String slot;
    private Integer durationMinutes;
    private Map<String, Object> pricing; // baseRate, discountAmount, total
    private Map<String, Object> payment; // method, razorpayOrderId, razorpayPaymentId, status
    private String qrPassToken;
    private String status; // "CONFIRMED", "CHECKED_IN", "CANCELLED"
    private String source; // "online" or "walk_in"
    private Long createdAt;
    private Long checkedInAt;
    private Long cancelledAt;
    private Integer refundAmount;

    public BookingRecord() {}

    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserPhone() { return userPhone; }
    public void setUserPhone(String userPhone) { this.userPhone = userPhone; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getVenueId() { return venueId; }
    public void setVenueId(String venueId) { this.venueId = venueId; }

    public String getVenueName() { return venueName; }
    public void setVenueName(String venueName) { this.venueName = venueName; }

    public String getCourtId() { return courtId; }
    public void setCourtId(String courtId) { this.courtId = courtId; }

    public String getCourtName() { return courtName; }
    public void setCourtName(String courtName) { this.courtName = courtName; }

    public String getSport() { return sport; }
    public void setSport(String sport) { this.sport = sport; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getSlot() { return slot; }
    public void setSlot(String slot) { this.slot = slot; }

    public Integer getDurationMinutes() { return durationMinutes != null ? durationMinutes : 60; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }

    public Map<String, Object> getPricing() { return pricing; }
    public void setPricing(Map<String, Object> pricing) { this.pricing = pricing; }

    public Map<String, Object> getPayment() { return payment; }
    public void setPayment(Map<String, Object> payment) { this.payment = payment; }

    public String getQrPassToken() { return qrPassToken; }
    public void setQrPassToken(String qrPassToken) { this.qrPassToken = qrPassToken; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getSource() { return source != null ? source : "online"; }
    public void setSource(String source) { this.source = source; }

    public Long getCreatedAt() { return createdAt; }
    public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }

    public Long getCheckedInAt() { return checkedInAt; }
    public void setCheckedInAt(Long checkedInAt) { this.checkedInAt = checkedInAt; }

    public Long getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(Long cancelledAt) { this.cancelledAt = cancelledAt; }

    public Integer getRefundAmount() { return refundAmount; }
    public void setRefundAmount(Integer refundAmount) { this.refundAmount = refundAmount; }
}

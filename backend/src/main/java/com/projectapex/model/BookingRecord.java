package com.projectapex.model;

import java.util.ArrayList;
import java.util.List;

public class BookingRecord {
    private String bookingId;
    private String bookingCode; // e.g. ARENA-94K2M
    private String qrPassToken;
    private String userId;
    private String userName;
    private String userPhone;
    private String userEmail;
    private String ownerId;
    private String venueId;
    private String venueName;
    private String venueLocality;
    private String resourceId;
    private String resourceName;
    private String sportType;
    private String bookingDate; // YYYY-MM-DD
    private List<BookedSlotItem> slots = new ArrayList<>();
    private long subtotalPaise;
    private String discountId;
    private String discountCode;
    private long discountAmountPaise = 0;
    private long taxPaise = 0;
    private long finalPayablePaise;
    private String status = "PENDING_PAYMENT"; // PENDING_PAYMENT, CONFIRMED, EXPIRED, PAYMENT_FAILED, CANCELLED_BY_USER, CANCELLED_BY_OWNER, COMPLETED, NO_SHOW, CONFLICT_REFUND_PENDING
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String refundId;
    private Long refundAmountPaise;
    private String refundStatus;
    private String cancellationReason;
    private String notes;
    private Long checkedInAt;
    private Long holdExpiresAt;
    private long createdAt = System.currentTimeMillis();
    private long updatedAt = System.currentTimeMillis();

    public BookingRecord() {
    }

    // Compatibility getters
    public String getId() {
        return bookingId;
    }

    public void setId(String id) {
        this.bookingId = id;
    }

    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public String getBookingCode() {
        return bookingCode;
    }

    public String getQrPassToken() {
        if (qrPassToken != null && !qrPassToken.isBlank()) {
            return qrPassToken;
        }
        return "APEX-PASS-" + (bookingCode != null ? bookingCode : bookingId);
    }

    public void setQrPassToken(String qrPassToken) {
        this.qrPassToken = qrPassToken;
    }

    public Long getCheckedInAt() {
        return checkedInAt;
    }

    public void setCheckedInAt(Long checkedInAt) {
        this.checkedInAt = checkedInAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Long getRefundAmount() {
        return refundAmountPaise != null ? refundAmountPaise / 100 : null;
    }

    public void setBookingCode(String bookingCode) {
        this.bookingCode = bookingCode;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserPhone() {
        return userPhone;
    }

    public void setUserPhone(String userPhone) {
        this.userPhone = userPhone;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getVenueId() {
        return venueId;
    }

    public void setVenueId(String venueId) {
        this.venueId = venueId;
    }

    public String getVenueName() {
        return venueName;
    }

    public void setVenueName(String venueName) {
        this.venueName = venueName;
    }

    public String getVenueLocality() {
        return venueLocality;
    }

    public void setVenueLocality(String venueLocality) {
        this.venueLocality = venueLocality;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public String getSportType() {
        return sportType;
    }

    public void setSportType(String sportType) {
        this.sportType = sportType;
    }

    public String getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(String bookingDate) {
        this.bookingDate = bookingDate;
    }

    public List<BookedSlotItem> getSlots() {
        return slots;
    }

    public void setSlots(List<BookedSlotItem> slots) {
        this.slots = slots != null ? slots : new ArrayList<>();
    }

    public long getSubtotalPaise() {
        return subtotalPaise;
    }

    public void setSubtotalPaise(long subtotalPaise) {
        this.subtotalPaise = subtotalPaise;
    }

    public String getDiscountId() {
        return discountId;
    }

    public void setDiscountId(String discountId) {
        this.discountId = discountId;
    }

    public String getDiscountCode() {
        return discountCode;
    }

    public void setDiscountCode(String discountCode) {
        this.discountCode = discountCode;
    }

    public long getDiscountAmountPaise() {
        return discountAmountPaise;
    }

    public void setDiscountAmountPaise(long discountAmountPaise) {
        this.discountAmountPaise = discountAmountPaise;
    }

    public long getTaxPaise() {
        return taxPaise;
    }

    public void setTaxPaise(long taxPaise) {
        this.taxPaise = taxPaise;
    }

    public long getFinalPayablePaise() {
        return finalPayablePaise;
    }

    public void setFinalPayablePaise(long finalPayablePaise) {
        this.finalPayablePaise = finalPayablePaise;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRazorpayOrderId() {
        return razorpayOrderId;
    }

    public void setRazorpayOrderId(String razorpayOrderId) {
        this.razorpayOrderId = razorpayOrderId;
    }

    public String getRazorpayPaymentId() {
        return razorpayPaymentId;
    }

    public void setRazorpayPaymentId(String razorpayPaymentId) {
        this.razorpayPaymentId = razorpayPaymentId;
    }

    public String getRefundId() {
        return refundId;
    }

    public void setRefundId(String refundId) {
        this.refundId = refundId;
    }

    public Long getRefundAmountPaise() {
        return refundAmountPaise;
    }

    public void setRefundAmountPaise(Long refundAmountPaise) {
        this.refundAmountPaise = refundAmountPaise;
    }

    public String getRefundStatus() {
        return refundStatus;
    }

    public void setRefundStatus(String refundStatus) {
        this.refundStatus = refundStatus;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
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

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public static class BookedSlotItem {
        private String startTime;
        private String endTime;
        private long unitPricePaise;

        public BookedSlotItem() {
        }

        public BookedSlotItem(String startTime, String endTime, long unitPricePaise) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.unitPricePaise = unitPricePaise;
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

        public long getUnitPricePaise() {
            return unitPricePaise;
        }

        public void setUnitPricePaise(long unitPricePaise) {
            this.unitPricePaise = unitPricePaise;
        }
    }
}

package com.projectapex.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class RazorpayOrderResponse {
    private String orderId;
    private String keyId;
    private Long amountInPaise;
    private String currency;
    private String venueName;
    private String courtName;
    private String date;
    private String slot;

    public RazorpayOrderResponse() {}

    public RazorpayOrderResponse(String orderId, String keyId, Long amountInPaise, String currency) {
        this.orderId = orderId;
        this.keyId = keyId;
        this.amountInPaise = amountInPaise;
        this.currency = currency;
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getKeyId() { return keyId; }
    public void setKeyId(String keyId) { this.keyId = keyId; }

    public Long getAmountInPaise() { return amountInPaise; }
    public void setAmountInPaise(Long amountInPaise) { this.amountInPaise = amountInPaise; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getVenueName() { return venueName; }
    public void setVenueName(String venueName) { this.venueName = venueName; }

    public String getCourtName() { return courtName; }
    public void setCourtName(String courtName) { this.courtName = courtName; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getSlot() { return slot; }
    public void setSlot(String slot) { this.slot = slot; }
}

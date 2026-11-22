package com.projectapex.dto.request;

public class CancelBookingRequest {
    private String reason = "Player requested cancellation";

    public CancelBookingRequest() {
    }

    public CancelBookingRequest(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}

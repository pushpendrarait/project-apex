package com.projectapex.dto.request;

import jakarta.validation.constraints.NotBlank;

public class AcquireLockRequest {
    @NotBlank(message = "Court / Facility ID is required")
    private String courtId;

    @NotBlank(message = "Date is required (YYYY-MM-DD)")
    private String date;

    @NotBlank(message = "Slot time is required (e.g. 18:00)")
    private String slot;

    public AcquireLockRequest() {}

    public String getCourtId() { return courtId; }
    public void setCourtId(String courtId) { this.courtId = courtId; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getSlot() { return slot; }
    public void setSlot(String slot) { this.slot = slot; }
}

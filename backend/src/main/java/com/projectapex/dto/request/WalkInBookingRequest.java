package com.projectapex.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class WalkInBookingRequest {
    @NotBlank(message = "Court ID is required")
    private String courtId;

    @NotBlank(message = "Date is required")
    private String date;

    @NotBlank(message = "Slot time is required")
    private String slot;

    @NotBlank(message = "Customer name is required")
    private String customerName;

    private String customerPhone;

    @NotNull(message = "Amount collected is required")
    @Min(value = 100, message = "Amount must be at least ₹100")
    private Integer amountCollected;

    public WalkInBookingRequest() {}

    public String getCourtId() { return courtId; }
    public void setCourtId(String courtId) { this.courtId = courtId; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getSlot() { return slot; }
    public void setSlot(String slot) { this.slot = slot; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public Integer getAmountCollected() { return amountCollected; }
    public void setAmountCollected(Integer amountCollected) { this.amountCollected = amountCollected; }
}

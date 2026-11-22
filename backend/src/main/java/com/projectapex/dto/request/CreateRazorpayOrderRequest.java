package com.projectapex.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateRazorpayOrderRequest {
    @NotBlank(message = "Court ID is required")
    private String courtId;

    @NotBlank(message = "Date is required")
    private String date;

    @NotBlank(message = "Slot time is required")
    private String slot;

    @NotNull(message = "Amount is required")
    @Min(value = 100, message = "Amount must be at least ₹100")
    private Integer amountInRupees;

    private String promoCode;

    public CreateRazorpayOrderRequest() {}

    public String getCourtId() { return courtId; }
    public void setCourtId(String courtId) { this.courtId = courtId; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getSlot() { return slot; }
    public void setSlot(String slot) { this.slot = slot; }

    public Integer getAmountInRupees() { return amountInRupees; }
    public void setAmountInRupees(Integer amountInRupees) { this.amountInRupees = amountInRupees; }

    public String getPromoCode() { return promoCode; }
    public void setPromoCode(String promoCode) { this.promoCode = promoCode; }
}

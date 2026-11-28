package com.projectapex.dto.request;

import java.util.List;

public class UpdateBookingRequest {
    private String bookingDate;
    private String slotTime;
    private List<String> slotStartTimes;
    private String userPhone;
    private String notes;

    public UpdateBookingRequest() {}

    public String getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(String bookingDate) {
        this.bookingDate = bookingDate;
    }

    public String getSlotTime() {
        return slotTime;
    }

    public void setSlotTime(String slotTime) {
        this.slotTime = slotTime;
    }

    public List<String> getSlotStartTimes() {
        return slotStartTimes;
    }

    public void setSlotStartTimes(List<String> slotStartTimes) {
        this.slotStartTimes = slotStartTimes;
    }

    public String getUserPhone() {
        return userPhone;
    }

    public void setUserPhone(String userPhone) {
        this.userPhone = userPhone;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}

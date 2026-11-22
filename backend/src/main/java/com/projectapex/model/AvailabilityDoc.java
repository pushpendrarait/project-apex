package com.projectapex.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AvailabilityDoc {
    private String docId; // courtId_YYYY-MM-DD
    private String courtId;
    private String venueId;
    private String date; // "2026-11-28"
    private Map<String, SlotInfo> slots;
    private Long lastUpdated;

    public AvailabilityDoc() {}

    public AvailabilityDoc(String courtId, String venueId, String date, Map<String, SlotInfo> slots) {
        this.docId = courtId + "_" + date;
        this.courtId = courtId;
        this.venueId = venueId;
        this.date = date;
        this.slots = slots;
        this.lastUpdated = System.currentTimeMillis();
    }

    public String getDocId() { return docId; }
    public void setDocId(String docId) { this.docId = docId; }

    public String getCourtId() { return courtId; }
    public void setCourtId(String courtId) { this.courtId = courtId; }

    public String getVenueId() { return venueId; }
    public void setVenueId(String venueId) { this.venueId = venueId; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public Map<String, SlotInfo> getSlots() { return slots; }
    public void setSlots(Map<String, SlotInfo> slots) { this.slots = slots; }

    public Long getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(Long lastUpdated) { this.lastUpdated = lastUpdated; }
}

package com.projectapex.model;

import java.util.ArrayList;
import java.util.List;

public class ResourceRecord {
    private String resourceId;
    private String venueId;
    private String ownerId;
    private String name;
    private List<String> sportTypes = new ArrayList<>();
    private int slotDurationMinutes = 60; // 30, 60, or 90
    private long basePricePaise = 100000; // e.g. ₹1000 (weekday)
    private long weekendPricePaise = 140000; // e.g. ₹1400 (Sat/Sun)
    private long peakHourPricePaise = 130000; // e.g. ₹1300 (evenings)
    private int maxPlayers = 14;
    private String surfaceType = "ARTIFICIAL_TURF"; // WOODEN, SYNTHETIC, CLAY, FELT_TABLE, etc.
    private boolean isIndoor = false;
    private boolean isActive = true;
    private long createdAt = System.currentTimeMillis();
    private long updatedAt = System.currentTimeMillis();

    public ResourceRecord() {
    }

    public ResourceRecord(String resourceId, String venueId, String ownerId, String name, List<String> sportTypes, long basePricePaise) {
        this.resourceId = resourceId;
        this.venueId = venueId;
        this.ownerId = ownerId;
        this.name = name;
        this.sportTypes = sportTypes != null ? sportTypes : new ArrayList<>();
        this.basePricePaise = basePricePaise;
        this.weekendPricePaise = (long) (basePricePaise * 1.3);
        this.peakHourPricePaise = (long) (basePricePaise * 1.25);
    }

    // Compatibility getters
    public String getId() {
        return resourceId;
    }

    public void setId(String id) {
        this.resourceId = id;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getVenueId() {
        return venueId;
    }

    public void setVenueId(String venueId) {
        this.venueId = venueId;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getSportTypes() {
        return sportTypes;
    }

    public void setSportTypes(List<String> sportTypes) {
        this.sportTypes = sportTypes != null ? sportTypes : new ArrayList<>();
    }

    public int getSlotDurationMinutes() {
        return slotDurationMinutes;
    }

    public void setSlotDurationMinutes(int slotDurationMinutes) {
        this.slotDurationMinutes = slotDurationMinutes;
    }

    public long getBasePricePaise() {
        return basePricePaise;
    }

    public void setBasePricePaise(long basePricePaise) {
        this.basePricePaise = basePricePaise;
    }

    public long getWeekendPricePaise() {
        return weekendPricePaise;
    }

    public void setWeekendPricePaise(long weekendPricePaise) {
        this.weekendPricePaise = weekendPricePaise;
    }

    public long getPeakHourPricePaise() {
        return peakHourPricePaise;
    }

    public void setPeakHourPricePaise(long peakHourPricePaise) {
        this.peakHourPricePaise = peakHourPricePaise;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public String getSurfaceType() {
        return surfaceType;
    }

    public void setSurfaceType(String surfaceType) {
        this.surfaceType = surfaceType;
    }

    public boolean isIndoor() {
        return isIndoor;
    }

    public void setIndoor(boolean indoor) {
        isIndoor = indoor;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
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
}

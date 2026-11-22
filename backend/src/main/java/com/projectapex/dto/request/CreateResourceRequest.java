package com.projectapex.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;

public class CreateResourceRequest {
    @NotBlank(message = "Resource name is required")
    private String name;

    private List<String> sportTypes = new ArrayList<>();
    private int slotDurationMinutes = 60;
    private long basePricePaise = 100000;
    private long weekendPricePaise = 140000;
    private long peakHourPricePaise = 130000;
    private int maxPlayers = 14;
    private String surfaceType = "ARTIFICIAL_TURF";
    private boolean isIndoor = false;

    public CreateResourceRequest() {
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
}

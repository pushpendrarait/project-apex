package com.projectapex.model;

import java.util.*;

public class ScheduleRuleRecord {
    private String resourceId;
    private String venueId;
    private String ownerId;
    private String timezone = "Asia/Kolkata";
    private Map<String, DayScheduleRule> weeklySchedule = new HashMap<>();
    private long updatedAt = System.currentTimeMillis();

    public ScheduleRuleRecord() {
        initDefaultWeek();
    }

    public ScheduleRuleRecord(String resourceId, String venueId, String ownerId) {
        this.resourceId = resourceId;
        this.venueId = venueId;
        this.ownerId = ownerId;
        initDefaultWeek();
    }

    private void initDefaultWeek() {
        String[] days = {"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"};
        for (String day : days) {
            DayScheduleRule rule = new DayScheduleRule(true, "06:00", "23:00");
            rule.getPeakWindows().add(new TimeWindow("18:00", "22:00"));
            weeklySchedule.put(day, rule);
        }
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

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public Map<String, DayScheduleRule> getWeeklySchedule() {
        return weeklySchedule;
    }

    public void setWeeklySchedule(Map<String, DayScheduleRule> weeklySchedule) {
        this.weeklySchedule = weeklySchedule != null ? weeklySchedule : new HashMap<>();
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public static class DayScheduleRule {
        private boolean isOpen = true;
        private String openTime = "06:00";
        private String closeTime = "23:00";
        private List<TimeWindow> peakWindows = new ArrayList<>();

        public DayScheduleRule() {
        }

        public DayScheduleRule(boolean isOpen, String openTime, String closeTime) {
            this.isOpen = isOpen;
            this.openTime = openTime;
            this.closeTime = closeTime;
        }

        public boolean isOpen() {
            return isOpen;
        }

        public void setOpen(boolean open) {
            isOpen = open;
        }

        public String getOpenTime() {
            return openTime;
        }

        public void setOpenTime(String openTime) {
            this.openTime = openTime;
        }

        public String getCloseTime() {
            return closeTime;
        }

        public void setCloseTime(String closeTime) {
            this.closeTime = closeTime;
        }

        public List<TimeWindow> getPeakWindows() {
            return peakWindows;
        }

        public void setPeakWindows(List<TimeWindow> peakWindows) {
            this.peakWindows = peakWindows != null ? peakWindows : new ArrayList<>();
        }
    }

    public static class TimeWindow {
        private String start;
        private String end;

        public TimeWindow() {
        }

        public TimeWindow(String start, String end) {
            this.start = start;
            this.end = end;
        }

        public String getStart() {
            return start;
        }

        public void setStart(String start) {
            this.start = start;
        }

        public String getEnd() {
            return end;
        }

        public void setEnd(String end) {
            this.end = end;
        }
    }
}

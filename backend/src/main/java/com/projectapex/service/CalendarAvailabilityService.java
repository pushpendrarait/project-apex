package com.projectapex.service;

import com.projectapex.model.*;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Calendar & Inventory Availability Engine
 * Dynamically computes discrete bookable slots from schedule rules, date overrides, and slot locks.
 * Strictly adheres to Firebase Spark Free-Tier rules: zero pre-generated empty slot documents!
 */
@Service
public class CalendarAvailabilityService {

    private final FirebaseService firebaseService;

    public CalendarAvailabilityService(FirebaseService firebaseService) {
        this.firebaseService = firebaseService;
    }

    /**
     * Dynamically generates and returns slot availability for a resource on a specific date
     */
    public List<SlotAvailability> getGeneratedSlots(String resourceId, String dateString) {
        ResourceRecord resource = firebaseService.getResources().get(resourceId);
        if (resource == null) {
            // Check compatibility court
            FacilityRecord court = firebaseService.getCourts().get(resourceId);
            if (court != null) {
                return getLegacySlots(court, dateString);
            }
            throw new IllegalArgumentException("Resource not found: " + resourceId);
        }

        LocalDate date;
        try {
            date = LocalDate.parse(dateString);
        } catch (Exception e) {
            date = LocalDate.now();
            dateString = date.toString();
        }

        DayOfWeek dow = date.getDayOfWeek();
        boolean isWeekend = (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY);

        // 1. Resolve Weekly Schedule Rules
        ScheduleRuleRecord scheduleRule = firebaseService.getScheduleRules().get(resourceId);
        ScheduleRuleRecord.DayScheduleRule dayRule = (scheduleRule != null && scheduleRule.getWeeklySchedule().containsKey(dow.name()))
                ? scheduleRule.getWeeklySchedule().get(dow.name())
                : new ScheduleRuleRecord.DayScheduleRule(true, "06:00", "23:00");

        if (dayRule.getPeakWindows().isEmpty()) {
            dayRule.getPeakWindows().add(new ScheduleRuleRecord.TimeWindow("18:00", "22:00"));
        }

        // 2. Resolve Date Overrides
        String overrideId = resourceId + "_" + dateString;
        DateOverrideRecord dateOverride = firebaseService.getDateOverrides().get(overrideId);

        boolean isFullDayClosed = dateOverride != null && dateOverride.isClosedFullDay();
        String openTimeStr = (dateOverride != null && dateOverride.getCustomOpenTime() != null)
                ? dateOverride.getCustomOpenTime() : dayRule.getOpenTime();
        String closeTimeStr = (dateOverride != null && dateOverride.getCustomCloseTime() != null)
                ? dateOverride.getCustomCloseTime() : dayRule.getCloseTime();

        LocalTime openTime = LocalTime.parse(openTimeStr);
        LocalTime closeTime = LocalTime.parse(closeTimeStr);
        int slotDuration = resource.getSlotDurationMinutes() > 0 ? resource.getSlotDurationMinutes() : 60;

        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");
        LocalTime nowTime = LocalTime.now();
        boolean isToday = date.equals(LocalDate.now());

        // Find active off-peak auto discounts
        DiscountRecord offPeakDiscount = firebaseService.getDiscounts().values().stream()
                .filter(d -> d.isActive() && "OFF_PEAK_AUTO".equalsIgnoreCase(d.getType()))
                .filter(d -> d.getApplicableResourceIds().isEmpty() || d.getApplicableResourceIds().contains(resourceId))
                .findFirst()
                .orElse(null);

        List<SlotAvailability> slots = new ArrayList<>();
        LocalTime current = openTime;

        while (current.isBefore(closeTime)) {
            LocalTime next = current.plusMinutes(slotDuration);
            // Stop if slot wraps around midnight or exceeds closing time
            if (next.isBefore(current) || next.isAfter(closeTime)) {
                break;
            }

            String startStr = current.format(timeFmt);
            String endStr = next.format(timeFmt);

            // Determine if in peak window
            boolean isPeak = false;
            for (ScheduleRuleRecord.TimeWindow pw : dayRule.getPeakWindows()) {
                LocalTime pwStart = LocalTime.parse(pw.getStart());
                LocalTime pwEnd = LocalTime.parse(pw.getEnd());
                if (!current.isBefore(pwStart) && current.isBefore(pwEnd)) {
                    isPeak = true;
                    break;
                }
            }

            // Price resolution priority:
            // 1. DateOverride price override
            // 2. Peak hour price
            // 3. Weekend price
            // 4. Base price
            long originalPricePaise = resource.getBasePricePaise();
            if (dateOverride != null && dateOverride.getSlotPriceOverrides().containsKey(startStr)) {
                originalPricePaise = dateOverride.getSlotPriceOverrides().get(startStr);
            } else if (isPeak && resource.getPeakHourPricePaise() > 0) {
                originalPricePaise = resource.getPeakHourPricePaise();
            } else if (isWeekend && resource.getWeekendPricePaise() > 0) {
                originalPricePaise = resource.getWeekendPricePaise();
            }

            // Calculate auto off-peak discount if applicable
            long finalPricePaise = originalPricePaise;
            String discountInfo = null;

            if (offPeakDiscount != null && offPeakDiscount.getTimeWindowStart() != null && offPeakDiscount.getTimeWindowEnd() != null) {
                LocalTime opStart = LocalTime.parse(offPeakDiscount.getTimeWindowStart());
                LocalTime opEnd = LocalTime.parse(offPeakDiscount.getTimeWindowEnd());
                if (!current.isBefore(opStart) && current.isBefore(opEnd)) {
                    if ("PERCENTAGE".equalsIgnoreCase(offPeakDiscount.getValueType())) {
                        long discountPaise = (long) (originalPricePaise * (offPeakDiscount.getValue() / 100.0));
                        finalPricePaise = Math.max(100L, originalPricePaise - discountPaise);
                        discountInfo = "Off-Peak " + (int) offPeakDiscount.getValue() + "% OFF";
                    } else if ("FLAT_PAISE".equalsIgnoreCase(offPeakDiscount.getValueType())) {
                        long discountPaise = (long) offPeakDiscount.getValue();
                        finalPricePaise = Math.max(100L, originalPricePaise - discountPaise);
                        discountInfo = "Off-Peak ₹" + (discountPaise / 100) + " OFF";
                    }
                }
            }

            // Status resolution:
            String status = "AVAILABLE";
            String blockedType = null;
            String blockedReason = null;

            if (isFullDayClosed) {
                status = "BLOCKED";
                blockedType = "MAINTENANCE";
                blockedReason = dateOverride.getClosureReason() != null ? dateOverride.getClosureReason() : "Full day closed";
            } else if (isToday && current.isBefore(nowTime)) {
                status = "PAST";
            } else if (dateOverride != null && dateOverride.getBlockedSlots().containsKey(startStr)) {
                DateOverrideRecord.BlockedSlotInfo bs = dateOverride.getBlockedSlots().get(startStr);
                status = "BLOCKED";
                blockedType = bs.getType();
                blockedReason = bs.getReason();
            } else {
                // Check deterministic slot lock: {resourceId}_{date}_{HHmm}
                String lockId = SlotLockRecord.buildLockId(resourceId, dateString, startStr);
                SlotLockRecord lock = firebaseService.getSlotLocks().get(lockId);

                if (lock != null) {
                    if ("BOOKED".equalsIgnoreCase(lock.getStatus())) {
                        status = "BOOKED";
                    } else if ("HELD".equalsIgnoreCase(lock.getStatus()) && !lock.isExpired()) {
                        status = "HELD";
                    }
                }
            }

            SlotAvailability slot = new SlotAvailability(resourceId, dateString, startStr, endStr, originalPricePaise, finalPricePaise, status);
            slot.setPeak(isPeak);
            slot.setWeekend(isWeekend);
            slot.setDiscountInfo(discountInfo);
            slot.setBlockedType(blockedType);
            slot.setBlockedReason(blockedReason);

            slots.add(slot);
            current = next;
        }

        return slots;
    }

    private List<SlotAvailability> getLegacySlots(FacilityRecord court, String dateString) {
        List<SlotAvailability> slots = new ArrayList<>();
        int basePaise = (int) (court.getBaseHourlyRate() * 100);
        int peakPaise = (int) (court.getPeakHourlyRate() * 100);
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");

        for (int h = 6; h < 23; h++) {
            LocalTime s = LocalTime.of(h, 0);
            LocalTime e = LocalTime.of(h + 1, 0);
            String startStr = s.format(timeFmt);
            String endStr = e.format(timeFmt);

            boolean isPeak = (h >= 18 && h <= 22);
            long price = isPeak ? peakPaise : basePaise;

            SlotAvailability sa = new SlotAvailability(court.getCourtId(), dateString, startStr, endStr, price, price, "AVAILABLE");
            sa.setPeak(isPeak);
            slots.add(sa);
        }
        return slots;
    }
}

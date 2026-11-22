package com.projectapex.service;

import com.projectapex.model.FacilityRecord;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class PricingService {

    public static class PricingResult {
        public int baseRate;
        public boolean isOddHour;
        public int discountPct;
        public int oddHourDiscountAmount;
        public String bandName;
        public int subtotal;
        public int convenienceFee;
        public int promoDiscountAmount;
        public String promoCode;
        public int total;
        public boolean isPeak;

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("baseRate", baseRate);
            map.put("isOddHour", isOddHour);
            map.put("discountPct", discountPct);
            map.put("oddHourDiscountAmount", oddHourDiscountAmount);
            map.put("bandName", bandName);
            map.put("subtotal", subtotal);
            map.put("convenienceFee", convenienceFee);
            map.put("promoDiscountAmount", promoDiscountAmount);
            map.put("promoCode", promoCode);
            map.put("total", total);
            map.put("isPeak", isPeak);
            return map;
        }
    }

    public PricingResult calculatePrice(FacilityRecord facility, String slotTime, String promoCode) {
        int baseRate = facility != null ? facility.getBaseHourlyRate() : 1000;
        int peakRate = facility != null ? facility.getPeakHourlyRate() : (int)(baseRate * 1.2);
        int hour = 18;
        try {
            hour = Integer.parseInt(slotTime.split(":")[0]);
        } catch (Exception ignored) {}

        PricingResult res = new PricingResult();
        res.baseRate = baseRate;
        res.convenienceFee = 20;

        // 1. Time-of-Day pricing evaluation
        if (hour >= 12 && hour < 16) {
            // Afternoon Grind (35% discount)
            res.isOddHour = true;
            res.discountPct = facility != null ? facility.getOddHourDiscountPct() : 35;
            res.bandName = "Afternoon Grind";
            res.oddHourDiscountAmount = Math.round(baseRate * (res.discountPct / 100.0f));
            res.subtotal = baseRate - res.oddHourDiscountAmount;
            res.isPeak = false;
        } else if (hour >= 6 && hour < 9) {
            // Early Bird (20% discount)
            res.isOddHour = true;
            res.discountPct = 20;
            res.bandName = "Early Bird";
            res.oddHourDiscountAmount = Math.round(baseRate * (res.discountPct / 100.0f));
            res.subtotal = baseRate - res.oddHourDiscountAmount;
            res.isPeak = false;
        } else if (hour >= 22 || hour < 2) {
            // Night Owl (25% discount)
            res.isOddHour = true;
            res.discountPct = 25;
            res.bandName = "Night Owl";
            res.oddHourDiscountAmount = Math.round(baseRate * (res.discountPct / 100.0f));
            res.subtotal = baseRate - res.oddHourDiscountAmount;
            res.isPeak = false;
        } else if (hour >= 18 && hour < 22) {
            // Prime Evening Peak (1.2x rate)
            res.isOddHour = false;
            res.discountPct = 0;
            res.bandName = "Prime Time Peak";
            res.oddHourDiscountAmount = 0;
            res.subtotal = peakRate;
            res.isPeak = true;
        } else {
            // Regular standard hours
            res.isOddHour = false;
            res.discountPct = 0;
            res.bandName = "Standard Hour";
            res.oddHourDiscountAmount = 0;
            res.subtotal = baseRate;
            res.isPeak = false;
        }

        // 2. Promo Code evaluation
        res.promoDiscountAmount = 0;
        if (promoCode != null && !promoCode.trim().isEmpty()) {
            String cleanPromo = promoCode.trim().toUpperCase();
            if ("THANE50".equals(cleanPromo)) {
                res.promoCode = "THANE50";
                res.promoDiscountAmount = 50;
            } else if ("APEX20".equals(cleanPromo)) {
                res.promoCode = "APEX20";
                res.promoDiscountAmount = Math.round(res.subtotal * 0.20f);
            } else if ("MIDNIGHT35".equals(cleanPromo)) {
                res.promoCode = "MIDNIGHT35";
                res.promoDiscountAmount = Math.round(res.subtotal * 0.35f);
            }
        }

        res.total = Math.max(0, res.subtotal - res.promoDiscountAmount + res.convenienceFee);
        return res;
    }
}

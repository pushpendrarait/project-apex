package com.projectapex.service;

import com.projectapex.model.*;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Service
public class PricingAndDiscountService {

    private final FirebaseService firebaseService;

    public PricingAndDiscountService(FirebaseService firebaseService) {
        this.firebaseService = firebaseService;
    }

    public static class PriceQuote {
        private String resourceId;
        private String bookingDate;
        private List<BookingRecord.BookedSlotItem> itemizedSlots = new ArrayList<>();
        private long subtotalPaise;
        private String discountId;
        private String discountCode;
        private long discountAmountPaise = 0;
        private long taxPaise = 0;
        private long finalPayablePaise;
        private String appliedDiscountDescription;

        public PriceQuote() {
        }

        public String getResourceId() {
            return resourceId;
        }

        public void setResourceId(String resourceId) {
            this.resourceId = resourceId;
        }

        public String getBookingDate() {
            return bookingDate;
        }

        public void setBookingDate(String bookingDate) {
            this.bookingDate = bookingDate;
        }

        public List<BookingRecord.BookedSlotItem> getItemizedSlots() {
            return itemizedSlots;
        }

        public void setItemizedSlots(List<BookingRecord.BookedSlotItem> itemizedSlots) {
            this.itemizedSlots = itemizedSlots;
        }

        public long getSubtotalPaise() {
            return subtotalPaise;
        }

        public void setSubtotalPaise(long subtotalPaise) {
            this.subtotalPaise = subtotalPaise;
        }

        public String getDiscountId() {
            return discountId;
        }

        public void setDiscountId(String discountId) {
            this.discountId = discountId;
        }

        public String getDiscountCode() {
            return discountCode;
        }

        public void setDiscountCode(String discountCode) {
            this.discountCode = discountCode;
        }

        public long getDiscountAmountPaise() {
            return discountAmountPaise;
        }

        public void setDiscountAmountPaise(long discountAmountPaise) {
            this.discountAmountPaise = discountAmountPaise;
        }

        public long getTaxPaise() {
            return taxPaise;
        }

        public void setTaxPaise(long taxPaise) {
            this.taxPaise = taxPaise;
        }

        public long getFinalPayablePaise() {
            return finalPayablePaise;
        }

        public void setFinalPayablePaise(long finalPayablePaise) {
            this.finalPayablePaise = finalPayablePaise;
        }

        public String getAppliedDiscountDescription() {
            return appliedDiscountDescription;
        }

        public void setAppliedDiscountDescription(String appliedDiscountDescription) {
            this.appliedDiscountDescription = appliedDiscountDescription;
        }
    }

    /**
     * Authoritative server-side pricing and discount calculation in integer paise
     */
    public PriceQuote calculateQuote(String venueId, String resourceId, String bookingDate,
                                      List<String> slotStartTimes, String promoCode, String userId) {
        ResourceRecord resource = firebaseService.getResources().get(resourceId);
        if (resource == null) {
            // Check compatibility court
            FacilityRecord court = firebaseService.getCourts().get(resourceId);
            if (court != null) {
                return calculateLegacyQuote(court, bookingDate, slotStartTimes, promoCode);
            }
            throw new IllegalArgumentException("Resource not found: " + resourceId);
        }

        LocalDate date = LocalDate.parse(bookingDate);
        DayOfWeek dow = date.getDayOfWeek();
        boolean isWeekend = (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY);

        ScheduleRuleRecord scheduleRule = firebaseService.getScheduleRules().get(resourceId);
        ScheduleRuleRecord.DayScheduleRule dayRule = (scheduleRule != null && scheduleRule.getWeeklySchedule().containsKey(dow.name()))
                ? scheduleRule.getWeeklySchedule().get(dow.name())
                : new ScheduleRuleRecord.DayScheduleRule(true, "06:00", "23:00");

        String overrideId = resourceId + "_" + bookingDate;
        DateOverrideRecord dateOverride = firebaseService.getDateOverrides().get(overrideId);

        int slotDuration = resource.getSlotDurationMinutes() > 0 ? resource.getSlotDurationMinutes() : 60;
        List<BookingRecord.BookedSlotItem> itemized = new ArrayList<>();
        long subtotalPaise = 0;

        for (String startTime : slotStartTimes) {
            LocalTime s = LocalTime.parse(startTime);
            LocalTime e = s.plusMinutes(slotDuration);
            String endTime = e.toString();

            boolean isPeak = false;
            for (ScheduleRuleRecord.TimeWindow pw : dayRule.getPeakWindows()) {
                LocalTime pwStart = LocalTime.parse(pw.getStart());
                LocalTime pwEnd = LocalTime.parse(pw.getEnd());
                if (!s.isBefore(pwStart) && s.isBefore(pwEnd)) {
                    isPeak = true;
                    break;
                }
            }

            long slotPrice = resource.getBasePricePaise();
            if (dateOverride != null && dateOverride.getSlotPriceOverrides().containsKey(startTime)) {
                slotPrice = dateOverride.getSlotPriceOverrides().get(startTime);
            } else if (isPeak && resource.getPeakHourPricePaise() > 0) {
                slotPrice = resource.getPeakHourPricePaise();
            } else if (isWeekend && resource.getWeekendPricePaise() > 0) {
                slotPrice = resource.getWeekendPricePaise();
            }

            subtotalPaise += slotPrice;
            itemized.add(new BookingRecord.BookedSlotItem(startTime, endTime, slotPrice));
        }

        // Evaluate Automatic Discounts
        long bestAutoDiscountPaise = 0;
        DiscountRecord bestAutoDiscount = null;

        for (DiscountRecord d : firebaseService.getDiscounts().values()) {
            if (!d.isActive()) continue;
            if (!d.getApplicableResourceIds().isEmpty() && !d.getApplicableResourceIds().contains(resourceId)) continue;
            if (!d.getApplicableVenueIds().isEmpty() && !d.getApplicableVenueIds().contains(venueId)) continue;

            if ("BULK_SLOTS_AUTO".equalsIgnoreCase(d.getType()) && d.getMinSlots() != null && slotStartTimes.size() >= d.getMinSlots()) {
                long dVal = calculateDiscountAmount(subtotalPaise, d);
                if (dVal > bestAutoDiscountPaise) {
                    bestAutoDiscountPaise = dVal;
                    bestAutoDiscount = d;
                }
            } else if ("OFF_PEAK_AUTO".equalsIgnoreCase(d.getType())) {
                // If all slots fall in off-peak window
                boolean allOffPeak = true;
                if (d.getTimeWindowStart() != null && d.getTimeWindowEnd() != null) {
                    LocalTime wStart = LocalTime.parse(d.getTimeWindowStart());
                    LocalTime wEnd = LocalTime.parse(d.getTimeWindowEnd());
                    for (String st : slotStartTimes) {
                        LocalTime s = LocalTime.parse(st);
                        if (s.isBefore(wStart) || !s.isBefore(wEnd)) {
                            allOffPeak = false;
                            break;
                        }
                    }
                }
                if (allOffPeak) {
                    long dVal = calculateDiscountAmount(subtotalPaise, d);
                    if (dVal > bestAutoDiscountPaise) {
                        bestAutoDiscountPaise = dVal;
                        bestAutoDiscount = d;
                    }
                }
            }
        }

        // Evaluate Promo Code
        long promoDiscountPaise = 0;
        DiscountRecord promoDiscount = null;
        if (promoCode != null && !promoCode.trim().isEmpty()) {
            String cleanCode = promoCode.trim().toUpperCase();
            DiscountRecord matched = firebaseService.getDiscounts().values().stream()
                    .filter(d -> d.isActive() && cleanCode.equalsIgnoreCase(d.getCode()))
                    .findFirst()
                    .orElse(null);

            if (matched != null) {
                if (subtotalPaise >= matched.getMinOrderPaise()) {
                    promoDiscountPaise = calculateDiscountAmount(subtotalPaise, matched);
                    promoDiscount = matched;
                }
            } else if ("THANE50".equalsIgnoreCase(cleanCode)) {
                // Flat ₹50 fallback promo
                promoDiscountPaise = Math.min(subtotalPaise, 5000L);
            }
        }

        // Stacking & Selection Logic:
        long totalDiscountPaise = 0;
        String appliedDesc = null;
        String appliedId = null;
        String appliedCode = null;

        if (promoDiscount != null && bestAutoDiscount != null) {
            if (promoDiscount.isStackableWithAutoDiscount()) {
                totalDiscountPaise = promoDiscountPaise + bestAutoDiscountPaise;
                appliedDesc = promoDiscount.getName() + " + " + bestAutoDiscount.getName();
                appliedId = promoDiscount.getDiscountId();
                appliedCode = promoDiscount.getCode();
            } else {
                if (promoDiscountPaise >= bestAutoDiscountPaise) {
                    totalDiscountPaise = promoDiscountPaise;
                    appliedDesc = promoDiscount.getName();
                    appliedId = promoDiscount.getDiscountId();
                    appliedCode = promoDiscount.getCode();
                } else {
                    totalDiscountPaise = bestAutoDiscountPaise;
                    appliedDesc = bestAutoDiscount.getName();
                    appliedId = bestAutoDiscount.getDiscountId();
                }
            }
        } else if (promoDiscount != null) {
            totalDiscountPaise = promoDiscountPaise;
            appliedDesc = promoDiscount.getName();
            appliedId = promoDiscount.getDiscountId();
            appliedCode = promoDiscount.getCode();
        } else if (bestAutoDiscount != null) {
            totalDiscountPaise = bestAutoDiscountPaise;
            appliedDesc = bestAutoDiscount.getName();
            appliedId = bestAutoDiscount.getDiscountId();
        } else if (promoCode != null && "THANE50".equalsIgnoreCase(promoCode.trim())) {
            totalDiscountPaise = Math.min(subtotalPaise, 5000L);
            appliedDesc = "Thane Welcome ₹50 Flat Off";
            appliedCode = "THANE50";
        }

        // Razorpay minimum transaction constraint: 100 paise (₹1.00)
        long finalPayablePaise = Math.max(100L, subtotalPaise - totalDiscountPaise);

        PriceQuote quote = new PriceQuote();
        quote.setResourceId(resourceId);
        quote.setBookingDate(bookingDate);
        quote.setItemizedSlots(itemized);
        quote.setSubtotalPaise(subtotalPaise);
        quote.setDiscountAmountPaise(totalDiscountPaise);
        quote.setDiscountId(appliedId);
        quote.setDiscountCode(appliedCode);
        quote.setFinalPayablePaise(finalPayablePaise);
        quote.setAppliedDiscountDescription(appliedDesc);

        return quote;
    }

    private long calculateDiscountAmount(long subtotalPaise, DiscountRecord discount) {
        if ("PERCENTAGE".equalsIgnoreCase(discount.getValueType())) {
            long calc = (long) (subtotalPaise * (discount.getValue() / 100.0));
            if (discount.getMaxDiscountPaise() > 0) {
                calc = Math.min(calc, discount.getMaxDiscountPaise());
            }
            return calc;
        } else {
            return (long) discount.getValue();
        }
    }

    private PriceQuote calculateLegacyQuote(FacilityRecord court, String bookingDate, List<String> slots, String promoCode) {
        long rate = (long) (court.getBaseHourlyRate() * 100);
        long subtotal = rate * slots.size();
        long discount = 0;
        String code = null;

        if (promoCode != null && promoCode.toUpperCase().contains("50")) {
            discount = 5000L;
            code = promoCode.toUpperCase();
        }

        long payable = Math.max(100L, subtotal - discount);

        PriceQuote quote = new PriceQuote();
        quote.setResourceId(court.getCourtId());
        quote.setBookingDate(bookingDate);
        quote.setSubtotalPaise(subtotal);
        quote.setDiscountAmountPaise(discount);
        quote.setDiscountCode(code);
        quote.setFinalPayablePaise(payable);
        return quote;
    }
}

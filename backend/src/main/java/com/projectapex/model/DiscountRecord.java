package com.projectapex.model;

import java.util.ArrayList;
import java.util.List;

public class DiscountRecord {
    private String discountId;
    private String ownerId;
    private String name;
    private String type = "PROMO_CODE"; // PROMO_CODE, OFF_PEAK_AUTO, BULK_SLOTS_AUTO
    private String code; // Uppercase trimmed code for PROMO_CODE
    private String valueType = "PERCENTAGE"; // PERCENTAGE, FLAT_PAISE
    private double value = 20.0; // 20% or 20000 paise (₹200)
    private long maxDiscountPaise = 50000; // max ₹500 cap
    private long minOrderPaise = 100000; // min ₹1000
    private Integer minSlots; // for BULK_SLOTS_AUTO (e.g. 2 or 3)
    private List<String> daysOfWeek = new ArrayList<>(); // e.g. ["MONDAY", "TUESDAY"]
    private String timeWindowStart; // "10:00"
    private String timeWindowEnd; // "16:00"
    private List<String> applicableVenueIds = new ArrayList<>();
    private List<String> applicableResourceIds = new ArrayList<>();
    private List<String> applicableSports = new ArrayList<>();
    private Long validFrom = System.currentTimeMillis() - 86400000L;
    private Long validUntil = System.currentTimeMillis() + (30L * 86400000L);
    private Integer maxTotalUses = 1000;
    private Integer maxUsesPerUser = 5;
    private int currentUses = 0;
    private boolean stackableWithAutoDiscount = false;
    private boolean isActive = true;
    private long createdAt = System.currentTimeMillis();

    public DiscountRecord() {
    }

    public DiscountRecord(String discountId, String ownerId, String name, String type, String code, double value, String valueType) {
        this.discountId = discountId;
        this.ownerId = ownerId;
        this.name = name;
        this.type = type;
        this.code = code != null ? code.trim().toUpperCase() : null;
        this.value = value;
        this.valueType = valueType;
    }

    public String getDiscountId() {
        return discountId;
    }

    public void setDiscountId(String discountId) {
        this.discountId = discountId;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code != null ? code.trim().toUpperCase() : null;
    }

    public String getValueType() {
        return valueType;
    }

    public void setValueType(String valueType) {
        this.valueType = valueType;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public long getMaxDiscountPaise() {
        return maxDiscountPaise;
    }

    public void setMaxDiscountPaise(long maxDiscountPaise) {
        this.maxDiscountPaise = maxDiscountPaise;
    }

    public long getMinOrderPaise() {
        return minOrderPaise;
    }

    public void setMinOrderPaise(long minOrderPaise) {
        this.minOrderPaise = minOrderPaise;
    }

    public Integer getMinSlots() {
        return minSlots;
    }

    public void setMinSlots(Integer minSlots) {
        this.minSlots = minSlots;
    }

    public List<String> getDaysOfWeek() {
        return daysOfWeek;
    }

    public void setDaysOfWeek(List<String> daysOfWeek) {
        this.daysOfWeek = daysOfWeek != null ? daysOfWeek : new ArrayList<>();
    }

    public String getTimeWindowStart() {
        return timeWindowStart;
    }

    public void setTimeWindowStart(String timeWindowStart) {
        this.timeWindowStart = timeWindowStart;
    }

    public String getTimeWindowEnd() {
        return timeWindowEnd;
    }

    public void setTimeWindowEnd(String timeWindowEnd) {
        this.timeWindowEnd = timeWindowEnd;
    }

    public List<String> getApplicableVenueIds() {
        return applicableVenueIds;
    }

    public void setApplicableVenueIds(List<String> applicableVenueIds) {
        this.applicableVenueIds = applicableVenueIds != null ? applicableVenueIds : new ArrayList<>();
    }

    public List<String> getApplicableResourceIds() {
        return applicableResourceIds;
    }

    public void setApplicableResourceIds(List<String> applicableResourceIds) {
        this.applicableResourceIds = applicableResourceIds != null ? applicableResourceIds : new ArrayList<>();
    }

    public List<String> getApplicableSports() {
        return applicableSports;
    }

    public void setApplicableSports(List<String> applicableSports) {
        this.applicableSports = applicableSports != null ? applicableSports : new ArrayList<>();
    }

    public Long getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(Long validFrom) {
        this.validFrom = validFrom;
    }

    public Long getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(Long validUntil) {
        this.validUntil = validUntil;
    }

    public Integer getMaxTotalUses() {
        return maxTotalUses;
    }

    public void setMaxTotalUses(Integer maxTotalUses) {
        this.maxTotalUses = maxTotalUses;
    }

    public Integer getMaxUsesPerUser() {
        return maxUsesPerUser;
    }

    public void setMaxUsesPerUser(Integer maxUsesPerUser) {
        this.maxUsesPerUser = maxUsesPerUser;
    }

    public int getCurrentUses() {
        return currentUses;
    }

    public void setCurrentUses(int currentUses) {
        this.currentUses = currentUses;
    }

    public boolean isStackableWithAutoDiscount() {
        return stackableWithAutoDiscount;
    }

    public void setStackableWithAutoDiscount(boolean stackableWithAutoDiscount) {
        this.stackableWithAutoDiscount = stackableWithAutoDiscount;
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
}

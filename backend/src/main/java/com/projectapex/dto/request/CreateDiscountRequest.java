package com.projectapex.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;

public class CreateDiscountRequest {
    @NotBlank(message = "Discount name is required")
    private String name;

    private String type = "PROMO_CODE"; // PROMO_CODE, OFF_PEAK_AUTO, BULK_SLOTS_AUTO
    private String code;
    private String valueType = "PERCENTAGE"; // PERCENTAGE, FLAT_PAISE
    private double value = 20.0;
    private long maxDiscountPaise = 50000;
    private long minOrderPaise = 100000;
    private Integer minSlots;
    private List<String> daysOfWeek = new ArrayList<>();
    private String timeWindowStart;
    private String timeWindowEnd;
    private List<String> applicableVenueIds = new ArrayList<>();
    private List<String> applicableResourceIds = new ArrayList<>();
    private List<String> applicableSports = new ArrayList<>();
    private Long validFrom = System.currentTimeMillis();
    private Long validUntil = System.currentTimeMillis() + (30L * 86400000L);
    private Integer maxTotalUses = 1000;
    private Integer maxUsesPerUser = 5;
    private boolean stackableWithAutoDiscount = false;

    public CreateDiscountRequest() {
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
        this.code = code;
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

    public boolean isStackableWithAutoDiscount() {
        return stackableWithAutoDiscount;
    }

    public void setStackableWithAutoDiscount(boolean stackableWithAutoDiscount) {
        this.stackableWithAutoDiscount = stackableWithAutoDiscount;
    }
}

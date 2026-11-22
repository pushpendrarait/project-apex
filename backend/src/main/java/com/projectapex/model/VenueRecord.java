package com.projectapex.model;

import java.util.*;

public class VenueRecord {
    private String venueId;
    private String ownerId;
    private String name;
    private String description;
    private String addressLine;
    private String locality;
    private String city;
    private String state = "Maharashtra";
    private String pincode = "400601";
    private double latitude = 19.2183;
    private double longitude = 72.9781;
    private String contactPhone;
    private List<String> amenities = new ArrayList<>();
    private List<String> sportTypes = new ArrayList<>();
    private long minSlotPricePaise = 80000; // e.g. ₹800
    private boolean hasActiveDiscounts = true;
    private List<String> imageUrls = new ArrayList<>();
    private Map<String, Object> cancellationPolicy = new HashMap<>();
    private String status = "ACTIVE"; // DRAFT, PENDING_APPROVAL, ACTIVE, INACTIVE
    private double rating = 4.8;
    private int reviewCount = 142;
    private long createdAt = System.currentTimeMillis();
    private long updatedAt = System.currentTimeMillis();

    public VenueRecord() {
        initDefaultPolicy();
    }

    public VenueRecord(String venueId, String ownerId, String name, String locality, String city) {
        this.venueId = venueId;
        this.ownerId = ownerId;
        this.name = name;
        this.locality = locality;
        this.city = city != null ? city.toLowerCase() : "thane";
        this.addressLine = locality + ", " + city;
        this.status = "ACTIVE";
        initDefaultPolicy();
    }

    private void initDefaultPolicy() {
        if (cancellationPolicy.isEmpty()) {
            cancellationPolicy.put("fullRefundHoursBefore", 24);
            cancellationPolicy.put("partialRefundHoursBefore", 6);
            cancellationPolicy.put("partialRefundPercent", 50);
        }
    }

    // Compatibility getters
    public String getId() {
        return venueId;
    }

    public void setId(String id) {
        this.venueId = id;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAddressLine() {
        return addressLine;
    }

    public void setAddressLine(String addressLine) {
        this.addressLine = addressLine;
    }

    public String getLocality() {
        return locality;
    }

    public void setLocality(String locality) {
        this.locality = locality;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city != null ? city.toLowerCase() : "thane";
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getPincode() {
        return pincode;
    }

    public void setPincode(String pincode) {
        this.pincode = pincode;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public List<String> getAmenities() {
        return amenities;
    }

    public void setAmenities(List<String> amenities) {
        this.amenities = amenities != null ? amenities : new ArrayList<>();
    }

    public List<String> getSportTypes() {
        return sportTypes;
    }

    public void setSportTypes(List<String> sportTypes) {
        this.sportTypes = sportTypes != null ? sportTypes : new ArrayList<>();
    }

    public long getMinSlotPricePaise() {
        return minSlotPricePaise;
    }

    public void setMinSlotPricePaise(long minSlotPricePaise) {
        this.minSlotPricePaise = minSlotPricePaise;
    }

    public boolean isHasActiveDiscounts() {
        return hasActiveDiscounts;
    }

    public void setHasActiveDiscounts(boolean hasActiveDiscounts) {
        this.hasActiveDiscounts = hasActiveDiscounts;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls != null ? imageUrls : new ArrayList<>();
    }

    public Map<String, Object> getCancellationPolicy() {
        return cancellationPolicy;
    }

    public void setCancellationPolicy(Map<String, Object> cancellationPolicy) {
        this.cancellationPolicy = cancellationPolicy != null ? cancellationPolicy : new HashMap<>();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public int getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(int reviewCount) {
        this.reviewCount = reviewCount;
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

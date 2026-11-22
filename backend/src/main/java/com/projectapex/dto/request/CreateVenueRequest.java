package com.projectapex.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreateVenueRequest {
    @NotBlank(message = "Venue name is required")
    private String name;

    private String description;
    private String addressLine;
    private String locality;
    private String city = "thane";
    private String state = "Maharashtra";
    private String pincode = "400601";
    private double latitude = 19.2183;
    private double longitude = 72.9781;
    private String contactPhone;
    private List<String> amenities = new ArrayList<>();
    private List<String> sportTypes = new ArrayList<>();
    private long minSlotPricePaise = 80000;
    private List<String> imageUrls = new ArrayList<>();
    private Map<String, Object> cancellationPolicy = new HashMap<>();

    public CreateVenueRequest() {
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
        this.city = city;
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
        this.cancellationPolicy = cancellationPolicy;
    }
}

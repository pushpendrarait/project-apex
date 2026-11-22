package com.projectapex.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class VenueRecord {
    private String venueId;
    private String name;
    private String ownerId;
    private String city;
    private String area;
    private String address;
    private List<String> sports;
    private List<String> amenities;
    private Double rating;
    private Integer reviewsCount;
    private Integer startingPrice;
    private Boolean featured;
    private String status; // "ACTIVE"
    private List<String> images;
    private Long createdAt;

    public VenueRecord() {}

    public String getVenueId() { return venueId; }
    public void setVenueId(String venueId) { this.venueId = venueId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public List<String> getSports() { return sports; }
    public void setSports(List<String> sports) { this.sports = sports; }

    public List<String> getAmenities() { return amenities; }
    public void setAmenities(List<String> amenities) { this.amenities = amenities; }

    public Double getRating() { return rating != null ? rating : 5.0; }
    public void setRating(Double rating) { this.rating = rating; }

    public Integer getReviewsCount() { return reviewsCount != null ? reviewsCount : 0; }
    public void setReviewsCount(Integer reviewsCount) { this.reviewsCount = reviewsCount; }

    public Integer getStartingPrice() { return startingPrice; }
    public void setStartingPrice(Integer startingPrice) { this.startingPrice = startingPrice; }

    public Boolean getFeatured() { return featured != null ? featured : false; }
    public void setFeatured(Boolean featured) { this.featured = featured; }

    public String getStatus() { return status != null ? status : "ACTIVE"; }
    public void setStatus(String status) { this.status = status; }

    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }

    public Long getCreatedAt() { return createdAt; }
    public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }
}

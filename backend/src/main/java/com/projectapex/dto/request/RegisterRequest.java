package com.projectapex.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public class RegisterRequest {
    @NotBlank(message = "Display name is required")
    private String displayName;

    @NotBlank(message = "Phone number is required")
    private String phone;

    @NotBlank(message = "Email address is required")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    private String role = "player"; // "player" or "venue_owner"
    private String city = "Thane";

    // Optional fields for Facility Owner
    private String facilityName;
    private String area;
    private List<String> sports;
    private Integer startingPrice;

    public RegisterRequest() {}

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getFacilityName() { return facilityName; }
    public void setFacilityName(String facilityName) { this.facilityName = facilityName; }

    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }

    public List<String> getSports() { return sports; }
    public void setSports(List<String> sports) { this.sports = sports; }

    public Integer getStartingPrice() { return startingPrice; }
    public void setStartingPrice(Integer startingPrice) { this.startingPrice = startingPrice; }
}

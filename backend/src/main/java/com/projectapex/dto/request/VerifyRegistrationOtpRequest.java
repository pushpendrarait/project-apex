package com.projectapex.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public class VerifyRegistrationOtpRequest {
    @NotBlank(message = "Session ID is required")
    private String sessionId;

    @NotBlank(message = "Phone OTP is required")
    private String phoneOtp;

    @NotBlank(message = "Email OTP is required")
    private String emailOtp;

    @NotBlank(message = "Role is required (player or venue_owner)")
    private String role; // "player" or "venue_owner"

    @NotBlank(message = "Display name is required")
    private String displayName;

    @NotBlank(message = "Phone number is required")
    private String phone;

    @NotBlank(message = "Email address is required")
    private String email;

    private String password;
    private String city;

    // Optional fields for Facility Owner
    private String facilityName;
    private String area;
    private List<String> sports;
    private Integer startingPrice;

    public VerifyRegistrationOtpRequest() {}

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getPhoneOtp() { return phoneOtp; }
    public void setPhoneOtp(String phoneOtp) { this.phoneOtp = phoneOtp; }

    public String getEmailOtp() { return emailOtp; }
    public void setEmailOtp(String emailOtp) { this.emailOtp = emailOtp; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

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

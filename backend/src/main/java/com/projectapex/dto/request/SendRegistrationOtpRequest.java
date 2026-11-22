package com.projectapex.dto.request;

import jakarta.validation.constraints.NotBlank;

public class SendRegistrationOtpRequest {
    @NotBlank(message = "Phone number is required")
    private String phone;

    @NotBlank(message = "Email address is required")
    private String email;

    @NotBlank(message = "Display name is required")
    private String displayName;

    public SendRegistrationOtpRequest() {}

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
}

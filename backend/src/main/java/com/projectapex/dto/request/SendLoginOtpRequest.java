package com.projectapex.dto.request;

import jakarta.validation.constraints.NotBlank;

public class SendLoginOtpRequest {
    @NotBlank(message = "Phone number or email is required")
    private String identifier;

    public SendLoginOtpRequest() {}

    public String getIdentifier() { return identifier; }
    public void setIdentifier(String identifier) { this.identifier = identifier; }
}

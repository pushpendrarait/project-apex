package com.projectapex.dto.request;

import jakarta.validation.constraints.NotBlank;

public class VerifyLoginOtpRequest {
    @NotBlank(message = "Session ID is required")
    private String sessionId;

    @NotBlank(message = "OTP code is required")
    private String otp;

    public VerifyLoginOtpRequest() {}

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getOtp() { return otp; }
    public void setOtp(String otp) { this.otp = otp; }
}

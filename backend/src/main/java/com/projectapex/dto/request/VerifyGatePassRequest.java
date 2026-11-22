package com.projectapex.dto.request;

import jakarta.validation.constraints.NotBlank;

public class VerifyGatePassRequest {
    @NotBlank(message = "QR Pass Token is required")
    private String qrPassToken;

    public VerifyGatePassRequest() {}

    public String getQrPassToken() { return qrPassToken; }
    public void setQrPassToken(String qrPassToken) { this.qrPassToken = qrPassToken; }
}

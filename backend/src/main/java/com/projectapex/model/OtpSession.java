package com.projectapex.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class OtpSession {
    private String sessionId;
    private String phone;
    private String email;
    private String phoneOtp;
    private String emailOtp;
    private String loginOtp;
    private String userId;
    private String purpose; // "REGISTRATION", "LOGIN"
    private Long createdAt;
    private Long expiresAt;
    private Integer attemptsRemaining;
    private Boolean verified;

    public OtpSession() {}

    public boolean isExpired() {
        return System.currentTimeMillis() > (expiresAt != null ? expiresAt : 0);
    }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhoneOtp() { return phoneOtp; }
    public void setPhoneOtp(String phoneOtp) { this.phoneOtp = phoneOtp; }

    public String getEmailOtp() { return emailOtp; }
    public void setEmailOtp(String emailOtp) { this.emailOtp = emailOtp; }

    public String getLoginOtp() { return loginOtp; }
    public void setLoginOtp(String loginOtp) { this.loginOtp = loginOtp; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public Long getCreatedAt() { return createdAt; }
    public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }

    public Long getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Long expiresAt) { this.expiresAt = expiresAt; }

    public Integer getAttemptsRemaining() { return attemptsRemaining != null ? attemptsRemaining : 3; }
    public void setAttemptsRemaining(Integer attemptsRemaining) { this.attemptsRemaining = attemptsRemaining; }

    public Boolean getVerified() { return verified != null ? verified : false; }
    public void setVerified(Boolean verified) { this.verified = verified; }
}

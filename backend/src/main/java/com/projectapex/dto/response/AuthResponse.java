package com.projectapex.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.projectapex.model.UserRecord;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {
    private String token;
    private UserRecord user;
    private String sessionId;
    private Integer expiresInSeconds;
    private String simulatedSmsOtp;
    private String simulatedEmailOtp;

    public AuthResponse() {}

    public AuthResponse(String token, UserRecord user) {
        this.token = token;
        this.user = user;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public UserRecord getUser() { return user; }
    public void setUser(UserRecord user) { this.user = user; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public Integer getExpiresInSeconds() { return expiresInSeconds; }
    public void setExpiresInSeconds(Integer expiresInSeconds) { this.expiresInSeconds = expiresInSeconds; }

    public String getSimulatedSmsOtp() { return simulatedSmsOtp; }
    public void setSimulatedSmsOtp(String simulatedSmsOtp) { this.simulatedSmsOtp = simulatedSmsOtp; }

    public String getSimulatedEmailOtp() { return simulatedEmailOtp; }
    public void setSimulatedEmailOtp(String simulatedEmailOtp) { this.simulatedEmailOtp = simulatedEmailOtp; }
}

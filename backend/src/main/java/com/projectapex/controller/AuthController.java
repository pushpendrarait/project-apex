package com.projectapex.controller;

import com.projectapex.dto.request.*;
import com.projectapex.dto.response.ApiResponse;
import com.projectapex.dto.response.AuthResponse;
import com.projectapex.model.UserRecord;
import com.projectapex.service.AuthService;
import com.projectapex.service.OtpService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;
    private final OtpService otpService;

    public AuthController(AuthService authService, OtpService otpService) {
        this.authService = authService;
        this.otpService = otpService;
    }

    /**
     * Technical Specification: Register Player Profile (ROLE_PLAYER)
     */
    @PostMapping("/auth/register-profile")
    public ResponseEntity<ApiResponse<AuthResponse>> registerProfile(@Valid @RequestBody RegisterProfileRequest req) {
        AuthResponse res = authService.registerProfile(req);
        return ResponseEntity.ok(ApiResponse.ok("Player profile registered successfully", res));
    }

    /**
     * Technical Specification: Register Facility Owner (ROLE_OWNER)
     */
    @PostMapping("/auth/register-owner")
    public ResponseEntity<ApiResponse<AuthResponse>> registerOwner(@Valid @RequestBody RegisterOwnerRequest req) {
        AuthResponse res = authService.registerOwner(req);
        return ResponseEntity.ok(ApiResponse.ok("Facility Owner registered successfully", res));
    }

    // =========================================================================
    // Password & OTP Authentication Endpoints (Backward Compatibility)
    // =========================================================================

    @PostMapping("/auth/login/password")
    public ResponseEntity<ApiResponse<AuthResponse>> loginWithPassword(@Valid @RequestBody LoginPasswordRequest req) {
        AuthResponse response = authService.loginWithPassword(req.getIdentifier(), req.getPassword());
        return ResponseEntity.ok(ApiResponse.ok("Authentication successful", response));
    }

    @PostMapping("/auth/register")
    public ResponseEntity<ApiResponse<AuthResponse>> registerWithPassword(@Valid @RequestBody RegisterRequest req) {
        AuthResponse response = authService.registerWithPassword(req);
        return ResponseEntity.ok(ApiResponse.ok("Account created successfully", response));
    }

    @PostMapping("/auth/registration/send-otp")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendRegistrationOtp(@Valid @RequestBody SendRegistrationOtpRequest req) {
        var session = otpService.sendRegistrationOtp(req.getPhone(), req.getEmail());
        return ResponseEntity.ok(ApiResponse.ok("Verification OTPs dispatched", Map.of(
                "sessionId", session.getSessionId(),
                "expiresInSeconds", 300
        )));
    }

    @PostMapping("/auth/registration/verify-otp")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyRegistrationOtp(@Valid @RequestBody VerifyRegistrationOtpRequest req) {
        AuthResponse response = authService.verifyAndRegister(req);
        return ResponseEntity.ok(ApiResponse.ok("Registration verified successfully", response));
    }

    @PostMapping("/auth/login/send-otp")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendLoginOtp(@Valid @RequestBody SendLoginOtpRequest req) {
        var session = otpService.sendLoginOtp(req.getIdentifier());
        return ResponseEntity.ok(ApiResponse.ok("Login OTP dispatched", Map.of(
                "sessionId", session.getSessionId(),
                "expiresInSeconds", 300
        )));
    }

    @PostMapping("/auth/login/verify-otp")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyLoginOtp(@Valid @RequestBody VerifyLoginOtpRequest req) {
        AuthResponse response = authService.verifyLoginOtp(req.getSessionId(), req.getOtp());
        return ResponseEntity.ok(ApiResponse.ok("Login successful", response));
    }
}

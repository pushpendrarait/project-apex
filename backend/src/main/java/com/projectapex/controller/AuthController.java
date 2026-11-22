package com.projectapex.controller;

import com.projectapex.dto.request.*;
import com.projectapex.dto.response.ApiResponse;
import com.projectapex.dto.response.AuthResponse;
import com.projectapex.model.OtpSession;
import com.projectapex.service.AuthService;
import com.projectapex.service.OtpService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final OtpService otpService;

    public AuthController(AuthService authService, OtpService otpService) {
        this.authService = authService;
        this.otpService = otpService;
    }

    /**
     * Dispatches Dual Registration OTPs (SMS to phone + Verification code to email)
     */
    @PostMapping("/register/send-otp")
    public ResponseEntity<ApiResponse<AuthResponse>> sendRegistrationOtp(@Valid @RequestBody SendRegistrationOtpRequest req) {
        OtpSession session = otpService.sendRegistrationOtp(req.getPhone(), req.getEmail(), req.getDisplayName());
        AuthResponse res = new AuthResponse();
        res.setSessionId(session.getSessionId());
        res.setExpiresInSeconds(300);
        res.setSimulatedSmsOtp(session.getPhoneOtp());
        res.setSimulatedEmailOtp(session.getEmailOtp());
        return ResponseEntity.ok(ApiResponse.ok("Dual OTPs dispatched via SMS and Email", res));
    }

    /**
     * Atomically verifies dual OTPs and provisions user / owner account
     */
    @PostMapping("/register/verify-otp")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyAndRegister(@Valid @RequestBody VerifyRegistrationOtpRequest req) {
        AuthResponse res = authService.verifyAndRegister(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Account verified and registered successfully", res));
    }

    /**
     * Dispatches passwordless Login OTP
     */
    @PostMapping("/login/send-otp")
    public ResponseEntity<ApiResponse<AuthResponse>> sendLoginOtp(@Valid @RequestBody SendLoginOtpRequest req) {
        OtpSession session = otpService.sendLoginOtp(req.getIdentifier());
        AuthResponse res = new AuthResponse();
        res.setSessionId(session.getSessionId());
        res.setExpiresInSeconds(300);
        res.setSimulatedSmsOtp(session.getLoginOtp());
        return ResponseEntity.ok(ApiResponse.ok("Login OTP dispatched successfully", res));
    }

    /**
     * Verifies Login OTP and issues JWT token
     */
    @PostMapping("/login/verify-otp")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyLoginOtp(@Valid @RequestBody VerifyLoginOtpRequest req) {
        AuthResponse res = authService.verifyLoginOtp(req.getSessionId(), req.getOtp());
        return ResponseEntity.ok(ApiResponse.ok("Signed in successfully", res));
    }

    /**
     * Direct identifier login
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody SendLoginOtpRequest req) {
        AuthResponse res = authService.login(req.getIdentifier());
        return ResponseEntity.ok(ApiResponse.ok("Signed in successfully", res));
    }

    /**
     * Logout / Session termination
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        return ResponseEntity.ok(ApiResponse.ok("Logged out successfully. Transitioned to guest mode.", null));
    }
}

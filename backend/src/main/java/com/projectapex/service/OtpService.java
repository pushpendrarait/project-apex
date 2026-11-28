package com.projectapex.service;

import com.projectapex.model.AuditLog;
import com.projectapex.model.OtpSession;
import com.projectapex.model.UserRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class OtpService {
    private static final Logger log = LoggerFactory.getLogger(OtpService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Pattern PHONE_PATTERN = Pattern.compile("^(?:\\+91|91)?[6-9]\\d{9}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

    @Value("${app.otp.ttl-seconds:300}")
    private long otpTtlSeconds;

    private final FirebaseService firebaseService;

    public OtpService(FirebaseService firebaseService) {
        this.firebaseService = firebaseService;
    }

    public String generateOtpCode() {
        return String.valueOf(100000 + RANDOM.nextInt(900000));
    }

    public String normalizePhone(String phone) {
        if (phone == null) return null;
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.length() == 10 && (digits.startsWith("6") || digits.startsWith("7") || digits.startsWith("8") || digits.startsWith("9"))) {
            return "+91 " + digits.substring(0, 5) + " " + digits.substring(5);
        }
        if (digits.length() == 12 && digits.startsWith("91")) {
            return "+91 " + digits.substring(2, 7) + " " + digits.substring(7);
        }
        return phone.trim();
    }

    public boolean isValidPhone(String phone) {
        if (phone == null) return false;
        String clean = phone.replaceAll("[\\s-]", "");
        return PHONE_PATTERN.matcher(clean).matches();
    }

    public boolean isValidEmail(String email) {
        if (email == null) return false;
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    public OtpSession sendRegistrationOtp(String rawPhone, String rawEmail) {
        return sendRegistrationOtp(rawPhone, rawEmail, "Sports Player");
    }

    /**
     * Dispatches Dual OTPs: 6-digit Mobile SMS OTP + 6-digit Email Verification Code
     */
    public OtpSession sendRegistrationOtp(String rawPhone, String rawEmail, String displayName) {
        if (!isValidPhone(rawPhone)) {
            throw new IllegalArgumentException("Please enter a valid 10-digit Indian mobile number (+91).");
        }
        if (!isValidEmail(rawEmail)) {
            throw new IllegalArgumentException("Please enter a valid email address (e.g. user@example.com).");
        }

        String phone = normalizePhone(rawPhone);
        String email = rawEmail.trim().toLowerCase();
        String phoneDigits = phone.replaceAll("[^0-9]", "");
        boolean duplicate = firebaseService.getUsers().values().stream().anyMatch(u ->
                (u.getEmail() != null && u.getEmail().trim().equalsIgnoreCase(email)) ||
                (phoneDigits.length() >= 10 && u.getPhone() != null
                        && u.getPhone().replaceAll("[^0-9]", "").endsWith(phoneDigits.substring(phoneDigits.length() - 10)))
        );
        if (duplicate) {
            throw new IllegalArgumentException("An account is already registered with this mobile number or email. Please sign in instead.");
        }
        long now = System.currentTimeMillis();
        String sessionId = "otp_reg_" + now + "_" + UUID.randomUUID().toString().substring(0, 6);

        String phoneOtp = generateOtpCode();
        String emailOtp = generateOtpCode();

        OtpSession session = new OtpSession();
        session.setSessionId(sessionId);
        session.setPhone(phone);
        session.setEmail(email);
        session.setPhoneOtp(phoneOtp);
        session.setEmailOtp(emailOtp);
        session.setPurpose("REGISTRATION");
        session.setCreatedAt(now);
        session.setExpiresAt(now + (otpTtlSeconds * 1000));
        session.setAttemptsRemaining(3);
        session.setVerified(false);

        firebaseService.getOtpVerifications().put(sessionId, session);

        // Simulated notification logging
        log.info("📱 [SMS Gateway to {}]: Your Project Apex Mobile OTP is {} (Valid for 5 mins).", phone, phoneOtp);
        log.info("📧 [Email Gateway to {}]: Your Project Apex Email Verification Code is {}.", email, emailOtp);

        // Security Audit Log
        AuditLog audit = new AuditLog(
                "audit_" + now + "_" + UUID.randomUUID().toString().substring(0, 6),
                "OTP_DISPATCHED",
                "system_auth",
                "otp_session",
                sessionId,
                Map.of("phone", phone, "email", email, "purpose", "REGISTRATION")
        );
        firebaseService.getAuditLogs().add(audit);

        return session;
    }

    /**
     * Verifies Dual Registration OTPs atomically
     */
    public OtpSession verifyRegistrationOtp(String sessionId, String phoneOtp, String emailOtp) {
        OtpSession session = firebaseService.getOtpVerifications().get(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Invalid or expired OTP session. Please request fresh OTPs.");
        }

        if (session.isExpired()) {
            throw new IllegalStateException("OTP codes have expired (5-minute lease exceeded). Please click Resend OTPs.");
        }

        if (session.getAttemptsRemaining() <= 0) {
            throw new IllegalStateException("Maximum verification attempts exceeded. Please request fresh OTPs.");
        }

        boolean phoneMatch = session.getPhoneOtp().equals(phoneOtp != null ? phoneOtp.trim() : "");
        boolean emailMatch = session.getEmailOtp().equals(emailOtp != null ? emailOtp.trim() : "");

        if (!phoneMatch || !emailMatch) {
            session.setAttemptsRemaining(session.getAttemptsRemaining() - 1);
            firebaseService.persistOtpSession(session);
            if (!phoneMatch && !emailMatch) {
                throw new IllegalArgumentException("Incorrect Mobile SMS OTP and Email verification code. (" + session.getAttemptsRemaining() + " attempts remaining)");
            } else if (!phoneMatch) {
                throw new IllegalArgumentException("Incorrect Mobile SMS OTP. (" + session.getAttemptsRemaining() + " attempts remaining)");
            } else {
                throw new IllegalArgumentException("Incorrect Email verification code. (" + session.getAttemptsRemaining() + " attempts remaining)");
            }
        }

        session.setVerified(true);
        firebaseService.persistOtpSession(session);
        log.info("✓ Dual OTP Verification Succeeded for session: {}", sessionId);

        AuditLog audit = new AuditLog(
                "audit_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 6),
                "OTP_VERIFIED_SUCCESS",
                "system_auth",
                "otp_session",
                sessionId,
                Map.of("phone", session.getPhone(), "email", session.getEmail())
        );
        firebaseService.getAuditLogs().add(audit);

        return session;
    }

    /**
     * Dispatches Passwordless Login OTP to registered user
     */
    public OtpSession sendLoginOtp(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) {
            throw new IllegalArgumentException("Please enter your registered mobile number or email.");
        }

        String clean = identifier.trim().toLowerCase();
        String cleanDigits = identifier.replaceAll("[^0-9]", "");

        UserRecord matchedUser = firebaseService.getUsers().values().stream()
                .filter(u -> (u.getEmail() != null && u.getEmail().toLowerCase().equals(clean)) ||
                             (u.getPhone() != null && cleanDigits.length() >= 10 && u.getPhone().replaceAll("[^0-9]", "").contains(cleanDigits.substring(cleanDigits.length() - 10))))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No registered account found matching \"" + identifier + "\". Please register as a new user."));

        long now = System.currentTimeMillis();
        String sessionId = "otp_login_" + now + "_" + UUID.randomUUID().toString().substring(0, 6);
        String loginOtp = generateOtpCode();

        OtpSession session = new OtpSession();
        session.setSessionId(sessionId);
        session.setUserId(matchedUser.getUid());
        session.setPhone(matchedUser.getPhone());
        session.setEmail(matchedUser.getEmail());
        session.setLoginOtp(loginOtp);
        session.setPurpose("LOGIN");
        session.setCreatedAt(now);
        session.setExpiresAt(now + (otpTtlSeconds * 1000));
        session.setAttemptsRemaining(3);
        session.setVerified(false);

        firebaseService.getOtpVerifications().put(sessionId, session);

        log.info("📱 [Apex Security]: Your Login OTP is {} for {}. Valid for 5 minutes.", loginOtp, matchedUser.getDisplayName());
        return session;
    }

    /**
     * Verifies Login OTP
     */
    public UserRecord verifyLoginOtp(String sessionId, String otp) {
        OtpSession session = firebaseService.getOtpVerifications().get(sessionId);
        if (session == null || session.isExpired()) {
            throw new IllegalArgumentException("Invalid or expired login OTP session.");
        }

        if (session.getAttemptsRemaining() <= 0) {
            throw new IllegalStateException("Maximum verification attempts exceeded.");
        }

        if (!session.getLoginOtp().equals(otp != null ? otp.trim() : "")) {
            session.setAttemptsRemaining(session.getAttemptsRemaining() - 1);
            firebaseService.persistOtpSession(session);
            throw new IllegalArgumentException("Incorrect Login OTP. (" + session.getAttemptsRemaining() + " attempts remaining)");
        }

        session.setVerified(true);
        firebaseService.persistOtpSession(session);
        UserRecord user = firebaseService.getUsers().get(session.getUserId());
        if (user == null) {
            throw new IllegalStateException("User profile not found.");
        }

        return user;
    }
}

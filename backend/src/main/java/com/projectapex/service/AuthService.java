package com.projectapex.service;

import com.projectapex.dto.request.VerifyRegistrationOtpRequest;
import com.projectapex.dto.response.AuthResponse;
import com.projectapex.model.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AuthService {

    private final FirebaseService firebaseService;
    private final OtpService otpService;
    private final JwtService jwtService;
    private final VenueService venueService;

    public AuthService(FirebaseService firebaseService,
                       OtpService otpService,
                       JwtService jwtService,
                       VenueService venueService) {
        this.firebaseService = firebaseService;
        this.otpService = otpService;
        this.jwtService = jwtService;
        this.venueService = venueService;
    }

    /**
     * Completes Player or Facility Owner Registration after Dual OTP validation
     */
    public synchronized AuthResponse verifyAndRegister(VerifyRegistrationOtpRequest req) {
        // 1. Verify Dual OTPs atomically
        OtpSession session = otpService.verifyRegistrationOtp(req.getSessionId(), req.getPhoneOtp(), req.getEmailOtp());

        long now = System.currentTimeMillis();
        String nameSlug = req.getDisplayName().toLowerCase().replaceAll("[^a-z0-9]", "_");
        String uid = "usr_" + nameSlug + "_" + (1000 + (int)(Math.random() * 9000));

        UserRecord user = new UserRecord(
                uid,
                req.getDisplayName(),
                session.getEmail(),
                session.getPhone(),
                req.getRole(),
                req.getCity() != null ? req.getCity() : "Thane"
        );
        user.setIsPhoneVerified(true);
        user.setIsEmailVerified(true);
        user.setOtpSessionId(session.getSessionId());

        if ("venue_owner".equalsIgnoreCase(req.getRole())) {
            // Auto-provision initial venue for facility owner
            String facilityName = req.getFacilityName() != null && !req.getFacilityName().trim().isEmpty()
                    ? req.getFacilityName()
                    : req.getDisplayName() + "'s Sports Arena";

            VenueRecord venue = new VenueRecord();
            venue.setName(facilityName);
            venue.setOwnerId(uid);
            venue.setCity(req.getCity() != null ? req.getCity() : "Thane");
            venue.setArea(req.getArea() != null ? req.getArea() : "Central Area");
            venue.setAddress(venue.getArea() + ", " + venue.getCity() + ", Maharashtra");
            venue.setSports(req.getSports() != null && !req.getSports().isEmpty() ? req.getSports() : List.of("cricket", "football"));
            venue.setAmenities(List.of("Floodlights", "Changing Rooms", "Parking", "Dugout"));
            venue.setStartingPrice(req.getStartingPrice() != null ? req.getStartingPrice() : 1200);

            VenueRecord createdVenue = venueService.createVenue(venue);
            user.setVenueId(createdVenue.getVenueId());

            // Auto-provision initial primary pitch
            FacilityRecord defaultCourt = new FacilityRecord();
            defaultCourt.setCourtId("court_" + createdVenue.getVenueId() + "_01");
            defaultCourt.setVenueId(createdVenue.getVenueId());
            defaultCourt.setName("Main Arena Pitch 1");
            defaultCourt.setSport(venue.getSports().get(0));
            defaultCourt.setCapacity(16);
            defaultCourt.setSurface("15mm Monofilament Synthetic Turf");
            defaultCourt.setDimensions("115ft x 65ft");
            defaultCourt.setBaseHourlyRate(venue.getStartingPrice());
            defaultCourt.setPeakHourlyRate((int)(venue.getStartingPrice() * 1.3));
            defaultCourt.setOddHourDiscountPct(35);
            defaultCourt.setHasFloodlights(true);
            defaultCourt.setStatus("ACTIVE");
            defaultCourt.setCreatedAt(now);
            firebaseService.getCourts().put(defaultCourt.getCourtId(), defaultCourt);

            venueService.getDailyAvailability(defaultCourt.getCourtId(), "2026-11-28");
        } else {
            // Player welcome package
            String initials = req.getDisplayName().replaceAll("[^A-Za-z]", "").toUpperCase();
            String prefix = initials.length() >= 5 ? initials.substring(0, 5) : initials;
            user.setReferralCode(prefix + (10 + (int)(Math.random() * 90)));
            user.setLoyaltyPoints(100);
        }

        firebaseService.getUsers().put(uid, user);

        // Security Audit Log
        AuditLog audit = new AuditLog(
                "audit_" + now,
                "USER_REGISTERED",
                uid,
                "user",
                uid,
                Map.of("role", user.getRole(), "phone", user.getPhone(), "email", user.getEmail())
        );
        firebaseService.getAuditLogs().add(audit);

        // Issue JWT token
        String token = jwtService.generateToken(user.getUid(), user.getRole(), user.getDisplayName(), user.getPhone(), user.getEmail());
        return new AuthResponse(token, user);
    }

    /**
     * Completes Login via Login OTP verification
     */
    public AuthResponse verifyLoginOtp(String sessionId, String otp) {
        UserRecord user = otpService.verifyLoginOtp(sessionId, otp);
        String token = jwtService.generateToken(user.getUid(), user.getRole(), user.getDisplayName(), user.getPhone(), user.getEmail());
        return new AuthResponse(token, user);
    }

    /**
     * Standard Login by identifier (Phone, Email, UID)
     */
    public AuthResponse login(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number or email is required.");
        }

        String clean = identifier.trim().toLowerCase();
        String cleanDigits = identifier.replaceAll("[^0-9]", "");

        UserRecord user = firebaseService.getUsers().values().stream()
                .filter(u -> (clean.equals(u.getUid())) ||
                             (u.getEmail() != null && u.getEmail().toLowerCase().equals(clean)) ||
                             (u.getPhone() != null && cleanDigits.length() >= 10 && u.getPhone().replaceAll("[^0-9]", "").contains(cleanDigits.substring(cleanDigits.length() - 10))))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No registered account found matching \"" + identifier + "\". Please check your credentials or register."));

        String token = jwtService.generateToken(user.getUid(), user.getRole(), user.getDisplayName(), user.getPhone(), user.getEmail());
        return new AuthResponse(token, user);
    }
}

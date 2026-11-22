package com.projectapex.service;

import com.projectapex.dto.request.RegisterOwnerRequest;
import com.projectapex.dto.request.RegisterProfileRequest;
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
     * Technical Specification: Register Player Profile (POST /api/v1/auth/register-profile)
     */
    public synchronized AuthResponse registerProfile(RegisterProfileRequest req) {
        if (req.getFullName() == null || req.getFullName().trim().isEmpty()) {
            throw new IllegalArgumentException("Full Name is required");
        }
        if (req.getPhone() == null || req.getPhone().trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number is required");
        }

        long now = System.currentTimeMillis();
        String nameSlug = req.getFullName().toLowerCase().replaceAll("[^a-z0-9]", "_");
        String uid = "usr_" + nameSlug + "_" + (1000 + (int) (Math.random() * 9000));
        String email = req.getEmail() != null ? req.getEmail().trim().toLowerCase() : uid + "@apex.dev";

        UserRecord user = new UserRecord(uid, email, req.getFullName().trim(), req.getPhone().trim(), "ROLE_PLAYER");
        user.setCity(req.getCity() != null ? req.getCity() : "Thane");
        user.setPreferredSports(req.getPreferredSports());
        user.setLoyaltyPoints(100);
        if (req.getPassword() != null && !req.getPassword().trim().isEmpty()) {
            user.setPasswordHash(FirebaseService.hashPassword(req.getPassword()));
        }

        firebaseService.getUsers().put(uid, user);
        String token = jwtService.generateToken(user.getUid(), "ROLE_PLAYER", user.getFullName(), user.getPhone(), user.getEmail());
        return new AuthResponse(token, user);
    }

    /**
     * Technical Specification: Register Facility Owner (POST /api/v1/auth/register-owner)
     */
    public synchronized AuthResponse registerOwner(RegisterOwnerRequest req) {
        if (req.getOwnerName() == null || req.getOwnerName().trim().isEmpty()) {
            throw new IllegalArgumentException("Owner Name is required");
        }
        if (req.getBusinessName() == null || req.getBusinessName().trim().isEmpty()) {
            throw new IllegalArgumentException("Business Name is required");
        }
        if (req.getEmail() == null || req.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }

        long now = System.currentTimeMillis();
        String nameSlug = req.getOwnerName().toLowerCase().replaceAll("[^a-z0-9]", "_");
        String uid = "owner_" + nameSlug + "_" + (1000 + (int) (Math.random() * 9000));

        UserRecord user = new UserRecord(uid, req.getEmail().trim().toLowerCase(), req.getOwnerName().trim(), req.getPhone().trim(), "ROLE_OWNER");
        user.setCity(req.getCity() != null ? req.getCity() : "Thane");

        OwnerProfile op = new OwnerProfile(req.getBusinessName(), req.getBusinessAddress(), req.getGstNumber(), req.getPanNumber());
        user.setOwnerProfile(op);
        if (req.getPassword() != null && !req.getPassword().trim().isEmpty()) {
            user.setPasswordHash(FirebaseService.hashPassword(req.getPassword()));
        }

        // Auto-provision initial venue for facility owner
        VenueRecord venue = new VenueRecord(
                "venue_" + nameSlug + "_" + (100 + (int) (Math.random() * 900)),
                uid,
                req.getBusinessName() + " Sports Arena",
                "Central Area",
                req.getCity() != null ? req.getCity() : "thane"
        );
        venue.setAddressLine(req.getBusinessAddress() != null ? req.getBusinessAddress() : "Central Area, Thane");
        venue.setSportTypes(List.of("CRICKET_TURF", "FOOTBALL_TURF"));
        venue.setMinSlotPricePaise(120000);
        venue.setStatus("ACTIVE");
        firebaseService.getVenues().put(venue.getVenueId(), venue);

        // Auto-provision primary resource
        ResourceRecord res = new ResourceRecord(
                "res_" + venue.getVenueId() + "_01",
                venue.getVenueId(),
                uid,
                "Main Box Cricket Arena 1",
                List.of("CRICKET_TURF"),
                120000
        );
        firebaseService.getResources().put(res.getResourceId(), res);
        firebaseService.getScheduleRules().put(res.getResourceId(), new ScheduleRuleRecord(res.getResourceId(), venue.getVenueId(), uid));

        firebaseService.getUsers().put(uid, user);
        String token = jwtService.generateToken(user.getUid(), "ROLE_OWNER", user.getFullName(), user.getPhone(), user.getEmail());
        return new AuthResponse(token, user);
    }

    public UserRecord getUser(String uid) {
        UserRecord user = firebaseService.getUsers().get(uid);
        if (user == null) {
            throw new IllegalArgumentException("User not found: " + uid);
        }
        return user;
    }

    public synchronized UserRecord updateProfile(String uid, RegisterProfileRequest req) {
        UserRecord user = getUser(uid);
        if (req.getFullName() != null) user.setFullName(req.getFullName());
        if (req.getPhone() != null) user.setPhone(req.getPhone());
        if (req.getCity() != null) user.setCity(req.getCity());
        if (req.getPreferredSports() != null) user.setPreferredSports(req.getPreferredSports());
        user.setUpdatedAt(System.currentTimeMillis());
        return user;
    }

    /**
     * Completes Player or Facility Owner Registration after Dual OTP validation
     */
    public synchronized AuthResponse verifyAndRegister(VerifyRegistrationOtpRequest req) {
        OtpSession session = otpService.verifyRegistrationOtp(req.getSessionId(), req.getPhoneOtp(), req.getEmailOtp());

        long now = System.currentTimeMillis();
        String nameSlug = req.getDisplayName().toLowerCase().replaceAll("[^a-z0-9]", "_");
        String uid = "usr_" + nameSlug + "_" + (1000 + (int)(Math.random() * 9000));

        UserRecord user = new UserRecord(
                uid,
                session.getEmail(),
                req.getDisplayName(),
                session.getPhone(),
                req.getRole()
        );
        user.setCity(req.getCity() != null ? req.getCity() : "Thane");
        user.setPhoneVerified(true);
        user.setEmailVerified(true);
        if (req.getPassword() != null && !req.getPassword().trim().isEmpty()) {
            user.setPasswordHash(FirebaseService.hashPassword(req.getPassword()));
        }

        if ("venue_owner".equalsIgnoreCase(req.getRole())) {
            String facilityName = req.getFacilityName() != null && !req.getFacilityName().trim().isEmpty()
                    ? req.getFacilityName()
                    : req.getDisplayName() + "'s Sports Arena";

            VenueRecord venue = new VenueRecord("venue_" + nameSlug + "_" + (100 + (int)(Math.random() * 900)),
                    uid, facilityName, req.getArea() != null ? req.getArea() : "Central Area", req.getCity() != null ? req.getCity() : "thane");
            firebaseService.getVenues().put(venue.getVenueId(), venue);

            FacilityRecord defaultCourt = new FacilityRecord();
            defaultCourt.setCourtId("court_" + venue.getVenueId() + "_01");
            defaultCourt.setVenueId(venue.getVenueId());
            defaultCourt.setName("Main Arena Pitch 1");
            defaultCourt.setSport("cricket");
            defaultCourt.setBaseHourlyRate(1200);
            defaultCourt.setPeakHourlyRate(1600);
            defaultCourt.setOddHourDiscountPct(35);
            defaultCourt.setHasFloodlights(true);
            defaultCourt.setStatus("ACTIVE");
            defaultCourt.setCreatedAt(now);
            firebaseService.getCourts().put(defaultCourt.getCourtId(), defaultCourt);
            venueService.getDailyAvailability(defaultCourt.getCourtId(), "2026-11-28");
        } else {
            user.setLoyaltyPoints(100);
        }

        firebaseService.getUsers().put(uid, user);
        String token = jwtService.generateToken(user.getUid(), user.getRole(), user.getFullName(), user.getPhone(), user.getEmail());
        return new AuthResponse(token, user);
    }

    public AuthResponse verifyLoginOtp(String sessionId, String otp) {
        UserRecord user = otpService.verifyLoginOtp(sessionId, otp);
        String token = jwtService.generateToken(user.getUid(), user.getRole(), user.getFullName(), user.getPhone(), user.getEmail());
        return new AuthResponse(token, user);
    }

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

        String token = jwtService.generateToken(user.getUid(), user.getRole(), user.getFullName(), user.getPhone(), user.getEmail());
        return new AuthResponse(token, user);
    }

    public AuthResponse loginWithPassword(String identifier, String password) {
        if (identifier == null || identifier.trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number or email is required.");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password is required.");
        }

        String clean = identifier.trim().toLowerCase();
        String cleanDigits = identifier.replaceAll("[^0-9]", "");

        UserRecord user = firebaseService.getUsers().values().stream()
                .filter(u -> (clean.equals(u.getUid())) ||
                             (u.getEmail() != null && u.getEmail().toLowerCase().equals(clean)) ||
                             (u.getPhone() != null && cleanDigits.length() >= 10 && u.getPhone().replaceAll("[^0-9]", "").contains(cleanDigits.substring(cleanDigits.length() - 10))))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No account registered with \"" + identifier + "\". Please check your details or create a new account."));

        String expectedHash = user.getPasswordHash();
        String inputHash = FirebaseService.hashPassword(password);
        if (expectedHash != null && !expectedHash.equals(inputHash)) {
            throw new IllegalArgumentException("Incorrect password. Please verify your credentials or reset your password.");
        }

        String token = jwtService.generateToken(user.getUid(), user.getRole(), user.getFullName(), user.getPhone(), user.getEmail());
        return new AuthResponse(token, user);
    }

    public synchronized AuthResponse registerWithPassword(com.projectapex.dto.request.RegisterRequest req) {
        if (req.getDisplayName() == null || req.getDisplayName().trim().isEmpty()) {
            throw new IllegalArgumentException("Full name is required");
        }
        if (req.getPhone() == null || req.getPhone().trim().isEmpty()) {
            throw new IllegalArgumentException("Mobile number is required");
        }
        if (req.getEmail() == null || req.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email address is required");
        }
        if (req.getPassword() == null || req.getPassword().length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }

        long now = System.currentTimeMillis();
        String nameSlug = req.getDisplayName().toLowerCase().replaceAll("[^a-z0-9]", "_");
        String uid = "usr_" + nameSlug + "_" + (1000 + (int)(Math.random() * 9000));

        UserRecord user = new UserRecord(
                uid,
                req.getEmail().trim().toLowerCase(),
                req.getDisplayName().trim(),
                req.getPhone().trim(),
                req.getRole() != null ? req.getRole() : "ROLE_PLAYER"
        );
        user.setCity(req.getCity() != null ? req.getCity() : "Thane");
        user.setPasswordHash(FirebaseService.hashPassword(req.getPassword()));
        user.setPhoneVerified(true);
        user.setEmailVerified(true);

        if ("venue_owner".equalsIgnoreCase(req.getRole())) {
            String facilityName = req.getFacilityName() != null && !req.getFacilityName().trim().isEmpty()
                    ? req.getFacilityName().trim()
                    : req.getDisplayName() + "'s Sports Arena";

            VenueRecord venue = new VenueRecord("venue_" + nameSlug + "_" + (100 + (int)(Math.random() * 900)),
                    uid, facilityName, req.getArea() != null ? req.getArea() : "Central Area", req.getCity() != null ? req.getCity() : "thane");
            firebaseService.getVenues().put(venue.getVenueId(), venue);

            FacilityRecord defaultCourt = new FacilityRecord();
            defaultCourt.setCourtId("court_" + venue.getVenueId() + "_01");
            defaultCourt.setVenueId(venue.getVenueId());
            defaultCourt.setName("Main Arena Pitch 1");
            defaultCourt.setSport("cricket");
            defaultCourt.setBaseHourlyRate(1200);
            defaultCourt.setPeakHourlyRate(1600);
            defaultCourt.setOddHourDiscountPct(35);
            defaultCourt.setHasFloodlights(true);
            defaultCourt.setStatus("ACTIVE");
            defaultCourt.setCreatedAt(now);
            firebaseService.getCourts().put(defaultCourt.getCourtId(), defaultCourt);
        } else {
            user.setLoyaltyPoints(100);
        }

        firebaseService.getUsers().put(uid, user);
        String token = jwtService.generateToken(user.getUid(), user.getRole(), user.getFullName(), user.getPhone(), user.getEmail());
        return new AuthResponse(token, user);
    }
}

package com.projectapex.service;

import com.projectapex.model.UserRecord;
import com.projectapex.model.VenueRecord;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private final FirebaseService firebaseService;

    public AdminService(FirebaseService firebaseService) {
        this.firebaseService = firebaseService;
    }

    public List<UserRecord> getPendingOwners() {
        return firebaseService.getUsers().values().stream()
                .filter(u -> "ROLE_OWNER".equalsIgnoreCase(u.getRole()))
                .filter(u -> u.getOwnerProfile() != null && "PENDING_VERIFICATION".equalsIgnoreCase(u.getOwnerProfile().getVerificationStatus()))
                .collect(Collectors.toList());
    }

    public UserRecord verifyOwner(String uid, String status) {
        UserRecord user = firebaseService.getUsers().get(uid);
        if (user == null) {
            throw new IllegalArgumentException("User not found: " + uid);
        }
        if (user.getOwnerProfile() != null) {
            user.getOwnerProfile().setVerificationStatus(status.toUpperCase());
            user.getOwnerProfile().setVerifiedAt(System.currentTimeMillis());
            user.setUpdatedAt(System.currentTimeMillis());
            firebaseService.persistUser(user);
        }
        return user;
    }

    public VenueRecord updateVenueStatus(String venueId, String status) {
        VenueRecord v = firebaseService.getVenues().get(venueId);
        if (v == null) {
            throw new IllegalArgumentException("Venue not found: " + venueId);
        }
        v.setStatus(status.toUpperCase());
        v.setUpdatedAt(System.currentTimeMillis());
        firebaseService.persistVenue(v);
        return v;
    }
}

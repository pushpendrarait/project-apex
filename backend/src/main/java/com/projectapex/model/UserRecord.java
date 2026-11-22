package com.projectapex.model;

import java.util.ArrayList;
import java.util.List;

public class UserRecord {
    private String uid;
    private String email;
    private String fullName;
    private String phone;
    private String role = "ROLE_PLAYER"; // ROLE_PLAYER, ROLE_OWNER, ROLE_ADMIN
    private String city = "Thane";
    private List<String> preferredSports = new ArrayList<>();
    private OwnerProfile ownerProfile;
    private String passwordHash;
    private long loyaltyPoints = 100;
    private boolean isPhoneVerified = true;
    private boolean isEmailVerified = true;
    private long createdAt = System.currentTimeMillis();
    private long updatedAt = System.currentTimeMillis();

    public UserRecord() {
    }

    public UserRecord(String uid, String email, String fullName, String phone, String role) {
        this.uid = uid;
        this.email = email;
        this.fullName = fullName;
        this.phone = phone;
        this.role = role != null ? role : "player";
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    // Compatibility getter
    public String getDisplayName() {
        return fullName;
    }

    public void setDisplayName(String displayName) {
        this.fullName = displayName;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public List<String> getPreferredSports() {
        return preferredSports;
    }

    public void setPreferredSports(List<String> preferredSports) {
        this.preferredSports = preferredSports != null ? preferredSports : new ArrayList<>();
    }

    public OwnerProfile getOwnerProfile() {
        return ownerProfile;
    }

    public void setOwnerProfile(OwnerProfile ownerProfile) {
        this.ownerProfile = ownerProfile;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public long getLoyaltyPoints() {
        return loyaltyPoints;
    }

    public void setLoyaltyPoints(long loyaltyPoints) {
        this.loyaltyPoints = loyaltyPoints;
    }

    public boolean isPhoneVerified() {
        return isPhoneVerified;
    }

    public boolean getIsPhoneVerified() {
        return isPhoneVerified;
    }

    public void setPhoneVerified(boolean phoneVerified) {
        isPhoneVerified = phoneVerified;
    }

    public void setIsPhoneVerified(boolean phoneVerified) {
        isPhoneVerified = phoneVerified;
    }

    public boolean isEmailVerified() {
        return isEmailVerified;
    }

    public boolean getIsEmailVerified() {
        return isEmailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        isEmailVerified = emailVerified;
    }

    public void setIsEmailVerified(boolean emailVerified) {
        isEmailVerified = emailVerified;
    }

    public String getVenueId() {
        return "venue_thane_01";
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
}

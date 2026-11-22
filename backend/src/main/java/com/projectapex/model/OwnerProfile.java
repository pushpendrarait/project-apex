package com.projectapex.model;

public class OwnerProfile {
    private String businessName;
    private String businessAddress;
    private String gstNumber;
    private String panNumber;
    private String verificationStatus = "APPROVED"; // Default APPROVED in dev, configurable
    private Long verifiedAt;
    private String payoutUpiOrBankMask;

    public OwnerProfile() {
    }

    public OwnerProfile(String businessName, String businessAddress, String gstNumber, String panNumber) {
        this.businessName = businessName;
        this.businessAddress = businessAddress;
        this.gstNumber = gstNumber;
        this.panNumber = panNumber;
        this.verificationStatus = "APPROVED";
        this.verifiedAt = System.currentTimeMillis();
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getBusinessAddress() {
        return businessAddress;
    }

    public void setBusinessAddress(String businessAddress) {
        this.businessAddress = businessAddress;
    }

    public String getGstNumber() {
        return gstNumber;
    }

    public void setGstNumber(String gstNumber) {
        this.gstNumber = gstNumber;
    }

    public String getPanNumber() {
        return panNumber;
    }

    public void setPanNumber(String panNumber) {
        this.panNumber = panNumber;
    }

    public String getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(String verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public Long getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(Long verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public String getPayoutUpiOrBankMask() {
        return payoutUpiOrBankMask;
    }

    public void setPayoutUpiOrBankMask(String payoutUpiOrBankMask) {
        this.payoutUpiOrBankMask = payoutUpiOrBankMask;
    }
}

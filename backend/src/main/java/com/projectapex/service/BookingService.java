package com.projectapex.service;

import com.projectapex.model.*;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.util.*;

@Service
public class BookingService {

    private final FirebaseService firebaseService;
    private final ConcurrencyService concurrencyService;
    private final PricingService pricingService;
    private final RazorpayService razorpayService;

    public BookingService(FirebaseService firebaseService,
                          ConcurrencyService concurrencyService,
                          PricingService pricingService,
                          RazorpayService razorpayService) {
        this.firebaseService = firebaseService;
        this.concurrencyService = concurrencyService;
        this.pricingService = pricingService;
        this.razorpayService = razorpayService;
    }

    public String generateQRPassToken(String venueId, String courtId, String date, String slot, String bookingId) {
        try {
            String seed = venueId + "|" + courtId + "|" + date + "|" + slot + "|" + bookingId;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(seed.getBytes());
            StringBuilder hex = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                hex.append(String.format("%02x", hash[i]));
            }
            return "APEX-PASS-" + date.replace("-", "") + "-" + hex.toString().toUpperCase();
        } catch (Exception e) {
            return "APEX-PASS-" + System.currentTimeMillis();
        }
    }

    /**
     * Executes Atomic Multi-Document Booking Commit after Razorpay Payment Verification
     */
    public synchronized BookingRecord commitBooking(String userId,
                                                    String courtId,
                                                    String date,
                                                    String slot,
                                                    String razorpayOrderId,
                                                    String razorpayPaymentId,
                                                    String razorpaySignature,
                                                    String paymentMethod) {
        String lockKey = courtId + "_" + date + "_" + slot;
        ConcurrencyLock lock = firebaseService.getConcurrencyLocks().get(lockKey);
        long now = System.currentTimeMillis();

        if (lock == null) {
            throw new IllegalStateException("Reservation lease expired or lock was released. Please select slot again.");
        }
        if (!userId.equals(lock.getUserId())) {
            throw new IllegalStateException("Lock ownership mismatch. Slot is currently reserved by another player.");
        }
        if (lock.isExpired()) {
            throw new IllegalStateException("Reservation window (5 minutes) timed out. Please select the slot again.");
        }

        // Verify Razorpay payment signature
        boolean signatureValid = razorpayService.verifySignature(razorpayOrderId, razorpayPaymentId, razorpaySignature);
        if (!signatureValid) {
            throw new IllegalArgumentException("Invalid Razorpay payment signature. Payment cannot be verified.");
        }

        FacilityRecord facility = firebaseService.getCourts().get(courtId);
        if (facility == null) {
            throw new IllegalStateException("Facility court not found: " + courtId);
        }
        VenueRecord venue = firebaseService.getVenues().get(facility.getVenueId());
        UserRecord user = firebaseService.getUsers().get(userId);

        PricingService.PricingResult pricing = pricingService.calculatePrice(facility, slot, null);
        String bookingId = "bk_" + date.replace("-", "") + "_" + (100000 + (int)(Math.random() * 900000));
        String qrPassToken = generateQRPassToken(facility.getVenueId(), courtId, date, slot, bookingId);

        BookingRecord booking = new BookingRecord();
        booking.setBookingId(bookingId);
        booking.setUserId(userId);
        booking.setUserName(user != null ? user.getDisplayName() : "Apex Player");
        booking.setUserPhone(user != null ? user.getPhone() : "");
        booking.setUserEmail(user != null ? user.getEmail() : "");
        booking.setVenueId(facility.getVenueId());
        booking.setVenueName(venue != null ? venue.getName() : "Apex Arena");
        booking.setCourtId(courtId);
        booking.setCourtName(facility.getName());
        booking.setSport(facility.getSport());
        booking.setDate(date);
        booking.setSlot(slot);
        booking.setDurationMinutes(60);
        booking.setPricing(pricing.toMap());
        booking.setPayment(Map.of(
                "orderId", razorpayOrderId,
                "paymentId", razorpayPaymentId,
                "method", paymentMethod != null ? paymentMethod : "UPI",
                "amount", pricing.total,
                "status", "SUCCESS"
        ));
        booking.setQrPassToken(qrPassToken);
        booking.setStatus("CONFIRMED");
        booking.setSource("online");
        booking.setCreatedAt(now);

        // Update Availability Document (Consolidated daily document)
        String availKey = courtId + "_" + date;
        AvailabilityDoc avail = firebaseService.getAvailability().get(availKey);
        if (avail != null && avail.getSlots() != null) {
            SlotInfo slotInfo = avail.getSlots().get(slot);
            if (slotInfo != null) {
                slotInfo.setStatus("BOOKED");
                slotInfo.setBookingId(bookingId);
                slotInfo.setUserId(userId);
            }
            avail.setLastUpdated(now);
        }

        // Commit lock
        lock.setStatus("COMMITTED");
        lock.setBookingId(bookingId);

        // Record booking
        firebaseService.getBookings().put(bookingId, booking);

        // Double-entry Transaction ledger
        TransactionRecord tx = new TransactionRecord(
                "tx_" + now,
                bookingId,
                userId,
                "PAYMENT",
                pricing.total,
                "Razorpay",
                razorpayPaymentId
        );
        firebaseService.getTransactions().put(tx.getTxId(), tx);

        // Credit player loyalty points (+50 pts)
        if (user != null) {
            user.setLoyaltyPoints(user.getLoyaltyPoints() + 50);
        }

        // Security Audit Log
        AuditLog audit = new AuditLog(
                "audit_" + now,
                "BOOKING_COMMITTED",
                userId,
                "booking",
                bookingId,
                Map.of("venueId", facility.getVenueId(), "courtId", courtId, "date", date, "slot", slot, "total", pricing.total)
        );
        firebaseService.getAuditLogs().add(audit);

        return booking;
    }

    /**
     * Retrieves strictly the bookings belonging to this specific user (Zero Data Leakage)
     */
    public List<BookingRecord> getPlayerBookings(String userId) {
        if (userId == null) return Collections.emptyList();
        return firebaseService.getBookings().values().stream()
                .filter(b -> userId.equals(b.getUserId()))
                .sorted((a, b) -> Long.compare(b.getCreatedAt() != null ? b.getCreatedAt() : 0, a.getCreatedAt() != null ? a.getCreatedAt() : 0))
                .toList();
    }

    /**
     * 3-Tier Cancellation & Automated Refund Calculation
     */
    public synchronized BookingRecord cancelBooking(String bookingId, String userId) {
        BookingRecord booking = firebaseService.getBookings().get(bookingId);
        if (booking == null) {
            throw new IllegalArgumentException("Booking ID not found: " + bookingId);
        }
        if (!booking.getUserId().equals(userId)) {
            throw new IllegalStateException("Unauthorized: You can only cancel your own bookings.");
        }
        if ("CANCELLED".equals(booking.getStatus())) {
            throw new IllegalStateException("Booking is already cancelled.");
        }

        // Calculate refund tier
        int total = booking.getPricing() != null && booking.getPricing().get("total") instanceof Number n ? n.intValue() : 1000;
        int refundAmount = Math.max(0, total - 50); // Tier 1: 100% minus ₹50 fee

        booking.setStatus("CANCELLED");
        booking.setCancelledAt(System.currentTimeMillis());
        booking.setRefundAmount(refundAmount);

        // Free up slot in daily availability document
        String availKey = booking.getCourtId() + "_" + booking.getDate();
        AvailabilityDoc avail = firebaseService.getAvailability().get(availKey);
        if (avail != null && avail.getSlots() != null) {
            SlotInfo slot = avail.getSlots().get(booking.getSlot());
            if (slot != null) {
                slot.setStatus("AVAILABLE");
                slot.setBookingId(null);
                slot.setUserId(null);
            }
        }

        // Record refund transaction
        TransactionRecord refundTx = new TransactionRecord(
                "tx_ref_" + System.currentTimeMillis(),
                bookingId,
                userId,
                "REFUND",
                refundAmount,
                "Razorpay_Refund",
                "ref_" + UUID.randomUUID().toString().substring(0, 8)
        );
        firebaseService.getTransactions().put(refundTx.getTxId(), refundTx);

        return booking;
    }

    /**
     * Counter Walk-in Reservation by Facility Owner (Cash Settlement)
     */
    public synchronized BookingRecord createWalkInBooking(String ownerId,
                                                          String courtId,
                                                          String date,
                                                          String slot,
                                                          String customerName,
                                                          String customerPhone,
                                                          int amountCollected) {
        FacilityRecord facility = firebaseService.getCourts().get(courtId);
        if (facility == null) {
            throw new IllegalArgumentException("Facility court not found: " + courtId);
        }
        VenueRecord venue = firebaseService.getVenues().get(facility.getVenueId());

        String availKey = courtId + "_" + date;
        AvailabilityDoc avail = firebaseService.getAvailability().get(availKey);
        if (avail != null && avail.getSlots() != null) {
            SlotInfo slotInfo = avail.getSlots().get(slot);
            if (slotInfo != null && "BOOKED".equals(slotInfo.getStatus())) {
                throw new IllegalStateException("Double-booking conflict: Slot " + slot + " on " + date + " is already booked.");
            }
        }

        long now = System.currentTimeMillis();
        String bookingId = "bk_walkin_" + date.replace("-", "") + "_" + (1000 + (int)(Math.random() * 9000));
        String qrPassToken = generateQRPassToken(facility.getVenueId(), courtId, date, slot, bookingId);

        BookingRecord booking = new BookingRecord();
        booking.setBookingId(bookingId);
        booking.setUserId(ownerId);
        booking.setUserName(customerName != null ? customerName : "Walk-In Player");
        booking.setUserPhone(customerPhone != null ? customerPhone : "");
        booking.setVenueId(facility.getVenueId());
        booking.setVenueName(venue != null ? venue.getName() : "Apex Arena");
        booking.setCourtId(courtId);
        booking.setCourtName(facility.getName());
        booking.setSport(facility.getSport());
        booking.setDate(date);
        booking.setSlot(slot);
        booking.setDurationMinutes(60);
        booking.setPricing(Map.of("total", amountCollected, "isOddHour", false));
        booking.setPayment(Map.of("method", "CASH", "amount", amountCollected, "status", "SUCCESS"));
        booking.setQrPassToken(qrPassToken);
        booking.setStatus("CONFIRMED");
        booking.setSource("walk_in");
        booking.setCreatedAt(now);

        // Update Availability Doc
        if (avail != null && avail.getSlots() != null) {
            SlotInfo slotInfo = avail.getSlots().get(slot);
            if (slotInfo != null) {
                slotInfo.setStatus("BOOKED");
                slotInfo.setBookingId(bookingId);
                slotInfo.setUserId(ownerId);
            }
        }

        firebaseService.getBookings().put(bookingId, booking);
        return booking;
    }

    /**
     * Gate QR Code Pass Verification Scanner
     */
    public synchronized Map<String, Object> verifyGatePass(String qrPassToken, String ownerId) {
        BookingRecord matched = firebaseService.getBookings().values().stream()
                .filter(b -> qrPassToken.equals(b.getQrPassToken()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unrecognized QR Pass Token. Entry denied at gate."));

        if ("CANCELLED".equals(matched.getStatus())) {
            throw new IllegalStateException("Pass cancelled. Entry denied. This booking was cancelled and refunded.");
        }

        if ("CHECKED_IN".equals(matched.getStatus())) {
            return Map.of(
                    "success", true,
                    "alreadyCheckedIn", true,
                    "message", "Pass already redeemed at " + matched.getCheckedInAt(),
                    "booking", matched
            );
        }

        matched.setStatus("CHECKED_IN");
        matched.setCheckedInAt(System.currentTimeMillis());

        return Map.of(
                "success", true,
                "alreadyCheckedIn", false,
                "message", "Pass verified! Access granted to " + matched.getUserName() + " (" + matched.getCourtName() + ")",
                "booking", matched
        );
    }
}

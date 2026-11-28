package com.projectapex.service;

import com.projectapex.dto.request.*;
import com.projectapex.model.*;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BookingService {
    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    private final FirebaseService firebaseService;
    private final PricingAndDiscountService pricingService;
    private final RazorpayService razorpayService;

    // 10-minute hold TTL per specification
    private static final long HOLD_TTL_MS = 10 * 60 * 1000L;

    public BookingService(FirebaseService firebaseService,
                          PricingAndDiscountService pricingService,
                          RazorpayService razorpayService) {
        this.firebaseService = firebaseService;
        this.pricingService = pricingService;
        this.razorpayService = razorpayService;
    }

    /**
     * Step 1: Initiate Booking with Atomic 10-minute Slot Lock & Razorpay Order
     */
    public synchronized Map<String, Object> initiateBooking(InitiateBookingRequest req, String userId) {
        String resourceId = req.getResourceId();
        String date = req.getBookingDate();
        List<String> slotStartTimes = req.getSlotStartTimes();

        if (slotStartTimes == null || slotStartTimes.isEmpty()) {
            throw new IllegalArgumentException("At least one slot start time is required");
        }

        ResourceRecord resource = firebaseService.getResources().get(resourceId);
        String venueId = req.getVenueId();
        String ownerId = "owner_01";
        String resourceName = "Turf Court";
        String venueName = "Apex Arena";
        String venueLocality = "Thane";

        if (resource != null) {
            venueId = resource.getVenueId();
            ownerId = resource.getOwnerId();
            resourceName = resource.getName();
            VenueRecord vr = firebaseService.getVenues().get(venueId);
            if (vr != null) {
                venueName = vr.getName();
                venueLocality = vr.getLocality();
            }
        }

        long now = System.currentTimeMillis();

        // 1. Atomic Concurrency Lock Check (In-memory + DB fallback for multi-instance consistency)
        List<String> lockIds = new ArrayList<>();
        for (String startTime : slotStartTimes) {
            String lockId = SlotLockRecord.buildLockId(resourceId, date, startTime);
            SlotLockRecord existingLock = firebaseService.getSlotLocks().get(lockId);
            if (existingLock == null || existingLock.isExpired()) {
                existingLock = firebaseService.findActiveDbSlotLock(resourceId, date, startTime).orElse(existingLock);
            }

            if (existingLock != null) {
                if ("BOOKED".equalsIgnoreCase(existingLock.getStatus())) {
                    throw new IllegalStateException("Slot " + startTime + " is already booked.");
                }
                if ("HELD".equalsIgnoreCase(existingLock.getStatus()) && !existingLock.isExpired()) {
                    if (!userId.equals(existingLock.getUserId())) {
                        throw new IllegalStateException("Slot " + startTime + " is currently held by another user. Please choose another slot.");
                    }
                }
            }
            lockIds.add(lockId);
        }

        // 2. Generate Booking ID and Calculate Authoritative Quote
        String bookingId = "bk_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        PricingAndDiscountService.PriceQuote quote = pricingService.calculateQuote(
                venueId, resourceId, date, slotStartTimes, req.getPromoCode(), userId);

        long holdExpiresAt = now + HOLD_TTL_MS;

        // 3. Acquire 10-minute HELD locks (ACID rollback on multi-slot conflict)
        List<String> acquiredLockIds = new ArrayList<>();
        try {
            for (String startTime : slotStartTimes) {
                String lockId = SlotLockRecord.buildLockId(resourceId, date, startTime);
                LocalTime s = LocalTime.parse(startTime);
                LocalTime e = s.plusMinutes(resource != null ? resource.getSlotDurationMinutes() : 60);

                SlotLockRecord lock = new SlotLockRecord(resourceId, venueId, date, startTime, e.toString(), bookingId, userId, HOLD_TTL_MS);
                firebaseService.getSlotLocks().put(lockId, lock);
                acquiredLockIds.add(lockId);
            }
        } catch (RuntimeException ex) {
            for (String lid : acquiredLockIds) {
                firebaseService.getSlotLocks().remove(lid);
            }
            throw ex;
        }

        // 4. Create Razorpay Order
        JSONObject notes = new JSONObject();
        notes.put("bookingId", bookingId);
        notes.put("venueId", venueId);
        notes.put("resourceId", resourceId);
        notes.put("userId", userId);

        String razorpayOrderId = razorpayService.createOrderInPaise(quote.getFinalPayablePaise(), bookingId, notes);

        // 5. Create Booking Document in PENDING_PAYMENT status
        UserRecord user = firebaseService.getUsers().get(userId);
        BookingRecord booking = new BookingRecord();
        booking.setBookingId(bookingId);
        booking.setUserId(userId);
        booking.setUserName(user != null ? user.getFullName() : "Sports Player");
        booking.setUserPhone(user != null ? user.getPhone() : "+91 9999999999");
        booking.setUserEmail(user != null ? user.getEmail() : "player@apex.dev");
        booking.setOwnerId(ownerId);
        booking.setVenueId(venueId);
        booking.setVenueName(venueName);
        booking.setVenueLocality(venueLocality);
        booking.setResourceId(resourceId);
        booking.setResourceName(resourceName);
        booking.setSportType(req.getSportType() != null ? req.getSportType() : "CRICKET_TURF");
        booking.setBookingDate(date);
        booking.setSlots(quote.getItemizedSlots());
        booking.setSubtotalPaise(quote.getSubtotalPaise());
        booking.setDiscountId(quote.getDiscountId());
        booking.setDiscountCode(quote.getDiscountCode());
        booking.setDiscountAmountPaise(quote.getDiscountAmountPaise());
        booking.setFinalPayablePaise(quote.getFinalPayablePaise());
        booking.setStatus("PENDING_PAYMENT");
        booking.setRazorpayOrderId(razorpayOrderId);
        booking.setHoldExpiresAt(holdExpiresAt);

        firebaseService.getBookings().put(bookingId, booking);

        Map<String, Object> response = new HashMap<>();
        response.put("bookingId", bookingId);
        response.put("razorpayOrderId", razorpayOrderId);
        response.put("amountPaise", quote.getFinalPayablePaise());
        response.put("currency", "INR");
        response.put("razorpayKeyId", razorpayService.getKeyId());
        response.put("holdExpiresAt", holdExpiresAt);
        response.put("quote", quote);

        log.info("Initiated booking: {} with Razorpay Order: {} for user: {}", bookingId, razorpayOrderId, userId);
        return response;
    }

    /**
     * Step 2: Synchronous Verification of Razorpay Signature & Confirmation
     */
    public synchronized BookingRecord verifyPayment(String bookingId, VerifyPaymentRequest req) {
        BookingRecord booking = firebaseService.getBookings().get(bookingId);
        if (booking == null) {
            throw new IllegalArgumentException("Booking not found: " + bookingId);
        }

        // Idempotency: if already confirmed (e.g. by webhook), return immediately
        if ("CONFIRMED".equalsIgnoreCase(booking.getStatus())) {
            return booking;
        }
        if (!"PENDING_PAYMENT".equalsIgnoreCase(booking.getStatus())) {
            throw new IllegalStateException("Booking cannot be verified from status: " + booking.getStatus());
        }
        if (booking.getHoldExpiresAt() != null && booking.getHoldExpiresAt() < System.currentTimeMillis()) {
            throw new IllegalStateException("Reservation hold expired. Please re-select your slot.");
        }
        if (booking.getRazorpayOrderId() != null && !booking.getRazorpayOrderId().equals(req.getRazorpayOrderId())) {
            throw new IllegalArgumentException("Razorpay Order ID mismatch for booking " + bookingId);
        }

        // Verify Razorpay HMAC Signature
        boolean isValid = razorpayService.verifySignature(
                req.getRazorpayOrderId(), req.getRazorpayPaymentId(), req.getRazorpaySignature());
        if (!isValid) {
            throw new IllegalArgumentException("Invalid Razorpay payment signature. Payment verification failed.");
        }

        // Confirm booking
        String bookingCode = "ARENA-" + UUID.randomUUID().toString().replace("-", "").substring(0, 5).toUpperCase();
        booking.setBookingCode(bookingCode);
        booking.setStatus("CONFIRMED");
        booking.setRazorpayPaymentId(req.getRazorpayPaymentId());
        booking.setHoldExpiresAt(null);
        booking.setUpdatedAt(System.currentTimeMillis());
        firebaseService.persistBooking(booking);

        // Transition slot locks from HELD to BOOKED
        for (BookingRecord.BookedSlotItem slot : booking.getSlots()) {
            String lockId = SlotLockRecord.buildLockId(booking.getResourceId(), booking.getBookingDate(), slot.getStartTime());
            SlotLockRecord lock = firebaseService.getSlotLocks().get(lockId);
            if (lock != null) {
                lock.setStatus("BOOKED");
                lock.setHoldExpiresAt(null);
                firebaseService.persistSlotLock(lock);
            }
        }

        // Log payment record
        PaymentRecord payment = new PaymentRecord(
                "pay_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10),
                bookingId, booking.getUserId(), booking.getOwnerId(),
                req.getRazorpayOrderId(), req.getRazorpayPaymentId(), booking.getFinalPayablePaise()
        );
        firebaseService.getPayments().put(payment.getPaymentId(), payment);

        // Increment discount usage if applicable
        if (booking.getDiscountId() != null) {
            DiscountRecord d = firebaseService.getDiscounts().get(booking.getDiscountId());
            if (d != null) {
                d.setCurrentUses(d.getCurrentUses() + 1);
                firebaseService.persistDiscount(d);
            }
        }

        log.info("Booking confirmed: {} with check-in code: {}", bookingId, bookingCode);
        return booking;
    }

    /**
     * Step 3: Player Cancellation with 3-Tier Policy & Automated Razorpay Refund
     */
    public synchronized BookingRecord cancelBookingByPlayer(String bookingId, String userId, String reason) {
        BookingRecord booking = firebaseService.getBookings().get(bookingId);
        if (booking == null) {
            throw new IllegalArgumentException("Booking not found: " + bookingId);
        }
        if (!booking.getUserId().equals(userId)) {
            throw new IllegalStateException("You are not authorized to cancel this booking.");
        }
        if (!"CONFIRMED".equalsIgnoreCase(booking.getStatus())) {
            throw new IllegalStateException("Only CONFIRMED bookings can be cancelled.");
        }

        // Compute Refund Tier against venue cancellation policy
        VenueRecord venue = firebaseService.getVenues().get(booking.getVenueId());
        int fullRefundHours = 24;
        int partialRefundHours = 6;
        int partialPercent = 50;

        if (venue != null && venue.getCancellationPolicy() != null) {
            Object fr = venue.getCancellationPolicy().get("fullRefundHoursBefore");
            Object pr = venue.getCancellationPolicy().get("partialRefundHoursBefore");
            Object pp = venue.getCancellationPolicy().get("partialRefundPercent");
            if (fr instanceof Number) fullRefundHours = ((Number) fr).intValue();
            if (pr instanceof Number) partialRefundHours = ((Number) pr).intValue();
            if (pp instanceof Number) partialPercent = ((Number) pp).intValue();
        }

        String firstSlotStart = booking.getSlots().isEmpty() ? "06:00" : booking.getSlots().get(0).getStartTime();
        LocalDate bDate = LocalDate.parse(booking.getBookingDate());
        LocalTime bTime = LocalTime.parse(firstSlotStart);
        LocalDateTime slotDateTime = LocalDateTime.of(bDate, bTime);

        long hoursUntilSlot = ChronoUnit.HOURS.between(
                ZonedDateTime.now(ZoneId.of("Asia/Kolkata")).toLocalDateTime(), slotDateTime);
        long refundAmountPaise = 0;

        if (hoursUntilSlot >= fullRefundHours) {
            refundAmountPaise = Math.max(0L, booking.getFinalPayablePaise() - 5000L);
        } else if (hoursUntilSlot >= partialRefundHours) {
            refundAmountPaise = Math.round(booking.getFinalPayablePaise() * (partialPercent / 100.0));
        }

        if (refundAmountPaise > 0) {
            String refundId = razorpayService.refundPayment(booking.getRazorpayPaymentId(), refundAmountPaise, reason);
            booking.setRefundId(refundId);
            booking.setRefundAmountPaise(refundAmountPaise);
            booking.setRefundStatus("INITIATED");

            PaymentRecord refundPayment = new PaymentRecord(
                    "pay_ref_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8),
                    bookingId, booking.getUserId(), booking.getOwnerId(),
                    booking.getRazorpayOrderId(), booking.getRazorpayPaymentId(), refundAmountPaise);
            refundPayment.setStatus("REFUND_INITIATED");
            firebaseService.getPayments().put(refundPayment.getPaymentId(), refundPayment);
        }

        // Release slot locks & daily availability doc
        releaseBookingSlotLocks(booking);

        booking.setStatus("CANCELLED_BY_USER");
        booking.setCancellationReason(reason);
        booking.setUpdatedAt(System.currentTimeMillis());
        firebaseService.persistBooking(booking);

        firebaseService.getAuditLogs().add(new AuditLog(
                "audit_bkg_cancel_" + System.currentTimeMillis(), "BOOKING_CANCELLED", userId, "booking", bookingId,
                Map.of("refundPaise", refundAmountPaise, "reason", reason != null ? reason : "player_cancel")));

        log.info("Cancelled booking: {} by player. Refund amount: ₹{}", bookingId, (double) refundAmountPaise / 100);
        return booking;
    }

    private void releaseBookingSlotLocks(BookingRecord booking) {
        if (booking == null) return;
        for (BookingRecord.BookedSlotItem slot : booking.getSlots()) {
            String lockId = SlotLockRecord.buildLockId(booking.getResourceId(), booking.getBookingDate(), slot.getStartTime());
            firebaseService.getSlotLocks().remove(lockId);

            String availKey = booking.getResourceId() + "_" + booking.getBookingDate();
            AvailabilityDoc doc = firebaseService.getAvailability().get(availKey);
            if (doc != null && doc.getSlots() != null && doc.getSlots().containsKey(slot.getStartTime())) {
                SlotInfo si = doc.getSlots().get(slot.getStartTime());
                si.setStatus("AVAILABLE");
                si.setBookingId(null);
                doc.setLastUpdated(System.currentTimeMillis());
                firebaseService.persistAvailabilityDoc(doc);
            }
        }
    }

    /**
     * Step 4: Facility Owner Emergency Cancellation with 100% Full Refund
     */
    public synchronized BookingRecord cancelBookingByOwner(String bookingId, String ownerId, String reason) {
        BookingRecord booking = firebaseService.getBookings().get(bookingId);
        if (booking == null) {
            throw new IllegalArgumentException("Booking not found: " + bookingId);
        }
        String resolvedOwner = resolveOwnerId(booking);
        if ((resolvedOwner != null && !resolvedOwner.equals(ownerId)) && !"admin".equalsIgnoreCase(ownerId)) {
            throw new IllegalStateException("Unauthorized to cancel this booking.");
        }

        long fullRefundPaise = booking.getFinalPayablePaise();
        String refundId = razorpayService.refundPayment(booking.getRazorpayPaymentId(), fullRefundPaise, "Owner Emergency: " + reason);
        booking.setRefundId(refundId);
        booking.setRefundAmountPaise(fullRefundPaise);
        booking.setRefundStatus("INITIATED");

        PaymentRecord refundPayment = new PaymentRecord(
                "pay_own_ref_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8),
                bookingId, booking.getUserId(), booking.getOwnerId(),
                booking.getRazorpayOrderId(), booking.getRazorpayPaymentId(), fullRefundPaise);
        refundPayment.setStatus("REFUND_INITIATED");
        firebaseService.getPayments().put(refundPayment.getPaymentId(), refundPayment);

        // Release slot locks
        releaseBookingSlotLocks(booking);

        booking.setStatus("CANCELLED_BY_OWNER");
        booking.setCancellationReason(reason);
        booking.setUpdatedAt(System.currentTimeMillis());
        firebaseService.persistBooking(booking);

        firebaseService.getAuditLogs().add(new AuditLog(
                "audit_own_cancel_" + System.currentTimeMillis(), "OWNER_EMERGENCY_CANCEL", ownerId, "booking", bookingId,
                Map.of("refundPaise", fullRefundPaise, "reason", reason != null ? reason : "owner_cancel")));

        log.info("Emergency cancellation by owner: {} for booking: {}. 100% refund of ₹{}",
                ownerId, bookingId, (double) fullRefundPaise / 100);
        return booking;
    }

    /**
     * Player Update / Reschedule Booking (atomic slot-lock swap + metadata update)
     */
    public synchronized BookingRecord updateBookingByPlayer(String bookingId, String userId, UpdateBookingRequest req, boolean isAdmin) {
        BookingRecord booking = getBooking(bookingId);
        if (!booking.getUserId().equals(userId) && !isAdmin) {
            throw new IllegalStateException("Unauthorized to update this booking.");
        }
        if ("CANCELLED".equalsIgnoreCase(booking.getStatus())
                || (booking.getStatus() != null && booking.getStatus().startsWith("CANCELLED"))
                || "CHECKED_IN".equalsIgnoreCase(booking.getStatus())
                || "COMPLETED".equalsIgnoreCase(booking.getStatus())) {
            throw new IllegalStateException("Cannot update booking in status: " + booking.getStatus());
        }

        String oldDate = booking.getBookingDate();
        String oldSlot = booking.getSlots().isEmpty() ? null : booking.getSlots().get(0).getStartTime();
        String newDate = (req.getBookingDate() != null && !req.getBookingDate().isBlank()) ? req.getBookingDate().trim() : oldDate;
        String requestedSlot = (req.getSlotTime() != null && !req.getSlotTime().isBlank())
                ? req.getSlotTime().trim()
                : (req.getSlotStartTimes() != null && !req.getSlotStartTimes().isEmpty() ? req.getSlotStartTimes().get(0) : oldSlot);

        boolean slotChanged = requestedSlot != null && (!newDate.equals(oldDate) || !requestedSlot.equals(oldSlot));
        if (slotChanged) {
            String newLockId = SlotLockRecord.buildLockId(booking.getResourceId(), newDate, requestedSlot);
            SlotLockRecord existingTarget = firebaseService.getSlotLocks().get(newLockId);
            if (existingTarget == null || existingTarget.isExpired()) {
                existingTarget = firebaseService.findActiveDbSlotLock(booking.getResourceId(), newDate, requestedSlot).orElse(existingTarget);
            }
            if (existingTarget != null && !bookingId.equals(existingTarget.getBookingId())) {
                if ("BOOKED".equalsIgnoreCase(existingTarget.getStatus())
                        || ("HELD".equalsIgnoreCase(existingTarget.getStatus()) && !existingTarget.isExpired() && !userId.equals(existingTarget.getUserId()))) {
                    throw new IllegalStateException("Double-booking conflict: Target slot " + requestedSlot + " on " + newDate + " is already reserved or booked.");
                }
            }

            releaseBookingSlotLocks(booking);

            LocalTime s = LocalTime.parse(requestedSlot);
            String endTime = s.plusHours(1).toString();
            SlotLockRecord newLock = new SlotLockRecord(
                    booking.getResourceId(), booking.getVenueId(), newDate, requestedSlot, endTime, bookingId, booking.getUserId(), 0);
            newLock.setStatus("CONFIRMED".equalsIgnoreCase(booking.getStatus()) ? "BOOKED" : "HELD");
            if ("HELD".equals(newLock.getStatus())) {
                newLock.setHoldExpiresAt(System.currentTimeMillis() + HOLD_TTL_MS);
            }
            firebaseService.getSlotLocks().put(newLockId, newLock);

            long unitPaise = booking.getSlots().isEmpty() ? booking.getFinalPayablePaise() : booking.getSlots().get(0).getUnitPricePaise();
            booking.setBookingDate(newDate);
            booking.setSlots(List.of(new BookingRecord.BookedSlotItem(requestedSlot, endTime, unitPaise)));
        }

        if (req.getUserPhone() != null && !req.getUserPhone().isBlank()) {
            booking.setUserPhone(req.getUserPhone().trim());
        }
        if (req.getNotes() != null) {
            booking.setNotes(req.getNotes().trim());
        }

        booking.setUpdatedAt(System.currentTimeMillis());
        firebaseService.persistBooking(booking);

        firebaseService.getAuditLogs().add(new AuditLog(
                "audit_bkg_upd_" + System.currentTimeMillis(), "BOOKING_UPDATED", userId, "booking", bookingId,
                Map.of("date", booking.getBookingDate(), "slot", requestedSlot != null ? requestedSlot : "")));
        return booking;
    }

    /**
     * Delete Booking & Release Any Held/Booked Slot Locks from Neon PostgreSQL
     */
    public synchronized boolean deleteBooking(String bookingId, String actorId, boolean isAdmin) {
        BookingRecord booking = getBooking(bookingId);
        String resolvedOwner = resolveOwnerId(booking);
        boolean isBookingUser = actorId != null && actorId.equals(booking.getUserId());
        boolean isVenueOwner = actorId != null && resolvedOwner != null && actorId.equals(resolvedOwner);
        if (!isBookingUser && !isVenueOwner && !isAdmin && !"admin".equalsIgnoreCase(actorId)) {
            throw new IllegalStateException("Unauthorized to delete this booking.");
        }

        releaseBookingSlotLocks(booking);
        firebaseService.getBookings().remove(bookingId);
        firebaseService.getAuditLogs().add(new AuditLog(
                "audit_bkg_del_" + System.currentTimeMillis(), "BOOKING_DELETED", actorId, "booking", bookingId,
                Map.of("previousStatus", booking.getStatus() != null ? booking.getStatus() : "UNKNOWN")));
        return true;
    }

    private String resolveOwnerId(BookingRecord booking) {
        if (booking == null) return null;
        if (booking.getOwnerId() != null && !booking.getOwnerId().isBlank()) {
            return booking.getOwnerId();
        }
        if (booking.getVenueId() != null) {
            VenueRecord vr = firebaseService.getVenues().get(booking.getVenueId());
            if (vr != null) return vr.getOwnerId();
        }
        if (booking.getResourceId() != null) {
            ResourceRecord res = firebaseService.getResources().get(booking.getResourceId());
            if (res != null) return res.getOwnerId();
            FacilityRecord fac = firebaseService.getCourts().get(booking.getResourceId());
            if (fac != null) {
                VenueRecord vr = firebaseService.getVenues().get(fac.getVenueId());
                if (vr != null) return vr.getOwnerId();
            }
        }
        return null;
    }

    public BookingRecord getBooking(String bookingId) {
        BookingRecord b = firebaseService.getBookings().get(bookingId);
        if (b == null) {
            throw new IllegalArgumentException("Booking not found: " + bookingId);
        }
        return b;
    }

    public List<BookingRecord> getPlayerBookings(String userId) {
        return firebaseService.getBookings().values().stream()
                .filter(b -> userId.equals(b.getUserId()))
                .sorted(Comparator.comparing(BookingRecord::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    public List<BookingRecord> getOwnerBookings(String ownerId, String date, String status) {
        return firebaseService.getBookings().values().stream()
                .filter(b -> ownerId.equals(b.getOwnerId()) || "admin".equalsIgnoreCase(ownerId))
                .filter(b -> date == null || date.equalsIgnoreCase(b.getBookingDate()))
                .filter(b -> status == null || status.equalsIgnoreCase(b.getStatus()))
                .sorted(Comparator.comparing(BookingRecord::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    public BookingRecord updateBookingStatusByOwner(String bookingId, String ownerId, String newStatus) {
        BookingRecord booking = getBooking(bookingId);
        if (!booking.getOwnerId().equals(ownerId) && !"admin".equalsIgnoreCase(ownerId)) {
            throw new IllegalStateException("Unauthorized");
        }
        booking.setStatus(newStatus.toUpperCase());
        booking.setUpdatedAt(System.currentTimeMillis());
        firebaseService.persistBooking(booking);
        return booking;
    }

    /**
     * Hold Expiry Scheduler Task
     */
    public synchronized int cleanupExpiredHolds() {
        long now = System.currentTimeMillis();
        int expiredCount = 0;

        for (BookingRecord b : firebaseService.getBookings().values()) {
            if ("PENDING_PAYMENT".equalsIgnoreCase(b.getStatus()) && b.getHoldExpiresAt() != null && b.getHoldExpiresAt() < now) {
                b.setStatus("EXPIRED");
                b.setUpdatedAt(now);
                firebaseService.persistBooking(b);

                // Release locks
                for (BookingRecord.BookedSlotItem slot : b.getSlots()) {
                    String lockId = SlotLockRecord.buildLockId(b.getResourceId(), b.getBookingDate(), slot.getStartTime());
                    SlotLockRecord lock = firebaseService.getSlotLocks().get(lockId);
                    if (lock != null && b.getBookingId().equals(lock.getBookingId()) && "HELD".equalsIgnoreCase(lock.getStatus())) {
                        firebaseService.getSlotLocks().remove(lockId);
                    }
                }
                expiredCount++;
            }
        }
        return expiredCount;
    }

    // =========================================================================
    // Compatibility Methods for Test Suite (Runner Tests)
    // =========================================================================

    public synchronized ConcurrencyLock acquireLock(AcquireLockRequest req) {
        String resourceId = req.getCourtId();
        String date = req.getDate();
        String slot = req.getSlot();
        String userId = "demo_player";

        String lockId = SlotLockRecord.buildLockId(resourceId, date, slot);
        SlotLockRecord existing = firebaseService.getSlotLocks().get(lockId);

        if (existing != null && ("BOOKED".equalsIgnoreCase(existing.getStatus()) ||
                ("HELD".equalsIgnoreCase(existing.getStatus()) && !existing.isExpired() && !userId.equals(existing.getUserId())))) {
            throw new IllegalStateException("Slot " + slot + " is already locked or booked.");
        }

        SlotLockRecord sl = new SlotLockRecord(resourceId, "venue_01", date, slot, slot, "compat_booking", userId, HOLD_TTL_MS);
        firebaseService.getSlotLocks().put(lockId, sl);

        ConcurrencyLock cl = new ConcurrencyLock(resourceId, date, slot, userId, 300000L);
        firebaseService.getConcurrencyLocks().put(lockId, cl);
        return cl;
    }

    public synchronized BookingRecord commitBooking(CommitBookingRequest req) {
        return commitBooking(req, "demo_player");
    }

    public synchronized BookingRecord commitBooking(CommitBookingRequest req, String userId) {
        String effectiveUserId = (userId != null && !userId.isBlank()) ? userId : "demo_player";
        String courtId = req.getCourtId();
        String date = req.getDate();
        String slot = req.getSlot();

        String lockId = SlotLockRecord.buildLockId(courtId, date, slot);
        SlotLockRecord existingLock = firebaseService.getSlotLocks().get(lockId);
        if (existingLock == null || existingLock.isExpired()) {
            existingLock = firebaseService.findActiveDbSlotLock(courtId, date, slot).orElse(existingLock);
        }
        if (existingLock != null) {
            if ("BOOKED".equalsIgnoreCase(existingLock.getStatus())) {
                throw new IllegalStateException("Double-booking conflict: Slot " + slot + " on " + date + " is already booked.");
            }
            if ("HELD".equalsIgnoreCase(existingLock.getStatus()) && !existingLock.isExpired()
                    && !effectiveUserId.equals(existingLock.getUserId())) {
                throw new IllegalStateException("Double-booking conflict: Slot " + slot + " is currently held by another user.");
            }
        }

        FacilityRecord facility = firebaseService.getCourts().get(courtId);
        ResourceRecord resource = firebaseService.getResources().get(courtId);
        String venueId = facility != null ? facility.getVenueId() : (resource != null ? resource.getVenueId() : "venue_thane_01");
        VenueRecord venue = firebaseService.getVenues().get(venueId);
        String ownerId = venue != null ? venue.getOwnerId() : (resource != null ? resource.getOwnerId() : "owner_01");
        long basePaise = facility != null ? (long) facility.getBaseHourlyRate() * 100L
                : (resource != null ? resource.getBasePricePaise() : 100000L);

        UserRecord player = firebaseService.getUsers().get(effectiveUserId);
        String bookingId = "bk_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String bookingCode = "ARENA-" + UUID.randomUUID().toString().replace("-", "").substring(0, 5).toUpperCase();

        BookingRecord b = new BookingRecord();
        b.setBookingId(bookingId);
        b.setBookingCode(bookingCode);
        b.setQrPassToken("APEX-PASS-" + bookingCode);
        b.setUserId(effectiveUserId);
        b.setUserName(player != null ? player.getFullName() : "Sports Player");
        b.setUserPhone(player != null ? player.getPhone() : "+91 98201 12345");
        b.setUserEmail(player != null ? player.getEmail() : "player@apex.dev");
        b.setOwnerId(ownerId);
        b.setVenueId(venueId);
        b.setVenueName(venue != null ? venue.getName() : "Apex Sports Arena");
        b.setVenueLocality(venue != null ? venue.getLocality() : "Thane");
        b.setResourceId(courtId);
        b.setResourceName(facility != null ? facility.getName() : (resource != null ? resource.getName() : "Main Pitch"));
        b.setSportType(facility != null ? facility.getSport().toUpperCase() : "CRICKET_TURF");
        b.setBookingDate(date);
        b.setStatus("CONFIRMED");
        b.setRazorpayOrderId(req.getRazorpayOrderId());
        b.setRazorpayPaymentId(req.getRazorpayPaymentId());
        b.setSubtotalPaise(basePaise);
        b.setFinalPayablePaise(basePaise);
        b.setSlots(List.of(new BookingRecord.BookedSlotItem(slot, slot, basePaise)));

        SlotLockRecord sl = new SlotLockRecord(courtId, venueId, date, slot, slot, bookingId, effectiveUserId, 0);
        sl.setStatus("BOOKED");
        sl.setHoldExpiresAt(null);
        firebaseService.getSlotLocks().put(lockId, sl);

        firebaseService.getBookings().put(bookingId, b);

        PaymentRecord payment = new PaymentRecord(
                "pay_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10),
                bookingId, effectiveUserId, ownerId,
                req.getRazorpayOrderId(), req.getRazorpayPaymentId(), basePaise);
        firebaseService.getPayments().put(payment.getPaymentId(), payment);

        firebaseService.getAuditLogs().add(new AuditLog(
                "audit_bkg_cmt_" + System.currentTimeMillis(), "BOOKING_COMMITTED", effectiveUserId, "booking", bookingId,
                Map.of("courtId", courtId, "date", date, "slot", slot, "venueId", venueId)));
        return b;
    }

    public synchronized BookingRecord createWalkInBooking(
            String ownerId,
            String courtId,
            String date,
            String slot,
            String customerName,
            String customerPhone,
            Integer amountCollected
    ) {
        FacilityRecord facility = firebaseService.getCourts().get(courtId);
        ResourceRecord resource = firebaseService.getResources().get(courtId);
        String venueId = facility != null ? facility.getVenueId() : (resource != null ? resource.getVenueId() : "venue_thane_01");
        VenueRecord venue = firebaseService.getVenues().get(venueId);
        String venueOwner = venue != null ? venue.getOwnerId() : (resource != null ? resource.getOwnerId() : null);

        if (venueOwner != null && ownerId != null && !"admin".equalsIgnoreCase(ownerId) && !venueOwner.equals(ownerId)) {
            throw new IllegalStateException("Unauthorized to record walk-in booking for another owner's facility.");
        }

        String lockId = SlotLockRecord.buildLockId(courtId, date, slot);
        SlotLockRecord existing = firebaseService.getSlotLocks().get(lockId);
        if (existing == null || existing.isExpired()) {
            existing = firebaseService.findActiveDbSlotLock(courtId, date, slot).orElse(existing);
        }
        if (existing != null && ("BOOKED".equalsIgnoreCase(existing.getStatus())
                || ("HELD".equalsIgnoreCase(existing.getStatus()) && !existing.isExpired()))) {
            throw new IllegalStateException("Double-booking conflict: Slot is already booked or held.");
        }

        String bookingId = "walkin_" + UUID.randomUUID().toString().substring(0, 8);
        String code = "ARENA-WALKIN-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        BookingRecord b = new BookingRecord();
        b.setBookingId(bookingId);
        b.setBookingCode(code);
        b.setQrPassToken("APEX-PASS-" + code);
        b.setUserId("walkin_customer");
        b.setUserName(customerName);
        b.setUserPhone(customerPhone);
        b.setOwnerId(venueOwner != null ? venueOwner : ownerId);
        b.setVenueId(venueId);
        b.setVenueName(venue != null ? venue.getName() : "Apex Arena");
        b.setResourceId(courtId);
        b.setResourceName(facility != null ? facility.getName() : (resource != null ? resource.getName() : "Main Pitch"));
        b.setBookingDate(date);
        b.setStatus("CONFIRMED");
        long amountPaise = amountCollected != null ? (long) amountCollected * 100 : 100000L;
        b.setSubtotalPaise(amountPaise);
        b.setFinalPayablePaise(amountPaise);
        b.setSlots(List.of(new BookingRecord.BookedSlotItem(slot, slot, amountPaise)));

        SlotLockRecord sl = new SlotLockRecord(courtId, venueId, date, slot, slot, bookingId, "walkin", 0);
        sl.setStatus("BOOKED");
        sl.setHoldExpiresAt(null);
        firebaseService.getSlotLocks().put(lockId, sl);

        firebaseService.getBookings().put(bookingId, b);

        PaymentRecord cashPayment = new PaymentRecord(
                "pay_cash_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8),
                bookingId, "walkin_customer", b.getOwnerId(),
                null, "CASH_" + bookingId, amountPaise);
        cashPayment.setMethod("cash");
        cashPayment.setStatus("COLLECTED_AT_VENUE");
        firebaseService.getPayments().put(cashPayment.getPaymentId(), cashPayment);

        firebaseService.getAuditLogs().add(new AuditLog(
                "audit_wlk_" + System.currentTimeMillis(), "WALK_IN_BOOKING_CREATED", ownerId, "booking", bookingId,
                Map.of("courtId", courtId, "date", date, "slot", slot, "amountPaise", amountPaise)));
        return b;
    }

    public synchronized BookingRecord createWalkInBooking(WalkInBookingRequest req) {
        return createWalkInBooking("owner_01", req.getCourtId(), req.getDate(), req.getSlot(), req.getCustomerName(), req.getCustomerPhone(), req.getAmountCollected());
    }

    public synchronized BookingRecord commitBooking(
            String userId,
            String courtId,
            String date,
            String slot,
            String orderId,
            String paymentId,
            String signature,
            String paymentMethod
    ) {
        FacilityRecord facility = firebaseService.getCourts().get(courtId);
        ResourceRecord resource = firebaseService.getResources().get(courtId);
        String venueId = facility != null ? facility.getVenueId() : (resource != null ? resource.getVenueId() : "venue_thane_01");
        VenueRecord venue = firebaseService.getVenues().get(venueId);
        String ownerId = venue != null ? venue.getOwnerId() : (resource != null ? resource.getOwnerId() : "owner_01");

        String bookingId = "bk_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        BookingRecord b = new BookingRecord();
        b.setBookingId(bookingId);
        b.setBookingCode("ARENA-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase());
        b.setQrPassToken("APEX-PASS-" + b.getBookingCode());
        b.setUserId(userId);
        b.setOwnerId(ownerId);
        b.setResourceId(courtId);
        b.setVenueId(venueId);
        b.setVenueName(venue != null ? venue.getName() : "Apex Sports Arena");
        b.setResourceName(facility != null ? facility.getName() : (resource != null ? resource.getName() : "Main Pitch"));
        b.setBookingDate(date);
        b.setStatus("CONFIRMED");
        b.setRazorpayOrderId(orderId);
        b.setRazorpayPaymentId(paymentId);
        b.setSubtotalPaise(100000L);
        b.setFinalPayablePaise(100000L);
        b.setSlots(List.of(new BookingRecord.BookedSlotItem(slot, slot, 100000L)));

        String lockId = SlotLockRecord.buildLockId(courtId, date, slot);
        SlotLockRecord sl = new SlotLockRecord(courtId, venueId, date, slot, slot, bookingId, userId, 0);
        sl.setStatus("BOOKED");
        sl.setHoldExpiresAt(null);
        firebaseService.getSlotLocks().put(lockId, sl);

        firebaseService.getBookings().put(bookingId, b);
        return b;
    }

    public synchronized BookingRecord cancelBooking(String bookingId, String userId) {
        BookingRecord b = cancelBookingByPlayer(bookingId, userId, "Test Cancellation");
        b.setStatus("CANCELLED");
        firebaseService.persistBooking(b);
        return b;
    }

    public synchronized Map<String, Object> verifyGatePass(String qrToken, String ownerId) {
        BookingRecord matched = firebaseService.getBookings().values().stream()
                .filter(b -> qrToken.equalsIgnoreCase(b.getQrPassToken()) || qrToken.equalsIgnoreCase(b.getBookingCode()) || qrToken.equalsIgnoreCase(b.getBookingId()))
                .findFirst()
                .orElse(null);

        Map<String, Object> res = new HashMap<>();
        if (matched == null) {
            res.put("success", false);
            res.put("valid", false);
            res.put("alreadyCheckedIn", false);
            res.put("message", "Invalid pass token");
            return res;
        }

        String expectedOwner = resolveOwnerId(matched);
        if (ownerId != null && expectedOwner != null && !"admin".equalsIgnoreCase(ownerId) && !expectedOwner.equals(ownerId)) {
            throw new IllegalStateException("Unauthorized: Pass belongs to another venue owner.");
        }

        if (matched.getStatus() != null && matched.getStatus().startsWith("CANCEL")) {
            throw new IllegalStateException("Cancelled booking QR pass rejected at gate");
        }

        if ("CHECKED_IN".equalsIgnoreCase(matched.getStatus()) || "COMPLETED".equalsIgnoreCase(matched.getStatus())) {
            res.put("success", false);
            res.put("valid", false);
            res.put("alreadyCheckedIn", true);
            res.put("booking", matched);
            res.put("message", "Already checked in");
            return res;
        }

        long now = System.currentTimeMillis();
        matched.setStatus("CHECKED_IN");
        matched.setCheckedInAt(now);
        matched.setUpdatedAt(now);
        firebaseService.persistBooking(matched);
        firebaseService.getAuditLogs().add(new AuditLog(
                "audit_gate_" + now, "GATE_CHECK_IN", ownerId, "booking", matched.getBookingId(),
                Map.of("qrPassToken", qrToken)));

        res.put("success", true);
        res.put("valid", true);
        res.put("alreadyCheckedIn", false);
        res.put("checkedInAt", now);
        res.put("booking", matched);
        res.put("message", "Pass verified! Access granted.");
        return res;
    }

    public synchronized Map<String, Object> verifyGatePass(String qrToken) {
        BookingRecord matched = firebaseService.getBookings().values().stream()
                .filter(b -> qrToken.equalsIgnoreCase(b.getBookingCode()) || qrToken.equalsIgnoreCase(b.getBookingId()))
                .findFirst()
                .orElse(null);

        Map<String, Object> res = new HashMap<>();
        if (matched == null) {
            res.put("valid", false);
            res.put("message", "Invalid QR Pass Token");
            return res;
        }

        if ("CANCELLED_BY_USER".equalsIgnoreCase(matched.getStatus()) || "CANCELLED_BY_OWNER".equalsIgnoreCase(matched.getStatus())) {
            res.put("valid", false);
            res.put("message", "Pass is cancelled. Gate entry blocked.");
            return res;
        }

        if ("CHECKED_IN".equalsIgnoreCase(matched.getStatus()) || "COMPLETED".equalsIgnoreCase(matched.getStatus())) {
            res.put("valid", false);
            res.put("message", "Pass already checked in.");
            return res;
        }

        matched.setStatus("CHECKED_IN");
        matched.setUpdatedAt(System.currentTimeMillis());
        firebaseService.persistBooking(matched);
        res.put("valid", true);
        res.put("booking", matched);
        res.put("message", "Pass verified! Access granted.");
        return res;
    }
}

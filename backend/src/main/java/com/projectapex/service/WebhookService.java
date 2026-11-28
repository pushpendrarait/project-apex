package com.projectapex.service;

import com.projectapex.model.*;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class WebhookService {
    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);

    private final FirebaseService firebaseService;
    private final RazorpayService razorpayService;

    public WebhookService(FirebaseService firebaseService, RazorpayService razorpayService) {
        this.firebaseService = firebaseService;
        this.razorpayService = razorpayService;
    }

    /**
     * Process asynchronous Razorpay Webhook event with raw payload HMAC verification
     */
    public synchronized boolean processWebhook(String rawPayload, String signatureHeader, String eventId) {
        // 1. Verify webhook signature against raw byte payload
        if (!razorpayService.verifyWebhookSignature(rawPayload, signatureHeader)) {
            log.warn("Rejected Razorpay Webhook: Invalid HMAC Signature");
            throw new IllegalArgumentException("Invalid Razorpay Webhook Signature");
        }

        // 2. Idempotent deduplication check
        if (eventId != null && firebaseService.getWebhookEvents().containsKey(eventId)) {
            log.info("Webhook event: {} already processed. Skipping duplicate.", eventId);
            return true;
        }

        JSONObject payload = new JSONObject(rawPayload);
        String event = payload.optString("event");
        log.info("Processing Razorpay Webhook: {} (Event ID: {})", event, eventId);

        if ("order.paid".equalsIgnoreCase(event) || "payment.captured".equalsIgnoreCase(event)) {
            JSONObject paymentEntity = payload.optJSONObject("payload") != null
                    ? payload.getJSONObject("payload").optJSONObject("payment") != null
                    ? payload.getJSONObject("payload").getJSONObject("payment").optJSONObject("entity") : null : null;

            if (paymentEntity != null) {
                String paymentId = paymentEntity.optString("id");
                String orderId = paymentEntity.optString("order_id");
                JSONObject notes = paymentEntity.optJSONObject("notes");
                String bookingId = notes != null ? notes.optString("bookingId") : null;

                if (bookingId != null) {
                    handlePaymentSuccess(bookingId, paymentId, orderId);
                }
            }
        } else if ("payment.failed".equalsIgnoreCase(event)) {
            JSONObject paymentEntity = payload.optJSONObject("payload") != null
                    ? payload.getJSONObject("payload").optJSONObject("payment") != null
                    ? payload.getJSONObject("payload").getJSONObject("payment").optJSONObject("entity") : null : null;

            if (paymentEntity != null) {
                JSONObject notes = paymentEntity.optJSONObject("notes");
                String bookingId = notes != null ? notes.optString("bookingId") : null;
                if (bookingId != null) {
                    handlePaymentFailure(bookingId);
                }
            }
        }

        // Store webhook event record
        if (eventId != null) {
            firebaseService.getWebhookEvents().put(eventId, new WebhookEventRecord(eventId, event, "PROCESSED"));
        }

        return true;
    }

    private void handlePaymentSuccess(String bookingId, String paymentId, String orderId) {
        BookingRecord booking = firebaseService.getBookings().get(bookingId);
        if (booking == null) return;

        if ("CONFIRMED".equalsIgnoreCase(booking.getStatus())) {
            log.info("Booking: {} already confirmed. Webhook is idempotent.", bookingId);
            return;
        }

        long now = System.currentTimeMillis();

        // Late payment after hold expiry edge case
        if (booking.getHoldExpiresAt() != null && booking.getHoldExpiresAt() < now) {
            boolean conflict = false;
            for (BookingRecord.BookedSlotItem slot : booking.getSlots()) {
                String lockId = SlotLockRecord.buildLockId(booking.getResourceId(), booking.getBookingDate(), slot.getStartTime());
                SlotLockRecord existing = firebaseService.getSlotLocks().get(lockId);
                if (existing != null && !bookingId.equals(existing.getBookingId()) && "BOOKED".equalsIgnoreCase(existing.getStatus())) {
                    conflict = true;
                    break;
                }
            }

            if (conflict) {
                log.warn("Late payment conflict for booking: {}. Triggering auto-refund.", bookingId);
                booking.setStatus("CONFLICT_REFUND_PENDING");
                String refundId = razorpayService.refundPayment(paymentId, booking.getFinalPayablePaise(), "Late payment after hold expiry conflict");
                booking.setRefundId(refundId);
                booking.setRefundAmountPaise(booking.getFinalPayablePaise());
                booking.setRefundStatus("INITIATED");
                booking.setUpdatedAt(now);
                firebaseService.persistBooking(booking);

                PaymentRecord refundPayment = new PaymentRecord(
                        "pay_ref_" + UUID.randomUUID().toString().substring(0, 8),
                        bookingId, booking.getUserId(), booking.getOwnerId(),
                        orderId, paymentId, booking.getFinalPayablePaise());
                refundPayment.setStatus("REFUND_INITIATED");
                firebaseService.getPayments().put(refundPayment.getPaymentId(), refundPayment);
                return;
            }
        }

        // Confirm booking
        String bookingCode = "ARENA-" + UUID.randomUUID().toString().replace("-", "").substring(0, 5).toUpperCase();
        booking.setBookingCode(bookingCode);
        booking.setStatus("CONFIRMED");
        booking.setRazorpayPaymentId(paymentId);
        booking.setHoldExpiresAt(null);
        booking.setUpdatedAt(now);
        firebaseService.persistBooking(booking);

        // Transition slot locks
        for (BookingRecord.BookedSlotItem slot : booking.getSlots()) {
            String lockId = SlotLockRecord.buildLockId(booking.getResourceId(), booking.getBookingDate(), slot.getStartTime());
            SlotLockRecord lock = firebaseService.getSlotLocks().get(lockId);
            if (lock != null) {
                lock.setStatus("BOOKED");
                lock.setHoldExpiresAt(null);
                firebaseService.persistSlotLock(lock);
            }
        }

        // Record payment
        PaymentRecord p = new PaymentRecord("pay_" + UUID.randomUUID().toString().substring(0, 8),
                bookingId, booking.getUserId(), booking.getOwnerId(), orderId, paymentId, booking.getFinalPayablePaise());
        firebaseService.getPayments().put(p.getPaymentId(), p);

        log.info("Confirmed booking: {} via Webhook with code: {}", bookingId, bookingCode);
    }

    private void handlePaymentFailure(String bookingId) {
        BookingRecord booking = firebaseService.getBookings().get(bookingId);
        if (booking == null) return;

        booking.setStatus("PAYMENT_FAILED");
        booking.setUpdatedAt(System.currentTimeMillis());
        firebaseService.persistBooking(booking);

        // Release locks
        for (BookingRecord.BookedSlotItem slot : booking.getSlots()) {
            String lockId = SlotLockRecord.buildLockId(booking.getResourceId(), booking.getBookingDate(), slot.getStartTime());
            SlotLockRecord lock = firebaseService.getSlotLocks().get(lockId);
            if (lock != null && bookingId.equals(lock.getBookingId())) {
                firebaseService.getSlotLocks().remove(lockId);
            }
        }
        log.info("Released slot locks for failed booking: {}", bookingId);
    }
}

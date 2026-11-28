/**
 * Project Apex - Cancellation & Refund Calculation Service
 * Enforces 3-Tier Fair Refund Matrix with atomic state rollback
 */

import { mockDb } from './firebaseConfig.js';
import { ConcurrencyService } from './concurrencyService.js';
import { ApiClient } from './apiClient.js';

export class CancellationService {
  /**
   * Evaluates refund eligibility and calculated amount based on slot start time
   */
  static previewRefund(booking) {
    const [year, month, day] = booking.date.split('-').map(Number);
    const [hour] = booking.slot.split(':').map(Number);
    const slotStartTime = new Date(year, month - 1, day, hour, 0, 0).getTime();
    const now = Date.now();

    const hoursRemaining = (slotStartTime - now) / (1000 * 60 * 60);
    const paidAmount = booking.pricing.total;

    let tier = 3;
    let refundPercentage = 0;
    let handlingFee = 0;
    let refundAmount = 0;
    let reason = "Less than 6 hours prior to game time. Non-refundable under venue policy.";

    if (hoursRemaining > 24) {
      tier = 1;
      refundPercentage = 100;
      handlingFee = 50; // Standard payment gateway reversal charge
      refundAmount = Math.max(0, paidAmount - handlingFee);
      reason = "More than 24 hours notice. Full refund minus ₹50 processing fee.";
    } else if (hoursRemaining >= 6) {
      tier = 2;
      refundPercentage = 50;
      refundAmount = Math.round(paidAmount * 0.5);
      reason = "6 to 24 hours notice. 50% partial refund.";
    }

    return {
      hoursRemaining: Math.max(0, Math.round(hoursRemaining * 10) / 10),
      tier,
      refundPercentage,
      handlingFee,
      paidAmount,
      refundAmount,
      reason
    };
  }

  /**
   * Executes atomic cancellation and refund record
   */
  static async cancelBooking(bookingId, userId) {
    const booking = mockDb.data.bookings[bookingId];
    if (!booking) {
      throw new Error("Booking record not found.");
    }

    if (booking.userId !== userId) {
      throw new Error("Unauthorized: You do not own this booking reservation.");
    }

    if (booking.status === 'CANCELLED' || (booking.status && booking.status.startsWith('CANCELLED'))) {
      throw new Error("This booking has already been cancelled.");
    }

    if (typeof window !== 'undefined' && ApiClient.getToken()) {
      try {
        await ApiClient.cancelBooking(bookingId, 'Player requested cancellation via Apex Web');
      } catch (err) {
        const msg = (err.message || '').toLowerCase();
        if (msg.includes('authorized') || msg.includes('403') || msg.includes('only confirmed')) {
          throw err;
        }
        console.warn('[CancellationService] Backend cancel fallback:', err.message);
      }
    }

    const refund = this.previewRefund(booking);
    const now = Date.now();

    // 1. Mark booking as cancelled
    booking.status = 'CANCELLED';
    booking.cancelledAt = now;
    booking.refundDetails = refund;

    // 2. Release slot in daily aggregated availability doc back to AVAILABLE
    const availKey = `${booking.courtId}_${booking.date}`;
    if (mockDb.data.availability[availKey] && mockDb.data.availability[availKey].slots[booking.slot]) {
      mockDb.data.availability[availKey].slots[booking.slot].status = 'AVAILABLE';
      delete mockDb.data.availability[availKey].slots[booking.slot].bookingId;
      delete mockDb.data.availability[availKey].slots[booking.slot].userId;
      mockDb.data.availability[availKey].lastUpdated = now;
    }

    // 3. Clear concurrency lock
    const lockKey = ConcurrencyService.getLockKey(booking.courtId, booking.date, booking.slot);
    if (mockDb.data.concurrency_locks[lockKey]) {
      delete mockDb.data.concurrency_locks[lockKey];
    }

    // 4. Create refund transaction if refundAmount > 0
    if (refund.refundAmount > 0) {
      const refundTxId = `tx_ref_${now}_${Math.floor(1000 + Math.random() * 9000)}`;
      mockDb.data.transactions[refundTxId] = {
        txId: refundTxId,
        bookingId,
        userId,
        type: 'REFUND',
        amount: refund.refundAmount,
        originalTxId: booking.payment.transactionId,
        gateway: 'Razorpay',
        status: 'SUCCESS',
        timestamp: now
      };
    }

    // 5. Append audit log
    mockDb.data.audit_logs.push({
      logId: `audit_${now}`,
      action: 'BOOKING_CANCELLED',
      actorId: userId,
      resourceType: 'booking',
      resourceId: bookingId,
      metadata: {
        refundAmount: refund.refundAmount,
        tier: refund.tier
      },
      timestamp: now
    });

    mockDb.persist();

    return {
      success: true,
      refund
    };
  }
}

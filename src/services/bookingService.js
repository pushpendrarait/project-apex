/**
 * Project Apex - Client-Side Atomic Booking Transaction Service
 * ZERO Cloud Functions Architecture: Executes atomic multi-document commit
 */

import { mockDb } from './firebaseConfig.js';
import { ConcurrencyService } from './concurrencyService.js';
import { PaymentService } from './paymentService.js';

export class BookingService {
  /**
   * Phase 3: Atomic Multi-Document Booking Commit
   * In Firestore, this executes inside `runTransaction(db, async (transaction) => ...)`
   */
  static async commitBooking({
    userId,
    userName,
    userPhone,
    venueId,
    venueName,
    courtId,
    courtName,
    sport,
    date,
    slot,
    pricing,
    paymentMethod = 'UPI'
  }) {
    const lockKey = ConcurrencyService.getLockKey(courtId, date, slot);
    const now = Date.now();

    // 1. Transaction step: verify concurrency lock ownership
    const lock = mockDb.data.concurrency_locks[lockKey];
    if (!lock) {
      throw new Error("Reservation lease expired or lock was released. Please re-select slot.");
    }

    if (lock.userId !== userId) {
      throw new Error("Lock ownership mismatch. Slot is currently reserved by another player.");
    }

    if (lock.expiresAt < now) {
      throw new Error("Reservation window (5 minutes) timed out. Please select the slot again.");
    }

    // 2. Process simulated payment
    const paymentResult = await PaymentService.processPayment({
      amount: pricing.total,
      method: paymentMethod
    });

    // 3. Generate Booking ID and Gate Pass Token
    const bookingId = `bk_${date.replace(/-/g, '')}_${Math.floor(100000 + Math.random() * 900000)}`;
    const qrPassToken = PaymentService.generateQRPassToken({
      venueId,
      courtId,
      date,
      slot,
      bookingId
    });

    const bookingRecord = {
      bookingId,
      userId,
      userName,
      userPhone,
      venueId,
      venueName,
      courtId,
      courtName,
      sport,
      date,
      slot,
      durationMinutes: 60,
      pricing,
      payment: {
        paymentId: paymentResult.gatewayReference,
        transactionId: paymentResult.transactionId,
        method: paymentMethod,
        amount: pricing.total,
        status: 'SUCCESS'
      },
      qrPassToken,
      status: 'CONFIRMED',
      createdAt: now
    };

    // 4. Update daily aggregated availability doc
    const availKey = `${courtId}_${date}`;
    if (!mockDb.data.availability[availKey]) {
      mockDb.data.availability[availKey] = {
        courtId,
        venueId,
        date,
        slots: {},
        lastUpdated: now
      };
    }

    if (!mockDb.data.availability[availKey].slots[slot]) {
      mockDb.data.availability[availKey].slots[slot] = {
        slotTime: slot,
        basePrice: pricing.baseRate,
        isOddHour: pricing.isOddHour
      };
    }

    mockDb.data.availability[availKey].slots[slot].status = 'BOOKED';
    mockDb.data.availability[availKey].slots[slot].bookingId = bookingId;
    mockDb.data.availability[availKey].slots[slot].userId = userId;
    mockDb.data.availability[availKey].lastUpdated = now;

    // 5. Commit lock status to COMMITTED
    mockDb.data.concurrency_locks[lockKey].status = 'COMMITTED';
    mockDb.data.concurrency_locks[lockKey].bookingId = bookingId;

    // 6. Write to Bookings collection
    mockDb.data.bookings[bookingId] = bookingRecord;

    // 7. Write to Transactions ledger
    mockDb.data.transactions[paymentResult.transactionId] = {
      txId: paymentResult.transactionId,
      bookingId,
      userId,
      type: 'PAYMENT',
      amount: pricing.total,
      gateway: 'Razorpay',
      gatewayRef: paymentResult.gatewayReference,
      status: 'SUCCESS',
      timestamp: now
    };

    // 8. Append to Audit Logs
    mockDb.data.audit_logs.push({
      logId: `audit_${now}`,
      action: 'BOOKING_COMMITTED',
      actorId: userId,
      resourceType: 'booking',
      resourceId: bookingId,
      metadata: {
        venueId,
        courtId,
        date,
        slot,
        amount: pricing.total
      },
      timestamp: now
    });

    // 9. Credit player loyalty points (+50 points)
    if (mockDb.data.users[userId]) {
      mockDb.data.users[userId].loyaltyPoints = (mockDb.data.users[userId].loyaltyPoints || 0) + 50;
    }

    // Persist all state atomically
    mockDb.persist();

    return {
      success: true,
      booking: bookingRecord
    };
  }

  /**
   * Retrieves player's bookings
   */
  static getPlayerBookings(userId) {
    return Object.values(mockDb.data.bookings)
      .filter(b => b.userId === userId)
      .sort((a, b) => b.createdAt - a.createdAt);
  }

  /**
   * Retrieves all bookings for a venue (Venue Owner Portal)
   */
  static getVenueBookings(venueId) {
    return Object.values(mockDb.data.bookings)
      .filter(b => b.venueId === venueId)
      .sort((a, b) => b.createdAt - a.createdAt);
  }
}

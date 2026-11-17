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
   * Phase 3b: Walk-In / Counter Reservation by Venue Owner
   * Allows turf managers to atomically reserve a slot for walk-in players,
   * collecting offline/cash payments without race conditions with online players.
   */
  static async createWalkInBooking({
    venueId,
    venueName,
    courtId,
    courtName,
    sport = 'cricket',
    date,
    slot,
    customerName = 'Walk-In Player',
    customerPhone = '+91 99999 00000',
    amountCollected,
    amount,
    paymentMethod = 'CASH',
    isMaintenance = false,
    ownerId = 'partner_owner'
  }) {
    const now = Date.now();
    const finalAmount = amountCollected !== undefined ? amountCollected : (amount !== undefined ? amount : 1000);
    const lockKey = ConcurrencyService.getLockKey(courtId, date, slot);
    const availKey = `${courtId}_${date}`;

    // 1. Verify slot is not already booked in daily availability
    let dailyDoc = mockDb.data.availability[availKey];
    if (dailyDoc && dailyDoc.slots && dailyDoc.slots[slot]) {
      const slotData = dailyDoc.slots[slot];
      if (slotData.status === 'BOOKED') {
        throw new Error(`Slot ${slot} on ${date} is already booked.`);
      }
    }

    // 2. Verify no active unexpired online lock
    const activeLock = mockDb.data.concurrency_locks[lockKey];
    if (activeLock && activeLock.status === 'HELD' && activeLock.expiresAt > now) {
      throw new Error(`Slot ${slot} is currently locked by an online player in checkout.`);
    }

    // Handle facility maintenance block
    if (isMaintenance) {
      if (!dailyDoc) {
        dailyDoc = { courtId, venueId, date, slots: {}, lastUpdated: now };
        mockDb.data.availability[availKey] = dailyDoc;
      }
      dailyDoc.slots[slot] = {
        ...(dailyDoc.slots[slot] || { slotTime: slot, basePrice: finalAmount }),
        status: 'MAINTENANCE',
        note: customerName || 'Facility Turf Maintenance'
      };
      dailyDoc.lastUpdated = now;
      mockDb.data.audit_logs.push({
        logId: `audit_${now}`,
        action: 'SLOT_MAINTENANCE_BLOCKED',
        actorId: ownerId,
        resourceType: 'court',
        resourceId: courtId,
        metadata: { date, slot, reason: customerName },
        timestamp: now
      });
      mockDb.persist();
      return { success: true, isMaintenance: true, slot, date };
    }

    // 3. Generate Walk-In Booking Record
    const cleanDate = date.replace(/-/g, '');
    const bookingId = `bk_walkin_${cleanDate}_${Math.floor(100000 + Math.random() * 900000)}`;
    const qrPassToken = PaymentService.generateQRPassToken({
      venueId,
      courtId,
      date,
      slot,
      bookingId
    });

    const bookingRecord = {
      bookingId,
      userId: `walkin_${cleanDate}_${Math.floor(1000 + Math.random() * 9000)}`,
      userName: customerName,
      userPhone: customerPhone,
      venueId,
      venueName: venueName || 'Sports Arena',
      courtId,
      courtName: courtName || 'Main Pitch',
      sport,
      date,
      slot,
      durationMinutes: 60,
      pricing: {
        baseRate: finalAmount,
        basePrice: finalAmount,
        oddHourDiscountAmount: 0,
        promoDiscountAmount: 0,
        convenienceFee: 0,
        total: finalAmount,
        isOddHour: false
      },
      payment: {
        paymentId: `cash_${now}`,
        transactionId: `tx_cash_${now}`,
        method: paymentMethod,
        amount: finalAmount,
        status: 'COLLECTED_AT_VENUE'
      },
      qrPassToken,
      status: 'CONFIRMED',
      source: 'walk_in',
      bookedBy: ownerId,
      createdAt: now
    };

    // 4. Update daily availability document
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
        basePrice: finalAmount,
        isOddHour: false
      };
    }

    mockDb.data.availability[availKey].slots[slot].status = 'BOOKED';
    mockDb.data.availability[availKey].slots[slot].bookingId = bookingId;
    mockDb.data.availability[availKey].slots[slot].source = 'walk_in';
    mockDb.data.availability[availKey].lastUpdated = now;

    // 5. Update concurrency lock to COMMITTED
    mockDb.data.concurrency_locks[lockKey] = {
      courtId,
      date,
      slot,
      userId: bookingRecord.userId,
      lockedAt: now,
      expiresAt: now,
      status: 'COMMITTED',
      bookingId
    };

    // 6. Write to Bookings collection
    mockDb.data.bookings[bookingId] = bookingRecord;

    // 7. Write to Transactions ledger
    mockDb.data.transactions[bookingRecord.payment.transactionId] = {
      txId: bookingRecord.payment.transactionId,
      bookingId,
      userId: bookingRecord.userId,
      type: 'WALK_IN_CASH',
      amount: finalAmount,
      gateway: paymentMethod,
      gatewayRef: `ref_cash_${now}`,
      status: 'SUCCESS',
      timestamp: now
    };

    // 8. Audit Log
    mockDb.data.audit_logs.push({
      logId: `audit_${now}`,
      action: 'WALK_IN_BOOKING_CREATED',
      actorId: ownerId,
      resourceType: 'booking',
      resourceId: bookingId,
      metadata: {
        venueId,
        courtId,
        date,
        slot,
        amount: finalAmount,
        customerName
      },
      timestamp: now
    });

    mockDb.persist();

    return {
      success: true,
      booking: bookingRecord
    };
  }

  /**
   * Gate Check-In Pass Verification and Status Update
   * Validates QR token and transitions status to CHECKED_IN
   */
  static checkInBooking(qrPassToken, actorId = 'gate_operator') {
    const token = (qrPassToken || '').trim().toUpperCase();
    if (!token) {
      throw new Error("Invalid or empty QR Pass Token.");
    }

    const booking = Object.values(mockDb.data.bookings).find(b => b.qrPassToken === token);
    if (!booking) {
      throw new Error("Unrecognized QR Pass Token. No matching booking found.");
    }

    const now = Date.now();

    if (booking.status === 'CANCELLED') {
      throw new Error(`Pass cancelled. Refund was already processed for this booking (${booking.bookingId}).`);
    }

    if (booking.status === 'CHECKED_IN') {
      return {
        success: true,
        alreadyCheckedIn: true,
        checkedInAt: booking.checkedInAt,
        booking,
        message: `Player was already checked in at ${new Date(booking.checkedInAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}.`
      };
    }

    // Perform check-in
    booking.status = 'CHECKED_IN';
    booking.checkedInAt = now;
    booking.checkedInBy = actorId;

    mockDb.data.audit_logs.push({
      logId: `audit_${now}`,
      action: 'GATE_CHECK_IN_COMPLETED',
      actorId,
      resourceType: 'booking',
      resourceId: booking.bookingId,
      metadata: {
        qrPassToken: token,
        userName: booking.userName,
        courtId: booking.courtId,
        slot: booking.slot
      },
      timestamp: now
    });

    mockDb.persist();

    return {
      success: true,
      alreadyCheckedIn: false,
      checkedInAt: now,
      booking,
      message: `Access granted for ${booking.userName} (${booking.courtName} @ ${booking.slot}).`
    };
  }

  /**
   * Alias for checkInBooking
   */
  static checkInPlayer(qrPassToken, actorId = 'gate_operator') {
    return this.checkInBooking(qrPassToken, actorId);
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

  /**
   * Gate QR Pass Check-In Verification
   * Transitions booking status from CONFIRMED -> CHECKED_IN
   */
  static checkInPlayer(qrPassToken, actorId = 'gate_staff') {
    const cleanToken = (qrPassToken || '').trim().toUpperCase();
    const booking = Object.values(mockDb.data.bookings).find(
      b => (b.qrPassToken && b.qrPassToken.toUpperCase() === cleanToken) || b.bookingId === qrPassToken
    );

    if (!booking) {
      throw new Error(`Unrecognized QR Pass Token: "${cleanToken}". No active reservation found.`);
    }

    if (booking.status === 'CANCELLED') {
      throw new Error("Pass cancelled and refunded. Gate entry denied.");
    }

    if (booking.status === 'CHECKED_IN') {
      const checkInTimeStr = new Date(booking.checkedInAt || Date.now()).toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit' });
      return {
        success: false,
        alreadyCheckedIn: true,
        message: `Pass already redeemed and checked in at ${checkInTimeStr}.`,
        booking
      };
    }

    const now = Date.now();
    booking.status = 'CHECKED_IN';
    booking.checkedInAt = now;

    // Append audit log
    mockDb.data.audit_logs.push({
      logId: `audit_${now}`,
      action: 'GATE_CHECK_IN',
      actorId,
      resourceType: 'booking',
      resourceId: booking.bookingId,
      metadata: {
        courtId: booking.courtId,
        date: booking.date,
        slot: booking.slot,
        qrPassToken: booking.qrPassToken
      },
      timestamp: now
    });

    mockDb.persist();

    return {
      success: true,
      alreadyCheckedIn: false,
      message: "Gate pass validated. Player checked in successfully.",
      booking
    };
  }

  /**
   * Alias for checkInPlayer
   */
  static async checkInBooking(qrPassToken, actorId) {
    return this.checkInPlayer(qrPassToken, actorId);
  }

  /**
   * Facility Owner Walk-in / Offline Reservation
   * Allows facility owners to book slots directly at the counter
   */
  static async createWalkInBooking({
    venueId,
    venueName,
    courtId,
    courtName,
    sport = 'cricket',
    date,
    slot,
    customerName,
    customerPhone = 'Walk-in',
    amountCollected = 1000,
    amount,
    paymentMethod = 'CASH',
    isMaintenance = false,
    ownerId = 'partner_owner'
  }) {
    const finalAmount = amount !== undefined ? amount : amountCollected;
    const availKey = `${courtId}_${date}`;
    let dailyDoc = mockDb.data.availability[availKey];

    if (!dailyDoc) {
      dailyDoc = {
        courtId,
        venueId,
        date,
        slots: {},
        lastUpdated: Date.now()
      };
      for (let h = 6; h <= 23; h++) {
        const sTime = `${String(h).padStart(2, '0')}:00`;
        dailyDoc.slots[sTime] = { slotTime: sTime, status: 'AVAILABLE', basePrice: finalAmount, currentPrice: finalAmount };
      }
      mockDb.data.availability[availKey] = dailyDoc;
    }

    const targetSlot = dailyDoc.slots[slot];
    if (targetSlot && (targetSlot.status === 'BOOKED' || targetSlot.status === 'MAINTENANCE')) {
      throw new Error(`Slot ${slot} on ${date} is already booked.`);
    }

    const now = Date.now();
    const cleanDate = date.replace(/-/g, '');
    const bookingId = `bk_wlk_${cleanDate}_${Math.floor(100000 + Math.random() * 900000)}`;
    const qrPassToken = `APEX-WLK-${courtId.slice(-3).toUpperCase()}-${Math.floor(100000 + Math.random() * 900000)}`;

    if (isMaintenance) {
      dailyDoc.slots[slot] = {
        ...(dailyDoc.slots[slot] || { slotTime: slot, basePrice: finalAmount, currentPrice: finalAmount }),
        status: 'MAINTENANCE',
        note: customerName || 'Facility Turf Maintenance'
      };
      dailyDoc.lastUpdated = now;

      mockDb.data.audit_logs.push({
        logId: `audit_${now}`,
        action: 'SLOT_MAINTENANCE_BLOCKED',
        actorId: ownerId,
        resourceType: 'court',
        resourceId: courtId,
        metadata: { date, slot, reason: customerName },
        timestamp: now
      });

      mockDb.persist();

      return {
        success: true,
        isMaintenance: true,
        slot,
        date
      };
    }

    const bookingRecord = {
      bookingId,
      userId: `walkin_${cleanDate}_${Math.floor(100 + Math.random() * 900)}`,
      userName: customerName.trim(),
      userPhone: customerPhone.trim(),
      venueId,
      venueName: venueName || 'Sports Arena',
      courtId,
      courtName: courtName || 'Pitch 1',
      sport,
      date,
      slot,
      durationMinutes: 60,
      pricing: {
        basePrice: finalAmount,
        discountAmount: 0,
        promoCode: null,
        convenienceFee: 0,
        total: finalAmount
      },
      payment: {
        paymentId: `pay_cash_${now}`,
        method: paymentMethod,
        status: 'SUCCESS'
      },
      qrPassToken,
      status: 'CONFIRMED',
      source: 'walk_in',
      createdAt: now
    };

    // Update daily availability document
    dailyDoc.slots[slot] = {
      ...(dailyDoc.slots[slot] || { slotTime: slot, basePrice: finalAmount, currentPrice: finalAmount }),
      status: 'BOOKED',
      bookingId,
      userId: bookingRecord.userId
    };
    dailyDoc.lastUpdated = now;

    // Save booking
    mockDb.data.bookings[bookingId] = bookingRecord;

    // Save transaction
    const txId = `tx_wlk_${now}`;
    mockDb.data.transactions[txId] = {
      txId,
      bookingId,
      userId: bookingRecord.userId,
      type: 'PAYMENT',
      amount: finalAmount,
      gateway: paymentMethod,
      gatewayRef: `ref_cash_${now}`,
      status: 'SUCCESS',
      timestamp: now
    };

    // Audit log
    mockDb.data.audit_logs.push({
      logId: `audit_${now}`,
      action: 'WALK_IN_BOOKING_CREATED',
      actorId: ownerId,
      resourceType: 'booking',
      resourceId: bookingId,
      metadata: { venueId, courtId, date, slot, amount: finalAmount, customerName },
      timestamp: now
    });

    mockDb.persist();

    return {
      success: true,
      booking: bookingRecord
    };
  }
}

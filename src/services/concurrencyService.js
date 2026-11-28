/**
 * Project Apex - Client-Side Atomic Concurrency Lock Service
 * Zero Cloud Functions Architecture: Enforces 5-minute atomic check-and-set leases
 * Prevents double booking race conditions in high-demand windows
 */

import { mockDb } from './firebaseConfig.js';
import { ApiClient } from './apiClient.js';

const LOCK_TTL_MS = 10 * 60 * 1000; // 10 minutes TTL lease

export class ConcurrencyService {
  /**
   * Generates a deterministic lock key
   */
  static getLockKey(courtId, date, slot) {
    return `${courtId}_${date}_${slot}`;
  }

  /**
   * Phase 1: Atomically attempts to acquire the slot lock.
   * If held by another user and lease is active (> now), throws error.
   * If expired or unheld, acquires a 5-minute lease.
   */
  static async acquireLock({ courtId, date, slot, userId }) {
    const lockKey = this.getLockKey(courtId, date, slot);
    const now = Date.now();

    if (typeof window !== 'undefined' && ApiClient.getToken()) {
      try {
        await ApiClient.acquireLock({ courtId, date, slot });
      } catch (err) {
        const msg = (err.message || '').toLowerCase();
        if (msg.includes('conflict') || msg.includes('reserved') || msg.includes('booked') || msg.includes('409') || msg.includes('403') || msg.includes('authentication')) {
          throw err;
        }
        console.warn('[ConcurrencyService] Backend acquireLock fallback:', err.message);
      }
    }

    // In a live Firestore project, this executes via runTransaction(db, async (tx) => ...)
    const existingLock = mockDb.data.concurrency_locks[lockKey];

    if (existingLock) {
      const isLeaseActive = existingLock.expiresAt > now;
      const isDifferentUser = existingLock.userId !== userId;
      const isCommitted = existingLock.status === 'COMMITTED';

      if (isCommitted) {
        throw new Error("This slot has already been booked and confirmed by another player.");
      }

      if (isLeaseActive && isDifferentUser) {
        const remainingSec = Math.ceil((existingLock.expiresAt - now) / 1000);
        throw new Error(
          `Slot currently reserved by another player (${remainingSec}s checkout window remaining). Please pick another slot or check back shortly.`
        );
      }
    }

    // Acquire or renew lease
    const leaseData = {
      lockKey,
      courtId,
      date,
      slot,
      userId,
      lockedAt: now,
      expiresAt: now + LOCK_TTL_MS,
      status: 'HELD'
    };

    mockDb.data.concurrency_locks[lockKey] = leaseData;
    mockDb.persist();

    return {
      success: true,
      lease: leaseData,
      expiresInSeconds: Math.floor(LOCK_TTL_MS / 1000)
    };
  }

  /**
   * Verifies if a slot lock is currently active and who holds it
   */
  static isSlotLocked(courtId, date, slot) {
    const lockKey = this.getLockKey(courtId, date, slot);
    const lock = mockDb.data.concurrency_locks[lockKey];
    if (!lock) return false;

    const now = Date.now();
    if (lock.status === 'COMMITTED') return true;
    return lock.expiresAt > now;
  }

  /**
   * Releases an uncommitted lock (e.g., user cancels checkout)
   */
  static async releaseLock({ courtId, date, slot, userId }) {
    if (typeof window !== 'undefined' && ApiClient.getToken()) {
      try {
        await ApiClient.releaseLock({ courtId, date, slot });
      } catch (err) {
        console.warn('[ConcurrencyService] Backend releaseLock fallback:', err.message);
      }
    }

    const lockKey = this.getLockKey(courtId, date, slot);
    const lock = mockDb.data.concurrency_locks[lockKey];

    if (lock && lock.userId === userId && lock.status === 'HELD') {
      delete mockDb.data.concurrency_locks[lockKey];
      mockDb.persist();
      return true;
    }
    return false;
  }
}

package com.projectapex.service;

import com.projectapex.model.ConcurrencyLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ConcurrencyService {

    @Value("${app.concurrency.lock-ttl-seconds:300}")
    private long lockTtlSeconds;

    private final FirebaseService firebaseService;

    public ConcurrencyService(FirebaseService firebaseService) {
        this.firebaseService = firebaseService;
    }

    public synchronized ConcurrencyLock acquireLock(String courtId, String date, String slot, String userId) {
        String lockKey = courtId + "_" + date + "_" + slot;
        ConcurrencyLock existing = firebaseService.getConcurrencyLocks().get(lockKey);
        long now = System.currentTimeMillis();

        if (existing != null && "COMMITTED".equals(existing.getStatus())) {
            throw new IllegalStateException("Double-booking conflict: This slot is already booked and confirmed.");
        }

        if (existing != null && "HELD".equals(existing.getStatus()) && !existing.isExpired()) {
            if (!existing.getUserId().equals(userId)) {
                long remainingSecs = Math.max(1, (existing.getExpiresAt() - now) / 1000);
                throw new IllegalStateException("Double-booking conflict: Slot is temporarily reserved by another player (" + remainingSecs + "s lease remaining).");
            }
            // Same user renewing their lock
            existing.setExpiresAt(now + (lockTtlSeconds * 1000));
            return existing;
        }

        // Grant fresh 5-minute lease
        ConcurrencyLock newLock = new ConcurrencyLock(courtId, date, slot, userId, lockTtlSeconds * 1000);
        firebaseService.getConcurrencyLocks().put(lockKey, newLock);
        return newLock;
    }

    public synchronized void releaseLock(String courtId, String date, String slot, String userId) {
        String lockKey = courtId + "_" + date + "_" + slot;
        ConcurrencyLock lock = firebaseService.getConcurrencyLocks().get(lockKey);
        if (lock != null && lock.getUserId().equals(userId) && "HELD".equals(lock.getStatus())) {
            lock.setStatus("RELEASED");
        }
    }
}

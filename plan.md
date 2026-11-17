# Technical Architecture Plan: Project Apex
**Mission Slug:** `001-project-apex-mvp`  
**Governing Agents:** `apex-pm`, `apex-backend`, `apex-database`  
**Status:** Approved for Implementation  

---

## 1. System Architecture Overview
Project Apex operates under a serverless-client architecture strictly bounded by the **Firebase Spark (Free) Tier**.

```
+-------------------------------------------------------------------------+
|                              CLIENT TIER                                |
|  [PWA Mobile/Web Shell] <--> [Local IndexedDB Cache & Service Worker]   |
|         |                                  |                            |
|  [Slot Grid UI]                   [Venue Explorer]                      |
|         |                                  |                            |
|  +---------------+                +----------------+                    |
|  | Concurrency   |                | Pricing Engine |                    |
|  | Service       |                | & Discounts    |                    |
|  +---------------+                +----------------+                    |
|         |                                  |                            |
|  [Client-Side Atomic Firestore Transactions (runTransaction)]           |
+-------------------------------------------------------------------------+
                                   |
                  (Zero Cloud Functions / No Node Server)
                                   |
                                   v
+-------------------------------------------------------------------------+
|                          FIREBASE SPARK TIER                            |
|  [Firebase Hosting]           --> Global CDN Distribution               |
|  [Cloud Firestore]            --> 8 Collections Architecture            |
|  [Firestore Security Rules]   --> Server-Side Integrity & Auth Defense  |
+-------------------------------------------------------------------------+
```

---

## 2. Spark Tier Constraint Optimization Protocol

### 2.1 The Read Quota Bottleneck (50,000 Reads/Day)
If each slot (24 slots per court) were stored as an individual subcollection document, loading 1 day for 20 courts would consume `20 * 24 = 480` reads per page load. Just 100 page views would wipe out the entire daily quota (48,000 reads)!

**The Read Minimization Solution:**
- **Consolidated Daily Document:** We aggregate all slots for a court into a single document:
  `availability/{courtId}_{YYYY-MM-DD}`.
- Structure of the document:
  ```json
  {
    "courtId": "court_thane_01",
    "venueId": "venue_apex_thane",
    "date": "2026-11-20",
    "slots": {
      "06:00": { "status": "AVAILABLE", "basePrice": 800, "isOddHour": true, "discountPct": 20 },
      "07:00": { "status": "AVAILABLE", "basePrice": 800, "isOddHour": true, "discountPct": 20 },
      "12:00": { "status": "AVAILABLE", "basePrice": 1000, "isOddHour": true, "discountPct": 35 },
      "19:00": { "status": "BOOKED", "bookingId": "bk_88291", "userId": "usr_991" }
    },
    "updatedAt": 1795000000
  }
  ```
- **Read Reduction Impact:**
  - Checking a full day's court schedule = **1 single document read**.
  - 20 courts for 1 day = 20 reads.
  - Adding IndexedDB 15-minute TTL caching on the client reduces repeated reads by 80%.
  - 50,000 daily reads easily sustains over 10,000 daily active player sessions!

---

## 3. Concurrency Lock & Race Condition Architecture (Zero Cloud Functions)

### 3.1 The Double-Booking Race Condition Problem
During peak demand (e.g., Friday 8:00 PM turf slots), multiple users click "Book" simultaneously. Without server-side Cloud Functions, this must be solved client-side via Firestore atomic transactions.

### 3.2 The Check-and-Set Concurrency Lock
We use a dedicated collection `concurrency_locks/{courtId_date_slot}`.
When a user clicks "Proceed to Checkout":
1. `runTransaction` reads `concurrency_locks/{lockId}`:
   ```javascript
   const lockRef = doc(db, 'concurrency_locks', `${courtId}_${date}_${slot}`);
   await runTransaction(db, async (transaction) => {
     const lockDoc = await transaction.get(lockRef);
     const now = Date.now();
     if (lockDoc.exists()) {
       const lockData = lockDoc.data();
       // Check if lock lease is active and held by another user
       if (lockData.expiresAt > now && lockData.userId !== currentUserId) {
         throw new Error("SLOT_LOCKED_BY_ANOTHER_PLAYER");
       }
     }
     // Acquire 5-minute lease
     transaction.set(lockRef, {
       courtId,
       date,
       slot,
       userId: currentUserId,
       lockedAt: now,
       expiresAt: now + (5 * 60 * 1000), // 5 min TTL
       status: 'HELD'
     });
   });
   ```
2. During the 5-minute window, other users see the slot marked as `RESERVED / CHECKING OUT`.
3. If the user completes payment, the final booking commit transaction atomically:
   - Validates that the lock is still owned by `currentUserId`.
   - Creates the `bookings` record.
   - Updates `availability/{courtId_date}` marking `slots[slot].status = 'BOOKED'`.
   - Records `transactions` and `audit_logs`.
   - Sets `concurrency_locks.status = 'COMMITTED'`.
4. If the user abandons checkout or the 5 minutes elapse, the lock expires automatically based on `expiresAt < now`, and the next user can acquire it immediately without requiring a cleanup cron job!

---

## 4. Technology Stack & Directory Structure
- **Frontend Core:** Mobile-first responsive Progressive Web Application (PWA).
- **Styling:** Modern Tailwind CSS with dark/light themes, sports stadium accents, crisp typography.
- **Iconography:** Lucide Icons.
- **Data & Auth:** Firebase Client SDK v10 (Firestore, Authentication, Hosting).
- **Offline Resilience:** Service Worker Cache API + IndexedDB via `idb-keyval` / native IndexedDB.
- **Tooling:** Spec Kitty CLI (`spec-kitty`), Multi-Agent Orchestrator CLI.

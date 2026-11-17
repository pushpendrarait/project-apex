# Feature Specification: Project Apex MVP
**Mission Slug:** `001-project-apex-mvp`  
**Governing Agent:** `apex-ba` (Business Analyst)  
**Coordinating Agent:** `apex-pm` (Project Manager)  
**Status:** Approved & Ready for Implementation  

---

## 1. Executive Vision & Scope
Project Apex is a centralized, multi-tenant sports facility booking platform engineered specifically for dense urban sports markets (Thane, Mumbai Suburbs, South Mumbai, and Navi Mumbai). It connects sports enthusiasts (box cricket, football, badminton, pickleball, squash, tennis) with verified venue facilities.

### 1.1 Core Value Proposition
- **For Players:** Instant, real-time slot discovery across sports, transparent pricing with dynamic discounts for off-peak/odd hours, 60-second frictionless booking, QR-based check-in passes, and fair cancellation policies.
- **For Venue Owners:** Automated digital reservation ledger, zero double-booking assurance, increased off-peak turf utilization via dynamic pricing, and walk-in reservation capabilities.
- **For Operators:** 100% operational on Firebase Spark (Free) Tier with **Zero Cloud Functions** and strict read quota minimization (<50,000 document reads/day).

---

## 2. Target Geos & Sports Taxonomy

### 2.1 Geographic Coverage
1. **Thane Zone:** Ghodbunder Road, Majiwada, Vartak Nagar, Hiranandani Estate, Meadows, Teen Hath Naka.
2. **Mumbai Suburbs:** Andheri West/East, Bandra, Powai, Malad, Borivali, Goregaon, Ghatkopar.
3. **South & Central Mumbai:** Dadar, Lower Parel, Worli, BKC, Colaba.
4. **Navi Mumbai:** Vashi, Nerul, Kharghar, Seawoods, Belapur.

### 2.2 Sports Taxonomy
- **Box Cricket Turf:** 6v6 / 8v8 artificial turf with netting, floodlights, boundary nets.
- **5-a-Side Football:** FIFA-grade artificial grass, goals, floodlights, bibs & balls included.
- **Badminton:** BWF-approved synthetic or wooden indoor courts with HVAC/ventilation.
- **Pickleball:** Dedicated outdoor and indoor courts with professional nets and paddle rentals.
- **Squash:** ASB glass-backed indoor air-conditioned squash courts.
- **Lawn Tennis:** Hard and clay courts with floodlights.

---

## 3. User Personas & Workflows

### 3.1 Player Persona: Rohan (27, Corporate Professional, Thane)
- **Goal:** Book a 2-hour box cricket slot on Friday night or Sunday morning with office colleagues.
- **Needs:** Fast availability search, upfront total price with split bill estimates, instant booking confirmation, and scannable QR ticket on mobile.

### 3.2 Venue Owner Persona: Santosh (45, Turf Owner, Ghodbunder Rd, Thane)
- **Goal:** Maximize turf occupancy, eliminate WhatsApp/phone scheduling errors, fill idle weekday morning/afternoon slots.
- **Needs:** Visual calendar of booked vs. empty slots, manual walk-in entry, automated dynamic off-peak discounts, and real-time QR check-in verification.

---

## 4. Business Rules & Functional Requirements

### 4.1 Dynamic Off-Peak & Odd-Hour Pricing Engine
To stimulate demand during traditionally low-traffic hours, the platform enforces automated time-band discounts:
- **Odd Hours / Night Owl (11:00 PM - 02:00 AM):** 25% Off standard hourly rate.
- **Early Bird (06:00 AM - 08:00 AM, Weekdays):** 20% Off standard hourly rate.
- **Afternoon Grind / Off-Peak (11:00 AM - 04:00 PM, Weekdays):** 35% Off standard hourly rate.
- **Prime Time (07:00 PM - 11:00 PM, Weekdays & All Weekend Evenings):** Standard Peak Rate (1.0x - 1.2x).
- **Promotional Coupon Codes:**
  - `THANE50`: Flat ₹50 off on first booking in Thane.
  - `APEX20`: 20% off up to ₹200 for new users.
  - `ODDHOUR35`: Extra 5% stackable discount on odd-hour slots.

### 4.2 Two-Phase Slot Reservation & Concurrency Lock Protocol
To prevent double bookings during simultaneous checkout without server-side Cloud Functions:
1. **Phase 1: Temporary Lock Acquisition (Client-Side Atomic Transaction):**
   - User selects an available slot.
   - Client executes a Firestore transaction on `concurrency_locks/{courtId_date_slot}`.
   - If the lock document exists and its `expiresAt` timestamp is in the future (> current time), the slot is deemed **Reserved** by another user and rejected.
   - If the lock document does not exist OR `expiresAt` < current time (expired lock lease), the transaction writes:
     `{ userId, courtId, date, slot, lockedAt: now(), expiresAt: now() + 5 minutes }`.
   - A 5-minute checkout countdown timer begins on the user's screen.
2. **Phase 2: Payment Execution:**
   - User reviews checkout summary and completes payment via UPI / Card (simulated / Razorpay integration).
3. **Phase 3: Atomic Booking Commit (Client-Side Atomic Transaction):**
   - Upon successful payment, client executes a multi-document atomic transaction:
     a. Checks that `concurrency_locks/{courtId_date_slot}` is still held by the active user.
     b. Creates confirmed record in `bookings/{bookingId}` with status `CONFIRMED`.
     c. Updates daily aggregated document `availability/{courtId_date}` marking slot as `BOOKED`.
     d. Writes to `transactions/{txId}` and `audit_logs/{logId}`.
     e. Deletes or marks `concurrency_locks/{courtId_date_slot}` as `COMMITTED`.

### 4.3 Cancellation & Refund Matrix
- **Tier 1 (> 24 hours prior to slot start):** 100% refund (minus ₹50 payment gateway handling fee).
- **Tier 2 (6 hours to 24 hours prior to slot start):** 50% refund.
- **Tier 3 (< 6 hours prior to slot start):** 0% refund (slot surrendered; venue compensated).
- **Rainout / Venue Force Majeure:** 100% full credit refund issued as platform wallet credits or original payment reversal.

---

## 5. Non-Functional & Infrastructure Constraints
1. **Firebase Spark Tier Strict Optimization:**
   - Max 50,000 reads/day.
   - Max 20,000 writes/day.
   - Max 1 GiB total storage.
2. **Zero Cloud Functions Mandate:**
   - Absolutely no Cloud Functions (`functions.https.onCall`, PubSub, or triggers).
   - All validation and atomicity guaranteed through Firebase Client SDK transactions (`runTransaction`) and comprehensive `firestore.rules`.
3. **Read Minimization Architecture:**
   - Availability checks must NEVER query individual slot documents.
   - Exactly **1 read per court per day** by using aggregated daily availability documents (`availability/{courtId_YYYY-MM-DD}`).
   - Client-side and IndexedDB persistent caching for venue listings.

---

## 6. Acceptance Criteria (Given-When-Then)

### AC-1: Location & Sport Filtering
- **Given** a user opens Project Apex in Thane,
- **When** the user filters by "Thane" and "Box Cricket",
- **Then** only venues located in Thane offering box cricket are rendered with starting prices and ratings within 200ms.

### AC-2: Odd-Hour Discount Display
- **Given** a user views court availability for a weekday,
- **When** the time slots between 11:00 AM and 04:00 PM are rendered,
- **Then** each slot displays a visual "35% OFF" discount badge and the adjusted hourly rate.

### AC-3: Atomic Race Condition Prevention
- **Given** Player A and Player B both click the same slot at 18:00 within 50ms of each other,
- **When** both clients attempt to acquire the lock transaction on `concurrency_locks`,
- **Then** exactly one player successfully acquires the 5-minute lock lease, and the other player receives an instant "Slot is currently being booked by another player" notification without corrupting database state.

### AC-4: Quota Protection Verification
- **Given** 1,000 users check today's schedule for 10 courts,
- **When** availability is fetched,
- **Then** the total Firestore reads consumed cannot exceed 10 reads (1 read per court per day cached locally).

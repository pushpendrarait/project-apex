# Firebase Spark (Free) Tier Quota Budget & Mathematical Proof
**Governing Agent:** `apex-deployment` (Deployment Engineer)  
**Infrastructure Target:** Firebase Spark Tier (Free Plan: 50,000 reads/day, 20,000 writes/day, 1 GiB storage)  
**Constraint Mandate:** Zero Cloud Functions. Zero Monthly Cloud Invoices ($0.00).  

---

## 1. The Naive Architecture Bottleneck (Why Standard Designs Fail)
In a naive subcollection architecture where every 1-hour slot is stored as an individual Firestore document:
- 20 courts × 24 slots/day = **480 documents** to represent a single day's schedule.
- A single user browsing availability across 5 venues (e.g., in Thane) consumes:
  `5 venues × 3 courts × 24 slots = 360 reads per page view`.
- **Result:** After just **138 page views**, the entire 50,000 daily read quota is exhausted (`138 × 360 = 49,680 reads`), crashing the platform.

---

## 2. Project Apex's Consolidated Daily Document Architecture

### 2.1 The 1-Read-Per-Court Protocol
Instead of querying slots individually, Project Apex stores all 24 slots for a given court in a **single aggregated document**:
`availability/{courtId}_{YYYY-MM-DD}`

```
Client Request -> Fetch availability/{courtId_date} -> Exactly 1 Firestore Document Read!
```

### 2.2 Mathematical Quota Consumption Model (5,000 Daily Active Users)

| Action | Daily Volume | Document Target | Reads / Event | Total Daily Reads |
| :--- | :--- | :--- | :--- | :--- |
| Initial Venue Catalog Load | 5,000 users | `venues` collection (5 docs, cached in IndexedDB for 24h) | 0.05 reads (effective) | 250 reads |
| Court Schedule Browse | 15,000 searches | `availability/{courtId_date}` (1 doc per court, cached 15m) | 0.3 reads (effective) | 4,500 reads |
| Slot Lock Acquisition | 800 attempts | `concurrency_locks/{lockId}` (single doc check) | 1 read | 800 reads |
| Booking Commit & Verification | 400 bookings | Multi-document transaction verification | 3 reads | 1,200 reads |
| Player Passes Check | 2,000 checks | `bookings` query filtered by `userId` | 2 reads | 4,000 reads |
| **TOTAL ESTIMATED DAILY READS** | — | — | — | **10,750 reads** |

**Safety Margin:**
$$\text{Safety Margin} = \frac{50,000 - 10,750}{50,000} \times 100 = 78.5\% \text{ Quota Headroom}$$

Even under peak weekend loads with 5,000 active players, the platform consumes less than 22% of the daily free tier limit.

---

## 3. Daily Write Budget Verification (20,000 Writes/Day Limit)

| Event | Number of Writes per Occurrence | Daily Events | Total Daily Writes |
| :--- | :--- | :--- | :--- |
| Slot Temporary Lock | 1 write (`concurrency_locks`) | 800 | 800 |
| Booking Atomic Commit | 4 writes (`bookings`, `availability`, `transactions`, `audit_logs`) | 400 | 1,600 |
| Cancellations | 3 writes (`bookings`, `availability`, `transactions`) | 40 | 120 |
| **TOTAL ESTIMATED WRITES** | — | — | **2,520 writes** |

**Safety Margin:** 87.4% under the 20,000 daily write limit.

---

## 4. Zero Cloud Functions Audit
```bash
# Automated audit command:
npm run orchestrator:verify
```
Scans the repository to guarantee:
- 0 Cloud Functions (`functions/` does not exist).
- 0 Node.js backend servers.
- 0 Cloud Run or GCF dependencies.
All transaction atomicity is executed client-side via Firebase SDK `runTransaction` guarded by `firestore.rules`.

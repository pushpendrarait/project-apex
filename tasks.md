# Work Packages Breakdown & Tasks Matrix
**Mission Slug:** `001-project-apex-mvp`  
**Governing Agent:** `apex-pm` (Project Manager & Spec Kitty Controller)  
**Coordinating Agents:** `apex-ba`, `apex-database`, `apex-backend`, `apex-frontend`, `apex-mobile`, `apex-marketing`, `apex-legal`, `apex-deployment`  
**Tracking Mode:** Spec Kitty SDD (Spec-Driven Development)  

---

## 1. Architectural Work Packages (WP01 - WP09)

| Work Package | Lead Agent | Title | Status | Lane |
| :--- | :--- | :--- | :--- | :--- |
| **WP01** | `apex-pm` | Project Governance, Spec Kitty SDD & Multi-Agent Orchestration | Complete | `done` |
| **WP02** | `apex-ba` | Business Analysis, PRD, User Personas & Acceptance Criteria | Complete | `done` |
| **WP03** | `apex-database` | Firestore 8-Collection Schema, Daily Aggregations & Security Rules | Complete | `done` |
| **WP04** | `apex-backend` | Atomic Concurrency Locks, Dynamic Pricing & Booking Transactions | Complete | `done` |
| **WP05** | `apex-frontend` | Mobile-First UI, Slot Grid, Checkout Modal & Partner Portal | Complete | `done` |
| **WP06** | `apex-mobile` | PWA Manifest, Service Worker & Offline IndexedDB Cache | Complete | `done` |
| **WP07** | `apex-marketing` | Urban Growth Strategy, Odd-Hour Discount Engine & Loyalty Loop | Complete | `done` |
| **WP08** | `apex-legal` | Indian Legal Compliance (IT Act 2000, DPDP Act 2023) & Liability Waivers | Complete | `done` |
| **WP09** | `apex-deployment` | Firebase Spark Deployment, Zero Cloud Functions Audit & Quota Guard | Complete | `done` |

---

## 2. Core End-to-End Business Workflows (WF01 - WF06)

All 6 core operational workflows are active in the application, interactive in the UI, and verified by automated end-to-end integration tests.

| Workflow Package | Coordinating Agents | Business Workflow Title | UI Component | Automated Test | Status | Lane |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **WF01** | `apex-ba`, `apex-frontend`, `apex-marketing` | User (Player) Registration & Welcome Loyalty Onboarding | `UserAuthModal.js` | `tests/runner.js` (Suite 5 & 7) | Complete | `done` |
| **WF02** | `apex-ba`, `apex-database`, `apex-backend` | Facility Owner Registration & Facility Auto-Provisioning | `UserAuthModal.js` (Partner Tab) | `tests/runner.js` (Suite 5 & 7) | Complete | `done` |
| **WF03** | `apex-frontend`, `apex-database`, `apex-backend` | Adding / Onboarding New Sports Facilities & Pitches | `AddVenueModal.js` | `tests/runner.js` (Suite 5 & 7) | Complete | `done` |
| **WF04** | `apex-backend`, `apex-frontend`, `apex-database` | Slot Reservation, Concurrency Lock & Atomic Booking Commit | `SlotCalendar.js`, `CheckoutModal.js` | `tests/runner.js` (Suite 1, 4, 7) | Complete | `done` |
| **WF05** | `apex-backend`, `apex-frontend`, `apex-database` | Facility Owner Gate QR Check-In & Walk-In Reservation | `VenueOwnerPortal.js` | `tests/runner.js` (Suite 6 & 7) | Complete | `done` |
| **WF06** | `apex-backend`, `apex-frontend`, `apex-legal` | 3-Tier Fair Cancellation & Automated Refunds | `PlayerPortal.js` | `tests/runner.js` (Suite 4 & 7) | Complete | `done` |

---

## 3. Granular Task Definitions

### WP01: Project Governance & Spec Kitty Setup (`apex-pm`)
- [x] T01.1: Initialize Spec Kitty project structure with `antigravity` and `gemini` profiles.
- [x] T01.2: Generate `spec.md`, `plan.md`, `data-model.md`, and `tasks.md`.
- [x] T01.3: Define agent roles and orchestrator contracts in `agents.config.json`.
- [x] T01.4: Maintain synchronized `kanban.json` work package lanes and status matrix.

### WP02: Business Analysis & User Stories (`apex-ba`)
- [x] T02.1: Document player (Rohan) and venue owner (Santosh) end-to-end workflows.
- [x] T02.2: Specify dynamic off-peak and odd-hour discount matrix for Thane and Mumbai.
- [x] T02.3: Define 3-tier cancellation refund rules (Tier 1 >24h 100%, Tier 2 6-24h 50%, Tier 3 <6h 0%).
- [x] T02.4: Build interactive `ScenarioRunnerModal.js` demonstrator for live multi-agent verification.

### WP03: Cloud Firestore 8-Collection Schema & Security Rules (`apex-database`)
- [x] T03.1: Implement Firestore Schema across 8 core collections (`users`, `venues`, `courts`, `availability`, `concurrency_locks`, `bookings`, `transactions`, `audit_logs`).
- [x] T03.2: Configure aggregated daily availability document structure (`availability/{courtId_date}`) for 1-read-per-court-day compliance.
- [x] T03.3: Write and validate `firestore.rules` preventing double-booking and unauthorized writes.
- [x] T03.4: Configure composite search indexes in `firestore.indexes.json`.

### WP04: Concurrency Lock & Client-Side Atomic Transactions (`apex-backend`)
- [x] T04.1: Implement `concurrencyService.js` with check-and-set and 5-minute TTL lease.
- [x] T04.2: Implement `bookingService.js` executing atomic multi-document commits via `runTransaction`.
- [x] T04.3: Implement `pricingEngine.js` for base, off-peak, odd-hour, and promo discounts.
- [x] T04.4: Implement `paymentService.js` with UPI/Card simulation and QR pass token generation.
- [x] T04.5: Implement `cancellationService.js` for refund tiers and atomic status updates.
- [x] T04.6: Implement walk-in reservations and gate check-in verification in `bookingService.js`.

### WP05: Responsive Mobile-First Web Frontend (`apex-frontend`)
- [x] T05.1: Build Venue Explorer with city filtering (Thane, Mumbai, Navi Mumbai) and sport categories.
- [x] T05.2: Build real-time interactive Slot Calendar & Time Grid (06:00 to 24:00) with visual odd-hour badges.
- [x] T05.3: Build Checkout Modal with 5-minute countdown reservation timer and instant breakdown.
- [x] T05.4: Build Booking Confirmation screen with scannable QR ticket pass.
- [x] T05.5: Build Player Portal ("My Passes") and Venue Owner Partner Management Portal.
- [x] T05.6: Build interactive Walk-In Reservation modal and Gate Check-In scanner in `VenueOwnerPortal.js`.

### WP06: Mobile App Experience & Offline Resilience (`apex-mobile`)
- [x] T06.1: Create Progressive Web App `manifest.json` with sports icons and theme colors.
- [x] T06.2: Build Service Worker `sw.js` for network-first/cache-fallback static asset delivery.
- [x] T06.3: Implement `offlineStorage.js` utilizing IndexedDB to store venue catalogs and confirmed QR passes.

### WP07: Urban Growth Playbook & Odd-Hour Marketing (`apex-marketing`)
- [x] T07.1: Author Thane & Mumbai expansion playbook (Ghodbunder leagues, corporate tournaments).
- [x] T07.2: Draft Odd-Hour and Off-Peak demand stimulation campaign strategies (`ODDHOUR35`, `THANE50`).
- [x] T07.3: Design player loyalty tier and referral rewards program (100 welcome pts, +50 pts per booking).

### WP08: Indian Legal Compliance & Liability Waivers (`apex-legal`)
- [x] T08.1: Draft Terms of Service compliant with IT Act 2000 Section 79 and DPDP Act 2023.
- [x] T08.2: Draft Physical Injury and Property Damage Sports Liability Waiver.
- [x] T08.3: Draft Facility Partner Onboarding & Settlement Agreement for Maharashtra turf operators.

### WP09: Firebase Spark Deployment & Quota Budget (`apex-deployment`)
- [x] T09.1: Configure `firebase.json` for hosting, rules, and indexes.
- [x] T09.2: Create `spark-tier-quota-budget.md` mathematical verification for 50k reads/day limit.
- [x] T09.3: Build automated pre-flight audit CLI verifying Zero Cloud Functions and security rules.
- [x] T09.4: Build orchestrator verification suite executing automated workflow tests.

---

## 4. Acceptance Criteria & Automated Verification Matrix

| Workflow ID | Acceptance Criteria Summary | UI Verification Path | Automated Test Assertion | Spark Tier Quota Impact | Status |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **WF01** | Player registers with mobile, email, name; receives 100 loyalty points; session becomes active. | Header "Login / Register" -> Player Register Form | `tests/runner.js: Suite 5 & 7` (`newUser.loyaltyPoints === 100`) | 1 Document Write to `/users` | `VERIFIED` |
| **WF02** | Turf Owner registers; profile created with `venue_owner` role; facility auto-provisioned with court schedule. | Header "Login / Register" -> Partner Register Form | `tests/runner.js: Suite 5 & 7` (`ownerReg.user.venueId === ownerReg.venue.venueId`) | 2 Document Writes (`/users`, `/venues`) | `VERIFIED` |
| **WF03** | Operator adds facility; enters location, sports, amenities; venue appears in search with court schedule. | Header "+ List Venue" / Partner Portal "+ Add Facility" | `tests/runner.js: Suite 5 & 7` (`newCourts.length >= 1`, `foundVenue !== undefined`) | 1 Venue Write + 1 Court Write | `VERIFIED` |
| **WF04** | Player selects slot; acquires 5-min TTL concurrency lock lease; concurrent players blocked; atomic commit generates QR pass. | Venue Explorer -> Slot Calendar -> Checkout Modal | `tests/runner.js: Suite 1, 4, 7` (`playerBFailed === true`, `booking.qrPassToken`) | 1 Daily Read + 1 Lock Set + 1 Atomic Commit | `VERIFIED` |
| **WF05** | Gate scanner validates QR pass & marks `CHECKED_IN`; duplicates blocked; owner can commit walk-in cash reservation. | Partner Portal -> Gate QR Scanner & "+ Walk-In" Modal | `tests/runner.js: Suite 6 & 7` (`gateCheckIn.success === true`, `walkInRes.booking.source === 'walk_in'`) | 1 Lock/Commit Write, 0 Cloud Functions | `VERIFIED` |
| **WF06** | Player cancels booking; policy preview evaluates tier (>24h=100%, 6-24h=50%, <6h=0%); slot restored to AVAILABLE. | "My Passes" Portal -> Cancel Booking Modal | `tests/runner.js: Suite 4 & 7` (`cancelResult.success === true`, `slot.status === 'AVAILABLE'`) | 1 Atomic Update, Slot recycled instantly | `VERIFIED` |

---

## 5. Verification Commands
- Run automated 64-test suite: `node tests/runner.js`
- Run orchestrator pre-flight audits: `node orchestrator/orchestrator.js verify`
- Run orchestrator workflow verification: `node orchestrator/orchestrator.js workflows`
- View multi-agent kanban status: `node orchestrator/orchestrator.js status`

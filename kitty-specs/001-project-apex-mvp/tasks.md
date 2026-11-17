# Work Packages Breakdown & Tasks Matrix
**Mission Slug:** `001-project-apex-mvp`  
**Governing Agent:** `apex-pm`  
**Tracking Mode:** Spec Kitty SDD  

---

## 1. Kanban Lanes Overview

| Work Package | Agent | Title | Status | Lane |
| :--- | :--- | :--- | :--- | :--- |
| **WP01** | `apex-pm` | Project Governance & Spec Kitty Setup | Complete | `done` |
| **WP02** | `apex-ba` | Business Analysis, PRD & Acceptance Criteria | Complete | `done` |
| **WP03** | `apex-database` | Firestore 8-Collection Schema & Security Rules | Ready | `doing` |
| **WP04** | `apex-backend` | Atomic Concurrency Locks & Booking Transactions | Ready | `doing` |
| **WP05** | `apex-frontend` | Mobile-First UI, Slot Grid & Checkout Modal | Ready | `doing` |
| **WP06** | `apex-mobile` | PWA Manifest, Service Worker & Offline Cache | Ready | `doing` |
| **WP07** | `apex-marketing` | Urban Growth Campaigns, Odd-Hour Discounts & Loyalty | Ready | `doing` |
| **WP08** | `apex-legal` | Indian Jurisdiction ToS, Liability Waivers & Agreements | Ready | `doing` |
| **WP09** | `apex-deployment` | Firebase Hosting, Firestore Rules & Quota Monitors | Ready | `doing` |

---

## 2. Granular Task Definitions

### WP01: Project Governance & Spec Kitty Setup (`apex-pm`)
- [x] T01.1: Initialize Spec Kitty project structure with `antigravity` and `gemini` profiles.
- [x] T01.2: Generate `spec.md`, `plan.md`, `data-model.md`, and `tasks.md`.
- [x] T01.3: Define agent roles and orchestrator contracts in `agents.config.json`.

### WP02: Business Analysis & User Stories (`apex-ba`)
- [x] T02.1: Document player and venue owner end-to-end workflows.
- [x] T02.2: Specify dynamic off-peak and odd-hour discount matrix (Thane & Mumbai).
- [x] T02.3: Define cancellation refund window rules (Tier 1 >24h 100%, Tier 2 6-24h 50%, Tier 3 <6h 0%).

### WP03: Cloud Firestore 8-Collection Schema & Security Rules (`apex-database`)
- [x] T03.1: Implement Firestore Schema across 8 core collections.
- [x] T03.2: Configure aggregated daily availability document structure (`availability/{courtId_date}`).
- [x] T03.3: Write and validate `firestore.rules` preventing double-booking and unauthorized writes.
- [x] T03.4: Configure composite search indexes in `firestore.indexes.json`.

### WP04: Concurrency Lock & Client-Side Atomic Transactions (`apex-backend`)
- [x] T04.1: Implement `concurrencyService.js` with check-and-set and 5-minute TTL lease.
- [x] T04.2: Implement `bookingService.js` executing atomic multi-document commits via `runTransaction`.
- [x] T04.3: Implement `pricingEngine.js` for base, off-peak, odd-hour, and promo discounts.
- [x] T04.4: Implement `paymentService.js` with UPI/Card simulation and QR pass token generation.
- [x] T04.5: Implement `cancellationService.js` for refund tiers and atomic status updates.

### WP05: Responsive Mobile-First Web Frontend (`apex-frontend`)
- [x] T05.1: Build Venue Explorer with city filtering (Thane, Mumbai, Navi Mumbai) and sport categories.
- [x] T05.2: Build real-time interactive Slot Calendar & Time Grid (06:00 to 24:00) with visual states.
- [x] T05.3: Build Checkout Modal with 5-minute countdown reservation timer and instant breakdown.
- [x] T05.4: Build Booking Confirmation screen with scannable QR ticket pass.
- [x] T05.5: Build Player Portal ("My Bookings") and Venue Owner Management Portal.

### WP06: Mobile App Experience & Offline Resilience (`apex-mobile`)
- [x] T06.1: Create Progressive Web App `manifest.json` with sports icons and theme colors.
- [x] T06.2: Build Service Worker `sw.js` for network-first/cache-fallback static asset delivery.
- [x] T06.3: Implement `offlineStorage.js` utilizing IndexedDB to store venue catalogs and confirmed QR passes.

### WP07: Urban Growth Playbook & Odd-Hour Marketing (`apex-marketing`)
- [x] T07.1: Author Thane & Mumbai expansion playbook (Ghodbunder leagues, corporate tournaments).
- [x] T07.2: Draft Odd-Hour and Off-Peak demand stimulation campaign strategies (`ODDHOUR35`, `THANE50`).
- [x] T07.3: Design player loyalty tier and referral rewards program.

### WP08: Indian Legal Compliance & Liability Waivers (`apex-legal`)
- [x] T08.1: Draft Terms of Service compliant with IT Act 2000 and DPDP Act 2023.
- [x] T08.2: Draft Physical Injury and Property Damage Sports Liability Waiver.
- [x] T08.3: Draft Facility Partner Onboarding & Settlement Agreement.

### WP09: Firebase Spark Deployment & Quota Budget (`apex-deployment`)
- [x] T09.1: Configure `firebase.json` for hosting, rules, and indexes.
- [x] T09.2: Create `spark-tier-quota-budget.md` mathematical verification for 50k reads/day limit.
- [x] T09.3: Build automated pre-flight audit CLI verifying Zero Cloud Functions and security rules.
- [x] T09.4: Deploy to Firebase Hosting and Firestore rules.

# Project Apex — Master QA Test Plan & End-to-End Test Cases

**Document ID:** `APEX-QA-PLAN-3.0`  
**Document Version:** `3.0.0` (3-Tier Multi-Cloud Architecture)  
**Author:** `apex-test-lead` (Test Lead, reporting to `apex-tech-lead`)  
**Collaborating Role:** `apex-ba` (Business Analyst — [brd.md](file:///c:/Users/pushp/googlews/project-apex/docs/brd.md))  
**Target Stack:**
- **Tier 1 (Presentation):** Firebase Hosting (`https://project-apex-pushp.web.app`) — Static PWA + Razorpay Standard Checkout
- **Tier 2 (Middleware):** Render (`https://project-apex-backend-fqe3.onrender.com/api/v1`) — Java 21 LTS + Spring Boot 3.2.3 REST API
- **Tier 3 (Persistence):** Neon Serverless PostgreSQL (`sslmode=require`, 13 relational tables in [schema.sql](file:///c:/Users/pushp/googlews/project-apex/backend/src/main/resources/db/schema.sql))
- **Mobile Client:** Android Kotlin / Jetpack Compose ([ApexApiClient.kt](file:///c:/Users/pushp/googlews/project-apex/android/app/src/main/kotlin/com/example/projectapex/network/ApexApiClient.kt))

---

## 1. Executive Summary & Test Scope

This Master Test Plan defines the end-to-end verification strategy and **55 structured QA test cases** covering every Player, Facility Owner, Guest, and Webhook/Admin workflow mandated by [brd.md](file:///c:/Users/pushp/googlews/project-apex/docs/brd.md) (`BA-01` through `BA-08`) and stakeholder CRUD requirements across all tiers.

### 1.1 Test Case Coverage Summary

| Suite ID | Workflow Domain | Test Case IDs | Total Cases | Personas Covered | Linked BA Rules |
| :--- | :--- | :--- | :---: | :--- | :--- |
| **Suite 1** | User Identity, Dual OTP KYC, Auth & Profiles | `TC-USR-001` – `TC-USR-012` | **12** | Guest, Player, Facility Owner, Webhook/Admin | `BA-01`, `BA-02` |
| **Suite 2** | Facility, Venue, Court, Schedule & Overrides | `TC-FAC-001` – `TC-FAC-010` | **10** | Guest, Player, Facility Owner, Webhook/Admin | `BA-02`, `BA-08` |
| **Suite 3** | Real-Time Slots, Dynamic Pricing & Slot Locks | `TC-PRC-001` – `TC-PRC-011` | **11** | Guest, Player, Facility Owner | `BA-03`, `BA-04` |
| **Suite 4** | Booking Lifecycle, Payments, Gate QR & Webhooks | `TC-BKG-001` – `TC-BKG-014` | **14** | Player, Facility Owner, Webhook/Admin | `BA-04`, `BA-05`, `BA-06`, `BA-07` |
| **Suite 5** | Cross-Tier Cloud Architecture, CORS, DB & Mobile | `TC-ARC-001` – `TC-ARC-008` | **8** | Guest, Player, Facility Owner, Webhook/Admin | `BA-08`, All |
| **Total** | **All Project Apex E2E Workflows** | **55 Test Cases** | **55** | **All 4 Persona Types** | **`BA-01` – `BA-08`** |

---

## 2. Test Environment, Tooling & Pre-Flight Baseline

### 2.1 Reference Artifacts Inspected

| Tier | Artifact | Purpose in Verification |
| :--- | :--- | :--- |
| **Requirements** | [brd.md](file:///c:/Users/pushp/googlews/project-apex/docs/brd.md) | Authoritative Business Rules `BA-01` – `BA-08` & Acceptance Criteria |
| **Tier 1 UI** | [index.html](file:///c:/Users/pushp/googlews/project-apex/index.html), [UserAuthModal.js](file:///c:/Users/pushp/googlews/project-apex/src/components/UserAuthModal.js), [VenueExplorer.js](file:///c:/Users/pushp/googlews/project-apex/src/components/VenueExplorer.js), [AddVenueModal.js](file:///c:/Users/pushp/googlews/project-apex/src/components/AddVenueModal.js), [SlotCalendar.js](file:///c:/Users/pushp/googlews/project-apex/src/components/SlotCalendar.js), [CheckoutModal.js](file:///c:/Users/pushp/googlews/project-apex/src/components/CheckoutModal.js), [PlayerPortal.js](file:///c:/Users/pushp/googlews/project-apex/src/components/PlayerPortal.js), [VenueOwnerPortal.js](file:///c:/Users/pushp/googlews/project-apex/src/components/VenueOwnerPortal.js), [AdminConsole.js](file:///c:/Users/pushp/googlews/project-apex/src/components/AdminConsole.js) | UI rendering, modal flows, client fallback state, and REST API wiring |
| **Tier 1 Adapter** | [apiClient.js](file:///c:/Users/pushp/googlews/project-apex/src/services/apiClient.js), [runner.js](file:///c:/Users/pushp/googlews/project-apex/tests/runner.js) | HTTP JSON requests, JWT Bearer injection, existing JS integration assertions |
| **Tier 2 Controllers** | [AuthController.java](file:///c:/Users/pushp/googlews/project-apex/backend/src/main/java/com/projectapex/controller/AuthController.java), [UserController.java](file:///c:/Users/pushp/googlews/project-apex/backend/src/main/java/com/projectapex/controller/UserController.java), [VenueDiscoveryController.java](file:///c:/Users/pushp/googlews/project-apex/backend/src/main/java/com/projectapex/controller/VenueDiscoveryController.java), [OwnerVenueController.java](file:///c:/Users/pushp/googlews/project-apex/backend/src/main/java/com/projectapex/controller/OwnerVenueController.java), [FacilityController.java](file:///c:/Users/pushp/googlews/project-apex/backend/src/main/java/com/projectapex/controller/FacilityController.java), [SlotController.java](file:///c:/Users/pushp/googlews/project-apex/backend/src/main/java/com/projectapex/controller/SlotController.java), [PaymentController.java](file:///c:/Users/pushp/googlews/project-apex/backend/src/main/java/com/projectapex/controller/PaymentController.java), [BookingController.java](file:///c:/Users/pushp/googlews/project-apex/backend/src/main/java/com/projectapex/controller/BookingController.java), [OwnerBookingController.java](file:///c:/Users/pushp/googlews/project-apex/backend/src/main/java/com/projectapex/controller/OwnerBookingController.java), [OwnerCalendarController.java](file:///c:/Users/pushp/googlews/project-apex/backend/src/main/java/com/projectapex/controller/OwnerCalendarController.java), [OwnerDiscountController.java](file:///c:/Users/pushp/googlews/project-apex/backend/src/main/java/com/projectapex/controller/OwnerDiscountController.java), [GateController.java](file:///c:/Users/pushp/googlews/project-apex/backend/src/main/java/com/projectapex/controller/GateController.java), [WebhookController.java](file:///c:/Users/pushp/googlews/project-apex/backend/src/main/java/com/projectapex/controller/WebhookController.java), [AdminController.java](file:///c:/Users/pushp/googlews/project-apex/backend/src/main/java/com/projectapex/controller/AdminController.java), [HealthController.java](file:///c:/Users/pushp/googlews/project-apex/backend/src/main/java/com/projectapex/controller/HealthController.java) | Endpoint contracts, validation, RBAC enforcement, exception mapping |
| **Tier 2 Security/Config** | [SecurityConfig.java](file:///c:/Users/pushp/googlews/project-apex/backend/src/main/java/com/projectapex/config/SecurityConfig.java), [CorsConfig.java](file:///c:/Users/pushp/googlews/project-apex/backend/src/main/java/com/projectapex/config/CorsConfig.java), [JwtAuthenticationFilter.java](file:///c:/Users/pushp/googlews/project-apex/backend/src/main/java/com/projectapex/config/JwtAuthenticationFilter.java), [GlobalExceptionHandler.java](file:///c:/Users/pushp/googlews/project-apex/backend/src/main/java/com/projectapex/exception/GlobalExceptionHandler.java), [application.properties](file:///c:/Users/pushp/googlews/project-apex/backend/src/main/resources/application.properties), [render.yaml](file:///c:/Users/pushp/googlews/project-apex/render.yaml) | Stateless JWT filter, RBAC URL matchers, CORS origins, exception HTTP codes |
| **Tier 3 Neon DB** | [schema.sql](file:///c:/Users/pushp/googlews/project-apex/backend/src/main/resources/db/schema.sql), [NeonDataSourceConfig.java](file:///c:/Users/pushp/googlews/project-apex/backend/src/main/java/com/projectapex/config/NeonDataSourceConfig.java), [NeonPersistenceService.java](file:///c:/Users/pushp/googlews/project-apex/backend/src/main/java/com/projectapex/service/NeonPersistenceService.java), [NeonPersistenceTests.java](file:///c:/Users/pushp/googlews/project-apex/backend/src/test/java/com/projectapex/NeonPersistenceTests.java) | 13 relational tables, `uq_apex_slot_lock`, SSL JDBC pool, paise `BIGINT` ledger |
| **Android Client** | [ApexApiClient.kt](file:///c:/Users/pushp/googlews/project-apex/android/app/src/main/kotlin/com/example/projectapex/network/ApexApiClient.kt), [ApexApiClientTest.kt](file:///c:/Users/pushp/googlews/project-apex/android/app/src/test/kotlin/com/example/projectapex/network/ApexApiClientTest.kt), [ApexRepositoryTest.kt](file:///c:/Users/pushp/googlews/project-apex/android/app/src/test/kotlin/com/example/projectapex/data/ApexRepositoryTest.kt) | Kotlin payload parity, base URL normalization, JVM unit tests & Gradle build |

### 2.2 Test Personas & Seeded Accounts

| Persona | Role (`SecurityConfig`) | Seeded / Test Identity | Capabilities Verified |
| :--- | :--- | :--- | :--- |
| **Guest** | Anonymous (`permitAll`) | Unauthenticated browser session | Browse sports/venues, check dynamic slots/quotes, attempt protected endpoints (`401/403`), register/login |
| **Player** | `ROLE_PLAYER` | `demo_player` (`rohan.deshmukh@apex.dev` / `+91 98201 12345`) + newly registered Players A & B | OTP/Password auth, profile CRUD, quote, hold locks, Razorpay pay, reschedule, 3-tier cancel, delete, tenant isolation |
| **Facility Owner** | `ROLE_OWNER` | `demo_owner` (`santosh@apexarena.com` / `+91 98190 54321`) + newly registered Owners A & B | Partner KYC registration, venue/court/schedule/override/discount CRUD, walk-in cash, gate QR check-in, emergency cancel, status update |
| **Webhook / Admin** | `ROLE_ADMIN` / Razorpay HMAC | `demo_admin` (`admin@apex.dev`) / Signed Razorpay payload | Owner KYC approval, venue status moderation, signed Razorpay webhooks, hold expiry sweeper, Neon DB audit & health checks |

### 2.3 Security & Evidence Rules During Execution
1. **No Secret Leakage:** All test logs and the final [test-evidence-and-release-certification.md](file:///c:/Users/pushp/googlews/project-apex/docs/qa/test-evidence-and-release-certification.md) report MUST mask `DATABASE_URL` credentials, `JWT_SECRET`, `RAZORPAY_KEY_SECRET`, `RAZORPAY_WEBHOOK_SECRET`, and full JWT bearer strings.
2. **Non-Destructive Neon Testing:** Never execute `TRUNCATE` or `DROP TABLE` against Neon PostgreSQL. All synthetic QA records MUST use `qa_*` / `test_*` IDs or dedicated test emails (`*@qa.apex.dev`) and be cleaned up with scoped `WHERE` clauses (`WHERE uid LIKE 'usr_qa_%'` etc.).
3. **Zero Fabricated Results:** Report actual observed HTTP status codes, JSON responses, browser UI observations, and SQL row outputs. Where implementation gaps prevent passing a test case, record status as `FAIL` / `BLOCKED` and link to the Gap Register (§8).

---

## 3. Suite 1: User Identity, Dual OTP KYC, Auth & Profile Workflows (`TC-USR-001` – `TC-USR-012`)

### `TC-USR-001` — Player Registration via Dual OTP KYC (Mobile SMS + Email)
- **Persona / User Type:** Guest → Player (`ROLE_PLAYER`)
- **Linked Requirement:** `BA-01`, `AC-01.1`
- **Preconditions:** Browser cleared (`localStorage` empty / Guest mode); no existing user in `apex_users` with phone `+91 98201 77001` or email `arjun.qa@apex.dev`.
- **Steps:**
  1. **UI:** Open Firebase app → Click `#btn-header-auth` → Switch to `Create Account` (`#mode-btn-signup`) → Select `🏏 Player Account` (`#role-btn-player`) → Fill Name `Arjun Verma`, Phone `+91 98201 77001`, City `Thane`, Email `arjun.qa@apex.dev`, Password `Password@123` → Submit (`#form-register-player`).
  2. **REST API Step A:** `POST /api/v1/auth/registration/send-otp` (and verify alias `/api/v1/auth/register/send-otp`) with `{ "phone": "+91 98201 77001", "email": "arjun.qa@apex.dev", "displayName": "Arjun Verma" }`.
  3. **UI + REST API Step B:** Retrieve 6-digit `phoneOtp` and `emailOtp` (from SMS/Email gateway logs or OTP preview), enter into `#input-verify-phone-otp` & `#input-verify-email-otp`, submit `#form-verify-dual-otp` → `POST /api/v1/auth/registration/verify-otp` with `{ sessionId, phoneOtp, emailOtp, role: "ROLE_PLAYER", displayName: "Arjun Verma", phone: "+91 98201 77001", email: "arjun.qa@apex.dev", city: "Thane", password: "Password@123" }`.
  4. **Neon DB:** Query `apex_otp_sessions`, `apex_users`, and `apex_audit_logs`.
- **Expected Assertions:**
  - **UI:** 5-minute (`05:00`) countdown timer renders in `#otp-timer-badge`; success banner confirms `100` welcome loyalty credits; header updates from `Guest` to `Arjun` (`Player` badge).
  - **REST API:** `send-otp` returns HTTP `200` with `data.sessionId` (`otp_reg_*`) and `expiresInSeconds: 300`; `verify-otp` returns HTTP `200` with `data.token` (valid HS256 JWT), `data.user.role = "ROLE_PLAYER"`, `loyaltyPoints = 100`, `isPhoneVerified = true`, `isEmailVerified = true`, and zero `passwordHash` exposed in JSON.
  - **Neon SQL:**
    ```sql
    SELECT session_id, purpose, attempts_remaining, verified FROM apex_otp_sessions WHERE email = 'arjun.qa@apex.dev';
    -- Expected: verified = TRUE, purpose = 'REGISTRATION'
    SELECT uid, full_name, phone, email, role, loyalty_points, is_phone_verified, is_email_verified, password_hash
    FROM apex_users WHERE email = 'arjun.qa@apex.dev';
    -- Expected: 1 row; role='ROLE_PLAYER', loyalty_points=100, is_phone_verified=TRUE, is_email_verified=TRUE, password_hash IS NOT NULL (hashed, not plaintext)
    SELECT action, resource_type, resource_id FROM apex_audit_logs WHERE resource_id = '<session_id>' OR actor_id = '<uid>';
    -- Expected: OTP_DISPATCHED & OTP_VERIFIED_SUCCESS rows present
    ```

### `TC-USR-002` — Player Direct Registration with Hashed Password (`/auth/register` & `/auth/register-profile`)
- **Persona / User Type:** Guest → Player (`ROLE_PLAYER`)
- **Linked Requirement:** `BA-01`
- **Preconditions:** No account for `rohit.qa@apex.dev` / `+91 98201 77002`.
- **Steps:**
  1. **REST API:** Call `POST /api/v1/auth/register-profile` with `{ "fullName": "Rohit Sharma QA", "phone": "+91 98201 77002", "email": "rohit.qa@apex.dev", "city": "Mumbai", "preferredSports": ["CRICKET_TURF", "BADMINTON"], "password": "StrongPass@123" }`.
  2. **REST API (Legacy Endpoint):** Call `POST /api/v1/auth/register` with `{ "displayName": "Kiran QA", "phone": "+91 98201 77003", "email": "kiran.qa@apex.dev", "password": "StrongPass@123", "role": "ROLE_PLAYER", "city": "Thane" }`.
  3. **Validation Negative Check:** Submit `POST /api/v1/auth/register` with `password: "123"` (`< 6` chars).
  4. **Neon DB:** Inspect `apex_users`.
- **Expected Assertions:**
  - **UI:** N/A (API contract used by Web & Android clients; verify Android `ApexApiClient.registerWithPassword` maps `ROLE_PLAYER`).
  - **REST API:** Valid requests return HTTP `200`, JWT `token`, `role = "ROLE_PLAYER"`, `loyaltyPoints = 100`. Short password `< 6` chars returns HTTP `400` (`"Password must be at least 6 characters"`).
  - **Neon SQL:**
    ```sql
    SELECT uid, role, city, preferred_sports_json, loyalty_points, password_hash FROM apex_users WHERE email IN ('rohit.qa@apex.dev', 'kiran.qa@apex.dev');
    -- Expected: 2 rows, password_hash != 'StrongPass@123', loyalty_points = 100
    ```

### `TC-USR-003` — Facility Owner Registration via Dual OTP & Auto-Provisioned Venue
- **Persona / User Type:** Guest → Facility Owner (`ROLE_OWNER`)
- **Linked Requirement:** `BA-01`, `BA-02`
- **Preconditions:** No existing account for `vikas.owner.qa@apex.dev` / `+91 98333 77004`.
- **Steps:**
  1. **UI:** Open `UserAuthModal` → `Create Account` → `🏢 Facility Owner Account` (`#role-btn-owner`) → Enter Owner Name `Vikas Shinde`, Phone `+91 98333 77004`, Email `vikas.owner.qa@apex.dev`, Password `OwnerPass@123`, Facility `Shinde Pro Turf`, City `Thane`, Area `Majiwada`, Rate `1300`, Sports `["cricket", "football"]` → Submit (`#form-register-owner`).
  2. **UI + REST API:** `POST /api/v1/auth/registration/send-otp` → Verify both OTPs via `POST /api/v1/auth/registration/verify-otp` with `role: "ROLE_OWNER"` (and verify compatibility with `"venue_owner"`), `facilityName`, `area`, `sports`, `startingPrice`.
  3. **UI:** Verify auto-navigation to `VenueOwnerPortal` (`#tab-partner`) displays `Shinde Pro Turf`.
  4. **Neon DB:** Query `apex_users`, `apex_venues`, and `apex_resources` / `apex_facilities`.
- **Expected Assertions:**
  - **UI:** Partner Portal loads immediately without `"Partner Access Required"` error; venue card shows `Shinde Pro Turf`, `Majiwada, Thane`, and at least 1 active court.
  - **REST API:** Returns HTTP `200` with JWT containing `role = "ROLE_OWNER"`; subsequent `GET /api/v1/owner/venues` with this JWT returns HTTP `200` containing the auto-provisioned venue.
  - **Neon SQL:**
    ```sql
    SELECT uid, role, is_phone_verified, is_email_verified FROM apex_users WHERE email = 'vikas.owner.qa@apex.dev';
    SELECT venue_id, owner_id, name, locality, city, status FROM apex_venues WHERE owner_id = '<owner_uid>';
    SELECT resource_id, venue_id, base_price_paise FROM apex_resources WHERE owner_id = '<owner_uid>';
    SELECT court_id, venue_id, base_hourly_rate_paise FROM apex_facilities WHERE venue_id = '<venue_id>';
    ```

### `TC-USR-004` — Facility Owner Registration with GST/PAN KYC (`/auth/register-owner`)
- **Persona / User Type:** Guest → Facility Owner (`ROLE_OWNER`)
- **Linked Requirement:** `BA-02`
- **Preconditions:** `app.owner.auto-approve=true` (or `false` for `PENDING_VERIFICATION` queue testing).
- **Steps:**
  1. **REST API:** `POST /api/v1/auth/register-owner` with `{ "ownerName": "Santosh QA", "businessName": "Ghodbunder Sports LLP", "phone": "+91 98190 77005", "email": "santosh.qa@apex.dev", "city": "Thane", "businessAddress": "Ghodbunder Rd, Thane West", "gstNumber": "27AAAAA1234A1Z5", "panNumber": "AAAAA1234A", "password": "OwnerPass@123" }`.
  2. **REST API:** Call `GET /api/v1/owner/venues` using the returned Bearer JWT.
  3. **Neon DB:** Inspect `apex_users`, `apex_venues`, `apex_resources`, and `apex_schedule_rules`.
- **Expected Assertions:**
  - **UI:** When signed in with this account, Partner Portal displays `"Ghodbunder Sports LLP Sports Arena"` and `"Main Box Cricket Arena 1"`.
  - **REST API:** HTTP `200`, `user.role = "ROLE_OWNER"`, `ownerProfile.gstNumber = "27AAAAA1234A1Z5"`, `ownerProfile.panNumber = "AAAAA1234A"`, `ownerProfile.verificationStatus = "APPROVED"` (or `"PENDING_VERIFICATION"` when `auto-approve=false`). `GET /api/v1/owner/venues` returns the auto-provisioned venue.
  - **Neon SQL:**
    ```sql
    SELECT uid, owner_business_name, owner_gst_number, owner_pan_number, owner_verification_status FROM apex_users WHERE email = 'santosh.qa@apex.dev';
    SELECT resource_id, weekly_schedule_json FROM apex_schedule_rules WHERE owner_id = '<owner_uid>';
    ```

### `TC-USR-005` — Passwordless Login via OTP (Mobile Number or Email)
- **Persona / User Type:** Player / Facility Owner
- **Linked Requirement:** `BA-01`
- **Preconditions:** Registered user `arjun.qa@apex.dev` (`+91 98201 77001`) exists in `apex_users`.
- **Steps:**
  1. **UI:** Open `UserAuthModal` → `Sign In` → Select `📱 Instant OTP Login` (`#btn-sub-otp`) → Enter `+91 98201 77001` (then repeat in second pass with `arjun.qa@apex.dev`) → Click `Send OTP` (`#btn-request-login-otp`).
  2. **REST API:** `POST /api/v1/auth/login/send-otp` with `{ "identifier": "+91 98201 77001" }`.
  3. **UI + REST API:** Enter 6-digit `loginOtp` into `#input-login-otp-code`, click `Verify & Sign In` (`#btn-verify-login-otp`) → `POST /api/v1/auth/login/verify-otp` `{ sessionId, otp }`.
  4. **Negative Check:** Call `POST /api/v1/auth/login/send-otp` with unregistered identifier `unknown.qa@apex.dev`.
- **Expected Assertions:**
  - **UI:** Modal switches to authenticated state; `localStorage.getItem('apex_jwt_token')` is populated with valid JWT; redirects to `PlayerPortal` (`bookings`) for Player or `VenueOwnerPortal` (`partner`) for Owner.
  - **REST API:** `send-otp` returns `200` with `sessionId` (`otp_login_*`); `verify-otp` returns `200` with valid JWT and user object; unknown identifier returns `400`.
  - **Neon SQL:**
    ```sql
    SELECT session_id, user_id, purpose, verified, attempts_remaining FROM apex_otp_sessions WHERE session_id = '<session_id>';
    -- Expected: purpose='LOGIN', verified=TRUE
    ```

### `TC-USR-006` — Credential Login via Password & Invalid Password Rejection
- **Persona / User Type:** Player / Facility Owner
- **Linked Requirement:** `BA-01`
- **Preconditions:** Registered user `demo_player` (`rohan.deshmukh@apex.dev`, password `password123`) exists in `apex_users`.
- **Steps:**
  1. **UI:** Open `UserAuthModal` → `🔐 Password Login` → Enter `rohan.deshmukh@apex.dev` + `wrongpass` → Submit (`#btn-submit-password-login`).
  2. **UI:** Enter `rohan.deshmukh@apex.dev` + `password123` → Submit.
  3. **REST API:** Execute `POST /api/v1/auth/login/password` for wrong password, then right password, then phone login (`+91 98201 12345`).
- **Expected Assertions:**
  - **UI:** Wrong password displays red `#auth-feedback` (`Incorrect password...`) and does NOT fall back to local mock login; valid password closes modal and updates header avatar to `RD`.
  - **REST API:** Wrong password returns HTTP `400`/`401` (`success: false`); valid email or phone returns HTTP `200` with valid 24h JWT (`exp - iat = 86400s`).
  - **Neon SQL:**
    ```sql
    SELECT uid, email, phone, password_hash FROM apex_users WHERE email = 'rohan.deshmukh@apex.dev';
    -- Expected: row unchanged, password_hash matches SHA-256/BCrypt hash of 'password123'
    ```

### `TC-USR-007` — Stateless Logout & Token Teardown
- **Persona / User Type:** Player / Facility Owner
- **Linked Requirement:** `BA-01`
- **Preconditions:** User is logged in with active `apex_jwt_token` and `apex_active_user_id` in `localStorage`.
- **Steps:**
  1. **UI:** Click `#btn-portal-logout` in `PlayerPortal` (or `#btn-signout-active` in `UserAuthModal`).
  2. **REST API:** Call `POST /api/v1/auth/logout` with Bearer token.
  3. **UI + REST API:** Attempt protected action `GET /api/v1/bookings/my` without Bearer token.
- **Expected Assertions:**
  - **UI:** Header reverts to `Guest`; `PlayerPortal` renders `"Access Your Game Passes"` prompt; `localStorage.getItem('apex_jwt_token')` is `null` and `apex_active_user_id` is `'guest'`.
  - **REST API:** `POST /api/v1/auth/logout` returns HTTP `200` (`"Logged out successfully"`). Subsequent unauthenticated `GET /api/v1/bookings/my` returns HTTP `401`/`403` (once `demo_player` fallback is disabled per Gap `GAP-SEC-01`).
  - **Neon SQL:** No unintended mutation in `apex_users`.

### `TC-USR-008` — Player Profile Read & Update (`/api/v1/users/me`)
- **Persona / User Type:** Player (`ROLE_PLAYER`)
- **Linked Requirement:** `BA-01`
- **Preconditions:** Authenticated as `arjun.qa@apex.dev` with Bearer JWT.
- **Steps:**
  1. **UI:** Navigate to `🎟️ My Passes` (`PlayerPortal`) → Inspect displayed name, phone, email, city, loyalty points, and OTP verified badge; open Profile Edit (when wired per Gap `GAP-UI-03`).
  2. **REST API:** Call `GET /api/v1/users/me` with Bearer JWT.
  3. **REST API:** Call `PUT /api/v1/users/me` with `{ "fullName": "Arjun Verma Updated", "phone": "+91 98201 77001", "city": "Navi Mumbai", "preferredSports": ["PICKLEBALL", "BADMINTON"] }`.
  4. **REST API Negative Check:** Attempt `GET /api/v1/users/me` anonymously or with `?fallbackUid=demo_player`.
- **Expected Assertions:**
  - **UI:** Profile card reflects `Arjun Verma Updated`, `Navi Mumbai`, and updated preferred sports.
  - **REST API:** `GET /users/me` returns HTTP `200` matching JWT `uid` only; `PUT /users/me` returns HTTP `200` with updated fields; anonymous `fallbackUid` does NOT expose `demo_player` data (`401` or `null` Guest response).
  - **Neon SQL:**
    ```sql
    SELECT full_name, city, preferred_sports_json, updated_at FROM apex_users WHERE email = 'arjun.qa@apex.dev';
    -- Expected: full_name='Arjun Verma Updated', city='Navi Mumbai', preferred_sports_json='["PICKLEBALL","BADMINTON"]'
    ```

### `TC-USR-009` — Facility Owner Profile Read, Update & Cross-User `/users/{uid}` Isolation
- **Persona / User Type:** Facility Owner (`ROLE_OWNER`)
- **Linked Requirement:** `BA-01`, `BA-02`
- **Preconditions:** Authenticated as Owner A (`vikas.owner.qa@apex.dev`); Player A (`arjun.qa@apex.dev`) also exists.
- **Steps:**
  1. **REST API (Owner Self):** Call `GET /api/v1/users/me` and `PUT /api/v1/users/me` with Owner A's JWT updating `fullName`, `city`, and `phone`.
  2. **REST API (Cross-Tenant PII Probe):** Using Player A's JWT, call `GET /api/v1/users/{ownerA_uid}`.
  3. **UI:** Reload `VenueOwnerPortal` and verify updated owner profile metadata.
- **Expected Assertions:**
  - **UI:** Updated owner name and city persist across page reload.
  - **REST API:** Owner `PUT /users/me` returns HTTP `200` and preserves `role = "ROLE_OWNER"` (role escalation to `ROLE_ADMIN` rejected). Cross-user `GET /api/v1/users/{ownerA_uid}` by Player A MUST return `403 Forbidden` and MUST NEVER leak `passwordHash`, `gstNumber`, or `panNumber`.
  - **Neon SQL:**
    ```sql
    SELECT uid, full_name, role, city, owner_gst_number FROM apex_users WHERE uid = '<ownerA_uid>';
    ```

### `TC-USR-010` — Duplicate Phone & Duplicate Email Registration Rejection
- **Persona / User Type:** Guest
- **Linked Requirement:** `BA-01`, Schema Unique Contract
- **Preconditions:** User `arjun.qa@apex.dev` with phone `+91 98201 77001` already registered in `apex_users`.
- **Steps:**
  1. **UI:** Open `UserAuthModal` → `Create Account` → Attempt registration with same phone `+91 98201 77001` and a new email `new.qa@apex.dev`; then retry with new phone `+91 98201 77099` and existing email `arjun.qa@apex.dev`.
  2. **REST API:** Send `POST /api/v1/auth/registration/send-otp`, `POST /api/v1/auth/register`, `POST /api/v1/auth/register-profile`, and `POST /api/v1/auth/register-owner` with duplicate phone (`9820177001` or `+91 98201 77001`) and duplicate email (`ARJUN.QA@apex.dev`).
  3. **Neon DB:** Verify row count in `apex_users`.
- **Expected Assertions:**
  - **UI:** Displays error `"An account is already registered with this mobile number / email. Please sign in instead."` before proceeding.
  - **REST API:** All 4 endpoints reject duplicate normalized phone or case-insensitive email with HTTP `409 Conflict` / `400 Bad Request` (currently flagged in Gap `GAP-AUTH-01`).
  - **Neon SQL:**
    ```sql
    SELECT COUNT(*) FROM apex_users WHERE LOWER(email) = 'arjun.qa@apex.dev'; -- Must equal 1
    SELECT COUNT(*) FROM apex_users WHERE phone LIKE '%98201%77001%';       -- Must equal 1
    ```

### `TC-USR-011` — 3-Attempt OTP Brute-Force Lockout & 300s Expiry Enforcement
- **Persona / User Type:** Guest / Player
- **Linked Requirement:** `BA-01`, `AC-01.2`
- **Preconditions:** Active registration OTP session (`otp_reg_*`, `attempts_remaining = 3`) and login OTP session (`otp_login_*`, `attempts_remaining = 3`).
- **Steps:**
  1. **UI + REST API (Registration Lockout):** Submit wrong `phoneOtp = "000000"` 3 consecutive times to `POST /api/v1/auth/registration/verify-otp`. Then submit the *correct* OTPs on Attempt #4.
  2. **UI + REST API (Login Lockout):** Submit wrong `otp = "000000"` 3 consecutive times to `POST /api/v1/auth/login/verify-otp`. Then submit the *correct* OTP on Attempt #4.
  3. **Expiry Check:** Set/wait until `expires_at < NOW()` (or `> 300s`) and submit valid OTP.
  4. **UI Resend Check:** Click `#btn-otp-resend` to confirm fresh session resets `attempts_remaining = 3` and timer to `05:00`.
- **Expected Assertions:**
  - **UI:** Feedback decrements attempts (`2 attempts left`, `1 attempts left`, `0 attempts left`), then blocks Attempt #4 with `"Maximum verification attempts exceeded. Please request fresh OTPs."` (fix client `OtpService.verifyLoginOtp` per Gap `GAP-AUTH-02`).
  - **REST API:** Attempts 1–3 return HTTP `400` with remaining attempts; Attempt 4 (even with valid OTP) returns HTTP `400/423` (`"Maximum verification attempts exceeded"`). Expired session returns HTTP `400` (`"OTP codes have expired"`).
  - **Neon SQL:**
    ```sql
    SELECT session_id, attempts_remaining, verified, expires_at FROM apex_otp_sessions WHERE session_id = '<session_id>';
    -- Expected: attempts_remaining = 0, verified = FALSE
    ```

### `TC-USR-012` — Admin Partner KYC Pending Queue & Approval Governance
- **Persona / User Type:** Webhook/Admin (`ROLE_ADMIN`) vs Unauthorized Player (`ROLE_PLAYER`)
- **Linked Requirement:** `BA-01`, `BA-02`
- **Preconditions:** Owner user `usr_pending_qa` created with `ownerProfile.verificationStatus = "PENDING_VERIFICATION"`.
- **Steps:**
  1. **REST API (RBAC Negative):** Call `GET /api/v1/admin/owners/pending` using a `ROLE_PLAYER` JWT and a `ROLE_OWNER` JWT.
  2. **REST API (Admin Read):** Call `GET /api/v1/admin/owners/pending` using `demo_admin` (`ROLE_ADMIN`) JWT.
  3. **REST API (Admin Approve):** Call `PATCH /api/v1/admin/owners/usr_pending_qa/verify` with `{ "status": "APPROVED" }`.
  4. **Neon DB:** Verify `owner_verification_status` and `owner_verified_at`.
- **Expected Assertions:**
  - **UI:** N/A (Admin KYC queue missing from `AdminConsole.js` — logged in Gap `GAP-UI-03`).
  - **REST API:** Player/Owner calls return HTTP `403 Forbidden`. Admin `GET` returns pending list; `PATCH` returns HTTP `200` with `verificationStatus = "APPROVED"` and non-null `verifiedAt`.
  - **Neon SQL:**
    ```sql
    SELECT uid, owner_verification_status, owner_verified_at FROM apex_users WHERE uid = 'usr_pending_qa';
    -- Expected: owner_verification_status = 'APPROVED', owner_verified_at > 0
    ```

---

## 4. Suite 2: Facility, Venue, Court, Schedule & Override Workflows (`TC-FAC-001` – `TC-FAC-010`)

### `TC-FAC-001` — Public Venue Search & Multi-Criteria Filtering (City, Sport, Locality, Price, Keyword)
- **Persona / User Type:** Guest / Player
- **Linked Requirement:** `BA-02`, `BA-08`
- **Preconditions:** Seeded venues exist across `thane`, `mumbai`, and `navi mumbai` in `apex_venues` with `status = 'ACTIVE'`.
- **Steps:**
  1. **UI:** Open `VenueExplorer` → Select City `Thane` (`#filter-city`) → Click Sport Chip `Box Cricket` (`[data-sport-id="cricket"]`) → Type `Ghodbunder` in `#filter-search` → Toggle Quick Pills (`#pill-top-rated`, `#pill-floodlights`, `#pill-parking`) → Change Sort (`#filter-sort` to `priceAsc` / `ratingDesc`).
  2. **REST API:** Execute `GET /api/v1/venues?city=thane&sportType=CRICKET_TURF`, `GET /api/v1/venues?locality=Bandra%20West&minPrice=50000&maxPrice=200000&hasActiveOffers=true`, and `GET /api/v1/venues?q=pickleball&page=0&size=10`.
  3. **REST API (Sports Catalog):** Execute `GET /api/v1/sports`.
- **Expected Assertions:**
  - **UI:** Count badge (`#venue-count-badge`) updates accurately; only matching active venues display; `#pill-reset-filters` restores all venues.
  - **REST API:** HTTP `200`; `GET /api/v1/sports` returns all 8 `SportType` entries; all returned `VenueRecord` items have `status = "ACTIVE"` and match filter predicates, sorted by `rating` desc; latency `< 200ms` p95 on Caffeine cache hit.
  - **Neon SQL:**
    ```sql
    SELECT venue_id, name, city, locality, min_slot_price_paise, status FROM apex_venues WHERE LOWER(city) = 'thane' AND status = 'ACTIVE';
    ```

### `TC-FAC-002` — View Venue Details & Granular Court/Resource Specs (Capacity, Dimensions, Surface, Floodlights, Paise Rates)
- **Persona / User Type:** Guest / Player
- **Linked Requirement:** `BA-02`
- **Preconditions:** `venue_thane_01` has multiple resources (`res_thane_crk_01`, `res_thane_fb_01`, `res_thane_badm_01`) and courts (`court_thane_01_crk`, `court_thane_01_fb`).
- **Steps:**
  1. **UI:** Click `Book Slots →` (`.btn-select-venue`) on `Apex Arena Ghodbunder` → Inspect `SlotCalendar` court tabs (`.court-tab`) and `VenueOwnerPortal` court inventory cards.
  2. **REST API:** Call `GET /api/v1/venues/venue_thane_01` and `GET /api/v1/venues/venue_thane_01/courts`.
  3. **Negative Check:** Call `GET /api/v1/venues/venue_nonexistent_999`.
- **Expected Assertions:**
  - **UI:** Court cards display `name`, `sport`, `surface`, `dimensions`, `capacity` (in Partner Portal), indoor/outdoor floodlit status, and base/odd-hour rates.
  - **REST API:** `GET /venues/{id}` returns `data.venue` and `data.resources` with `maxPlayers`, `surfaceType`, `isIndoor`, `slotDurationMinutes`, `basePricePaise`, `weekendPricePaise`, `peakHourPricePaise`; `GET /venues/{id}/courts` returns `FacilityRecord` specs (`capacity`, `dimensions`, `surface`, `hasFloodlights`, `baseHourlyRate`, `peakHourlyRate`). Unknown ID returns `400/404`.
  - **Neon SQL:**
    ```sql
    SELECT resource_id, name, max_players, surface_type, is_indoor, base_price_paise, weekend_price_paise, peak_hour_price_paise FROM apex_resources WHERE venue_id = 'venue_thane_01';
    SELECT court_id, name, capacity, dimensions, surface, has_floodlights, base_hourly_rate_paise FROM apex_facilities WHERE venue_id = 'venue_thane_01';
    ```

### `TC-FAC-003` — Owner Create New Venue (`POST /api/v1/owner/venues` & `AddVenueModal`)
- **Persona / User Type:** Facility Owner (`ROLE_OWNER`)
- **Linked Requirement:** `BA-02`
- **Preconditions:** Authenticated as Owner A (`ROLE_OWNER`).
- **Steps:**
  1. **UI:** In `VenueOwnerPortal`, click `+ Add New Facility` (`#btn-owner-add-venue`) → Fill `AddVenueModal`: Name `Kolshet Smash Arena`, City `Thane`, Area `Kolshet Road`, Address `Plot 12, Kolshet Rd, Thane 400607`, Price `1100`, Sports `["cricket", "pickleball"]`, Amenities `["FIFA-grade Turf", "LED Floodlights"]` → Submit (`#form-add-venue`).
  2. **REST API:** `POST /api/v1/owner/venues` with Owner A JWT and `{ "name": "Kolshet Smash Arena", "locality": "Kolshet Road", "city": "thane", "addressLine": "Plot 12, Kolshet Rd", "pincode": "400607", "sportTypes": ["CRICKET_TURF", "PICKLEBALL"], "amenities": ["LED Floodlights"], "minSlotPricePaise": 110000 }`.
  3. **Cache Eviction Check:** Immediately call public `GET /api/v1/venues?city=thane`.
- **Expected Assertions:**
  - **UI:** Success message appears; `Kolshet Smash Arena` appears in `VenueExplorer` and `VenueOwnerPortal`.
  - **REST API:** HTTP `200` with `ownerId = <ownerA_uid>`, `status = "ACTIVE"`, `minSlotPricePaise = 110000`; public `GET /api/v1/venues?city=thane` immediately includes the new venue (`@CacheEvict` verified).
  - **Neon SQL:**
    ```sql
    SELECT venue_id, owner_id, name, city, min_slot_price_paise, status FROM apex_venues WHERE name = 'Kolshet Smash Arena';
    ```

### `TC-FAC-004` — Owner Create Court / Playable Resource & Facility Specs
- **Persona / User Type:** Facility Owner (`ROLE_OWNER`)
- **Linked Requirement:** `BA-02`, `AC-02.1`
- **Preconditions:** Venue `venue_kolshet_qa` owned by Owner A exists.
- **Steps:**
  1. **UI:** Click `+ Add Court / Facility` (`#btn-owner-add-court`) in `VenueOwnerPortal` → Enter Name `Turf Alpha 8v8`, Sport `cricket`, Capacity `16`, Surface `50mm Synthetic AstroTurf`, Environment `Outdoor Floodlit`, Base `1200`, Peak `1600`, Off-Peak `35` → Submit (`#form-add-court`).
  2. **REST API (Resource Model):** `POST /api/v1/owner/venues/{venueId}/resources` with `{ "name": "Turf Alpha 8v8", "sportTypes": ["CRICKET_TURF"], "slotDurationMinutes": 60, "basePricePaise": 120000, "weekendPricePaise": 156000, "peakHourPricePaise": 150000, "maxPlayers": 16, "surfaceType": "ARTIFICIAL_TURF", "isIndoor": false }`.
  3. **REST API (Facility Model):** `POST /api/v1/facilities` with `{ "venueId": "{venueId}", "name": "Turf Alpha 8v8", "sport": "cricket", "capacity": 16, "surface": "15mm Monofilament Synthetic Turf", "dimensions": "115ft x 65ft", "baseHourlyRate": 1200, "peakHourlyRate": 1600, "oddHourDiscountPct": 35, "hasFloodlights": true }`.
  4. **Validation Negative Check:** Submit `POST /api/v1/facilities` with `baseHourlyRate: 100` (`< ₹200`).
- **Expected Assertions:**
  - **UI:** New court card appears in `VenueOwnerPortal` with `16 Players`, `₹1200/hr`, `₹1600/hr`, and becomes selectable in `SlotCalendar`.
  - **REST API:** Resource creation returns `200`; Facility creation returns `201 Created`; `baseHourlyRate < 200` returns `400 Bad Request`; `GET /api/v1/availability?resourceId={resourceId}&date={tomorrow}` immediately generates slots.
  - **Neon SQL:**
    ```sql
    SELECT resource_id, venue_id, base_price_paise, max_players FROM apex_resources WHERE venue_id = '<venueId>';
    SELECT resource_id, timezone, weekly_schedule_json FROM apex_schedule_rules WHERE venue_id = '<venueId>';
    SELECT court_id, venue_id, base_hourly_rate_paise, dimensions, capacity FROM apex_facilities WHERE venue_id = '<venueId>';
    ```

### `TC-FAC-005` — Owner Update Venue Details & Cancellation Policy
- **Persona / User Type:** Facility Owner (`ROLE_OWNER`)
- **Linked Requirement:** `BA-02`, `BA-06`
- **Preconditions:** Owner A owns `venue_kolshet_qa`.
- **Steps:**
  1. **REST API:** Call `PUT /api/v1/owner/venues/{venueId}` with Owner A's JWT updating `name` to `Kolshet Smash Arena Pro`, `minSlotPricePaise` to `95000`, `amenities` to `["FIFA-grade Turf", "Shower Suites"]`, and `cancellationPolicy` `{ "fullRefundHoursBefore": 24, "partialRefundHoursBefore": 6, "partialRefundPercent": 50 }`.
  2. **REST API (Cache Check):** Call `GET /api/v1/venues/{venueId}` and `GET /api/v1/venues?city=thane`.
- **Expected Assertions:**
  - **UI:** Venue card in `VenueExplorer` reflects updated name `Kolshet Smash Arena Pro` and `₹950/hr` (once UI edits/fetches live API per Gap `GAP-UI-03`).
  - **REST API:** `PUT` returns HTTP `200`; subsequent `GET` returns updated fields immediately (`@CacheEvict` clears `venues` cache).
  - **Neon SQL:**
    ```sql
    SELECT name, min_slot_price_paise, amenities_json, cancellation_policy_json, updated_at FROM apex_venues WHERE venue_id = '<venueId>';
    ```

### `TC-FAC-006` — Owner Update Court / Resource Specs & Paise Pricing
- **Persona / User Type:** Facility Owner (`ROLE_OWNER`)
- **Linked Requirement:** `BA-02`, `BA-03`
- **Preconditions:** Resource `res_alpha_qa` belongs to Owner A.
- **Steps:**
  1. **REST API:** Call `PUT /api/v1/owner/resources/{resourceId}` with `{ "name": "Turf Alpha 8v8 Floodlit", "sportTypes": ["CRICKET_TURF", "MULTI_SPORT"], "slotDurationMinutes": 60, "basePricePaise": 135000, "weekendPricePaise": 175000, "peakHourPricePaise": 165000, "maxPlayers": 18, "surfaceType": "FIFA_PRO_TURF", "isIndoor": false }`.
  2. **REST API:** Call `GET /api/v1/availability?resourceId={resourceId}&date={weekday_future}` and `GET /api/v1/venues/{venueId}`.
- **Expected Assertions:**
  - **UI:** Updated price `₹1,350` and capacity `18 Players` reflected in slot grid and court card.
  - **REST API:** HTTP `200`; generated standard slot returns `originalPricePaise = 135000`; peak slot (`19:00`) returns `165000`.
  - **Neon SQL:**
    ```sql
    SELECT name, base_price_paise, weekend_price_paise, peak_hour_price_paise, max_players, surface_type FROM apex_resources WHERE resource_id = '<resourceId>';
    ```

### `TC-FAC-007` — Owner Update Weekly Schedule Rule & Date Override (Closure, Blocked Slots, Holiday Surge)
- **Persona / User Type:** Facility Owner (`ROLE_OWNER`)
- **Linked Requirement:** `BA-02`, `BA-03`
- **Preconditions:** Resource `res_alpha_qa` owned by Owner A; future dates `D1` (partial block + custom price) and `D2` (full day closure).
- **Steps:**
  1. **REST API (Schedule Rule):** `PUT /api/v1/owner/resources/{resourceId}/schedule` updating `MONDAY` open `07:00`, close `22:00`, peak windows `[{ "start": "18:00", "end": "21:00" }]`.
  2. **REST API (Date Override D1):** `PUT /api/v1/owner/resources/{resourceId}/overrides/{D1}` with `{ "isClosedFullDay": false, "blockedSlots": { "15:00": { "type": "MAINTENANCE", "reason": "Turf brushing" } }, "slotPriceOverrides": { "19:00": 200000 } }`.
  3. **REST API (Date Override D2):** `PUT /api/v1/owner/resources/{resourceId}/overrides/{D2}` with `{ "isClosedFullDay": true, "closureReason": "Monsoon Drainage Maintenance" }`.
  4. **REST API (Slot Query):** Query `GET /api/v1/availability` for `D1` and `D2`.
- **Expected Assertions:**
  - **UI:** On `D1`, `15:00` displays as Blocked/Maintenance and `19:00` shows `₹2,000`; on `D2`, all slots display `BLOCKED` with closure reason.
  - **REST API:** Both `PUT` calls return HTTP `200`. On `D1`, `15:00` has `status = "BLOCKED"`, `blockedType = "MAINTENANCE"`, and `19:00` has `originalPricePaise = 200000`. On `D2`, all slots have `status = "BLOCKED"`.
  - **Neon SQL:**
    ```sql
    SELECT resource_id, weekly_schedule_json FROM apex_schedule_rules WHERE resource_id = '<resourceId>';
    SELECT override_id, is_closed_full_day, closure_reason, blocked_slots_json, slot_price_overrides_json FROM apex_date_overrides WHERE resource_id = '<resourceId>';
    ```

### `TC-FAC-008` — Owner Delete Venue, Court/Resource & Facility (Cascade & Active Booking Guard)
- **Persona / User Type:** Facility Owner (`ROLE_OWNER`)
- **Linked Requirement:** Stakeholder CRUD Matrix (Venue/Court/Facility Delete)
- **Preconditions:** Venue `venue_del_qa` owned by Owner A with court `res_del_qa` / `court_del_qa`.
- **Steps:**
  1. **UI (Local Fallback Check):** Trigger `VenueService.deleteVenue(venueId)` (or UI delete button once wired).
  2. **REST API (Expected Contract):**
     - `DELETE /api/v1/owner/resources/{resourceId}` (or `DELETE /api/v1/facilities/{courtId}`)
     - `DELETE /api/v1/owner/venues/{venueId}`
  3. **Active Booking Guard:** Attempt to delete a resource/venue that has a future `CONFIRMED` booking.
  4. **Public Discovery Check:** Call `GET /api/v1/venues` and `GET /api/v1/venues/{venueId}`.
- **Expected Assertions:**
  - **UI:** Deleted venue/court disappears from `VenueExplorer` and `VenueOwnerPortal`.
  - **REST API:** Clean delete returns HTTP `200`/`204`; active future booking blocks hard delete with `409 Conflict` (or requires cancelling bookings first); subsequent `GET /api/v1/venues` excludes deleted/inactive venue. *(Note: Backend `DELETE` endpoints are currently MISSING — see Gap `GAP-CRUD-01`.)*
  - **Neon SQL:**
    ```sql
    SELECT venue_id, status FROM apex_venues WHERE venue_id = '<venueId>';         -- Deleted or status = 'INACTIVE'/'DELETED'
    SELECT resource_id, is_active FROM apex_resources WHERE venue_id = '<venueId>'; -- Deleted or is_active = FALSE
    SELECT court_id, status FROM apex_facilities WHERE venue_id = '<venueId>';      -- Deleted or status = 'INACTIVE'
    ```

### `TC-FAC-009` — Unauthorized Cross-Owner & Player Mutation Rejection (`403 Forbidden`)
- **Persona / User Type:** Player (`ROLE_PLAYER`) & Facility Owner B (`ROLE_OWNER`)
- **Linked Requirement:** `BA-01`, `BA-02`, `AC-02.2`
- **Preconditions:** Owner A owns `venue_A` and `res_A`; Owner B owns `venue_B`; Player A has `ROLE_PLAYER` JWT.
- **Steps:**
  1. **REST API (Player → Owner Endpoints):** Using Player A's JWT, send `POST /api/v1/owner/venues`, `PUT /api/v1/owner/venues/{venue_A}`, `POST /api/v1/facilities`, `PUT /api/v1/owner/resources/{res_A}`, and `DELETE /api/v1/owner/venues/{venue_A}`.
  2. **REST API (Owner B → Owner A Resources):** Using Owner B's JWT, send `PUT /api/v1/owner/venues/{venue_A}`, `POST /api/v1/owner/venues/{venue_A}/resources`, `PUT /api/v1/owner/resources/{res_A}`, `PUT /api/v1/owner/resources/{res_A}/schedule`, `PUT /api/v1/owner/resources/{res_A}/overrides/2026-12-20`, and `POST /api/v1/facilities` with `venueId = venue_A`.
  3. **UI Check:** Sign in as Player A and navigate to `Partner Portal` (`#tab-partner`).
- **Expected Assertions:**
  - **UI:** `VenueOwnerPortal` displays `"Partner Access Required"` and prevents access to management actions.
  - **REST API:** All cross-role (`ROLE_PLAYER`) and cross-tenant (Owner B → Owner A) requests MUST return HTTP **`403 Forbidden`** with zero database changes. *(Note: Currently `POST /api/v1/facilities` lacks RBAC & owner checks and `GlobalExceptionHandler` maps `IllegalStateException("Unauthorized")` to `400` — see Gaps `GAP-SEC-02`, `GAP-SEC-03`.)*
  - **Neon SQL:**
    ```sql
    SELECT name, owner_id, updated_at FROM apex_venues WHERE venue_id = '<venue_A>'; -- Unchanged
    ```

### `TC-FAC-010` — Admin Venue Moderation Status Governance (`ACTIVE` ↔ `INACTIVE`)
- **Persona / User Type:** Webhook/Admin (`ROLE_ADMIN`)
- **Linked Requirement:** `BA-02`, Master API Matrix
- **Preconditions:** `venue_thane_02` is `ACTIVE`.
- **Steps:**
  1. **REST API (Admin):** `PATCH /api/v1/admin/venues/venue_thane_02/status` with `{ "status": "INACTIVE" }`.
  2. **REST API (Discovery):** Call `GET /api/v1/venues?city=thane`.
  3. **REST API (Restore):** `PATCH /api/v1/admin/venues/venue_thane_02/status` with `{ "status": "ACTIVE" }`.
- **Expected Assertions:**
  - **UI:** Suspended venue is hidden from public `VenueExplorer` discovery grid.
  - **REST API:** `PATCH` returns HTTP `200` (`status: "INACTIVE"`); `GET /api/v1/venues` excludes inactive venue (note: verify `@CacheEvict(value="venues")` runs on status update!). Restoring returns it to active discovery.
  - **Neon SQL:**
    ```sql
    SELECT venue_id, status, updated_at FROM apex_venues WHERE venue_id = 'venue_thane_02';
    ```

---

## 5. Suite 3: Real-Time Slots, Dynamic Pricing Matrix & Concurrency Locks (`TC-PRC-001` – `TC-PRC-011`)

### `TC-PRC-001` — Real-Time Slot Generation (Zero Empty Rows, IST Past Marking, Overrides)
- **Persona / User Type:** Guest / Player
- **Linked Requirement:** `BA-03`, `BA-08`
- **Preconditions:** Resource `res_thane_crk_01` has `ScheduleRule` (`06:00–23:00`, `60 min` duration) and NO pre-generated slot rows in DB.
- **Steps:**
  1. **REST API (Today):** Call `GET /api/v1/availability?resourceId=res_thane_crk_01&date={today_IST}` at e.g. `14:30 IST`.
  2. **REST API (Future Date):** Call `GET /api/v1/availability?resourceId=res_thane_crk_01&date={today+3d}` and `GET /api/v1/slots?courtId=court_thane_01_crk&date={today+3d}`.
  3. **UI:** Select `Apex Arena Ghodbunder` in `VenueExplorer` and inspect `SlotCalendar` pills.
- **Expected Assertions:**
  - **UI:** Slots render in `SlotCalendar`; booked slots show `Sold Out`, held slots show `In Checkout`, off-peak slots show `-XX%` badge.
  - **REST API:** HTTP `200`; today's slots before current IST time return `status = "PAST"`; future date returns 17+ `AVAILABLE` slots with computed `originalPricePaise` & `finalPricePaise`.
  - **Neon SQL:**
    ```sql
    SELECT COUNT(*) FROM apex_slot_locks WHERE resource_id = 'res_thane_crk_01' AND booking_date = '<today+3d>';
    -- Expected: 0 rows (no pre-generated empty slot rows)
    ```

### `TC-PRC-002` — Early Bird Off-Peak Auto-Discount (`06:00–08:00`, `-20%`)
- **Persona / User Type:** Player (`ROLE_PLAYER`)
- **Linked Requirement:** `BA-03`
- **Preconditions:** Weekday date (e.g., Wednesday); `res_thane_crk_01` base rate `120000` paise (`₹1,200`).
- **Steps:**
  1. **UI:** Open `SlotCalendar` → Filter `🌅 Morning` (`[data-band="morning"]`) → Select `06:00` or `07:00` slot.
  2. **REST API:** Call `POST /api/v1/pricing/quote` with `{ "venueId": "venue_thane_01", "resourceId": "res_thane_crk_01", "date": "<weekday>", "slots": ["07:00"] }`.
- **Expected Assertions:**
  - **UI:** Slot shows `-20%` badge (`Early Bird`), base `₹1,200`, discount `-₹240`, subtotal `₹960`.
  - **REST API:** `subtotalPaise = 120000`, `discountAmountPaise = 24000`, `finalPayablePaise = 96000`. *(Note: Backend `FirebaseService.seedInitialData` currently omits Early Bird `OFF_PEAK_AUTO` rule — see Gap `GAP-PRC-01`.)*
  - **Neon SQL:**
    ```sql
    SELECT discount_id, type, discount_value, time_window_start, time_window_end, is_active FROM apex_discounts WHERE type = 'OFF_PEAK_AUTO';
    ```

### `TC-PRC-003` — Afternoon Grind Off-Peak Auto-Discount (`11:00–16:00`, `-35%`)
- **Persona / User Type:** Player (`ROLE_PLAYER`)
- **Linked Requirement:** `BA-03`, `AC-03.1`
- **Preconditions:** Weekday date; `res_thane_crk_01` base rate `120000` paise (`₹1,200`).
- **Steps:**
  1. **UI:** Open `SlotCalendar` → Click `☀️ Afternoon` (`[data-band="afternoon"]`) → Select `13:00` (and verify `14:00`).
  2. **REST API:** Call `GET /api/v1/availability?resourceId=res_thane_crk_01&date=<weekday>` and `POST /api/v1/pricing/quote` for `["13:00"]`.
- **Expected Assertions:**
  - **UI:** `13:00` pill displays purple `-35%` tag and `₹780` (`₹1,200` struck through).
  - **REST API:** `subtotalPaise = 120000`, `discountAmountPaise = 42000` (35% of `120000`), `finalPayablePaise = 78000` (`₹780`). *(Note: Verify seeded `disc_offpeak` aligns at 35% vs 25% — Gap `GAP-PRC-01`.)*
  - **Neon SQL:**
    ```sql
    SELECT discount_id, discount_value, time_window_start, time_window_end, days_of_week_json FROM apex_discounts WHERE discount_id = 'disc_offpeak';
    ```

### `TC-PRC-004` — Night Owl Off-Peak Auto-Discount (`23:00–02:00`, `-25%`)
- **Persona / User Type:** Player (`ROLE_PLAYER`)
- **Linked Requirement:** `BA-03`
- **Preconditions:** Resource schedule configured with Night Owl operating hours extending to `02:00` (or `23:00–00:00` band) and Night Owl `25%` rule active.
- **Steps:**
  1. **UI:** Open `SlotCalendar` → Click `🌙 Night Owl` (`[data-band="night"]`) → Select `23:00` slot.
  2. **REST API:** Call `POST /api/v1/pricing/quote` for `slots: ["23:00"]`.
- **Expected Assertions:**
  - **UI:** Slot shows `-25%` Night Owl badge (`₹900` on `₹1,200` base).
  - **REST API:** `subtotalPaise = 120000`, `discountAmountPaise = 30000` (25%), `finalPayablePaise = 90000`. *(Note: Backend default schedule closes at `23:00` and `disc_nightowl` is not seeded — Gap `GAP-PRC-01`.)*
  - **Neon SQL:** Verify Night Owl `OFF_PEAK_AUTO` rule in `apex_discounts` and schedule close time in `apex_schedule_rules`.

### `TC-PRC-005` — Prime Evening Peak & Weekend Surge Pricing (`+20%` / `+30%` & DateOverride Precedence)
- **Persona / User Type:** Player (`ROLE_PLAYER`)
- **Linked Requirement:** `BA-03`
- **Preconditions:** `res_thane_crk_01` has `basePricePaise = 120000`, `peakHourPricePaise = 150000` (`1.25x`), `weekendPricePaise = 156000` (`1.30x`); peak window `18:00–22:00`.
- **Steps:**
  1. **REST API (Weekday Peak):** `POST /api/v1/pricing/quote` on a weekday at `["19:00"]`.
  2. **REST API (Weekend Non-Peak):** `POST /api/v1/pricing/quote` on a Saturday/Sunday at `["10:00"]`.
  3. **REST API (DateOverride Event Rate):** Set `slotPriceOverrides: { "19:00": 200000 }` via `DateOverride`, then call `POST /api/v1/pricing/quote` at `["19:00"]`.
  4. **UI:** Inspect `🌆 Prime Evening` tab in `SlotCalendar`.
- **Expected Assertions:**
  - **UI:** Prime Evening pills display red `PEAK` badge and surged rate (`₹1,440` in client 1.2x or `₹1,600` court peak rate).
  - **REST API:** Precedence strictly resolves: `DateOverride` (`200000`) > `PeakHour` (`150000`) > `Weekend` (`156000`) > `Base` (`120000`).
  - **Neon SQL:**
    ```sql
    SELECT base_price_paise, weekend_price_paise, peak_hour_price_paise FROM apex_resources WHERE resource_id = 'res_thane_crk_01';
    ```

### `TC-PRC-006` — Bulk Multi-Slot Auto-Discount (`2+` Slots `-15%`) & Highest Auto-Discount Tie-Breaker
- **Persona / User Type:** Player (`ROLE_PLAYER` / Tournament Organizer)
- **Linked Requirement:** `BA-03`, `AC-03.2`
- **Preconditions:** `disc_bulk` (`BULK_SLOTS_AUTO`, `minSlots = 2`, `15%`) active in `apex_discounts`.
- **Steps:**
  1. **REST API (2 Evening Slots, No Promo):** `POST /api/v1/pricing/quote` with `resourceId: "res_thane_crk_01"`, `slots: ["19:00", "20:00"]`, `promoCode: null`.
  2. **REST API (2 Afternoon Slots — Off-Peak vs Bulk Tie-Breaker):** `POST /api/v1/pricing/quote` with `slots: ["13:00", "14:00"]` (where Afternoon `-35%` > Bulk `-15%`).
  3. **UI:** Attempt selecting 2 consecutive slots in `SlotCalendar` (`GAP-UI-02` tracks single-slot UI limitation).
- **Expected Assertions:**
  - **UI:** Checkout summary shows 2 slots and 15% bulk discount line item (once multi-slot selection is enabled).
  - **REST API:** For `19:00, 20:00` (`subtotalPaise = 300000`), `disc_bulk` applies `45000` paise (15% off) → `finalPayablePaise = 255000`. For `13:00, 14:00`, engine selects `bestAutoDiscountPaise` (35% Afternoon > 15% Bulk).
  - **Neon SQL:**
    ```sql
    SELECT discount_id, type, min_slots, discount_value FROM apex_discounts WHERE type = 'BULK_SLOTS_AUTO';
    ```

### `TC-PRC-007` — Promotional Coupon Codes (`THANE50`, `TURF20`, `APEX20`, `ODDHOUR35`) & Stacking Logic
- **Persona / User Type:** Player (`ROLE_PLAYER` / Corporate Lead)
- **Linked Requirement:** `BA-03`, `AC-03.2`
- **Preconditions:** Discounts seeded; test venues in both `Thane` (`venue_thane_01`) and `Mumbai` (`venue_mumbai_01`).
- **Steps:**
  1. **UI + REST API (`THANE50`):** Apply `THANE50` on a `₹1,200` Thane slot (`>= ₹500` min order) and test on a Mumbai slot (verify city rule if configured).
  2. **REST API (`TURF20` vs `BULK_SLOTS_AUTO`):** Quote `["19:00", "20:00"]` (`300000` paise) with `promoCode: "TURF20"` (20% capped at `50000` paise, non-stackable).
  3. **UI + REST API (`APEX20`):** Apply `APEX20` (20% off up to `₹200` / `20000` paise, min order `₹600`).
  4. **UI + REST API (`ODDHOUR35`):** Apply `ODDHOUR35` on an off-peak slot (`14:00`) AND on a peak slot (`19:00`, expecting rejection `"Coupon valid exclusively on Odd-Hour discounted slots"`).
- **Expected Assertions:**
  - **UI:** `CheckoutModal` displays green applied badge for valid promos and red `#promo-feedback` warning for invalid/ineligible codes.
  - **REST API:** `TURF20` (`50000` paise) beats `BULK_SLOTS_AUTO` (`45000` paise) → `finalPayablePaise = 250000`. Stackable promo adds `promoDiscountPaise + bestAutoDiscountPaise`; non-stackable picks `max(...)`. *(Note: Seed `APEX20` & `ODDHOUR35` into `apex_discounts` per Gap `GAP-PRC-02`.)*
  - **Neon SQL:**
    ```sql
    SELECT discount_id, code, value_type, discount_value, max_discount_paise, min_order_paise, stackable_with_auto_discount FROM apex_discounts WHERE type = 'PROMO_CODE';
    ```

### `TC-PRC-008` — Razorpay Minimum Order Guard (`100` Paise / `₹1.00` Floor)
- **Persona / User Type:** Player (`ROLE_PLAYER`)
- **Linked Requirement:** `BA-03`, `AC-03.3`
- **Preconditions:** Low-rate resource (`basePricePaise = 5000` / ₹50, or promo discount `>= subtotalPaise`).
- **Steps:**
  1. **REST API (Pricing Quote):** Call `POST /api/v1/pricing/quote` on a `5000` paise slot with flat `THANE50` (`5000` paise off) or `100%` discount.
  2. **REST API (Initiate Booking):** Call `POST /api/v1/bookings/initiate` with same payload.
  3. **UI / Legacy Check:** Check `calculateSlotPrice` (`pricingEngine.js`) and `PricingService.java` behavior when `promoDiscountAmount >= subtotal`.
- **Expected Assertions:**
  - **UI:** Total payable never drops below `₹1.00` (`100 paise`).
  - **REST API:** `PricingAndDiscountService` returns `finalPayablePaise = 100` (`>= 100L`), and Razorpay order creation requests `amount = 100` paise. *(Note: `PricingService.java` & `pricingEngine.js` use `0` floor — see Gap `GAP-PRC-03`.)*
  - **Neon SQL:**
    ```sql
    SELECT booking_id, subtotal_paise, discount_amount_paise, final_payable_paise FROM apex_bookings WHERE booking_id = '<bookingId>';
    -- Expected: final_payable_paise >= 100
    ```

### `TC-PRC-009` — Owner Dynamic Discount Rule CRUD & Active Toggle
- **Persona / User Type:** Facility Owner (`ROLE_OWNER`)
- **Linked Requirement:** `BA-03`, Master API Matrix
- **Preconditions:** Authenticated as Owner A (`ROLE_OWNER`).
- **Steps:**
  1. **REST API (Create):** `POST /api/v1/owner/discounts` with `{ "name": "Monsoon Madness 30%", "type": "PROMO_CODE", "code": "MONSOON30", "valueType": "PERCENTAGE", "value": 30.0, "maxDiscountPaise": 40000, "minOrderPaise": 80000, "stackableWithAutoDiscount": true }`.
  2. **REST API (List & Update):** `GET /api/v1/owner/discounts` → `PUT /api/v1/owner/discounts/{discountId}` updating `value` to `25.0`.
  3. **REST API (Toggle Off & Quote):** `PATCH /api/v1/owner/discounts/{discountId}/toggle` (`isActive = false`) → Call `POST /api/v1/pricing/quote` with `promoCode: "MONSOON30"`.
- **Expected Assertions:**
  - **UI:** N/A (Owner Discount UI missing — Gap `GAP-UI-03`).
  - **REST API:** Create/List/Update/Toggle return HTTP `200`; deactivated `MONSOON30` yields `discountAmountPaise = 0` in quote; Owner B calling `PUT/PATCH` on Owner A's discount returns `403`.
  - **Neon SQL:**
    ```sql
    SELECT discount_id, code, discount_value, stackable_with_auto_discount, is_active FROM apex_discounts WHERE code = 'MONSOON30';
    ```

### `TC-PRC-010` — Slot Lock Acquisition, Same-User Renewal & Explicit Release
- **Persona / User Type:** Player (`ROLE_PLAYER`)
- **Linked Requirement:** `BA-04`
- **Preconditions:** Slot `16:00` on `res_thane_crk_01` (`date = tomorrow`) is `AVAILABLE`.
- **Steps:**
  1. **UI:** Click `16:00` in `SlotCalendar` → Click `🔒 Lock Slot & Proceed` (`#btn-lock-proceed`) → Observe countdown timer in `CheckoutModal` → Click `Release Slot & Cancel` (`#btn-cancel-checkout`).
  2. **REST API (Initiate Lock):** `POST /api/v1/bookings/initiate` by Player A → Verify `holdExpiresAt ~ now + 600,000ms` (10 min).
  3. **REST API (Same-User Re-Entry):** Player A retries `POST /api/v1/bookings/initiate` (or `POST /api/v1/slots/lock`) for same slot while lease is active.
  4. **REST API (Release):** Call `DELETE /api/v1/slots/lock?courtId=...&date=...&slot=16:00` with Player A JWT.
- **Expected Assertions:**
  - **UI:** Checkout modal shows `10:00` (BRD target; currently `05:00` per Gap `GAP-UI-01`) countdown; cancelling immediately releases lock and slot returns to `AVAILABLE` (not `In Checkout`).
  - **REST API:** Initiate succeeds (`200`), same-user retry succeeds without self-conflict, release returns `200`, and `GET /api/v1/availability` shows `status = "AVAILABLE"`.
  - **Neon SQL:**
    ```sql
    SELECT lock_id, status, user_id, hold_expires_at FROM apex_slot_locks WHERE resource_id = 'res_thane_crk_01' AND booking_date = '<tomorrow>' AND start_time = '16:00';
    -- Held during step 2 (`status = 'HELD'`); 0 rows after release (`DELETE`)
    ```

### `TC-PRC-011` — Concurrent Double-Booking Conflict (`409 Conflict`), Expired Hold Reclamation & Multi-Slot Rollback
- **Persona / User Type:** Player A & Player B (`ROLE_PLAYER`)
- **Linked Requirement:** `BA-04`, `AC-04.1`, `AC-04.2`
- **Preconditions:** Slots `19:00` and `20:00` on `res_thane_crk_01` (`tomorrow + 1`) are `AVAILABLE`; `18:00` is `AVAILABLE`.
- **Steps:**
  1. **REST API (Concurrent Race):** Fire two parallel `POST /api/v1/bookings/initiate` requests (`Player A` and `Player B`) within `< 20ms` for `["19:00", "20:00"]`.
  2. **REST API (Multi-Slot Atomic Rollback):** While `19:00` is `HELD` by Player A, Player B sends `POST /api/v1/bookings/initiate` for `["18:00", "19:00"]`. Verify `18:00` is NOT left orphaned as `HELD`.
  3. **REST API (Expired Lease Reclamation):** Set `hold_expires_at = now - 10000` on Player A's lock (or wait `> 10 min`) and have Player B call `POST /api/v1/bookings/initiate` for `["19:00"]`.
- **Expected Assertions:**
  - **UI:** Losing player sees conflict alert `"Slot is currently held by another user"`.
  - **REST API:** In Step 1, exactly ONE player receives HTTP `200` (`bookingId` + `razorpayOrderId`) and the other receives HTTP **`409 Conflict`**. In Step 2, Player B gets `409` and `18:00` remains `AVAILABLE`. In Step 3, expired hold is reclaimed atomically for Player B (`200`).
  - **Neon SQL:**
    ```sql
    SELECT lock_id, user_id, status, hold_expires_at FROM apex_slot_locks WHERE resource_id = 'res_thane_crk_01' AND booking_date = '<tomorrow+1>';
    -- Expected: 19:00 & 20:00 belong strictly to winning user; 0 lock rows for 18:00; uq_apex_slot_lock prevents duplicate rows
    ```

---

## 6. Suite 4: Booking Full Lifecycle, Payments, Gate QR & Webhooks (`TC-BKG-001` – `TC-BKG-014`)

### `TC-BKG-001` — Online Razorpay Booking Initiate & Synchronous HMAC Verify-Payment Commit
- **Persona / User Type:** Player (`ROLE_PLAYER`)
- **Linked Requirement:** `BA-04`, `BA-05`, `AC-05.1`
- **Preconditions:** Player A authenticated; slot `18:00` on `res_thane_fb_01` (`tomorrow + 2`) is `AVAILABLE`.
- **Steps:**
  1. **UI:** Select `18:00` in `SlotCalendar` → Proceed to `CheckoutModal` → Select `UPI / QR` → Click `Pay ₹...` → Complete Razorpay test checkout modal.
  2. **REST API Step 1:** `POST /api/v1/bookings/initiate` `{ "venueId": "venue_thane_01", "resourceId": "res_thane_fb_01", "bookingDate": "<tomorrow+2>", "slotStartTimes": ["18:00"], "sportType": "FOOTBALL_TURF", "promoCode": "TURF20" }`.
  3. **REST API Step 2:** Compute HMAC-SHA256 `HmacSHA256(razorpayOrderId + "|" + "pay_qa_001", RAZORPAY_KEY_SECRET)` → Call `POST /api/v1/bookings/{bookingId}/verify-payment` with `{ razorpayOrderId, razorpayPaymentId: "pay_qa_001", razorpaySignature }`.
  4. **Neon DB:** Inspect `apex_bookings`, `apex_slot_locks`, `apex_payments`, and `apex_discounts`.
- **Expected Assertions:**
  - **UI:** Navigates to `BookingConfirmation` displaying `ARENA-XXXXX` / `APEX-PASS-*` QR card, `Total Paid`, payment reference, and saves pass to `OfflineStorage`.
  - **REST API:** `initiate` returns `200` with `bookingId`, `razorpayOrderId`, `amountPaise`, `holdExpiresAt`; `verify-payment` returns `200` with `status = "CONFIRMED"`, `bookingCode` matching `^ARENA-[A-Z0-9]{5}$`, `qrPassToken` starting with `APEX-PASS-ARENA-`, `holdExpiresAt = null`.
  - **Neon SQL:**
    ```sql
    SELECT booking_id, booking_code, qr_pass_token, status, final_payable_paise, razorpay_order_id, razorpay_payment_id, hold_expires_at
    FROM apex_bookings WHERE booking_id = '<bookingId>';
    -- Expected: status='CONFIRMED', hold_expires_at IS NULL
    SELECT status, hold_expires_at FROM apex_slot_locks WHERE booking_id = '<bookingId>';
    -- Expected: status='BOOKED', hold_expires_at IS NULL
    SELECT payment_id, amount_paise, status, tx_type FROM apex_payments WHERE booking_id = '<bookingId>';
    -- Expected: 1 row, amount_paise = final_payable_paise, status='CAPTURED', tx_type='PAYMENT'
    ```

### `TC-BKG-002` — Invalid / Tampered Razorpay HMAC Signature & Order-Mismatch Rejection
- **Persona / User Type:** Player (`ROLE_PLAYER`)
- **Linked Requirement:** `BA-05`
- **Preconditions:** `PENDING_PAYMENT` booking `bk_tamper` exists with `razorpayOrderId = "order_A"` and active `HELD` lock.
- **Steps:**
  1. **REST API (Bad Signature):** Call `POST /api/v1/bookings/bk_tamper/verify-payment` with valid `order_A`, `pay_qa_002`, and forged `razorpaySignature = "0000deadbeef"`.
  2. **REST API (Bypass String Blocked in Prod):** Test `razorpaySignature = "test_sig_bypass"` and `"SIMULATED_TEST_SIGNATURE"` under production profile (`GAP-SEC-05`).
  3. **REST API (Cross-Order Replay):** Compute a valid signature for a different `order_B` (`₹1` booking) and submit `razorpayOrderId: "order_B"` to `/api/v1/bookings/bk_tamper/verify-payment`.
- **Expected Assertions:**
  - **UI:** Error alert `"Payment verification failed"` shown; booking confirmation does NOT render.
  - **REST API:** All three tampered requests return HTTP `400`/`403` (`"Invalid Razorpay payment signature"` or `"Order ID mismatch"`); booking remains `PENDING_PAYMENT` (or `HELD`), never `CONFIRMED`.
  - **Neon SQL:**
    ```sql
    SELECT status, razorpay_payment_id FROM apex_bookings WHERE booking_id = 'bk_tamper';
    -- Expected: status = 'PENDING_PAYMENT', razorpay_payment_id IS NULL
    SELECT COUNT(*) FROM apex_payments WHERE booking_id = 'bk_tamper'; -- Expected: 0
    ```

### `TC-BKG-003` — 10-Minute Checkout Hold Expiry Sweeper (`BookingHoldExpiryScheduler`) & Late Sync Guard
- **Persona / User Type:** Player / Webhook/Admin
- **Linked Requirement:** `BA-04`, `AC-04.2`
- **Preconditions:** Player A initiates booking `bk_exp_qa` (`PENDING_PAYMENT`, `hold_expires_at = now - 61000`, `slot_locks.status = 'HELD'`).
- **Steps:**
  1. **Scheduler Execution:** Wait `<= 60s` for `BookingHoldExpiryScheduler.cleanupExpiredHolds()` (`@Scheduled(fixedDelay = 60000)`) to execute.
  2. **REST API (Slot Availability):** Call `GET /api/v1/availability` for that resource and date.
  3. **REST API (Late Sync Verify Guard):** Attempt `POST /api/v1/bookings/bk_exp_qa/verify-payment` after booking transitioned to `EXPIRED`.
- **Expected Assertions:**
  - **UI:** Checkout modal timer hits `00:00`, alerts hold expiration, closes modal, and slot returns to `AVAILABLE`.
  - **REST API:** Scheduler marks `bk_exp_qa` `EXPIRED` and deletes its `HELD` lock; `GET /availability` shows `AVAILABLE`; late synchronous `verify-payment` on `EXPIRED` booking is rejected (`409`/`400`) rather than confirming expired hold (fix per Gap `GAP-BKG-02`).
  - **Neon SQL:**
    ```sql
    SELECT status FROM apex_bookings WHERE booking_id = 'bk_exp_qa';          -- Expected: 'EXPIRED'
    SELECT COUNT(*) FROM apex_slot_locks WHERE booking_id = 'bk_exp_qa';     -- Expected: 0
    ```

### `TC-BKG-004` — Player View Own Bookings & Passes — Strict Tenant Isolation
- **Persona / User Type:** Player A & Player B (`ROLE_PLAYER`)
- **Linked Requirement:** `BA-01`, `AC-01.3`
- **Preconditions:** Player A has booking `bk_playerA`; Player B has booking `bk_playerB`.
- **Steps:**
  1. **UI:** Sign in as Player A → Open `🎟️ My Passes` (`PlayerPortal`) → Verify only Player A's bookings appear; click `🎟️ View Pass` to open QR Pass.
  2. **REST API (Own List):** Call `GET /api/v1/bookings/my` and `GET /api/v1/bookings/my-passes` with Player A's JWT.
  3. **REST API (Cross-User Read):** Call `GET /api/v1/bookings/bk_playerB` using Player A's JWT.
- **Expected Assertions:**
  - **UI:** Zero bookings belonging to Player B or `demo_player` appear in Player A's portal.
  - **REST API:** `/bookings/my` and `/bookings/my-passes` return identically filtered lists where every item has `userId == playerA_uid`. Cross-user `GET /bookings/bk_playerB` MUST return HTTP `403 Forbidden` (fix per Gap `GAP-SEC-01`).
  - **Neon SQL:**
    ```sql
    SELECT booking_id, user_id FROM apex_bookings WHERE user_id = '<playerA_uid>';
    ```

### `TC-BKG-005` — Player Update / Reschedule Confirmed Booking (Atomic Date/Slot Swap)
- **Persona / User Type:** Player (`ROLE_PLAYER`)
- **Linked Requirement:** Stakeholder CRUD Matrix (Player Update/Reschedule Booking)
- **Preconditions:** Player A owns `CONFIRMED` booking `bk_resched` at `10:00` on `D1` (`> 6h` before match); target slot `11:00` on `D2` is `AVAILABLE`; competing slot `19:00` on `D2` is `BOOKED`.
- **Steps:**
  1. **UI:** In `PlayerPortal`, click `Reschedule` on `bk_resched` → Select `D2 @ 11:00` → Confirm swap.
  2. **REST API (Happy Path):** Call `PUT /api/v1/bookings/{bookingId}/reschedule` (or `PATCH /api/v1/bookings/{bookingId}`) with Player A JWT and `{ "newBookingDate": "<D2>", "newSlotStartTimes": ["11:00"] }`.
  3. **REST API (Conflict & Auth Negative):** Attempt reschedule to already-booked `19:00` (`409 Conflict`), and attempt reschedule of `bk_resched` using Player B's JWT (`403 Forbidden`).
- **Expected Assertions:**
  - **UI:** Pass updates to `D2 @ 11:00`; old slot `D1 @ 10:00` becomes `AVAILABLE`.
  - **REST API:** Atomic transaction acquires lock for `D2 @ 11:00` (`BOOKED`), deletes lock for `D1 @ 10:00`, updates `slots` & `bookingDate`, computes any price delta, and returns HTTP `200`. *(Note: Reschedule endpoint & UI are currently MISSING — see Gap `GAP-CRUD-02`.)*
  - **Neon SQL:**
    ```sql
    SELECT booking_date, slots_json, status FROM apex_bookings WHERE booking_id = 'bk_resched';
    SELECT lock_id, booking_date, start_time, status FROM apex_slot_locks WHERE booking_id = 'bk_resched';
    -- Expected: only D2 @ 11:00 locked with status='BOOKED'; D1 @ 10:00 lock deleted
    ```

### `TC-BKG-006` — Player Tier 1 Early Cancellation (`> 24h` Notice: `100% - ₹50` Fee & Lock Release)
- **Persona / User Type:** Player (`ROLE_PLAYER`)
- **Linked Requirement:** `BA-06`, `AC-06.1`
- **Preconditions:** `CONFIRMED` booking `bk_tier1` for `₹1,200` (`120000` paise) starting `30 hours` from current IST time.
- **Steps:**
  1. **UI:** Open `PlayerPortal` → Verify cancel button label reads `Cancel (Refund: ₹1150)` → Click button and confirm dialog.
  2. **REST API:** Call `POST /api/v1/bookings/bk_tier1/cancel` with Player A JWT and `{ "reason": "Team unavailable" }`.
  3. **REST API (Slot Availability):** Call `GET /api/v1/availability` for the cancelled slot.
- **Expected Assertions:**
  - **UI:** Booking status changes to `CANCELLED_BY_USER` / `CANCELLED`; alert confirms `₹1,150` refunded; slot becomes `AVAILABLE` again in `SlotCalendar`.
  - **REST API:** HTTP `200`; `status = "CANCELLED_BY_USER"`, `refundAmountPaise = 115000` (`120000 - 5000` handling fee!), `refundStatus = "INITIATED"`, non-null `refundId`. *(Note: Backend `BookingService.cancelBookingByPlayer` currently misses the `5000L` fee deduction — see Gap `GAP-BKG-01`.)*
  - **Neon SQL:**
    ```sql
    SELECT status, refund_id, refund_amount_paise, refund_status, cancellation_reason FROM apex_bookings WHERE booking_id = 'bk_tier1';
    -- Expected: status='CANCELLED_BY_USER', refund_amount_paise=115000
    SELECT COUNT(*) FROM apex_slot_locks WHERE booking_id = 'bk_tier1'; -- Expected: 0
    SELECT payment_id, amount_paise, tx_type, status FROM apex_payments WHERE booking_id = 'bk_tier1'; -- Expected: CAPTURE + REFUND rows
    ```

### `TC-BKG-007` — Player Tier 2 (`6–24h`: `50%`), Tier 3 (`< 6h`: `0%`) Cancellation & Double/Cross-User Guards
- **Persona / User Type:** Player A & Player B (`ROLE_PLAYER`)
- **Linked Requirement:** `BA-06`, `AC-06.2`, `AC-06.3`
- **Preconditions:** `CONFIRMED` booking `bk_tier2` (`120000` paise, `10h` from IST now); `CONFIRMED` booking `bk_tier3` (`120000` paise, `3h` from IST now).
- **Steps:**
  1. **UI + REST API (Tier 2):** Cancel `bk_tier2` via `POST /api/v1/bookings/bk_tier2/cancel` as Player A.
  2. **UI + REST API (Tier 3):** Cancel `bk_tier3` via `POST /api/v1/bookings/bk_tier3/cancel` as Player A.
  3. **REST API (Negative Guards):** Retry cancelling `bk_tier2` a second time; have Player B attempt to cancel Player A's booking.
- **Expected Assertions:**
  - **UI:** Tier 2 shows `50%` (`₹600` refund); Tier 3 shows `0%` (`₹0` refund); both release slots back to `AVAILABLE`.
  - **REST API:** Tier 2: `refundAmountPaise = 60000`, `refundStatus = "INITIATED"`. Tier 3: `refundAmountPaise = 0` / `null`, `refundId = null` (no Razorpay refund API call). Double-cancel returns `400`; cross-user cancel returns `403 Forbidden`.
  - **Neon SQL:**
    ```sql
    SELECT booking_id, status, refund_amount_paise, refund_id FROM apex_bookings WHERE booking_id IN ('bk_tier2', 'bk_tier3');
    SELECT COUNT(*) FROM apex_slot_locks WHERE booking_id IN ('bk_tier2', 'bk_tier3'); -- Expected: 0
    ```

### `TC-BKG-008` — Player Delete / Remove Cancelled or Expired Booking
- **Persona / User Type:** Player (`ROLE_PLAYER`)
- **Linked Requirement:** Stakeholder CRUD Matrix (Player Delete/Remove Booking)
- **Preconditions:** Player A has an `EXPIRED` or `CANCELLED_BY_USER` booking `bk_old_qa` and an active `CONFIRMED` future booking `bk_active_qa`.
- **Steps:**
  1. **UI:** In `PlayerPortal`, click `Remove / Delete` on `bk_old_qa`.
  2. **REST API (Cancelled/Expired):** Call `DELETE /api/v1/bookings/bk_old_qa` with Player A JWT.
  3. **REST API (Active Guard & Auth Guard):** Attempt `DELETE /api/v1/bookings/bk_active_qa` (active confirmed future booking) and `DELETE /api/v1/bookings/{playerB_booking}` (cross-user).
- **Expected Assertions:**
  - **UI:** `bk_old_qa` is removed from the visible `My Game Bookings` list; active booking cannot be deleted without cancelling first.
  - **REST API:** Deleting cancelled/expired booking returns `200`/`204` (soft-archived or deleted while preserving financial `apex_payments` audit rows); deleting active confirmed booking returns `409 Conflict`; cross-user delete returns `403 Forbidden`. *(Note: Endpoint & UI action currently MISSING — see Gap `GAP-CRUD-03`.)*
  - **Neon SQL:**
    ```sql
    SELECT status FROM apex_bookings WHERE booking_id = 'bk_old_qa'; -- Soft-deleted/archived or removed
    SELECT COUNT(*) FROM apex_payments WHERE booking_id = 'bk_old_qa'; -- Financial ledger preserved
    ```

### `TC-BKG-009` — Facility Owner Emergency / Rainout Cancellation (`100%` Full Refund, `₹0` Fee)
- **Persona / User Type:** Facility Owner (`ROLE_OWNER`)
- **Linked Requirement:** `BA-06`, `AC-06.4`
- **Preconditions:** `CONFIRMED` booking `bk_rain_qa` (`120000` paise) at Owner A's venue starting `1 hour` from now (`< 6h` window).
- **Steps:**
  1. **REST API:** Call `POST /api/v1/owner/bookings/bk_rain_qa/cancel` with Owner A JWT and `{ "reason": "Heavy waterlogging on pitch" }`.
  2. **REST API (Cross-Owner Guard):** Have Owner B attempt emergency cancel on Owner A's booking.
  3. **UI Check:** Inspect `PlayerPortal` and `VenueOwnerPortal` ledgers.
- **Expected Assertions:**
  - **UI:** Both portals display `CANCELLED_BY_OWNER`; slot returns to `AVAILABLE` (or owner blocks slot for maintenance).
  - **REST API:** Owner A call returns `200` with `status = "CANCELLED_BY_OWNER"`, `refundAmountPaise = 120000` (`100%` full refund, zero ₹50 fee), `refundStatus = "INITIATED"`; Owner B returns `403`.
  - **Neon SQL:**
    ```sql
    SELECT status, refund_amount_paise, refund_status, cancellation_reason FROM apex_bookings WHERE booking_id = 'bk_rain_qa';
    SELECT COUNT(*) FROM apex_slot_locks WHERE booking_id = 'bk_rain_qa'; -- Expected: 0
    ```

### `TC-BKG-010` — Facility Owner Update Booking Status (`COMPLETED` / `NO_SHOW`)
- **Persona / User Type:** Facility Owner (`ROLE_OWNER`)
- **Linked Requirement:** Master API Matrix (`OwnerBookingController`)
- **Preconditions:** `CONFIRMED` or `CHECKED_IN` booking `bk_status_qa` at Owner A's venue.
- **Steps:**
  1. **REST API (List):** `GET /api/v1/owner/bookings?date={bookingDate}` with Owner A JWT.
  2. **REST API (Update):** `PATCH /api/v1/owner/bookings/bk_status_qa/status` with `{ "status": "COMPLETED" }` (and test `"NO_SHOW"` on a missed slot).
  3. **REST API (Analytics):** Call `GET /api/v1/owner/analytics/summary` with Owner A JWT.
  4. **REST API (Cross-Owner Negative):** Call `PATCH /api/v1/owner/bookings/bk_status_qa/status` using Owner B JWT.
- **Expected Assertions:**
  - **UI:** Booking status badge updates in `VenueOwnerPortal` ledger.
  - **REST API:** Status update returns `200` with `status = "COMPLETED"`; analytics summary increments `confirmedBookings` and `grossRevenuePaise`; Owner B attempt returns `403`.
  - **Neon SQL:**
    ```sql
    SELECT booking_id, status, updated_at FROM apex_bookings WHERE booking_id = 'bk_status_qa';
    ```

### `TC-BKG-011` — Facility Owner Counter Walk-In Cash Booking & Double-Booking Guard
- **Persona / User Type:** Facility Owner (`ROLE_OWNER`)
- **Linked Requirement:** `BA-07`, `AC-07.4`
- **Preconditions:** Slot `17:00` on `court_thane_01_crk` / `res_thane_crk_01` (`tomorrow`) is `AVAILABLE`; Owner A authenticated.
- **Steps:**
  1. **UI:** In `VenueOwnerPortal`, click `⚡ + Walk-In Reservation` (`#btn-owner-walkin`) → Select court, date, slot `17:00`, Customer `Aditya Kadam`, Phone `+91 98111 22334`, Cash `1000` → Submit (`#form-walkin`).
  2. **REST API:** `POST /api/v1/gate/walk-in` with `{ "courtId": "court_thane_01_crk", "date": "<tomorrow>", "slot": "17:00", "customerName": "Aditya Kadam", "customerPhone": "+91 98111 22334", "amountCollected": 1000 }`.
  3. **REST API (Conflict Guard):** Attempt a second `POST /api/v1/gate/walk-in` AND an online `POST /api/v1/bookings/initiate` on the same `17:00` slot.
  4. **REST API (RBAC Check):** Attempt `POST /api/v1/gate/walk-in` using a `ROLE_PLAYER` JWT.
- **Expected Assertions:**
  - **UI:** Walk-in entry appears in Facility Ledger tagged `WALK-IN (CASH)` and `CONFIRMED`; public `SlotCalendar` renders `17:00` as `Sold Out` (`BOOKED`).
  - **REST API:** First walk-in returns `201 Created` with `bookingCode` starting `ARENA-WALKIN-` and `finalPayablePaise = 100000`; duplicate walk-in and competing online hold return `409 Conflict` / `IllegalStateException`; `ROLE_PLAYER` call returns `403 Forbidden` (fix per Gap `GAP-SEC-02`).
  - **Neon SQL:**
    ```sql
    SELECT booking_id, booking_code, owner_id, final_payable_paise, status FROM apex_bookings WHERE booking_code LIKE 'ARENA-WALKIN-%' AND booking_date = '<tomorrow>';
    SELECT lock_id, status FROM apex_slot_locks WHERE resource_id = 'court_thane_01_crk' AND booking_date = '<tomorrow>' AND start_time = '17:00';
    -- Expected: status = 'BOOKED'
    ```

### `TC-BKG-012` — Gate QR Check-In Verification Matrix (Valid, Duplicate Block, Cancelled Block, Invalid Block)
- **Persona / User Type:** Facility Owner (`ROLE_OWNER`)
- **Linked Requirement:** `BA-07`, `AC-07.1`, `AC-07.2`, `AC-07.3`
- **Preconditions:** Owner A has a `CONFIRMED` booking (`ARENA-XXXXX` / `APEX-PASS-*`) and a `CANCELLED_BY_USER` booking from `TC-BKG-006`.
- **Steps:**
  1. **UI + REST API (Valid First Scan):** Enter token in `#input-gate-token` and click `Verify QR Token` (`#btn-verify-token`) → `POST /api/v1/gate/verify-pass` `{ "qrPassToken": "<validToken>" }`.
  2. **UI + REST API (Duplicate Scan):** Scan the exact same token a second time immediately.
  3. **UI + REST API (Cancelled Booking Pass):** Scan the cancelled booking's `qrPassToken`.
  4. **UI + REST API (Invalid / Unknown Token):** Scan `"APEX-INVALID-99999"`.
  5. **REST API (Cross-Owner / Player Guard):** Have Owner B and Player A attempt `POST /api/v1/gate/verify-pass` for Owner A's pass.
- **Expected Assertions:**
  - **UI:** Scan #1 shows green `"✓ Gate Pass Verified & Attendance Marked"` & ledger badge changes to `CHECKED IN`; Scan #2 shows amber `"⚠️ Player Already Checked In"`; Scan #3 & #4 show red `"❌ Verification Failed"`.
  - **REST API:** Scan #1 returns `200` (`valid: true, alreadyCheckedIn: false, booking.status: "CHECKED_IN"`); Scan #2 returns `valid: false, alreadyCheckedIn: true, message: "Already checked in"`; Scan #3 is strictly rejected (`409` / `valid: false`); Scan #4 returns `valid: false, "Invalid pass token"`; Owner B & Player A return `403` (fix per Gap `GAP-SEC-02`).
  - **Neon SQL:**
    ```sql
    SELECT booking_id, status, checked_in_at, updated_at FROM apex_bookings WHERE booking_id = '<valid_booking_id>';
    -- Expected: status='CHECKED_IN', checked_in_at IS NOT NULL (add column per Gap GAP-DB-01)
    ```

### `TC-BKG-013` — Razorpay Webhook Signature Verification, Raw Payload HMAC & Idempotency
- **Persona / User Type:** Webhook/Admin (Razorpay Webhook Server)
- **Linked Requirement:** `BA-05`, `AC-05.2`
- **Preconditions:** Pending booking `bk_wh_qa` (`hold_expires_at > now`, `HELD` lock) and a separate pending booking `bk_wh_fail` exist.
- **Steps:**
  1. **REST API (Invalid/Missing Signature):** Send `POST /api/v1/webhooks/razorpay` with missing `X-Razorpay-Signature`, then with tampered signature.
  2. **REST API (`payment.captured` Valid Raw Payload):** Construct exact JSON string (including whitespace) for `payment.captured` with `notes.bookingId = "bk_wh_qa"`, sign exact raw bytes with `RAZORPAY_WEBHOOK_SECRET`, send with `X-Razorpay-Event-Id: "evt_qa_cap_01"`.
  3. **REST API (Duplicate Replay Idempotency):** Replay the exact same webhook request (`evt_qa_cap_01`) twice more.
  4. **REST API (`payment.failed`):** Send signed `payment.failed` webhook (`evt_qa_fail_01`) for `bk_wh_fail`.
- **Expected Assertions:**
  - **UI:** `bk_wh_qa` transitions to `CONFIRMED` in `PlayerPortal` even if user closed checkout modal; `bk_wh_fail` slot immediately returns to `AVAILABLE`.
  - **REST API:** Missing/invalid signature returns `400`/`401` (`"Invalid Razorpay Webhook Signature"`); `evt_qa_cap_01` #1 returns `200` confirming booking and transitioning locks `HELD -> BOOKED`; `evt_qa_cap_01` #2 & #3 return `200` idempotently without creating duplicate payment rows; `payment.failed` sets status `PAYMENT_FAILED` and releases `slot_locks`.
  - **Neon SQL:**
    ```sql
    SELECT event_id, event_type, processed_status FROM apex_webhook_events WHERE event_id IN ('evt_qa_cap_01', 'evt_qa_fail_01');
    SELECT COUNT(*) FROM apex_payments WHERE booking_id = 'bk_wh_qa'; -- Must equal 1 despite 3 webhook deliveries
    SELECT status FROM apex_bookings WHERE booking_id = 'bk_wh_fail'; -- 'PAYMENT_FAILED'
    SELECT COUNT(*) FROM apex_slot_locks WHERE booking_id = 'bk_wh_fail'; -- Expected: 0
    ```

### `TC-BKG-014` — Razorpay Webhook Late Payment Conflict After Hold Expiry (Auto-Refund)
- **Persona / User Type:** Webhook/Admin (Razorpay) + Competing Players A & B
- **Linked Requirement:** `BA-05`, `AC-05.3`
- **Preconditions:** Player A initiated `bk_late_A` (`100000` paise) for slot `20:00`, hold expired (`hold_expires_at < now`), and Player B subsequently initiated & confirmed `bk_win_B` (`status = 'CONFIRMED'`, `slot_locks.status = 'BOOKED'`).
- **Steps:**
  1. **REST API:** Send valid HMAC-signed `POST /api/v1/webhooks/razorpay` (`event = "payment.captured"`, `X-Razorpay-Event-Id: "evt_qa_late_01"`, `notes.bookingId = "bk_late_A"`, `payment.id = "pay_late_A"`).
  2. **Inspect Competing Booking & Refund:** Call `GET /api/v1/bookings/bk_win_B` (as Player B) and check `bk_late_A`.
- **Expected Assertions:**
  - **UI:** Player B retains `CONFIRMED` pass; slot remains `BOOKED` for Player B; Player A sees `CONFLICT_REFUND_PENDING` / auto-refund initiated.
  - **REST API:** Webhook returns `200 OK`; `bk_win_B` stays `CONFIRMED`; `bk_late_A` transitions to `CONFLICT_REFUND_PENDING`, `refundId` starts with `rfnd_`, `refundStatus = "INITIATED"`, `refundAmountPaise = 100000` (set per Gap `GAP-BKG-03`).
  - **Neon SQL:**
    ```sql
    SELECT booking_id, status, refund_id, refund_amount_paise, refund_status FROM apex_bookings WHERE booking_id IN ('bk_late_A', 'bk_win_B');
    SELECT booking_id, status FROM apex_slot_locks WHERE resource_id = '<res>' AND booking_date = '<date>' AND start_time = '20:00';
    -- Expected: slot_locks.booking_id = 'bk_win_B', status = 'BOOKED'
    ```

---

## 7. Suite 5: Cross-Tier Multi-Cloud Architecture, CORS, DB & Mobile Parity (`TC-ARC-001` – `TC-ARC-008`)

### `TC-ARC-001` — Firebase Hosting CORS Preflight (`OPTIONS`) & Origin Policy
- **Persona / User Type:** Guest / Webhook/Admin
- **Linked Requirement:** `BA-08`, `AC-08.1`
- **Preconditions:** Tier 2 deployed on Render (`render.yaml`) and Tier 1 deployed on `https://project-apex-pushp.web.app`.
- **Steps:**
  1. **REST API (Allowed Origin Preflight):** Send `OPTIONS /api/v1/bookings/initiate` with `Origin: https://project-apex-pushp.web.app` and `Access-Control-Request-Method: POST`.
  2. **REST API (FirebaseApp Alias):** Send `OPTIONS /api/v1/venues` with `Origin: https://project-apex-pushp.firebaseapp.com`.
  3. **REST API (Disallowed Origin):** Send `OPTIONS /api/v1/venues` with `Origin: https://evil-phish.example.com`.
- **Expected Assertions:**
  - **UI:** Clicking `#btn-header-backend` (`3-Tier API`) in `https://project-apex-pushp.web.app` shows `🟢 Online` with zero browser CORS console errors.
  - **REST API:** Allowed origins return `Access-Control-Allow-Origin` matching origin + allowed methods (`GET,POST,PUT,DELETE,OPTIONS,PATCH`); unauthorized origin is rejected once wildcard `*` in [CorsConfig.java](file:///c:/Users/pushp/googlews/project-apex/backend/src/main/java/com/projectapex/config/CorsConfig.java) and `@CrossOrigin(origins = "*")` are tightened to `cors.allowed-origins` (`GAP-SEC-06`).
  - **Neon SQL:** N/A (HTTP edge check).

### `TC-ARC-002` — Zero Secrets & Zero DB Credentials in Tier 1 Static Assets
- **Persona / User Type:** Webhook/Admin (Security Auditor)
- **Linked Requirement:** `BA-08`, `AC-08.2`
- **Preconditions:** Build/deploy workspace (`index.html`, `src/**/*.js`, `sw.js`, `firebase.json`).
- **Steps:**
  1. **Static Scan:** Search all files served by Firebase Hosting (`public: "."` excluding `backend/`, `docs/`, `tests/`, `scripts/` per [firebase.json](file:///c:/Users/pushp/googlews/project-apex/firebase.json)) for `postgresql://`, `postgres://`, `neon.tech`, `jdbc:`, `PGPASSWORD`, `rzp_live`, `keySecret`, `s3cr3t_t3st_k3y_4p3x`, `whsec_`, `ApexSecretKeyForJwtTokenSigning`.
  2. **Live Bundle Fetch:** Inspect HTTP responses from `https://project-apex-pushp.web.app/src/services/apiClient.js` and `firebaseConfig.js`.
- **Expected Assertions:**
  - **UI:** Only public `RENDER_API_BASE_URL`, `LOCAL_API_BASE_URL`, and public Firebase Web config exist; Razorpay `key_id` is received dynamically from Tier 2 during checkout.
  - **REST API:** Zero secret strings found in any Tier 1 asset (`0` matches).
  - **Neon SQL:** Zero direct DB connection strings or SQL queries exist in Tier 1.

### `TC-ARC-003` — Render Middleware Health Probe & Neon SSL JDBC Pool Verification
- **Persona / User Type:** Webhook/Admin
- **Linked Requirement:** `BA-08`, `AC-08.3`
- **Preconditions:** Render service configured with `DATABASE_URL` pointing to Neon `-pooler` endpoint (`?sslmode=require`).
- **Steps:**
  1. **UI:** Click `#btn-header-backend` (`3-Tier API`) → Click `Test Ping` (`#btn-test-backend-ping`).
  2. **REST API:** Send `GET /api/v1/health` and measure warm response latency across 5 samples.
  3. **Config Check:** Verify `NeonDataSourceConfig.resolveSpec` handling of `postgresql://`, `postgres://`, `jdbc:postgresql://` (appending `sslmode=require` automatically) and HikariCP `maximumPoolSize=5`.
- **Expected Assertions:**
  - **UI:** Modal displays `TIER 1: Active`, `TIER 2: Online • <ms>`, `TIER 3: Neon PostgreSQL • 🟢 Connected` with live `Users`, `Venues`, `Bookings` counts.
  - **REST API:** `GET /api/v1/health` returns HTTP `200`, `data.status = "UP"`, `data.database = "Neon PostgreSQL"`, `data.dbConnected = true`, warm p95 `< 100ms`.
  - **Neon SQL:**
    ```sql
    SELECT 1;
    SELECT COUNT(*) FROM apex_users;
    SELECT COUNT(*) FROM apex_venues;
    SELECT COUNT(*) FROM apex_bookings;
    ```

### `TC-ARC-004` — Neon Serverless PostgreSQL Restart Durability Across All Core Tables
- **Persona / User Type:** Webhook/Admin
- **Linked Requirement:** `BA-08`, `AC-08.3`
- **Preconditions:** Fresh records created across workflows in `TC-USR-001`, `TC-FAC-003`, `TC-FAC-004`, `TC-PRC-009`, `TC-BKG-001`, `TC-BKG-013`.
- **Steps:**
  1. **Neon DB Snapshot (Pre-Restart):** Capture row counts and sample IDs across `apex_users`, `apex_venues`, `apex_facilities`, `apex_resources`, `apex_bookings`, `apex_slot_locks`, `apex_payments`, `apex_otp_sessions`, `apex_audit_logs`, `apex_schedule_rules`, `apex_date_overrides`, `apex_discounts`, and `apex_webhook_events`.
  2. **Tier 2 Restart:** Restart Spring Boot JVM / Render container cleanly.
  3. **Hydration Verification:** Call `GET /api/v1/health`, `GET /api/v1/venues`, `GET /api/v1/bookings/my`, and `POST /api/v1/webhooks/razorpay` (replay `evt_qa_cap_01`).
- **Expected Assertions:**
  - **UI:** All user profiles, created venues/courts, bookings, and QR passes remain visible after backend restart.
  - **REST API:** `FirebaseService.hydrateFromDatabase()` reloads all 13 tables accurately; replayed webhook deduplicates against hydrated `apex_webhook_events`; slot locks reject double-bookings on previously `BOOKED` slots.
  - **Neon SQL:**
    ```sql
    SELECT 'apex_users' AS tbl, COUNT(*) FROM apex_users
    UNION ALL SELECT 'apex_venues', COUNT(*) FROM apex_venues
    UNION ALL SELECT 'apex_facilities', COUNT(*) FROM apex_facilities
    UNION ALL SELECT 'apex_resources', COUNT(*) FROM apex_resources
    UNION ALL SELECT 'apex_schedule_rules', COUNT(*) FROM apex_schedule_rules
    UNION ALL SELECT 'apex_date_overrides', COUNT(*) FROM apex_date_overrides
    UNION ALL SELECT 'apex_discounts', COUNT(*) FROM apex_discounts
    UNION ALL SELECT 'apex_slot_locks', COUNT(*) FROM apex_slot_locks
    UNION ALL SELECT 'apex_bookings', COUNT(*) FROM apex_bookings
    UNION ALL SELECT 'apex_payments', COUNT(*) FROM apex_payments
    UNION ALL SELECT 'apex_otp_sessions', COUNT(*) FROM apex_otp_sessions
    UNION ALL SELECT 'apex_webhook_events', COUNT(*) FROM apex_webhook_events
    UNION ALL SELECT 'apex_audit_logs', COUNT(*) FROM apex_audit_logs;
    ```

### `TC-ARC-005` — Tier 1 UI ↔ Tier 2 REST API Live Wiring Parity (No Silent `mockDb` Divergence)
- **Persona / User Type:** Player / Facility Owner
- **Linked Requirement:** `BA-08`
- **Preconditions:** Tier 1 served locally (`http://localhost:3000`) or on Firebase pointing to live Tier 2 Java API; DevTools Network tab recording.
- **Steps:**
  1. **UI Network Trace:** Perform Player Dual-OTP signup (`UserAuthModal`), search venues (`VenueExplorer`), select slot & acquire lock (`SlotCalendar`), create Razorpay order & pay (`CheckoutModal`), view passes & cancel (`PlayerPortal`), add venue/court, walk-in, and gate QR scan (`VenueOwnerPortal`).
  2. **Cross-Browser Incognito Check:** Open a fresh incognito browser (empty `localStorage`) and sign in as the newly created player/owner.
- **Expected Assertions:**
  - **UI:** Every operation fires a live HTTP request via `ApiClient` (zero operations silently swallowed into `localStorage` `mockDb`); fresh incognito browser sees the exact venues, courts, bookings, and check-in states created in session 1. *(Note: Major UI wiring gap exists today — see Gap `GAP-UI-01` & `GAP-UI-03`.)*
  - **REST API:** All Network requests match `/api/v1/*` DTO schemas (`200`/`201`) with zero `400` validation mismatches (e.g., `CheckoutModal` -> `/payments/razorpay/create-order` payload fix).
  - **Neon SQL:** Every UI mutation produces corresponding persisted rows in `apex_users`, `apex_venues`, `apex_bookings`, `apex_slot_locks`, and `apex_payments`.

### `TC-ARC-006` — `Asia/Kolkata` (`IST`) Timezone Determinism on UTC Cloud Containers
- **Persona / User Type:** Player / Facility Owner
- **Linked Requirement:** `brd.md` §8 (`Timezone Determinism`), `BA-03`, `BA-06`
- **Preconditions:** Run Tier 2 JVM with `-Duser.timezone=UTC` (simulating Render Docker runtime) at `23:45 IST` (`18:15 UTC`, when IST date is `D+1` vs UTC `D`) and `00:30 IST` (`19:00 UTC`).
- **Steps:**
  1. **REST API (Slot Generation):** Call `GET /api/v1/availability?resourceId=res_thane_crk_01&date={today_IST}`.
  2. **REST API (Cancellation Window):** Create booking `24.5 hours` before match in `Asia/Kolkata` and call `POST /api/v1/bookings/{id}/cancel`.
  3. **REST API (Owner Calendar):** Call `GET /api/v1/owner/calendar` with omitted `startDate`.
- **Expected Assertions:**
  - **UI:** `SlotCalendar` and `OwnerCalendar` dates and `PAST` slot markers match `IST` clock, never lagging by 5h 30m.
  - **REST API:** Backend uses explicit `ZoneId.of("Asia/Kolkata")` so `hoursUntilSlot` and `isToday` evaluate identically under `-Duser.timezone=UTC` and `Asia/Kolkata` (fix per Gap `GAP-TZ-01`).
  - **Neon SQL:** Timestamps stored as UTC epoch milliseconds (`BIGINT`); `booking_date` matches IST calendar date.

### `TC-ARC-007` — Automated Backend (`mvn test`) & Service Integration (`tests/runner.js`) Suites
- **Persona / User Type:** Webhook/Admin (QA Automation)
- **Linked Requirement:** All (`BA-01` – `BA-08`)
- **Preconditions:** Java 21 + Maven installed in `backend/`; Node.js installed at project root.
- **Steps:**
  1. **CLI (Backend):** Execute `mvn test` inside `c:\Users\pushp\googlews\project-apex\backend` (`BackendApplicationTests`, `NeonPersistenceTests`, `RouteParityTests`, `TechSpecWorkflowsTest`).
  2. **CLI (Frontend/Service Runner):** Execute `node tests/runner.js` at `c:\Users\pushp\googlews\project-apex`.
- **Expected Assertions:**
  - **UI / CLI:** Both runners exit with code `0`; 100% test cases pass with zero skipped assertions.
  - **REST API:** `RouteParityTests` confirms `/auth/registration/*`, `/auth/register/*`, `/auth/logout`, and `/bookings/my` + `/my-passes`; `NeonPersistenceTests` verifies all 13 SQL tables and `uq_apex_slot_lock`; `TechSpecWorkflowsTest` verifies E2E booking, webhook, and refund.
  - **Neon SQL:** H2 PostgreSQL-mode (`MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE`) passes identical DDL from `db/schema.sql`.

### `TC-ARC-008` — Android Kotlin Client Unit & Gradle Build Verification
- **Persona / User Type:** Webhook/Admin (Mobile QA)
- **Linked Requirement:** Multi-Client Contract Parity (`ApexApiClient.kt`)
- **Preconditions:** Android SDK / JDK configured in `c:\Users\pushp\googlews\project-apex\android`.
- **Steps:**
  1. **CLI:** Execute `./gradlew testDebugUnitTest assembleDebug` inside `c:\Users\pushp\googlews\project-apex\android`.
  2. **Contract Inspection:** Verify `ApexApiClientTest` (`testNormalizeBaseUrlStripsSlashesAndApiV1Suffix`, `testParseHealthJsonNeonPostgresResponse`, `testJwtAuthParsingAndLogoutCleanup`, `testBackendDomainPayloadMappers`) and `ApexRepositoryTest`.
- **Expected Assertions:**
  - **UI / Mobile Build:** Gradle unit tests pass 100% and debug APK compiles cleanly without missing dependencies.
  - **REST API:** `ApexApiClient.kt` URL normalization, JWT Bearer injection, `/api/v1/health` JSON mapping, `slots`, `bookings/initiate`, `verify-payment`, `gate/verify-pass`, `gate/walk-in`, and `facilities` match Java backend responses.
  - **Neon SQL:** Verified via backend contract parity (`parseVenue`, `parseResourceAsCourt`, `parseBookingPass` read `minSlotPricePaise`, `basePricePaise`, `finalPayablePaise`, `bookingCode`, `qrPassToken`).

---

## 8. Engineering Remediation & Code/API/UI Gap Register (Pre-Certification Blockers)

During QA audit of the codebase prior to execution, the following **21 concrete defects and missing features** were identified (`8 P0`, `12 P1`, `1 P2`). Engineering (`apex-backend`, `apex-database`, `apex-frontend`, `apex-deployment`) MUST resolve **all P0 blockers** and reconcile **P1 gaps** before formal Release Certification.

### 8.1 P0 — Critical Security, RBAC & Financial Integrity Blockers

| Gap ID | Severity | Component / Exact File & Lines | Observed Defect vs BRD / Stakeholder Requirement | Required Fix |
| :--- | :---: | :--- | :--- | :--- |
| **`GAP-SEC-01`** | **P0** | [BookingController.java](file:///c:/Users/pushp/googlews/project-apex/backend/src/main/java/com/projectapex/controller/BookingController.java#L31-L100), [UserController.java](file:///c:/Users/pushp/googlews/project-apex/backend/src/main/java/com/projectapex/controller/UserController.java#L20-L63), [SlotController.java](file:///c:/Users/pushp/googlews/project-apex/backend/src/main/java/com/projectapex/controller/SlotController.java#L41-L69) | **Tenant Isolation & Anonymous Fallback Bypass:** `BookingController.getBooking(bookingId)` has **zero ownership check** (any authenticated user can read any booking/QR token). `initiateBooking`, `getMyBookings`, `cancelBooking`, `UserController.getCurrentUser`/`updateCurrentUser`, and `SlotController` fall back to `"demo_player"`, `fallbackUid`, or spoofable `X-User-Id`/`X-Owner-Id` headers; `UserController.getUserById` exposes any user (including `passwordHash`) to any caller. | Extract `uid` strictly from JWT `Authentication` (never `X-User-Id`, `fallbackUid`, or `"demo_player"`). Reject cross-user reads/mutations on `/bookings/{id}` and `/users/{uid}` with `403`. Add `@JsonIgnore` on `UserRecord.passwordHash`. |
| **`GAP-SEC-02`** | **P0** | [SecurityConfig.java](file:///c:/Users/pushp/googlews/project-apex/backend/src/main/java/com/projectapex/config/SecurityConfig.java#L31-L43), [FacilityController.java](file:///c:/Users/pushp/googlews/project-apex/backend/src/main/java/com/projectapex/controller/FacilityController.java#L26-L38), [GateController.java](file:///c:/Users/pushp/googlews/project-apex/backend/src/main/java/com/projectapex/controller/GateController.java#L29-L66), [BookingService.java](file:///c:/Users/pushp/googlews/project-apex/backend/src/main/java/com/projectapex/service/BookingService.java#L521-L557), [VenueService.java](file:///c:/Users/pushp/googlews/project-apex/backend/src/main/java/com/projectapex/service/VenueService.java#L251-L279) | **Unprotected Owner/Gate Endpoints & Missing Venue Ownership Checks:** `/api/v1/facilities`, `/api/v1/gate/**`, and `/api/v1/bookings/walkin` are NOT protected by `hasAnyRole("OWNER", "ADMIN")` in `SecurityConfig` — any `ROLE_PLAYER` can create courts, log walk-ins, or check in QR passes! Furthermore, `VenueService.createFacility` and `BookingService.verifyGatePass` / `createWalkInBooking` never check whether `ownerId` owns the target venue/booking. | Protect `/api/v1/facilities/**`, `/api/v1/gate/**`, and `/api/v1/bookings/walkin` with `hasAnyRole("OWNER", "ADMIN")`; enforce `venue.getOwnerId().equals(ownerId)` in `createFacility`, `createWalkInBooking`, and `verifyGatePass` (unless `ROLE_ADMIN`). |
| **`GAP-SEC-03`** | **P0** | [GlobalExceptionHandler.java](file:///c:/Users/pushp/googlews/project-apex/backend/src/main/java/com/projectapex/exception/GlobalExceptionHandler.java#L26-L33) | **Unauthorized Mutations Return `400` Instead of `403`:** Services throw `IllegalStateException("Unauthorized...")`, which `GlobalExceptionHandler` maps to `400 Bad Request` (or `409` if message contains `"conflict"`/`"reserved"`). | Introduce `AccessDeniedException` / `ForbiddenException` returning HTTP `403 Forbidden`, and reserve `409 Conflict` explicitly for double-booking / lock conflicts. |
| **`GAP-SEC-04`** | **P0** | [AuthService.java](file:///c:/Users/pushp/googlews/project-apex/backend/src/main/java/com/projectapex/service/AuthService.java#L146-L160), [JwtAuthenticationFilter.java](file:///c:/Users/pushp/googlews/project-apex/backend/src/main/java/com/projectapex/config/JwtAuthenticationFilter.java#L41-L45), [VenueOwnerPortal.js](file:///c:/Users/pushp/googlews/project-apex/src/components/VenueOwnerPortal.js#L66) | **Role String Split-Brain (`"venue_owner"` vs `"ROLE_OWNER"`):** `AuthService.verifyAndRegister` & `registerWithPassword` save `"venue_owner"` literally → JWT filter sets `ROLE_VENUE_OWNER` → `SecurityConfig` (`hasAnyRole("OWNER", "ADMIN")`) blocks the newly registered owner with `403` on `/api/v1/owner/**`! Conversely, `VenueOwnerPortal.js` checks `role !== 'venue_owner'`, locking out `demo_owner` (`ROLE_OWNER`). | Normalize backend roles cleanly to `ROLE_PLAYER` / `ROLE_OWNER` / `ROLE_ADMIN` on all registration paths (mapping `"player"` → `"ROLE_PLAYER"`, `"venue_owner"`/`"owner"` → `"ROLE_OWNER"`) and accept both in UI role checks. Also fix seeded venues in `FirebaseService` (`owner_01` vs `demo_owner`). |
| **`GAP-SEC-05`** | **P0** | [RazorpayService.java](file:///c:/Users/pushp/googlews/project-apex/backend/src/main/java/com/projectapex/service/RazorpayService.java#L103-L136) | **Unconditional Signature Verification Bypass:** `verifySignature` accepts `test_sig_*` / `"SIMULATED_TEST_SIGNATURE"` and `verifyWebhookSignature` accepts `test_webhook_sig_*` / `"SIMULATED_WEBHOOK_SIG"` unconditionally in production. | Gate simulated signature shortcuts strictly behind `@Value("${app.razorpay.allow-simulated-signatures:false}")` (`false` in production). |
| **`GAP-SEC-06`** | **P0** | [CorsConfig.java](file:///c:/Users/pushp/googlews/project-apex/backend/src/main/java/com/projectapex/config/CorsConfig.java#L19), Controllers `@CrossOrigin(origins = "*")` | **Wildcard CORS Policy:** `CorsConfig` allows `*` origin patterns (`setAllowedOriginPatterns(List.of("*"))` with `allowCredentials(true)`) instead of reading `${cors.allowed-origins}` from [application.properties](file:///c:/Users/pushp/googlews/project-apex/backend/src/main/resources/application.properties#L29). | Inject `${cors.allowed-origins}` into `CorsConfig` and remove `@CrossOrigin(origins = "*")` overrides on controllers. |
| **`GAP-AUTH-01`** | **P0** | [AuthService.java](file:///c:/Users/pushp/googlews/project-apex/backend/src/main/java/com/projectapex/service/AuthService.java#L33-L302), [OtpService.java](file:///c:/Users/pushp/googlews/project-apex/backend/src/main/java/com/projectapex/service/OtpService.java#L66-L112), [schema.sql](file:///c:/Users/pushp/googlews/project-apex/backend/src/main/resources/db/schema.sql#L12-L40) | **No Duplicate Phone/Email Rejection on Backend:** Backend `OtpService.sendRegistrationOtp` and all 4 `AuthService` registration methods allow duplicate emails/phones; `apex_users` only defines non-unique indexes instead of `UNIQUE` constraints. | Enforce normalized phone & case-insensitive email uniqueness in `OtpService` + `AuthService` (`409 Conflict`) and add `UNIQUE` constraints on `apex_users(phone)` and `apex_users(email)`. |
| **`GAP-AUTH-02`** | **P0** | [otpService.js](file:///c:/Users/pushp/googlews/project-apex/src/services/otpService.js#L285-L301) | **Client Login OTP Missing 3-Attempt Lockout Guard:** `OtpService.verifyLoginOtp` decrements `attemptsRemaining` into negative integers without checking `if (session.attemptsRemaining <= 0)` before comparing OTPs. | Add `if (session.attemptsRemaining <= 0) throw new Error(...)` before verifying OTP in `otpService.js`. |

### 8.2 P1 — Missing CRUD Endpoints, Pricing/Refund Logic & Database Gaps

| Gap ID | Severity | Component / Exact File & Lines | Observed Defect vs BRD / Stakeholder Requirement | Required Fix |
| :--- | :---: | :--- | :--- | :--- |
| **`GAP-CRUD-01`** | **P1** | [OwnerVenueController.java](file:///c:/Users/pushp/googlews/project-apex/backend/src/main/java/com/projectapex/controller/OwnerVenueController.java), [FacilityController.java](file:///c:/Users/pushp/googlews/project-apex/backend/src/main/java/com/projectapex/controller/FacilityController.java), [NeonPersistenceService.java](file:///c:/Users/pushp/googlews/project-apex/backend/src/main/java/com/projectapex/service/NeonPersistenceService.java) | **Missing Owner Delete Endpoints:** Zero `@DeleteMapping` endpoints exist for `/api/v1/owner/venues/{venueId}`, `/api/v1/owner/resources/{resourceId}`, or `/api/v1/facilities/{courtId}` (and no `PUT /api/v1/facilities/{courtId}`). `NeonPersistenceService` has no delete methods for venues/resources/facilities. | Add ownership-checked `DELETE` (soft-delete or guarded cascade) endpoints for venues, resources, and facilities + `@CacheEvict` + Neon SQL deletes. |
| **`GAP-CRUD-02`** | **P1** | [BookingController.java](file:///c:/Users/pushp/googlews/project-apex/backend/src/main/java/com/projectapex/controller/BookingController.java), [BookingService.java](file:///c:/Users/pushp/googlews/project-apex/backend/src/main/java/com/projectapex/service/BookingService.java), [PlayerPortal.js](file:///c:/Users/pushp/googlews/project-apex/src/components/PlayerPortal.js) | **Missing Player Booking Reschedule Flow:** No API endpoint (`PUT/PATCH /api/v1/bookings/{bookingId}/reschedule`), service method, or UI modal exists to swap booking date/slot with atomic lock transfer. | Implement atomic reschedule endpoint + lock transfer transaction + `PlayerPortal` reschedule modal. |
| **`GAP-CRUD-03`** | **P1** | [BookingController.java](file:///c:/Users/pushp/googlews/project-apex/backend/src/main/java/com/projectapex/controller/BookingController.java), [PlayerPortal.js](file:///c:/Users/pushp/googlews/project-apex/src/components/PlayerPortal.js) | **Missing Player Delete/Remove Booking Flow:** No `DELETE /api/v1/bookings/{bookingId}` (for archived/cancelled/expired bookings) exists in backend or `PlayerPortal`. | Add ownership-verified `DELETE /api/v1/bookings/{bookingId}` (soft-archiving/deleting non-active bookings while preserving `apex_payments` audit rows). |
| **`GAP-BKG-01`** | **P1** | [BookingService.java](file:///c:/Users/pushp/googlews/project-apex/backend/src/main/java/com/projectapex/service/BookingService.java#L266-L276) | **Tier 1 (`> 24h`) Missing ₹50 Handling Fee Deduction:** Backend refunds `100%` (`booking.getFinalPayablePaise()`) on Tier 1 instead of `Math.max(0L, booking.getFinalPayablePaise() - 5000L)` mandated by `brd.md` BA-06 and `cancellationService.js`. Also `cancelBookingByPlayer` / `cancelBookingByOwner` do not write `REFUND` rows into `apex_payments`. | Deduct `5000L` (₹50) on Tier 1 (`hoursUntilSlot > 24`) and persist `REFUND` rows in `apex_payments`. |
| **`GAP-BKG-02`** | **P1** | [BookingService.java](file:///c:/Users/pushp/googlews/project-apex/backend/src/main/java/com/projectapex/service/BookingService.java#L169-L226) | **`verifyPayment` Lacks Hold-Expiry, Status & Order-Match Guards:** Does not verify `status == PENDING_PAYMENT`, `holdExpiresAt >= now`, or `req.getRazorpayOrderId().equals(booking.getRazorpayOrderId())`, and `verifyPayment` + `initiateBooking` lack `@Transactional` wrapping for Neon DB lock atomicity across horizontal Render pods. | Validate booking status, hold expiry, and exact `razorpayOrderId` match; wrap lock acquisition and confirmation in `@Transactional`. |
| **`GAP-BKG-03`** | **P1** | [WebhookService.java](file:///c:/Users/pushp/googlews/project-apex/backend/src/main/java/com/projectapex/service/WebhookService.java#L103-L111) | **Late Conflict Auto-Refund Incomplete Ledger Update:** Sets `CONFLICT_REFUND_PENDING` and calls Razorpay refund, but omits `booking.setRefundAmountPaise(...)` and does not insert a `REFUND` row in `apex_payments`. | Populate `refundAmountPaise` and persist `REFUND` ledger row in `apex_payments`. |
| **`GAP-DB-01`** | **P1** | [schema.sql](file:///c:/Users/pushp/googlews/project-apex/backend/src/main/resources/db/schema.sql#L186-L219), [BookingRecord.java](file:///c:/Users/pushp/googlews/project-apex/backend/src/main/java/com/projectapex/model/BookingRecord.java), [NeonPersistenceService.java](file:///c:/Users/pushp/googlews/project-apex/backend/src/main/java/com/projectapex/service/NeonPersistenceService.java#L1033-L1172) | **Missing `checked_in_at` Column & Unhydrated `qr_pass_token`:** `apex_bookings` and `BookingRecord` lack `checked_in_at` (`BA-07`); `findAllBookings()` fails to select/set `qr_pass_token` on DB hydration; `UserRecord.getVenueId()` hardcodes `"venue_thane_01"`. | Add `checked_in_at BIGINT` to `apex_bookings`, `BookingRecord`, and `NeonPersistenceService`; hydrate `qr_pass_token` properly; fix `UserRecord.getVenueId()`. |
| **`GAP-PRC-01`** | **P1** | [FirebaseService.java](file:///c:/Users/pushp/googlews/project-apex/backend/src/main/java/com/projectapex/service/FirebaseService.java#L309-L321), [PricingAndDiscountService.java](file:///c:/Users/pushp/googlews/project-apex/backend/src/main/java/com/projectapex/service/PricingAndDiscountService.java#L179-L212), [CalendarAvailabilityService.java](file:///c:/Users/pushp/googlews/project-apex/backend/src/main/java/com/projectapex/service/CalendarAvailabilityService.java#L56-L95) | **Dynamic Pricing & Schedule Band Gaps:** `FirebaseService` seeds Afternoon Grind at `25%` instead of `35%`, omits Early Bird (`06:00–08:00`, `-20%`) and Night Owl (`23:00–02:00`, `-25%`), and omits `APEX20` & `ODDHOUR35`. `PricingAndDiscountService` ignores `daysOfWeek`, `applicableSports`, `validFrom/validUntil`, `maxTotalUses`, and `maxUsesPerUser`; default schedule stops at `23:00` so Night Owl slots cannot generate. | Seed all BRD discounts (`35%` Afternoon, `20%` Early Bird, `25%` Night Owl, `APEX20`, `ODDHOUR35`, `TURF20`, `THANE50`, `BULK_SLOTS_AUTO`); enforce `daysOfWeek`, validity windows, and usage caps in `PricingAndDiscountService`. |
| **`GAP-PRC-02`** | **P1** | [PricingService.java](file:///c:/Users/pushp/googlews/project-apex/backend/src/main/java/com/projectapex/service/PricingService.java#L113), [pricingEngine.js](file:///c:/Users/pushp/googlews/project-apex/src/services/pricingEngine.js#L112), [VenueService.java](file:///c:/Users/pushp/googlews/project-apex/backend/src/main/java/com/projectapex/service/VenueService.java#L251-L306) | **Dual-Model Split-Brain & Missing 100 Paise Floor in Legacy Pricing:** `FacilityRecord` (`apex_facilities`) and `ResourceRecord` (`apex_resources`) are disconnected; `PricingService` & `pricingEngine.js` clamp at `0` instead of `100` paise (`₹1`). | Synchronize `FacilityRecord` ↔ `ResourceRecord` writes (or unify onto `ResourceRecord`) and enforce `Math.max(1, ...)` in rupees / `Math.max(100L, ...)` in paise everywhere. |
| **`GAP-TZ-01`** | **P1** | [BookingService.java](file:///c:/Users/pushp/googlews/project-apex/backend/src/main/java/com/projectapex/service/BookingService.java#L263), [CalendarAvailabilityService.java](file:///c:/Users/pushp/googlews/project-apex/backend/src/main/java/com/projectapex/service/CalendarAvailabilityService.java#L77-L78), [OwnerCalendarController.java](file:///c:/Users/pushp/googlews/project-apex/backend/src/main/java/com/projectapex/controller/OwnerCalendarController.java#L47) | **Missing `Asia/Kolkata` ZoneId:** Services call `LocalDate.now()`, `LocalTime.now()`, and `LocalDateTime.now()` without `ZoneId.of("Asia/Kolkata")`, causing a 5.5-hour drift on UTC Render containers. | Inject shared `ZoneId IST = ZoneId.of("Asia/Kolkata")` across all date/time and cancellation window evaluations. |

### 8.3 P1/P2 — Tier 1 Frontend UI ↔ Tier 2 Backend API Wiring Gaps

| Gap ID | Severity | Component / Exact File & Lines | Observed Wiring Defect | Required Fix |
| :--- | :---: | :--- | :--- | :--- |
| **`GAP-UI-01`** | **P1** | [SlotCalendar.js](file:///c:/Users/pushp/googlews/project-apex/src/components/SlotCalendar.js#L7-L22), [CheckoutModal.js](file:///c:/Users/pushp/googlews/project-apex/src/components/CheckoutModal.js#L21-L246), [concurrencyService.js](file:///c:/Users/pushp/googlews/project-apex/src/services/concurrencyService.js#L9) | **Checkout Payload Mismatch, 5-Min Hold & Single-Slot Lock:** `CheckoutModal.js` calls `ApiClient.createRazorpayOrder({ amount, currency, receipt })` which mismatches `CreateRazorpayOrderRequest` (`courtId, date, slot, amountInRupees`) and fails `400`; `SlotCalendar` enforces 5-min (`300s`) hold instead of 10-min (`600s`) and only allows selecting 1 slot (blocking 2+ slot 15% bulk discount in UI). | Wire `SlotCalendar` multi-slot selection + `CheckoutModal` directly to `POST /api/v1/pricing/quote`, `POST /api/v1/bookings/initiate` (10-min `600s` hold), and `POST /api/v1/bookings/{id}/verify-payment`. |
| **`GAP-UI-02`** | **P1** | [UserAuthModal.js](file:///c:/Users/pushp/googlews/project-apex/src/components/UserAuthModal.js#L576-L936), [PlayerPortal.js](file:///c:/Users/pushp/googlews/project-apex/src/components/PlayerPortal.js#L215), [VenueExplorer.js](file:///c:/Users/pushp/googlews/project-apex/src/components/VenueExplorer.js), [VenueOwnerPortal.js](file:///c:/Users/pushp/googlews/project-apex/src/components/VenueOwnerPortal.js), [AddVenueModal.js](file:///c:/Users/pushp/googlews/project-apex/src/components/AddVenueModal.js) | **UI Bypasses `ApiClient` for Local `mockDb`:** Registration/Login OTPs, Venue discovery, Venue/Court creation, Slots, Bookings, Cancellations, Walk-Ins, and Gate QR Check-In mutate browser `localStorage` `mockDb` instead of live Tier 2 Render REST endpoints; `PlayerPortal` logout skips `ApiClient.logout()`; `UserAuthModal` `verifyLoginOtp` ignores `ApiClient` JWT return. | Route all UI service calls through `ApiClient` first whenever backend is online (falling back to offline cache only for read resilience, never silently faking financial/auth mutations when server rejects `4xx`). |
| **`GAP-UI-03`** | **P2** | [PlayerPortal.js](file:///c:/Users/pushp/googlews/project-apex/src/components/PlayerPortal.js), [VenueOwnerPortal.js](file:///c:/Users/pushp/googlews/project-apex/src/components/VenueOwnerPortal.js), [AdminConsole.js](file:///c:/Users/pushp/googlews/project-apex/src/components/AdminConsole.js) | **Missing Admin & Management UI Screens:** No UI screens exist for User Profile Edit (`PUT /users/me`), Booking Reschedule/Delete, Owner Venue/Court Edit & Delete, Owner Weekly Schedule/DateOverride/Discount configuration, Owner Emergency Cancel/Status Update, or Admin Pending Owner KYC & Venue Moderation (`AdminConsole.js` still renders Spark/Firestore mock metrics). | Add UI controls for all implemented backend endpoints and update `AdminConsole.js` to display live Tier 2/3 Render + Neon PostgreSQL telemetry and Admin KYC/venue actions. |

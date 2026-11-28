# Project Apex: 3-Tier Free-Tier Deployment Guide & Operational Runbook
**Governing Role:** `apex-deploy` (Cloud Release & Free-Tier Deployment Engineer)  
**Repository:** [`pushpendrarait/project-apex`](https://github.com/pushpendrarait/project-apex) (`main` branch)  
**Target Stack (100% Free Tier):**
1. **Tier 1 (Frontend & PWA Edge):** Firebase Hosting Spark Free Tier (`project-apex-pushp`)
2. **Tier 2 (Middleware API):** Render Cloud Free Tier Docker Web Service (`project-apex-backend` via `render.yaml`)
3. **Tier 3 (Relational ACID Database):** Neon Serverless PostgreSQL Free Tier (`backend/src/main/resources/db/schema.sql` with `sslmode=require`)

---

## 1. Prerequisites & Tooling
- **Node.js** v18+ and npm
- **Java 21 LTS** & **Apache Maven 3.9+** (for local verification and multi-stage Docker builds)
- **Firebase CLI** (`firebase-tools`) authenticated to the project owner account:
  ```bash
  firebase login:list
  ```
- **GitHub CLI (`gh`)** / Git authenticated as `pushpendrarait`
- **External Cloud Accounts (Free Tier):**
  - **Neon Console** (`https://console.neon.tech`) for `DATABASE_URL`
  - **Render Dashboard** (`https://dashboard.render.com`) for Blueprint / Docker Web Service deployment

---

## 2. Pre-Flight Quality & Compliance Gates
Run **both** verification suites from the repository before committing or deploying:

1. **Java 21 Spring Boot & Neon Persistence Tests:**
   ```bash
   cd backend && mvn test
   ```
   Verifies `BackendApplicationTests` and `NeonPersistenceTests` (Neon `postgresql://` -> `jdbc:postgresql://` + `sslmode=require` resolution, schema initialization, and `/api/v1/health`).

2. **3-Tier Architectural Audits & 7-Workflow Acceptance Suite:**
   ```bash
   npm test
   ```
   Executes `node orchestrator/orchestrator.js verify && node tests/runner.js`:
   - **12 Architectural Audits:** Zero Cloud Functions, Spark Tier daily consolidated `availability/` read minimization, 5-min concurrency locks, `firestore.rules` guards, PWA `manifest.json` + `sw.js`, dual OTP auth, `tests/runner.js`, Spring Boot + Razorpay middleware, `docs/brd.md` (`BA-01`..`BA-08`), `render.yaml` + `backend/Dockerfile` + `NeonDataSourceConfig.java`, `schema.sql` (`apex_slot_locks` / `uq_apex_slot_lock`), and `index.html` + `firebase.json` + `src/services/apiClient.js`.
   - **7 End-to-End Workflows (`WF01`–`WF07`):** 110 assertions across registration, owner auto-provisioning, venue onboarding, atomic slot holds, walk-in/gate check-in, 3-tier cancellations, and middleware/Razorpay checkout.

---

## 3. Tier 3 Deployment — Neon Serverless PostgreSQL (Free Tier)
1. In the [Neon Console](https://console.neon.tech), create a **Free Tier** PostgreSQL project (recommended region: `ap-southeast-1` Singapore to co-locate with Render).
2. Copy the pooled or direct PostgreSQL connection string:
   ```text
   postgresql://<user>:<password>@<endpoint>.aws.neon.tech/neondb?sslmode=require
   ```
3. **SSL & Pool Behavior (`NeonDataSourceConfig.java`):**
   - Reads `NEON_DATABASE_URL` or `DATABASE_URL` (normalizes `postgres://` / `postgresql://` to `jdbc:postgresql://` and enforces `sslmode=require`).
   - Uses Neon Free Tier friendly HikariCP sizing (`maximumPoolSize=5`, `minimumIdle=1`).
   - Falls back automatically to embedded H2 PostgreSQL-Mode if `DATABASE_URL` is unset (local/test).
4. **Schema Initialization (`backend/src/main/resources/db/schema.sql`):**
   - `NeonPersistenceService.ensureSchema()` runs `db/schema.sql` idempotently (`CREATE TABLE IF NOT EXISTS`, 13 tables including `apex_slot_locks` with `uq_apex_slot_lock`) at application startup.
   - Optional manual pre-provisioning:
     ```bash
     psql "$DATABASE_URL" -f backend/src/main/resources/db/schema.sql
     ```

---

## 4. Tier 2 Deployment — Render Free Tier Docker Web Service
Render configuration is defined in [`render.yaml`](../../render.yaml) and [`backend/Dockerfile`](../../backend/Dockerfile):
- **Service Name:** `project-apex-backend`
- **Runtime / Plan:** `docker` / `free` (Region: `singapore`)
- **Health Check Path:** `/api/v1/health` (Port `8080`)

### 1-Click Blueprint Provisioning
Open the Render Blueprint URL linked to the GitHub repository:
```text
https://dashboard.render.com/blueprint/new?repo=https://github.com/pushpendrarait/project-apex
```

### Required Render Environment Variables
Configure the following variables when prompted by the Blueprint (`sync: false` fields in `render.yaml`):
| Variable | Source / Value | Notes |
| --- | --- | --- |
| `DATABASE_URL` | Neon Free Tier connection string (`?sslmode=require`) | Required for Tier 3 Neon persistence |
| `RAZORPAY_KEY_ID` | Razorpay Test/Live Key ID | Required for checkout order creation |
| `RAZORPAY_KEY_SECRET` | Razorpay Test/Live Key Secret | Required for HMAC signature verification |
| `RAZORPAY_WEBHOOK_SECRET` | Razorpay Webhook Secret | Required for webhook validation |
| `JWT_SECRET` | Auto-generated (`generateValue: true` in `render.yaml`) | Managed by Render |
| `CORS_ALLOWED_ORIGINS` | `https://project-apex-pushp.web.app,https://project-apex-pushp.firebaseapp.com,http://localhost:3000,http://localhost:5173` | Pre-configured in `render.yaml` |

*(Optional if deploying via Render CLI/API with credentials: set `RENDER_API_KEY` in the shell environment.)*

---

## 5. Tier 1 Deployment — Firebase Hosting Spark Free Tier
1. Confirm `.firebaserc` points to `project-apex-pushp` and `firebase.json` defines SPA rewrites (`"**" -> "/index.html"`) and `firestore` rules/indexes.
2. Deploy hosting, Firestore rules, and Firestore indexes:
   ```bash
   firebase deploy --only hosting,firestore:rules,firestore:indexes
   ```
3. **Live Tier 1 URLs:**
   - Primary Web App: `https://project-apex-pushp.web.app`
   - Alternate Domain: `https://project-apex-pushp.firebaseapp.com`
   - Direct Sign-In Deep Link: `https://project-apex-pushp.web.app/login` (handled by Firebase Hosting rewrite to `/index.html` and routed client-side to `UserAuthModal`)

---

## 6. GitHub Synchronization (`pushpendrarait/project-apex`)
Per repository automation rules (`AGENTS.md` / `GEMINI.md`), every update or cloud deployment must be committed and pushed to `main`:
```bash
git status
git add .
git commit -m "Deploy and verify 3-tier Firebase, Render, and Neon configuration"
git push origin main
```

---

## 7. Live Verification Checks
After deployment, verify both public tiers directly via HTTP `GET`:

1. **Tier 1 Home & 3-Tier UI Shell:**
   ```bash
   curl -i https://project-apex-pushp.web.app
   ```
   Expect `HTTP/2 200` and served HTML containing `3-Tier Live Architecture` and `/login` client routing.

2. **Tier 1 Login Deep-Link:**
   ```bash
   curl -i https://project-apex-pushp.web.app/login
   ```
   Expect `HTTP/2 200` (served via Firebase Hosting rewrite to `/index.html`).

3. **Tier 2 + Tier 3 Health & DB Connectivity:**
   ```bash
   curl -i https://project-apex-backend-fqe3.onrender.com/api/v1/health
   ```
   Expect `HTTP/2 200` with JSON payload `"status":"UP"`, `"service":"project-apex-backend"`, `"database":"Neon PostgreSQL"`, and `"dbConnected":true`. *(Note: Render Free Tier instances spin down after idle periods; allow up to ~50s on cold start.)*

# Project Apex: Deployment Guide & Operational Runbook
**Governing Agent:** `apex-deployment` (Deployment Engineer)  
**Target:** Firebase Spark Free Tier (`project-apex-pushp`)  

---

## 1. Prerequisites & Environment Setup
- Node.js v18+ installed.
- Firebase CLI (`firebase-tools`) installed and authenticated:
  ```bash
  firebase login
  ```
- Active Firebase Project: `project-apex-pushp` configured in `.firebaserc`.

---

## 2. Pre-Flight Architecture Verification
Before deploying, execute the automated architectural compliance auditor:
```bash
node orchestrator/orchestrator.js verify
```
This script audits:
1. **Zero Cloud Functions Check:** Asserts no `functions/` directory or server compute exists.
2. **Read Minimization Check:** Validates daily consolidated doc structure in `availability/`.
3. **Security Rules Validation:** Validates `firestore.rules` syntax and locking guards.
4. **PWA & Offline Cache:** Verifies `manifest.json` and `sw.js` presence.

---

## 3. Deployment Commands
Deploy hosting, rules, and indexes to Firebase Spark Tier:
```bash
firebase deploy --only hosting,firestore:rules,firestore:indexes
```
Live Hosting URL: `https://project-apex-pushp.web.app` or `https://project-apex-pushp.firebaseapp.com`

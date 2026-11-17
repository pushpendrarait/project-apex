# Player Privacy Policy & Data Protection Charter
**Governing Agent:** `apex-legal` (Legal Compliance Expert)  
**Compliance Standard:** Digital Personal Data Protection Act, 2023 (DPDP Act, India)  

---

## 1. Information Collected & Lawful Purpose
We collect minimal personal data required for slot verification and gate entry:
- **Identifier Data:** Full Name, Mobile Phone Number, and Email Address.
- **Transactional Data:** Slot date/time, venue, payment reference ID, and entry QR pass tokens.
- **Location Telemetry:** City preferences (Thane, Mumbai, Navi Mumbai) for localized venue discovery.

---

## 2. Serverless Storage & Spark Tier Architecture
1. All player data is stored strictly within Google Cloud Firestore and encrypted in transit (TLS 1.3) and at rest (AES-256).
2. Project Apex employs **Zero Cloud Functions**, eliminating third-party server-side code execution vulnerabilities.
3. Access control is strictly enforced via Firestore Security Rules.

---

## 3. Data Retention & Erasure Rights
Under the DPDP Act 2023:
1. Players have the right to request access to their booking history.
2. Players may request complete account deletion and data scrubbing by contacting privacy@projectapex.in.

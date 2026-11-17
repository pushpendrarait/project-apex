# Project Apex: Terms of Service & Intermediary Agreement
**Governing Agent:** `apex-legal` (Legal Compliance Expert)  
**Jurisdiction:** Mumbai & Thane, Maharashtra, Republic of India  
**Applicable Statutes:** Information Technology Act, 2000 (Section 79); Consumer Protection (E-Commerce) Rules, 2020; Indian Contract Act, 1872  

---

## 1. Electronic Record & Intermediary Status
This document is an electronic record in terms of the Information Technology Act, 2000, and rules thereunder as applicable, and the amended provisions pertaining to electronic records in various statutes as amended by the Information Technology Act, 2000.

Project Apex ("Platform") acts purely as an online marketplace and technological intermediary facilitating reservations between prospective sports players and registered sports facilities ("Venue Partners"). The Platform does not own, operate, or maintain any physical turf or court.

---

## 2. Reservation & Concurrency Lock Protocol
1. **5-Minute Temporary Lock Lease:** When an authenticated player selects an available slot, the Platform grants a temporary atomic reservation lease of 5 minutes.
2. **Lease Expiration:** If payment confirmation is not completed within 300 seconds, the lock document expires automatically, releasing the slot back to the public pool without liability to the Platform.
3. **Double Booking Indemnity:** In the rare event of simultaneous concurrent network contention, the atomic Firestore transaction will resolve in favor of the first committed transaction. The rejected party will receive an immediate refund or credit.

---

## 3. Dynamic Off-Peak Pricing & Promo Codes
1. The Platform employs dynamic time-band pricing algorithms offering up to 35% discount during odd hours (11:00 AM - 04:00 PM, 11:00 PM - 02:00 AM).
2. Promotional coupons (e.g., `THANE50`, `APEX20`, `ODDHOUR35`) are non-transferable and subject to minimum spend criteria.

---

## 4. Cancellation & Refund Policy
- **Tier 1 (> 24 hours prior):** 100% refund, minus a nominal ₹50 payment gateway handling charge.
- **Tier 2 (6 - 24 hours prior):** 50% refund.
- **Tier 3 (< 6 hours prior):** 0% refund (full fee forfeited to compensate Venue Partner).
- **Force Majeure & Rainouts:** If a venue becomes unplayable due to heavy rains or power failure, the Venue Partner will issue an alternate slot or platform credit.

---

## 5. Governing Law & Dispute Resolution
These Terms shall be governed by and constructed in accordance with the laws of India. Any disputes arising out of reservations in Thane or Mumbai shall be subject to the exclusive jurisdiction of the competent courts in Thane / Mumbai, Maharashtra.

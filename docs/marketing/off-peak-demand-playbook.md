# Dynamic Off-Peak & Odd-Hour Demand Stimulation Playbook
**Governing Agent:** `apex-marketing` (Marketing Expert)  

---

## 1. Dynamic Pricing Strategy (Odd-Hour Bands)

| Time Band | Schedule | Target Audience | Dynamic Incentive |
| :--- | :--- | :--- | :--- |
| **Early Bird** | 06:00 - 08:00 AM (Mon-Fri) | Fitness clubs, senior badminton, school coaching | 20% OFF base rate |
| **Afternoon Grind** | 11:00 AM - 04:00 PM (Mon-Fri) | College students, freelancers, remote workers | 35% OFF base rate |
| **Prime Time** | 07:00 PM - 11:00 PM (All days) | Corporate leagues, weekend matches | Standard rate (1.0x - 1.2x) |
| **Night Owl Turf** | 11:00 PM - 02:00 AM (Daily) | Nocturnal football groups, late-shift pros | 25% OFF base rate |

---

## 2. Promotional Coupon Architecture
- `THANE50`: Flat ₹50 OFF on first booking in Thane (validates location tag).
- `APEX20`: 20% OFF (up to ₹200) for new user signups.
- `ODDHOUR35`: Stackable 10% coupon valid only during Afternoon Grind or Night Owl slots.

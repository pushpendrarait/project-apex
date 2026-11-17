# Data Model Specification: Project Apex (8-Collection Firestore Architecture)
**Mission Slug:** `001-project-apex-mvp`  
**Governing Agent:** `apex-database` (Database Expert)  
**Status:** Approved for Implementation  

---

## 1. Overview of 8 Core Collections
Designed specifically for the Firebase Spark Tier (<50,000 reads/day):

| Collection | Key Strategy | Purpose | Read/Write Frequency |
| :--- | :--- | :--- | :--- |
| `users` | Document ID = `auth.uid` | Player and Venue Owner profiles, roles, loyalty points | Low |
| `venues` | Document ID = `venue_{slug}` | Multi-tenant sports facility catalog, locations, amenities | Moderate (cached) |
| `courts` | Document ID = `court_{slug}` | Specific courts/turfs per venue, sport type, base pricing | Moderate (cached) |
| `availability` | Document ID = `{courtId}_{YYYY-MM-DD}` | Consolidated daily schedule (all 24 slots in 1 doc) | High (1 read/court/day) |
| `concurrency_locks` | Document ID = `{courtId}_{YYYY-MM-DD}_{slot}` | 5-minute atomic reservation leases to prevent double booking | Transactional |
| `bookings` | Document ID = `bk_{timestamp}_{random}` | Immutable booking records with QR codes and statuses | Transactional |
| `transactions` | Document ID = `tx_{timestamp}_{random}` | Financial ledger (payments, refunds, discounts applied) | Transactional |
| `audit_logs` | Document ID = `log_{timestamp}_{random}` | Security & audit trail for state changes and cancellations | Append-only |

---

## 2. Granular Collection Schemas

### 2.1 Collection: `users`
**Path:** `/users/{uid}`
```json
{
  "uid": "usr_991823746",
  "displayName": "Rohan Deshmukh",
  "email": "rohan.deshmukh@example.com",
  "phone": "+919876543210",
  "role": "player", // 'player' | 'venue_owner' | 'admin'
  "city": "Thane",
  "loyaltyPoints": 250,
  "referralCode": "ROHAN50",
  "referredBy": null,
  "createdAt": 1795000000,
  "updatedAt": 1795000000
}
```

### 2.2 Collection: `venues`
**Path:** `/venues/{venueId}`
```json
{
  "venueId": "venue_apex_thane_ghodbunder",
  "name": "Apex Arena Ghodbunder",
  "ownerId": "usr_owner_441",
  "city": "Thane",
  "area": "Ghodbunder Road",
  "address": "Opp. Viviana Mall Annex, Ghodbunder Rd, Thane West, Maharashtra 400607",
  "coordinates": { "lat": 19.2345, "lng": 72.9781 },
  "sports": ["cricket", "football", "badminton"],
  "amenities": ["Floodlights", "Changing Rooms", "Cafeteria", "Parking", "Drinking Water", "Dugout"],
  "rating": 4.8,
  "totalReviews": 142,
  "thumbnail": "https://images.unsplash.com/photo-1540747913346-19e32dc3e97e?auto=format&fit=crop&w=800&q=80",
  "featured": true,
  "status": "ACTIVE"
}
```

### 2.3 Collection: `courts`
**Path:** `/courts/{courtId}`
```json
{
  "courtId": "court_thane_cricket_01",
  "venueId": "venue_apex_thane_ghodbunder",
  "name": "Turf A - Floodlight Box Cricket",
  "sport": "cricket",
  "surface": "Artificial Grass (15mm)",
  "dimensions": "110ft x 65ft",
  "baseHourlyRate": 1200, // INR
  "peakHourlyRate": 1500, // INR
  "oddHourDiscountPct": 30, // 30% off during odd hours
  "operatingHours": {
    "open": "06:00",
    "close": "02:00"
  },
  "isActive": true
}
```

### 2.4 Collection: `availability` (Consolidated Daily Document)
**Path:** `/availability/{courtId}_{YYYY-MM-DD}`
*Critical: 1 Document Contains all slots for the entire 24-hour day to minimize Firestore read counts.*
```json
{
  "courtId": "court_thane_cricket_01",
  "venueId": "venue_apex_thane_ghodbunder",
  "date": "2026-11-20",
  "slots": {
    "06:00": { "status": "AVAILABLE", "price": 960, "isOddHour": true, "discountPct": 20 },
    "07:00": { "status": "AVAILABLE", "price": 960, "isOddHour": true, "discountPct": 20 },
    "08:00": { "status": "AVAILABLE", "price": 1200, "isOddHour": false, "discountPct": 0 },
    "11:00": { "status": "AVAILABLE", "price": 780, "isOddHour": true, "discountPct": 35 },
    "12:00": { "status": "AVAILABLE", "price": 780, "isOddHour": true, "discountPct": 35 },
    "19:00": { "status": "BOOKED", "bookingId": "bk_782910", "userId": "usr_991823746" },
    "20:00": { "status": "BOOKED", "bookingId": "bk_782911", "userId": "usr_102938475" },
    "23:00": { "status": "AVAILABLE", "price": 900, "isOddHour": true, "discountPct": 25 },
    "00:00": { "status": "AVAILABLE", "price": 900, "isOddHour": true, "discountPct": 25 }
  },
  "lastUpdated": 1795000000
}
```

### 2.5 Collection: `concurrency_locks`
**Path:** `/concurrency_locks/{courtId}_{date}_{slot}`
```json
{
  "lockId": "court_thane_cricket_01_2026-11-20_19:00",
  "courtId": "court_thane_cricket_01",
  "date": "2026-11-20",
  "slot": "19:00",
  "userId": "usr_991823746",
  "lockedAt": 1795000100000,
  "expiresAt": 1795000400000, // 5 minutes TTL
  "status": "HELD" // 'HELD' | 'COMMITTED' | 'RELEASED'
}
```

### 2.6 Collection: `bookings`
**Path:** `/bookings/{bookingId}`
```json
{
  "bookingId": "bk_20261120_881920",
  "userId": "usr_991823746",
  "userName": "Rohan Deshmukh",
  "userPhone": "+919876543210",
  "venueId": "venue_apex_thane_ghodbunder",
  "venueName": "Apex Arena Ghodbunder",
  "courtId": "court_thane_cricket_01",
  "courtName": "Turf A - Floodlight Box Cricket",
  "sport": "cricket",
  "date": "2026-11-20",
  "slot": "19:00",
  "durationMinutes": 60,
  "pricing": {
    "basePrice": 1200,
    "discountAmount": 0,
    "promoCode": null,
    "convenienceFee": 20,
    "totalPaid": 1220
  },
  "payment": {
    "paymentId": "pay_rzp_99281729",
    "method": "UPI",
    "status": "SUCCESS"
  },
  "qrPassToken": "APEX-TH-CRK-881920",
  "status": "CONFIRMED", // 'CONFIRMED' | 'CHECKED_IN' | 'CANCELLED' | 'REFUNDED'
  "createdAt": 1795000150000
}
```

### 2.7 Collection: `transactions`
**Path:** `/transactions/{txId}`
```json
{
  "txId": "tx_20261120_339102",
  "bookingId": "bk_20261120_881920",
  "userId": "usr_991823746",
  "type": "PAYMENT", // 'PAYMENT' | 'REFUND' | 'FEE'
  "amount": 1220,
  "gateway": "Razorpay",
  "gatewayRef": "pay_rzp_99281729",
  "status": "SUCCESS",
  "timestamp": 1795000150000
}
```

### 2.8 Collection: `audit_logs`
**Path:** `/audit_logs/{logId}`
```json
{
  "logId": "audit_1795000150001",
  "action": "BOOKING_CREATED",
  "actorId": "usr_991823746",
  "resourceType": "booking",
  "resourceId": "bk_20261120_881920",
  "metadata": {
    "courtId": "court_thane_cricket_01",
    "slot": "19:00",
    "amount": 1220
  },
  "timestamp": 1795000150000
}
```

# Project Apex — Native iOS App (SwiftUI)

Native iOS 16+ SwiftUI client synchronized 1-to-1 with the **Project Apex** Web (Firebase Hosting) and Android (Kotlin/Jetpack Compose) applications, sharing the exact same **3-Tier Architecture**:

1. **Tier 1 (Client):** Native iOS SwiftUI (`ProjectApex`)
2. **Tier 2 (Middleware):** Render Java 21 LTS + Spring Boot 3.2.x REST API (`https://project-apex-backend.onrender.com/api/v1/*`)
3. **Tier 3 (Database):** Neon Serverless PostgreSQL (with H2 PostgreSQL-Mode fallback in local dev)

## Features & Contract Parity
- **Integer Paise & Epoch Milliseconds:** All currency values use `Int64` paise (`1 INR = 100 paise`) and timestamps use `Int64` epoch milliseconds.
- **Dynamic 17-Slot Schedule Engine (06:00–23:00):** Live `/api/v1/availability` queries with automatic offline zero-pregeneration schedule fallback.
- **10-Minute Atomic Hold + Pricing Engine:** `/api/v1/pricing/quote`, `/api/v1/bookings/initiate`, and `/api/v1/bookings/{id}/verify-payment` with `WELCOME50`, `THANE50`, `APEX20`, and `TURF20` promo support.
- **Match Passes & High-Contrast Gate QR:** `/api/v1/bookings/my` and `/api/v1/bookings/{id}/cancel` enforcing the 3-tier refund policy (`>=24h`, `6–23h`, `<6h`).
- **Owner Portal & Gate Scanner:** `/api/v1/gate/verify-pass`, `/api/v1/gate/walk-in`, and `/api/v1/facilities`.
- **Dual Auth & JWT Bearer:** Password and OTP login/registration (`/api/v1/auth/*`) injecting `Authorization: Bearer <token>` and `X-User-Id` / `X-Owner-Id`.
- **3-Tier Live Health Monitor:** Calls `/api/v1/health` with 8s timeout (for Render JVM cold starts) and 1-tap presets for **Render Cloud** (`https://project-apex-backend.onrender.com`) and **Simulator Localhost** (`http://localhost:8080`).

## Building & Running on macOS / Xcode

### Option A: Using XcodeGen (Recommended for `.xcodeproj`)
```bash
cd ios
brew install xcodegen
xcodegen generate
open ProjectApex.xcodeproj
```
Select an iOS 16+ Simulator (e.g., iPhone 15) and press **Run (`Cmd+R`)** or **Test (`Cmd+U`)**.

### Option B: Running Unit Tests via Swift Package Manager
```bash
cd ios
swift test
```

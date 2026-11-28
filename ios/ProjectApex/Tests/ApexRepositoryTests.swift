import XCTest
@testable import ProjectApex

final class ApexRepositoryTests: XCTestCase {

    @MainActor
    func testDynamicSlotsGeneratedWithoutPregeneration() {
        let repository = ApexRepository(checkOnInit: false)
        XCTAssertFalse(repository.venues.isEmpty)
        XCTAssertFalse(repository.courts.isEmpty)

        let court = repository.courts[0]
        let tomorrowDate = Calendar.current.date(byAdding: .day, value: 1, to: Date()) ?? Date()
        let tomorrowStr = ApexRepository.dateFormatter.string(from: tomorrowDate)

        let slots = repository.getDynamicSlotsForDate(courtId: court.id, dateStr: tomorrowStr)
        XCTAssertEqual(slots.count, 17)
        XCTAssertEqual(slots.first?.startTime, "06:00")
        XCTAssertEqual(slots.last?.endTime, "23:00")
        XCTAssertTrue(slots.allSatisfy { $0.finalPricePaise > 0 })
    }

    @MainActor
    func testHoldSessionCreationAndExpiry() {
        let repository = ApexRepository(checkOnInit: false)
        let venue = repository.venues[0]
        let court = repository.courts[0]
        let tomorrowDate = Calendar.current.date(byAdding: .day, value: 1, to: Date()) ?? Date()
        let tomorrowStr = ApexRepository.dateFormatter.string(from: tomorrowDate)

        let slots = repository.getDynamicSlotsForDate(courtId: court.id, dateStr: tomorrowStr)
        let selected = [slots[0], slots[1]]

        let hold = repository.createHoldSession(venue: venue, court: court, dateStr: tomorrowStr, selectedSlots: selected)
        XCTAssertEqual(hold.slots.count, 2)
        XCTAssertTrue((590...600).contains(hold.remainingSeconds()))
        XCTAssertEqual(repository.activeHold, hold)

        let updatedSlots = repository.getDynamicSlotsForDate(courtId: court.id, dateStr: tomorrowStr)
        XCTAssertEqual(updatedSlots[0].status, .HELD)
        XCTAssertEqual(updatedSlots[1].status, .HELD)

        repository.releaseActiveHold()
        XCTAssertNil(repository.activeHold)

        let releasedSlots = repository.getDynamicSlotsForDate(courtId: court.id, dateStr: tomorrowStr)
        XCTAssertEqual(releasedSlots[0].status, .AVAILABLE)
    }

    @MainActor
    func testRazorpayPaisePricingAndPromoCodes() {
        let repository = ApexRepository(checkOnInit: false)
        let venue = repository.venues[0]
        let court = repository.courts[0]
        let tomorrowDate = Calendar.current.date(byAdding: .day, value: 1, to: Date()) ?? Date()
        let tomorrowStr = ApexRepository.dateFormatter.string(from: tomorrowDate)

        let slots = repository.getDynamicSlotsForDate(courtId: court.id, dateStr: tomorrowStr)
        let hold = repository.createHoldSession(venue: venue, court: court, dateStr: tomorrowStr, selectedSlots: [slots[0]])

        let quoteStandard = repository.calculateQuote(hold: hold, promoCode: nil)
        XCTAssertTrue(quoteStandard.totalPayablePaise > 0)
        XCTAssertEqual(quoteStandard.promoDiscountPaise, 0)
        let expectedFee = ((quoteStandard.baseCourtPaise - quoteStandard.offPeakDiscountPaise + quoteStandard.peakSurgePaise) * 2) / 100
        XCTAssertEqual(quoteStandard.convenienceFeePaise, expectedFee)

        let quoteWelcome = repository.calculateQuote(hold: hold, promoCode: "WELCOME50")
        XCTAssertEqual(quoteWelcome.promoDiscountPaise, 5_000)
        XCTAssertTrue(quoteWelcome.totalPayablePaise < quoteStandard.totalPayablePaise)

        let quoteApex20 = repository.calculateQuote(hold: hold, promoCode: "APEX20")
        XCTAssertTrue(quoteApex20.promoDiscountPaise > 0)
    }

    @MainActor
    func testBookingConfirmationAndMatchPass() {
        let repository = ApexRepository(checkOnInit: false)
        let venue = repository.venues[0]
        let court = repository.courts[0]
        let tomorrowDate = Calendar.current.date(byAdding: .day, value: 1, to: Date()) ?? Date()
        let tomorrowStr = ApexRepository.dateFormatter.string(from: tomorrowDate)

        let slots = repository.getDynamicSlotsForDate(courtId: court.id, dateStr: tomorrowStr)
        let hold = repository.createHoldSession(venue: venue, court: court, dateStr: tomorrowStr, selectedSlots: [slots[0]])
        let quote = repository.calculateQuote(hold: hold, promoCode: "WELCOME50")

        let pass = repository.confirmBooking(hold: hold, quote: quote)
        XCTAssertTrue(pass.bookingCode.hasPrefix("ARENA-"))
        XCTAssertEqual(pass.status, .CONFIRMED)
        XCTAssertEqual(pass.amountPaidPaise, quote.totalPayablePaise)

        let slotsAfterBooking = repository.getDynamicSlotsForDate(courtId: court.id, dateStr: tomorrowStr)
        XCTAssertEqual(slotsAfterBooking[0].status, .BOOKED)
    }

    @MainActor
    func testGateQrVerification() {
        let repository = ApexRepository(checkOnInit: false)
        let venue = repository.venues[0]
        let court = repository.courts[0]
        let tomorrowDate = Calendar.current.date(byAdding: .day, value: 1, to: Date()) ?? Date()
        let tomorrowStr = ApexRepository.dateFormatter.string(from: tomorrowDate)

        let slots = repository.getDynamicSlotsForDate(courtId: court.id, dateStr: tomorrowStr)
        let hold = repository.createHoldSession(venue: venue, court: court, dateStr: tomorrowStr, selectedSlots: [slots[0]])
        let quote = repository.calculateQuote(hold: hold, promoCode: nil)
        let pass = repository.confirmBooking(hold: hold, quote: quote)

        let (firstOk, firstMsg) = repository.verifyGateQr(bookingCode: pass.bookingCode)
        XCTAssertTrue(firstOk)
        XCTAssertTrue(firstMsg.contains("Verified"))

        let (secondOk, secondMsg) = repository.verifyGateQr(bookingCode: pass.bookingCode)
        XCTAssertFalse(secondOk)
        XCTAssertTrue(secondMsg.contains("Already Checked-In"))
    }

    @MainActor
    func testOwnerAddCourt() {
        let repository = ApexRepository(checkOnInit: false)
        let initialCount = repository.courts.count

        let newCourt = repository.addCourt(
            venueId: "ven_thane_apex",
            name: "Test Pickleball Arena",
            sportType: .PICKLEBALL,
            capacity: 4,
            surfaceType: "Acrylic Hard",
            isIndoor: true,
            dimensions: "44 x 20 ft",
            basePricePaise: 60_000,
            peakPricePaise: 90_000
        )

        XCTAssertEqual(repository.courts.count, initialCount + 1)
        XCTAssertEqual(newCourt.sportType, .PICKLEBALL)
    }

    func testApiClientUrlNormalizationAndHealthDecoding() throws {
        XCTAssertEqual(
            ApexApiClient.normalizeBaseUrl("https://project-apex-backend-fqe3.onrender.com/api/v1/"),
            "https://project-apex-backend-fqe3.onrender.com"
        )
        XCTAssertEqual(
            ApexApiClient.normalizeBaseUrl("  http://localhost:8080/API/V1/// "),
            "http://localhost:8080"
        )
        XCTAssertEqual(ApexApiClient.normalizeBaseUrl(nil), "")

        let client = ApexApiClient()
        XCTAssertEqual(client.baseUrl, ApexApiClient.renderProdURL)

        let healthJson = """
        {
          "success": true,
          "message": "Project Apex Java Middleware is healthy and operational",
          "data": {
            "status": "UP",
            "service": "project-apex-backend",
            "hosting": "Render",
            "runtime": "Java 21 LTS + Spring Boot 3.2.x",
            "database": "Neon PostgreSQL",
            "dbConnected": true,
            "usersCount": 14,
            "venuesCount": 4,
            "bookingsCount": 29,
            "timestamp": 1764317121000
          }
        }
        """
        let health = try client.parseHealthJson(Data(healthJson.utf8), url: ApexApiClient.renderProdURL, latencyMs: 120)
        XCTAssertTrue(health.isOnline)
        XCTAssertEqual(health.database, "Neon PostgreSQL")
        XCTAssertTrue(health.dbConnected)
        XCTAssertEqual(health.usersCount, 14)
        XCTAssertEqual(health.venuesCount, 4)
        XCTAssertEqual(health.bookingsCount, 29)
        XCTAssertEqual(health.latencyMs, 120)
    }

    func testApiClientJwtAuthDecodingAndLogout() async throws {
        let authJson = """
        {
          "success": true,
          "message": "Authentication successful",
          "data": {
            "token": "eyJhbGciOiJIUzI1NiJ9.ios-test.signature",
            "user": {
              "uid": "demo_owner",
              "email": "santosh@apexarena.com",
              "fullName": "Santosh Kadam",
              "phone": "+91 98190 54321",
              "role": "ROLE_OWNER",
              "loyaltyPoints": 420
            }
          }
        }
        """
        let client = ApexApiClient()
        let result = try client.parseAndStoreAuth(Data(authJson.utf8))
        XCTAssertEqual(result.token, "eyJhbGciOiJIUzI1NiJ9.ios-test.signature")
        XCTAssertEqual(client.authToken, "eyJhbGciOiJIUzI1NiJ9.ios-test.signature")
        XCTAssertEqual(client.activeUserId, "demo_owner")
        XCTAssertEqual(result.user.role, .ROLE_OWNER)
        XCTAssertEqual(result.user.jwtToken, result.token)

        await client.logout()
        XCTAssertNil(client.authToken)
        XCTAssertNil(client.activeUserId)
    }
}

import Foundation

@MainActor
public final class ApexRepository: ObservableObject {
    public let apiClient: ApexApiClient

    @Published public var currentUser: UserSession
    @Published public var selectedCity: String = "Thane"
    @Published public var selectedSport: SportType? = nil
    @Published public var backendUrl: String
    @Published public var isBackendConnected: Bool = false
    @Published public var healthStatus: HealthStatus? = nil
    @Published public var activeHold: HoldSession? = nil
    @Published public var bookingPasses: [BookingPass] = []
    @Published public var venues: [Venue] = []
    @Published public var courts: [Court] = []

    private var lockedSlots: [String: Int64] = [:]
    private var bookedSlots: [String: String] = [:]

    public init(apiClient: ApexApiClient = ApexApiClient(), checkOnInit: Bool = true) {
        self.apiClient = apiClient
        self.backendUrl = apiClient.baseUrl
        self.currentUser = UserSession(
            uid: "guest_001",
            name: "Guest Player",
            email: "guest@projectapex.in",
            phone: "+91 98200 12345",
            role: .ROLE_PLAYER,
            loyaltyPoints: 50,
            isGuest: true,
            jwtToken: nil
        )
        initializeInitialData()

        if checkOnInit {
            Task {
                await checkBackendConnectivityAndAwait()
            }
        }
    }

    private func initializeInitialData() {
        venues = [
            Venue(
                id: "ven_thane_apex",
                name: "Apex Super Arena & Turf",
                area: "Ghodbunder Road",
                city: "Thane",
                rating: 4.9,
                reviewCount: 384,
                distanceKm: 1.8,
                startingPricePaise: 79_900,
                supportedSports: [.CRICKET_TURF, .FOOTBALL_TURF, .BADMINTON, .PICKLEBALL],
                address: "Near Ovala Ground, Ghodbunder Rd, Thane West 400615",
                amenities: ["FIFA Grade Turf", "LED Floodlights", "BMS Sound System", "Covered Parking", "Locker & Showers"],
                badge: "⚡ FAST-FILLING"
            ),
            Venue(
                id: "ven_thane_hiranandani",
                name: "Hiranandani Sports Hub",
                area: "Hiranandani Estate",
                city: "Thane",
                rating: 4.8,
                reviewCount: 210,
                distanceKm: 3.2,
                startingPricePaise: 89_900,
                supportedSports: [.BADMINTON, .LAWN_TENNIS, .SQUASH, .POOL_TABLE],
                address: "The Walk, Hiranandani Estate, Thane West 400607",
                amenities: ["BWF Approved Wooden Courts", "AC Squash Arenas", "Pro Shop", "Cafe & Smoothies"],
                badge: "⭐ TOP RATED"
            ),
            Venue(
                id: "ven_mumbai_bkc",
                name: "BKC Pro Sports Complex",
                area: "Bandra Kurla Complex",
                city: "Mumbai",
                rating: 4.9,
                reviewCount: 512,
                distanceKm: 14.5,
                startingPricePaise: 149_900,
                supportedSports: [.CRICKET_TURF, .FOOTBALL_TURF, .PICKLEBALL, .MULTI_SPORT],
                address: "G Block, BKC, Bandra East, Mumbai 400051",
                amenities: ["7v7 Pro Arena", "Tournament Lighting", "Live Stream Cameras", "Valet Parking"],
                badge: "🏆 PREMIER"
            ),
            Venue(
                id: "ven_navi_vashi",
                name: "Vashi Sky Turf & Badminton",
                area: "Sector 17, Vashi",
                city: "Navi Mumbai",
                rating: 4.7,
                reviewCount: 175,
                distanceKm: 18.0,
                startingPricePaise: 69_900,
                supportedSports: [.FOOTBALL_TURF, .BADMINTON, .POOL_TABLE],
                address: "Rooftop, Inorbit Mall Area, Vashi, Navi Mumbai 400703",
                amenities: ["Rooftop Skyline View", "Dual Turf", "Chilled Dugouts", "Steam & Showers"],
                badge: "🔥 POPULAR"
            )
        ]

        courts = [
            Court(
                id: "crt_box_cricket_1",
                venueId: "ven_thane_apex",
                name: "Pitch 1: Box Cricket Arena",
                sportType: .CRICKET_TURF,
                capacity: 14,
                surfaceType: "Monofilament 50mm AstroTurf",
                isIndoor: false,
                dimensions: "110 x 55 ft",
                baseHourlyPricePaise: 80_000,
                peakHourlyPricePaise: 120_000
            ),
            Court(
                id: "crt_football_1",
                venueId: "ven_thane_apex",
                name: "Turf A: 5-a-Side Football",
                sportType: .FOOTBALL_TURF,
                capacity: 10,
                surfaceType: "FIFA Quality Pro Synthetic Turf",
                isIndoor: false,
                dimensions: "100 x 60 ft",
                baseHourlyPricePaise: 100_000,
                peakHourlyPricePaise: 150_000
            ),
            Court(
                id: "crt_badminton_1",
                venueId: "ven_thane_apex",
                name: "Court 1: BWF Vinyl Badminton",
                sportType: .BADMINTON,
                capacity: 4,
                surfaceType: "BWF Approved Shock Absorption Vinyl",
                isIndoor: true,
                dimensions: "44 x 20 ft",
                baseHourlyPricePaise: 45_000,
                peakHourlyPricePaise: 70_000
            ),
            Court(
                id: "crt_pickleball_1",
                venueId: "ven_thane_apex",
                name: "Pickle Court 1: Speed Play",
                sportType: .PICKLEBALL,
                capacity: 4,
                surfaceType: "Textured Acrylic Pro Surface",
                isIndoor: false,
                dimensions: "44 x 20 ft",
                baseHourlyPricePaise: 50_000,
                peakHourlyPricePaise: 75_000
            ),
            Court(
                id: "crt_tennis_hiranandani",
                venueId: "ven_thane_hiranandani",
                name: "Centre Court: Lawn Tennis",
                sportType: .LAWN_TENNIS,
                capacity: 4,
                surfaceType: "DecoTurf Multi-Layer Hard Court",
                isIndoor: false,
                dimensions: "78 x 36 ft",
                baseHourlyPricePaise: 90_000,
                peakHourlyPricePaise: 130_000
            ),
            Court(
                id: "crt_squash_hiranandani",
                venueId: "ven_thane_hiranandani",
                name: "Squash Arena Alpha (AC)",
                sportType: .SQUASH,
                capacity: 2,
                surfaceType: "Armourcoat Plaster & Maple Wood Floor",
                isIndoor: true,
                dimensions: "32 x 21 ft",
                baseHourlyPricePaise: 70_000,
                peakHourlyPricePaise: 95_000
            )
        ]

        let todayStr = Self.dateFormatter.string(from: Date())
        let nowMs = Int64(Date().timeIntervalSince1970 * 1000)
        let samplePass = BookingPass(
            id: "bk_init_001",
            bookingCode: "ARENA-7391K",
            venueName: "Apex Super Arena & Turf",
            courtName: "Pitch 1: Box Cricket Arena",
            sportType: .CRICKET_TURF,
            date: todayStr,
            timeSlotRange: "19:00 - 20:00",
            amountPaidPaise: 120_000,
            status: .CONFIRMED,
            createdAtMs: nowMs - 3_600_000
        )
        bookingPasses = [samplePass]
        bookedSlots["crt_box_cricket_1_\(todayStr)_19:00"] = samplePass.id
    }

    public static let dateFormatter: DateFormatter = {
        let df = DateFormatter()
        df.dateFormat = "yyyy-MM-dd"
        df.locale = Locale(identifier: "en_US_POSIX")
        return df
    }()

    public static let dateTimeFormatter: DateFormatter = {
        let df = DateFormatter()
        df.dateFormat = "yyyy-MM-dd HH:mm"
        df.locale = Locale(identifier: "en_US_POSIX")
        return df
    }()

    // MARK: - Filters & Connectivity

    public func selectCity(_ city: String) {
        selectedCity = city
    }

    public func selectSport(_ sport: SportType?) {
        selectedSport = sport
    }

    @discardableResult
    public func checkBackendConnectivityAndAwait(customUrl: String? = nil) async -> HealthStatus? {
        let rawCandidate = customUrl ?? backendUrl
        let normalized = ApexApiClient.normalizeBaseUrl(rawCandidate)
        let urlToTest = normalized.isEmpty ? ApexApiClient.renderProdURL : normalized

        if customUrl != nil && !normalized.isEmpty {
            backendUrl = normalized
            apiClient.baseUrl = normalized
        }

        if let health = await apiClient.checkHealth(customUrl: urlToTest), health.isOnline {
            healthStatus = health
            isBackendConnected = true
            backendUrl = urlToTest
            apiClient.baseUrl = urlToTest
            return health
        }

        let pingOk = await apiClient.pingBackend(customUrl: urlToTest)
        isBackendConnected = pingOk
        let isLocal = urlToTest.contains("localhost") || urlToTest.contains("127.0.0.1")
        let fallback = HealthStatus(
            status: pingOk ? "UP" : "STANDBY",
            service: "project-apex-backend",
            hosting: isLocal ? "Localhost Dev" : "Render",
            runtime: "Java 21 LTS + Spring Boot 3.2.x",
            database: "Neon PostgreSQL",
            dbConnected: pingOk,
            url: urlToTest,
            isOnline: pingOk
        )
        healthStatus = fallback
        return fallback
    }

    public func syncWithBackend() async {
        _ = await syncVenuesFromBackend()
        if let token = apiClient.authToken, !token.isEmpty {
            _ = await syncBookingsFromBackend()
        }
    }

    @discardableResult
    public func syncVenuesFromBackend() async -> Bool {
        guard let remoteVenues = try? await apiClient.fetchVenues(), !remoteVenues.isEmpty else {
            return false
        }
        var collectedCourts: [Court] = []
        for venue in remoteVenues {
            if let (_, resources) = try? await apiClient.fetchVenueDetail(venueId: venue.id) {
                collectedCourts.append(contentsOf: resources)
            }
        }
        venues = remoteVenues
        if !collectedCourts.isEmpty {
            courts = collectedCourts
        }
        return true
    }

    @discardableResult
    public func syncBookingsFromBackend() async -> Bool {
        guard let token = apiClient.authToken, !token.isEmpty else { return false }
        guard let remote = try? await apiClient.fetchMyBookings() else { return false }
        bookingPasses = remote
        return true
    }

    // MARK: - Dynamic 17-Slot Schedule Engine (Zero Pre-Generation)

    public func loadSlotsForDate(courtId: String, dateStr: String) async -> [TimeSlot] {
        if isBackendConnected, let remote = try? await apiClient.fetchAvailability(resourceId: courtId, date: dateStr), !remote.isEmpty {
            return remote
        }
        return getDynamicSlotsForDate(courtId: courtId, dateStr: dateStr)
    }

    public func getDynamicSlotsForDate(courtId: String, dateStr: String) -> [TimeSlot] {
        guard let court = courts.first(where: { $0.id == courtId }) else { return [] }

        let targetDate = Self.dateFormatter.date(from: dateStr) ?? Date()
        let cal = Calendar.current
        let weekday = cal.component(.weekday, from: targetDate)
        let isWeekend = (weekday == 1 || weekday == 7) // Sunday=1, Saturday=7

        let todayStr = Self.dateFormatter.string(from: Date())
        let isToday = (todayStr == dateStr)
        let currentHour = cal.component(.hour, from: Date())
        let nowMs = Int64(Date().timeIntervalSince1970 * 1000)

        var slots: [TimeSlot] = []
        for hour in 6...22 {
            let startStr = String(format: "%02d:00", hour)
            let endStr = String(format: "%02d:00", hour + 1)
            let slotId = "\(courtId)_\(dateStr)_\(startStr)"

            let isOffPeak = !isWeekend && (hour < 17)
            let isPeak = isWeekend || (hour >= 18)

            let basePaise = court.baseHourlyPricePaise
            let finalPaise: Int64
            if isOffPeak {
                finalPaise = (basePaise * 80) / 100
            } else if isPeak {
                finalPaise = court.peakHourlyPricePaise
            } else {
                finalPaise = basePaise
            }

            let status: SlotStatus
            if isToday && hour <= currentHour {
                status = .PAST
            } else if bookedSlots[slotId] != nil {
                status = .BOOKED
            } else if let expiry = lockedSlots[slotId], expiry > nowMs {
                status = .HELD
            } else {
                status = .AVAILABLE
            }

            slots.append(
                TimeSlot(
                    id: slotId,
                    courtId: courtId,
                    date: dateStr,
                    startTime: startStr,
                    endTime: endStr,
                    basePricePaise: basePaise,
                    finalPricePaise: finalPaise,
                    status: status,
                    isOffPeak: isOffPeak,
                    isPeak: isPeak
                )
            )
        }
        return slots
    }

    // MARK: - 10-Minute Hold Session

    @discardableResult
    public func createHoldSession(
        venue: Venue,
        court: Court,
        dateStr: String,
        selectedSlots: [TimeSlot]
    ) -> HoldSession {
        let holdId = "hold_" + UUID().uuidString.prefix(8).lowercased()
        let expiry = Int64(Date().timeIntervalSince1970 * 1000) + (10 * 60 * 1000)
        var total: Int64 = 0
        for s in selectedSlots {
            lockedSlots[s.id] = expiry
            total += s.finalPricePaise
        }

        let session = HoldSession(
            holdId: holdId,
            venueId: venue.id,
            venueName: venue.name,
            courtId: court.id,
            courtName: court.name,
            sportType: court.sportType,
            date: dateStr,
            slots: selectedSlots,
            expiresAtTimestampMs: expiry,
            totalPaise: total
        )
        activeHold = session
        return session
    }

    public func releaseActiveHold() {
        guard let hold = activeHold else { return }
        for s in hold.slots {
            lockedSlots.removeValue(forKey: s.id)
        }
        activeHold = nil
    }

    // MARK: - Pricing & Promo Engine (Integer Paise)

    public func calculateQuote(hold: HoldSession, promoCode: String?) -> PriceBreakdown {
        var baseTotal: Int64 = 0
        var offPeakDiscount: Int64 = 0
        var peakSurge: Int64 = 0

        for s in hold.slots {
            baseTotal += s.basePricePaise
            if s.isOffPeak {
                offPeakDiscount += (s.basePricePaise - s.finalPricePaise)
            } else if s.isPeak {
                peakSurge += (s.finalPricePaise - s.basePricePaise)
            }
        }

        var promoDiscount: Int64 = 0
        var appliedPromo: String? = nil
        let normalizedCode = promoCode?.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()

        if normalizedCode == "WELCOME50" || normalizedCode == "THANE50" {
            promoDiscount = 5_000
            appliedPromo = "\(normalizedCode!) (-₹50)"
        } else if normalizedCode == "APEX20" || normalizedCode == "TURF20" {
            var calc = ((baseTotal + peakSurge - offPeakDiscount) * 20) / 100
            if calc > 15_000 { calc = 15_000 }
            promoDiscount = calc
            appliedPromo = "\(normalizedCode!) (20% off)"
        }

        let subtotal = max(10_000, baseTotal - offPeakDiscount + peakSurge - promoDiscount)
        let convenienceFee = (subtotal * 2) / 100
        let totalPayable = subtotal + convenienceFee

        return PriceBreakdown(
            baseCourtPaise: baseTotal,
            offPeakDiscountPaise: offPeakDiscount,
            peakSurgePaise: peakSurge,
            promoDiscountPaise: promoDiscount,
            convenienceFeePaise: convenienceFee,
            totalPayablePaise: totalPayable,
            promoCodeApplied: appliedPromo
        )
    }

    public func calculateQuoteWithBackend(hold: HoldSession, promoCode: String?) async -> PriceBreakdown {
        if isBackendConnected,
           let remote = try? await apiClient.calculatePricingQuote(
                venueId: hold.venueId,
                resourceId: hold.courtId,
                date: hold.date,
                slots: hold.slots.map { $0.startTime },
                promoCode: promoCode
           ) {
            return remote
        }
        return calculateQuote(hold: hold, promoCode: promoCode)
    }

    // MARK: - Booking Confirmation & 3-Tier Cancellation

    @discardableResult
    public func confirmBooking(hold: HoldSession, quote: PriceBreakdown) -> BookingPass {
        let digits = Int.random(in: 1000...9999)
        let letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        let letter = letters.randomElement() ?? "X"
        let bookingCode = "ARENA-\(digits)\(letter)"

        let timeRange: String
        if hold.slots.count == 1, let first = hold.slots.first {
            timeRange = first.formattedTime
        } else if let first = hold.slots.first, let last = hold.slots.last {
            timeRange = "\(first.startTime) - \(last.endTime)"
        } else {
            timeRange = "18:00 - 19:00"
        }

        let pass = BookingPass(
            id: "bk_" + UUID().uuidString.prefix(10).lowercased(),
            bookingCode: bookingCode,
            venueName: hold.venueName,
            courtName: hold.courtName,
            sportType: hold.sportType,
            date: hold.date,
            timeSlotRange: timeRange,
            amountPaidPaise: quote.totalPayablePaise,
            status: .CONFIRMED,
            createdAtMs: Int64(Date().timeIntervalSince1970 * 1000)
        )

        for s in hold.slots {
            lockedSlots.removeValue(forKey: s.id)
            bookedSlots[s.id] = pass.id
        }

        bookingPasses = [pass] + bookingPasses
        activeHold = nil
        return pass
    }

    public func confirmBookingWithBackend(
        hold: HoldSession,
        quote: PriceBreakdown,
        promoCode: String? = nil
    ) async -> BookingPass {
        if isBackendConnected, let token = apiClient.authToken, !token.isEmpty {
            if let initiation = try? await apiClient.initiateBooking(
                venueId: hold.venueId,
                resourceId: hold.courtId,
                bookingDate: hold.date,
                slotStartTimes: hold.slots.map { $0.startTime },
                sportType: hold.sportType.rawValue,
                promoCode: promoCode
            ), !initiation.bookingId.isEmpty {
                let paymentId = "pay_ios_" + UUID().uuidString.replacingOccurrences(of: "-", with: "").prefix(10).lowercased()
                if let verified = try? await apiClient.verifyBookingPayment(
                    bookingId: initiation.bookingId,
                    razorpayOrderId: initiation.razorpayOrderId,
                    razorpayPaymentId: paymentId,
                    razorpaySignature: "SIMULATED_TEST_SIGNATURE"
                ) {
                    for s in hold.slots {
                        lockedSlots.removeValue(forKey: s.id)
                        bookedSlots[s.id] = verified.id
                    }
                    bookingPasses = [verified] + bookingPasses.filter { $0.id != verified.id }
                    activeHold = nil
                    return verified
                }
            }
        }
        return confirmBooking(hold: hold, quote: quote)
    }

    @discardableResult
    public func cancelBooking(passId: String) -> (Bool, String) {
        guard let pass = bookingPasses.first(where: { $0.id == passId }) else {
            return (false, "Pass not found")
        }
        if pass.status != .CONFIRMED {
            return (false, "Booking is already \(pass.status.rawValue)")
        }

        let slotStart = pass.timeSlotRange.components(separatedBy: "-").first?.trimmingCharacters(in: .whitespaces) ?? "06:00"
        let matchDate = Self.dateTimeFormatter.date(from: "\(pass.date) \(slotStart)") ?? Date()
        let diffHours = Int64(matchDate.timeIntervalSinceNow / 3600)

        let refundPaise: Int64
        let tierDesc: String

        if diffHours >= 24 {
            refundPaise = max(0, pass.amountPaidPaise - 5_000)
            tierDesc = ">24 hrs before match: 100% refund (minus ₹50 processing fee)"
        } else if (6...23).contains(diffHours) {
            refundPaise = pass.amountPaidPaise / 2
            tierDesc = "6-24 hrs before match: 50% refund"
        } else {
            refundPaise = 0
            tierDesc = "<6 hrs before match: Non-refundable tier"
        }

        let updated = BookingPass(
            id: pass.id,
            bookingCode: pass.bookingCode,
            venueName: pass.venueName,
            courtName: pass.courtName,
            sportType: pass.sportType,
            date: pass.date,
            timeSlotRange: pass.timeSlotRange,
            amountPaidPaise: pass.amountPaidPaise,
            status: .CANCELLED,
            createdAtMs: pass.createdAtMs,
            refundAmountPaise: refundPaise,
            cancellationTier: tierDesc,
            qrPassToken: pass.qrPassToken
        )

        bookingPasses = bookingPasses.map { $0.id == passId ? updated : $0 }
        for (key, value) in bookedSlots where value == passId {
            bookedSlots.removeValue(forKey: key)
        }

        let refundRs = refundPaise / 100
        return (true, "Cancelled successfully. Refund ₹\(refundRs) initiated via original payment method (\(tierDesc)).")
    }

    public func cancelBookingWithBackend(
        passId: String,
        reason: String = "Player requested cancellation"
    ) async -> (Bool, String) {
        if isBackendConnected, let token = apiClient.authToken, !token.isEmpty {
            if let cancelled = try? await apiClient.cancelBooking(bookingId: passId, reason: reason) {
                bookingPasses = bookingPasses.map { $0.id == passId ? cancelled : $0 }
                for (key, value) in bookedSlots where value == passId {
                    bookedSlots.removeValue(forKey: key)
                }
                return (true, "Cancelled on server. Refund ₹\(cancelled.refundAmountRupees) processed.")
            }
        }
        return cancelBooking(passId: passId)
    }

    // MARK: - Owner Portal & Gate Scanner

    @discardableResult
    public func addCourt(
        venueId: String,
        name: String,
        sportType: SportType,
        capacity: Int,
        surfaceType: String,
        isIndoor: Bool,
        dimensions: String,
        basePricePaise: Int64,
        peakPricePaise: Int64
    ) -> Court {
        let court = Court(
            id: "crt_cust_" + UUID().uuidString.prefix(6).lowercased(),
            venueId: venueId,
            name: name,
            sportType: sportType,
            capacity: capacity,
            surfaceType: surfaceType,
            isIndoor: isIndoor,
            dimensions: dimensions,
            baseHourlyPricePaise: basePricePaise,
            peakHourlyPricePaise: peakPricePaise
        )
        courts.append(court)
        return court
    }

    @discardableResult
    public func addCourtWithBackend(
        venueId: String,
        name: String,
        sportType: SportType,
        capacity: Int,
        surfaceType: String,
        isIndoor: Bool,
        dimensions: String,
        basePricePaise: Int64,
        peakPricePaise: Int64
    ) async -> Court {
        if isBackendConnected, let token = apiClient.authToken, !token.isEmpty {
            if let remote = try? await apiClient.createFacility(
                venueId: venueId,
                name: name,
                sport: sportType.rawValue.lowercased(),
                capacity: capacity,
                surface: surfaceType,
                dimensions: dimensions,
                baseHourlyRateRupees: max(200, Int(basePricePaise / 100)),
                peakHourlyRateRupees: max(200, Int(peakPricePaise / 100)),
                hasFloodlights: !isIndoor
            ) {
                courts.append(remote)
                return remote
            }
        }
        return addCourt(
            venueId: venueId,
            name: name,
            sportType: sportType,
            capacity: capacity,
            surfaceType: surfaceType,
            isIndoor: isIndoor,
            dimensions: dimensions,
            basePricePaise: basePricePaise,
            peakPricePaise: peakPricePaise
        )
    }

    @discardableResult
    public func verifyGateQr(bookingCode: String) -> (Bool, String) {
        let trimmed = bookingCode.trimmingCharacters(in: .whitespacesAndNewlines)
        guard let pass = bookingPasses.first(where: {
            $0.bookingCode.caseInsensitiveCompare(trimmed) == .orderedSame ||
            ($0.qrPassToken?.caseInsensitiveCompare(trimmed) == .orderedSame)
        }) else {
            return (false, "Invalid QR code: Pass not found in system")
        }

        if pass.status == .CHECKED_IN {
            return (false, "Already Checked-In at \(pass.venueName)")
        }
        if pass.status == .CANCELLED {
            return (false, "Entry Denied: Booking was cancelled")
        }

        let checkedIn = BookingPass(
            id: pass.id,
            bookingCode: pass.bookingCode,
            venueName: pass.venueName,
            courtName: pass.courtName,
            sportType: pass.sportType,
            date: pass.date,
            timeSlotRange: pass.timeSlotRange,
            amountPaidPaise: pass.amountPaidPaise,
            status: .CHECKED_IN,
            createdAtMs: pass.createdAtMs,
            refundAmountPaise: pass.refundAmountPaise,
            cancellationTier: pass.cancellationTier,
            qrPassToken: pass.qrPassToken
        )
        bookingPasses = bookingPasses.map { $0.id == pass.id ? checkedIn : $0 }
        return (true, "Verified! Welcome \(pass.courtName) (\(pass.timeSlotRange))")
    }

    public func verifyGateQrWithBackend(bookingCode: String) async -> (Bool, String) {
        if isBackendConnected, let token = apiClient.authToken, !token.isEmpty {
            if let gate = try? await apiClient.verifyGatePass(qrPassToken: bookingCode) {
                if let updated = gate.booking {
                    bookingPasses = bookingPasses.map {
                        ($0.id == updated.id || $0.bookingCode.caseInsensitiveCompare(updated.bookingCode) == .orderedSame) ? updated : $0
                    }
                }
                return (gate.valid, gate.message)
            }
        }
        return verifyGateQr(bookingCode: bookingCode)
    }

    // MARK: - Authentication

    @discardableResult
    public func loginWithPassword(emailOrPhone: String, password: String) -> Result<UserSession, Error> {
        let lower = emailOrPhone.lowercased()
        let role: UserRole = (lower.contains("owner") || lower.contains("santosh")) ? .ROLE_OWNER : .ROLE_PLAYER
        let user = UserSession(
            uid: "usr_" + UUID().uuidString.prefix(8).lowercased(),
            name: role == .ROLE_OWNER ? "Rajesh Patil (Owner)" : "Pushpendra Rait",
            email: emailOrPhone.contains("@") ? emailOrPhone : "pushpendra@projectapex.in",
            phone: emailOrPhone.contains("@") ? "+91 98200 88990" : emailOrPhone,
            role: role,
            loyaltyPoints: 280,
            isGuest: false
        )
        currentUser = user
        return .success(user)
    }

    public func authenticateWithBackend(identifier: String, password: String) async -> Result<UserSession, Error> {
        if isBackendConnected {
            do {
                let auth = try await apiClient.loginWithPassword(identifier: identifier, password: password)
                currentUser = auth.user
                _ = await syncBookingsFromBackend()
                return .success(auth.user)
            } catch let apiError as ApexApiError {
                if case .httpError(_, let msg) = apiError, msg.lowercased().contains("incorrect password") {
                    return .failure(apiError)
                }
            } catch {}
        }
        return loginWithPassword(emailOrPhone: identifier, password: password)
    }

    public func sendLoginOtpWithBackend(identifier: String) async -> Result<OtpDispatchResult, Error> {
        if isBackendConnected, let res = try? await apiClient.sendLoginOtp(identifier: identifier) {
            return .success(res)
        }
        let simOtp = String(Int.random(in: 100_000...999_999))
        return .success(
            OtpDispatchResult(
                sessionId: "local_login_" + UUID().uuidString.prefix(6),
                expiresInSeconds: 300,
                simulatedSmsOtp: simOtp
            )
        )
    }

    public func verifyLoginOtpWithBackend(sessionId: String?, identifier: String, otp: String) async -> Result<UserSession, Error> {
        if isBackendConnected, let sessionId, !sessionId.isEmpty, !sessionId.hasPrefix("local_") {
            do {
                let auth = try await apiClient.verifyLoginOtp(sessionId: sessionId, otp: otp)
                currentUser = auth.user
                _ = await syncBookingsFromBackend()
                return .success(auth.user)
            } catch let err {
                return .failure(err)
            }
        }
        return loginWithOtp(phone: identifier, otp: otp)
    }

    public func sendRegistrationOtpWithBackend(phone: String, email: String, displayName: String) async -> Result<OtpDispatchResult, Error> {
        if isBackendConnected, let res = try? await apiClient.sendRegistrationOtp(phone: phone, email: email, displayName: displayName) {
            return .success(res)
        }
        return .success(
            OtpDispatchResult(
                sessionId: "local_reg_" + UUID().uuidString.prefix(6),
                expiresInSeconds: 300,
                simulatedSmsOtp: String(Int.random(in: 100_000...999_999)),
                simulatedEmailOtp: String(Int.random(in: 100_000...999_999))
            )
        )
    }

    public func registerUserWithBackend(
        name: String,
        email: String,
        phone: String,
        password: String,
        role: UserRole,
        mobileOtp: String,
        emailOtp: String,
        sessionId: String? = nil
    ) async -> Result<UserSession, Error> {
        let trimmedName = name.trimmingCharacters(in: .whitespacesAndNewlines)
        let trimmedEmail = email.trimmingCharacters(in: .whitespacesAndNewlines)
        let trimmedPhone = phone.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmedName.isEmpty, !trimmedEmail.isEmpty, !trimmedPhone.isEmpty, password.count >= 6 else {
            return .failure(NSError(domain: "ApexAuth", code: 400, userInfo: [NSLocalizedDescriptionKey: "Please fill all mandatory fields (Password min 6 chars)"]))
        }
        guard mobileOtp.count >= 4, emailOtp.count >= 4 else {
            return .failure(NSError(domain: "ApexAuth", code: 400, userInfo: [NSLocalizedDescriptionKey: "Please enter verified Mobile and Email OTPs"]))
        }

        if isBackendConnected {
            do {
                let auth: AuthResult
                if let sessionId, !sessionId.isEmpty, !sessionId.hasPrefix("local_") {
                    auth = try await apiClient.verifyRegistrationOtp(
                        sessionId: sessionId,
                        phoneOtp: mobileOtp,
                        emailOtp: emailOtp,
                        displayName: trimmedName,
                        phone: trimmedPhone,
                        email: trimmedEmail,
                        role: role,
                        city: selectedCity,
                        password: password
                    )
                } else {
                    auth = try await apiClient.registerWithPassword(
                        displayName: trimmedName,
                        email: trimmedEmail,
                        phone: trimmedPhone,
                        password: password,
                        city: selectedCity,
                        role: role
                    )
                }
                currentUser = auth.user
                await syncWithBackend()
                return .success(auth.user)
            } catch {}
        }

        return registerUser(
            name: trimmedName,
            email: trimmedEmail,
            phone: trimmedPhone,
            password: password,
            role: role,
            mobileOtp: mobileOtp,
            emailOtp: emailOtp
        )
    }

    public func loginWithOtp(phone: String, otp: String) -> Result<UserSession, Error> {
        guard otp.count >= 4 else {
            return .failure(NSError(domain: "ApexAuth", code: 400, userInfo: [NSLocalizedDescriptionKey: "Please enter a valid 6-digit OTP"]))
        }
        let suffix = String(phone.suffix(4))
        let user = UserSession(
            uid: "usr_otp_" + UUID().uuidString.prefix(6).lowercased(),
            name: "Player \(suffix)",
            email: "player\(suffix)@projectapex.in",
            phone: phone,
            role: .ROLE_PLAYER,
            loyaltyPoints: 100,
            isGuest: false
        )
        currentUser = user
        return .success(user)
    }

    public func registerUser(
        name: String,
        email: String,
        phone: String,
        password: String,
        role: UserRole,
        mobileOtp: String,
        emailOtp: String
    ) -> Result<UserSession, Error> {
        guard !name.trimmingCharacters(in: .whitespaces).isEmpty,
              !email.trimmingCharacters(in: .whitespaces).isEmpty,
              !phone.trimmingCharacters(in: .whitespaces).isEmpty,
              password.count >= 6 else {
            return .failure(NSError(domain: "ApexAuth", code: 400, userInfo: [NSLocalizedDescriptionKey: "Please fill all mandatory fields (Password min 6 chars)"]))
        }
        guard mobileOtp.count >= 4, emailOtp.count >= 4 else {
            return .failure(NSError(domain: "ApexAuth", code: 400, userInfo: [NSLocalizedDescriptionKey: "Please enter verified Mobile and Email OTPs"]))
        }
        let user = UserSession(
            uid: "usr_reg_" + UUID().uuidString.prefix(6).lowercased(),
            name: name,
            email: email,
            phone: phone,
            role: role,
            loyaltyPoints: 150,
            isGuest: false
        )
        currentUser = user
        return .success(user)
    }

    public func logout() {
        apiClient.authToken = nil
        apiClient.activeUserId = nil
        currentUser = UserSession(
            uid: "guest_001",
            name: "Guest Player",
            email: "guest@projectapex.in",
            phone: "+91 98200 12345",
            role: .ROLE_PLAYER,
            loyaltyPoints: 0,
            isGuest: true,
            jwtToken: nil
        )
    }

    public func logoutWithBackend() async {
        await apiClient.logout()
        logout()
    }

    public func switchRole(_ newRole: UserRole) {
        currentUser = UserSession(
            uid: currentUser.uid,
            name: newRole == .ROLE_OWNER ? "Apex Arena Manager" : "Pushpendra Rait",
            email: currentUser.email,
            phone: currentUser.phone,
            role: newRole,
            loyaltyPoints: currentUser.loyaltyPoints,
            isGuest: currentUser.isGuest,
            jwtToken: currentUser.jwtToken
        )
    }
}

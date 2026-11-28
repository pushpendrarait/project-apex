import Foundation

public enum ApexApiError: LocalizedError {
    case invalidURL(String)
    case httpError(Int, String)
    case decodingError(String)

    public var errorDescription: String? {
        switch self {
        case .invalidURL(let url):
            return "Invalid backend URL: \(url)"
        case .httpError(let code, let message):
            return message.isEmpty ? "HTTP \(code) request failed" : message
        case .decodingError(let reason):
            return "Response decode error: \(reason)"
        }
    }
}

public final class ApexApiClient: @unchecked Sendable {
    public static let renderProdURL = "https://project-apex-backend-fqe3.onrender.com"
    public static let simulatorLocalURL = "http://localhost:8080"
    public static let defaultTimeoutSeconds: TimeInterval = 8.0

    public var baseUrl: String {
        didSet {
            let normalized = Self.normalizeBaseUrl(baseUrl)
            if !normalized.isEmpty && normalized != baseUrl {
                baseUrl = normalized
            }
        }
    }

    public var authToken: String?
    public var activeUserId: String?

    private let session: URLSession
    private let decoder = JSONDecoder()
    private let encoder = JSONEncoder()

    public init(baseUrl: String = ApexApiClient.renderProdURL, session: URLSession = .shared) {
        let normalized = Self.normalizeBaseUrl(baseUrl)
        self.baseUrl = normalized.isEmpty ? Self.renderProdURL : normalized
        self.session = session
    }

    /// Strips trailing slashes and any trailing `/api/v1` segment cleanly.
    public static func normalizeBaseUrl(_ url: String?) -> String {
        guard let url else { return "" }
        var cleaned = url.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !cleaned.isEmpty else { return "" }
        while cleaned.hasSuffix("/") {
            cleaned.removeLast()
        }
        if cleaned.lowercased().hasSuffix("/api/v1") {
            cleaned = String(cleaned.dropLast("/api/v1".count))
        }
        while cleaned.hasSuffix("/") {
            cleaned.removeLast()
        }
        return cleaned
    }

    private func endpointURL(_ path: String, baseOverride: String? = nil) throws -> URL {
        let root = Self.normalizeBaseUrl(baseOverride ?? baseUrl)
        let normalizedPath = path.hasPrefix("/") ? path : "/\(path)"
        guard !root.isEmpty, let url = URL(string: "\(root)\(normalizedPath)") else {
            throw ApexApiError.invalidURL("\(root)\(normalizedPath)")
        }
        return url
    }

    // MARK: - Health & Connectivity

    public func parseHealthJson(
        _ data: Data,
        url: String? = nil,
        latencyMs: Int64 = 0
    ) throws -> HealthStatus {
        let targetUrl = Self.normalizeBaseUrl(url ?? baseUrl)
        let isLocal = targetUrl.contains("localhost") || targetUrl.contains("127.0.0.1")

        let payload: BackendHealthPayload
        var wrapperMessage: String?
        var wrapperSuccess = true

        if let wrapped = try? decoder.decode(ApiResponse<BackendHealthPayload>.self, from: data),
           let inner = wrapped.data {
            payload = inner
            wrapperMessage = wrapped.message
            wrapperSuccess = wrapped.success
        } else {
            payload = try decoder.decode(BackendHealthPayload.self, from: data)
        }

        let status = payload.status ?? (wrapperSuccess ? "UP" : "DOWN")
        let isOnline = status.caseInsensitiveCompare("UP") == .orderedSame || wrapperSuccess

        return HealthStatus(
            status: status,
            service: payload.service ?? "project-apex-backend",
            hosting: payload.hosting ?? (isLocal ? "Localhost Dev (8080)" : "Render"),
            runtime: payload.runtime ?? "Java 21 LTS + Spring Boot 3.2.x",
            database: payload.database ?? "Neon PostgreSQL",
            dbConnected: payload.dbConnected ?? false,
            usersCount: payload.usersCount ?? 0,
            venuesCount: payload.venuesCount ?? 0,
            bookingsCount: payload.bookingsCount ?? 0,
            latencyMs: latencyMs,
            url: targetUrl,
            isOnline: isOnline,
            message: wrapperMessage,
            timestampMs: payload.timestamp ?? Int64(Date().timeIntervalSince1970 * 1000)
        )
    }

    public func checkHealth(customUrl: String? = nil) async -> HealthStatus? {
        let targetBase = Self.normalizeBaseUrl(customUrl ?? baseUrl)
        guard !targetBase.isEmpty, let url = try? endpointURL("/api/v1/health", baseOverride: targetBase) else {
            return nil
        }

        let start = Date()
        do {
            var request = URLRequest(url: url, timeoutInterval: Self.defaultTimeoutSeconds)
            request.httpMethod = "GET"
            applyHeaders(to: &request)

            let (data, response) = try await session.data(for: request)
            guard let http = response as? HTTPURLResponse, (200...299).contains(http.statusCode) else {
                return nil
            }
            let latencyMs = max(0, Int64(Date().timeIntervalSince(start) * 1000))
            return try parseHealthJson(data, url: targetBase, latencyMs: latencyMs)
        } catch {
            return nil
        }
    }

    public func pingBackend(customUrl: String? = nil) async -> Bool {
        let targetBase = Self.normalizeBaseUrl(customUrl ?? baseUrl)
        guard !targetBase.isEmpty else { return false }

        if let health = await checkHealth(customUrl: targetBase), health.isOnline {
            return true
        }

        guard let sportsUrl = try? endpointURL("/api/v1/sports", baseOverride: targetBase) else {
            return false
        }
        do {
            var request = URLRequest(url: sportsUrl, timeoutInterval: Self.defaultTimeoutSeconds)
            request.httpMethod = "GET"
            applyHeaders(to: &request)
            let (_, response) = try await session.data(for: request)
            guard let http = response as? HTTPURLResponse else { return false }
            return (200...299).contains(http.statusCode)
        } catch {
            return false
        }
    }

    // MARK: - Authentication APIs

    public func loginWithPassword(identifier: String, password: String) async throws -> AuthResult {
        let body = try encoder.encode(["identifier": identifier, "password": password])
        let data = try await sendRequest(path: "/api/v1/auth/login/password", method: "POST", body: body)
        return try parseAndStoreAuth(data)
    }

    public func registerWithPassword(
        displayName: String,
        email: String,
        phone: String,
        password: String,
        city: String = "Thane",
        role: UserRole = .ROLE_PLAYER
    ) async throws -> AuthResult {
        let backendRole = role == .ROLE_OWNER ? "venue_owner" : "player"
        let payload = RegisterPasswordRequestBody(
            displayName: displayName,
            email: email,
            phone: phone,
            password: password,
            city: city,
            role: backendRole
        )
        let data = try await sendRequest(path: "/api/v1/auth/register", method: "POST", body: try encoder.encode(payload))
        return try parseAndStoreAuth(data)
    }

    public func sendRegistrationOtp(phone: String, email: String, displayName: String) async throws -> OtpDispatchResult {
        let body = try encoder.encode([
            "phone": phone,
            "email": email,
            "displayName": displayName
        ])
        let data = try await sendRequest(path: "/api/v1/auth/registration/send-otp", method: "POST", body: body)
        let wrapper = try decodeWrapped(BackendOtpDispatchPayload.self, from: data)
        return OtpDispatchResult(
            sessionId: wrapper.sessionId ?? "",
            expiresInSeconds: wrapper.expiresInSeconds ?? 300,
            simulatedSmsOtp: wrapper.simulatedSmsOtp,
            simulatedEmailOtp: wrapper.simulatedEmailOtp
        )
    }

    public func verifyRegistrationOtp(
        sessionId: String,
        phoneOtp: String,
        emailOtp: String,
        displayName: String,
        phone: String,
        email: String,
        role: UserRole = .ROLE_PLAYER,
        city: String = "Thane",
        password: String? = nil
    ) async throws -> AuthResult {
        let backendRole = role == .ROLE_OWNER ? "venue_owner" : "player"
        let payload = VerifyRegistrationOtpRequestBody(
            sessionId: sessionId,
            phoneOtp: phoneOtp,
            emailOtp: emailOtp,
            role: backendRole,
            displayName: displayName,
            phone: phone,
            email: email,
            password: password,
            city: city
        )
        let data = try await sendRequest(
            path: "/api/v1/auth/registration/verify-otp",
            method: "POST",
            body: try encoder.encode(payload)
        )
        return try parseAndStoreAuth(data)
    }

    public func sendLoginOtp(identifier: String) async throws -> OtpDispatchResult {
        let body = try encoder.encode(["identifier": identifier])
        let data = try await sendRequest(path: "/api/v1/auth/login/send-otp", method: "POST", body: body)
        let wrapper = try decodeWrapped(BackendOtpDispatchPayload.self, from: data)
        return OtpDispatchResult(
            sessionId: wrapper.sessionId ?? "",
            expiresInSeconds: wrapper.expiresInSeconds ?? 300,
            simulatedSmsOtp: wrapper.simulatedSmsOtp,
            simulatedEmailOtp: wrapper.simulatedEmailOtp
        )
    }

    public func verifyLoginOtp(sessionId: String, otp: String) async throws -> AuthResult {
        let body = try encoder.encode(["sessionId": sessionId, "otp": otp])
        let data = try await sendRequest(path: "/api/v1/auth/login/verify-otp", method: "POST", body: body)
        return try parseAndStoreAuth(data)
    }

    public func logout() async {
        defer {
            authToken = nil
            activeUserId = nil
        }
        _ = try? await sendRequest(path: "/api/v1/auth/logout", method: "POST", body: Data("{}".utf8))
    }

    // MARK: - Venues, Availability & Pricing APIs

    public func fetchVenues(city: String? = nil, sportType: String? = nil) async throws -> [Venue] {
        var components = URLComponents()
        var items: [URLQueryItem] = []
        if let city, !city.isEmpty, city.caseInsensitiveCompare("all") != .orderedSame,
           city.caseInsensitiveCompare("All Cities") != .orderedSame {
            items.append(URLQueryItem(name: "city", value: city.lowercased()))
        }
        if let sportType, !sportType.isEmpty, sportType.caseInsensitiveCompare("all") != .orderedSame {
            items.append(URLQueryItem(name: "sportType", value: sportType))
        }
        components.queryItems = items.isEmpty ? nil : items
        let suffix = components.percentEncodedQuery.map { "?\($0)" } ?? ""
        let data = try await sendRequest(path: "/api/v1/venues\(suffix)", method: "GET")
        let dtos = try decodeWrapped([BackendVenueDTO].self, from: data)
        return dtos.map { $0.toDomain() }
    }

    public func fetchVenueDetail(venueId: String) async throws -> (Venue, [Court]) {
        let escaped = venueId.addingPercentEncoding(withAllowedCharacters: .urlPathAllowed) ?? venueId
        let data = try await sendRequest(path: "/api/v1/venues/\(escaped)", method: "GET")
        let detail = try decodeWrapped(BackendVenueDetailDTO.self, from: data)
        return (detail.venue.toDomain(), (detail.resources ?? []).map { $0.toCourt() })
    }

    public func fetchAvailability(resourceId: String, date: String) async throws -> [TimeSlot] {
        var comps = URLComponents()
        comps.queryItems = [
            URLQueryItem(name: "resourceId", value: resourceId),
            URLQueryItem(name: "date", value: date)
        ]
        let query = comps.percentEncodedQuery ?? ""
        let data = try await sendRequest(path: "/api/v1/availability?\(query)", method: "GET")
        let dtos = try decodeWrapped([BackendSlotDTO].self, from: data)
        return dtos.map { $0.toDomain(defaultResourceId: resourceId, defaultDate: date) }
    }

    public func calculatePricingQuote(
        venueId: String,
        resourceId: String,
        date: String,
        slots: [String],
        promoCode: String? = nil
    ) async throws -> PriceBreakdown {
        let req = PricingQuoteRequestBody(
            venueId: venueId,
            resourceId: resourceId,
            date: date,
            slots: slots,
            promoCode: promoCode
        )
        let data = try await sendRequest(path: "/api/v1/pricing/quote", method: "POST", body: try encoder.encode(req))
        let dto = try decodeWrapped(BackendPriceQuoteDTO.self, from: data)
        return dto.toDomain()
    }

    // MARK: - Bookings & Passes APIs

    public func initiateBooking(
        venueId: String,
        resourceId: String,
        bookingDate: String,
        slotStartTimes: [String],
        sportType: String? = nil,
        promoCode: String? = nil
    ) async throws -> BookingInitiationResult {
        let req = InitiateBookingRequestBody(
            venueId: venueId,
            resourceId: resourceId,
            bookingDate: bookingDate,
            slotStartTimes: slotStartTimes,
            sportType: sportType,
            promoCode: promoCode
        )
        let data = try await sendRequest(path: "/api/v1/bookings/initiate", method: "POST", body: try encoder.encode(req))
        let dto = try decodeWrapped(BackendInitiateBookingDTO.self, from: data)
        let quote = dto.quote?.toDomain()
        return BookingInitiationResult(
            bookingId: dto.bookingId ?? "",
            razorpayOrderId: dto.razorpayOrderId ?? "",
            amountPaise: dto.amountPaise ?? quote?.totalPayablePaise ?? 0,
            currency: dto.currency ?? "INR",
            razorpayKeyId: dto.razorpayKeyId,
            holdExpiresAtMs: dto.holdExpiresAt ?? Int64(Date().timeIntervalSince1970 * 1000) + 600_000,
            quote: quote
        )
    }

    public func verifyBookingPayment(
        bookingId: String,
        razorpayOrderId: String,
        razorpayPaymentId: String,
        razorpaySignature: String
    ) async throws -> BookingPass {
        let escaped = bookingId.addingPercentEncoding(withAllowedCharacters: .urlPathAllowed) ?? bookingId
        let body = try encoder.encode([
            "razorpayOrderId": razorpayOrderId,
            "razorpayPaymentId": razorpayPaymentId,
            "razorpaySignature": razorpaySignature
        ])
        let data = try await sendRequest(path: "/api/v1/bookings/\(escaped)/verify-payment", method: "POST", body: body)
        return try decodeWrapped(BackendBookingDTO.self, from: data).toDomain()
    }

    public func fetchMyBookings() async throws -> [BookingPass] {
        let data = try await sendRequest(path: "/api/v1/bookings/my", method: "GET")
        return try decodeWrapped([BackendBookingDTO].self, from: data).map { $0.toDomain() }
    }

    public func cancelBooking(bookingId: String, reason: String = "Player requested cancellation") async throws -> BookingPass {
        let escaped = bookingId.addingPercentEncoding(withAllowedCharacters: .urlPathAllowed) ?? bookingId
        let body = try encoder.encode(["reason": reason])
        let data = try await sendRequest(path: "/api/v1/bookings/\(escaped)/cancel", method: "POST", body: body)
        return try decodeWrapped(BackendBookingDTO.self, from: data).toDomain()
    }

    // MARK: - Gate Scanner, Walk-In & Facilities APIs

    public func verifyGatePass(qrPassToken: String) async throws -> GateVerifyResult {
        let body = try encoder.encode(["qrPassToken": qrPassToken.trimmingCharacters(in: .whitespacesAndNewlines)])
        let data = try await sendRequest(path: "/api/v1/gate/verify-pass", method: "POST", body: body)
        let dto = try decodeWrapped(BackendGateVerifyDTO.self, from: data)
        let isValid = dto.valid ?? dto.success ?? false
        return GateVerifyResult(
            valid: isValid,
            alreadyCheckedIn: dto.alreadyCheckedIn ?? false,
            message: dto.message ?? (isValid ? "Pass verified!" : "Invalid pass"),
            booking: dto.booking?.toDomain()
        )
    }

    public func createWalkInBooking(
        courtId: String,
        date: String,
        slot: String,
        customerName: String,
        customerPhone: String,
        amountCollectedRupees: Int
    ) async throws -> BookingPass {
        let req = WalkInRequestBody(
            courtId: courtId,
            date: date,
            slot: slot,
            customerName: customerName,
            customerPhone: customerPhone,
            amountCollected: max(100, amountCollectedRupees)
        )
        let data = try await sendRequest(path: "/api/v1/gate/walk-in", method: "POST", body: try encoder.encode(req))
        return try decodeWrapped(BackendBookingDTO.self, from: data).toDomain()
    }

    public func createFacility(
        venueId: String,
        name: String,
        sport: String,
        capacity: Int,
        surface: String,
        dimensions: String,
        baseHourlyRateRupees: Int,
        peakHourlyRateRupees: Int,
        hasFloodlights: Bool = true,
        amenities: [String] = []
    ) async throws -> Court {
        let req = CreateFacilityRequestBody(
            venueId: venueId,
            name: name,
            sport: sport,
            facilityType: sport,
            capacity: max(1, capacity),
            surface: surface,
            dimensions: dimensions,
            baseHourlyRate: max(200, baseHourlyRateRupees),
            peakHourlyRate: max(200, peakHourlyRateRupees),
            hasFloodlights: hasFloodlights,
            amenities: amenities
        )
        let data = try await sendRequest(path: "/api/v1/facilities", method: "POST", body: try encoder.encode(req))
        return try decodeWrapped(BackendFacilityDTO.self, from: data).toCourt()
    }

    // MARK: - Auth Parsing (Testable Offline)

    public func parseAndStoreAuth(_ data: Data) throws -> AuthResult {
        let dto = try decodeWrapped(BackendAuthDTO.self, from: data)
        let token = dto.token ?? ""
        let userDto = dto.user
        let roleStr = userDto?.role ?? "ROLE_PLAYER"
        let role: UserRole = roleStr.uppercased().contains("OWNER") ? .ROLE_OWNER : .ROLE_PLAYER
        let uid = userDto?.uid ?? "usr_remote"

        let session = UserSession(
            uid: uid,
            name: userDto?.fullName ?? userDto?.displayName ?? "Apex Player",
            email: userDto?.email ?? "",
            phone: userDto?.phone ?? "",
            role: role,
            loyaltyPoints: userDto?.loyaltyPoints ?? 100,
            isGuest: false,
            jwtToken: token.isEmpty ? nil : token
        )

        if !token.isEmpty {
            self.authToken = token
        }
        self.activeUserId = uid
        return AuthResult(token: token, user: session)
    }

    // MARK: - HTTP Transport

    private func applyHeaders(to request: inout URLRequest) {
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        if request.httpMethod == "POST" || request.httpMethod == "PUT" {
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        }
        if let token = authToken, !token.isEmpty {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        if let uid = activeUserId, !uid.isEmpty, uid != "guest_001" {
            request.setValue(uid, forHTTPHeaderField: "X-User-Id")
            request.setValue(uid, forHTTPHeaderField: "X-Owner-Id")
        }
    }

    private func sendRequest(path: String, method: String, body: Data? = nil) async throws -> Data {
        let url = try endpointURL(path)
        var request = URLRequest(url: url, timeoutInterval: Self.defaultTimeoutSeconds)
        request.httpMethod = method
        request.httpBody = body
        applyHeaders(to: &request)

        let (data, response) = try await session.data(for: request)
        guard let http = response as? HTTPURLResponse else {
            throw ApexApiError.httpError(0, "No HTTP response")
        }

        if !(200...299).contains(http.statusCode) {
            let errMessage = (try? decoder.decode(BackendErrorEnvelope.self, from: data).message)
                ?? String(data: data, encoding: .utf8)
                ?? "Request failed"
            throw ApexApiError.httpError(http.statusCode, errMessage)
        }
        return data
    }

    private func decodeWrapped<T: Codable>(_ type: T.Type, from data: Data) throws -> T {
        if let wrapped = try? decoder.decode(ApiResponse<T>.self, from: data) {
            if !wrapped.success {
                throw ApexApiError.httpError(400, wrapped.message ?? "API request failed")
            }
            if let inner = wrapped.data {
                return inner
            }
        }
        do {
            return try decoder.decode(T.self, from: data)
        } catch {
            throw ApexApiError.decodingError(error.localizedDescription)
        }
    }
}

// MARK: - Wire DTOs

private struct BackendErrorEnvelope: Codable {
    let message: String?
}

private struct BackendHealthPayload: Codable {
    let status: String?
    let service: String?
    let hosting: String?
    let runtime: String?
    let database: String?
    let dbConnected: Bool?
    let usersCount: Int64?
    let venuesCount: Int64?
    let bookingsCount: Int64?
    let timestamp: Int64?
}

private struct BackendAuthDTO: Codable {
    let token: String?
    let user: BackendUserDTO?
}

private struct BackendUserDTO: Codable {
    let uid: String?
    let email: String?
    let fullName: String?
    let displayName: String?
    let phone: String?
    let role: String?
    let loyaltyPoints: Int?
}

private struct BackendOtpDispatchPayload: Codable {
    let sessionId: String?
    let expiresInSeconds: Int?
    let simulatedSmsOtp: String?
    let simulatedEmailOtp: String?
}

private struct RegisterPasswordRequestBody: Codable {
    let displayName: String
    let email: String
    let phone: String
    let password: String
    let city: String
    let role: String
}

private struct VerifyRegistrationOtpRequestBody: Codable {
    let sessionId: String
    let phoneOtp: String
    let emailOtp: String
    let role: String
    let displayName: String
    let phone: String
    let email: String
    let password: String?
    let city: String
}

private struct PricingQuoteRequestBody: Codable {
    let venueId: String
    let resourceId: String
    let date: String
    let slots: [String]
    let promoCode: String?
}

private struct InitiateBookingRequestBody: Codable {
    let venueId: String
    let resourceId: String
    let bookingDate: String
    let slotStartTimes: [String]
    let sportType: String?
    let promoCode: String?
}

private struct WalkInRequestBody: Codable {
    let courtId: String
    let date: String
    let slot: String
    let customerName: String
    let customerPhone: String
    let amountCollected: Int
}

private struct CreateFacilityRequestBody: Codable {
    let venueId: String
    let name: String
    let sport: String
    let facilityType: String
    let capacity: Int
    let surface: String
    let dimensions: String
    let baseHourlyRate: Int
    let peakHourlyRate: Int
    let hasFloodlights: Bool
    let amenities: [String]
}

private struct BackendVenueDTO: Codable {
    let venueId: String?
    let id: String?
    let name: String?
    let locality: String?
    let area: String?
    let city: String?
    let rating: Double?
    let reviewCount: Int?
    let distanceKm: Double?
    let minSlotPricePaise: Int64?
    let startingPrice: Int64?
    let sportTypes: [String]?
    let sports: [String]?
    let addressLine: String?
    let address: String?
    let amenities: [String]?
    let hasActiveDiscounts: Bool?

    func toDomain() -> Venue {
        let rawCity = city ?? "Thane"
        let titleCity = rawCity
            .split(separator: " ")
            .map { $0.prefix(1).uppercased() + $0.dropFirst().lowercased() }
            .joined(separator: " ")
        let rawSports = (sportTypes ?? sports ?? []).map { SportType.fromApiName($0) }
        var dedupedSports: [SportType] = []
        for s in rawSports where !dedupedSports.contains(s) {
            dedupedSports.append(s)
        }
        if dedupedSports.isEmpty { dedupedSports = [.CRICKET_TURF] }

        let loc = locality ?? area ?? "Central"
        let minPrice = minSlotPricePaise ?? ((startingPrice ?? 800) * 100)
        return Venue(
            id: venueId ?? id ?? "ven_remote",
            name: name ?? "Apex Arena",
            area: loc,
            city: titleCity,
            rating: rating ?? 4.8,
            reviewCount: reviewCount ?? 120,
            distanceKm: distanceKm ?? 2.4,
            startingPricePaise: minPrice,
            supportedSports: dedupedSports,
            address: addressLine ?? address ?? "\(loc), \(titleCity)",
            amenities: amenities ?? [],
            badge: (hasActiveDiscounts ?? false) ? "⚡ OFFERS LIVE" : nil
        )
    }
}

private struct BackendVenueDetailDTO: Codable {
    let venue: BackendVenueDTO
    let resources: [BackendResourceDTO]?
}

private struct BackendResourceDTO: Codable {
    let resourceId: String?
    let id: String?
    let venueId: String?
    let name: String?
    let sportTypes: [String]?
    let basePricePaise: Int64?
    let peakHourPricePaise: Int64?
    let maxPlayers: Int?
    let surfaceType: String?
    let dimensions: String?
    let indoor: Bool?
    let isIndoor: Bool?

    func toCourt() -> Court {
        let base = basePricePaise ?? 100_000
        let peak = peakHourPricePaise ?? ((base * 130) / 100)
        return Court(
            id: resourceId ?? id ?? "res_remote",
            venueId: venueId ?? "",
            name: name ?? "Playable Unit",
            sportType: SportType.fromApiName(sportTypes?.first),
            capacity: maxPlayers ?? 10,
            surfaceType: surfaceType ?? "Synthetic Turf",
            isIndoor: isIndoor ?? indoor ?? false,
            dimensions: dimensions ?? "100 x 60 ft",
            baseHourlyPricePaise: base,
            peakHourlyPricePaise: peak
        )
    }
}

private struct BackendFacilityDTO: Codable {
    let courtId: String?
    let id: String?
    let venueId: String?
    let name: String?
    let sport: String?
    let facilityType: String?
    let capacity: Int?
    let surface: String?
    let dimensions: String?
    let baseHourlyRate: Int64?
    let peakHourlyRate: Int64?
    let hasFloodlights: Bool?

    func toCourt() -> Court {
        let baseRs = baseHourlyRate ?? 1000
        let peakRs = peakHourlyRate ?? ((baseRs * 130) / 100)
        return Court(
            id: courtId ?? id ?? "crt_remote",
            venueId: venueId ?? "",
            name: name ?? "New Court",
            sportType: SportType.fromApiName(sport ?? facilityType),
            capacity: capacity ?? 10,
            surfaceType: surface ?? "Synthetic Turf",
            isIndoor: !(hasFloodlights ?? true),
            dimensions: dimensions ?? "100 x 60 ft",
            baseHourlyPricePaise: baseRs * 100,
            peakHourlyPricePaise: peakRs * 100
        )
    }
}

private struct BackendSlotDTO: Codable {
    let resourceId: String?
    let date: String?
    let startTime: String?
    let endTime: String?
    let originalPricePaise: Int64?
    let finalPricePaise: Int64?
    let status: String?
    let isPeak: Bool?
    let peak: Bool?
    let discountInfo: String?

    func toDomain(defaultResourceId: String, defaultDate: String) -> TimeSlot {
        let resId = resourceId ?? defaultResourceId
        let d = date ?? defaultDate
        let start = startTime ?? "06:00"
        let end = endTime ?? "07:00"
        let base = originalPricePaise ?? finalPricePaise ?? 80_000
        let finalP = finalPricePaise ?? base
        let upperStatus = (status ?? "AVAILABLE").uppercased()
        let mappedStatus: SlotStatus
        switch upperStatus {
        case "HELD": mappedStatus = .HELD
        case "BOOKED", "BLOCKED": mappedStatus = .BOOKED
        case "PAST": mappedStatus = .PAST
        default: mappedStatus = .AVAILABLE
        }
        return TimeSlot(
            id: "\(resId)_\(d)_\(start)",
            courtId: resId,
            date: d,
            startTime: start,
            endTime: end,
            basePricePaise: base,
            finalPricePaise: finalP,
            status: mappedStatus,
            isOffPeak: finalP < base || !(discountInfo ?? "").isEmpty,
            isPeak: isPeak ?? peak ?? false
        )
    }
}

private struct BackendPriceQuoteDTO: Codable {
    let subtotalPaise: Int64?
    let discountAmountPaise: Int64?
    let taxPaise: Int64?
    let finalPayablePaise: Int64?
    let appliedDiscountDescription: String?
    let discountCode: String?

    func toDomain() -> PriceBreakdown {
        let sub = subtotalPaise ?? 0
        let disc = discountAmountPaise ?? 0
        let tax = taxPaise ?? 0
        let total = finalPayablePaise ?? max(100, sub - disc + tax)
        return PriceBreakdown(
            baseCourtPaise: sub,
            offPeakDiscountPaise: 0,
            peakSurgePaise: 0,
            promoDiscountPaise: disc,
            convenienceFeePaise: tax,
            totalPayablePaise: total,
            promoCodeApplied: appliedDiscountDescription ?? discountCode
        )
    }
}

private struct BackendInitiateBookingDTO: Codable {
    let bookingId: String?
    let razorpayOrderId: String?
    let amountPaise: Int64?
    let currency: String?
    let razorpayKeyId: String?
    let holdExpiresAt: Int64?
    let quote: BackendPriceQuoteDTO?
}

private struct BackendBookedSlotDTO: Codable {
    let startTime: String?
    let endTime: String?
    let unitPricePaise: Int64?
}

private struct BackendBookingDTO: Codable {
    let bookingId: String?
    let id: String?
    let bookingCode: String?
    let qrPassToken: String?
    let venueName: String?
    let resourceName: String?
    let courtName: String?
    let sportType: String?
    let bookingDate: String?
    let timeSlotRange: String?
    let slots: [BackendBookedSlotDTO]?
    let finalPayablePaise: Int64?
    let status: String?
    let createdAt: Int64?
    let refundAmountPaise: Int64?
    let cancellationReason: String?

    func toDomain() -> BookingPass {
        let range: String
        if let slots, !slots.isEmpty {
            let s = slots.first?.startTime ?? "06:00"
            let e = slots.last?.endTime ?? "07:00"
            range = "\(s) - \(e)"
        } else {
            range = timeSlotRange ?? "18:00 - 19:00"
        }

        let upper = (status ?? "CONFIRMED").uppercased()
        let mappedStatus: BookingStatus
        if upper.hasPrefix("CANCEL") {
            mappedStatus = .CANCELLED
        } else if upper == "CHECKED_IN" || upper == "COMPLETED" {
            mappedStatus = .CHECKED_IN
        } else {
            mappedStatus = .CONFIRMED
        }

        let bId = bookingId ?? id ?? "bk_remote"
        let bCode = bookingCode ?? bId
        return BookingPass(
            id: bId,
            bookingCode: bCode,
            venueName: venueName ?? "Apex Arena",
            courtName: resourceName ?? courtName ?? "Turf Court",
            sportType: SportType.fromApiName(sportType),
            date: bookingDate ?? "",
            timeSlotRange: range,
            amountPaidPaise: finalPayablePaise ?? 0,
            status: mappedStatus,
            createdAtMs: createdAt ?? Int64(Date().timeIntervalSince1970 * 1000),
            refundAmountPaise: refundAmountPaise ?? 0,
            cancellationTier: cancellationReason,
            qrPassToken: qrPassToken ?? "APEX-PASS-\(bCode)"
        )
    }
}

private struct BackendGateVerifyDTO: Codable {
    let success: Bool?
    let valid: Bool?
    let alreadyCheckedIn: Bool?
    let message: String?
    let booking: BackendBookingDTO?
}

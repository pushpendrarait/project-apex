import Foundation

public enum SportType: String, CaseIterable, Codable, Identifiable, Sendable {
    case CRICKET_TURF
    case FOOTBALL_TURF
    case BADMINTON
    case PICKLEBALL
    case POOL_TABLE
    case LAWN_TENNIS
    case SQUASH
    case MULTI_SPORT

    public var id: String { rawValue }

    public var displayName: String {
        switch self {
        case .CRICKET_TURF: return "Box Cricket"
        case .FOOTBALL_TURF: return "Football Turf"
        case .BADMINTON: return "Badminton"
        case .PICKLEBALL: return "Pickleball"
        case .POOL_TABLE: return "Pool & Snooker"
        case .LAWN_TENNIS: return "Lawn Tennis"
        case .SQUASH: return "Squash"
        case .MULTI_SPORT: return "Multi-Sport"
        }
    }

    public var subtitle: String {
        switch self {
        case .CRICKET_TURF: return "Full-enclosed turf with LED floodlights"
        case .FOOTBALL_TURF: return "5v5 & 7v7 FIFA-grade artificial grass"
        case .BADMINTON: return "BWF-approved shock-absorbing vinyl courts"
        case .PICKLEBALL: return "Fast-action USA Pickleball standard courts"
        case .POOL_TABLE: return "English 8-ball & professional tournament slate"
        case .LAWN_TENNIS: return "DecoTurf hard courts with night lighting"
        case .SQUASH: return "Glass-back air-conditioned squash arena"
        case .MULTI_SPORT: return "Versatile court for Basketball, Futsal & Volleyball"
        }
    }

    public var emoji: String {
        switch self {
        case .CRICKET_TURF: return "🏏"
        case .FOOTBALL_TURF: return "⚽"
        case .BADMINTON: return "🏸"
        case .PICKLEBALL: return "🏓"
        case .POOL_TABLE: return "🎱"
        case .LAWN_TENNIS: return "🎾"
        case .SQUASH: return "💥"
        case .MULTI_SPORT: return "🏟️"
        }
    }

    public static func fromApiName(_ raw: String?) -> SportType {
        guard let cleaned = raw?.trimmingCharacters(in: .whitespacesAndNewlines).uppercased(),
              !cleaned.isEmpty else {
            return .MULTI_SPORT
        }
        if let exact = Self.allCases.first(where: {
            $0.rawValue.caseInsensitiveCompare(cleaned) == .orderedSame ||
            $0.displayName.caseInsensitiveCompare(cleaned) == .orderedSame
        }) {
            return exact
        }
        if cleaned.contains("CRICKET") { return .CRICKET_TURF }
        if cleaned.contains("FOOTBALL") || cleaned.contains("SOCCER") { return .FOOTBALL_TURF }
        if cleaned.contains("BADMINTON") { return .BADMINTON }
        if cleaned.contains("PICKLE") { return .PICKLEBALL }
        if cleaned.contains("POOL") || cleaned.contains("SNOOKER") { return .POOL_TABLE }
        if cleaned.contains("TENNIS") { return .LAWN_TENNIS }
        if cleaned.contains("SQUASH") { return .SQUASH }
        return .MULTI_SPORT
    }
}

public struct Venue: Identifiable, Codable, Equatable, Sendable {
    public let id: String
    public let name: String
    public let area: String
    public let city: String
    public let rating: Double
    public let reviewCount: Int
    public let distanceKm: Double
    public let startingPricePaise: Int64
    public let supportedSports: [SportType]
    public let address: String
    public let amenities: [String]
    public let badge: String?

    public var startingPriceRupees: Int64 { startingPricePaise / 100 }

    public init(
        id: String,
        name: String,
        area: String,
        city: String,
        rating: Double,
        reviewCount: Int,
        distanceKm: Double,
        startingPricePaise: Int64,
        supportedSports: [SportType],
        address: String,
        amenities: [String],
        badge: String? = nil
    ) {
        self.id = id
        self.name = name
        self.area = area
        self.city = city
        self.rating = rating
        self.reviewCount = reviewCount
        self.distanceKm = distanceKm
        self.startingPricePaise = startingPricePaise
        self.supportedSports = supportedSports
        self.address = address
        self.amenities = amenities
        self.badge = badge
    }
}

public struct Court: Identifiable, Codable, Equatable, Sendable {
    public let id: String
    public let venueId: String
    public let name: String
    public let sportType: SportType
    public let capacity: Int
    public let surfaceType: String
    public let isIndoor: Bool
    public let dimensions: String
    public let baseHourlyPricePaise: Int64
    public let peakHourlyPricePaise: Int64

    public var baseHourlyPriceRupees: Int64 { baseHourlyPricePaise / 100 }
    public var peakHourlyPriceRupees: Int64 { peakHourlyPricePaise / 100 }

    public init(
        id: String,
        venueId: String,
        name: String,
        sportType: SportType,
        capacity: Int,
        surfaceType: String,
        isIndoor: Bool,
        dimensions: String,
        baseHourlyPricePaise: Int64,
        peakHourlyPricePaise: Int64
    ) {
        self.id = id
        self.venueId = venueId
        self.name = name
        self.sportType = sportType
        self.capacity = capacity
        self.surfaceType = surfaceType
        self.isIndoor = isIndoor
        self.dimensions = dimensions
        self.baseHourlyPricePaise = baseHourlyPricePaise
        self.peakHourlyPricePaise = peakHourlyPricePaise
    }
}

public enum SlotStatus: String, Codable, Sendable {
    case AVAILABLE
    case HELD
    case BOOKED
    case PAST
}

public struct TimeSlot: Identifiable, Codable, Equatable, Sendable {
    public let id: String
    public let courtId: String
    public let date: String
    public let startTime: String
    public let endTime: String
    public let basePricePaise: Int64
    public let finalPricePaise: Int64
    public let status: SlotStatus
    public let isOffPeak: Bool
    public let isPeak: Bool

    public var finalPriceRupees: Int64 { finalPricePaise / 100 }
    public var basePriceRupees: Int64 { basePricePaise / 100 }
    public var formattedTime: String { "\(startTime) - \(endTime)" }

    public init(
        id: String,
        courtId: String,
        date: String,
        startTime: String,
        endTime: String,
        basePricePaise: Int64,
        finalPricePaise: Int64,
        status: SlotStatus = .AVAILABLE,
        isOffPeak: Bool = false,
        isPeak: Bool = false
    ) {
        self.id = id
        self.courtId = courtId
        self.date = date
        self.startTime = startTime
        self.endTime = endTime
        self.basePricePaise = basePricePaise
        self.finalPricePaise = finalPricePaise
        self.status = status
        self.isOffPeak = isOffPeak
        self.isPeak = isPeak
    }
}

public struct HoldSession: Identifiable, Codable, Equatable, Sendable {
    public var id: String { holdId }
    public let holdId: String
    public let venueId: String
    public let venueName: String
    public let courtId: String
    public let courtName: String
    public let sportType: SportType
    public let date: String
    public let slots: [TimeSlot]
    public let expiresAtTimestampMs: Int64
    public let totalPaise: Int64

    public init(
        holdId: String,
        venueId: String,
        venueName: String,
        courtId: String,
        courtName: String,
        sportType: SportType,
        date: String,
        slots: [TimeSlot],
        expiresAtTimestampMs: Int64,
        totalPaise: Int64
    ) {
        self.holdId = holdId
        self.venueId = venueId
        self.venueName = venueName
        self.courtId = courtId
        self.courtName = courtName
        self.sportType = sportType
        self.date = date
        self.slots = slots
        self.expiresAtTimestampMs = expiresAtTimestampMs
        self.totalPaise = totalPaise
    }

    public func remainingSeconds(nowMs: Int64 = Int64(Date().timeIntervalSince1970 * 1000)) -> Int64 {
        let rem = (expiresAtTimestampMs - nowMs) / 1000
        return rem > 0 ? rem : 0
    }
}

public struct PriceBreakdown: Codable, Equatable, Sendable {
    public let baseCourtPaise: Int64
    public let offPeakDiscountPaise: Int64
    public let peakSurgePaise: Int64
    public let promoDiscountPaise: Int64
    public let convenienceFeePaise: Int64
    public let totalPayablePaise: Int64
    public let promoCodeApplied: String?

    public var baseCourtRupees: Int64 { baseCourtPaise / 100 }
    public var offPeakDiscountRupees: Int64 { offPeakDiscountPaise / 100 }
    public var peakSurgeRupees: Int64 { peakSurgePaise / 100 }
    public var promoDiscountRupees: Int64 { promoDiscountPaise / 100 }
    public var convenienceFeeRupees: Int64 { convenienceFeePaise / 100 }
    public var totalPayableRupees: Int64 { totalPayablePaise / 100 }

    public init(
        baseCourtPaise: Int64,
        offPeakDiscountPaise: Int64,
        peakSurgePaise: Int64,
        promoDiscountPaise: Int64,
        convenienceFeePaise: Int64,
        totalPayablePaise: Int64,
        promoCodeApplied: String? = nil
    ) {
        self.baseCourtPaise = baseCourtPaise
        self.offPeakDiscountPaise = offPeakDiscountPaise
        self.peakSurgePaise = peakSurgePaise
        self.promoDiscountPaise = promoDiscountPaise
        self.convenienceFeePaise = convenienceFeePaise
        self.totalPayablePaise = totalPayablePaise
        self.promoCodeApplied = promoCodeApplied
    }
}

public enum BookingStatus: String, Codable, Sendable {
    case CONFIRMED
    case CHECKED_IN
    case CANCELLED
}

public struct BookingPass: Identifiable, Codable, Equatable, Sendable {
    public let id: String
    public let bookingCode: String
    public let venueName: String
    public let courtName: String
    public let sportType: SportType
    public let date: String
    public let timeSlotRange: String
    public let amountPaidPaise: Int64
    public let status: BookingStatus
    public let createdAtMs: Int64
    public let refundAmountPaise: Int64
    public let cancellationTier: String?
    public let qrPassToken: String?

    public var amountPaidRupees: Int64 { amountPaidPaise / 100 }
    public var refundAmountRupees: Int64 { refundAmountPaise / 100 }

    public init(
        id: String,
        bookingCode: String,
        venueName: String,
        courtName: String,
        sportType: SportType,
        date: String,
        timeSlotRange: String,
        amountPaidPaise: Int64,
        status: BookingStatus = .CONFIRMED,
        createdAtMs: Int64 = Int64(Date().timeIntervalSince1970 * 1000),
        refundAmountPaise: Int64 = 0,
        cancellationTier: String? = nil,
        qrPassToken: String? = nil
    ) {
        self.id = id
        self.bookingCode = bookingCode
        self.venueName = venueName
        self.courtName = courtName
        self.sportType = sportType
        self.date = date
        self.timeSlotRange = timeSlotRange
        self.amountPaidPaise = amountPaidPaise
        self.status = status
        self.createdAtMs = createdAtMs
        self.refundAmountPaise = refundAmountPaise
        self.cancellationTier = cancellationTier
        self.qrPassToken = qrPassToken
    }
}

public enum UserRole: String, Codable, Sendable {
    case ROLE_PLAYER
    case ROLE_OWNER
}

public struct UserSession: Codable, Equatable, Sendable {
    public let uid: String
    public let name: String
    public let email: String
    public let phone: String
    public let role: UserRole
    public let loyaltyPoints: Int
    public let isGuest: Bool
    public let jwtToken: String?

    public init(
        uid: String,
        name: String,
        email: String,
        phone: String,
        role: UserRole = .ROLE_PLAYER,
        loyaltyPoints: Int = 120,
        isGuest: Bool = false,
        jwtToken: String? = nil
    ) {
        self.uid = uid
        self.name = name
        self.email = email
        self.phone = phone
        self.role = role
        self.loyaltyPoints = loyaltyPoints
        self.isGuest = isGuest
        self.jwtToken = jwtToken
    }
}

public struct HealthStatus: Codable, Equatable, Sendable {
    public let status: String
    public let service: String
    public let hosting: String
    public let runtime: String
    public let database: String
    public let dbConnected: Bool
    public let usersCount: Int64
    public let venuesCount: Int64
    public let bookingsCount: Int64
    public let latencyMs: Int64
    public let url: String
    public let isOnline: Bool
    public let message: String?
    public let timestampMs: Int64

    public init(
        status: String = "STANDBY",
        service: String = "project-apex-backend",
        hosting: String = "Render",
        runtime: String = "Java 21 LTS + Spring Boot 3.2.x",
        database: String = "Neon PostgreSQL",
        dbConnected: Bool = false,
        usersCount: Int64 = 0,
        venuesCount: Int64 = 0,
        bookingsCount: Int64 = 0,
        latencyMs: Int64 = 0,
        url: String = "",
        isOnline: Bool = false,
        message: String? = nil,
        timestampMs: Int64 = Int64(Date().timeIntervalSince1970 * 1000)
    ) {
        self.status = status
        self.service = service
        self.hosting = hosting
        self.runtime = runtime
        self.database = database
        self.dbConnected = dbConnected
        self.usersCount = usersCount
        self.venuesCount = venuesCount
        self.bookingsCount = bookingsCount
        self.latencyMs = latencyMs
        self.url = url
        self.isOnline = isOnline
        self.message = message
        self.timestampMs = timestampMs
    }
}

public struct ApiResponse<T: Codable>: Codable {
    public let success: Bool
    public let message: String?
    public let data: T?
    public let timestamp: Int64?

    public init(success: Bool, message: String? = nil, data: T? = nil, timestamp: Int64? = nil) {
        self.success = success
        self.message = message
        self.data = data
        self.timestamp = timestamp
    }
}

public struct AuthResult: Codable, Equatable, Sendable {
    public let token: String
    public let user: UserSession

    public init(token: String, user: UserSession) {
        self.token = token
        self.user = user
    }
}

public struct OtpDispatchResult: Codable, Equatable, Sendable {
    public let sessionId: String
    public let expiresInSeconds: Int
    public let simulatedSmsOtp: String?
    public let simulatedEmailOtp: String?

    public init(
        sessionId: String,
        expiresInSeconds: Int = 300,
        simulatedSmsOtp: String? = nil,
        simulatedEmailOtp: String? = nil
    ) {
        self.sessionId = sessionId
        self.expiresInSeconds = expiresInSeconds
        self.simulatedSmsOtp = simulatedSmsOtp
        self.simulatedEmailOtp = simulatedEmailOtp
    }
}

public struct BookingInitiationResult: Codable, Equatable, Sendable {
    public let bookingId: String
    public let razorpayOrderId: String
    public let amountPaise: Int64
    public let currency: String
    public let razorpayKeyId: String?
    public let holdExpiresAtMs: Int64
    public let quote: PriceBreakdown?

    public init(
        bookingId: String,
        razorpayOrderId: String,
        amountPaise: Int64,
        currency: String = "INR",
        razorpayKeyId: String? = nil,
        holdExpiresAtMs: Int64,
        quote: PriceBreakdown? = nil
    ) {
        self.bookingId = bookingId
        self.razorpayOrderId = razorpayOrderId
        self.amountPaise = amountPaise
        self.currency = currency
        self.razorpayKeyId = razorpayKeyId
        self.holdExpiresAtMs = holdExpiresAtMs
        self.quote = quote
    }
}

public struct GateVerifyResult: Codable, Equatable, Sendable {
    public let valid: Bool
    public let alreadyCheckedIn: Bool
    public let message: String
    public let booking: BookingPass?

    public init(
        valid: Bool,
        alreadyCheckedIn: Bool = false,
        message: String,
        booking: BookingPass? = nil
    ) {
        self.valid = valid
        self.alreadyCheckedIn = alreadyCheckedIn
        self.message = message
        self.booking = booking
    }
}

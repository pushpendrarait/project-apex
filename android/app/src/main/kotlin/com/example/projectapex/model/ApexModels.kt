package com.example.projectapex.model

import java.io.Serializable

enum class SportType(
    val displayName: String,
    val subtitle: String,
    val emoji: String
) {
    CRICKET_TURF("Box Cricket", "Full-enclosed turf with LED floodlights", "🏏"),
    FOOTBALL_TURF("Football Turf", "5v5 & 7v7 FIFA-grade artificial grass", "⚽"),
    BADMINTON("Badminton", "BWF-approved shock-absorbing vinyl courts", "🏸"),
    PICKLEBALL("Pickleball", "Fast-action USA Pickleball standard courts", "🏓"),
    POOL_TABLE("Pool & Snooker", "English 8-ball & professional tournament slate", "🎱"),
    LAWN_TENNIS("Lawn Tennis", "DecoTurf hard courts with night lighting", "🎾"),
    SQUASH("Squash", "Glass-back air-conditioned squash arena", "💥"),
    MULTI_SPORT("Multi-Sport", "Versatile court for Basketball, Futsal & Volleyball", "🏟️");

    companion object {
        fun fromApiName(raw: String?): SportType {
            val cleaned = raw?.trim()?.uppercase() ?: return MULTI_SPORT
            entries.firstOrNull {
                it.name.equals(cleaned, ignoreCase = true) ||
                    it.displayName.equals(cleaned, ignoreCase = true)
            }?.let { return it }

            return when {
                cleaned.contains("CRICKET") -> CRICKET_TURF
                cleaned.contains("FOOTBALL") || cleaned.contains("SOCCER") -> FOOTBALL_TURF
                cleaned.contains("BADMINTON") -> BADMINTON
                cleaned.contains("PICKLE") -> PICKLEBALL
                cleaned.contains("POOL") || cleaned.contains("SNOOKER") -> POOL_TABLE
                cleaned.contains("TENNIS") -> LAWN_TENNIS
                cleaned.contains("SQUASH") -> SQUASH
                else -> MULTI_SPORT
            }
        }
    }
}

data class Venue(
    val id: String,
    val name: String,
    val area: String,
    val city: String,
    val rating: Double,
    val reviewCount: Int,
    val distanceKm: Double,
    val startingPricePaise: Long,
    val supportedSports: List<SportType>,
    val address: String,
    val amenities: List<String>,
    val badge: String? = null
) : Serializable {
    val startingPriceRupees: Long get() = startingPricePaise / 100
}

data class Court(
    val id: String,
    val venueId: String,
    val name: String,
    val sportType: SportType,
    val capacity: Int,
    val surfaceType: String,
    val isIndoor: Boolean,
    val dimensions: String,
    val baseHourlyPricePaise: Long,
    val peakHourlyPricePaise: Long
) : Serializable {
    val baseHourlyPriceRupees: Long get() = baseHourlyPricePaise / 100
    val peakHourlyPriceRupees: Long get() = peakHourlyPricePaise / 100
}

enum class SlotStatus {
    AVAILABLE,
    HELD,
    BOOKED,
    PAST
}

data class TimeSlot(
    val id: String,
    val courtId: String,
    val date: String,          // YYYY-MM-DD
    val startTime: String,     // HH:mm
    val endTime: String,       // HH:mm
    val basePricePaise: Long,
    val finalPricePaise: Long,
    val status: SlotStatus = SlotStatus.AVAILABLE,
    val isOffPeak: Boolean = false,
    val isPeak: Boolean = false
) : Serializable {
    val finalPriceRupees: Long get() = finalPricePaise / 100
    val basePriceRupees: Long get() = basePricePaise / 100
    val formattedTime: String get() = "$startTime - $endTime"
}

data class HoldSession(
    val holdId: String,
    val venueId: String,
    val venueName: String,
    val courtId: String,
    val courtName: String,
    val sportType: SportType,
    val date: String,
    val slots: List<TimeSlot>,
    val expiresAtTimestampMs: Long,
    val totalPaise: Long
) : Serializable {
    fun remainingSeconds(): Long {
        val rem = (expiresAtTimestampMs - System.currentTimeMillis()) / 1000
        return if (rem > 0) rem else 0
    }
}

data class PriceBreakdown(
    val baseCourtPaise: Long,
    val offPeakDiscountPaise: Long,
    val peakSurgePaise: Long,
    val promoDiscountPaise: Long,
    val convenienceFeePaise: Long,
    val totalPayablePaise: Long,
    val promoCodeApplied: String? = null
) : Serializable {
    val baseCourtRupees: Long get() = baseCourtPaise / 100
    val offPeakDiscountRupees: Long get() = offPeakDiscountPaise / 100
    val peakSurgeRupees: Long get() = peakSurgePaise / 100
    val promoDiscountRupees: Long get() = promoDiscountPaise / 100
    val convenienceFeeRupees: Long get() = convenienceFeePaise / 100
    val totalPayableRupees: Long get() = totalPayablePaise / 100
}

enum class BookingStatus {
    CONFIRMED,
    CHECKED_IN,
    CANCELLED
}

data class BookingPass(
    val id: String,
    val bookingCode: String,     // e.g. "ARENA-8492X"
    val venueName: String,
    val courtName: String,
    val sportType: SportType,
    val date: String,
    val timeSlotRange: String,
    val amountPaidPaise: Long,
    val status: BookingStatus = BookingStatus.CONFIRMED,
    val createdAtMs: Long = System.currentTimeMillis(),
    val refundAmountPaise: Long = 0,
    val cancellationTier: String? = null,
    val qrPassToken: String? = null
) : Serializable {
    val amountPaidRupees: Long get() = amountPaidPaise / 100
    val refundAmountRupees: Long get() = refundAmountPaise / 100
}

enum class UserRole {
    ROLE_PLAYER,
    ROLE_OWNER
}

data class UserSession(
    val uid: String,
    val name: String,
    val email: String,
    val phone: String,
    val role: UserRole = UserRole.ROLE_PLAYER,
    val loyaltyPoints: Int = 120,
    val isGuest: Boolean = false,
    val jwtToken: String? = null
) : Serializable

data class HealthStatus(
    val status: String = "STANDBY",
    val service: String = "project-apex-backend",
    val hosting: String = "Render",
    val runtime: String = "Java 21 LTS + Spring Boot 3.2.x",
    val database: String = "Neon PostgreSQL",
    val dbConnected: Boolean = false,
    val usersCount: Long = 0L,
    val venuesCount: Long = 0L,
    val bookingsCount: Long = 0L,
    val latencyMs: Long = 0L,
    val url: String = "",
    val isOnline: Boolean = false,
    val message: String? = null,
    val timestampMs: Long = System.currentTimeMillis()
) : Serializable

data class AuthResult(
    val token: String,
    val user: UserSession
) : Serializable

data class OtpDispatchResult(
    val sessionId: String,
    val expiresInSeconds: Int = 300,
    val simulatedSmsOtp: String? = null,
    val simulatedEmailOtp: String? = null
) : Serializable

data class BookingInitiationResult(
    val bookingId: String,
    val razorpayOrderId: String,
    val amountPaise: Long,
    val currency: String = "INR",
    val razorpayKeyId: String? = null,
    val holdExpiresAtMs: Long,
    val quote: PriceBreakdown? = null
) : Serializable

data class GateVerifyResult(
    val valid: Boolean,
    val alreadyCheckedIn: Boolean = false,
    val message: String,
    val booking: BookingPass? = null
) : Serializable

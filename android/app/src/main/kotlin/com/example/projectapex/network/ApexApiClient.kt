package com.example.projectapex.network

import com.example.projectapex.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class ApexApiClient(
    baseUrl: String = RENDER_PROD_URL
) {
    var baseUrl: String = normalizeBaseUrl(baseUrl).ifEmpty { RENDER_PROD_URL }
        set(value) {
            val norm = normalizeBaseUrl(value)
            if (norm.isNotEmpty()) {
                field = norm
            }
        }

    var authToken: String? = null
    var activeUserId: String? = null

    companion object {
        const val RENDER_PROD_URL = "https://project-apex-backend-fqe3.onrender.com"
        const val EMULATOR_LOCAL_URL = "http://10.0.2.2:8080"
        const val DEFAULT_TIMEOUT_MS = 8000

        /**
         * Strips trailing slashes and any trailing `/api/v1` segment so callers can store
         * either root origin or `/api/v1`-suffixed URLs without creating duplicate paths.
         */
        fun normalizeBaseUrl(url: String?): String {
            if (url == null) return ""
            var cleaned = url.trim()
            if (cleaned.isEmpty()) return ""
            while (cleaned.endsWith("/")) {
                cleaned = cleaned.dropLast(1)
            }
            if (cleaned.lowercase().endsWith("/api/v1")) {
                cleaned = cleaned.dropLast("/api/v1".length)
            }
            while (cleaned.endsWith("/")) {
                cleaned = cleaned.dropLast(1)
            }
            return cleaned
        }
    }

    private fun buildUrl(path: String, baseOverride: String? = null): String {
        val root = normalizeBaseUrl(baseOverride ?: baseUrl)
        val normalizedPath = if (path.startsWith("/")) path else "/$path"
        return "$root$normalizedPath"
    }

    private fun encodeQuery(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.name())

    /**
     * Parses `/api/v1/health` JSON into [HealthStatus], supporting both wrapped `ApiResponse`
     * (`{ "success": true, "data": { ... } }`) and unwrapped health payloads.
     */
    fun parseHealthJson(
        jsonStr: String,
        url: String = baseUrl,
        latencyMs: Long = 0L
    ): HealthStatus {
        val root = ApexJson.parseObject(jsonStr)
        val payload = root.obj("data") ?: root
        val isLocal = url.contains("10.0.2.2") || url.contains("localhost") || url.contains("127.0.0.1")

        val rawStatus = payload.str("status") ?: if (root.bool("success", true)) "UP" else "DOWN"
        val dbConnected = payload.bool("dbConnected", false)
        val isOnline = rawStatus.equals("UP", ignoreCase = true) || root.bool("success", false)

        return HealthStatus(
            status = rawStatus,
            service = payload.str("service") ?: "project-apex-backend",
            hosting = payload.str("hosting") ?: if (isLocal) "Localhost Dev (8080)" else "Render",
            runtime = payload.str("runtime") ?: "Java 21 LTS + Spring Boot 3.2.x",
            database = payload.str("database") ?: "Neon PostgreSQL",
            dbConnected = dbConnected,
            usersCount = payload.lng("usersCount", 0L),
            venuesCount = payload.lng("venuesCount", 0L),
            bookingsCount = payload.lng("bookingsCount", 0L),
            latencyMs = latencyMs,
            url = normalizeBaseUrl(url),
            isOnline = isOnline,
            message = root.str("message"),
            timestampMs = payload.lng("timestamp", root.lng("timestamp", System.currentTimeMillis()))
        )
    }

    suspend fun checkHealth(customUrl: String? = null): HealthStatus? = withContext(Dispatchers.IO) {
        val targetBase = normalizeBaseUrl(customUrl ?: baseUrl)
        if (targetBase.isEmpty()) return@withContext null

        val endpoint = buildUrl("/api/v1/health", targetBase)
        val startedAt = System.currentTimeMillis()
        try {
            val raw = httpRequest(endpoint, method = "GET")
            val latencyMs = (System.currentTimeMillis() - startedAt).coerceAtLeast(0L)
            parseHealthJson(raw, targetBase, latencyMs)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun pingBackend(customUrl: String? = null): Boolean = withContext(Dispatchers.IO) {
        val targetBase = normalizeBaseUrl(customUrl ?: baseUrl)
        if (targetBase.isEmpty()) return@withContext false

        val health = checkHealth(targetBase)
        if (health != null && health.isOnline) {
            return@withContext true
        }

        // Graceful fallback for older backends exposing /api/v1/sports
        val sportsEndpoint = buildUrl("/api/v1/sports", targetBase)
        try {
            val conn = openConnection(sportsEndpoint, "GET")
            val code = conn.responseCode
            conn.disconnect()
            code in 200..299
        } catch (_: Exception) {
            false
        }
    }

    suspend fun getAvailableSports(): List<String> = withContext(Dispatchers.IO) {
        val endpoint = buildUrl("/api/v1/sports")
        try {
            val raw = httpRequest(endpoint, "GET")
            val parsed = ApexJson.parse(raw)
            when (parsed) {
                is List<*> -> parsed.mapNotNull { it?.toString() }
                is Map<*, *> -> {
                    @Suppress("UNCHECKED_CAST")
                    val root = parsed as Map<String, Any?>
                    root.arr("data")?.mapNotNull { item ->
                        when (item) {
                            is Map<*, *> -> {
                                @Suppress("UNCHECKED_CAST")
                                val obj = item as Map<String, Any?>
                                obj.str("name") ?: obj.str("id")
                            }
                            else -> item?.toString()
                        }
                    } ?: emptyList()
                }
                else -> emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    // =========================================================================
    // Authentication APIs
    // =========================================================================

    suspend fun loginWithPassword(identifier: String, password: String): Result<AuthResult> = withContext(Dispatchers.IO) {
        runCatching {
            val body = ApexJson.encode(
                mapOf(
                    "identifier" to identifier,
                    "password" to password
                )
            )
            val raw = httpRequest(buildUrl("/api/v1/auth/login/password"), "POST", body)
            parseAndStoreAuth(raw)
        }
    }

    suspend fun registerWithPassword(
        displayName: String,
        email: String,
        phone: String,
        password: String,
        city: String = "Thane",
        role: UserRole = UserRole.ROLE_PLAYER
    ): Result<AuthResult> = withContext(Dispatchers.IO) {
        runCatching {
            val backendRole = if (role == UserRole.ROLE_OWNER) "venue_owner" else "player"
            val body = ApexJson.encode(
                mapOf(
                    "displayName" to displayName,
                    "email" to email,
                    "phone" to phone,
                    "password" to password,
                    "city" to city,
                    "role" to backendRole
                )
            )
            val raw = httpRequest(buildUrl("/api/v1/auth/register"), "POST", body)
            parseAndStoreAuth(raw)
        }
    }

    suspend fun sendRegistrationOtp(
        phone: String,
        email: String,
        displayName: String
    ): Result<OtpDispatchResult> = withContext(Dispatchers.IO) {
        runCatching {
            val body = ApexJson.encode(
                mapOf(
                    "phone" to phone,
                    "email" to email,
                    "displayName" to displayName
                )
            )
            val raw = httpRequest(buildUrl("/api/v1/auth/registration/send-otp"), "POST", body)
            val data = extractDataObject(raw)
            OtpDispatchResult(
                sessionId = data.str("sessionId") ?: "",
                expiresInSeconds = data.intVal("expiresInSeconds", 300),
                simulatedSmsOtp = data.str("simulatedSmsOtp"),
                simulatedEmailOtp = data.str("simulatedEmailOtp")
            )
        }
    }

    suspend fun verifyRegistrationOtp(
        sessionId: String,
        phoneOtp: String,
        emailOtp: String,
        displayName: String,
        phone: String,
        email: String,
        role: UserRole = UserRole.ROLE_PLAYER,
        city: String = "Thane",
        password: String? = null
    ): Result<AuthResult> = withContext(Dispatchers.IO) {
        runCatching {
            val backendRole = if (role == UserRole.ROLE_OWNER) "venue_owner" else "player"
            val payload = linkedMapOf<String, Any?>(
                "sessionId" to sessionId,
                "phoneOtp" to phoneOtp,
                "emailOtp" to emailOtp,
                "displayName" to displayName,
                "phone" to phone,
                "email" to email,
                "role" to backendRole,
                "city" to city
            )
            if (!password.isNullOrBlank()) {
                payload["password"] = password
            }
            val raw = httpRequest(buildUrl("/api/v1/auth/registration/verify-otp"), "POST", ApexJson.encode(payload))
            parseAndStoreAuth(raw)
        }
    }

    suspend fun sendLoginOtp(identifier: String): Result<OtpDispatchResult> = withContext(Dispatchers.IO) {
        runCatching {
            val body = ApexJson.encode(mapOf("identifier" to identifier))
            val raw = httpRequest(buildUrl("/api/v1/auth/login/send-otp"), "POST", body)
            val data = extractDataObject(raw)
            OtpDispatchResult(
                sessionId = data.str("sessionId") ?: "",
                expiresInSeconds = data.intVal("expiresInSeconds", 300),
                simulatedSmsOtp = data.str("simulatedSmsOtp"),
                simulatedEmailOtp = data.str("simulatedEmailOtp")
            )
        }
    }

    suspend fun verifyLoginOtp(sessionId: String, otp: String): Result<AuthResult> = withContext(Dispatchers.IO) {
        runCatching {
            val body = ApexJson.encode(
                mapOf(
                    "sessionId" to sessionId,
                    "otp" to otp
                )
            )
            val raw = httpRequest(buildUrl("/api/v1/auth/login/verify-otp"), "POST", body)
            parseAndStoreAuth(raw)
        }
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        try {
            httpRequest(buildUrl("/api/v1/auth/logout"), "POST", "{}")
        } catch (_: Exception) {
            // Stateless JWT logout may return 404/401 or run offline; local token cleanup still applies
        } finally {
            authToken = null
            activeUserId = null
        }
    }

    // =========================================================================
    // Venues, Availability & Pricing APIs
    // =========================================================================

    suspend fun fetchVenues(
        city: String? = null,
        sportType: String? = null
    ): Result<List<Venue>> = withContext(Dispatchers.IO) {
        runCatching {
            val params = mutableListOf<String>()
            if (!city.isNullOrBlank() && !city.equals("all", ignoreCase = true) && !city.equals("All Cities", ignoreCase = true)) {
                params.add("city=${encodeQuery(city.lowercase())}")
            }
            if (!sportType.isNullOrBlank() && !sportType.equals("all", ignoreCase = true)) {
                params.add("sportType=${encodeQuery(sportType)}")
            }
            val query = if (params.isNotEmpty()) "?${params.joinToString("&")}" else ""
            val raw = httpRequest(buildUrl("/api/v1/venues$query"), "GET")
            val dataList = extractDataArray(raw)
            dataList.mapNotNull { item ->
                @Suppress("UNCHECKED_CAST")
                (item as? Map<String, Any?>)?.let { parseVenue(it) }
            }
        }
    }

    suspend fun fetchVenueDetail(venueId: String): Result<Pair<Venue, List<Court>>> = withContext(Dispatchers.IO) {
        runCatching {
            val raw = httpRequest(buildUrl("/api/v1/venues/${encodeQuery(venueId)}"), "GET")
            val data = extractDataObject(raw)
            val venueMap = data.obj("venue") ?: data
            val venue = parseVenue(venueMap)
            val resources = data.arr("resources").orEmpty().mapNotNull { item ->
                @Suppress("UNCHECKED_CAST")
                (item as? Map<String, Any?>)?.let { parseResourceAsCourt(it) }
            }
            Pair(venue, resources)
        }
    }

    suspend fun fetchAvailability(resourceId: String, date: String): Result<List<TimeSlot>> = withContext(Dispatchers.IO) {
        runCatching {
            val endpoint = buildUrl("/api/v1/availability?resourceId=${encodeQuery(resourceId)}&date=${encodeQuery(date)}")
            val raw = httpRequest(endpoint, "GET")
            val dataList = extractDataArray(raw)
            dataList.mapNotNull { item ->
                @Suppress("UNCHECKED_CAST")
                (item as? Map<String, Any?>)?.let { parseTimeSlot(it, resourceId, date) }
            }
        }
    }

    suspend fun calculatePricingQuote(
        venueId: String,
        resourceId: String,
        date: String,
        slots: List<String>,
        promoCode: String? = null
    ): Result<PriceBreakdown> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = linkedMapOf<String, Any?>(
                "venueId" to venueId,
                "resourceId" to resourceId,
                "date" to date,
                "slots" to slots
            )
            if (!promoCode.isNullOrBlank()) {
                payload["promoCode"] = promoCode.trim()
            }
            val raw = httpRequest(buildUrl("/api/v1/pricing/quote"), "POST", ApexJson.encode(payload))
            val data = extractDataObject(raw)
            parsePriceBreakdown(data)
        }
    }

    // =========================================================================
    // Booking Lifecycle & Passes APIs
    // =========================================================================

    suspend fun initiateBooking(
        venueId: String,
        resourceId: String,
        bookingDate: String,
        slotStartTimes: List<String>,
        sportType: String? = null,
        promoCode: String? = null
    ): Result<BookingInitiationResult> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = linkedMapOf<String, Any?>(
                "venueId" to venueId,
                "resourceId" to resourceId,
                "bookingDate" to bookingDate,
                "slotStartTimes" to slotStartTimes
            )
            if (!sportType.isNullOrBlank()) payload["sportType"] = sportType
            if (!promoCode.isNullOrBlank()) payload["promoCode"] = promoCode.trim()

            val raw = httpRequest(buildUrl("/api/v1/bookings/initiate"), "POST", ApexJson.encode(payload))
            val data = extractDataObject(raw)
            val quoteObj = data.obj("quote")?.let { parsePriceBreakdown(it) }
            BookingInitiationResult(
                bookingId = data.str("bookingId") ?: "",
                razorpayOrderId = data.str("razorpayOrderId") ?: "",
                amountPaise = data.lng("amountPaise", quoteObj?.totalPayablePaise ?: 0L),
                currency = data.str("currency") ?: "INR",
                razorpayKeyId = data.str("razorpayKeyId"),
                holdExpiresAtMs = data.lng("holdExpiresAt", System.currentTimeMillis() + 600_000L),
                quote = quoteObj
            )
        }
    }

    suspend fun verifyBookingPayment(
        bookingId: String,
        razorpayOrderId: String,
        razorpayPaymentId: String,
        razorpaySignature: String
    ): Result<BookingPass> = withContext(Dispatchers.IO) {
        runCatching {
            val body = ApexJson.encode(
                mapOf(
                    "razorpayOrderId" to razorpayOrderId,
                    "razorpayPaymentId" to razorpayPaymentId,
                    "razorpaySignature" to razorpaySignature
                )
            )
            val raw = httpRequest(buildUrl("/api/v1/bookings/${encodeQuery(bookingId)}/verify-payment"), "POST", body)
            val data = extractDataObject(raw)
            parseBookingPass(data)
        }
    }

    suspend fun fetchMyBookings(): Result<List<BookingPass>> = withContext(Dispatchers.IO) {
        runCatching {
            val raw = httpRequest(buildUrl("/api/v1/bookings/my"), "GET")
            val dataList = extractDataArray(raw)
            dataList.mapNotNull { item ->
                @Suppress("UNCHECKED_CAST")
                (item as? Map<String, Any?>)?.let { parseBookingPass(it) }
            }
        }
    }

    suspend fun cancelBooking(
        bookingId: String,
        reason: String = "Player requested cancellation"
    ): Result<BookingPass> = withContext(Dispatchers.IO) {
        runCatching {
            val body = ApexJson.encode(mapOf("reason" to reason))
            val raw = httpRequest(buildUrl("/api/v1/bookings/${encodeQuery(bookingId)}/cancel"), "POST", body)
            val data = extractDataObject(raw)
            parseBookingPass(data)
        }
    }

    // =========================================================================
    // Gate Scanner, Walk-In & Owner Facilities APIs
    // =========================================================================

    suspend fun verifyGatePass(qrPassToken: String): Result<GateVerifyResult> = withContext(Dispatchers.IO) {
        runCatching {
            val body = ApexJson.encode(mapOf("qrPassToken" to qrPassToken.trim()))
            val raw = httpRequest(buildUrl("/api/v1/gate/verify-pass"), "POST", body)
            val data = extractDataObject(raw)
            val valid = data.bool("valid", data.bool("success", false))
            val alreadyCheckedIn = data.bool("alreadyCheckedIn", false)
            val message = data.str("message") ?: if (valid) "Pass verified!" else "Invalid pass"
            val booking = data.obj("booking")?.let { parseBookingPass(it) }
            GateVerifyResult(
                valid = valid,
                alreadyCheckedIn = alreadyCheckedIn,
                message = message,
                booking = booking
            )
        }
    }

    suspend fun createWalkInBooking(
        courtId: String,
        date: String,
        slot: String,
        customerName: String,
        customerPhone: String,
        amountCollectedRupees: Int
    ): Result<BookingPass> = withContext(Dispatchers.IO) {
        runCatching {
            val body = ApexJson.encode(
                mapOf(
                    "courtId" to courtId,
                    "date" to date,
                    "slot" to slot,
                    "customerName" to customerName,
                    "customerPhone" to customerPhone,
                    "amountCollected" to amountCollectedRupees.coerceAtLeast(100)
                )
            )
            val raw = httpRequest(buildUrl("/api/v1/gate/walk-in"), "POST", body)
            val data = extractDataObject(raw)
            parseBookingPass(data)
        }
    }

    suspend fun createFacility(
        venueId: String,
        name: String,
        sport: String,
        capacity: Int,
        surface: String,
        dimensions: String,
        baseHourlyRateRupees: Int,
        peakHourlyRateRupees: Int,
        hasFloodlights: Boolean = true,
        amenities: List<String> = emptyList()
    ): Result<Court> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = linkedMapOf<String, Any?>(
                "venueId" to venueId,
                "name" to name,
                "sport" to sport,
                "facilityType" to sport,
                "capacity" to capacity.coerceAtLeast(1),
                "surface" to surface,
                "dimensions" to dimensions,
                "baseHourlyRate" to baseHourlyRateRupees.coerceAtLeast(200),
                "peakHourlyRate" to peakHourlyRateRupees.coerceAtLeast(200),
                "hasFloodlights" to hasFloodlights,
                "amenities" to amenities
            )
            val raw = httpRequest(buildUrl("/api/v1/facilities"), "POST", ApexJson.encode(payload))
            val data = extractDataObject(raw)
            parseFacilityAsCourt(data)
        }
    }

    // =========================================================================
    // JSON Mapping Helpers (tested offline in JVM unit tests)
    // =========================================================================

    internal fun parseAndStoreAuth(rawJson: String): AuthResult {
        val data = extractDataObject(rawJson)
        val token = data.str("token") ?: ""
        val userMap = data.obj("user") ?: emptyMap()
        val roleStr = userMap.str("role") ?: "ROLE_PLAYER"
        val role = if (roleStr.contains("OWNER", ignoreCase = true)) {
            UserRole.ROLE_OWNER
        } else {
            UserRole.ROLE_PLAYER
        }

        val uid = userMap.str("uid") ?: "usr_remote"
        val session = UserSession(
            uid = uid,
            name = userMap.str("fullName") ?: userMap.str("displayName") ?: "Apex Player",
            email = userMap.str("email") ?: "",
            phone = userMap.str("phone") ?: "",
            role = role,
            loyaltyPoints = userMap.intVal("loyaltyPoints", 100),
            isGuest = false,
            jwtToken = token.ifBlank { null }
        )

        if (token.isNotBlank()) {
            authToken = token
        }
        activeUserId = uid
        return AuthResult(token = token, user = session)
    }

    internal fun parseVenue(map: Map<String, Any?>): Venue {
        val rawCity = map.str("city") ?: "Thane"
        val titleCity = rawCity.split(" ").joinToString(" ") { part ->
            part.replaceFirstChar { ch -> if (ch.isLowerCase()) ch.titlecase() else ch.toString() }
        }
        val sportStrings = map.strList("sportTypes").ifEmpty { map.strList("sports") }
        val sports = sportStrings.map { SportType.fromApiName(it) }.distinct().ifEmpty {
            listOf(SportType.CRICKET_TURF)
        }
        val minPrice = map.lng("minSlotPricePaise", map.lng("startingPrice", 800L) * 100L)
        val locality = map.str("locality") ?: map.str("area") ?: "Central"
        val badge = if (map.bool("hasActiveDiscounts", false)) "⚡ OFFERS LIVE" else null

        return Venue(
            id = map.str("venueId") ?: map.str("id") ?: "ven_remote",
            name = map.str("name") ?: "Apex Arena",
            area = locality,
            city = titleCity,
            rating = map.dbl("rating", 4.8),
            reviewCount = map.intVal("reviewCount", 120),
            distanceKm = map.dbl("distanceKm", 2.4),
            startingPricePaise = minPrice,
            supportedSports = sports,
            address = map.str("addressLine") ?: map.str("address") ?: "$locality, $titleCity",
            amenities = map.strList("amenities"),
            badge = badge
        )
    }

    internal fun parseResourceAsCourt(map: Map<String, Any?>): Court {
        val sportStrings = map.strList("sportTypes")
        val primarySport = SportType.fromApiName(sportStrings.firstOrNull())
        val basePaise = map.lng("basePricePaise", 100000L)
        val peakPaise = map.lng("peakHourPricePaise", (basePaise * 130L) / 100L)
        val isIndoor = map.bool("indoor", map.bool("isIndoor", false))

        return Court(
            id = map.str("resourceId") ?: map.str("id") ?: "res_remote",
            venueId = map.str("venueId") ?: "",
            name = map.str("name") ?: "Playable Unit",
            sportType = primarySport,
            capacity = map.intVal("maxPlayers", 10),
            surfaceType = map.str("surfaceType") ?: "Synthetic Turf",
            isIndoor = isIndoor,
            dimensions = map.str("dimensions") ?: "100 x 60 ft",
            baseHourlyPricePaise = basePaise,
            peakHourlyPricePaise = peakPaise
        )
    }

    internal fun parseFacilityAsCourt(map: Map<String, Any?>): Court {
        val baseRupees = map.lng("baseHourlyRate", 1000L)
        val peakRupees = map.lng("peakHourlyRate", (baseRupees * 130L) / 100L)
        return Court(
            id = map.str("courtId") ?: map.str("id") ?: "crt_remote",
            venueId = map.str("venueId") ?: "",
            name = map.str("name") ?: "New Court",
            sportType = SportType.fromApiName(map.str("sport") ?: map.str("facilityType")),
            capacity = map.intVal("capacity", 10),
            surfaceType = map.str("surface") ?: "Synthetic Turf",
            isIndoor = !(map.bool("hasFloodlights", true)),
            dimensions = map.str("dimensions") ?: "100 x 60 ft",
            baseHourlyPricePaise = baseRupees * 100L,
            peakHourlyPricePaise = peakRupees * 100L
        )
    }

    internal fun parseTimeSlot(map: Map<String, Any?>, resourceId: String, date: String): TimeSlot {
        val start = map.str("startTime") ?: "06:00"
        val end = map.str("endTime") ?: "07:00"
        val basePaise = map.lng("originalPricePaise", map.lng("finalPricePaise", 80000L))
        val finalPaise = map.lng("finalPricePaise", basePaise)
        val statusRaw = (map.str("status") ?: "AVAILABLE").uppercase()
        val status = when (statusRaw) {
            "HELD" -> SlotStatus.HELD
            "BOOKED", "BLOCKED" -> SlotStatus.BOOKED
            "PAST" -> SlotStatus.PAST
            else -> SlotStatus.AVAILABLE
        }
        val isPeak = map.bool("isPeak", map.bool("peak", false))
        val isOffPeak = finalPaise < basePaise || !(map.str("discountInfo").isNullOrBlank())

        return TimeSlot(
            id = "${resourceId}_${date}_$start",
            courtId = map.str("resourceId") ?: resourceId,
            date = map.str("date") ?: date,
            startTime = start,
            endTime = end,
            basePricePaise = basePaise,
            finalPricePaise = finalPaise,
            status = status,
            isOffPeak = isOffPeak,
            isPeak = isPeak
        )
    }

    internal fun parsePriceBreakdown(map: Map<String, Any?>): PriceBreakdown {
        val subtotal = map.lng("subtotalPaise", 0L)
        val discount = map.lng("discountAmountPaise", 0L)
        val tax = map.lng("taxPaise", 0L)
        val finalPayable = map.lng("finalPayablePaise", (subtotal - discount + tax).coerceAtLeast(100L))
        val promoApplied = map.str("appliedDiscountDescription") ?: map.str("discountCode")

        return PriceBreakdown(
            baseCourtPaise = subtotal,
            offPeakDiscountPaise = 0L,
            peakSurgePaise = 0L,
            promoDiscountPaise = discount,
            convenienceFeePaise = tax,
            totalPayablePaise = finalPayable,
            promoCodeApplied = promoApplied
        )
    }

    internal fun parseBookingPass(map: Map<String, Any?>): BookingPass {
        val slots = map.arr("slots").orEmpty().mapNotNull {
            @Suppress("UNCHECKED_CAST")
            it as? Map<String, Any?>
        }
        val timeRange = if (slots.isNotEmpty()) {
            val firstStart = slots.first().str("startTime") ?: "06:00"
            val lastEnd = slots.last().str("endTime") ?: "07:00"
            "$firstStart - $lastEnd"
        } else {
            map.str("timeSlotRange") ?: "18:00 - 19:00"
        }

        val statusStr = (map.str("status") ?: "CONFIRMED").uppercase()
        val status = when {
            statusStr.startsWith("CANCEL") -> BookingStatus.CANCELLED
            statusStr == "CHECKED_IN" || statusStr == "COMPLETED" -> BookingStatus.CHECKED_IN
            else -> BookingStatus.CONFIRMED
        }

        val bookingId = map.str("bookingId") ?: map.str("id") ?: "bk_remote"
        val bookingCode = map.str("bookingCode") ?: bookingId
        val qrToken = map.str("qrPassToken") ?: "APEX-PASS-$bookingCode"

        return BookingPass(
            id = bookingId,
            bookingCode = bookingCode,
            venueName = map.str("venueName") ?: "Apex Arena",
            courtName = map.str("resourceName") ?: map.str("courtName") ?: "Turf Court",
            sportType = SportType.fromApiName(map.str("sportType")),
            date = map.str("bookingDate") ?: "",
            timeSlotRange = timeRange,
            amountPaidPaise = map.lng("finalPayablePaise", 0L),
            status = status,
            createdAtMs = map.lng("createdAt", System.currentTimeMillis()),
            refundAmountPaise = map.lng("refundAmountPaise", 0L),
            cancellationTier = map.str("cancellationReason"),
            qrPassToken = qrToken
        )
    }

    private fun extractDataObject(rawJson: String): Map<String, Any?> {
        val root = ApexJson.parseObject(rawJson)
        if (root.containsKey("success") && !root.bool("success", true)) {
            throw RuntimeException(root.str("message") ?: "API request failed")
        }
        return root.obj("data") ?: root
    }

    private fun extractDataArray(rawJson: String): List<Any?> {
        val parsed = ApexJson.parse(rawJson)
        return when (parsed) {
            is List<*> -> parsed
            is Map<*, *> -> {
                @Suppress("UNCHECKED_CAST")
                val root = parsed as Map<String, Any?>
                if (root.containsKey("success") && !root.bool("success", true)) {
                    throw RuntimeException(root.str("message") ?: "API request failed")
                }
                root.arr("data") ?: emptyList()
            }
            else -> emptyList()
        }
    }

    // =========================================================================
    // HTTP Transport Layer (JWT Bearer + X-User-Id / X-Owner-Id Injection)
    // =========================================================================

    private fun openConnection(urlStr: String, method: String): HttpURLConnection {
        val url = URL(urlStr)
        return (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = DEFAULT_TIMEOUT_MS
            readTimeout = DEFAULT_TIMEOUT_MS
            requestMethod = method
            setRequestProperty("Accept", "application/json")
            if (method == "POST" || method == "PUT") {
                setRequestProperty("Content-Type", "application/json")
            }
            val token = authToken
            if (!token.isNullOrBlank()) {
                setRequestProperty("Authorization", "Bearer $token")
            }
            val uid = activeUserId
            if (!uid.isNullOrBlank() && uid != "guest_001") {
                setRequestProperty("X-User-Id", uid)
                setRequestProperty("X-Owner-Id", uid)
            }
        }
    }

    private fun httpRequest(urlStr: String, method: String, body: String? = null): String {
        val conn = openConnection(urlStr, method)
        if (body != null && (method == "POST" || method == "PUT")) {
            conn.doOutput = true
            OutputStreamWriter(conn.outputStream, StandardCharsets.UTF_8).use { writer ->
                writer.write(body)
                writer.flush()
            }
        }

        val code = conn.responseCode
        val isSuccess = code in 200..299
        val stream = (if (isSuccess) conn.inputStream else conn.errorStream) ?: conn.inputStream
        val responseBody = if (stream != null) {
            BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8)).use { reader ->
                reader.readText()
            }
        } else {
            ""
        }
        conn.disconnect()

        if (!isSuccess) {
            val errorMsg = try {
                ApexJson.parseObject(responseBody).str("message")
            } catch (_: Exception) {
                null
            }
            throw RuntimeException(errorMsg ?: "HTTP $code: $responseBody")
        }
        return responseBody
    }
}

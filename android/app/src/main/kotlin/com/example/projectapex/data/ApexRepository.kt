package com.example.projectapex.data

import com.example.projectapex.model.*
import com.example.projectapex.network.ApexApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class ApexRepository(
    val apiClient: ApexApiClient = ApexApiClient(),
    checkOnInit: Boolean = true
) {
    private val scope = CoroutineScope(Dispatchers.Default)

    // Current User Session (Default Guest)
    private val _currentUser = MutableStateFlow(
        UserSession(
            uid = "guest_001",
            name = "Guest Player",
            email = "guest@projectapex.in",
            phone = "+91 98200 12345",
            role = UserRole.ROLE_PLAYER,
            loyaltyPoints = 50,
            isGuest = true,
            jwtToken = null
        )
    )
    val currentUser: StateFlow<UserSession> = _currentUser.asStateFlow()

    // City Selection
    private val _selectedCity = MutableStateFlow("Thane")
    val selectedCity: StateFlow<String> = _selectedCity.asStateFlow()

    // Sport Filter
    private val _selectedSport = MutableStateFlow<SportType?>(null)
    val selectedSport: StateFlow<SportType?> = _selectedSport.asStateFlow()

    // Backend Connectivity & 3-Tier Live Health
    private val _backendUrl = MutableStateFlow(apiClient.baseUrl)
    val backendUrl: StateFlow<String> = _backendUrl.asStateFlow()

    private val _isBackendConnected = MutableStateFlow(false)
    val isBackendConnected: StateFlow<Boolean> = _isBackendConnected.asStateFlow()

    private val _healthStatus = MutableStateFlow<HealthStatus?>(null)
    val healthStatus: StateFlow<HealthStatus?> = _healthStatus.asStateFlow()

    // Active Slot Hold (10-minute hold window)
    private val _activeHold = MutableStateFlow<HoldSession?>(null)
    val activeHold: StateFlow<HoldSession?> = _activeHold.asStateFlow()

    // Persistent Bookings / Passes
    private val _bookingPasses = MutableStateFlow<List<BookingPass>>(emptyList())
    val bookingPasses: StateFlow<List<BookingPass>> = _bookingPasses.asStateFlow()

    // In-memory slot locks & booked slot ids: courtId_date_slotId -> expiryMs or "BOOKED"
    private val lockedSlots = ConcurrentHashMap<String, Long>()
    private val bookedSlots = ConcurrentHashMap<String, String>()

    // Venues Data
    private val _venues = MutableStateFlow<List<Venue>>(emptyList())
    val venues: StateFlow<List<Venue>> = _venues.asStateFlow()

    // Courts Data
    private val _courts = MutableStateFlow<List<Court>>(emptyList())
    val courts: StateFlow<List<Court>> = _courts.asStateFlow()

    init {
        initializeInitialData()
        if (checkOnInit) {
            checkBackendConnectivity()
        }
    }

    private fun initializeInitialData() {
        val initialVenues = listOf(
            Venue(
                id = "ven_thane_apex",
                name = "Apex Super Arena & Turf",
                area = "Ghodbunder Road",
                city = "Thane",
                rating = 4.9,
                reviewCount = 384,
                distanceKm = 1.8,
                startingPricePaise = 79900L, // ₹799
                supportedSports = listOf(SportType.CRICKET_TURF, SportType.FOOTBALL_TURF, SportType.BADMINTON, SportType.PICKLEBALL),
                address = "Near Ovala Ground, Ghodbunder Rd, Thane West 400615",
                amenities = listOf("FIFA Grade Turf", "LED Floodlights", "BMS Sound System", "Covered Parking", "Locker & Showers"),
                badge = "⚡ FAST-FILLING"
            ),
            Venue(
                id = "ven_thane_hiranandani",
                name = "Hiranandani Sports Hub",
                area = "Hiranandani Estate",
                city = "Thane",
                rating = 4.8,
                reviewCount = 210,
                distanceKm = 3.2,
                startingPricePaise = 89900L, // ₹899
                supportedSports = listOf(SportType.BADMINTON, SportType.LAWN_TENNIS, SportType.SQUASH, SportType.POOL_TABLE),
                address = "The Walk, Hiranandani Estate, Thane West 400607",
                amenities = listOf("BWF Approved Wooden Courts", "AC Squash Arenas", "Pro Shop", "Cafe & Smoothies"),
                badge = "⭐ TOP RATED"
            ),
            Venue(
                id = "ven_mumbai_bkc",
                name = "BKC Pro Sports Complex",
                area = "Bandra Kurla Complex",
                city = "Mumbai",
                rating = 4.9,
                reviewCount = 512,
                distanceKm = 14.5,
                startingPricePaise = 149900L, // ₹1499
                supportedSports = listOf(SportType.CRICKET_TURF, SportType.FOOTBALL_TURF, SportType.PICKLEBALL, SportType.MULTI_SPORT),
                address = "G Block, BKC, Bandra East, Mumbai 400051",
                amenities = listOf("7v7 Pro Arena", "Tournament Lighting", "Live Stream Cameras", "Valet Parking"),
                badge = "🏆 PREMIER"
            ),
            Venue(
                id = "ven_navi_vashi",
                name = "Vashi Sky Turf & Badminton",
                area = "Sector 17, Vashi",
                city = "Navi Mumbai",
                rating = 4.7,
                reviewCount = 175,
                distanceKm = 18.0,
                startingPricePaise = 69900L, // ₹699
                supportedSports = listOf(SportType.FOOTBALL_TURF, SportType.BADMINTON, SportType.POOL_TABLE),
                address = "Rooftop, Inorbit Mall Area, Vashi, Navi Mumbai 400703",
                amenities = listOf("Rooftop Skyline View", "Dual Turf", "Chilled Dugouts", "Steam & Showers"),
                badge = "🔥 POPULAR"
            )
        )
        _venues.value = initialVenues

        val initialCourts = listOf(
            Court(
                id = "crt_box_cricket_1",
                venueId = "ven_thane_apex",
                name = "Pitch 1: Box Cricket Arena",
                sportType = SportType.CRICKET_TURF,
                capacity = 14,
                surfaceType = "Monofilament 50mm AstroTurf",
                isIndoor = false,
                dimensions = "110 x 55 ft",
                baseHourlyPricePaise = 80000L, // ₹800
                peakHourlyPricePaise = 120000L // ₹1200
            ),
            Court(
                id = "crt_football_1",
                venueId = "ven_thane_apex",
                name = "Turf A: 5-a-Side Football",
                sportType = SportType.FOOTBALL_TURF,
                capacity = 10,
                surfaceType = "FIFA Quality Pro Synthetic Turf",
                isIndoor = false,
                dimensions = "100 x 60 ft",
                baseHourlyPricePaise = 100000L, // ₹1000
                peakHourlyPricePaise = 150000L  // ₹1500
            ),
            Court(
                id = "crt_badminton_1",
                venueId = "ven_thane_apex",
                name = "Court 1: BWF Vinyl Badminton",
                sportType = SportType.BADMINTON,
                capacity = 4,
                surfaceType = "BWF Approved Shock Absorption Vinyl",
                isIndoor = true,
                dimensions = "44 x 20 ft",
                baseHourlyPricePaise = 45000L,  // ₹450
                peakHourlyPricePaise = 70000L   // ₹700
            ),
            Court(
                id = "crt_pickleball_1",
                venueId = "ven_thane_apex",
                name = "Pickle Court 1: Speed Play",
                sportType = SportType.PICKLEBALL,
                capacity = 4,
                surfaceType = "Textured Acrylic Pro Surface",
                isIndoor = false,
                dimensions = "44 x 20 ft",
                baseHourlyPricePaise = 50000L,  // ₹500
                peakHourlyPricePaise = 75000L   // ₹750
            ),
            Court(
                id = "crt_tennis_hiranandani",
                venueId = "ven_thane_hiranandani",
                name = "Centre Court: Lawn Tennis",
                sportType = SportType.LAWN_TENNIS,
                capacity = 4,
                surfaceType = "DecoTurf Multi-Layer Hard Court",
                isIndoor = false,
                dimensions = "78 x 36 ft",
                baseHourlyPricePaise = 90000L,  // ₹900
                peakHourlyPricePaise = 130000L  // ₹1300
            ),
            Court(
                id = "crt_squash_hiranandani",
                venueId = "ven_thane_hiranandani",
                name = "Squash Arena Alpha (AC)",
                sportType = SportType.SQUASH,
                capacity = 2,
                surfaceType = "Armourcoat Plaster & Maple Wood Floor",
                isIndoor = true,
                dimensions = "32 x 21 ft",
                baseHourlyPricePaise = 70000L,  // ₹700
                peakHourlyPricePaise = 95000L   // ₹950
            )
        )
        _courts.value = initialCourts

        // Sample initial pass
        val samplePass = BookingPass(
            id = "bk_init_001",
            bookingCode = "ARENA-7391K",
            venueName = "Apex Super Arena & Turf",
            courtName = "Pitch 1: Box Cricket Arena",
            sportType = SportType.CRICKET_TURF,
            date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
            timeSlotRange = "19:00 - 20:00",
            amountPaidPaise = 120000L,
            status = BookingStatus.CONFIRMED,
            createdAtMs = System.currentTimeMillis() - 3600000
        )
        _bookingPasses.value = listOf(samplePass)
        bookedSlots["crt_box_cricket_1_${samplePass.date}_19:00"] = samplePass.id
    }

    fun selectCity(city: String) {
        _selectedCity.value = city
    }

    fun selectSport(sport: SportType?) {
        _selectedSport.value = sport
    }

    fun checkBackendConnectivity(customUrl: String? = null) {
        scope.launch {
            checkBackendConnectivityAndAwait(customUrl)
        }
    }

    suspend fun checkBackendConnectivityAndAwait(customUrl: String? = null): HealthStatus? {
        val rawCandidate = customUrl ?: _backendUrl.value
        val normalized = ApexApiClient.normalizeBaseUrl(rawCandidate)
        val urlToTest = normalized.ifEmpty { ApexApiClient.RENDER_PROD_URL }

        if (customUrl != null && normalized.isNotEmpty()) {
            _backendUrl.value = normalized
            apiClient.baseUrl = normalized
        }

        val health = apiClient.checkHealth(urlToTest)
        if (health != null && health.isOnline) {
            _healthStatus.value = health
            _isBackendConnected.value = true
            _backendUrl.value = urlToTest
            apiClient.baseUrl = urlToTest
            return health
        }

        // Graceful fallback check for /api/v1/sports
        val sportsAlive = apiClient.pingBackend(urlToTest)
        _isBackendConnected.value = sportsAlive
        val fallbackHealth = if (sportsAlive) {
            HealthStatus(
                status = "UP",
                service = "project-apex-backend",
                hosting = if (urlToTest.contains("10.0.2.2") || urlToTest.contains("localhost")) "Localhost Dev" else "Render",
                runtime = "Java 21 LTS + Spring Boot 3.2.x",
                database = "Neon PostgreSQL",
                dbConnected = true,
                url = urlToTest,
                isOnline = true
            )
        } else {
            HealthStatus(
                status = "STANDBY",
                service = "project-apex-backend",
                hosting = if (urlToTest.contains("10.0.2.2") || urlToTest.contains("localhost")) "Localhost Dev" else "Render",
                runtime = "Java 21 LTS + Spring Boot 3.2.x",
                database = "Neon PostgreSQL",
                dbConnected = false,
                url = urlToTest,
                isOnline = false
            )
        }
        _healthStatus.value = fallbackHealth
        return fallbackHealth
    }

    // =========================================================================
    // Live 3-Tier Backend Sync (Venues, Courts, Slots, Bookings)
    // =========================================================================

    suspend fun syncWithBackend() {
        syncVenuesFromBackend()
        if (!apiClient.authToken.isNullOrBlank()) {
            syncBookingsFromBackend()
        }
    }

    suspend fun syncVenuesFromBackend(): Boolean {
        val venuesResult = apiClient.fetchVenues()
        val remoteVenues = venuesResult.getOrNull() ?: return false
        if (remoteVenues.isEmpty()) return false

        val collectedCourts = mutableListOf<Court>()
        for (v in remoteVenues) {
            val detail = apiClient.fetchVenueDetail(v.id).getOrNull()
            if (detail != null) {
                collectedCourts.addAll(detail.second)
            }
        }

        _venues.value = remoteVenues
        if (collectedCourts.isNotEmpty()) {
            _courts.value = collectedCourts
        }
        return true
    }

    suspend fun syncBookingsFromBackend(): Boolean {
        if (apiClient.authToken.isNullOrBlank()) return false
        val remoteBookings = apiClient.fetchMyBookings().getOrNull() ?: return false
        _bookingPasses.value = remoteBookings
        return true
    }

    suspend fun loadSlotsForDate(courtId: String, dateStr: String): List<TimeSlot> {
        if (_isBackendConnected.value) {
            val remote = apiClient.fetchAvailability(courtId, dateStr).getOrNull()
            if (!remote.isNullOrEmpty()) {
                return remote
            }
        }
        return getDynamicSlotsForDate(courtId, dateStr)
    }

    // Dynamic Slot Availability (Zero Pre-Generation Engine)
    fun getDynamicSlotsForDate(courtId: String, dateStr: String): List<TimeSlot> {
        val court = _courts.value.find { it.id == courtId } ?: return emptyList()
        val slots = mutableListOf<TimeSlot>()

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateObj = try { sdf.parse(dateStr) } catch (_: Exception) { Date() }
        val cal = Calendar.getInstance().apply { time = dateObj ?: Date() }
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val isWeekend = (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY)

        val todayStr = sdf.format(Date())
        val isToday = (todayStr == dateStr)
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        // Operating hours: 06:00 to 23:00 (1-hour slots)
        for (hour in 6..22) {
            val startHourStr = String.format(Locale.getDefault(), "%02d:00", hour)
            val endHourStr = String.format(Locale.getDefault(), "%02d:00", hour + 1)
            val slotId = "${courtId}_${dateStr}_$startHourStr"

            // Pricing rules:
            // Off-peak: Weekdays before 17:00 (20% off base)
            // Peak: Weekends or 18:00-22:00
            val isOffPeak = !isWeekend && (hour < 17)
            val isPeak = isWeekend || (hour >= 18)

            val basePaise = court.baseHourlyPricePaise
            val finalPaise = when {
                isOffPeak -> (basePaise * 80L) / 100L // 20% discount
                isPeak -> court.peakHourlyPricePaise
                else -> basePaise
            }

            // Status determination
            val status = when {
                isToday && hour <= currentHour -> SlotStatus.PAST
                bookedSlots.containsKey(slotId) -> SlotStatus.BOOKED
                lockedSlots.containsKey(slotId) && lockedSlots[slotId]!! > System.currentTimeMillis() -> SlotStatus.HELD
                else -> SlotStatus.AVAILABLE
            }

            slots.add(
                TimeSlot(
                    id = slotId,
                    courtId = courtId,
                    date = dateStr,
                    startTime = startHourStr,
                    endTime = endHourStr,
                    basePricePaise = basePaise,
                    finalPricePaise = finalPaise,
                    status = status,
                    isOffPeak = isOffPeak,
                    isPeak = isPeak
                )
            )
        }
        return slots
    }

    // 10-Minute Hold Session
    fun createHoldSession(
        venue: Venue,
        court: Court,
        dateStr: String,
        selectedSlots: List<TimeSlot>
    ): HoldSession {
        val holdId = "hold_" + UUID.randomUUID().toString().take(8)
        val expiry = System.currentTimeMillis() + (10 * 60 * 1000L) // 10 minutes

        var total = 0L
        for (s in selectedSlots) {
            lockedSlots[s.id] = expiry
            total += s.finalPricePaise
        }

        val session = HoldSession(
            holdId = holdId,
            venueId = venue.id,
            venueName = venue.name,
            courtId = court.id,
            courtName = court.name,
            sportType = court.sportType,
            date = dateStr,
            slots = selectedSlots,
            expiresAtTimestampMs = expiry,
            totalPaise = total
        )
        _activeHold.value = session
        return session
    }

    fun releaseActiveHold() {
        val hold = _activeHold.value ?: return
        for (s in hold.slots) {
            lockedSlots.remove(s.id)
        }
        _activeHold.value = null
    }

    // Price Quote & Breakdown (integer paise)
    fun calculateQuote(hold: HoldSession, promoCode: String?): PriceBreakdown {
        var baseTotal = 0L
        var offPeakDiscount = 0L
        var peakSurge = 0L

        for (s in hold.slots) {
            baseTotal += s.basePricePaise
            if (s.isOffPeak) {
                offPeakDiscount += (s.basePricePaise - s.finalPricePaise)
            } else if (s.isPeak) {
                peakSurge += (s.finalPricePaise - s.basePricePaise)
            }
        }

        var promoDiscount = 0L
        var appliedPromo: String? = null
        val normalizedCode = promoCode?.trim()?.uppercase()

        if (normalizedCode == "WELCOME50" || normalizedCode == "THANE50") {
            promoDiscount = 5000L // ₹50 off = 5000 paise
            appliedPromo = "$normalizedCode (-₹50)"
        } else if (normalizedCode == "APEX20" || normalizedCode == "TURF20") {
            promoDiscount = ((baseTotal + peakSurge - offPeakDiscount) * 20L) / 100L
            if (promoDiscount > 15000L) promoDiscount = 15000L // Cap at ₹150
            appliedPromo = "$normalizedCode (20% off)"
        }

        val subtotal = (baseTotal - offPeakDiscount + peakSurge - promoDiscount).coerceAtLeast(10000L) // Min 100 INR rule
        val convenienceFee = (subtotal * 2L) / 100L // 2% convenience fee in paise
        val totalPayable = subtotal + convenienceFee

        return PriceBreakdown(
            baseCourtPaise = baseTotal,
            offPeakDiscountPaise = offPeakDiscount,
            peakSurgePaise = peakSurge,
            promoDiscountPaise = promoDiscount,
            convenienceFeePaise = convenienceFee,
            totalPayablePaise = totalPayable,
            promoCodeApplied = appliedPromo
        )
    }

    suspend fun calculateQuoteWithBackend(hold: HoldSession, promoCode: String?): PriceBreakdown {
        if (_isBackendConnected.value) {
            val remote = apiClient.calculatePricingQuote(
                venueId = hold.venueId,
                resourceId = hold.courtId,
                date = hold.date,
                slots = hold.slots.map { it.startTime },
                promoCode = promoCode
            ).getOrNull()
            if (remote != null) {
                return remote
            }
        }
        return calculateQuote(hold, promoCode)
    }

    // Confirm Booking & Generate Match Pass with QR Code (Local / Unit-Test Synchronous)
    fun confirmBooking(hold: HoldSession, quote: PriceBreakdown): BookingPass {
        val randomSuffix = (1000..9999).random().toString() + ('A'..'Z').random()
        val bookingCode = "ARENA-$randomSuffix"

        val timeRange = if (hold.slots.size == 1) {
            hold.slots.first().formattedTime
        } else {
            "${hold.slots.first().startTime} - ${hold.slots.last().endTime}"
        }

        val pass = BookingPass(
            id = "bk_${UUID.randomUUID().toString().take(10)}",
            bookingCode = bookingCode,
            venueName = hold.venueName,
            courtName = hold.courtName,
            sportType = hold.sportType,
            date = hold.date,
            timeSlotRange = timeRange,
            amountPaidPaise = quote.totalPayablePaise,
            status = BookingStatus.CONFIRMED,
            createdAtMs = System.currentTimeMillis()
        )

        // Mark slots as booked
        for (s in hold.slots) {
            lockedSlots.remove(s.id)
            bookedSlots[s.id] = pass.id
        }

        _bookingPasses.value = listOf(pass) + _bookingPasses.value
        _activeHold.value = null
        return pass
    }

    suspend fun confirmBookingWithBackend(
        hold: HoldSession,
        quote: PriceBreakdown,
        promoCode: String? = null
    ): BookingPass {
        if (_isBackendConnected.value && !apiClient.authToken.isNullOrBlank()) {
            val initResult = apiClient.initiateBooking(
                venueId = hold.venueId,
                resourceId = hold.courtId,
                bookingDate = hold.date,
                slotStartTimes = hold.slots.map { it.startTime },
                sportType = hold.sportType.name,
                promoCode = promoCode
            ).getOrNull()

            if (initResult != null && initResult.bookingId.isNotBlank()) {
                val paymentId = "pay_mobile_" + UUID.randomUUID().toString().replace("-", "").take(10)
                val verified = apiClient.verifyBookingPayment(
                    bookingId = initResult.bookingId,
                    razorpayOrderId = initResult.razorpayOrderId,
                    razorpayPaymentId = paymentId,
                    razorpaySignature = "SIMULATED_TEST_SIGNATURE"
                ).getOrNull()

                if (verified != null) {
                    for (s in hold.slots) {
                        lockedSlots.remove(s.id)
                        bookedSlots[s.id] = verified.id
                    }
                    _bookingPasses.value = listOf(verified) + _bookingPasses.value.filterNot { it.id == verified.id }
                    _activeHold.value = null
                    return verified
                }
            }
        }
        return confirmBooking(hold, quote)
    }

    // 3-Tier Cancellation Policy:
    // > 24 hours: 100% refund minus ₹50 fee
    // 6 to 24 hours: 50% refund
    // < 6 hours: 0% refund
    fun cancelBooking(passId: String): Pair<Boolean, String> {
        val pass = _bookingPasses.value.find { it.id == passId }
            ?: return Pair(false, "Pass not found")

        if (pass.status != BookingStatus.CONFIRMED) {
            return Pair(false, "Booking is already ${pass.status}")
        }

        // Calculate hours until match
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val slotStart = pass.timeSlotRange.split("-").first().trim()
        val matchDateTime = try {
            sdf.parse("${pass.date} $slotStart")
        } catch (_: Exception) { Date() }

        val diffMs = (matchDateTime?.time ?: System.currentTimeMillis()) - System.currentTimeMillis()
        val diffHours = diffMs / (1000 * 60 * 60)

        val refundPaise: Long
        val tierDesc: String

        when {
            diffHours >= 24 -> {
                // > 24 hours: 100% minus ₹50 (5000 paise) cancellation fee
                refundPaise = (pass.amountPaidPaise - 5000L).coerceAtLeast(0L)
                tierDesc = ">24 hrs before match: 100% refund (minus ₹50 processing fee)"
            }
            diffHours in 6..23 -> {
                // 6-24 hours: 50% refund
                refundPaise = pass.amountPaidPaise / 2
                tierDesc = "6-24 hrs before match: 50% refund"
            }
            else -> {
                // < 6 hours: 0% refund
                refundPaise = 0L
                tierDesc = "<6 hrs before match: Non-refundable tier"
            }
        }

        val updatedPass = pass.copy(
            status = BookingStatus.CANCELLED,
            refundAmountPaise = refundPaise,
            cancellationTier = tierDesc
        )

        _bookingPasses.value = _bookingPasses.value.map {
            if (it.id == passId) updatedPass else it
        }

        // Free up slots
        val toRemove = bookedSlots.filterValues { it == passId }.keys
        toRemove.forEach { bookedSlots.remove(it) }

        val refundRs = refundPaise / 100
        return Pair(true, "Cancelled successfully. Refund ₹$refundRs initiated via original payment method ($tierDesc).")
    }

    suspend fun cancelBookingWithBackend(
        passId: String,
        reason: String = "Player requested cancellation"
    ): Pair<Boolean, String> {
        if (_isBackendConnected.value && !apiClient.authToken.isNullOrBlank()) {
            val res = apiClient.cancelBooking(passId, reason)
            if (res.isSuccess) {
                val cancelled = res.getOrThrow()
                _bookingPasses.value = _bookingPasses.value.map {
                    if (it.id == passId) cancelled else it
                }
                val toRemove = bookedSlots.filterValues { it == passId }.keys
                toRemove.forEach { bookedSlots.remove(it) }
                return Pair(true, "Cancelled on server. Refund ₹${cancelled.refundAmountRupees} processed.")
            }
        }
        return cancelBooking(passId)
    }

    // Facility Owner Portal Actions
    fun addCourt(
        venueId: String,
        name: String,
        sportType: SportType,
        capacity: Int,
        surfaceType: String,
        isIndoor: Boolean,
        dimensions: String,
        basePricePaise: Long,
        peakPricePaise: Long
    ): Court {
        val court = Court(
            id = "crt_cust_" + UUID.randomUUID().toString().take(6),
            venueId = venueId,
            name = name,
            sportType = sportType,
            capacity = capacity,
            surfaceType = surfaceType,
            isIndoor = isIndoor,
            dimensions = dimensions,
            baseHourlyPricePaise = basePricePaise,
            peakHourlyPricePaise = peakPricePaise
        )
        _courts.value = _courts.value + court
        return court
    }

    suspend fun addCourtWithBackend(
        venueId: String,
        name: String,
        sportType: SportType,
        capacity: Int,
        surfaceType: String,
        isIndoor: Boolean,
        dimensions: String,
        basePricePaise: Long,
        peakPricePaise: Long
    ): Court {
        if (_isBackendConnected.value && !apiClient.authToken.isNullOrBlank()) {
            val remote = apiClient.createFacility(
                venueId = venueId,
                name = name,
                sport = sportType.name.lowercase(),
                capacity = capacity,
                surface = surfaceType,
                dimensions = dimensions,
                baseHourlyRateRupees = (basePricePaise / 100L).toInt().coerceAtLeast(200),
                peakHourlyRateRupees = (peakPricePaise / 100L).toInt().coerceAtLeast(200),
                hasFloodlights = !isIndoor
            ).getOrNull()
            if (remote != null) {
                _courts.value = _courts.value + remote
                return remote
            }
        }
        return addCourt(
            venueId = venueId,
            name = name,
            sportType = sportType,
            capacity = capacity,
            surfaceType = surfaceType,
            isIndoor = isIndoor,
            dimensions = dimensions,
            basePricePaise = basePricePaise,
            peakPricePaise = peakPricePaise
        )
    }

    fun verifyGateQr(bookingCode: String): Pair<Boolean, String> {
        val pass = _bookingPasses.value.find {
            it.bookingCode.equals(bookingCode.trim(), ignoreCase = true) ||
                (!it.qrPassToken.isNullOrBlank() && it.qrPassToken.equals(bookingCode.trim(), ignoreCase = true))
        } ?: return Pair(false, "Invalid QR code: Pass not found in system")

        if (pass.status == BookingStatus.CHECKED_IN) {
            return Pair(false, "Already Checked-In at ${pass.venueName}")
        }
        if (pass.status == BookingStatus.CANCELLED) {
            return Pair(false, "Entry Denied: Booking was cancelled")
        }

        val checkedInPass = pass.copy(status = BookingStatus.CHECKED_IN)
        _bookingPasses.value = _bookingPasses.value.map {
            if (it.id == pass.id) checkedInPass else it
        }
        return Pair(true, "Verified! Welcome ${pass.courtName} (${pass.timeSlotRange})")
    }

    suspend fun verifyGateQrWithBackend(bookingCode: String): Pair<Boolean, String> {
        if (_isBackendConnected.value && !apiClient.authToken.isNullOrBlank()) {
            val res = apiClient.verifyGatePass(bookingCode)
            if (res.isSuccess) {
                val gate = res.getOrThrow()
                if (gate.booking != null) {
                    _bookingPasses.value = _bookingPasses.value.map {
                        if (it.id == gate.booking.id || it.bookingCode.equals(gate.booking.bookingCode, ignoreCase = true)) {
                            gate.booking
                        } else it
                    }
                }
                return Pair(gate.valid, gate.message)
            }
        }
        return verifyGateQr(bookingCode)
    }

    // User Authentication (Sync local methods preserved + async backend-synced helpers)
    fun loginWithPassword(emailOrPhone: String, pass: String): Result<UserSession> {
        val role = if (emailOrPhone.contains("owner", ignoreCase = true) ||
            emailOrPhone.contains("santosh", ignoreCase = true)
        ) UserRole.ROLE_OWNER else UserRole.ROLE_PLAYER
        val user = UserSession(
            uid = "usr_" + UUID.randomUUID().toString().take(8),
            name = if (role == UserRole.ROLE_OWNER) "Rajesh Patil (Owner)" else "Pushpendra Rait",
            email = if (emailOrPhone.contains("@")) emailOrPhone else "pushpendra@projectapex.in",
            phone = if (emailOrPhone.contains("@")) "+91 98200 88990" else emailOrPhone,
            role = role,
            loyaltyPoints = 280,
            isGuest = false
        )
        _currentUser.value = user
        return Result.success(user)
    }

    suspend fun authenticateWithBackend(identifier: String, password: String): Result<UserSession> {
        if (_isBackendConnected.value) {
            val res = apiClient.loginWithPassword(identifier, password)
            if (res.isSuccess) {
                val auth = res.getOrThrow()
                _currentUser.value = auth.user
                syncBookingsFromBackend()
                return Result.success(auth.user)
            }
            val err = res.exceptionOrNull()
            if (err != null && err !is IOException && err.message?.contains("Incorrect password", ignoreCase = true) == true) {
                return Result.failure(err)
            }
        }
        return loginWithPassword(identifier, password)
    }

    suspend fun sendLoginOtpWithBackend(identifier: String): Result<OtpDispatchResult> {
        if (_isBackendConnected.value) {
            val res = apiClient.sendLoginOtp(identifier)
            if (res.isSuccess) return res
        }
        val simOtp = (100000..999999).random().toString()
        return Result.success(
            OtpDispatchResult(
                sessionId = "local_login_" + UUID.randomUUID().toString().take(6),
                expiresInSeconds = 300,
                simulatedSmsOtp = simOtp
            )
        )
    }

    suspend fun verifyLoginOtpWithBackend(sessionId: String?, phoneOrIdentifier: String, otp: String): Result<UserSession> {
        if (_isBackendConnected.value && !sessionId.isNullOrBlank() && !sessionId.startsWith("local_")) {
            val res = apiClient.verifyLoginOtp(sessionId, otp)
            if (res.isSuccess) {
                val auth = res.getOrThrow()
                _currentUser.value = auth.user
                syncBookingsFromBackend()
                return Result.success(auth.user)
            }
            val err = res.exceptionOrNull()
            if (err != null && err !is IOException) {
                return Result.failure(err)
            }
        }
        return loginWithOtp(phoneOrIdentifier, otp)
    }

    suspend fun sendRegistrationOtpWithBackend(
        phone: String,
        email: String,
        displayName: String
    ): Result<OtpDispatchResult> {
        if (_isBackendConnected.value) {
            val res = apiClient.sendRegistrationOtp(phone, email, displayName)
            if (res.isSuccess) return res
        }
        val sms = (100000..999999).random().toString()
        val mail = (100000..999999).random().toString()
        return Result.success(
            OtpDispatchResult(
                sessionId = "local_reg_" + UUID.randomUUID().toString().take(6),
                expiresInSeconds = 300,
                simulatedSmsOtp = sms,
                simulatedEmailOtp = mail
            )
        )
    }

    suspend fun registerUserWithBackend(
        name: String,
        email: String,
        phone: String,
        pass: String,
        role: UserRole,
        mobileOtp: String,
        emailOtp: String,
        sessionId: String? = null
    ): Result<UserSession> {
        if (name.isBlank() || email.isBlank() || phone.isBlank() || pass.length < 6) {
            return Result.failure(IllegalArgumentException("Please fill all mandatory fields (Password min 6 chars)"))
        }
        if (mobileOtp.length < 4 || emailOtp.length < 4) {
            return Result.failure(IllegalArgumentException("Please enter verified Mobile and Email OTPs"))
        }

        if (_isBackendConnected.value) {
            val authRes = if (!sessionId.isNullOrBlank() && !sessionId.startsWith("local_")) {
                apiClient.verifyRegistrationOtp(
                    sessionId = sessionId,
                    phoneOtp = mobileOtp,
                    emailOtp = emailOtp,
                    displayName = name,
                    phone = phone,
                    email = email,
                    role = role,
                    city = _selectedCity.value,
                    password = pass
                )
            } else {
                apiClient.registerWithPassword(
                    displayName = name,
                    email = email,
                    phone = phone,
                    password = pass,
                    city = _selectedCity.value,
                    role = role
                )
            }

            if (authRes.isSuccess) {
                val auth = authRes.getOrThrow()
                _currentUser.value = auth.user
                syncWithBackend()
                return Result.success(auth.user)
            }
        }
        return registerUser(name, email, phone, pass, role, mobileOtp, emailOtp)
    }

    fun loginWithOtp(phone: String, otp: String): Result<UserSession> {
        if (otp.length < 4) {
            return Result.failure(IllegalArgumentException("Please enter a valid 6-digit OTP"))
        }
        val user = UserSession(
            uid = "usr_otp_" + UUID.randomUUID().toString().take(6),
            name = "Player ${phone.takeLast(4)}",
            email = "player${phone.takeLast(4)}@projectapex.in",
            phone = phone,
            role = UserRole.ROLE_PLAYER,
            loyaltyPoints = 100,
            isGuest = false
        )
        _currentUser.value = user
        return Result.success(user)
    }

    fun registerUser(
        name: String,
        email: String,
        phone: String,
        pass: String,
        role: UserRole,
        mobileOtp: String,
        emailOtp: String
    ): Result<UserSession> {
        if (name.isBlank() || email.isBlank() || phone.isBlank() || pass.length < 6) {
            return Result.failure(IllegalArgumentException("Please fill all mandatory fields (Password min 6 chars)"))
        }
        if (mobileOtp.length < 4 || emailOtp.length < 4) {
            return Result.failure(IllegalArgumentException("Please enter verified Mobile and Email OTPs"))
        }
        val user = UserSession(
            uid = "usr_reg_" + UUID.randomUUID().toString().take(6),
            name = name,
            email = email,
            phone = phone,
            role = role,
            loyaltyPoints = 150,
            isGuest = false
        )
        _currentUser.value = user
        return Result.success(user)
    }

    fun logout() {
        apiClient.authToken = null
        apiClient.activeUserId = null
        _currentUser.value = UserSession(
            uid = "guest_001",
            name = "Guest Player",
            email = "guest@projectapex.in",
            phone = "+91 98200 12345",
            role = UserRole.ROLE_PLAYER,
            loyaltyPoints = 0,
            isGuest = true,
            jwtToken = null
        )
    }

    suspend fun logoutWithBackend() {
        apiClient.logout()
        logout()
    }

    fun switchRole(newRole: UserRole) {
        val current = _currentUser.value
        _currentUser.value = current.copy(
            role = newRole,
            name = if (newRole == UserRole.ROLE_OWNER) "Apex Arena Manager" else "Pushpendra Rait"
        )
    }
}

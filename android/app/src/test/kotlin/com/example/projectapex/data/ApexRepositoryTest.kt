package com.example.projectapex.data

import com.example.projectapex.model.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.*

class ApexRepositoryTest {

    private lateinit var repository: ApexRepository

    @Before
    fun setup() {
        repository = ApexRepository()
    }

    @Test
    fun testDynamicSlotsGeneratedWithoutPregeneration() {
        val venues = repository.venues.value
        assertFalse(venues.isEmpty())

        val courts = repository.courts.value
        assertFalse(courts.isEmpty())

        val court = courts.first()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        // Choose tomorrow's date
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        val tomorrow = sdf.format(cal.time)

        val slots = repository.getDynamicSlotsForDate(court.id, tomorrow)
        // 06:00 to 23:00 has 17 one-hour slots
        assertEquals(17, slots.size)
        assertEquals("06:00", slots.first().startTime)
        assertEquals("23:00", slots.last().endTime)
        assertTrue(slots.all { it.finalPricePaise > 0 })
    }

    @Test
    fun testHoldSessionCreationAndExpiry() {
        val venue = repository.venues.value.first()
        val court = repository.courts.value.first()
        val tomorrow = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(
            Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }.time
        )

        val slots = repository.getDynamicSlotsForDate(court.id, tomorrow)
        val selected = listOf(slots[0], slots[1])

        val hold = repository.createHoldSession(venue, court, tomorrow, selected)
        assertNotNull(hold)
        assertEquals(2, hold.slots.size)
        assertTrue(hold.remainingSeconds() in 590..600) // 10 minutes hold
        assertEquals(hold, repository.activeHold.value)

        // Verify slots are now locked
        val updatedSlots = repository.getDynamicSlotsForDate(court.id, tomorrow)
        assertEquals(SlotStatus.HELD, updatedSlots[0].status)
        assertEquals(SlotStatus.HELD, updatedSlots[1].status)

        // Release hold
        repository.releaseActiveHold()
        assertNull(repository.activeHold.value)
        val releasedSlots = repository.getDynamicSlotsForDate(court.id, tomorrow)
        assertEquals(SlotStatus.AVAILABLE, releasedSlots[0].status)
    }

    @Test
    fun testRazorpayPaisePricingAndPromoCodes() {
        val venue = repository.venues.value.first()
        val court = repository.courts.value.first()
        val tomorrow = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(
            Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }.time
        )

        val slots = repository.getDynamicSlotsForDate(court.id, tomorrow)
        val hold = repository.createHoldSession(venue, court, tomorrow, listOf(slots[0]))

        // Test without promo
        val quoteStandard = repository.calculateQuote(hold, null)
        assertTrue(quoteStandard.totalPayablePaise > 0)
        assertEquals(0L, quoteStandard.promoDiscountPaise)
        assertEquals((quoteStandard.baseCourtPaise - quoteStandard.offPeakDiscountPaise + quoteStandard.peakSurgePaise) * 2 / 100, quoteStandard.convenienceFeePaise)

        // Test WELCOME50 (-₹50 = 5000 paise)
        val quoteWelcome = repository.calculateQuote(hold, "WELCOME50")
        assertEquals(5000L, quoteWelcome.promoDiscountPaise)
        assertTrue(quoteWelcome.totalPayablePaise < quoteStandard.totalPayablePaise)

        // Test APEX20 (20% off)
        val quoteApex20 = repository.calculateQuote(hold, "APEX20")
        assertTrue(quoteApex20.promoDiscountPaise > 0)
    }

    @Test
    fun testBookingConfirmationAndMatchPass() {
        val venue = repository.venues.value.first()
        val court = repository.courts.value.first()
        val tomorrow = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(
            Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }.time
        )

        val slots = repository.getDynamicSlotsForDate(court.id, tomorrow)
        val hold = repository.createHoldSession(venue, court, tomorrow, listOf(slots[0]))
        val quote = repository.calculateQuote(hold, "WELCOME50")

        val pass = repository.confirmBooking(hold, quote)
        assertNotNull(pass)
        assertTrue(pass.bookingCode.startsWith("ARENA-"))
        assertEquals(BookingStatus.CONFIRMED, pass.status)
        assertEquals(quote.totalPayablePaise, pass.amountPaidPaise)

        // Verify slot is now BOOKED
        val slotsAfterBooking = repository.getDynamicSlotsForDate(court.id, tomorrow)
        assertEquals(SlotStatus.BOOKED, slotsAfterBooking[0].status)
    }

    @Test
    fun testGateQrVerification() {
        val venue = repository.venues.value.first()
        val court = repository.courts.value.first()
        val tomorrow = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(
            Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }.time
        )

        val slots = repository.getDynamicSlotsForDate(court.id, tomorrow)
        val hold = repository.createHoldSession(venue, court, tomorrow, listOf(slots[0]))
        val quote = repository.calculateQuote(hold, null)
        val pass = repository.confirmBooking(hold, quote)

        // Verify Gate QR
        val (isSuccess, msg) = repository.verifyGateQr(pass.bookingCode)
        assertTrue(isSuccess)
        assertTrue(msg.contains("Verified"))

        // Second scan should indicate already checked in
        val (secondScanSuccess, secondMsg) = repository.verifyGateQr(pass.bookingCode)
        assertFalse(secondScanSuccess)
        assertTrue(secondMsg.contains("Already Checked-In"))
    }

    @Test
    fun testOwnerAddCourt() {
        val initialCount = repository.courts.value.size
        val newCourt = repository.addCourt(
            venueId = "ven_thane_apex",
            name = "Test Pickleball Arena",
            sportType = SportType.PICKLEBALL,
            capacity = 4,
            surfaceType = "Acrylic Hard",
            isIndoor = true,
            dimensions = "44 x 20 ft",
            basePricePaise = 60000L,
            peakPricePaise = 90000L
        )

        assertNotNull(newCourt)
        assertEquals(initialCount + 1, repository.courts.value.size)
        assertEquals(SportType.PICKLEBALL, newCourt.sportType)
    }
}

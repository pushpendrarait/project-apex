package com.example.projectapex.network

import com.example.projectapex.data.ApexRepository
import com.example.projectapex.model.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class ApexApiClientTest {

    @Test
    fun testNormalizeBaseUrlStripsSlashesAndApiV1Suffix() {
        assertEquals(
            "https://project-apex-backend-fqe3.onrender.com",
            ApexApiClient.normalizeBaseUrl("https://project-apex-backend-fqe3.onrender.com/")
        )
        assertEquals(
            "https://project-apex-backend-fqe3.onrender.com",
            ApexApiClient.normalizeBaseUrl("https://project-apex-backend-fqe3.onrender.com/api/v1")
        )
        assertEquals(
            "https://project-apex-backend-fqe3.onrender.com",
            ApexApiClient.normalizeBaseUrl("  https://project-apex-backend-fqe3.onrender.com/API/V1///  ")
        )
        assertEquals(
            "http://10.0.2.2:8080",
            ApexApiClient.normalizeBaseUrl("http://10.0.2.2:8080/api/v1/")
        )
        assertEquals("", ApexApiClient.normalizeBaseUrl("   "))
        assertEquals("", ApexApiClient.normalizeBaseUrl(null))
    }

    @Test
    fun testDefaultBaseUrlMatchesRenderProduction() {
        val client = ApexApiClient()
        assertEquals(ApexApiClient.RENDER_PROD_URL, client.baseUrl)
        assertEquals("https://project-apex-backend-fqe3.onrender.com", ApexApiClient.RENDER_PROD_URL)
        assertEquals("http://10.0.2.2:8080", ApexApiClient.EMULATOR_LOCAL_URL)
    }

    @Test
    fun testParseHealthJsonNeonPostgresResponse() {
        val sampleJson = """
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
        """.trimIndent()

        val client = ApexApiClient()
        val health = client.parseHealthJson(sampleJson, ApexApiClient.RENDER_PROD_URL, 142L)

        assertTrue(health.isOnline)
        assertEquals("UP", health.status)
        assertEquals("project-apex-backend", health.service)
        assertEquals("Render", health.hosting)
        assertEquals("Java 21 LTS + Spring Boot 3.2.x", health.runtime)
        assertEquals("Neon PostgreSQL", health.database)
        assertTrue(health.dbConnected)
        assertEquals(14L, health.usersCount)
        assertEquals(4L, health.venuesCount)
        assertEquals(29L, health.bookingsCount)
        assertEquals(142L, health.latencyMs)
        assertEquals(ApexApiClient.RENDER_PROD_URL, health.url)
    }

    @Test
    fun testParseHealthJsonH2FallbackMode() {
        val h2Json = """
            {
              "status": "UP",
              "service": "project-apex-backend",
              "hosting": "Localhost",
              "runtime": "Java 21 LTS + Spring Boot 3.2.x",
              "database": "H2 PostgreSQL-Mode",
              "dbConnected": true,
              "usersCount": 3,
              "venuesCount": 4,
              "bookingsCount": 0
            }
        """.trimIndent()

        val client = ApexApiClient(ApexApiClient.EMULATOR_LOCAL_URL)
        val health = client.parseHealthJson(h2Json, ApexApiClient.EMULATOR_LOCAL_URL, 18L)

        assertTrue(health.isOnline)
        assertEquals("H2 PostgreSQL-Mode", health.database)
        assertEquals(3L, health.usersCount)
        assertEquals(4L, health.venuesCount)
        assertEquals(0L, health.bookingsCount)
    }

    @Test
    fun testJwtAuthParsingAndLogoutCleanup() = runTest {
        val authResponseJson = """
            {
              "success": true,
              "message": "Authentication successful",
              "data": {
                "token": "eyJhbGciOiJIUzI1NiJ9.apex-test-payload.signature",
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
        """.trimIndent()

        val client = ApexApiClient()
        val authResult = client.parseAndStoreAuth(authResponseJson)

        assertEquals("eyJhbGciOiJIUzI1NiJ9.apex-test-payload.signature", authResult.token)
        assertEquals("eyJhbGciOiJIUzI1NiJ9.apex-test-payload.signature", client.authToken)
        assertEquals("demo_owner", client.activeUserId)
        assertEquals(UserRole.ROLE_OWNER, authResult.user.role)
        assertEquals("Santosh Kadam", authResult.user.name)
        assertEquals(420, authResult.user.loyaltyPoints)
        assertEquals(authResult.token, authResult.user.jwtToken)
        assertFalse(authResult.user.isGuest)

        client.logout()
        assertNull(client.authToken)
        assertNull(client.activeUserId)
    }

    @Test
    fun testBackendDomainPayloadMappers() {
        val client = ApexApiClient()

        val venue = client.parseVenue(
            ApexJson.parseObject(
                """
                {
                  "venueId": "venue_thane_01",
                  "name": "Apex Arena Ghodbunder",
                  "locality": "Ghodbunder Road",
                  "city": "thane",
                  "rating": 4.9,
                  "reviewCount": 312,
                  "minSlotPricePaise": 60000,
                  "hasActiveDiscounts": true,
                  "sportTypes": ["CRICKET_TURF", "BADMINTON"],
                  "amenities": ["FIFA-grade Turf", "LED Floodlights"]
                }
                """.trimIndent()
            )
        )
        assertEquals("venue_thane_01", venue.id)
        assertEquals("Thane", venue.city)
        assertEquals(60000L, venue.startingPricePaise)
        assertTrue(venue.supportedSports.contains(SportType.CRICKET_TURF))
        assertTrue(venue.supportedSports.contains(SportType.BADMINTON))

        // Jackson serializes `isPeak` boolean getter as `"peak"`
        val slot = client.parseTimeSlot(
            ApexJson.parseObject(
                """
                {
                  "resourceId": "res_thane_crk_01",
                  "date": "2026-11-29",
                  "startTime": "19:00",
                  "endTime": "20:00",
                  "originalPricePaise": 120000,
                  "finalPricePaise": 130000,
                  "status": "AVAILABLE",
                  "peak": true
                }
                """.trimIndent()
            ),
            "res_thane_crk_01",
            "2026-11-29"
        )
        assertTrue(slot.isPeak)
        assertEquals(SlotStatus.AVAILABLE, slot.status)
        assertEquals(130000L, slot.finalPricePaise)

        val pass = client.parseBookingPass(
            ApexJson.parseObject(
                """
                {
                  "bookingId": "bk_9911aabb",
                  "bookingCode": "ARENA-9911A",
                  "qrPassToken": "APEX-PASS-ARENA-9911A",
                  "venueName": "Apex Arena Ghodbunder",
                  "resourceName": "Main Arena 8v8 Box Cricket",
                  "sportType": "CRICKET_TURF",
                  "bookingDate": "2026-11-29",
                  "finalPayablePaise": 122400,
                  "status": "CONFIRMED",
                  "slots": [
                    { "startTime": "19:00", "endTime": "20:00", "unitPricePaise": 120000 }
                  ]
                }
                """.trimIndent()
            )
        )
        assertEquals("bk_9911aabb", pass.id)
        assertEquals("ARENA-9911A", pass.bookingCode)
        assertEquals("APEX-PASS-ARENA-9911A", pass.qrPassToken)
        assertEquals("19:00 - 20:00", pass.timeSlotRange)
        assertEquals(122400L, pass.amountPaidPaise)
        assertEquals(BookingStatus.CONFIRMED, pass.status)
    }

    @Test
    fun testRepositoryExposesBackendUrlAndHealthStatusOffline() {
        val repo = ApexRepository(checkOnInit = false)
        assertEquals(ApexApiClient.RENDER_PROD_URL, repo.backendUrl.value)
        assertFalse(repo.isBackendConnected.value)
        assertNull(repo.healthStatus.value)
        assertNull(repo.currentUser.value.jwtToken)
        assertTrue(repo.currentUser.value.isGuest)
    }
}

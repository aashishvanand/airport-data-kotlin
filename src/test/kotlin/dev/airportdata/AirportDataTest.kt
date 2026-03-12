package dev.airportdata

import kotlin.test.*

class AirportDataTest {

    private val airportData = AirportData()

    // ========================================================================
    // getAirportByIata
    // ========================================================================

    @Test
    fun `should retrieve airport data for a valid IATA code`() {
        val airports = airportData.getAirportByIata("LHR")
        val airport = airports.first()
        assertEquals("LHR", airport.iata)
        assertTrue(airport.airport.contains("Heathrow"))
    }

    // ========================================================================
    // getAirportByIcao
    // ========================================================================

    @Test
    fun `should retrieve airport data for a valid ICAO code`() {
        val airports = airportData.getAirportByIcao("EGLL")
        val airport = airports.first()
        assertEquals("EGLL", airport.icao)
        assertTrue(airport.airport.contains("Heathrow"))
    }

    // ========================================================================
    // getAirportByCountryCode
    // ========================================================================

    @Test
    fun `should retrieve all airports for a given country code`() {
        val airports = airportData.getAirportByCountryCode("US")
        assertTrue(airports.size > 100, "There should be many US airports")
        assertEquals("US", airports.first().countryCode)
    }

    // ========================================================================
    // getAirportByContinent
    // ========================================================================

    @Test
    fun `should retrieve all airports for a given continent code`() {
        val airports = airportData.getAirportByContinent("EU")
        assertTrue(airports.size > 100, "There should be many EU airports")
        assertTrue(airports.all { it.continent == "EU" })
    }

    // ========================================================================
    // findNearbyAirports
    // ========================================================================

    @Test
    fun `should find airports within a given radius`() {
        val lat = 51.5074
        val lon = -0.1278
        val airports = airportData.findNearbyAirports(lat, lon, 50.0) // 50km radius
        assertTrue(airports.isNotEmpty())
        assertTrue(airports.any { it.iata == "LHR" })
    }

    // ========================================================================
    // getAirportsByType
    // ========================================================================

    @Test
    fun `should retrieve all large airports`() {
        val airports = airportData.getAirportsByType("large_airport")
        assertTrue(airports.size > 10)
        assertTrue(airports.all { it.type == "large_airport" })
    }

    @Test
    fun `should retrieve all medium airports`() {
        val airports = airportData.getAirportsByType("medium_airport")
        assertTrue(airports.size > 10)
        assertTrue(airports.all { it.type == "medium_airport" })
    }

    @Test
    fun `should retrieve all airports when searching for airport`() {
        val airports = airportData.getAirportsByType("airport")
        assertTrue(airports.size > 50, "Should include large, medium, and small airports")
        assertTrue(airports.all { it.type.contains("airport") })
    }

    @Test
    fun `should handle different airport types`() {
        val heliports = airportData.getAirportsByType("heliport")
        assertTrue(heliports is List)
        if (heliports.isNotEmpty()) {
            assertTrue(heliports.all { it.type == "heliport" })
        }

        val seaplaneBases = airportData.getAirportsByType("seaplane_base")
        assertTrue(seaplaneBases is List)
        if (seaplaneBases.isNotEmpty()) {
            assertTrue(seaplaneBases.all { it.type == "seaplane_base" })
        }
    }

    @Test
    fun `should handle case-insensitive type searches`() {
        val upperCase = airportData.getAirportsByType("LARGE_AIRPORT")
        val lowerCase = airportData.getAirportsByType("large_airport")
        assertEquals(upperCase.size, lowerCase.size)
        assertTrue(upperCase.isNotEmpty())
    }

    @Test
    fun `should return empty list for non-existent type`() {
        val airports = airportData.getAirportsByType("nonexistent_type")
        assertEquals(0, airports.size)
    }

    // ========================================================================
    // getAutocompleteSuggestions
    // ========================================================================

    @Test
    fun `should return suggestions based on airport name`() {
        val suggestions = airportData.getAutocompleteSuggestions("London")
        assertTrue(suggestions.isNotEmpty())
        assertTrue(suggestions.size <= 10)
        assertTrue(suggestions.any { it.iata == "LHR" })
    }

    // ========================================================================
    // calculateDistance
    // ========================================================================

    @Test
    fun `should calculate the distance between two airports using IATA codes`() {
        val distance = airportData.calculateDistance("LHR", "JFK")
        // Approximately 5541 km
        assertEquals(5541.0, distance, 50.0)
    }

    // ========================================================================
    // findAirports (Advanced Filtering)
    // ========================================================================

    @Test
    fun `should find airports with multiple matching criteria`() {
        val airports = airportData.findAirports(mapOf("country_code" to "GB", "type" to "airport"))
        assertTrue(airports.all { it.countryCode == "GB" && it.type.lowercase() == "airport" || it.type.contains("airport") })
    }

    @Test
    fun `should filter by scheduled service availability`() {
        val airportsWithService = airportData.findAirports(mapOf("has_scheduled_service" to true))
        val airportsWithoutService = airportData.findAirports(mapOf("has_scheduled_service" to false))

        assertTrue(airportsWithService.size + airportsWithoutService.size > 0)

        if (airportsWithService.isNotEmpty()) {
            assertTrue(airportsWithService.all { it.hasScheduledService })
        }

        if (airportsWithoutService.isNotEmpty()) {
            assertTrue(airportsWithoutService.all { !it.hasScheduledService })
        }
    }

    // ========================================================================
    // getAirportsByTimezone
    // ========================================================================

    @Test
    fun `should find all airports within a specific timezone`() {
        val airports = airportData.getAirportsByTimezone("Europe/London")
        assertTrue(airports.size > 10)
        assertTrue(airports.all { it.time == "Europe/London" })
    }

    // ========================================================================
    // getAirportLinks
    // ========================================================================

    @Test
    fun `should retrieve a map of all available external links`() {
        val links = airportData.getAirportLinks("LHR")
        assertNotNull(links.wikipedia)
        assertTrue(links.wikipedia!!.contains("Heathrow_Airport"))
        assertNotNull(links.website)
    }

    @Test
    fun `should handle airports with missing links gracefully`() {
        val links = airportData.getAirportLinks("HND")
        assertNotNull(links.wikipedia)
        assertTrue(links.wikipedia!!.contains("Tokyo_International_Airport"))
        assertNotNull(links.website)
    }

    // ========================================================================
    // getAirportStatsByCountry
    // ========================================================================

    @Test
    fun `should return comprehensive statistics for a country`() {
        val stats = airportData.getAirportStatsByCountry("SG")
        assertTrue(stats.total > 0)
        assertNotNull(stats.byType)
        assertTrue(stats.timezones is List)
    }

    @Test
    fun `should calculate correct statistics for US airports`() {
        val stats = airportData.getAirportStatsByCountry("US")
        assertTrue(stats.total > 1000)
        assertTrue(stats.byType.containsKey("large_airport"))
        assertTrue(stats.byType["large_airport"]!! > 0)
    }

    @Test
    fun `should throw error for invalid country code in stats`() {
        assertFailsWith<AirportDataException> {
            airportData.getAirportStatsByCountry("XYZ")
        }
    }

    // ========================================================================
    // getAirportStatsByContinent
    // ========================================================================

    @Test
    fun `should return comprehensive statistics for a continent`() {
        val stats = airportData.getAirportStatsByContinent("AS")
        assertTrue(stats.total > 100)
        assertNotNull(stats.byType)
        assertNotNull(stats.byCountry)
        assertTrue(stats.byCountry.size > 10)
    }

    @Test
    fun `should include country breakdown`() {
        val stats = airportData.getAirportStatsByContinent("EU")
        assertTrue(stats.byCountry.containsKey("GB"))
        assertTrue(stats.byCountry.containsKey("FR"))
        assertTrue(stats.byCountry.containsKey("DE"))
    }

    // ========================================================================
    // getLargestAirportsByContinent
    // ========================================================================

    @Test
    fun `should return top airports by runway length`() {
        val airports = airportData.getLargestAirportsByContinent("AS", 5, "runway")
        assertTrue(airports.size <= 5)
        assertTrue(airports.isNotEmpty())
        // Check sorted by runway length descending
        for (i in 0 until airports.size - 1) {
            val runway1 = airports[i].runwayLength ?: 0
            val runway2 = airports[i + 1].runwayLength ?: 0
            assertTrue(runway1 >= runway2)
        }
    }

    @Test
    fun `should return top airports by elevation`() {
        val airports = airportData.getLargestAirportsByContinent("SA", 5, "elevation")
        assertTrue(airports.size <= 5)
        // Check sorted by elevation descending
        for (i in 0 until airports.size - 1) {
            val elev1 = airports[i].elevationFt ?: 0
            val elev2 = airports[i + 1].elevationFt ?: 0
            assertTrue(elev1 >= elev2)
        }
    }

    @Test
    fun `should respect the limit parameter`() {
        val airports = airportData.getLargestAirportsByContinent("EU", 3)
        assertTrue(airports.size <= 3)
    }

    // ========================================================================
    // getMultipleAirports
    // ========================================================================

    @Test
    fun `should fetch multiple airports by IATA codes`() {
        val airports = airportData.getMultipleAirports(listOf("SIN", "LHR", "JFK"))
        assertEquals(3, airports.size)
        assertEquals("SIN", airports[0]!!.iata)
        assertEquals("LHR", airports[1]!!.iata)
        assertEquals("JFK", airports[2]!!.iata)
    }

    @Test
    fun `should handle mix of IATA and ICAO codes`() {
        val airports = airportData.getMultipleAirports(listOf("SIN", "EGLL", "JFK"))
        assertEquals(3, airports.size)
        assertTrue(airports.all { it != null })
    }

    @Test
    fun `should return null for invalid codes`() {
        val airports = airportData.getMultipleAirports(listOf("SIN", "INVALID", "LHR"))
        assertEquals(3, airports.size)
        assertNotNull(airports[0])
        assertNull(airports[1])
        assertNotNull(airports[2])
    }

    @Test
    fun `should handle empty array`() {
        val airports = airportData.getMultipleAirports(emptyList())
        assertEquals(0, airports.size)
    }

    // ========================================================================
    // calculateDistanceMatrix
    // ========================================================================

    @Test
    fun `should calculate distance matrix for multiple airports`() {
        val matrix = airportData.calculateDistanceMatrix(listOf("SIN", "LHR", "JFK"))
        assertNotNull(matrix.airports)
        assertNotNull(matrix.distances)
        assertEquals(3, matrix.airports.size)

        // Check diagonal is zero
        assertEquals(0.0, matrix.distances["SIN"]!!["SIN"]!!)
        assertEquals(0.0, matrix.distances["LHR"]!!["LHR"]!!)
        assertEquals(0.0, matrix.distances["JFK"]!!["JFK"]!!)

        // Check symmetry
        assertEquals(matrix.distances["SIN"]!!["LHR"]!!, matrix.distances["LHR"]!!["SIN"]!!)
        assertEquals(matrix.distances["SIN"]!!["JFK"]!!, matrix.distances["JFK"]!!["SIN"]!!)

        // Check reasonable distances
        assertTrue(matrix.distances["SIN"]!!["LHR"]!! > 5000)
        assertTrue(matrix.distances["LHR"]!!["JFK"]!! > 3000)
    }

    @Test
    fun `should throw error for less than 2 airports in matrix`() {
        assertFailsWith<AirportDataException> {
            airportData.calculateDistanceMatrix(listOf("SIN"))
        }
    }

    @Test
    fun `should throw error for invalid codes in matrix`() {
        assertFailsWith<AirportDataException> {
            airportData.calculateDistanceMatrix(listOf("SIN", "INVALID"))
        }
    }

    // ========================================================================
    // findNearestAirport
    // ========================================================================

    @Test
    fun `should find nearest airport to coordinates`() {
        val nearest = airportData.findNearestAirport(1.35019, 103.994003)
        assertNotNull(nearest.distance)
        assertEquals("SIN", nearest.iata)
        assertTrue(nearest.distance < 2) // Very close to Changi
    }

    @Test
    fun `should find nearest airport with type filter`() {
        val nearest = airportData.findNearestAirport(
            51.5074, -0.1278,
            mapOf("type" to "large_airport")
        )
        assertNotNull(nearest)
        assertEquals("large_airport", nearest.type)
        assertTrue(nearest.distance >= 0)
    }

    @Test
    fun `should find nearest airport with type and country filters`() {
        val nearest = airportData.findNearestAirport(
            40.7128, -74.0060,
            mapOf("type" to "large_airport", "country_code" to "US")
        )
        assertNotNull(nearest)
        assertTrue(nearest.distance >= 0)
        assertEquals("large_airport", nearest.type)
        assertEquals("US", nearest.countryCode)
    }

    // ========================================================================
    // validateIataCode
    // ========================================================================

    @Test
    fun `should return true for valid IATA codes`() {
        assertTrue(airportData.validateIataCode("SIN"))
        assertTrue(airportData.validateIataCode("LHR"))
        assertTrue(airportData.validateIataCode("JFK"))
    }

    @Test
    fun `should return false for invalid IATA codes`() {
        assertFalse(airportData.validateIataCode("XYZ"))
        assertFalse(airportData.validateIataCode("ZZZ"))
    }

    @Test
    fun `should return false for incorrect IATA format`() {
        assertFalse(airportData.validateIataCode("ABCD"))
        assertFalse(airportData.validateIataCode("AB"))
        assertFalse(airportData.validateIataCode("abc"))
        assertFalse(airportData.validateIataCode(""))
    }

    // ========================================================================
    // validateIcaoCode
    // ========================================================================

    @Test
    fun `should return true for valid ICAO codes`() {
        assertTrue(airportData.validateIcaoCode("WSSS"))
        assertTrue(airportData.validateIcaoCode("EGLL"))
        assertTrue(airportData.validateIcaoCode("KJFK"))
    }

    @Test
    fun `should return false for invalid ICAO codes`() {
        assertFalse(airportData.validateIcaoCode("XXXX"))
        assertFalse(airportData.validateIcaoCode("ZZZ0"))
    }

    @Test
    fun `should return false for incorrect ICAO format`() {
        assertFalse(airportData.validateIcaoCode("ABC"))
        assertFalse(airportData.validateIcaoCode("ABCDE"))
        assertFalse(airportData.validateIcaoCode("abcd"))
        assertFalse(airportData.validateIcaoCode(""))
    }

    // ========================================================================
    // getAirportCount
    // ========================================================================

    @Test
    fun `should return total count of all airports`() {
        val count = airportData.getAirportCount()
        assertTrue(count > 5000)
    }

    @Test
    fun `should return count with type filter`() {
        val largeCount = airportData.getAirportCount(mapOf("type" to "large_airport"))
        val totalCount = airportData.getAirportCount()
        assertTrue(largeCount > 0)
        assertTrue(largeCount < totalCount)
    }

    @Test
    fun `should return count with country filter`() {
        val usCount = airportData.getAirportCount(mapOf("country_code" to "US"))
        assertTrue(usCount > 1000)
    }

    @Test
    fun `should return count with multiple filters`() {
        val count = airportData.getAirportCount(
            mapOf("country_code" to "US", "type" to "large_airport")
        )
        assertTrue(count > 0)
        assertTrue(count < 200)
    }

    // ========================================================================
    // isAirportOperational
    // ========================================================================

    @Test
    fun `should return true for operational airports`() {
        assertTrue(airportData.isAirportOperational("SIN"))
        assertTrue(airportData.isAirportOperational("LHR"))
        assertTrue(airportData.isAirportOperational("JFK"))
    }

    @Test
    fun `should work with both IATA and ICAO codes`() {
        assertTrue(airportData.isAirportOperational("SIN"))
        assertTrue(airportData.isAirportOperational("WSSS"))
    }

    @Test
    fun `should throw error for invalid airport code in operational check`() {
        assertFailsWith<AirportDataException> {
            airportData.isAirportOperational("INVALID")
        }
    }
}

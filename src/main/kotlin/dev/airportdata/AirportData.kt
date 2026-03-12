package dev.airportdata

import kotlinx.serialization.json.Json
import java.io.InputStreamReader
import java.util.zip.GZIPInputStream
import kotlin.math.*

/**
 * A comprehensive library for retrieving airport information by IATA codes, ICAO codes,
 * and various other criteria. Provides easy access to a large dataset of airports worldwide.
 *
 * Thread-safe: uses lazy initialization for data loading and index building.
 *
 * Usage:
 * ```kotlin
 * val airportData = AirportData()
 * val airport = airportData.getAirportByIata("SIN")
 * ```
 */
class AirportData {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    // Lazy-loaded airport list and indexes
    private val airports: List<Airport> by lazy { loadAirports() }
    private val iataIndex: Map<String, List<Int>> by lazy { buildIataIndex() }
    private val icaoIndex: Map<String, List<Int>> by lazy { buildIcaoIndex() }
    private val countryIndex: Map<String, List<Int>> by lazy { buildCountryIndex() }
    private val continentIndex: Map<String, List<Int>> by lazy { buildContinentIndex() }
    private val typeIndex: Map<String, List<Int>> by lazy { buildTypeIndex() }
    private val timezoneIndex: Map<String, List<Int>> by lazy { buildTimezoneIndex() }

    companion object {
        private const val EARTH_RADIUS_KM = 6371.0
        private const val RESOURCE_PATH = "/airports.json.gz"
    }

    // ========================================================================
    // Data Loading
    // ========================================================================

    private fun loadAirports(): List<Airport> {
        val inputStream = this::class.java.getResourceAsStream(RESOURCE_PATH)
            ?: throw AirportDataException("Airport data resource not found: $RESOURCE_PATH")

        val gzipStream = GZIPInputStream(inputStream)
        val reader = InputStreamReader(gzipStream, Charsets.UTF_8)
        val jsonString = reader.use { it.readText() }

        return json.decodeFromString<List<Airport>>(jsonString)
    }

    // ========================================================================
    // Index Building
    // ========================================================================

    private fun buildIataIndex(): Map<String, List<Int>> {
        val index = mutableMapOf<String, MutableList<Int>>()
        airports.forEachIndexed { i, airport ->
            if (airport.iata.isNotEmpty()) {
                index.getOrPut(airport.iata.uppercase()) { mutableListOf() }.add(i)
            }
        }
        return index
    }

    private fun buildIcaoIndex(): Map<String, List<Int>> {
        val index = mutableMapOf<String, MutableList<Int>>()
        airports.forEachIndexed { i, airport ->
            if (airport.icao.isNotEmpty()) {
                index.getOrPut(airport.icao.uppercase()) { mutableListOf() }.add(i)
            }
        }
        return index
    }

    private fun buildCountryIndex(): Map<String, List<Int>> {
        val index = mutableMapOf<String, MutableList<Int>>()
        airports.forEachIndexed { i, airport ->
            if (airport.countryCode.isNotEmpty()) {
                index.getOrPut(airport.countryCode.uppercase()) { mutableListOf() }.add(i)
            }
        }
        return index
    }

    private fun buildContinentIndex(): Map<String, List<Int>> {
        val index = mutableMapOf<String, MutableList<Int>>()
        airports.forEachIndexed { i, airport ->
            if (airport.continent.isNotEmpty()) {
                index.getOrPut(airport.continent.uppercase()) { mutableListOf() }.add(i)
            }
        }
        return index
    }

    private fun buildTypeIndex(): Map<String, List<Int>> {
        val index = mutableMapOf<String, MutableList<Int>>()
        airports.forEachIndexed { i, airport ->
            if (airport.type.isNotEmpty()) {
                index.getOrPut(airport.type.lowercase()) { mutableListOf() }.add(i)
            }
        }
        return index
    }

    private fun buildTimezoneIndex(): Map<String, List<Int>> {
        val index = mutableMapOf<String, MutableList<Int>>()
        airports.forEachIndexed { i, airport ->
            if (airport.time.isNotEmpty()) {
                index.getOrPut(airport.time) { mutableListOf() }.add(i)
            }
        }
        return index
    }

    // ========================================================================
    // Core Search Functions
    // ========================================================================

    /**
     * Finds airports by their 3-letter IATA code.
     *
     * @param iataCode The IATA code to search for (case-insensitive)
     * @return List of airports matching the IATA code
     * @throws AirportDataException if the code format is invalid or no airport is found
     */
    fun getAirportByIata(iataCode: String): List<Airport> {
        val code = iataCode.trim().uppercase()
        if (code.length != 3) {
            throw AirportDataException("Invalid IATA code format: $iataCode. Must be 3 characters.")
        }

        val indices = iataIndex[code]
            ?: throw AirportDataException("No data found for IATA code: $iataCode")

        return indices.map { airports[it] }
    }

    /**
     * Finds airports by their 4-character ICAO code.
     *
     * @param icaoCode The ICAO code to search for (case-insensitive)
     * @return List of airports matching the ICAO code
     * @throws AirportDataException if the code format is invalid or no airport is found
     */
    fun getAirportByIcao(icaoCode: String): List<Airport> {
        val code = icaoCode.trim().uppercase()
        if (code.length != 4) {
            throw AirportDataException("Invalid ICAO code format: $icaoCode. Must be 4 characters.")
        }

        val indices = icaoIndex[code]
            ?: throw AirportDataException("No data found for ICAO code: $icaoCode")

        return indices.map { airports[it] }
    }

    /**
     * Searches for airports by name (case-insensitive, minimum 2 characters).
     *
     * @param query The search query
     * @return List of airports whose names contain the query string
     * @throws AirportDataException if the query is too short
     */
    fun searchByName(query: String): List<Airport> {
        val trimmed = query.trim()
        if (trimmed.length < 2) {
            throw AirportDataException("Search query must be at least 2 characters long.")
        }

        val lowerQuery = trimmed.lowercase()
        return airports.filter { it.airport.lowercase().contains(lowerQuery) }
    }

    // ========================================================================
    // Geographic Functions
    // ========================================================================

    /**
     * Finds airports within a specified radius of given coordinates using the Haversine formula.
     *
     * @param lat Latitude of the center point
     * @param lon Longitude of the center point
     * @param radiusKm Radius in kilometers
     * @return List of airports within the radius, each with an added distance field
     * @throws AirportDataException if coordinates or radius are invalid
     */
    fun findNearbyAirports(lat: Double, lon: Double, radiusKm: Double): List<Airport> {
        if (lat < -90 || lat > 90) {
            throw AirportDataException("Latitude must be between -90 and 90.")
        }
        if (lon < -180 || lon > 180) {
            throw AirportDataException("Longitude must be between -180 and 180.")
        }
        if (radiusKm <= 0) {
            throw AirportDataException("Radius must be positive.")
        }

        return airports.filter { airport ->
            haversineDistance(lat, lon, airport.latitude, airport.longitude) <= radiusKm
        }
    }

    /**
     * Calculates the great-circle distance between two airports using IATA or ICAO codes.
     *
     * @param code1 IATA or ICAO code of the first airport
     * @param code2 IATA or ICAO code of the second airport
     * @return Distance in kilometers
     * @throws AirportDataException if either airport code is not found
     */
    fun calculateDistance(code1: String, code2: String): Double {
        val airport1 = resolveAirport(code1)
            ?: throw AirportDataException("Airport not found for code: $code1")
        val airport2 = resolveAirport(code2)
            ?: throw AirportDataException("Airport not found for code: $code2")

        return haversineDistance(
            airport1.latitude, airport1.longitude,
            airport2.latitude, airport2.longitude
        )
    }

    /**
     * Finds the single nearest airport to given coordinates, optionally with filters.
     *
     * @param lat Latitude
     * @param lon Longitude
     * @param filters Optional map of filters (type, country_code, has_scheduled_service)
     * @return The nearest airport with a distance field, or null if no airports match
     */
    fun findNearestAirport(
        lat: Double,
        lon: Double,
        filters: Map<String, Any>? = null
    ): NearestAirportResult {
        if (lat < -90 || lat > 90) {
            throw AirportDataException("Latitude must be between -90 and 90.")
        }
        if (lon < -180 || lon > 180) {
            throw AirportDataException("Longitude must be between -180 and 180.")
        }

        var candidates = airports.asSequence()

        if (filters != null) {
            candidates = applyFilters(candidates, filters)
        }

        var nearestAirport: Airport? = null
        var nearestDistance = Double.MAX_VALUE

        for (airport in candidates) {
            val dist = haversineDistance(lat, lon, airport.latitude, airport.longitude)
            if (dist < nearestDistance) {
                nearestDistance = dist
                nearestAirport = airport
            }
        }

        if (nearestAirport == null) {
            throw AirportDataException("No airports found matching the given criteria.")
        }

        return NearestAirportResult(nearestAirport, nearestDistance)
    }

    // ========================================================================
    // Filtering Functions
    // ========================================================================

    /**
     * Finds all airports in a specific country.
     *
     * @param countryCode 2-letter ISO country code
     * @return List of airports in the country
     * @throws AirportDataException if no airports are found
     */
    fun getAirportByCountryCode(countryCode: String): List<Airport> {
        val code = countryCode.trim().uppercase()
        val indices = countryIndex[code]
            ?: throw AirportDataException("No airports found for country code: $countryCode")

        return indices.map { airports[it] }
    }

    /**
     * Finds all airports on a specific continent.
     *
     * @param continentCode 2-letter continent code (AS, EU, NA, SA, AF, OC, AN)
     * @return List of airports on the continent
     * @throws AirportDataException if no airports are found
     */
    fun getAirportByContinent(continentCode: String): List<Airport> {
        val code = continentCode.trim().uppercase()
        val indices = continentIndex[code]
            ?: throw AirportDataException("No airports found for continent code: $continentCode")

        return indices.map { airports[it] }
    }

    /**
     * Finds airports by their type.
     *
     * When type is "airport", returns large_airport, medium_airport, and small_airport combined.
     *
     * @param type Airport type (large_airport, medium_airport, small_airport, heliport, seaplane_base, or "airport" for all)
     * @return List of airports matching the type
     */
    fun getAirportsByType(type: String): List<Airport> {
        val normalizedType = type.trim().lowercase()

        if (normalizedType == "airport") {
            val result = mutableListOf<Airport>()
            listOf("large_airport", "medium_airport", "small_airport").forEach { t ->
                typeIndex[t]?.forEach { idx -> result.add(airports[idx]) }
            }
            return result
        }

        val indices = typeIndex[normalizedType] ?: return emptyList()
        return indices.map { airports[it] }
    }

    /**
     * Finds all airports within a specific timezone.
     *
     * @param timezone IANA timezone identifier (e.g., "Europe/London")
     * @return List of airports in the timezone
     * @throws AirportDataException if no airports are found
     */
    fun getAirportsByTimezone(timezone: String): List<Airport> {
        val tz = timezone.trim()
        val indices = timezoneIndex[tz]
            ?: throw AirportDataException("No airports found for timezone: $timezone")

        return indices.map { airports[it] }
    }

    /**
     * Finds airports matching multiple criteria.
     *
     * Supported filter keys:
     * - country_code: 2-letter country code
     * - continent: 2-letter continent code
     * - type: Airport type string
     * - has_scheduled_service: Boolean
     * - min_runway_ft: Minimum runway length in feet (Int)
     *
     * @param filters Map of filter criteria
     * @return List of matching airports
     */
    fun findAirports(filters: Map<String, Any>): List<Airport> {
        var candidates = airports.asSequence()
        candidates = applyFilters(candidates, filters)
        return candidates.toList()
    }

    // ========================================================================
    // Advanced Functions
    // ========================================================================

    /**
     * Provides autocomplete suggestions for search interfaces (returns max 10 results).
     * Matches against airport name and IATA code.
     *
     * @param query Search query (minimum 1 character)
     * @return Up to 10 matching airports
     */
    fun getAutocompleteSuggestions(query: String): List<Airport> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()

        val lowerQuery = trimmed.lowercase()
        return airports.filter { airport ->
            airport.airport.lowercase().contains(lowerQuery) ||
                airport.iata.lowercase().contains(lowerQuery)
        }.take(10)
    }

    /**
     * Gets external links for an airport using IATA or ICAO code.
     *
     * @param code IATA or ICAO code
     * @return AirportLinks with available external links
     * @throws AirportDataException if the airport is not found
     */
    fun getAirportLinks(code: String): AirportLinks {
        val airport = resolveAirport(code)
            ?: throw AirportDataException("Airport not found for code: $code")

        return AirportLinks(
            website = airport.website.ifEmpty { null },
            wikipedia = airport.wikipedia.ifEmpty { null },
            flightradar24 = airport.flightradar24Url.ifEmpty { null },
            radarbox = airport.radarboxUrl.ifEmpty { null },
            flightaware = airport.flightawareUrl.ifEmpty { null }
        )
    }

    // ========================================================================
    // Statistical & Analytical Functions
    // ========================================================================

    /**
     * Gets comprehensive statistics about airports in a specific country.
     *
     * @param countryCode 2-letter ISO country code
     * @return CountryStats with total, byType, withScheduledService, averageRunwayLength, averageElevation, timezones
     * @throws AirportDataException if no airports are found for the country
     */
    fun getAirportStatsByCountry(countryCode: String): CountryStats {
        val code = countryCode.trim().uppercase()
        val indices = countryIndex[code]
            ?: throw AirportDataException("No airports found for country code: $countryCode")

        val countryAirports = indices.map { airports[it] }
        return computeCountryStats(countryAirports)
    }

    /**
     * Gets comprehensive statistics about airports on a specific continent.
     *
     * @param continentCode 2-letter continent code
     * @return ContinentStats with total, byType, byCountry, withScheduledService, averageRunwayLength, averageElevation, timezones
     * @throws AirportDataException if no airports are found for the continent
     */
    fun getAirportStatsByContinent(continentCode: String): ContinentStats {
        val code = continentCode.trim().uppercase()
        val indices = continentIndex[code]
            ?: throw AirportDataException("No airports found for continent code: $continentCode")

        val continentAirports = indices.map { airports[it] }
        return computeContinentStats(continentAirports)
    }

    /**
     * Gets the largest airports on a continent sorted by runway length or elevation.
     *
     * @param continentCode 2-letter continent code
     * @param limit Maximum number of airports to return (default 10)
     * @param sortBy Sort criteria: "runway" or "elevation" (default "runway")
     * @return List of airports sorted by the specified criteria
     */
    fun getLargestAirportsByContinent(
        continentCode: String,
        limit: Int = 10,
        sortBy: String = "runway"
    ): List<Airport> {
        val code = continentCode.trim().uppercase()
        val indices = continentIndex[code]
            ?: throw AirportDataException("No airports found for continent code: $continentCode")

        val continentAirports = indices.map { airports[it] }

        return when (sortBy.lowercase()) {
            "elevation" -> continentAirports
                .sortedByDescending { it.elevationFt ?: 0 }
                .take(limit)
            else -> continentAirports
                .sortedByDescending { it.runwayLength ?: 0 }
                .take(limit)
        }
    }

    // ========================================================================
    // Bulk Operations
    // ========================================================================

    /**
     * Fetches multiple airports by their IATA or ICAO codes in one call.
     *
     * @param codes List of IATA or ICAO codes
     * @return List of Airport objects (null for codes not found), maintaining input order
     */
    fun getMultipleAirports(codes: List<String>): List<Airport?> {
        return codes.map { code -> resolveAirport(code) }
    }

    /**
     * Calculates distances between all pairs of airports in a list.
     *
     * @param codes List of IATA or ICAO codes (minimum 2)
     * @return DistanceMatrix with airport info and distance pairs
     * @throws AirportDataException if less than 2 codes provided or any code is invalid
     */
    fun calculateDistanceMatrix(codes: List<String>): DistanceMatrix {
        if (codes.size < 2) {
            throw AirportDataException("At least 2 airport codes are required for a distance matrix.")
        }

        val resolvedAirports = codes.map { code ->
            resolveAirport(code)
                ?: throw AirportDataException("Airport not found for code: $code")
        }

        val airportInfos = resolvedAirports.mapIndexed { i, airport ->
            AirportInfo(
                code = codes[i].trim().uppercase(),
                name = airport.airport,
                iata = airport.iata,
                icao = airport.icao
            )
        }

        val distances = mutableMapOf<String, MutableMap<String, Double>>()
        for (i in resolvedAirports.indices) {
            val codeI = codes[i].trim().uppercase()
            distances[codeI] = mutableMapOf()

            for (j in resolvedAirports.indices) {
                val codeJ = codes[j].trim().uppercase()
                if (i == j) {
                    distances[codeI]!![codeJ] = 0.0
                } else {
                    val dist = haversineDistance(
                        resolvedAirports[i].latitude, resolvedAirports[i].longitude,
                        resolvedAirports[j].latitude, resolvedAirports[j].longitude
                    )
                    distances[codeI]!![codeJ] = Math.round(dist).toDouble()
                }
            }
        }

        return DistanceMatrix(airports = airportInfos, distances = distances)
    }

    // ========================================================================
    // Validation & Utilities
    // ========================================================================

    /**
     * Validates if an IATA code exists in the database.
     *
     * @param code The IATA code to validate
     * @return true if valid and exists, false otherwise
     */
    fun validateIataCode(code: String): Boolean {
        val trimmed = code.trim()
        if (trimmed.length != 3) return false
        if (!trimmed.all { it.isUpperCase() }) return false
        return iataIndex.containsKey(trimmed)
    }

    /**
     * Validates if an ICAO code exists in the database.
     *
     * @param code The ICAO code to validate
     * @return true if valid and exists, false otherwise
     */
    fun validateIcaoCode(code: String): Boolean {
        val trimmed = code.trim()
        if (trimmed.length != 4) return false
        if (!trimmed.all { it.isUpperCase() || it.isDigit() }) return false
        return icaoIndex.containsKey(trimmed)
    }

    /**
     * Gets the count of airports matching the given filters without fetching all data.
     *
     * @param filters Optional map of filter criteria (same keys as findAirports)
     * @return Count of matching airports
     */
    fun getAirportCount(filters: Map<String, Any>? = null): Int {
        if (filters == null || filters.isEmpty()) {
            return airports.size
        }

        var candidates = airports.asSequence()
        candidates = applyFilters(candidates, filters)
        return candidates.count()
    }

    /**
     * Checks if an airport has scheduled commercial service.
     *
     * @param code IATA or ICAO code
     * @return true if the airport has scheduled service
     * @throws AirportDataException if the airport is not found
     */
    fun isAirportOperational(code: String): Boolean {
        val airport = resolveAirport(code)
            ?: throw AirportDataException("Airport not found for code: $code")

        return airport.hasScheduledService
    }

    // ========================================================================
    // Internal Helpers
    // ========================================================================

    /**
     * Resolves an airport by either IATA or ICAO code.
     */
    private fun resolveAirport(code: String): Airport? {
        val trimmed = code.trim().uppercase()
        return when (trimmed.length) {
            3 -> iataIndex[trimmed]?.firstOrNull()?.let { airports[it] }
            4 -> icaoIndex[trimmed]?.firstOrNull()?.let { airports[it] }
            else -> null
        }
    }

    /**
     * Applies filter criteria to a sequence of airports.
     */
    private fun applyFilters(
        candidates: Sequence<Airport>,
        filters: Map<String, Any>
    ): Sequence<Airport> {
        var result = candidates

        filters["country_code"]?.let { value ->
            val cc = value.toString().uppercase()
            result = result.filter { it.countryCode.equals(cc, ignoreCase = true) }
        }

        filters["continent"]?.let { value ->
            val cont = value.toString().uppercase()
            result = result.filter { it.continent.equals(cont, ignoreCase = true) }
        }

        filters["type"]?.let { value ->
            val typeStr = value.toString().lowercase()
            if (typeStr == "airport") {
                result = result.filter {
                    it.type.lowercase().contains("airport")
                }
            } else {
                result = result.filter { it.type.equals(typeStr, ignoreCase = true) }
            }
        }

        filters["has_scheduled_service"]?.let { value ->
            val scheduled = when (value) {
                is Boolean -> value
                is String -> value.equals("true", ignoreCase = true)
                else -> false
            }
            result = result.filter { it.hasScheduledService == scheduled }
        }

        filters["min_runway_ft"]?.let { value ->
            val minRunway = when (value) {
                is Int -> value
                is Long -> value.toInt()
                is Double -> value.toInt()
                is String -> value.toIntOrNull() ?: 0
                else -> 0
            }
            result = result.filter { (it.runwayLength ?: 0) >= minRunway }
        }

        return result
    }

    /**
     * Haversine formula to calculate the great-circle distance between two points.
     */
    private fun haversineDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_KM * c
    }

    /**
     * Computes country-level statistics.
     */
    private fun computeCountryStats(countryAirports: List<Airport>): CountryStats {
        val byType = countryAirports.groupBy { it.type }
            .mapValues { it.value.size }

        val withScheduledService = countryAirports.count { it.hasScheduledService }

        val runwayLengths = countryAirports.mapNotNull { it.runwayLength }
        val averageRunwayLength = if (runwayLengths.isNotEmpty()) {
            runwayLengths.average()
        } else 0.0

        val elevations = countryAirports.mapNotNull { it.elevationFt }
        val averageElevation = if (elevations.isNotEmpty()) {
            elevations.average()
        } else 0.0

        val timezones = countryAirports.map { it.time }
            .filter { it.isNotEmpty() }
            .distinct()
            .sorted()

        return CountryStats(
            total = countryAirports.size,
            byType = byType,
            withScheduledService = withScheduledService,
            averageRunwayLength = averageRunwayLength,
            averageElevation = averageElevation,
            timezones = timezones
        )
    }

    /**
     * Computes continent-level statistics.
     */
    private fun computeContinentStats(continentAirports: List<Airport>): ContinentStats {
        val byType = continentAirports.groupBy { it.type }
            .mapValues { it.value.size }

        val byCountry = continentAirports.groupBy { it.countryCode }
            .mapValues { it.value.size }
            .filter { it.key.isNotEmpty() }

        val withScheduledService = continentAirports.count { it.hasScheduledService }

        val runwayLengths = continentAirports.mapNotNull { it.runwayLength }
        val averageRunwayLength = if (runwayLengths.isNotEmpty()) {
            runwayLengths.average()
        } else 0.0

        val elevations = continentAirports.mapNotNull { it.elevationFt }
        val averageElevation = if (elevations.isNotEmpty()) {
            elevations.average()
        } else 0.0

        val timezones = continentAirports.map { it.time }
            .filter { it.isNotEmpty() }
            .distinct()
            .sorted()

        return ContinentStats(
            total = continentAirports.size,
            byType = byType,
            byCountry = byCountry,
            withScheduledService = withScheduledService,
            averageRunwayLength = averageRunwayLength,
            averageElevation = averageElevation,
            timezones = timezones
        )
    }
}

/**
 * Result of findNearestAirport, wrapping an Airport with its distance.
 */
data class NearestAirportResult(
    val airport: Airport,
    val distance: Double
) {
    // Delegate all Airport properties for convenience
    val iata: String get() = airport.iata
    val icao: String get() = airport.icao
    val time: String get() = airport.time
    val countryCode: String get() = airport.countryCode
    val continent: String get() = airport.continent
    val airportName: String get() = airport.airport
    val latitude: Double get() = airport.latitude
    val longitude: Double get() = airport.longitude
    val elevationFt: Int? get() = airport.elevationFt
    val type: String get() = airport.type
    val scheduledService: String get() = airport.scheduledService
    val hasScheduledService: Boolean get() = airport.hasScheduledService
    val wikipedia: String get() = airport.wikipedia
    val website: String get() = airport.website
    val runwayLength: Int? get() = airport.runwayLength
    val flightradar24Url: String get() = airport.flightradar24Url
    val radarboxUrl: String get() = airport.radarboxUrl
    val flightawareUrl: String get() = airport.flightawareUrl
}

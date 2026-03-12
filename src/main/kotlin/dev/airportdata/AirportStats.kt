package dev.airportdata

/**
 * Comprehensive statistics about airports in a country.
 */
data class CountryStats(
    val total: Int,
    val byType: Map<String, Int>,
    val withScheduledService: Int,
    val averageRunwayLength: Double,
    val averageElevation: Double,
    val timezones: List<String>
)

/**
 * Comprehensive statistics about airports on a continent.
 */
data class ContinentStats(
    val total: Int,
    val byType: Map<String, Int>,
    val byCountry: Map<String, Int>,
    val withScheduledService: Int,
    val averageRunwayLength: Double,
    val averageElevation: Double,
    val timezones: List<String>
)

/**
 * Information about an airport in a distance matrix.
 */
data class AirportInfo(
    val code: String,
    val name: String,
    val iata: String,
    val icao: String
)

/**
 * Result of a distance matrix calculation between multiple airports.
 */
data class DistanceMatrix(
    val airports: List<AirportInfo>,
    val distances: Map<String, Map<String, Double>>
)

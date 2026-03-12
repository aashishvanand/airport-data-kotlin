package dev.airportdata

/**
 * External links associated with an airport.
 */
data class AirportLinks(
    val website: String?,
    val wikipedia: String?,
    val flightradar24: String?,
    val radarbox: String?,
    val flightaware: String?
)

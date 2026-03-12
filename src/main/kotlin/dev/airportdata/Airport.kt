package dev.airportdata

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull

/**
 * Serializer that handles JSON fields which can be either an integer or an empty string.
 * Returns null for empty strings or missing values.
 */
internal object IntOrEmptySerializer : KSerializer<Int?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("IntOrEmpty", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Int?) {
        if (value != null) encoder.encodeInt(value) else encoder.encodeString("")
    }

    override fun deserialize(decoder: Decoder): Int? {
        val jsonDecoder = decoder as? JsonDecoder ?: return decoder.decodeString().toIntOrNull()
        val element = jsonDecoder.decodeJsonElement() as? JsonPrimitive ?: return null
        return element.intOrNull ?: element.content.toIntOrNull()
    }
}

/**
 * Serializer that handles JSON fields which can be either a double/int or an empty string.
 * Returns null for empty strings or missing values.
 */
internal object DoubleOrEmptySerializer : KSerializer<Double?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("DoubleOrEmpty", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Double?) {
        if (value != null) encoder.encodeDouble(value) else encoder.encodeString("")
    }

    override fun deserialize(decoder: Decoder): Double? {
        val jsonDecoder = decoder as? JsonDecoder ?: return decoder.decodeString().toDoubleOrNull()
        val element = jsonDecoder.decodeJsonElement() as? JsonPrimitive ?: return null
        return element.doubleOrNull ?: element.content.toDoubleOrNull()
    }
}

/**
 * Represents an airport with all associated data.
 *
 * @property iata 3-letter IATA code (e.g., "SIN")
 * @property icao 4-letter ICAO code (e.g., "WSSS")
 * @property time Timezone identifier (e.g., "Asia/Singapore")
 * @property utc UTC offset (can be fractional, e.g., 5.5)
 * @property countryCode 2-letter ISO country code (e.g., "SG")
 * @property continent 2-letter continent code (AS, EU, NA, SA, AF, OC, AN)
 * @property airport Full airport name
 * @property latitude Latitude coordinate
 * @property longitude Longitude coordinate
 * @property elevationFt Elevation in feet (null if not available)
 * @property type Airport type (large_airport, medium_airport, small_airport, heliport, seaplane_base)
 * @property scheduledService Whether the airport has scheduled commercial service ("TRUE" or "FALSE")
 * @property wikipedia Wikipedia URL
 * @property website Airport website URL
 * @property runwayLength Longest runway length in feet (null if not available)
 * @property flightradar24Url Flightradar24 tracking URL
 * @property radarboxUrl RadarBox tracking URL
 * @property flightawareUrl FlightAware tracking URL
 */
@Serializable
data class Airport(
    val iata: String = "",
    val icao: String = "",
    val time: String = "",
    @Serializable(with = DoubleOrEmptySerializer::class)
    val utc: Double? = null,
    @SerialName("country_code")
    val countryCode: String = "",
    val continent: String = "",
    val airport: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    @SerialName("elevation_ft")
    @Serializable(with = IntOrEmptySerializer::class)
    val elevationFt: Int? = null,
    val type: String = "",
    @SerialName("scheduled_service")
    val scheduledService: String = "",
    val wikipedia: String = "",
    val website: String = "",
    @SerialName("runway_length")
    @Serializable(with = IntOrEmptySerializer::class)
    val runwayLength: Int? = null,
    @SerialName("flightradar24_url")
    val flightradar24Url: String = "",
    @SerialName("radarbox_url")
    val radarboxUrl: String = "",
    @SerialName("flightaware_url")
    val flightawareUrl: String = ""
) {
    /**
     * Whether this airport has scheduled commercial service.
     */
    val hasScheduledService: Boolean
        get() = scheduledService.equals("TRUE", ignoreCase = true)
}

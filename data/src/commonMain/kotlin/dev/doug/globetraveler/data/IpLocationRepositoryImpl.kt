package dev.doug.globetraveler.data

import co.touchlab.kermit.Logger
import dev.doug.globetraveler.domain.ApproximateLocation
import dev.doug.globetraveler.domain.DeviceLocationRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable

/**
 * IP-geolocation implementation: polls geojs.io's bare-IP endpoint every [pollInterval] and
 * performs the (heavier) geo lookup only when the IP actually changed. State-level accuracy.
 */
internal class IpLocationRepositoryImpl(
    private val httpClient: HttpClient,
    private val pollInterval: Duration = 1.minutes,
) : DeviceLocationRepository {

    private val log = Logger.withTag("IpLocationRepository")

    override fun observeLocation(): Flow<ApproximateLocation> = flow {
        var lastIp: String? = null
        while (true) {
            try {
                val ip = httpClient.get(IP_URL).body<IpResponse>().ip
                if (ip != lastIp) {
                    val geo = httpClient.get(GEO_URL).body<GeoResponse>()
                    emit(ApproximateLocation(geo.latitude.toDouble(), geo.longitude.toDouble()))
                    // Only after a successful emit, so a failed geo lookup retries next poll.
                    lastIp = ip
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.w(e) { "location refresh failed; retrying in $pollInterval" }
            }
            delay(pollInterval)
        }
    }

    @Serializable
    private data class IpResponse(val ip: String)

    // geojs returns coordinates as JSON strings.
    @Serializable
    private data class GeoResponse(val latitude: String, val longitude: String)

    private companion object {
        const val IP_URL = "https://get.geojs.io/v1/ip.json"
        const val GEO_URL = "https://get.geojs.io/v1/ip/geo.json"
    }
}

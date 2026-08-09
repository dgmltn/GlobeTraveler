package dev.doug.globetraveler.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import co.touchlab.kermit.Logger
import dev.doug.globetraveler.domain.ApproximateLocation
import dev.doug.globetraveler.domain.DeviceLocationRepository
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.koin.core.scope.Scope

internal actual fun Scope.deviceLocationRepository(): DeviceLocationRepository =
    SystemLocationRepository(get<Context>())

/**
 * Framework [LocationManager] implementation: coarse fixes from the fused (API 31+) or network
 * provider. Waits for the coarse permission grant, then emits the last known position followed
 * by periodic updates.
 */
internal class SystemLocationRepository(
    private val context: Context,
    private val updateInterval: Duration = 1.minutes,
    private val permissionPollInterval: Duration = 3.seconds,
) : DeviceLocationRepository {

    private val log = Logger.withTag("SystemLocationRepository")

    override fun observeLocation(): Flow<ApproximateLocation> = callbackFlow {
        // The first-launch grant happens after collection starts; poll until it lands.
        while (!hasCoarsePermission()) delay(permissionPollInterval)

        val locationManager =
            context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val provider = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            LocationManager.FUSED_PROVIDER
        } else {
            LocationManager.NETWORK_PROVIDER
        }

        // Explicit object rather than a SAM lambda: pre-API-30 devices still invoke the legacy
        // methods, which the lambda wouldn't implement.
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                trySend(location.toApproximate())
            }

            override fun onProviderEnabled(provider: String) {}

            override fun onProviderDisabled(provider: String) {}

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        }

        try {
            locationManager.getLastKnownLocation(provider)?.let { trySend(it.toApproximate()) }
            locationManager.requestLocationUpdates(
                provider,
                updateInterval.inWholeMilliseconds,
                MIN_DISTANCE_METERS,
                listener,
                Looper.getMainLooper(),
            )
        } catch (e: SecurityException) {
            log.w(e) { "location access rejected despite granted permission" }
        } catch (e: IllegalArgumentException) {
            log.w(e) { "location provider $provider unavailable" }
        }

        awaitClose { locationManager.removeUpdates(listener) }
    }

    private fun hasCoarsePermission(): Boolean =
        context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    private fun Location.toApproximate() = ApproximateLocation(latitude, longitude)

    private companion object {
        const val MIN_DISTANCE_METERS = 1_000f
    }
}

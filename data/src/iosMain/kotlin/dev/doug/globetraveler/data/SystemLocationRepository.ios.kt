package dev.doug.globetraveler.data

import co.touchlab.kermit.Logger
import dev.doug.globetraveler.domain.ApproximateLocation
import dev.doug.globetraveler.domain.DeviceLocationRepository
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.koin.core.scope.Scope
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLLocationAccuracyThreeKilometers
import platform.Foundation.NSError
import platform.darwin.NSObject

internal actual fun Scope.deviceLocationRepository(): DeviceLocationRepository =
    SystemLocationRepository()

/**
 * CoreLocation implementation at reduced (~3 km) accuracy. Requests when-in-use authorization
 * itself on first use; denied authorization simply means no emissions.
 */
internal class SystemLocationRepository : DeviceLocationRepository {

    private val log = Logger.withTag("SystemLocationRepository")

    override fun observeLocation(): Flow<ApproximateLocation> = callbackFlow {
        val manager = CLLocationManager()
        val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
            override fun locationManager(
                manager: CLLocationManager,
                didUpdateLocations: List<*>,
            ) {
                val location = didUpdateLocations.lastOrNull() as? CLLocation ?: return
                @OptIn(ExperimentalForeignApi::class)
                location.coordinate.useContents {
                    trySend(ApproximateLocation(latitude, longitude))
                }
            }

            // Fires once when the delegate is set, and again after the user answers the
            // authorization prompt — so all start-up paths funnel through here.
            override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
                if (manager.authorizationStatus == kCLAuthorizationStatusNotDetermined) {
                    manager.requestWhenInUseAuthorization()
                } else {
                    manager.startUpdatingLocation()
                }
            }

            override fun locationManager(
                manager: CLLocationManager,
                didFailWithError: NSError,
            ) {
                log.w { "location update failed: ${didFailWithError.localizedDescription}" }
            }
        }
        manager.desiredAccuracy = kCLLocationAccuracyThreeKilometers
        manager.delegate = delegate

        awaitClose {
            // Capturing `delegate` here also keeps it alive while collected — CLLocationManager
            // holds its delegate weakly.
            manager.stopUpdatingLocation()
            if (manager.delegate === delegate) manager.delegate = null
        }
    }
}

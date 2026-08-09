package dev.doug.globetraveler.data

import dev.doug.globetraveler.domain.ApproximateLocation
import dev.doug.globetraveler.domain.DeviceLocationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.koin.core.scope.Scope

internal actual fun Scope.deviceLocationRepository(): DeviceLocationRepository =
    NoLocationRepository

/** The JVM target exists only for tests; there is no system location service. */
private object NoLocationRepository : DeviceLocationRepository {
    override fun observeLocation(): Flow<ApproximateLocation> = emptyFlow()
}

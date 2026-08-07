package dev.doug.globetraveler.domain

import kotlinx.coroutines.flow.Flow

/** A rough device position — state-level accuracy is all callers may assume. */
data class ApproximateLocation(val latitude: Double, val longitude: Double)

interface DeviceLocationRepository {
    /**
     * Cold stream of the device's approximate location. Emits on the first successful fix and
     * again whenever the position is re-resolved (the dot can move). Never emits null; no
     * emission means the location isn't known yet. Implementations must not throw — failures
     * are logged and retried internally.
     */
    fun observeLocation(): Flow<ApproximateLocation>
}

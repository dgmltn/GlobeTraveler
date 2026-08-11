package dev.doug.globetraveler.domain

import kotlinx.coroutines.flow.Flow

interface VisitRepository {
    fun observeVisits(trackedMapId: TrackedMapId): Flow<List<Visit>>

    /** Visit counts across all tracked maps, for the switcher menu. */
    fun observeVisitCounts(): Flow<Map<TrackedMapId, Int>>

    suspend fun toggle(regionId: RegionId)
    suspend fun updateDetails(visit: Visit)
}

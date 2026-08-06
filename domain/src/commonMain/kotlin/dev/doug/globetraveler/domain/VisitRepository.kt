package dev.doug.globetraveler.domain

import kotlinx.coroutines.flow.Flow

interface VisitRepository {
    fun observeVisits(mapId: MapId): Flow<List<Visit>>
    suspend fun toggle(regionId: RegionId)
    suspend fun updateDetails(visit: Visit)
}

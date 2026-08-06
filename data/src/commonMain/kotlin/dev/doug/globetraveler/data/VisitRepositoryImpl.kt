package dev.doug.globetraveler.data

import dev.doug.globetraveler.domain.MapId
import dev.doug.globetraveler.domain.RegionId
import dev.doug.globetraveler.domain.Visit
import dev.doug.globetraveler.domain.VisitRepository
import kotlin.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class VisitRepositoryImpl(
    private val dao: VisitDao,
    private val clock: Clock,
) : VisitRepository {

    override fun observeVisits(mapId: MapId): Flow<List<Visit>> =
        dao.observe(mapId.value).map { entities -> entities.map(VisitEntity::toVisit) }

    override suspend fun toggle(regionId: RegionId) {
        val existing = dao.get(regionId.mapId.value, regionId.code.value)
        if (existing == null) {
            dao.upsert(
                VisitEntity(
                    mapId = regionId.mapId.value,
                    regionCode = regionId.code.value,
                    visitedAt = null,
                    notes = null,
                    markedAtEpochMillis = clock.now().toEpochMilliseconds(),
                ),
            )
        } else {
            dao.delete(regionId.mapId.value, regionId.code.value)
        }
    }

    override suspend fun updateDetails(visit: Visit) {
        val existing = dao.get(visit.regionId.mapId.value, visit.regionId.code.value)
        dao.upsert(
            visit.toEntity().copy(
                markedAtEpochMillis = existing?.markedAtEpochMillis
                    ?: clock.now().toEpochMilliseconds(),
            ),
        )
    }
}

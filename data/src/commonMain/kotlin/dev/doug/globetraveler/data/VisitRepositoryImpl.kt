package dev.doug.globetraveler.data

import dev.doug.globetraveler.domain.RegionId
import dev.doug.globetraveler.domain.TrackedMapId
import dev.doug.globetraveler.domain.Visit
import dev.doug.globetraveler.domain.VisitRepository
import kotlin.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class VisitRepositoryImpl(
    private val dao: VisitDao,
    private val clock: Clock,
) : VisitRepository {

    override fun observeVisits(trackedMapId: TrackedMapId): Flow<List<Visit>> =
        dao.observe(trackedMapId.value).map { entities -> entities.map(VisitEntity::toVisit) }

    override fun observeVisitCounts(): Flow<Map<TrackedMapId, Int>> =
        dao.observeCounts().map { counts ->
            counts.associate { TrackedMapId(it.trackedMapId) to it.visits }
        }

    override suspend fun toggle(regionId: RegionId) {
        val existing = dao.get(regionId.trackedMapId.value, regionId.code.value)
        if (existing == null) {
            dao.upsert(
                VisitEntity(
                    trackedMapId = regionId.trackedMapId.value,
                    regionCode = regionId.code.value,
                    visitedAt = null,
                    notes = null,
                    markedAtEpochMillis = clock.now().toEpochMilliseconds(),
                ),
            )
        } else {
            dao.delete(regionId.trackedMapId.value, regionId.code.value)
        }
    }

    override suspend fun updateDetails(visit: Visit) {
        val existing = dao.get(visit.regionId.trackedMapId.value, visit.regionId.code.value)
        dao.upsert(
            visit.toEntity().copy(
                markedAtEpochMillis = existing?.markedAtEpochMillis
                    ?: clock.now().toEpochMilliseconds(),
            ),
        )
    }
}

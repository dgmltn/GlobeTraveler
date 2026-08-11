package dev.doug.globetraveler.data

import androidx.room3.Entity
import dev.doug.globetraveler.domain.RegionCode
import dev.doug.globetraveler.domain.RegionId
import dev.doug.globetraveler.domain.TrackedMapId
import dev.doug.globetraveler.domain.Visit
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

@Entity(tableName = "visits", primaryKeys = ["trackedMapId", "regionCode"])
data class VisitEntity(
    val trackedMapId: String,
    val regionCode: String,
    val visitedAt: String?,
    val notes: String?,
    val markedAtEpochMillis: Long,
)

internal fun VisitEntity.toVisit(): Visit = Visit(
    regionId = RegionId(TrackedMapId(trackedMapId), RegionCode(regionCode)),
    visitedAt = visitedAt?.let(LocalDate::parse),
    notes = notes,
    markedAt = Instant.fromEpochMilliseconds(markedAtEpochMillis),
)

internal fun Visit.toEntity(): VisitEntity = VisitEntity(
    trackedMapId = regionId.trackedMapId.value,
    regionCode = regionId.code.value,
    visitedAt = visitedAt?.toString(),
    notes = notes,
    markedAtEpochMillis = markedAt.toEpochMilliseconds(),
)

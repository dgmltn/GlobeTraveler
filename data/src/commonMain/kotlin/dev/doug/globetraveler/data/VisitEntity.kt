package dev.doug.globetraveler.data

import androidx.room3.Entity
import dev.doug.globetraveler.domain.MapId
import dev.doug.globetraveler.domain.RegionCode
import dev.doug.globetraveler.domain.RegionId
import dev.doug.globetraveler.domain.Visit
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

@Entity(tableName = "visits", primaryKeys = ["mapId", "regionCode"])
data class VisitEntity(
    val mapId: String,
    val regionCode: String,
    val visitedAt: String?,
    val notes: String?,
    val markedAtEpochMillis: Long,
)

internal fun VisitEntity.toVisit(): Visit = Visit(
    regionId = RegionId(MapId(mapId), RegionCode(regionCode)),
    visitedAt = visitedAt?.let(LocalDate::parse),
    notes = notes,
    markedAt = Instant.fromEpochMilliseconds(markedAtEpochMillis),
)

internal fun Visit.toEntity(): VisitEntity = VisitEntity(
    mapId = regionId.mapId.value,
    regionCode = regionId.code.value,
    visitedAt = visitedAt?.toString(),
    notes = notes,
    markedAtEpochMillis = markedAt.toEpochMilliseconds(),
)

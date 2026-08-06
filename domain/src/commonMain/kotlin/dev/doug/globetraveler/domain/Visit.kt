package dev.doug.globetraveler.domain

import kotlin.time.Instant
import kotlinx.datetime.LocalDate

data class Visit(
    val regionId: RegionId,
    val visitedAt: LocalDate?,
    val notes: String?,
    val markedAt: Instant,
)

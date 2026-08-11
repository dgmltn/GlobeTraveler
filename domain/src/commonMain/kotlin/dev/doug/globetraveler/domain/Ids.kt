package dev.doug.globetraveler.domain

import kotlin.jvm.JvmInline

@JvmInline
value class MapId(val value: String)

@JvmInline
value class RegionCode(val value: String)

data class RegionId(val trackedMapId: TrackedMapId, val code: RegionCode)

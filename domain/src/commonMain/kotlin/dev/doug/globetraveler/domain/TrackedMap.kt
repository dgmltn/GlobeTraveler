package dev.doug.globetraveler.domain

import kotlin.jvm.JvmInline
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow

@JvmInline
value class TrackedMapId(val value: String)

/** Accent identity for a tracked map; actual colors live in `:design`. */
enum class MapAccent { Green, Blue, Orange, Purple, Red, Teal }

/**
 * A named tracking collection over a geography pack — e.g. "Visited" or "License plates"
 * over us-states. Visits are keyed per tracked map, so each collection has independent
 * marks, dates, and notes.
 */
data class TrackedMap(
    val id: TrackedMapId,
    val packId: MapId,
    val name: String,
    val accent: MapAccent,
    val createdAt: Instant,
)

interface TrackedMapRepository {
    /** All maps in creation order. Never empty: a default "Visited" map is seeded. */
    fun observeMaps(): Flow<List<TrackedMap>>

    /** The selected map, falling back to the oldest when nothing was selected yet. */
    fun observeActiveMap(): Flow<TrackedMap>

    /** Creates a map with the next unused accent and makes it active. */
    suspend fun create(name: String): TrackedMap

    suspend fun setActive(id: TrackedMapId)

    suspend fun rename(id: TrackedMapId, name: String)

    /** Deletes the map and its visits. No-op on the last remaining map. */
    suspend fun delete(id: TrackedMapId)
}

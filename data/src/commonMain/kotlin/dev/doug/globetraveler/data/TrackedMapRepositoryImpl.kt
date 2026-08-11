package dev.doug.globetraveler.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.doug.globetraveler.domain.MapAccent
import dev.doug.globetraveler.domain.MapId
import dev.doug.globetraveler.domain.TrackedMap
import dev.doug.globetraveler.domain.TrackedMapId
import dev.doug.globetraveler.domain.TrackedMapRepository
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

class TrackedMapRepositoryImpl(
    private val dao: TrackedMapDao,
    private val dataStore: DataStore<Preferences>,
    private val clock: Clock,
) : TrackedMapRepository {

    override fun observeMaps(): Flow<List<TrackedMap>> = dao.observeAll()
        .onStart { seedIfEmpty() }
        .map { entities -> entities.map(TrackedMapEntity::toTrackedMap) }
        // The seed lands before collection starts, so an empty emission can only be a
        // transient state during a fresh install's first frame — suppress it.
        .filter { it.isNotEmpty() }

    override fun observeActiveMap(): Flow<TrackedMap> = combine(
        observeMaps(),
        dataStore.data.map { preferences -> preferences[ACTIVE_MAP_KEY] },
    ) { maps, activeId ->
        // Unset or dangling (deleted map) selection falls back to the oldest map.
        maps.firstOrNull { it.id.value == activeId } ?: maps.first()
    }.distinctUntilChanged()

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun create(name: String): TrackedMap {
        val existing = observeMaps().first()
        val used = existing.map { it.accent }.toSet()
        val accent = MapAccent.entries.firstOrNull { it !in used }
            ?: MapAccent.entries[existing.size % MapAccent.entries.size]
        val map = TrackedMap(
            id = TrackedMapId(Uuid.random().toString()),
            packId = MapId(DEFAULT_PACK_ID),
            name = name.trim(),
            accent = accent,
            createdAt = clock.now(),
        )
        dao.upsert(map.toEntity())
        setActive(map.id)
        return map
    }

    override suspend fun setActive(id: TrackedMapId) {
        dataStore.edit { preferences -> preferences[ACTIVE_MAP_KEY] = id.value }
    }

    // Fixed id makes concurrent seeding idempotent (upsert of the same row).
    private suspend fun seedIfEmpty() {
        if (dao.count() == 0) {
            dao.upsert(
                TrackedMapEntity(
                    id = DEFAULT_MAP_ID,
                    packId = DEFAULT_PACK_ID,
                    name = "Visited",
                    accent = MapAccent.Green.name,
                    createdAtEpochMillis = clock.now().toEpochMilliseconds(),
                ),
            )
        }
    }

    private companion object {
        val ACTIVE_MAP_KEY = stringPreferencesKey("activeMapId")
        const val DEFAULT_MAP_ID = "visited"
        const val DEFAULT_PACK_ID = "us-states"
    }
}

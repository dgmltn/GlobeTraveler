package dev.doug.globetraveler.data

import androidx.room3.Dao
import androidx.room3.Entity
import androidx.room3.Query
import androidx.room3.Upsert
import dev.doug.globetraveler.domain.MapAccent
import dev.doug.globetraveler.domain.MapId
import dev.doug.globetraveler.domain.TrackedMap
import dev.doug.globetraveler.domain.TrackedMapId
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "tracked_maps")
data class TrackedMapEntity(
    @androidx.room3.PrimaryKey val id: String,
    val packId: String,
    val name: String,
    val accent: String,
    val createdAtEpochMillis: Long,
)

@Dao
interface TrackedMapDao {

    @Query("SELECT * FROM tracked_maps ORDER BY createdAtEpochMillis")
    fun observeAll(): Flow<List<TrackedMapEntity>>

    @Query("SELECT COUNT(*) FROM tracked_maps")
    suspend fun count(): Int

    @Upsert
    suspend fun upsert(entity: TrackedMapEntity)

    @Query("UPDATE tracked_maps SET name = :name WHERE id = :id")
    suspend fun rename(id: String, name: String)

    @Query("DELETE FROM tracked_maps WHERE id = :id")
    suspend fun delete(id: String)
}

internal fun TrackedMapEntity.toTrackedMap(): TrackedMap = TrackedMap(
    id = TrackedMapId(id),
    packId = MapId(packId),
    name = name,
    // valueOf would crash on an unknown value from a newer schema; fall back instead.
    accent = MapAccent.entries.firstOrNull { it.name == accent } ?: MapAccent.Green,
    createdAt = Instant.fromEpochMilliseconds(createdAtEpochMillis),
)

internal fun TrackedMap.toEntity(): TrackedMapEntity = TrackedMapEntity(
    id = id.value,
    packId = packId.value,
    name = name,
    accent = accent.name,
    createdAtEpochMillis = createdAt.toEpochMilliseconds(),
)

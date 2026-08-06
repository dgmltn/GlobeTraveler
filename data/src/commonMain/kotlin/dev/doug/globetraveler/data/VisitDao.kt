package dev.doug.globetraveler.data

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface VisitDao {

    @Query("SELECT * FROM visits WHERE mapId = :mapId")
    fun observe(mapId: String): Flow<List<VisitEntity>>

    @Query("SELECT * FROM visits WHERE mapId = :mapId AND regionCode = :code")
    suspend fun get(mapId: String, code: String): VisitEntity?

    @Upsert
    suspend fun upsert(entity: VisitEntity)

    @Query("DELETE FROM visits WHERE mapId = :mapId AND regionCode = :code")
    suspend fun delete(mapId: String, code: String)
}

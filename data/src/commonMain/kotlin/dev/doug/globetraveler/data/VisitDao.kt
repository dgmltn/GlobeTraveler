package dev.doug.globetraveler.data

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface VisitDao {

    @Query("SELECT * FROM visits WHERE trackedMapId = :trackedMapId")
    fun observe(trackedMapId: String): Flow<List<VisitEntity>>

    @Query("SELECT trackedMapId, COUNT(*) AS visits FROM visits GROUP BY trackedMapId")
    fun observeCounts(): Flow<List<VisitCount>>

    @Query("SELECT * FROM visits WHERE trackedMapId = :trackedMapId AND regionCode = :code")
    suspend fun get(trackedMapId: String, code: String): VisitEntity?

    @Upsert
    suspend fun upsert(entity: VisitEntity)

    @Query("DELETE FROM visits WHERE trackedMapId = :trackedMapId AND regionCode = :code")
    suspend fun delete(trackedMapId: String, code: String)
}

data class VisitCount(val trackedMapId: String, val visits: Int)

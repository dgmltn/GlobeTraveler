package dev.doug.globetraveler.data

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

@Database(entities = [VisitEntity::class, TrackedMapEntity::class], version = 2)
@ConstructedBy(GlobeDatabaseConstructor::class)
abstract class GlobeDatabase : RoomDatabase() {
    abstract fun visitDao(): VisitDao
    abstract fun trackedMapDao(): TrackedMapDao
}

/**
 * v1 kept one implicit collection: visits keyed by the geography pack id. v2 introduces
 * tracked maps; every existing visit becomes part of a seeded "Visited" map.
 */
internal val MIGRATION_1_2 = object : Migration(1, 2) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE tracked_maps (" +
                "id TEXT NOT NULL PRIMARY KEY, packId TEXT NOT NULL, name TEXT NOT NULL, " +
                "accent TEXT NOT NULL, createdAtEpochMillis INTEGER NOT NULL)",
        )
        connection.execSQL(
            "INSERT INTO tracked_maps VALUES ('visited', 'us-states', 'Visited', 'Green', 0)",
        )
        connection.execSQL(
            "CREATE TABLE visits_new (" +
                "trackedMapId TEXT NOT NULL, regionCode TEXT NOT NULL, visitedAt TEXT, " +
                "notes TEXT, markedAtEpochMillis INTEGER NOT NULL, " +
                "PRIMARY KEY(trackedMapId, regionCode))",
        )
        connection.execSQL(
            "INSERT INTO visits_new " +
                "SELECT 'visited', regionCode, visitedAt, notes, markedAtEpochMillis FROM visits",
        )
        connection.execSQL("DROP TABLE visits")
        connection.execSQL("ALTER TABLE visits_new RENAME TO visits")
    }
}

@Suppress("KotlinNoActualForExpect")
expect object GlobeDatabaseConstructor : RoomDatabaseConstructor<GlobeDatabase> {
    override fun initialize(): GlobeDatabase
}

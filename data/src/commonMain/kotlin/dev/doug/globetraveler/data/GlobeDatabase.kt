package dev.doug.globetraveler.data

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor

@Database(entities = [VisitEntity::class], version = 1)
@ConstructedBy(GlobeDatabaseConstructor::class)
abstract class GlobeDatabase : RoomDatabase() {
    abstract fun visitDao(): VisitDao
}

@Suppress("KotlinNoActualForExpect")
expect object GlobeDatabaseConstructor : RoomDatabaseConstructor<GlobeDatabase> {
    override fun initialize(): GlobeDatabase
}

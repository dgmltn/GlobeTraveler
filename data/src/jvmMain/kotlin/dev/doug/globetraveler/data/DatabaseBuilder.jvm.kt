package dev.doug.globetraveler.data

import androidx.room3.Room
import androidx.room3.RoomDatabase
import java.io.File
import org.koin.core.scope.Scope

internal actual fun Scope.databaseBuilder(): RoomDatabase.Builder<GlobeDatabase> =
    Room.databaseBuilder<GlobeDatabase>(
        name = File(System.getProperty("java.io.tmpdir"), "globe.db").absolutePath,
    )

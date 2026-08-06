package dev.doug.globetraveler.data

import androidx.room3.Room
import androidx.room3.RoomDatabase
import org.koin.core.scope.Scope
import platform.Foundation.NSHomeDirectory

internal actual fun Scope.databaseBuilder(): RoomDatabase.Builder<GlobeDatabase> =
    Room.databaseBuilder<GlobeDatabase>(
        name = NSHomeDirectory() + "/Documents/globe.db",
    )

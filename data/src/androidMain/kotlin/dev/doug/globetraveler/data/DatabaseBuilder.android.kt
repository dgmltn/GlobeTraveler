package dev.doug.globetraveler.data

import android.content.Context
import androidx.room3.Room
import androidx.room3.RoomDatabase
import org.koin.core.scope.Scope

internal actual fun Scope.databaseBuilder(): RoomDatabase.Builder<GlobeDatabase> {
    val context = get<Context>()
    return Room.databaseBuilder<GlobeDatabase>(
        context = context,
        name = context.getDatabasePath("globe.db").absolutePath,
    )
}

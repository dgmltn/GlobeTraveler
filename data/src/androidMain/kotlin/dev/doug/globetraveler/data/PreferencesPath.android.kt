package dev.doug.globetraveler.data

import android.content.Context
import okio.Path
import okio.Path.Companion.toPath
import org.koin.core.scope.Scope

internal actual fun Scope.preferencesPath(): Path =
    get<Context>().filesDir.resolve("globe.preferences_pb").absolutePath.toPath()

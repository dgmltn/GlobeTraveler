package dev.doug.globetraveler.data

import java.io.File
import okio.Path
import okio.Path.Companion.toPath
import org.koin.core.scope.Scope

internal actual fun Scope.preferencesPath(): Path =
    File(System.getProperty("java.io.tmpdir"), "globe.preferences_pb").absolutePath.toPath()

package dev.doug.globetraveler.data

import okio.Path
import okio.Path.Companion.toPath
import org.koin.core.scope.Scope
import platform.Foundation.NSHomeDirectory

internal actual fun Scope.preferencesPath(): Path =
    (NSHomeDirectory() + "/Documents/globe.preferences_pb").toPath()

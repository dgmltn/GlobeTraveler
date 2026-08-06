package dev.doug.globetraveler.app

import androidx.compose.ui.window.ComposeUIViewController
import dev.doug.globetraveler.design.GlobeTheme
import dev.doug.globetraveler.map.MapScreen
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController {
    GlobeTheme {
        MapScreen()
    }
}

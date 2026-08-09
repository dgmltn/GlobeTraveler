package dev.doug.globetraveler.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import dev.doug.globetraveler.design.GlobeTheme
import dev.doug.globetraveler.map.MapScreen

class MainActivity : ComponentActivity() {

    // The you-are-here dot appears once granted; denial just means no dot.
    private val requestLocationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestLocationPermission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        setContent {
            GlobeTheme {
                MapScreen()
            }
        }
    }
}

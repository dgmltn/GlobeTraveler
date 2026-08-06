package dev.doug.globetraveler.app

import android.app.Application
import dev.doug.globetraveler.data.dataModule
import dev.doug.globetraveler.map.mapModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class GlobeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@GlobeApp)
            modules(dataModule(), mapModule())
        }
    }
}

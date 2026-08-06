package dev.doug.globetraveler.app

import dev.doug.globetraveler.data.dataModule
import dev.doug.globetraveler.map.mapModule
import org.koin.core.context.startKoin

fun doInitKoin() {
    startKoin {
        modules(dataModule(), mapModule())
    }
}

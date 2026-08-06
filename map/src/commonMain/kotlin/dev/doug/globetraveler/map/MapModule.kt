package dev.doug.globetraveler.map

import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

fun mapModule(): Module = module {
    viewModelOf(::MapViewModel)
}

package dev.doug.globetraveler.data

import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import dev.doug.globetraveler.domain.MapPackRepository
import dev.doug.globetraveler.domain.VisitRepository
import kotlin.time.Clock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.core.scope.Scope
import org.koin.dsl.module

internal expect fun Scope.databaseBuilder(): RoomDatabase.Builder<GlobeDatabase>

fun dataModule(): Module = module {
    single { Json { ignoreUnknownKeys = true } }
    single<Clock> { Clock.System }
    single<GlobeDatabase> {
        databaseBuilder()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
    }
    single { get<GlobeDatabase>().visitDao() }
    single<VisitRepository> { VisitRepositoryImpl(get(), get()) }
    single<MapPackRepository> { MapPackRepositoryImpl(get()) }
}

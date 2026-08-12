package dev.doug.globetraveler.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import dev.doug.globetraveler.domain.DeviceLocationRepository
import dev.doug.globetraveler.domain.MapPackRepository
import dev.doug.globetraveler.domain.TrackedMapRepository
import dev.doug.globetraveler.domain.VisitRepository
import kotlin.time.Clock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.serialization.json.Json
import okio.Path
import org.koin.core.module.Module
import org.koin.core.scope.Scope
import org.koin.dsl.module

internal expect fun Scope.databaseBuilder(): RoomDatabase.Builder<GlobeDatabase>

internal expect fun Scope.deviceLocationRepository(): DeviceLocationRepository

/** Absolute path for the preferences DataStore file (must end in .preferences_pb). */
internal expect fun Scope.preferencesPath(): Path

fun dataModule(): Module = module {
    single { Json { ignoreUnknownKeys = true } }
    single<Clock> { Clock.System }
    single<GlobeDatabase> {
        databaseBuilder()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .addMigrations(MIGRATION_1_2)
            .build()
    }
    single { get<GlobeDatabase>().visitDao() }
    single { get<GlobeDatabase>().trackedMapDao() }
    single<DataStore<Preferences>> {
        PreferenceDataStoreFactory.createWithPath(produceFile = { preferencesPath() })
    }
    single<VisitRepository> { VisitRepositoryImpl(get(), get()) }
    single<TrackedMapRepository> { TrackedMapRepositoryImpl(get(), get(), get(), get()) }
    single<MapPackRepository> { MapPackRepositoryImpl(get()) }
    single<DeviceLocationRepository> { deviceLocationRepository() }
}

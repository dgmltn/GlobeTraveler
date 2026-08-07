package dev.doug.globetraveler.data

import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import co.touchlab.kermit.Logger
import dev.doug.globetraveler.domain.DeviceLocationRepository
import dev.doug.globetraveler.domain.MapPackRepository
import dev.doug.globetraveler.domain.VisitRepository
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.Logger as KtorLogger
import io.ktor.serialization.kotlinx.json.json
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
    single {
        HttpClient {
            expectSuccess = true
            install(ContentNegotiation) {
                json(get())
            }
            install(Logging) {
                level = LogLevel.INFO
                logger = object : KtorLogger {
                    private val log = Logger.withTag("HttpClient")
                    override fun log(message: String) {
                        log.d { message }
                    }
                }
            }
        }
    }
    single<DeviceLocationRepository> { IpLocationRepositoryImpl(get()) }
}

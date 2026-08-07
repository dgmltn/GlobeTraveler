package dev.doug.globetraveler.data

import dev.doug.globetraveler.domain.ApproximateLocation
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json

/** In-memory stand-in for geojs.io: serves the bare-IP and geo endpoints, counts requests. */
private class FakeGeoJs {
    val ipRequests = AtomicInteger(0)
    val geoRequests = AtomicInteger(0)

    @Volatile var currentIp: String = "1.1.1.1"
    @Volatile var failIp: Boolean = false
    @Volatile var latitude: String = "33.0924"
    @Volatile var longitude: String = "-117.2624"

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    private val engine = MockEngine { request ->
        when (request.url.encodedPath) {
            "/v1/ip.json" -> {
                ipRequests.incrementAndGet()
                if (failIp) {
                    respond("upstream error", HttpStatusCode.InternalServerError)
                } else {
                    respond("""{"ip":"$currentIp"}""", HttpStatusCode.OK, jsonHeaders)
                }
            }
            "/v1/ip/geo.json" -> {
                geoRequests.incrementAndGet()
                respond(
                    """{"ip":"$currentIp","latitude":"$latitude","longitude":"$longitude","country":"United States"}""",
                    HttpStatusCode.OK,
                    jsonHeaders,
                )
            }
            else -> respond("not found", HttpStatusCode.NotFound)
        }
    }

    val client = HttpClient(engine) {
        expectSuccess = true
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }
}

class IpLocationRepositoryImplTest {

    private fun repository(server: FakeGeoJs) =
        IpLocationRepositoryImpl(server.client, pollInterval = 5.milliseconds)

    @Test
    fun `emits the geo lookup result as the first location`() = runTest {
        val server = FakeGeoJs()
        val first = withContext(Dispatchers.Default) {
            withTimeout(5.seconds) { repository(server).observeLocation().first() }
        }
        assertEquals(ApproximateLocation(latitude = 33.0924, longitude = -117.2624), first)
    }

    @Test
    fun `refreshes the location when the ip changes`() = runTest {
        val server = FakeGeoJs()
        val locations = withContext(Dispatchers.Default) {
            withTimeout(5.seconds) {
                repository(server).observeLocation()
                    .onEach {
                        // After each emission, "move" the device to a new network.
                        server.currentIp = "2.2.2.2"
                        server.latitude = "45.5152"
                        server.longitude = "-122.6784"
                    }
                    .take(2)
                    .toList()
            }
        }
        assertEquals(ApproximateLocation(latitude = 33.0924, longitude = -117.2624), locations[0])
        assertEquals(ApproximateLocation(latitude = 45.5152, longitude = -122.6784), locations[1])
    }

    @Test
    fun `does not re-query geo while the ip is unchanged`() = runTest {
        val server = FakeGeoJs()
        withContext(Dispatchers.Default) {
            val emissions = CopyOnWriteArrayList<ApproximateLocation>()
            val job = launch { repository(server).observeLocation().toList(emissions) }
            withTimeout(5.seconds) {
                while (server.ipRequests.get() < 5) delay(5.milliseconds)
            }
            job.cancel()
            job.join()
            assertEquals(1, emissions.size)
            assertEquals(1, server.geoRequests.get())
        }
    }

    @Test
    fun `emits nothing while polls fail and recovers on the next success`() = runTest {
        val server = FakeGeoJs().apply { failIp = true }
        val first = withContext(Dispatchers.Default) {
            launch {
                withTimeout(5.seconds) {
                    while (server.ipRequests.get() < 3) delay(5.milliseconds)
                }
                server.failIp = false
            }
            withTimeout(5.seconds) { repository(server).observeLocation().first() }
        }
        assertEquals(ApproximateLocation(latitude = 33.0924, longitude = -117.2624), first)
    }
}

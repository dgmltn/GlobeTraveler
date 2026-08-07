# "You are here" Dot Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Show an approximate, IP-geolocated "you are here" dot on the map that refreshes when the device's public IP changes.

**Architecture:** `:domain` gains a `DeviceLocationRepository` contract exposing a cold `Flow<ApproximateLocation>`; `:data` implements it by polling geojs.io's bare-IP endpoint and doing a geo lookup only when the IP changes; `:map` folds the flow into `MapUiState` and renders two `CircleLayer`s over a one-point GeoJSON source.

**Tech Stack:** Ktor client 3.5.2 (first HTTP dependency; okhttp/darwin/java engines per platform), kotlinx-serialization, Koin, maplibre-compose 0.13.1, Kermit.

## Global Constraints

- Spec: `docs/superpowers/specs/2026-08-06-you-are-here-design.md`.
- Work happens on branch `feat/you-are-here` (already created); WIP commits per task, squash-merged to `main` at the end.
- Commit subjects start with a bracketed tag (`[domain]`, `[data]`, `[map]`). No AI attribution trailers/footers.
- Ktor version **3.5.2** (pinned 2026-08-06 from Maven Central metadata — do not substitute remembered versions).
- geojs endpoints: `https://get.geojs.io/v1/ip.json` → `{"ip":"76.88.29.243"}`; `https://get.geojs.io/v1/ip/geo.json` → `latitude`/`longitude` are JSON **strings** (e.g. `"33.0924"`).
- Durations are `kotlin.time.Duration`, never unit-suffixed primitives.
- Tests await conditions with real timeouts (`withTimeout` on `Dispatchers.Default`); never `advanceUntilIdle()` + immediate assert.

---

### Task 1: Domain contract + Ktor catalog entries

**Files:**
- Create: `domain/src/commonMain/kotlin/dev/doug/globetraveler/domain/DeviceLocation.kt`
- Modify: `gradle/libs.versions.toml`
- Modify: `domain/README.md`

**Interfaces:**
- Produces: `ApproximateLocation(latitude: Double, longitude: Double)` data class; `DeviceLocationRepository` with `fun observeLocation(): Flow<ApproximateLocation>`; version-catalog aliases `libs.ktor.clientCore`, `libs.ktor.clientContentNegotiation`, `libs.ktor.serializationKotlinxJson`, `libs.ktor.clientLogging`, `libs.ktor.clientOkhttp`, `libs.ktor.clientDarwin`, `libs.ktor.clientJava`, `libs.ktor.clientMock`.

- [ ] **Step 1: Create the domain contract** (pure data class + interface — no test; logic lives in Task 2)

```kotlin
// domain/src/commonMain/kotlin/dev/doug/globetraveler/domain/DeviceLocation.kt
package dev.doug.globetraveler.domain

import kotlinx.coroutines.flow.Flow

/** A rough device position — state-level accuracy is all callers may assume. */
data class ApproximateLocation(val latitude: Double, val longitude: Double)

interface DeviceLocationRepository {
    /**
     * Cold stream of the device's approximate location. Emits on the first successful fix and
     * again whenever the position is re-resolved (the dot can move). Never emits null; no
     * emission means the location isn't known yet. Implementations must not throw — failures
     * are logged and retried internally.
     */
    fun observeLocation(): Flow<ApproximateLocation>
}
```

- [ ] **Step 2: Add Ktor to the version catalog**

In `gradle/libs.versions.toml` add to `[versions]`:

```toml
ktor = "3.5.2"
```

and to `[libraries]` (after the kermit line):

```toml
ktor-clientCore = { module = "io.ktor:ktor-client-core", version.ref = "ktor" }
ktor-clientContentNegotiation = { module = "io.ktor:ktor-client-content-negotiation", version.ref = "ktor" }
ktor-serializationKotlinxJson = { module = "io.ktor:ktor-serialization-kotlinx-json", version.ref = "ktor" }
ktor-clientLogging = { module = "io.ktor:ktor-client-logging", version.ref = "ktor" }
ktor-clientOkhttp = { module = "io.ktor:ktor-client-okhttp", version.ref = "ktor" }
ktor-clientDarwin = { module = "io.ktor:ktor-client-darwin", version.ref = "ktor" }
ktor-clientJava = { module = "io.ktor:ktor-client-java", version.ref = "ktor" }
ktor-clientMock = { module = "io.ktor:ktor-client-mock", version.ref = "ktor" }
```

- [ ] **Step 3: Update `domain/README.md`** — add `DeviceLocationRepository`/`ApproximateLocation` to its contents list, matching the file's existing style.

- [ ] **Step 4: Compile**

Run: `./gradlew :domain:compileKotlinJvm`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add domain gradle/libs.versions.toml
git commit -m "[domain] DeviceLocationRepository contract + Ktor catalog entries"
```

---

### Task 2: IP location repository in `:data` (TDD)

**Files:**
- Modify: `data/build.gradle.kts` (dependencies)
- Create: `data/src/commonMain/kotlin/dev/doug/globetraveler/data/IpLocationRepositoryImpl.kt`
- Test: `data/src/jvmTest/kotlin/dev/doug/globetraveler/data/IpLocationRepositoryImplTest.kt`

**Interfaces:**
- Consumes: `ApproximateLocation`, `DeviceLocationRepository` (Task 1).
- Produces: `internal class IpLocationRepositoryImpl(httpClient: HttpClient, pollInterval: Duration = 1.minutes) : DeviceLocationRepository` — Task 3 constructs it as `IpLocationRepositoryImpl(get())`.

- [ ] **Step 1: Add Ktor dependencies to `data/build.gradle.kts`**

In the `sourceSets` block, extend `commonMain.dependencies` with:

```kotlin
implementation(libs.ktor.clientCore)
implementation(libs.ktor.clientContentNegotiation)
implementation(libs.ktor.serializationKotlinxJson)
implementation(libs.ktor.clientLogging)
```

and add sibling blocks (androidMain/iosMain/jvmMain exist as directories; the DSL accessors are available):

```kotlin
androidMain.dependencies {
    implementation(libs.ktor.clientOkhttp)
}
iosMain.dependencies {
    implementation(libs.ktor.clientDarwin)
}
jvmMain.dependencies {
    implementation(libs.ktor.clientJava)
}
```

and extend `jvmTest.dependencies` with:

```kotlin
implementation(libs.kotlinx.coroutinesCore)
implementation(libs.ktor.clientMock)
```

- [ ] **Step 2: Write the failing tests**

```kotlin
// data/src/jvmTest/kotlin/dev/doug/globetraveler/data/IpLocationRepositoryImplTest.kt
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
```

- [ ] **Step 3: Run tests to verify they fail**

Run: `./gradlew :data:jvmTest --tests "dev.doug.globetraveler.data.IpLocationRepositoryImplTest" --console=plain`
Expected: FAIL — `IpLocationRepositoryImpl` unresolved (compilation error).

- [ ] **Step 4: Write the implementation**

```kotlin
// data/src/commonMain/kotlin/dev/doug/globetraveler/data/IpLocationRepositoryImpl.kt
package dev.doug.globetraveler.data

import co.touchlab.kermit.Logger
import dev.doug.globetraveler.domain.ApproximateLocation
import dev.doug.globetraveler.domain.DeviceLocationRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable

/**
 * IP-geolocation implementation: polls geojs.io's bare-IP endpoint every [pollInterval] and
 * performs the (heavier) geo lookup only when the IP actually changed. State-level accuracy.
 */
internal class IpLocationRepositoryImpl(
    private val httpClient: HttpClient,
    private val pollInterval: Duration = 1.minutes,
) : DeviceLocationRepository {

    private val log = Logger.withTag("IpLocationRepository")

    override fun observeLocation(): Flow<ApproximateLocation> = flow {
        var lastIp: String? = null
        while (true) {
            try {
                val ip = httpClient.get(IP_URL).body<IpResponse>().ip
                if (ip != lastIp) {
                    val geo = httpClient.get(GEO_URL).body<GeoResponse>()
                    emit(ApproximateLocation(geo.latitude.toDouble(), geo.longitude.toDouble()))
                    // Only after a successful emit, so a failed geo lookup retries next poll.
                    lastIp = ip
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.w(e) { "location refresh failed; retrying in $pollInterval" }
            }
            delay(pollInterval)
        }
    }

    @Serializable
    private data class IpResponse(val ip: String)

    // geojs returns coordinates as JSON strings.
    @Serializable
    private data class GeoResponse(val latitude: String, val longitude: String)

    private companion object {
        const val IP_URL = "https://get.geojs.io/v1/ip.json"
        const val GEO_URL = "https://get.geojs.io/v1/ip/geo.json"
    }
}
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `./gradlew :data:jvmTest --tests "dev.doug.globetraveler.data.IpLocationRepositoryImplTest" --console=plain`
Expected: PASS (4 tests)

- [ ] **Step 6: Commit**

```bash
git add data
git commit -m "[data] IpLocationRepositoryImpl: geojs.io polling behind DeviceLocationRepository"
```

---

### Task 3: Koin wiring (HttpClient + repository)

**Files:**
- Modify: `data/src/commonMain/kotlin/dev/doug/globetraveler/data/DataModule.kt`
- Modify: `data/README.md`

**Interfaces:**
- Consumes: `IpLocationRepositoryImpl(get())` (Task 2).
- Produces: Koin singletons `HttpClient` and `DeviceLocationRepository` — Task 4's ViewModel receives `DeviceLocationRepository` by constructor injection.

- [ ] **Step 1: Register the client and repository in `dataModule()`**

Add these singles to the existing `module { ... }` block (new imports: `dev.doug.globetraveler.domain.DeviceLocationRepository`, `io.ktor.client.HttpClient`, `io.ktor.client.plugins.contentnegotiation.ContentNegotiation`, `io.ktor.client.plugins.logging.LogLevel`, `io.ktor.client.plugins.logging.Logging`, `io.ktor.serialization.kotlinx.json.json`, `co.touchlab.kermit.Logger`; alias the Ktor logger interface as `import io.ktor.client.plugins.logging.Logger as KtorLogger`):

```kotlin
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
```

- [ ] **Step 2: Update `data/README.md`** — mention the Ktor HttpClient and `IpLocationRepositoryImpl` (geojs.io IP geolocation) in the module's contents/dependencies lists, matching existing style.

- [ ] **Step 3: Verify all targets still build**

Run: `./gradlew :data:jvmTest :app-android:assembleDebug :app-ios:linkDebugFrameworkIosSimulatorArm64 --console=plain`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add data
git commit -m "[data] Wire HttpClient + DeviceLocationRepository into Koin"
```

---

### Task 4: ViewModel + UiState (TDD)

**Files:**
- Modify: `map/src/commonMain/kotlin/dev/doug/globetraveler/map/MapUiState.kt`
- Modify: `map/src/commonMain/kotlin/dev/doug/globetraveler/map/MapViewModel.kt`
- Modify: `map/src/androidUnitTest/kotlin/dev/doug/globetraveler/map/fakes/Fakes.kt`
- Test: `map/src/androidUnitTest/kotlin/dev/doug/globetraveler/map/MapViewModelTest.kt`

**Interfaces:**
- Consumes: `DeviceLocationRepository`, `ApproximateLocation` (Task 1).
- Produces: `MapUiState.userLocation: ApproximateLocation?` (default null) — Task 5 renders it; `MapViewModel` constructor gains a third parameter `deviceLocationRepository: DeviceLocationRepository` (Koin resolves it automatically via the existing `viewModel { }` definition in `MapModule`; check `MapModule.kt` — if the definition is constructor-reflection-free like `viewModel { MapViewModel(get(), get()) }`, add a third `get()`).

- [ ] **Step 1: Add the fake to `Fakes.kt`**

```kotlin
class FakeDeviceLocationRepository : DeviceLocationRepository {
    private val locations = MutableSharedFlow<ApproximateLocation>(replay = 1)
    override fun observeLocation(): Flow<ApproximateLocation> = locations
    suspend fun emit(location: ApproximateLocation) = locations.emit(location)
}
```

(new imports: `dev.doug.globetraveler.domain.ApproximateLocation`, `dev.doug.globetraveler.domain.DeviceLocationRepository`, `kotlinx.coroutines.flow.MutableSharedFlow`)

- [ ] **Step 2: Write the failing test**

In `MapViewModelTest`, promote the location fake to a field so tests can drive it, passing it as the third constructor argument in `setUp`:

```kotlin
private lateinit var locationRepository: FakeDeviceLocationRepository

@BeforeTest
fun setUp() {
    Dispatchers.setMain(UnconfinedTestDispatcher())
    locationRepository = FakeDeviceLocationRepository()
    viewModel = MapViewModel(FakeMapPackRepository(), FakeVisitRepository(), locationRepository)
}
```

New test (imports: `dev.doug.globetraveler.domain.ApproximateLocation`, `dev.doug.globetraveler.map.fakes.FakeDeviceLocationRepository`):

```kotlin
@Test
fun `user location flows into state and can move`() = runTest {
    val loaded = awaitState { !it.loading }
    assertNull(loaded.userLocation)

    locationRepository.emit(ApproximateLocation(latitude = 33.0, longitude = -117.0))
    val located = awaitState { it.userLocation != null }
    assertEquals(ApproximateLocation(latitude = 33.0, longitude = -117.0), located.userLocation)

    locationRepository.emit(ApproximateLocation(latitude = 45.5, longitude = -122.6))
    awaitState { it.userLocation == ApproximateLocation(latitude = 45.5, longitude = -122.6) }
}
```

- [ ] **Step 3: Run tests to verify they fail**

Run: `./gradlew :map:testDebugUnitTest --tests "dev.doug.globetraveler.map.MapViewModelTest" --console=plain`
Expected: FAIL — compilation error (constructor arity / missing `userLocation`).

- [ ] **Step 4: Implement**

`MapUiState.kt` — add the field:

```kotlin
val userLocation: ApproximateLocation? = null,
```

(import `dev.doug.globetraveler.domain.ApproximateLocation`)

`MapViewModel.kt` — add the constructor parameter and fold the flow into `combine` (imports: `dev.doug.globetraveler.domain.ApproximateLocation`, `dev.doug.globetraveler.domain.DeviceLocationRepository`, `kotlinx.coroutines.flow.map`, `kotlinx.coroutines.flow.onStart`):

```kotlin
class MapViewModel(
    private val mapPackRepository: MapPackRepository,
    private val visitRepository: VisitRepository,
    deviceLocationRepository: DeviceLocationRepository,
) : ViewModel() {
```

```kotlin
    // combine() waits for every flow's first emission; the location stream may never emit
    // (offline), so it must open with an explicit "unknown" value.
    private val userLocation = deviceLocationRepository.observeLocation()
        .map<ApproximateLocation, ApproximateLocation?> { it }
        .onStart { emit(null) }

    val state: StateFlow<MapUiState> = combine(
        pack,
        visitRepository.observeVisits(US_STATES_MAP_ID),
        detailsCode,
        userLocation,
    ) { loaded, visits, details, location ->
        buildState(loaded, visits, details, location)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), MapUiState())
```

`buildState` gains the parameter and threads it through both branches:

```kotlin
    private fun buildState(
        pack: MapPack?,
        visits: List<Visit>,
        detailsCode: String?,
        userLocation: ApproximateLocation?,
    ): MapUiState {
        if (pack == null) return MapUiState(userLocation = userLocation)
```

and in the populated `MapUiState(...)` constructor call add `userLocation = userLocation,`.

If `MapModule.kt` lists constructor args explicitly (`MapViewModel(get(), get())`), add the third `get()`.

- [ ] **Step 5: Run tests to verify they pass**

Run: `./gradlew :map:testDebugUnitTest --tests "dev.doug.globetraveler.map.MapViewModelTest" --console=plain`
Expected: PASS (all tests, including pre-existing ones)

- [ ] **Step 6: Commit**

```bash
git add map
git commit -m "[map] Fold device location stream into MapUiState"
```

---

### Task 5: Render the dot + docs + full verification

**Files:**
- Modify: `design/src/commonMain/kotlin/dev/doug/globetraveler/design/MapPalette.kt`
- Modify: `map/src/commonMain/kotlin/dev/doug/globetraveler/map/MapScreen.kt`
- Modify: `map/README.md`, `ARCHITECTURE.md`

**Interfaces:**
- Consumes: `MapUiState.userLocation` (Task 4), `MapPalette` Light/Dark instances (existing).
- Produces: user-visible dot; `MapPalette.youAreHere: Color` field.

- [ ] **Step 1: Add the dot color to `MapPalette`**

Add `val youAreHere: Color` to the data class; `Light` gets `youAreHere = Color(0xFF1E88E5)` (location blue 600), `Dark` gets `youAreHere = Color(0xFF64B5F6)` (blue 300, reads on the near-black basemap).

- [ ] **Step 2: Render two circle layers in `LoadableMap`**

`LoadableMap` (and its callers `StatesMap` ← `MapContent`) gains a `userLocation: ApproximateLocation?` parameter, passed down from `state.userLocation`. Inside the `MaplibreMap` content lambda, **after** the existing four layers (later declaration = drawn on top):

```kotlin
        if (userLocation != null) {
            // rememberGeoJsonSource setData()s on recomposition, so the dot moves with new fixes.
            val youAreHere = rememberGeoJsonSource(
                GeoJsonData.JsonString(
                    """{"type":"Feature","properties":{},"geometry":""" +
                        """{"type":"Point","coordinates":[${userLocation.longitude},${userLocation.latitude}]}}""",
                ),
            )
            // No click handlers: taps pass through to the state polygons underneath.
            CircleLayer(
                id = "you-are-here-halo",
                source = youAreHere,
                color = const(palette.youAreHere),
                opacity = const(0.2f),
                radius = const(14.dp),
            )
            CircleLayer(
                id = "you-are-here-dot",
                source = youAreHere,
                color = const(palette.youAreHere),
                radius = const(5.dp),
                strokeColor = const(Color.White),
                strokeWidth = const(2.dp),
            )
        }
```

(new imports: `dev.doug.globetraveler.domain.ApproximateLocation`, `org.maplibre.compose.layers.CircleLayer`)

- [ ] **Step 3: Update docs** — `map/README.md`: add the you-are-here dot to Contains/Rendering; `ARCHITECTURE.md`: note IP-based approximate location (geojs.io via Ktor) as a key decision, one or two lines in existing style.

- [ ] **Step 4: Full verification**

Run: `./gradlew :app-android:assembleDebug :map:testDebugUnitTest :data:jvmTest :app-ios:linkDebugFrameworkIosSimulatorArm64 --console=plain`
Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 5: Commit**

```bash
git add design map ARCHITECTURE.md
git commit -m "[map] Render you-are-here dot from user location"
```

---

### Task 6: Land it

- [ ] Squash-merge `feat/you-are-here` into `main` with a fresh message (per Doug's git workflow; no push):

```bash
git switch main
git merge --squash feat/you-are-here
git commit -m "[map] You-are-here dot from IP geolocation

State-level accuracy only, so location is IP-based (geojs.io, keyless HTTPS) —
no OS location permissions. A cold Flow contract (DeviceLocationRepository)
lets the dot move; the impl polls the bare-IP endpoint each minute and re-runs
the geo lookup only when the IP changes. First Ktor dependency in the project."
git branch -D feat/you-are-here
```

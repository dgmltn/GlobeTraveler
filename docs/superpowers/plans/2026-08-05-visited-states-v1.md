# GlobeTraveler v1 (Visited US States Map) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** KMP app (Android + iOS) whose whole UI is a pannable/zoomable MapLibre map of the US; tapping a state toggles it visited (colored fill), long-press opens a details sheet (date, notes), stored locally in Room.

**Architecture:** Feature-split modules (`:domain`, `:data`, `:design`, `:map`, `:app-android`, `:app-ios`). Map packs are data (descriptor JSON + GeoJSON in `:data` compose resources). The map screen renders two GeoJSON sources (visited/unvisited) derived in the ViewModel from the pack geometry + Room visits; maplibre-compose (`org.maplibre.compose:maplibre-compose`) provides the shared-code map on both platforms.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, maplibre-compose 0.13.x, OpenFreeMap Positron style, Room 3 (+ bundled SQLite), Koin, kotlinx-serialization/datetime, Kermit, XcodeGen + SPM (MapLibre native 6.25.1) for iOS.

## Global Constraints

- Spec: `docs/superpowers/specs/2026-08-05-visited-states-design.md`. v1 scope only; map-switcher UI, stats, sync, visit levels are OUT.
- All work on branch `feat/visited-states-v1` off `main`; WIP commits freely; local `git merge --squash` at the end; **never push**; no AI attribution trailers/footers; commit subjects start with a `[tag]`.
- Module names exactly `:domain`, `:data`, `:design`, `:map`, `:app-android`, `:app-ios`. The word `core` must not appear anywhere.
- Versions: pinned to latest stable from live registry metadata via `scripts/latest-versions.sh` (Task 1). No alpha/beta/rc. AGP stays on latest stable **8.x** (AGP 9 migration out of scope). Dependency versions ONLY in `gradle/libs.versions.toml`; project config (minSdk 26, targetSdk/compileSdk latest stable, JDK toolchain 21) in `gradle.properties` read via `providers.gradleProperty`.
- Time values: `kotlin.time.Instant` / `Duration` / `kotlinx.datetime.LocalDate` in all APIs; primitives (epoch millis, ISO strings) only inside Room entities and mappers.
- Compose files: stateful/stateless split; `@Preview` (FQN `androidx.compose.ui.tooling.preview.Preview` from `org.jetbrains.compose.ui:ui-tooling-preview`) for every public stateless composable, named `Preview_*`, one per meaningful state, wrapped in `GlobeTheme`. Declare compose artifacts as catalog coordinates (`org.jetbrains.compose.*`), not `compose.*` accessors.
- Collection-valued UiState fields use kotlinx-collections-immutable types.
- Kermit for all logging; no `println`.
- Tests: `:domain`/`:data` in `jvmTest` (real collaborators: in-memory Room + bundled driver); `:map` ViewModel tests in `androidUnitTest` with hand-written fakes, asserting by awaiting state conditions (`state.first { … }` under `withTimeout`), never `advanceUntilIdle(); state.value`. TDD for repository + ViewModel logic. Screens compile-verified + previews.
- Every module gets a `README.md`; root gets `ARCHITECTURE.md` with mermaid dependency graph per app target.

---

### Task 1: Toolchain, version pinning, Gradle scaffold

**Files:**
- Create: `scripts/latest-versions.sh`, `.gitignore`, `gradle.properties`, `gradle/libs.versions.toml`, `settings.gradle.kts`, `build.gradle.kts`, gradle wrapper files

**Interfaces:**
- Produces: version catalog aliases used by every later task: `libs.plugins.kotlinMultiplatform`, `kotlinSerialization`, `androidApplication`, `androidLibrary`, `composeMultiplatform`, `composeCompiler`, `ksp`; `libs.room3.runtime`, `libs.room3.compiler`, `libs.sqlite.bundled`, `libs.koin.core`, `libs.koin.compose`, `libs.koin.composeViewmodel`, `libs.koin.android`, `libs.kotlinx.serializationJson`, `libs.kotlinx.datetime`, `libs.kotlinx.collectionsImmutable`, `libs.kotlinx.coroutinesCore`, `libs.kotlinx.coroutinesTest`, `libs.kermit`, `libs.maplibre.compose`, `libs.lifecycle.viewmodelCompose`, `libs.compose.material3`, `libs.compose.foundation`, `libs.compose.ui`, `libs.compose.uiToolingPreview`, `libs.compose.componentsResources`, `libs.androidx.activityCompose`, `libs.kotlin.test`

- [ ] **Step 1: Create branch**

```bash
git switch -c feat/visited-states-v1 main
```

- [ ] **Step 2: Write `scripts/latest-versions.sh`** — queries live registry metadata; prints `name=version` lines. Google Maven via `https://maven.google.com/<group-path>/group-index.xml`; Maven Central via `https://repo1.maven.org/maven2/<group-path>/<artifact>/maven-metadata.xml`; Gradle via `https://services.gradle.org/versions/current`. Filter with `grep -vE '(alpha|beta|rc|dev|snapshot|SNAPSHOT|M[0-9])'`; for AGP additionally filter to `^8\.`. Cover every library in the Interfaces list above. Make executable, run it.

- [ ] **Step 3: Install wrapper** using the version the script reported: `gradle wrapper --gradle-version <X>` (if no system gradle: `brew install gradle` first).

- [ ] **Step 4: Write `.gitignore`** (`.gradle/`, `build/`, `.kotlin/`, `.idea/`, `local.properties`, `*.xcodeproj`, `xcuserdata/`, `.DS_Store`, `Pods/`, `DerivedData/`).

- [ ] **Step 5: Write `gradle.properties`**:

```properties
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g
org.gradle.caching=true
org.gradle.configuration-cache=true
android.useAndroidX=true
globetraveler.jdk=21
globetraveler.minSdk=26
globetraveler.compileSdk=<latest stable>
globetraveler.targetSdk=<latest stable>
```

- [ ] **Step 6: Write `gradle/libs.versions.toml`** with the pinned versions from Step 2. Compose artifact coordinates (`org.jetbrains.compose.material3:material3`, `…foundation:foundation`, `…ui:ui`, `…ui:ui-tooling-preview`, `…components:components-resources`) all share the CMP version. `kotlinSerialization` plugin version = Kotlin version.

- [ ] **Step 7: Write `settings.gradle.kts`** (pluginManagement + dependencyResolutionManagement repos: google, mavenCentral, gradlePluginPortal; `rootProject.name = "GlobeTraveler"`; include `:domain`, `:data`, `:design`, `:map`, `:app-android`, `:app-ios`) and root `build.gradle.kts` (all plugin aliases with `apply false`).

- [ ] **Step 8: Verify**: `./gradlew help` succeeds with configuration cache.

- [ ] **Step 9: Commit** `[deps] Scaffold Gradle project with pinned version catalog`

---

### Task 2: `:domain` module

**Files:**
- Create: `domain/build.gradle.kts`, `domain/README.md`, `domain/src/commonMain/kotlin/dev/doug/globetraveler/domain/{Ids.kt,Region.kt,MapDescriptor.kt,MapPack.kt,Visit.kt,VisitRepository.kt,MapPackRepository.kt}`
- Test: `domain/src/jvmTest/kotlin/dev/doug/globetraveler/domain/RegionIdTest.kt`

**Interfaces:**
- Produces (exact):

```kotlin
@JvmInline value class MapId(val value: String)
@JvmInline value class RegionCode(val value: String)
data class RegionId(val mapId: MapId, val code: RegionCode)
data class Region(val code: RegionCode, val name: String)
data class CameraDefaults(val latitude: Double, val longitude: Double, val zoom: Double)
data class MapDescriptor(val mapId: MapId, val name: String, val geometryAsset: String, val camera: CameraDefaults, val regions: List<Region>)
data class MapPack(val descriptor: MapDescriptor, val geometryGeoJson: String)
data class Visit(val regionId: RegionId, val visitedAt: LocalDate?, val notes: String?, val markedAt: Instant)  // kotlin.time.Instant

interface VisitRepository {
    fun observeVisits(mapId: MapId): Flow<List<Visit>>
    suspend fun toggle(regionId: RegionId)
    suspend fun updateDetails(visit: Visit)
}
interface MapPackRepository { suspend fun load(mapId: MapId): MapPack }
```

- [ ] Module `build.gradle.kts`: plugins kmp + `androidLibrary` (Android consumers `:data`/`:map` need an android target; the module stays pure Kotlin — no Android APIs). Targets: `jvm()`, `androidTarget()`, `iosArm64()`, `iosSimulatorArm64()`. `namespace = "dev.doug.globetraveler.domain"`, compile/min SDK from `providers.gradleProperty`. commonMain deps: `kotlinx.coroutinesCore`, `kotlinx.datetime`. `jvmTest`: `kotlin.test`.
- [ ] Write failing test `RegionIdTest` (e.g. two `RegionId`s with same mapId+code are equal; `MapId`/`RegionCode` are value classes wrapping raw strings).
- [ ] Run `./gradlew :domain:jvmTest` → FAIL (types missing).
- [ ] Add the types above; run `./gradlew :domain:jvmTest` → PASS.
- [ ] Write `domain/README.md`. Commit `[domain] Add domain models and repository contracts`

---

### Task 3: Geodata pipeline (map pack for us-states)

**Files:**
- Create: `scripts/build-mappack.py`, `resources/geodata/README.md`
- Generated (committed): `resources/geodata/source/gz_2010_us_040_00_20m.json`, `data/src/commonMain/composeResources/files/us-states.geojson`, `data/src/commonMain/composeResources/files/us-states.descriptor.json`

**Interfaces:**
- Produces: `us-states.geojson` — FeatureCollection, 50 features, each `properties = {"code": "<USPS>", "name": "<state name>"}`; `us-states.descriptor.json`:

```json
{"mapId": "us-states", "name": "United States", "geometryAsset": "us-states.geojson",
 "camera": {"latitude": 39.5, "longitude": -98.35, "zoom": 3.0},
 "regions": [{"code": "AK", "name": "Alaska"}, …50 alphabetical by code…]}
```

- [ ] Write `scripts/build-mappack.py`: download US Census 2010 cartographic boundaries (20m) GeoJSON from `https://eric.clst.org/assets/wiki/uploads/Stuff/gz_2010_us_040_00_20m.json` into `resources/geodata/source/` (skip if present); embed full FIPS→USPS dict; filter out DC (FIPS 11) and PR (72) leaving exactly 50; rewrite each feature's properties to `{code, name}`; round coordinates to 4 decimals; write minified GeoJSON + descriptor (regions sorted by code) to `data/src/commonMain/composeResources/files/`. Script asserts: 50 features, all codes unique, every feature Polygon/MultiPolygon.
- [ ] Run it; verify assertions pass and output GeoJSON is < 1 MB.
- [ ] Write `resources/geodata/README.md` (source, license = public domain US Census, how to regenerate).
- [ ] Commit `[geodata] Add us-states map pack pipeline and generated pack`

---

### Task 4: `:data` module (Room + repositories + Koin)

**Files:**
- Create: `data/build.gradle.kts`, `data/README.md`, `data/src/commonMain/kotlin/dev/doug/globetraveler/data/{VisitEntity.kt,VisitDao.kt,GlobeDatabase.kt,VisitRepositoryImpl.kt,MapPackRepositoryImpl.kt,DataModule.kt}`, `data/src/{androidMain,iosMain,jvmMain}/kotlin/dev/doug/globetraveler/data/DatabaseBuilder.<platform>.kt`
- Test: `data/src/jvmTest/kotlin/dev/doug/globetraveler/data/{VisitRepositoryImplTest.kt,MapPackRepositoryImplTest.kt}`

**Interfaces:**
- Consumes: `:domain` contracts (Task 2), pack files (Task 3).
- Produces: `fun dataModule(): Module` (Koin) binding `VisitRepository` + `MapPackRepository`; `expect fun databaseBuilder(): RoomDatabase.Builder<GlobeDatabase>` with android/ios/jvm actuals (android actual takes context from Koin).

Key code:

```kotlin
@Entity(tableName = "visits", primaryKeys = ["mapId", "regionCode"])
data class VisitEntity(val mapId: String, val regionCode: String, val visitedAt: String?, val notes: String?, val markedAtEpochMillis: Long)

@Dao interface VisitDao {
    @Query("SELECT * FROM visits WHERE mapId = :mapId") fun observe(mapId: String): Flow<List<VisitEntity>>
    @Query("SELECT * FROM visits WHERE mapId = :mapId AND regionCode = :code") suspend fun get(mapId: String, code: String): VisitEntity?
    @Upsert suspend fun upsert(entity: VisitEntity)
    @Query("DELETE FROM visits WHERE mapId = :mapId AND regionCode = :code") suspend fun delete(mapId: String, code: String)
}

@Database(entities = [VisitEntity::class], version = 1)
@ConstructedBy(GlobeDatabaseConstructor::class)
abstract class GlobeDatabase : RoomDatabase() { abstract fun visitDao(): VisitDao }
@Suppress("KotlinNoActualForExpect") expect object GlobeDatabaseConstructor : RoomDatabaseConstructor<GlobeDatabase>

class VisitRepositoryImpl(private val dao: VisitDao, private val clock: Clock) : VisitRepository { /* mapper converts ISO/epoch ↔ LocalDate/Instant at this boundary */ }

class MapPackRepositoryImpl(private val json: Json) : MapPackRepository {
    override suspend fun load(mapId: MapId): MapPack  // Res.readBytes("files/${mapId.value}.descriptor.json") + geometry asset
}
```

- [ ] `build.gradle.kts`: plugins kmp, androidLibrary, ksp, composeMultiplatform + composeCompiler (for `components-resources` `Res` only), kotlinSerialization. Targets: androidTarget, iosArm64, iosSimulatorArm64, jvm. Deps: `:domain`, room3-runtime, sqlite-bundled, koin-core, serializationJson, datetime, kermit, componentsResources. KSP: `room3-compiler` for `kspAndroid`, `kspJvm`, `kspIosArm64`, `kspIosSimulatorArm64`; `ksp { arg("room.schemaLocation", "$projectDir/schemas") }`. Compose resources: `publicResClass = false`, package `dev.doug.globetraveler.data`.
- [ ] TDD `VisitRepositoryImpl` in `jvmTest` with real in-memory Room (`Room.inMemoryDatabaseBuilder<GlobeDatabase>().setDriver(BundledSQLiteDriver())`): toggle-inserts (observe emits 1 visit, `markedAt` = clock now), toggle-again-deletes, updateDetails upserts date+notes preserving visited row, observe filters by mapId. Fail → implement → pass.
- [ ] TDD `MapPackRepositoryImpl` in `jvmTest`: loads `us-states`, descriptor has 50 regions, geometry parses as FeatureCollection with 50 features whose `code`s match descriptor. Fail → implement → pass.
- [ ] Platform `databaseBuilder` actuals (android: `context.getDatabasePath("globe.db")`; ios: `NSHomeDirectory() + "/Documents/globe.db"`; jvm: file under `System.getProperty("java.io.tmpdir")` — tests use in-memory directly). All builders `.setDriver(BundledSQLiteDriver())`.
- [ ] `dataModule()`: singles for `Json { ignoreUnknownKeys = true }`, `GlobeDatabase` (from `databaseBuilder()`), `VisitDao`, `Clock.System`, repo impls bound to interfaces.
- [ ] `./gradlew :data:jvmTest` → PASS. README. Commit `[data] Room visits store, map pack loading, Koin module`

---

### Task 5: `:design` module

**Files:**
- Create: `design/build.gradle.kts`, `design/README.md`, `design/src/commonMain/kotlin/dev/doug/globetraveler/design/{GlobeTheme.kt,MapPalette.kt}`

**Interfaces:**
- Produces: `@Composable fun GlobeTheme(content: @Composable () -> Unit)` (Material3 light/dark color schemes); `object MapPalette { val visitedFill = Color(0xFF2E7D32); val visitedOutline = Color(0xFF1B5E20); val unvisitedFill = Color(0x14000000); val border = Color(0xFF757575) }` — fill layers apply their own alpha (~0.6 visited).

- [ ] Module with kmp + androidLibrary + compose plugins; targets android/ios; deps: material3, ui, foundation. Write theme + palette. Compile: `./gradlew :design:compileDebugKotlinAndroid` (or `assemble`). README. Commit `[design] Theme and map palette`

---

### Task 6: `:map` feature module

**Files:**
- Create: `map/build.gradle.kts`, `map/README.md`, `map/src/commonMain/kotlin/dev/doug/globetraveler/map/{GeoJsonSplitter.kt,MapUiState.kt,MapViewModel.kt,MapScreen.kt,VisitDetailsSheet.kt,MapModule.kt}`
- Test: `map/src/androidUnitTest/kotlin/dev/doug/globetraveler/map/{GeoJsonSplitterTest.kt,MapViewModelTest.kt,fakes/Fakes.kt}`

**Interfaces:**
- Consumes: `:domain` contracts, `:design` theme, Koin `dataModule()`.
- Produces: `@Composable fun MapScreen()` (stateful root, `koinViewModel()`), `fun mapModule(): Module`.

```kotlin
object GeoJsonSplitter {
    data class Split(val visitedGeoJson: String, val unvisitedGeoJson: String)
    fun split(featureCollectionJson: String, visitedCodes: Set<String>): Split  // kotlinx-serialization JsonObject filtering on properties.code
}

data class RegionDetails(val region: Region, val visit: Visit?)  // sheet target
data class MapUiState(
    val loading: Boolean = true,
    val mapName: String = "",
    val cameraDefaults: CameraDefaults? = null,
    val totalCount: Int = 0,
    val visitedCount: Int = 0,
    val visitedGeoJson: String = EMPTY_FEATURE_COLLECTION,
    val unvisitedGeoJson: String = EMPTY_FEATURE_COLLECTION,
    val details: RegionDetails? = null,
)
class MapViewModel(packRepo: MapPackRepository, visitRepo: VisitRepository) : ViewModel() {
    val state: StateFlow<MapUiState>              // combine(pack, observeVisits) → stateIn(WhileSubscribed(5.seconds))
    fun onRegionTapped(code: String)              // toggle
    fun onRegionLongPressed(code: String)         // sets details
    fun onDetailsSave(visitedAt: LocalDate?, notes: String?)  // updateDetails (insert if absent)
    fun onDetailsRemoveVisit()                    // toggle off from sheet
    fun onDetailsDismissed()
}
```

- [ ] `build.gradle.kts`: kmp, androidLibrary, compose plugins, serialization. Targets android + ios. Deps: `:domain`, `:design`, maplibre-compose, koin-compose, koin-composeViewmodel, lifecycle-viewmodelCompose, material3/ui/foundation/uiToolingPreview, serializationJson, collectionsImmutable, kermit. `androidUnitTest`: kotlin-test, coroutinesTest, koin? (not needed — construct VM directly).
- [ ] TDD `GeoJsonSplitter` (androidUnitTest, tiny 3-feature FeatureCollection literal): split by codes; features preserved verbatim; unknown codes ignored. Fail → implement → pass (`./gradlew :map:testDebugUnitTest`).
- [ ] TDD `MapViewModel` with hand-written fakes (`FakeVisitRepository` over `MutableStateFlow<Map<RegionId, Visit>>`, `FakeMapPackRepository` returning a 3-region pack). `Dispatchers.setMain(UnconfinedTestDispatcher())`; assert via `withTimeout(5.seconds) { vm.state.first { predicate } }`. Cases: loads pack (counts, camera); tap toggles visited (visitedGeoJson gains feature, count increments); tap again reverts; long-press sets `details` with existing visit; save persists date+notes; dismiss clears. Fail → implement → pass.
- [ ] `MapScreen.kt`: stateful `MapScreen()` collects state, delegates to stateless `MapContent(state, onRegionTapped, onRegionLongPressed, …)`:
  - `rememberCameraState(firstPosition = CameraPosition(target = Position(camera.latitude, camera.longitude), zoom = camera.zoom))`
  - `MaplibreMap(baseStyle = BaseStyle.Uri("https://tiles.openfreemap.org/styles/positron"), cameraState = camera, onMapLongClick = { _, offset -> resolve feature via camera.projection?.queryRenderedFeatures(offset), read `code` property (parse `feature.toJson()` with kotlinx-serialization helper `Feature.regionCode()`), call onRegionLongPressed; ClickResult.Consume if hit })`
  - Two `rememberGeoJsonSource(GeoJsonData.JsonString(state.visitedGeoJson))` / unvisited; layers bottom→top: `FillLayer("unvisited-fill", unvisitedSrc, color = const(MapPalette.unvisitedFill), onClick = { fs -> code(fs) → onRegionTapped; ClickResult.Consume })`, `FillLayer("visited-fill", visitedSrc, color = const(MapPalette.visitedFill), opacity = const(0.6f), onClick = toggle likewise)`, `LineLayer("borders", allStatesSrc?, …)` — simplest: a `LineLayer` per source (border color `MapPalette.border`, visited outline `MapPalette.visitedOutline`, width `const(1.dp)`).
  - Overlay chrome: `CounterChip(visitedCount, totalCount)` top-center; loading = centered `CircularProgressIndicator`. **Style-failure investigation step:** check maplibre-compose 0.13 API docs for a style/load-failure callback; if present wire error state + retry (re-set `baseStyle`), if absent log via Kermit and note the deviation in `map/README.md`.
  - Previews: `Preview_CounterChip`, `Preview_MapContent_Loading` (map itself isn't previewable — preview the chrome states).
- [ ] `VisitDetailsSheet.kt`: stateless `VisitDetailsSheetContent(regionName, visitedAt, notes, onSave, onRemoveVisit, onDismiss)` inside `ModalBottomSheet` wrapper driven by `state.details`; Material3 `DatePicker` in a dialog for date; `OutlinedTextField` for notes. Previews: `Preview_VisitDetailsSheetContent` (visited with date+notes), `Preview_VisitDetailsSheetContent_NewVisit`.
- [ ] `mapModule()`: `viewModel { MapViewModel(get(), get()) }` + single `MapId("us-states")`… (VM takes mapId const internally in v1; keep `US_STATES_MAP_ID` in `:map`).
- [ ] `./gradlew :map:testDebugUnitTest` PASS; `./gradlew :map:assemble` compiles all targets. README. Commit `[map] Map screen, ViewModel, details sheet`

---

### Task 7: `:app-android`

**Files:**
- Create: `app-android/build.gradle.kts`, `app-android/README.md`, `app-android/src/main/{AndroidManifest.xml,kotlin/dev/doug/globetraveler/app/{GlobeApp.kt,MainActivity.kt}}`

**Interfaces:**
- Consumes: `dataModule()`, `mapModule()`, `GlobeTheme`, `MapScreen`.

- [ ] Android application module (`androidApplication` plugin + kotlin-android? use kmp with androidTarget? — plain `com.android.application` + `org.jetbrains.kotlin.android` + compose plugins is the thin-entry norm). `applicationId "dev.doug.globetraveler"`, SDKs from `gradle.properties`. Manifest: `<uses-permission android:name="android.permission.INTERNET"/>`, app label "GlobeTraveler", `MainActivity` exported with launcher intent-filter.
- [ ] `GlobeApp : Application` → `startKoin { androidContext(this@GlobeApp); modules(dataModule(), mapModule()) }` (android `databaseBuilder` actual pulls context from Koin). `MainActivity : ComponentActivity` → `setContent { GlobeTheme { MapScreen() } }`.
- [ ] `./gradlew :app-android:assembleDebug` → BUILD SUCCESSFUL. If an emulator/device is available: install + launch + `adb` screenshot; otherwise note it.
- [ ] README. Commit `[Android] App entry point with Koin composition root`

---

### Task 8: `:app-ios`

**Files:**
- Create: `app-ios/build.gradle.kts` (KMP framework module), `app-ios/src/iosMain/kotlin/dev/doug/globetraveler/app/{MainViewController.kt,KoinIos.kt}`, `app-ios/project.yml`, `app-ios/Sources/GlobeTravelerApp.swift`, `app-ios/Resources/Info.plist`, `app-ios/README.md`

**Interfaces:**
- Consumes: `MapScreen`, `GlobeTheme`, Koin modules.
- Produces: framework `GlobeTravelerKit` exposing `MainViewControllerKt.MainViewController()` and `KoinIosKt.doInitKoin()`.

- [ ] `app-ios/build.gradle.kts`: kmp + compose; iosArm64/iosSimulatorArm64 each `binaries.framework { baseName = "GlobeTravelerKit"; isStatic = true }`; deps `:map`, `:design`, `:data`, `:domain`, koin-core.
- [ ] `MainViewController.kt`: `fun MainViewController(): UIViewController = ComposeUIViewController { GlobeTheme { MapScreen() } }`; `KoinIos.kt`: `fun initKoin() { startKoin { modules(dataModule(), mapModule()) } }`.
- [ ] `project.yml` (XcodeGen; `brew install xcodegen` if missing): app target GlobeTraveler; SPM package `https://github.com/maplibre/maplibre-gl-native-distribution.git` exact 6.25.1 (product MapLibre); pre-build script phase running `./gradlew :app-ios:embedAndSignAppleFrameworkForXcode`; `FRAMEWORK_SEARCH_PATHS` to the gradle build dir; **linker order: Kotlin framework before MapLibre.framework** (maplibre-compose text-rendering requirement — set via `OTHER_LDFLAGS` ordering); `GlobeTravelerApp.swift`: SwiftUI `@main` App with `UIViewControllerRepresentable` wrapping `MainViewControllerKt.MainViewController()`, `init { KoinIosKt.doInitKoin() }`.
- [ ] Generate + build: `cd app-ios && xcodegen && xcodebuild -project GlobeTraveler.xcodeproj -scheme GlobeTraveler -destination 'generic/platform=iOS Simulator' build`. Fix until BUILD SUCCEEDED. Boot a simulator, install, launch, screenshot if practical.
- [ ] README. Commit `[iOS] Xcode app via XcodeGen with MapLibre SPM`

---

### Task 9: Docs, verification, squash merge

- [ ] Write root `README.md` (what the app is, how to build both platforms, how to regenerate the map pack) and `ARCHITECTURE.md` (module table + mermaid dependency graph per app target).
- [ ] Full verification: `./gradlew :domain:jvmTest :data:jvmTest :map:testDebugUnitTest :app-android:assembleDebug` all green; iOS build from Task 8 confirmed.
- [ ] Commit `[docs] Architecture and module documentation`
- [ ] Squash merge per git prefs: `git switch main && git merge --squash feat/visited-states-v1 && git commit` (fresh message, e.g. `[app] GlobeTraveler v1: visited US states map (Android + iOS)`), `git branch -D feat/visited-states-v1`. **Stop — do not push.**

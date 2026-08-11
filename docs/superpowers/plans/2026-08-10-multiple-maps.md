# Multiple Tracked Maps Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Multiple named tracking collections ("Visited", "License plates") over the same
us-states geography, switched from a dropdown on the top-center counter, with per-collection
visits, notes, and accent colors.

**Architecture:** New `TrackedMap` domain concept keyed by `TrackedMapId`; `RegionId`/`Visit`
rekey from `MapId` to `TrackedMapId`. Room gains a `tracked_maps` table (db v2, manual
migration preserving existing visits under a seeded "Visited" map). Active-map selection in
DataStore Preferences. The `:map` ViewModel resolves the active map, `flatMapLatest`s its
visits, and the title composable renders name + count + dropdown switcher; fill colors come
from the map's accent.

**Tech Stack:** Room 3.0.1 (existing), androidx DataStore Preferences **1.2.1** (new, KMP,
`createWithPath`), kotlin.uuid for ids, Material3 DropdownMenu/AlertDialog.

## Global Constraints

- Version catalog only for dependency versions; DataStore pinned 1.2.1 (live-metadata check done).
- kotlin.time types (`Instant`, `Duration`) — no epoch primitives outside boundaries.
- Compose files: stateful/stateless split, `Preview_` previews for public stateless composables.
- Tests: `:data` in jvmTest with real Room (bundled driver) + temp-file DataStore;
  `:map` in androidUnitTest with fakes, awaiting state conditions (no `advanceUntilIdle`).
- Commit after each task (WIP commits; squash at the end).

---

### Task 1: Domain model + contracts

**Files:**
- Create: `domain/src/commonMain/kotlin/dev/doug/globetraveler/domain/TrackedMap.kt`
- Modify: `domain/.../Ids.kt` (RegionId), `domain/.../VisitRepository.kt`
- Modify: `domain/README.md`

**Produces (later tasks rely on):**
```kotlin
@JvmInline value class TrackedMapId(val value: String)
enum class MapAccent { Green, Blue, Orange, Purple, Red, Teal }
data class TrackedMap(
    val id: TrackedMapId,
    val packId: MapId,
    val name: String,
    val accent: MapAccent,
    val createdAt: Instant,
)
interface TrackedMapRepository {
    fun observeMaps(): Flow<List<TrackedMap>>          // createdAt order, never empty after seed
    fun observeActiveMap(): Flow<TrackedMap>           // pref or oldest fallback
    suspend fun create(name: String): TrackedMap       // next unused accent, becomes active
    suspend fun setActive(id: TrackedMapId)
}
// RegionId: data class RegionId(val trackedMapId: TrackedMapId, val code: RegionCode)
// VisitRepository: observeVisits(trackedMapId: TrackedMapId): Flow<List<Visit>>;
//                  observeVisitCounts(): Flow<Map<TrackedMapId, Int>>; toggle/updateDetails unchanged shapes.
```

- [ ] Write the types + contracts (no tests — pure declarations), update README, commit.

### Task 2: Accent colors in :design

**Files:**
- Modify: `design/src/commonMain/kotlin/dev/doug/globetraveler/design/MapPalette.kt`

**Produces:**
```kotlin
data class AccentColors(val fill: Color, val outline: Color)
fun MapAccent.accentColors(dark: Boolean): AccentColors
// Green must equal today's visited colors: light fill 0xFF2E7D32 / outline 0xFF1B5E20,
// dark fill 0xFF81C784 / outline 0xFFA5D6A7. Others: Material-ish 700-level (light) /
// 300-level (dark) pairs for Blue 0xFF1565C0/0xFF0D47A1 + 0xFF64B5F6/0xFF90CAF9,
// Orange 0xFFEF6C00/0xFFE65100 + 0xFFFFB74D/0xFFFFCC80,
// Purple 0xFF6A1B9A/0xFF4A148C + 0xFFBA68C8/0xFFCE93D8,
// Red 0xFFC62828/0xFFB71C1C + 0xFFE57373/0xFFEF9A9A,
// Teal 0xFF00695C/0xFF004D40 + 0xFF4DB6AC/0xFF80CBC4.
// MapPalette drops visitedFill/visitedOutline (they move to accents); keeps
// unvisitedFill/border/youAreHere.
```

- [ ] Implement, `./gradlew :design:compileDebugKotlinAndroid`, commit.

### Task 3: Data layer — Room v2 + tracked-map repo + DataStore

**Files:**
- Create: `data/.../TrackedMapEntity.kt`, `data/.../TrackedMapDao.kt`,
  `data/.../TrackedMapRepositoryImpl.kt`, `data/.../ActiveMapStore.kt` (DataStore wrapper),
  `data/src/{androidMain,iosMain,jvmMain}/.../DataStorePath.*.kt` (expect/actual path)
- Modify: `VisitEntity.kt` (column `trackedMapId`), `VisitDao.kt` (+`observeCounts`),
  `VisitRepositoryImpl.kt`, `GlobeDatabase.kt` (v2 + entity + dao + migration),
  `DataModule.kt`, `data/build.gradle.kts` + `gradle/libs.versions.toml` (datastore 1.2.1)
- Test: `data/src/jvmTest/.../TrackedMapRepositoryImplTest.kt`, update `VisitRepositoryImplTest.kt`

**Migration (v1→v2), registered via `.addMigrations(MIGRATION_1_2)` in DataModule:**
```sql
CREATE TABLE tracked_maps (
  id TEXT NOT NULL PRIMARY KEY, packId TEXT NOT NULL, name TEXT NOT NULL,
  accent TEXT NOT NULL, createdAtEpochMillis INTEGER NOT NULL);
INSERT INTO tracked_maps VALUES ('visited','us-states','Visited','Green',0);
CREATE TABLE visits_new (
  trackedMapId TEXT NOT NULL, regionCode TEXT NOT NULL, visitedAt TEXT, notes TEXT,
  markedAtEpochMillis INTEGER NOT NULL, PRIMARY KEY(trackedMapId, regionCode));
INSERT INTO visits_new SELECT 'visited', regionCode, visitedAt, notes, markedAtEpochMillis FROM visits;
DROP TABLE visits;
ALTER TABLE visits_new RENAME TO visits;
```

**Repo behavior:** `observeMaps()` seeds `('visited', packId us-states, "Visited", Green, clock.now())`
when empty (fresh installs) before emitting; `create` picks `MapAccent.entries[count % entries.size]`
skipping used ones first, id = `Uuid.random().toString()`, writes active pref; active pref key
`stringPreferencesKey("activeMapId")`; `observeActiveMap()` = combine(maps, pref) falling back
to oldest when unset/dangling.

**Tests (jvmTest, temp-file DataStore via `PreferenceDataStoreFactory.createWithPath`):**
- [ ] seeds Visited on first observe / create assigns unused accent and becomes active /
      setActive persists / dangling pref falls back to oldest / visit counts group by map /
      rekeyed visit toggle+details round-trip. Run `./gradlew :data:jvmTest`, commit.

### Task 4: Map UI — state, ViewModel, switcher

**Files:**
- Modify: `map/.../MapUiState.kt` (+`activeMap: TrackedMap?`, `maps: ImmutableList<MapRow>`,
  `MapRow(map: TrackedMap, visitedCount: Int)`), `map/.../MapViewModel.kt`,
  `map/.../MapScreen.kt`, `map/.../fakes/Fakes.kt`, `map/README.md`, `ARCHITECTURE.md`
- Create: `map/.../MapSwitcher.kt` (title + dropdown + new-map dialog composables)
- Test: `map/src/androidUnitTest/.../MapViewModelTest.kt` (+switch test),

**ViewModel wiring:**
```kotlin
private val activeMap = trackedMapRepository.observeActiveMap()
private val visits = activeMap.flatMapLatest { visitRepository.observeVisits(it.id) }
state = combine(pack, activeMap, trackedMapRepository.observeMaps(),
                visitRepository.observeVisitCounts(), visits, detailsCode, userLocation, ...)
fun onMapSelected(id: TrackedMapId) / fun onMapCreated(name: String)  // launch repo calls
// onRegionTapped/onRegionLongPressed build RegionId(activeMap.id, code) from state.
```

**UI:** `MapSwitcher(activeName, activeAccentColor, visitedCount, totalCount, rows, onSelect,
onCreate, modifier)` — stateless; `Text("$name · $count/$total ▾"-ish)` in accent color,
`DropdownMenu` rows "name · n/total" (active bolded), divider, "New map…" → `AlertDialog`
with `OutlinedTextField`, Create disabled when blank. Previews: populated, single-map, dialog.
Fill layers: `accentColors(darkTheme)` from `state.activeMap.accent`.

- [ ] Implement, update fakes/tests, `./gradlew :map:testDebugUnitTest`, commit.

### Task 5: Verify + docs + merge

- [ ] `./gradlew :app-android:assembleDebug :data:jvmTest :map:testDebugUnitTest
      :data:compileKotlinIosSimulatorArm64 :map:compileKotlinIosSimulatorArm64`
- [ ] Emulator: fresh install → "Visited · 0/50" shows; create "License plates" → switches,
      accent changes; mark states in each; switch back — counts independent; notes dialog per map.
- [ ] **Upgrade check:** install previous main APK, mark 2 states, install this build over it,
      confirm marks survive under "Visited".
- [ ] Update data/README, map/README, ARCHITECTURE (tracked maps + DataStore), commit,
      squash-merge to main.

# GlobeTraveler — Visited States Map: Design

**Date:** 2026-08-05
**Status:** Approved design, pre-implementation

## Purpose

A Kotlin Multiplatform app (Android + iOS) that tracks which US states the user has
visited. The entire main interface is a pannable, zoomable map; tapping a state toggles
it visited and fills it with color. The map content is data-driven ("map packs") so new
maps — counties in a state, countries of the world — can be added later without
rendering or schema changes.

## Decisions made

| Decision | Choice |
|---|---|
| Platforms | Android + iOS |
| Visit model | Visited flag + optional details (date, notes) |
| Storage | Local only (Room); no accounts, no sync |
| Tap behavior | Tap toggles visited instantly; long-press opens a details sheet |
| Rendering | MapLibre (via **maplibre-compose**, Compose Multiplatform) + GeoJSON overlays |
| Basemap | OpenFreeMap **Positron** style (free, no API key, muted so fills pop) |
| Navigation | None in v1 (one screen + modal sheet); add Navigation 3 when a second screen appears |
| Map switcher UI | Out of scope for v1 — `us-states` pack is hardcoded |

## Architecture & modules

Feature-split module shape:

| Module | Purpose |
|---|---|
| `:domain` | Pure Kotlin. `MapId`, `RegionId`, `MapDescriptor`, `Region`, `Visit`; `VisitRepository` and `MapPackRepository` contracts. Has a `jvm()` target for fast tests. |
| `:data` | Room 3 database + DAOs, repository implementations, map-pack loading from bundled resources (descriptor JSON + GeoJSON). |
| `:design` | Theme, color palette (visited-fill colors, accents complementing the Positron basemap), typography. |
| `:map` | Map feature: `MapScreen` (maplibre-compose), visited-fill overlay, tap/long-press handling, visit-details bottom sheet, `MapViewModel` (single UiState StateFlow). |
| `:app-android` | Thin Android entry point + Koin composition root. |
| `:app-ios` | Thin iOS entry point + Koin composition root. XcodeGen: `project.yml` + Swift entry committed, `.xcodeproj` gitignored. |

### Libraries

| Concern | Library |
|---|---|
| Map | maplibre-compose (Compose Multiplatform wrapper over MapLibre Native, Android + iOS) |
| DI | Koin (koin-core, koin-compose, koin-compose-viewmodel; koin-android in `:app-android`) |
| Database | Room 3 (`androidx.room3:room3-runtime`, KSP compiler, `androidx.sqlite:sqlite-bundled`) |
| Serialization | kotlinx-serialization-json (map-pack descriptors) |
| Time | kotlinx-datetime + `kotlin.time.Instant` |
| Logging | Kermit, tagged `Logger` instances |
| UI | Compose Multiplatform + Material 3 (modal bottom sheet, date picker) |
| Collections | kotlinx-collections-immutable for collection-valued UiState fields |

Not used in v1 (nothing needs them): Ktor, DataStore, Coil, Navigation 3.

Versions pinned to latest stable from live registry metadata via `scripts/latest-versions.sh`
into `gradle/libs.versions.toml`; project config (JDK, minSdk, targetSdk, compileSdk) in
`gradle.properties`. maplibre-compose's current version and exact API surface (layer/source
composables, click callbacks, expression builders) are verified live at implementation time.

## Data model

### Visits

One Room table; a row existing means "visited":

```
visits(
  mapId: String,        // e.g. "us-states"
  regionCode: String,   // e.g. "CA"
  visitedAt: LocalDate?,// optional user-entered visit date
  notes: String?,       // optional user notes
  markedAt: Instant,    // when the row was created
  primary key (mapId, regionCode)
)
```

`VisitRepository`:
- `observeVisits(mapId): Flow<List<Visit>>`
- `toggle(regionId)` — insert (visited) or delete (unvisited)
- `updateDetails(visit)` — upsert date/notes

### Map packs

A map pack is **data, not code** — the extensibility seam:

- **Descriptor** (bundled JSON): `mapId`, display name, region list (`code`, `name`),
  initial camera position + bounds, name of the GeoJSON asset.
- **Geometry**: a GeoJSON FeatureCollection; each feature carries the region `code`
  property matching the descriptor.

`MapPackRepository.load(mapId): MapPack` reads both from bundled compose resources in
`:data`. Adding counties or world countries later = a new descriptor + GeoJSON file.

Geometry source: US Census cartographic boundary files (public domain), simplified so the
bundled GeoJSON stays small (target: low hundreds of KB). Raw source data + conversion
script live in `/resources/geodata/`; the processed output is bundled in `:data` resources.

## Map screen

`MapScreen` in `:map` commonMain:

- maplibre-compose map, base style = OpenFreeMap Positron.
- `GeoJsonSource` (states FeatureCollection) + two layers:
  - `FillLayer` — data-driven fill: visited → theme fill color at ~60% opacity;
    unvisited → transparent.
  - `LineLayer` — state borders, so unvisited states read as tappable.
- Visited-ness flows from `MapViewModel` UiState (set of visited region codes) into the
  layer expression / feature state.
- Pan/zoom/rotate from MapLibre; camera constrained to the pack's bounds.
- Floating counter chip: "12 / 50".

### Interaction

- **Tap** on a state → toggle visited immediately.
- **Long-press** → Material 3 modal bottom sheet: state name, visited toggle, date
  picker, notes field. Save persists via `VisitRepository`.
- Tap on empty basemap → no-op.

### Error handling

- Basemap style/tiles need network on first load. If style load fails: error state over
  the map area with retry. MapLibre caches tiles thereafter.
- Visits are local-only Room — no other failure modes surfaced in v1.

## Testing

- `:domain`, `:data`: `jvmTest`, real collaborators — in-memory Room with bundled driver.
- `MapViewModel`: Android host-test source set; assert by awaiting state conditions
  (never `advanceUntilIdle(); state.value`).
- `MapScreen`: compile-verified + smoke-tested on device.
- TDD for repository and ViewModel logic.

## Docs

- Root `ARCHITECTURE.md`: module list with one-line purposes + mermaid dependency graph
  per app target.
- Per-module `README.md` (what it is, what it depends on).

## Out of scope (v1)

- Map switcher UI / multiple packs at runtime
- Stats/list screens, sharing, sync, accounts
- Visit levels (lived/stayed/passed-through)
- Offline basemap bundling

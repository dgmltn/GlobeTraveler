# :map

The map feature: the app's single screen.

**Contains:** `MapScreen` (stateful root) → `MapContent` (stateless) → `StatesMap`
(maplibre-compose map with OpenFreeMap Positron base style, visited/unvisited GeoJSON
sources, fill + border layers, layer-level tap/long-press handlers), `CounterChip`,
`MapLoadErrorBanner` (style-load failure + retry, which recreates the map), the
`VisitDetailsSheet` (date + notes), `MapViewModel` (single UiState StateFlow combining
map pack + visits), and `GeoJsonSplitter` (partitions the pack's FeatureCollection by
visited codes).

**Depends on:** `:domain` (contracts), `:design` (theme/palette), maplibre-compose,
Koin (compose + viewmodel), lifecycle, kotlinx-serialization.

**Interaction:** tap a state → toggle visited; long-press → details sheet.

**Tests:** `./gradlew :map:testDebugUnitTest` — ViewModel behavior against hand-written
fakes (await state conditions), GeoJsonSplitter partitioning. The map composable itself
is compile-verified + smoke-tested on device.

# :map

The map feature: the app's single screen.

**Contains:** `MapScreen` (stateful root) → `MapContent` (stateless) → `StatesMap`
(maplibre-compose map with OpenFreeMap Positron base style), `CounterChip`,
`MapLoadErrorBanner` (style-load failure + retry, which recreates the map), the
`VisitDetailsSheet` (date + notes), and `MapViewModel` (single UiState StateFlow
combining map pack + visits).

**Rendering:** the pack's FeatureCollection is loaded into a single static GeoJSON
source exactly once. Visited-ness is expressed as a layer `filter`
(`code ∈ visitedCodes`) on the fill/outline layers — toggling a state swaps a tiny
filter expression instead of re-parsing/re-tessellating geometry, so fills update
instantly.

**Depends on:** `:domain` (contracts), `:design` (theme/palette), maplibre-compose,
Koin (compose + viewmodel), lifecycle, kotlinx-serialization,
kotlinx-collections-immutable.

**Interaction:** tap a state → toggle visited; long-press → details sheet.

**Tests:** `./gradlew :map:testDebugUnitTest` — ViewModel behavior against hand-written
fakes (await state conditions), including a regression test that geometry is never
rebuilt when visits change. The map composable itself is compile-verified +
smoke-tested on device.

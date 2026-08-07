# :map

The map feature: the app's single screen.

**Contains:** `MapScreen` (stateful root) → `MapContent` (stateless) → `StatesMap`
(maplibre-compose map with OpenFreeMap base styles — Positron in light mode, Dark in
dark mode), `CounterChip`,
`MapLoadErrorBanner` (style-load failure + retry, which recreates the map), the
`VisitDetailsSheet` (date + notes), `globeOrnamentOptions` (expect/actual ornament
placement: no scale bar, attribution + logo on the bottom, safe-area padded), and
`MapViewModel` (single UiState StateFlow combining map pack + visits).

**Rendering:** the pack's FeatureCollection is loaded into a single static GeoJSON
source exactly once. Visited-ness is expressed as a layer `filter`
(`code ∈ visitedCodes`) on the fill/outline layers — toggling a state swaps a tiny
filter expression instead of re-parsing/re-tessellating geometry, so fills update
instantly.

**Depends on:** `:domain` (contracts), `:design` (theme/palette), maplibre-compose,
Koin (compose + viewmodel), lifecycle, kotlinx-serialization,
kotlinx-collections-immutable.

**Interaction:** tap a state → toggle visited; long-press → details sheet. The map is
locked north-up (rotation/tilt gestures disabled), so the compass never appears.

**Tests:** `./gradlew :map:testDebugUnitTest` — ViewModel behavior against hand-written
fakes (await state conditions), including a regression test that geometry is never
rebuilt when visits change. The map composable itself is compile-verified +
smoke-tested on device.

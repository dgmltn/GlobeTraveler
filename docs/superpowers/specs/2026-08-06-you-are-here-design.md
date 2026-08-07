# "You are here" dot — design

**Date:** 2026-08-06
**Status:** Approved (design reviewed in conversation)

## Goal

Show an approximate "you are here" dot on the map. State-level accuracy is sufficient, so
location comes from IP geolocation — no OS location permissions, no platform location APIs.

## Location source

- **Service:** geojs.io — HTTPS, keyless, no meaningful rate limits.
  - Bare IP: `https://get.geojs.io/v1/ip.json` (a few bytes)
  - Geo lookup: `https://get.geojs.io/v1/ip/geo.json` (note: `latitude`/`longitude` are JSON
    *strings*)
- **Transport:** Ktor client (first HTTP dependency in the project): core + content-negotiation +
  kotlinx-json + logging; engines okhttp (android), darwin (ios), java (jvm). Versions pinned
  from live Maven metadata.

## Contracts (`:domain`)

- `ApproximateLocation(latitude: Double, longitude: Double)`
- `DeviceLocationRepository { fun observeLocation(): Flow<ApproximateLocation> }` — a cold
  stream so the dot can move; any future provider (GPS, manual pin) swaps in behind this.

## Implementation (`:data`)

`IpLocationRepositoryImpl(httpClient, pollInterval = 1.minutes)`:

- Loop while collected: fetch bare IP → if first check or IP differs from last seen, fetch the
  geo lookup and emit its coordinates → delay `pollInterval` → repeat.
- Unchanged IP → no geo request (assert via request counts in tests).
- Any failure: Kermit warning, no emission (last emitted value simply stands downstream);
  retry on the next poll. No emission until first success — offline means no dot.
- Koin-wired in `dataModule`.

## UI (`:map`)

- `MapViewModel` folds the flow into the existing `combine(...)` as `MapUiState.userLocation:
  ApproximateLocation?` (default null). `WhileSubscribed(5s)` on the state flow means polling
  pauses in background, resumes on return.
- Rendering in `MapScreen`: one-point GeoJSON source updated on each emission, drawn as two
  `CircleLayer`s — translucent halo (~14dp) conveying "approximate", solid dot (~5dp, white
  stroke). Location-blue with `MapPalette` Light/Dark variants. No click handlers, so taps pass
  through to states. Camera is never moved by the dot.

## Testing

- `:data` jvmTest with Ktor `MockEngine` + virtual time: first emission, refresh on IP change,
  no geo request when IP unchanged, failure → no emission then recovery on next poll.
- `:map` ViewModel test: fake `DeviceLocationRepository` emission lands in `MapUiState`.

## Out of scope

Persisting last location across launches, accuracy-scaled halo, camera follow, real GPS.

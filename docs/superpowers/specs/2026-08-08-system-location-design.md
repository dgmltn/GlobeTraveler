# System location for the "you are here" dot — design

**Date:** 2026-08-08
**Status:** Approved (design reviewed in conversation)

## Goal

Replace the IP-geolocation source behind the "you are here" dot with the platform's built-in
location service, using coarse/reduced accuracy — state-level is still all that's needed. The
IP/geojs implementation and its Ktor dependency chain are deleted everywhere.

## Contracts (`:domain`) — unchanged

`ApproximateLocation` and `DeviceLocationRepository.observeLocation(): Flow<ApproximateLocation>`
stay exactly as they are: cold flow, no emission until a fix is known, never throws. `:map`
(MapViewModel, MapScreen, fakes, tests) is untouched.

## Implementation (`:data`)

New expect/actual, mirroring the existing `databaseBuilder()` pattern:

```kotlin
internal expect fun Scope.deviceLocationRepository(): DeviceLocationRepository
```

wired in `dataModule()` as `single<DeviceLocationRepository> { deviceLocationRepository() }`.

- **androidMain — `SystemLocationRepository`** over framework `LocationManager` (no new
  dependency). `callbackFlow` that:
  1. Waits for `ACCESS_COARSE_LOCATION`, re-checking every few seconds, so the dot appears
     right after the first-launch grant without restarting collection.
  2. Emits `getLastKnownLocation` immediately when available.
  3. Subscribes to `requestLocationUpdates` on `FUSED_PROVIDER` (API 31+; `NETWORK_PROVIDER`
     below — minSdk is 26) with ~60 s / ~1 km thresholds; each fix is an emission.
- **iosMain** — `CLLocationManager` with `kCLLocationAccuracyThreeKilometers`; requests
  when-in-use authorization itself when status is not-determined; delegate fixes become
  emissions. `NSLocationWhenInUseUsageDescription` added to `app-ios/project.yml`.
- **jvmMain** — never-emitting flow; the JVM target exists only for tests.

## Permission UX

- **Android:** `MainActivity` launches `ActivityResultContracts.RequestPermission` for
  `ACCESS_COARSE_LOCATION` at startup when not yet granted; `AndroidManifest.xml` declares the
  permission. Denied → no dot, no nagging beyond the system's own re-prompt behavior.
- **iOS:** the repository's own authorization request covers it; the usage-description string
  is the only app-side change.

## Cleanup

- Delete `IpLocationRepositoryImpl` + `IpLocationRepositoryImplTest`.
- Remove the `HttpClient` Koin single and Ktor client config from `DataModule`.
- Remove all Ktor artifacts from `data/build.gradle.kts` and `gradle/libs.versions.toml`
  (Ktor was used only by the IP repo). kotlinx-serialization stays — `MapPackRepositoryImpl`
  uses it.
- Update `data/README.md`, `domain/README.md`, `map/README.md`, `ARCHITECTURE.md` where they
  describe the IP source / Ktor.

## Testing

Platform impls are thin callback glue — compile-verified per project convention. Existing
MapViewModel tests keep passing against the unchanged fake repository. The IP repo's jvmTest
suite is deleted with it. Verification is running the app on-device and seeing the dot.

## Out of scope

Precise/fine location, camera follow, background location, persisting last fix across
launches, a location-permission education/settings screen.

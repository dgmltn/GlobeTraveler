# :domain

App-wide domain models and repository contracts. Pure Kotlin — no Android, Compose,
or persistence dependencies.

**Contains:** `MapId`/`RegionCode`/`RegionId`, `Region`, `MapDescriptor` (+`CameraDefaults`),
`MapPack`, `TrackedMap` (+`TrackedMapId`/`MapAccent` — a named tracking collection over a
pack; visits are keyed per tracked map), `Visit`, `ApproximateLocation`, and the
`VisitRepository` / `MapPackRepository` / `TrackedMapRepository` /
`DeviceLocationRepository` contracts implemented by `:data`.

**Depends on:** kotlinx-coroutines (Flow), kotlinx-datetime (LocalDate). Nothing else.

**Tests:** `./gradlew :domain:jvmTest`

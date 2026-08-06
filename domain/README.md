# :domain

App-wide domain models and repository contracts. Pure Kotlin — no Android, Compose,
or persistence dependencies.

**Contains:** `MapId`/`RegionCode`/`RegionId`, `Region`, `MapDescriptor` (+`CameraDefaults`),
`MapPack`, `Visit`, and the `VisitRepository` / `MapPackRepository` contracts implemented
by `:data`.

**Depends on:** kotlinx-coroutines (Flow), kotlinx-datetime (LocalDate). Nothing else.

**Tests:** `./gradlew :domain:jvmTest`

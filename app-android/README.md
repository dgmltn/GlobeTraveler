# :app-android

Thin Android entry point and DI composition root.

**Contains:** `GlobeApp` (starts Koin with `dataModule()` + `mapModule()`),
`MainActivity` (`GlobeTheme { MapScreen() }`), manifest (INTERNET permission for
basemap tiles).

**Depends on:** `:domain`, `:data`, `:design`, `:map`, activity-compose, koin-android.

**Build:** `./gradlew :app-android:assembleDebug`

# :app-ios

Thin iOS entry point and DI composition root.

**Contains:** Kotlin framework `GlobeTravelerKit` (`MainViewController()` wrapping
`GlobeTheme { MapScreen() }`, `doInitKoin()`), XcodeGen `project.yml` (SwiftUI app,
MapLibre native via SPM pinned to 6.25.1, pre-build script that runs
`:app-ios:embedAndSignAppleFrameworkForXcode`), `Sources/GlobeTravelerApp.swift`.

**Depends on:** `:domain`, `:data`, `:design`, `:map`, koin-core.

**Build:**

```
cd app-ios
xcodegen                     # generates GlobeTraveler.xcodeproj (gitignored)
xcodebuild -project GlobeTraveler.xcodeproj -scheme GlobeTraveler \
  -destination 'platform=iOS Simulator,name=iPhone 16 Pro' build
```

Notes: `Info.plist` must keep `CADisableMinimumFrameDurationOnPhone: true` (Compose
requires it). The Kotlin framework is intentionally linked before MapLibre.framework
(maplibre-compose text-rendering requirement).

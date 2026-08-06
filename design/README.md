# :design

The design system: theme and foundational visual constants shared by all feature modules.

**Contains:** `GlobeTheme` (Material 3 light/dark color schemes), `MapPalette`
(map fill/border colors tuned for the Positron basemap).

**Depends on:** Compose Multiplatform runtime/ui/foundation/material3 (exposed as `api`
so feature modules get Compose via this module's theme surface).

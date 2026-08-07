# :design

The design system: theme and foundational visual constants shared by all feature modules.

**Contains:** `GlobeTheme` (Material 3 light/dark color schemes), `MapPalette`
(map fill/border colors, with `Light`/`Dark` variants tuned to the Positron and Dark
basemaps).

**Depends on:** Compose Multiplatform runtime/ui/foundation/material3 (exposed as `api`
so feature modules get Compose via this module's theme surface).

# State-only basemap labels — design

**Date:** 2026-08-09
**Status:** Approved (design reviewed in conversation)

## Goal

The basemap must show no city/town/village/suburb/country/continent names — state names
only. Non-place labels (water names, road shields) are untouched.

## Approach

City names are symbol layers inside the remote OpenFreeMap style JSONs, and
maplibre-compose cannot mutate a base style loaded by URI — but it accepts
`BaseStyle.Json`. So the styles are pre-stripped offline and bundled, following the
`build-mappack.py` precedent (chosen over runtime fetch-and-strip, which would
reintroduce an HTTP client for a document that rarely changes; trade-off: the style is
pinned until the script is re-run).

## Script (`scripts/strip-basemap-labels.py`)

Downloads Positron (light) and Dark styles fresh on every run, drops every `symbol`
layer with `source-layer: "place"` whose id doesn't end in `_state` (Positron:
`label_state`; Dark: `place_state`), and writes minified results to
`map/src/commonMain/composeResources/files/basemap-{light,dark}.json` (committed,
"generated — do not edit"). Asserts exactly one surviving place layer and that a
sane number of layers were removed, so upstream renames fail loudly instead of
silently restoring city names. Tile/glyph/sprite URLs are absolute, so rendering
still streams from openfreemap.org.

## Runtime (`:map`)

`:map` gains compose components-resources (same setup as `:data`). `LoadableMap` reads
the bundled JSON for the current color scheme via `Res.readBytes` and passes
`BaseStyle.Json` instead of `BaseStyle.Uri`. Until the local read completes (instant in
practice) the map isn't composed — same gate as pack loading.

## Revision (same day)

Outside the US, country names should show — and non-US states/provinces should not.
The script now keeps the `*country*` label layers and injects a US-only name filter
into the state layer (`["in", ["get","name"], ["literal", <the 50 region names from
us-states.descriptor.json>]]`, ANDed with the existing class filter), so the map pack
descriptor stays the single source of truth for which states label. Continents and all
city/town/village/suburb labels remain stripped.

## Testing

An `:map` host-side unit test reads both bundled files (module-relative path) and
asserts every remaining `place` layer id ends in `_state`. The script's own asserts
guard regeneration. Real verification is on-device.

## Out of scope

Removing water/road labels, custom state-label styling, runtime style refresh.

# Multiple tracked maps — design

**Date:** 2026-08-10
**Status:** Approved (design reviewed in conversation; option "counter becomes the switcher")

## Goal

Several ongoing collections over the same geography — e.g. "Visited" and "License plates" —
one visible at a time, each with its own visits (dates + notes per state per collection).
The top-center counter becomes the switcher.

## Concepts

The geography pack (`MapId("us-states")`) and the tracking collection are now distinct.
A **tracked map** is a named collection over a pack:

- `TrackedMapId` (value class), `TrackedMap(id, packId: MapId, name, accent: MapAccent,
  createdAt: Instant)`.
- `MapAccent` enum in `:domain` (Green, Blue, Orange, Purple, Red, Teal); actual colors
  live in `:design` (light + dark fill/outline per accent; Green = today's visited green).
  Accents are auto-assigned round-robin on create — no picker in v1.
- `RegionId.mapId: MapId` becomes `RegionId.trackedMapId: TrackedMapId`; `Visit` otherwise
  unchanged (dates/notes are naturally per collection).

## Contracts (`:domain`)

- `VisitRepository`: `observeVisits(TrackedMapId)`, `toggle(RegionId)`,
  `updateDetails(Visit)` — same shape, rekeyed. Plus `observeVisitCounts():
  Flow<Map<TrackedMapId, Int>>` for the switcher menu rows.
- New `TrackedMapRepository`: `observeMaps()`, `observeActiveMap()`,
  `suspend create(name): TrackedMap` (auto-accent, becomes active),
  `suspend setActive(TrackedMapId)`. Rename/delete deferred.

## Data (`:data`)

- New Room table `tracked_maps` (id pk, packId, name, accent, createdAtEpochMillis).
  DB version 1 → 2 with a **manual migration**: create `tracked_maps`, seed a
  `('visited', 'us-states', 'Visited', 'GREEN', 0)` row, and rebuild `visits` renaming
  `mapId` → `trackedMapId` with all existing rows assigned to `'visited'` (existing data
  on Doug's phone survives).
- `TrackedMapRepositoryImpl` seeds the default "Visited" map when the table is empty
  (fresh installs), assigns the next unused accent by cycling the enum, and generates ids
  from `kotlin.uuid.Uuid`.
- Active map id lives in **DataStore Preferences** (new KMP dependency, latest stable
  pinned from live metadata; `createWithPath` via the same expect/actual pattern as the
  database builder). `observeActiveMap()` = active id pref + maps list, falling back to
  the oldest map when the pref is unset or dangling.

## UI (`:map`)

- The title becomes "<name> · <n>/<total>" in the active map's accent color with a ▾.
  Tapping opens a `DropdownMenu` under the scrim: one row per map ("License plates ·
  34/50"), active row highlighted, divider, then "New map…".
- "New map…" opens a dialog with a name field; create switches to the new map.
- The map's visited fill/outline colors come from the active accent, so each collection
  is visually distinct at a glance. Unvisited fill, borders, and the you-are-here dot are
  unchanged.
- Switching maps re-keys the visits flow (`flatMapLatest`); geometry never reloads.
- `VisitDetailsSheet` untouched — it already edits the `Visit` it is given.

## Testing

- `:data` jvmTest: tracked-map repo (default seeding, create/auto-accent/active
  persistence via temp-file DataStore, dangling-pref fallback) and rekeyed visit repo.
- `:map` host tests: switching updates state; existing ViewModel tests updated to the
  new fakes.
- Manual upgrade check on the emulator: install the current release, mark states,
  install this build over it, confirm the marks survive under "Visited".

## Out of scope

Rename/delete maps, accent picker, compare/overlay view, per-map camera, multiple
geography packs in the UI.

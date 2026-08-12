# Rename & delete tracked maps — design

**Date:** 2026-08-11
**Status:** Approved (design reviewed in conversation; pencil entry point chosen)

## Goal

Rename and delete tracked maps from the switcher menu.

## UI (`:map`)

Each switcher menu row gains a trailing "✎". Tapping it closes the menu and opens an
**Edit map** dialog: name field pre-filled (Save = rename, disabled when blank), and a
"Delete map" button in the error color. Delete swaps the dialog to a confirmation —
"Delete "<name>"? Its <n> marked states, dates, and notes will be deleted." — with a
final Delete button. The last remaining map's Delete button is disabled ("never empty"
invariant, no empty-state screen). Deleting the active map is allowed; the existing
dangling-selection fallback lands on the oldest remaining map.

## Contracts (`:domain`)

`TrackedMapRepository` gains `suspend fun rename(id: TrackedMapId, name: String)` (trimmed)
and `suspend fun delete(id: TrackedMapId)` (no-op on the last map).

## Data (`:data`)

`TrackedMapDao`: UPDATE name / DELETE by id. `VisitDao`: DELETE by trackedMapId.
`TrackedMapRepositoryImpl` gains the `VisitDao` and deletes visits first, then the map
(orphan-safe order). No schema change, no migration.

## Testing

jvmTest: rename persists (and trims), delete cascades visits, deleting the active map
falls back to the oldest, deleting the last map is a no-op. ViewModel test: rename shows
up in rows; delete removes the row and switches state. Emulator pass over the dialog flow.

## Out of scope

Accent editing, reordering, undo.

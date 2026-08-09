# Top gradient scrim + plain visited counter — design

**Date:** 2026-08-09
**Status:** Approved (design reviewed in conversation)

## Goal

Replace the counter pill's black scrim with a softer top-of-screen treatment: the counter
becomes plain text, and a full-width vertical gradient (theme background at the top,
transparent at the bottom) spans the status bar and the counter title.

## Theme (`:design`)

`GlobeTheme`'s color schemes gain explicit `background` values matching the basemap ground
colors from the bundled styles (see `scripts/strip-basemap-labels.py` output): light
`#F2F3F0` (Positron ground), dark `#0C0C0C` (Dark ground). The gradient then reads as the
map fading out, and `onBackground` text is legible over it in both themes.

## UI (`:map`)

- `CounterChip` → renamed `VisitedCounter`: plain `Text` in `colorScheme.onBackground`,
  no `Surface`, no scrim. Previews renamed and kept (populated + zero states).
- New private `CounterScrim`: full-width `Box` aligned top, background
  `Brush.verticalGradient(background → transparent)`, content inset by the status-bar
  window insets, `VisitedCounter` centered with small vertical padding. The gradient's
  height is intrinsic — status bar + counter + padding — replacing the hardcoded 48 dp
  offset. Shown when not loading, same as the chip today.
- No touch handling: the scrim is decorative; map gestures under it are unaffected
  (Box has no click handlers, so taps pass through as before).

## Testing

Compile + previews; ViewModel tests untouched (pure presentation change). On-device look
check in light and dark.

## Out of scope

Bottom-edge treatment, ornament restyling, counter animation.

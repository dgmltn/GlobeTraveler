#!/usr/bin/env python3
"""Strips non-state place labels from the OpenFreeMap basemap styles bundled in :map.

Downloads the Positron (light) and Dark styles and removes every place-label symbol
layer except state names — no city/town/village/suburb/country/continent names. The
minified results land in map/src/commonMain/composeResources/files/ and are committed;
re-run to pick up upstream style changes.

Usage: scripts/strip-basemap-labels.py
"""

import json
import pathlib
import urllib.request

ROOT = pathlib.Path(__file__).resolve().parent.parent
OUT_DIR = ROOT / "map/src/commonMain/composeResources/files"
STYLES = {
    "basemap-light.json": "https://tiles.openfreemap.org/styles/positron",
    "basemap-dark.json": "https://tiles.openfreemap.org/styles/dark",
}


def keeps(layer):
    if layer.get("type") != "symbol" or layer.get("source-layer") != "place":
        return True
    # Positron names it label_state, Dark names it place_state.
    return layer["id"].endswith("_state")


def main():
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    for name, url in STYLES.items():
        # openfreemap.org rejects urllib's default User-Agent with a 403.
        request = urllib.request.Request(url, headers={"User-Agent": "globetraveler-build"})
        with urllib.request.urlopen(request) as response:
            style = json.load(response)

        kept = [layer for layer in style["layers"] if keeps(layer)]
        removed = len(style["layers"]) - len(kept)
        style["layers"] = kept

        state_layers = [l["id"] for l in kept if l.get("source-layer") == "place"]
        assert state_layers and all(i.endswith("_state") for i in state_layers), state_layers
        assert removed >= 5, f"{name}: only removed {removed} layers; upstream layout changed?"

        out = OUT_DIR / name
        out.write_text(json.dumps(style, separators=(",", ":")), encoding="utf-8")
        print(f"wrote {out} ({out.stat().st_size / 1024:.0f} KiB, "
              f"removed {removed} place layers, kept {', '.join(state_layers)})")


if __name__ == "__main__":
    main()

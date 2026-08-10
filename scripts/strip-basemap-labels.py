#!/usr/bin/env python3
"""Strips place labels from the OpenFreeMap basemap styles bundled in :map.

Downloads the Positron (light) and Dark styles and rewrites their place labels: US state
names and country names stay, everything else (cities, towns, villages, suburbs,
continents, non-US states/provinces) goes. The state layer is limited to the US by a
name filter built from the map pack's descriptor. The minified results land in
map/src/commonMain/composeResources/files/ and are committed; re-run to pick up
upstream style changes.

Usage: scripts/strip-basemap-labels.py
"""

import json
import pathlib
import urllib.request

ROOT = pathlib.Path(__file__).resolve().parent.parent
DESCRIPTOR = ROOT / "data/src/commonMain/composeResources/files/us-states.descriptor.json"
OUT_DIR = ROOT / "map/src/commonMain/composeResources/files"
STYLES = {
    "basemap-light.json": "https://tiles.openfreemap.org/styles/positron",
    "basemap-dark.json": "https://tiles.openfreemap.org/styles/dark",
}


def main():
    state_names = [r["name"] for r in json.loads(DESCRIPTOR.read_text())["regions"]]
    assert len(state_names) == 50, f"expected 50 state names, got {len(state_names)}"
    us_only = ["in", ["get", "name"], ["literal", state_names]]

    OUT_DIR.mkdir(parents=True, exist_ok=True)
    for name, url in STYLES.items():
        # openfreemap.org rejects urllib's default User-Agent with a 403.
        request = urllib.request.Request(url, headers={"User-Agent": "globetraveler-build"})
        with urllib.request.urlopen(request) as response:
            style = json.load(response)

        kept, state_layers, country_layers = [], [], []
        for layer in style["layers"]:
            if layer.get("type") != "symbol" or layer.get("source-layer") != "place":
                kept.append(layer)
            elif "country" in layer["id"]:
                country_layers.append(layer["id"])
                kept.append(layer)
            # Positron names it label_state, Dark names it place_state.
            elif layer["id"].endswith("_state"):
                old = layer.get("filter")
                layer["filter"] = ["all", old, us_only] if old else us_only
                state_layers.append(layer["id"])
                kept.append(layer)
        removed = len(style["layers"]) - len(kept)
        style["layers"] = kept

        assert len(state_layers) == 1, f"{name}: state layers {state_layers}"
        assert len(country_layers) >= 3, f"{name}: country layers {country_layers}"
        assert removed >= 4, f"{name}: only removed {removed} layers; upstream layout changed?"

        out = OUT_DIR / name
        out.write_text(json.dumps(style, separators=(",", ":")), encoding="utf-8")
        print(f"wrote {out} ({out.stat().st_size / 1024:.0f} KiB, removed {removed}, "
              f"kept {', '.join(state_layers + country_layers)})")


if __name__ == "__main__":
    main()

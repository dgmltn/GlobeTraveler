# Geodata

Source data and pipeline for bundled map packs.

- `source/gz_2010_us_040_00_20m.json` — US Census Bureau 2010 cartographic boundary
  file (states, 1:20,000,000), public domain, mirrored from
  https://eric.clst.org/technical/stategeodata/ . Committed for reproducibility.
- Processed output lives in `data/src/commonMain/composeResources/files/`:
  `us-states.geojson` (50 states, properties `{code, name}`, coordinates rounded to
  4 decimals) and `us-states.descriptor.json` (region list + default camera).

**Regenerate:** `python3 scripts/build-mappack.py` (re-downloads the source only if
missing).

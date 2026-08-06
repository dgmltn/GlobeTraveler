#!/usr/bin/env python3
"""Builds the us-states map pack bundled in :data.

Downloads the US Census 2010 cartographic boundary GeoJSON (1:20,000,000) once into
resources/geodata/source/, then writes a minified 50-state FeatureCollection plus a
descriptor JSON into data/src/commonMain/composeResources/files/.

Usage: scripts/build-mappack.py
"""

import json
import pathlib
import urllib.request

ROOT = pathlib.Path(__file__).resolve().parent.parent
SOURCE_URL = "https://eric.clst.org/assets/wiki/uploads/Stuff/gz_2010_us_040_00_20m.json"
SOURCE_FILE = ROOT / "resources/geodata/source/gz_2010_us_040_00_20m.json"
OUT_DIR = ROOT / "data/src/commonMain/composeResources/files"

# FIPS state codes -> USPS abbreviations. DC (11) and PR (72) intentionally absent.
FIPS_TO_USPS = {
    "01": "AL", "02": "AK", "04": "AZ", "05": "AR", "06": "CA", "08": "CO",
    "09": "CT", "10": "DE", "12": "FL", "13": "GA", "15": "HI", "16": "ID",
    "17": "IL", "18": "IN", "19": "IA", "20": "KS", "21": "KY", "22": "LA",
    "23": "ME", "24": "MD", "25": "MA", "26": "MI", "27": "MN", "28": "MS",
    "29": "MO", "30": "MT", "31": "NE", "32": "NV", "33": "NH", "34": "NJ",
    "35": "NM", "36": "NY", "37": "NC", "38": "ND", "39": "OH", "40": "OK",
    "41": "OR", "42": "PA", "44": "RI", "45": "SC", "46": "SD", "47": "TN",
    "48": "TX", "49": "UT", "50": "VT", "51": "VA", "53": "WA", "54": "WV",
    "55": "WI", "56": "WY",
}

CAMERA = {"latitude": 39.5, "longitude": -98.35, "zoom": 3.0}


def round_coords(value, places=4):
    if isinstance(value, float):
        return round(value, places)
    if isinstance(value, list):
        return [round_coords(v, places) for v in value]
    return value


def main():
    if not SOURCE_FILE.exists():
        SOURCE_FILE.parent.mkdir(parents=True, exist_ok=True)
        print(f"downloading {SOURCE_URL}")
        urllib.request.urlretrieve(SOURCE_URL, SOURCE_FILE)

    raw = json.loads(SOURCE_FILE.read_text(encoding="latin-1"))
    features = []
    for feature in raw["features"]:
        fips = feature["properties"]["STATE"]
        code = FIPS_TO_USPS.get(fips)
        if code is None:
            continue
        geometry = feature["geometry"]
        assert geometry["type"] in ("Polygon", "MultiPolygon"), geometry["type"]
        features.append({
            "type": "Feature",
            "properties": {"code": code, "name": feature["properties"]["NAME"]},
            "geometry": {
                "type": geometry["type"],
                "coordinates": round_coords(geometry["coordinates"]),
            },
        })

    codes = [f["properties"]["code"] for f in features]
    assert len(features) == 50, f"expected 50 states, got {len(features)}"
    assert len(set(codes)) == 50, "duplicate state codes"

    features.sort(key=lambda f: f["properties"]["code"])
    OUT_DIR.mkdir(parents=True, exist_ok=True)

    geojson_path = OUT_DIR / "us-states.geojson"
    geojson_path.write_text(json.dumps(
        {"type": "FeatureCollection", "features": features},
        separators=(",", ":"),
    ), encoding="utf-8")

    descriptor_path = OUT_DIR / "us-states.descriptor.json"
    descriptor_path.write_text(json.dumps({
        "mapId": "us-states",
        "name": "United States",
        "geometryAsset": "us-states.geojson",
        "camera": CAMERA,
        "regions": [
            {"code": f["properties"]["code"], "name": f["properties"]["name"]}
            for f in features
        ],
    }, indent=2), encoding="utf-8")

    print(f"wrote {geojson_path} ({geojson_path.stat().st_size / 1024:.0f} KiB)")
    print(f"wrote {descriptor_path}")


if __name__ == "__main__":
    main()

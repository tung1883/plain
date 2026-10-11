#!/usr/bin/env python3
"""Build app/src/main/assets/map/roads.json — a driving-network graph for the Hanoi bbox,
read on-device by RoadGraph.java and routed over by RouteFinder.java's A*.

Downloads Vietnam's OSM extract from Geofabrik once (cached locally, not committed — it's
~300MB) and streams through it with pyosmium, keeping only drivable ways that touch the bbox.
This is a one-off developer-machine step, not part of the app or its build.

(Originally this used the Overpass API directly, matching the plan's write-up, but every public
Overpass mirror reachable from here was down or serving an empty/stub database the day this was
run — see commit history. A full-country extract + local filtering is slower to set up once but
doesn't depend on a public API's uptime.)

Usage:
    .venv/Scripts/python.exe build_roads.py
"""

import json
import math
import sys
from pathlib import Path

import osmium
import requests

BBOX = (105.6, 20.85, 106.05, 21.25)  # west, south, east, north — same as hanoi.pmtiles
EXTRACT_URL = "https://download.geofabrik.de/asia/vietnam-latest.osm.pbf"
CACHE_DIR = Path(__file__).resolve().parent / ".cache"
CACHE_PATH = CACHE_DIR / "vietnam-latest.osm.pbf"
OUT_PATH = Path(__file__).resolve().parents[2] / "app/src/main/assets/map/roads.json"

DRIVABLE_HIGHWAY = {
    "motorway", "trunk", "primary", "secondary", "tertiary", "unclassified",
    "residential", "motorway_link", "trunk_link", "primary_link", "secondary_link",
    "tertiary_link", "living_street", "service",
}


def ensure_extract():
    if CACHE_PATH.is_file():
        return
    CACHE_DIR.mkdir(parents=True, exist_ok=True)
    print(f"Downloading {EXTRACT_URL} (~330MB, one-time)...", file=sys.stderr)
    with requests.get(EXTRACT_URL, stream=True, timeout=600) as r:
        r.raise_for_status()
        tmp = CACHE_PATH.with_suffix(".pbf.part")
        with open(tmp, "wb") as f:
            for chunk in r.iter_content(chunk_size=1024 * 1024):
                f.write(chunk)
        tmp.rename(CACHE_PATH)


def haversine(lat1, lon1, lat2, lon2):
    r = 6371000.0
    p1, p2 = math.radians(lat1), math.radians(lat2)
    dp = math.radians(lat2 - lat1)
    dl = math.radians(lon2 - lon1)
    a = math.sin(dp / 2) ** 2 + math.cos(p1) * math.cos(p2) * math.sin(dl / 2) ** 2
    return 2 * r * math.asin(math.sqrt(a))


def in_bbox(lat, lon):
    west, south, east, north = BBOX
    return south <= lat <= north and west <= lon <= east


class RoadHandler(osmium.SimpleHandler):
    def __init__(self):
        super().__init__()
        self.ways = []  # (list of (lat,lon), oneway)
        self.scanned = 0

    def way(self, w):
        self.scanned += 1
        highway = w.tags.get("highway")
        if highway not in DRIVABLE_HIGHWAY:
            return
        pts = []
        touches_bbox = False
        for n in w.nodes:
            if not n.location.valid():
                return  # incomplete way (node outside this extract) — skip
            lat, lon = n.location.lat, n.location.lon
            pts.append((lat, lon))
            if in_bbox(lat, lon):
                touches_bbox = True
        if not touches_bbox or len(pts) < 2:
            return
        oneway_tag = w.tags.get("oneway", "no")
        if oneway_tag in ("yes", "1", "true"):
            oneway = "forward"
        elif oneway_tag == "-1":
            oneway = "reverse"
        else:
            oneway = "both"
        self.ways.append((pts, oneway))


def main():
    ensure_extract()

    print("Scanning extract for drivable ways touching the bbox "
          "(single pass, can take a few minutes)...", file=sys.stderr)
    handler = RoadHandler()
    handler.apply_file(str(CACHE_PATH), locations=True)
    print(f"Scanned {handler.scanned} ways, kept {len(handler.ways)} in-bbox drivable ways.",
          file=sys.stderr)

    # Dedupe coordinates into dense node indices (two ways sharing an intersection point get
    # the same node id as long as their coordinates match exactly, which OSM guarantees for
    # shared nodes).
    coord_to_idx = {}
    node_lat, node_lon = [], []

    def idx_for(lat, lon):
        key = (lat, lon)
        idx = coord_to_idx.get(key)
        if idx is None:
            idx = len(node_lat)
            coord_to_idx[key] = idx
            node_lat.append(lat)
            node_lon.append(lon)
        return idx

    edge_from, edge_to, edge_length = [], [], []

    def add_edge(a, b):
        length = haversine(node_lat[a], node_lon[a], node_lat[b], node_lon[b])
        if length <= 0:
            return
        edge_from.append(a)
        edge_to.append(b)
        edge_length.append(round(length, 1))

    for pts, oneway in handler.ways:
        idxs = [idx_for(lat, lon) for lat, lon in pts]
        for a, b in zip(idxs, idxs[1:]):
            if a == b:
                continue
            if oneway in ("both", "forward"):
                add_edge(a, b)
            if oneway in ("both", "reverse"):
                add_edge(b, a)

    out = {
        "bbox": list(BBOX),
        "nodeCount": len(node_lat),
        "edgeCount": len(edge_from),
        "nodes": {"lat": node_lat, "lon": node_lon},
        "edges": {"from": edge_from, "to": edge_to, "length": edge_length},
    }

    OUT_PATH.parent.mkdir(parents=True, exist_ok=True)
    with open(OUT_PATH, "w", encoding="utf-8") as f:
        json.dump(out, f)

    print(f"Wrote {OUT_PATH} - {out['nodeCount']} nodes, {out['edgeCount']} edges "
          f"({OUT_PATH.stat().st_size / 1024:.0f} KB)", file=sys.stderr)


if __name__ == "__main__":
    main()

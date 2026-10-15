#!/usr/bin/env python3
"""Build a driving-network graph (roads.json) for a given bbox, read on-device by
RoadGraph.java and routed over by RouteFinder.java's A*.

Downloads a Geofabrik country/region OSM extract once per URL (cached locally under .cache/,
not committed) and streams through it with pyosmium, keeping only drivable ways that touch the
bbox. This is a one-off developer-machine step, not part of the app or its build.

(Originally this used the Overpass API directly, matching the plan's write-up, but every public
Overpass mirror reachable from here was down or serving an empty/stub database the day this was
run — see commit history. A full-country extract + local filtering is slower to set up once but
doesn't depend on a public API's uptime.)

Collapses chains of degree-2 "pass-through" nodes (a point where exactly one road just continues
straight through, not an intersection or dead end) into a single longer edge — otherwise every
OSM way-point becomes its own graph node, which is why the original Hanoi-only roads.json was
45MB / 673k nodes for what should be a far smaller intersection graph. A* only ever needs to stop
at actual decision points; the collapsed-away points' lat/lon don't matter for routing distance
(each surviving edge's length is still the real along-the-road sum, not a straight-line shortcut).

Usage:
    .venv/Scripts/python.exe build_roads.py --bbox west,south,east,north --out <path> \
        --extract-url https://download.geofabrik.de/asia/xxx-latest.osm.pbf
"""

import argparse
import json
import math
import sys
from pathlib import Path

import osmium
import requests

CACHE_DIR = Path(__file__).resolve().parent / ".cache"

DRIVABLE_HIGHWAY = {
    "motorway", "trunk", "primary", "secondary", "tertiary",
    "motorway_link", "trunk_link", "primary_link", "secondary_link", "tertiary_link",
}
# "unclassified", "residential", "living_street" and "service" were included for the original
# single-city Hanoi build, but at country scale they're the overwhelming majority of the raw
# node/edge count (45M+ edges, which a pure-Python collapse pass can't handle without running
# out of memory) for streets that add little to city-to-city routing. Keeping just the arterial
# backbone (motorway through tertiary) — a route may start/end a short walk from the exact
# address on a side street, snapped to the nearest backbone node instead.


def ensure_extract(extract_url, cache_path):
    if cache_path.is_file():
        return
    CACHE_DIR.mkdir(parents=True, exist_ok=True)
    print(f"Downloading {extract_url} (one-time)...", file=sys.stderr)
    with requests.get(extract_url, stream=True, timeout=600) as r:
        r.raise_for_status()
        tmp = cache_path.with_suffix(".pbf.part")
        with open(tmp, "wb") as f:
            for chunk in r.iter_content(chunk_size=1024 * 1024):
                f.write(chunk)
        tmp.rename(cache_path)


def haversine(lat1, lon1, lat2, lon2):
    r = 6371000.0
    p1, p2 = math.radians(lat1), math.radians(lat2)
    dp = math.radians(lat2 - lat1)
    dl = math.radians(lon2 - lon1)
    a = math.sin(dp / 2) ** 2 + math.cos(p1) * math.cos(p2) * math.sin(dl / 2) ** 2
    return 2 * r * math.asin(math.sqrt(a))


def make_road_handler(bbox, drivable_highway):
    """Builds the deduped node/edge graph directly inside way() rather than collecting every
    matched way's full point list first and converting afterward — for a whole big country,
    keeping every matched way's raw (lat, lon) tuples alive until a later pass is what was
    actually blowing up memory (millions of motorway/trunk/primary ways across all of China,
    each with many shape points). This way each way's points are discarded as soon as they're
    folded into the (much more compact, deduped) node/edge arrays."""
    west, south, east, north = bbox

    def in_bbox(lat, lon):
        return south <= lat <= north and west <= lon <= east

    class RoadHandler(osmium.SimpleHandler):
        def __init__(self):
            super().__init__()
            self.scanned = 0
            self.coord_to_idx = {}
            self.node_lat = []
            self.node_lon = []
            self.edges = []  # (from, to, length)

        def idx_for(self, lat, lon):
            key = (lat, lon)
            idx = self.coord_to_idx.get(key)
            if idx is None:
                idx = len(self.node_lat)
                self.coord_to_idx[key] = idx
                self.node_lat.append(lat)
                self.node_lon.append(lon)
            return idx

        def way(self, w):
            self.scanned += 1
            highway = w.tags.get("highway")
            if highway not in drivable_highway:
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

            idxs = [self.idx_for(lat, lon) for lat, lon in pts]
            for a, b in zip(idxs, idxs[1:]):
                if a == b:
                    continue
                length = haversine(self.node_lat[a], self.node_lon[a],
                                    self.node_lat[b], self.node_lon[b])
                if length <= 0:
                    continue
                length = round(length, 1)
                if oneway in ("both", "forward"):
                    self.edges.append((a, b, length))
                if oneway in ("both", "reverse"):
                    self.edges.append((b, a, length))

    return RoadHandler()


def collapse_pass_through_nodes(node_count, edges):
    """edges: list of (from, to, length) directed edges (already deduped from parallel OSM
    ways). Returns a new (conn) adjacency structure with degree-2 chains contracted.

    conn[node] = {neighbor: {"fwd": length|None, "rev": length|None}}, where "fwd" is the
    length of the edge node->neighbor and "rev" is neighbor->node, from node's perspective.
    """
    conn = [dict() for _ in range(node_count)]

    def set_edge(a, b, length):
        slot = conn[a].setdefault(b, {"fwd": None, "rev": None})
        if slot["fwd"] is None or length < slot["fwd"]:
            slot["fwd"] = length
        slot_b = conn[b].setdefault(a, {"fwd": None, "rev": None})
        if slot_b["rev"] is None or length < slot_b["rev"]:
            slot_b["rev"] = length

    for a, b, length in edges:
        if a == b:
            continue
        set_edge(a, b, length)

    removed = [False] * node_count
    worklist = [n for n in range(node_count) if len(conn[n]) == 2]
    in_worklist = set(worklist)

    while worklist:
        b = worklist.pop()
        in_worklist.discard(b)
        if removed[b] or len(conn[b]) != 2:
            continue
        (p, cp), (q, cq) = conn[b].items()

        # p -> b -> q (needs edge p->b, i.e. conn[b][p]["rev"], then b->q, conn[b][q]["fwd"])
        if cp["rev"] is not None and cq["fwd"] is not None:
            length = cp["rev"] + cq["fwd"]
            slot = conn[p].setdefault(q, {"fwd": None, "rev": None})
            if slot["fwd"] is None or length < slot["fwd"]:
                slot["fwd"] = length
            slot_q = conn[q].setdefault(p, {"fwd": None, "rev": None})
            if slot_q["rev"] is None or length < slot_q["rev"]:
                slot_q["rev"] = length

        # q -> b -> p (needs edge q->b, conn[b][q]["rev"], then b->p, conn[b][p]["fwd"])
        if cq["rev"] is not None and cp["fwd"] is not None:
            length = cq["rev"] + cp["fwd"]
            slot = conn[q].setdefault(p, {"fwd": None, "rev": None})
            if slot["fwd"] is None or length < slot["fwd"]:
                slot["fwd"] = length
            slot_p = conn[p].setdefault(q, {"fwd": None, "rev": None})
            if slot_p["rev"] is None or length < slot_p["rev"]:
                slot_p["rev"] = length

        del conn[p][b]
        del conn[q][b]
        conn[b].clear()
        removed[b] = True

        for n in (p, q):
            if not removed[n] and len(conn[n]) == 2 and n not in in_worklist:
                worklist.append(n)
                in_worklist.add(n)

    return conn, removed


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--bbox", required=True,
                         help="west,south,east,north")
    parser.add_argument("--out", required=True, help="output roads.json path")
    parser.add_argument("--extract-url", required=True,
                         help="one or more comma-separated Geofabrik .osm.pbf URLs covering "
                              "this bbox (e.g. several provinces) — scanned one at a time into "
                              "the same graph, so a huge country can be built as several "
                              "smaller regional extracts instead of one giant file")
    parser.add_argument("--highways", default=",".join(sorted(DRIVABLE_HIGHWAY)),
                         help="comma-separated OSM highway= values to keep — trim this for a "
                              "huge bbox (e.g. a whole big country) if the default backbone set "
                              "is still too many raw nodes/edges for the collapse pass to handle")
    args = parser.parse_args()

    bbox = tuple(float(x) for x in args.bbox.split(","))
    out_path = Path(args.out)
    drivable_highway = set(args.highways.split(","))
    extract_urls = [u.strip() for u in args.extract_url.split(",")]

    print(f"Keeping highway types: {sorted(drivable_highway)}", file=sys.stderr)
    handler = make_road_handler(bbox, drivable_highway)
    for extract_url in extract_urls:
        cache_path = CACHE_DIR / extract_url.rsplit("/", 1)[-1]
        ensure_extract(extract_url, cache_path)

        print(f"Scanning {cache_path.name} for drivable ways touching the bbox "
              "(single pass, can take a few minutes)...", file=sys.stderr)
        # handler.apply_file(path, locations=True) builds osmium's node-location index fully
        # in RAM — fine for a city-sized extract, but for a whole big country that index alone
        # is multiple GB before a single way gets filtered. A disk-backed (memory-mapped)
        # index trades some speed for a bounded memory footprint instead. One index per
        # extract file (node ids aren't shared across separate Geofabrik extracts).
        idx_path = cache_path.with_suffix(".nodecache.dat")
        location_index = osmium.index.create_map(f"sparse_file_array,{idx_path}")
        location_handler = osmium.NodeLocationsForWays(location_index)
        osmium.apply(osmium.io.Reader(str(cache_path)), location_handler, handler)
        del location_handler, location_index
        try:
            idx_path.unlink(missing_ok=True)
        except OSError:
            pass  # Windows keeps the mmap'd index file locked briefly after use — harmless
                   # leftover under .cache/, not worth failing the whole build over.

    node_lat, node_lon, raw_edges = handler.node_lat, handler.node_lon, handler.edges
    print(f"Scanned {handler.scanned} ways across {len(extract_urls)} extract(s). Built raw "
          f"graph: {len(node_lat)} nodes, {len(raw_edges)} edges. Collapsing degree-2 "
          f"pass-through nodes...", file=sys.stderr)

    conn, removed = collapse_pass_through_nodes(len(node_lat), raw_edges)

    # Renumber surviving nodes to a dense 0..M-1 range.
    old_to_new = {}
    out_lat, out_lon = [], []
    for old in range(len(node_lat)):
        if removed[old]:
            continue
        old_to_new[old] = len(out_lat)
        out_lat.append(node_lat[old])
        out_lon.append(node_lon[old])

    edge_from, edge_to, edge_length = [], [], []
    for old in range(len(node_lat)):
        if removed[old]:
            continue
        a = old_to_new[old]
        for neighbor, slot in conn[old].items():
            if removed[neighbor]:
                continue  # shouldn't happen — collapse clears removed nodes' conn entries
            b = old_to_new[neighbor]
            if slot["fwd"] is not None:
                edge_from.append(a)
                edge_to.append(b)
                edge_length.append(slot["fwd"])

    out = {
        "bbox": list(bbox),
        "nodeCount": len(out_lat),
        "edgeCount": len(edge_from),
        "nodes": {"lat": out_lat, "lon": out_lon},
        "edges": {"from": edge_from, "to": edge_to, "length": edge_length},
    }

    out_path.parent.mkdir(parents=True, exist_ok=True)
    with open(out_path, "w", encoding="utf-8") as f:
        json.dump(out, f)

    print(f"Wrote {out_path} - {out['nodeCount']} nodes (was {len(node_lat)}), "
          f"{out['edgeCount']} edges ({out_path.stat().st_size / 1024:.0f} KB)", file=sys.stderr)


if __name__ == "__main__":
    main()

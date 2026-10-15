#!/usr/bin/env node
"use strict";

// Build <area>/places.json — a flat, searchable place/POI index, decoded straight from that
// area's already-bundled .pmtiles vector tiles (the "pois" and "places" source-layers, per
// style.json). Read on-device by PlaceIndex.java.
//
// Usage: node build_places.js --pmtiles=<path> --bbox=west,south,east,north --zoom=<n> --out=<path>
// (zoom should be the deepest zoom that pmtiles archive was extracted to, for the fullest
// feature set — e.g. 14 for hanoi.pmtiles, 11 for vietnam.pmtiles.)

const fs = require("fs");
const path = require("path");
const { PMTiles } = require("pmtiles");
const { VectorTile } = require("@mapbox/vector-tile");
const Pbf = require("pbf");

function arg(name, fallback) {
  const prefix = `--${name}=`;
  const found = process.argv.find((a) => a.startsWith(prefix));
  return found ? found.slice(prefix.length) : fallback;
}

const ZOOM = parseInt(arg("zoom", "14"), 10);
const BBOX = arg("bbox", "105.6,20.85,106.05,21.25").split(",").map(Number);
const PMTILES_PATH = path.resolve(arg("pmtiles",
    path.resolve(__dirname, "../../app/src/main/assets/map/hanoi.pmtiles")));
const OUT_PATH = path.resolve(arg("out",
    path.resolve(__dirname, "../../app/src/main/assets/map/places.json")));
const SOURCE_LAYERS = ["pois", "places"];

// pmtiles' FileSource is browser-only (uses the File API) — a small Node fs-based Source.
class NodeFileSource {
  constructor(filePath) {
    this.filePath = filePath;
    this.fd = fs.openSync(filePath, "r");
  }
  getKey() {
    return this.filePath;
  }
  async getBytes(offset, length) {
    const buf = Buffer.alloc(length);
    fs.readSync(this.fd, buf, 0, length, offset);
    return { data: buf.buffer.slice(buf.byteOffset, buf.byteOffset + buf.byteLength) };
  }
}

function lonToTileX(lon, z) {
  return Math.floor(((lon + 180) / 360) * Math.pow(2, z));
}
function latToTileY(lat, z) {
  const rad = (lat * Math.PI) / 180;
  return Math.floor(
    ((1 - Math.log(Math.tan(rad) + 1 / Math.cos(rad)) / Math.PI) / 2) * Math.pow(2, z)
  );
}
function tileXToLon(x, z) {
  return (x / Math.pow(2, z)) * 360 - 180;
}
function tileYToLat(y, z) {
  const n = Math.PI - (2 * Math.PI * y) / Math.pow(2, z);
  return (180 / Math.PI) * Math.atan(0.5 * (Math.exp(n) - Math.exp(-n)));
}

async function main() {
  const source = new NodeFileSource(PMTILES_PATH);
  const pm = new PMTiles(source);
  const header = await pm.getHeader();
  console.error(`${PMTILES_PATH}: minZoom=${header.minZoom} maxZoom=${header.maxZoom}`);

  const [west, south, east, north] = BBOX;
  const xMin = lonToTileX(west, ZOOM);
  const xMax = lonToTileX(east, ZOOM);
  const yMin = latToTileY(north, ZOOM); // north has the smaller tile-y
  const yMax = latToTileY(south, ZOOM);

  console.error(`Scanning tiles z${ZOOM} x[${xMin}..${xMax}] y[${yMin}..${yMax}]...`);

  const places = [];
  let tilesWithData = 0;

  for (let x = xMin; x <= xMax; x++) {
    for (let y = yMin; y <= yMax; y++) {
      const resp = await pm.getZxy(ZOOM, x, y);
      if (!resp) continue;
      tilesWithData++;
      const tile = new VectorTile(new Pbf(resp.data));
      for (const layerName of SOURCE_LAYERS) {
        const layer = tile.layers[layerName];
        if (!layer) continue;
        for (let i = 0; i < layer.length; i++) {
          const feature = layer.feature(i);
          const props = feature.properties;
          const name = props.name;
          if (!name) continue;
          const geom = feature.loadGeometry(); // array of rings of {x,y} in tile-local coords
          if (!geom.length || !geom[0].length) continue;
          const [px, py] = [geom[0][0].x, geom[0][0].y];
          const fx = x + px / layer.extent;
          const fy = y + py / layer.extent;
          places.push({
            name: String(name),
            lat: tileYToLat(fy, ZOOM),
            lon: tileXToLon(fx, ZOOM),
            kind: String(props.kind || layerName),
          });
        }
      }
    }
  }

  console.error(`${tilesWithData} tiles had data, ${places.length} raw features collected.`);

  // Dedupe by name + coordinates rounded to ~4 decimals (~11m) — adjacent tiles can both carry
  // a feature near their shared boundary.
  const seen = new Set();
  const deduped = [];
  for (const p of places) {
    const key = `${p.name}|${p.lat.toFixed(4)}|${p.lon.toFixed(4)}`;
    if (seen.has(key)) continue;
    seen.add(key);
    deduped.push(p);
  }

  const out = { count: deduped.length, places: deduped };
  fs.writeFileSync(OUT_PATH, JSON.stringify(out));
  const sizeKb = (fs.statSync(OUT_PATH).size / 1024).toFixed(0);
  console.error(`Wrote ${OUT_PATH} - ${deduped.length} places (${sizeKb} KB)`);
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});

package com.plainphone.app;

/** One bundled offline map region — a pmtiles basemap + MapLibre style + the two
 *  {@code tools/nav} build outputs (place search index, driving-routing graph) for the same
 *  bbox. {@link #assetDir} points at wherever those four files live under {@code assets/}; Hanoi
 *  keeps its original flat {@code assets/map/} location (not moved into its own subfolder) since
 *  that's already in active use, but any area added after it lives at {@code map/<id>/}. */
final class MapArea {

    final String id;
    final String displayName;
    final String assetDir;
    final double centerLat;
    final double centerLon;
    final double defaultZoom;

    MapArea(String id, String displayName, String assetDir,
            double centerLat, double centerLon, double defaultZoom) {
        this.id = id;
        this.displayName = displayName;
        this.assetDir = assetDir;
        this.centerLat = centerLat;
        this.centerLon = centerLon;
        this.defaultZoom = defaultZoom;
    }

    String pmtilesAsset() { return assetDir + "/" + id + ".pmtiles"; }
    String styleAsset() { return assetDir + "/style.json"; }
    String placesAsset() { return assetDir + "/places.json"; }
    String roadsAsset() { return assetDir + "/roads.json"; }
}

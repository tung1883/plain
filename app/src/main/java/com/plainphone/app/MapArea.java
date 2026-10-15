package com.plainphone.app;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/** One offline map region — a pmtiles basemap + MapLibre style + the two {@code tools/nav}
 *  build outputs (place search index, driving-routing graph) for the same bbox. Areas are
 *  downloaded on demand by {@link AreaDownloader} into {@code filesDir/<assetDir>/...}. */
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

    /** Opens one of this area's files, from wherever {@link AreaDownloader} unzipped it under
     *  {@code filesDir}. */
    InputStream open(Context context, String relativePath) throws IOException {
        return new FileInputStream(new File(context.getFilesDir(), relativePath));
    }
}

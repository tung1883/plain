package com.plainphone.app;

import android.content.Context;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

/** The map areas shown together as one merged map — no picker, no switching. {@link MapPanel}
 *  layers every area {@link #all} returns into one basemap, {@link PlaceIndex} merges all their
 *  place lists into one search, and {@link RoadGraph} merges all their road graphs into one
 *  routing graph (offsetting node ids so they don't collide — see {@code RoadGraph.load}).
 *
 * <p>No area ships inside the APK: each one is downloaded on demand by
 * {@link AreaDownloader} (see {@link AreasPanel}, the Nav section's Areas tab) into
 * {@code filesDir/map/<id>/}, each carrying its own {@code meta.json} written at packaging time
 * — {@link #all} just scans for those on every call, so a freshly downloaded area shows up
 * automatically with no further code change.
 *
 * <p>Areas with no shared road data between them (e.g. two bboxes that don't touch) still won't
 * route across each other — that's a real limit of how the data was built, not a bug: there's
 * no connecting road data to route over. */
final class MapAreas {

    private MapAreas() {}

    /** Every downloaded area found under {@code filesDir/map/}; empty until one is added. */
    static List<MapArea> all(Context context) {
        List<MapArea> areas = new ArrayList<>();
        File mapDir = new File(context.getFilesDir(), "map");
        File[] subdirs = mapDir.listFiles();
        if (subdirs != null) {
            for (File dir : subdirs) {
                MapArea area = loadDownloaded(dir);
                if (area != null) areas.add(area);
            }
        }
        // lowest order first = drawn first = underneath; the first area also sets the camera
        areas.sort(java.util.Comparator.comparingInt((MapArea a) -> a.order).thenComparing(a -> a.id));
        return areas;
    }

    static boolean isDownloaded(Context context, String areaId) {
        return new File(new File(context.getFilesDir(), "map"), areaId + "/meta.json").isFile();
    }

    private static MapArea loadDownloaded(File dir) {
        File metaFile = new File(dir, "meta.json");
        if (!metaFile.isFile()) return null;
        try {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(metaFile))) {
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
            }
            JSONObject meta = new JSONObject(sb.toString());
            return new MapArea(meta.getString("id"), meta.getString("displayName"),
                    "map/" + dir.getName(), meta.getDouble("centerLat"),
                    meta.getDouble("centerLon"), meta.getDouble("defaultZoom"), meta.optInt("order", 0));
        } catch (Exception e) {
            return null; // corrupt/partial download — skip it rather than crash the merge
        }
    }
}

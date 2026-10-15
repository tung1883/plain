package com.plainphone.app;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Fetches the small JSON manifest of downloadable {@link MapArea}s — committed to the repo at
 *  {@code map-areas-catalog.json} and served over plain HTTPS via raw.githubusercontent.com, no
 *  backend needed. Each entry just names a {@link MapArea} and where to download its zip (a
 *  GitHub Release asset — see {@code tools/nav}'s packaging step and
 *  {@link AreaDownloader}). */
final class AreaCatalog {

    private AreaCatalog() {}

    // The main branch, so the catalog tracks whatever is actually shipping.
    private static final String CATALOG_URL =
            "https://raw.githubusercontent.com/tung1883/plain/main/map-areas-catalog.json";

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    interface OnLoaded {
        void onLoaded(List<Entry> entries); // null if the fetch failed
    }

    static final class Entry {
        final String id;
        final String displayName;
        final int sizeMb;
        final String zipUrl;

        Entry(String id, String displayName, int sizeMb, String zipUrl) {
            this.id = id;
            this.displayName = displayName;
            this.sizeMb = sizeMb;
            this.zipUrl = zipUrl;
        }
    }

    static void fetch(OnLoaded callback) {
        new Thread(() -> {
            List<Entry> entries = fetchSync();
            MAIN.post(() -> callback.onLoaded(entries));
        }, "AreaCatalog-fetch").start();
    }

    private static List<Entry> fetchSync() {
        try {
            URL url = new URL(CATALOG_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(10_000);
            try (InputStream in = conn.getInputStream()) {
                String json = readAll(in);
                JSONArray array = new JSONObject(json).getJSONArray("areas");
                List<Entry> entries = new ArrayList<>();
                for (int i = 0; i < array.length(); i++) {
                    JSONObject o = array.getJSONObject(i);
                    entries.add(new Entry(o.getString("id"), o.getString("displayName"),
                            o.getInt("sizeMb"), o.getString("zipUrl")));
                }
                return entries;
            } finally {
                conn.disconnect();
            }
        } catch (IOException | org.json.JSONException e) {
            return null;
        }
    }

    private static String readAll(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        return sb.toString();
    }
}

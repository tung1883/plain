package com.plainphone.app;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Lazy, process-wide index of {@code assets/map/places.json} (place names + coordinates,
 *  decoded at build time from the bundled Hanoi basemap's vector tiles — see
 *  {@code tools/nav/build_places.js}). Loaded once on a background thread since the asset is
 *  too large (thousands of rows) to parse on the UI thread without jank; a plain {@link Thread}
 *  is enough here — this is a bounded, fast, non-retryable one-shot parse, not the kind of
 *  resumable/retryable work {@link FileIndex} uses {@link JobQueue} for. */
final class PlaceIndex {

    private PlaceIndex() {}

    interface OnLoaded {
        void onLoaded();
    }

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private static volatile List<Place> places;
    private static volatile boolean loading;

    static boolean isLoaded() {
        return places != null;
    }

    /** No-op if already loaded or a load is already in flight. */
    static void ensureLoaded(Context context, OnLoaded callback) {
        if (places != null) {
            if (callback != null) callback.onLoaded();
            return;
        }
        synchronized (PlaceIndex.class) {
            if (loading) return;
            loading = true;
        }
        Context appContext = context.getApplicationContext();
        new Thread(() -> {
            List<Place> loaded = load(appContext);
            places = loaded;
            loading = false;
            if (callback != null) MAIN.post(callback::onLoaded);
        }, "PlaceIndex-load").start();
    }

    /** Empty before the index has loaded — callers doing a first search right after launch
     *  should use {@link #ensureLoaded} and retry from its callback. */
    static List<Place> search(TextMatch.Query query, int limit) {
        List<Place> snapshot = places;
        List<Place> hits = new ArrayList<>();
        if (snapshot == null || query.empty) return hits;

        List<Scored> scored = new ArrayList<>();
        for (Place place : snapshot) {
            int score = TextMatch.score(place.folded, place.signature, query);
            if (score != TextMatch.NO_MATCH) scored.add(new Scored(place, score));
        }
        Collections.sort(scored, new Comparator<Scored>() {
            @Override
            public int compare(Scored a, Scored b) {
                if (a.score != b.score) return Integer.compare(a.score, b.score);
                return Integer.compare(a.place.name.length(), b.place.name.length());
            }
        });
        for (int i = 0; i < scored.size() && i < limit; i++) hits.add(scored.get(i).place);
        return hits;
    }

    private static final class Scored {
        final Place place;
        final int score;

        Scored(Place place, int score) {
            this.place = place;
            this.score = score;
        }
    }

    private static List<Place> load(Context context) {
        List<Place> result = new ArrayList<>();
        try (InputStream in = context.getAssets().open("map/places.json")) {
            String json = readAll(in);
            JSONArray array = new JSONObject(json).getJSONArray("places");
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.getJSONObject(i);
                result.add(new Place(o.getString("name"), o.getDouble("lat"), o.getDouble("lon"),
                        o.optString("kind", "")));
            }
        } catch (Exception e) {
            // Leave result as whatever was parsed before the failure (likely empty) — search()
            // treats a non-null-but-short list the same as a normal, if sparse, index.
        }
        return result;
    }

    private static String readAll(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        return sb.toString();
    }
}

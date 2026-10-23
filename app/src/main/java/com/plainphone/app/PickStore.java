package com.plainphone.app;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Keeps the {@link PickHistory} in a file under the app's private storage. */
final class PickStore {

    private static final String FILE = "search_history.json";
    private static final Object LOCK = new Object();
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private static PickHistory history;

    private PickStore() {}

    private static PickHistory get(Context context) {
        synchronized (LOCK) {
            if (history == null) {
                history = PickHistory.fromJson(read(new File(context.getApplicationContext().getFilesDir(), FILE)),
                        System::currentTimeMillis);
            }
            return history;
        }
    }

    /** Remember that the user picked {@code result} while searching for {@code foldedQuery}. */
    static void record(Context context, SearchResult result, String foldedQuery) {
        String key = ResultKey.of(result);
        if (key == null || foldedQuery == null || foldedQuery.isEmpty()) return;
        final Context app = context.getApplicationContext();
        synchronized (LOCK) {
            get(app).record(key, foldedQuery);
        }
        IO.execute(() -> save(app));
    }

    /** Lower the scores of results the user tends to pick for this query (idempotent per render). */
    static void apply(Context context, List<SearchResult> results, String foldedQuery) {
        if (results.isEmpty() || foldedQuery == null || foldedQuery.isEmpty()) return;
        synchronized (LOCK) {
            PickHistory h = get(context);
            for (SearchResult r : results) {
                String key = ResultKey.of(r);
                r.rank(key == null ? r.baseScore : PickHistory.adjust(r.baseScore, h.boost(key, foldedQuery)));
            }
        }
    }

    static void clear(Context context) {
        final Context app = context.getApplicationContext();
        synchronized (LOCK) {
            get(app).clear();
        }
        IO.execute(() -> save(app));
    }

    private static void save(Context app) {
        String json;
        synchronized (LOCK) {
            json = get(app).toJson();
        }
        File target = new File(app.getFilesDir(), FILE);
        File tmp = new File(app.getFilesDir(), FILE + ".tmp");
        try (FileOutputStream out = new FileOutputStream(tmp)) {
            out.write(json.getBytes(StandardCharsets.UTF_8));
            out.getFD().sync();
        } catch (IOException e) {
            tmp.delete();
            return;
        }
        if (!tmp.renameTo(target)) tmp.delete();
    }

    private static String read(File f) {
        if (!f.exists()) return "";
        try (java.io.FileInputStream in = new java.io.FileInputStream(f)) {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }
}

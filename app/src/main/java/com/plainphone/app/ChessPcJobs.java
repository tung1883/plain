package com.plainphone.app;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/** The phone's record of the one puzzle-generation job running on a paired computer. The job
 *  itself lives in plaind; this is just enough to find it again after the app was killed, the
 *  phone locked or the link dropped: which job, which computer, and the last result sequence
 *  number this phone has stored (so a reconnect replays only what's missing).
 *
 *  <p>Kept apart from {@link JobQueue}: nothing here runs on the phone, so there is no worker,
 *  no foreground job service and nothing to park behind a lock. */
final class ChessPcJobs {

    private ChessPcJobs() {}

    static final String STATE_STARTING = "starting"; // created locally, not yet confirmed by the PC
    static final String STATE_RUNNING = "running";
    static final String STATE_DONE = "done";
    static final String STATE_CANCELLED = "cancelled";
    static final String STATE_FAILED = "failed";

    static final class Record {
        String jobId;
        String hostId;
        String hostLabel;
        String scope = Config.CHESS_PUZZLEGEN_SCOPE_ALL;
        int total;
        int workers;   // 0 = daemon default
        int hashMb;    // 0 = daemon default
        /** Generator params sent with {@code chess.job.start} (numbers/booleans only). */
        Map<String, Object> params = new LinkedHashMap<>();
        String state = STATE_STARTING;
        String error;
        /** Highest result sequence number stored (and acked) on this phone. */
        long lastSeq;
        int scanned, found, dropped;
        int etaSeconds = -1;
        boolean cancelRequested;
        long startedAt;

        /** Still working on the PC (or about to be). */
        boolean running() { return STATE_STARTING.equals(state) || STATE_RUNNING.equals(state); }
    }

    private static final String PREFS = "chess_pc_job";
    private static final String KEY = "record";

    private static Record cached;
    private static boolean loaded;

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** The current record or null. Never blocks on anything but a prefs read, first time only. */
    static synchronized Record current(Context context) {
        if (!loaded) {
            cached = parse(prefs(context).getString(KEY, null));
            loaded = true;
        }
        return cached;
    }

    static boolean running(Context context) {
        Record r = current(context);
        return r != null && r.running();
    }

    static synchronized void save(Context context, Record r) {
        cached = r;
        loaded = true;
        prefs(context).edit().putString(KEY, r == null ? null : toJson(r)).apply();
    }

    static synchronized void clear(Context context) {
        save(context, null);
    }

    // --- (de)serialisation -----------------------------------------------------------

    static String toJson(Record r) {
        try {
            JSONObject o = new JSONObject();
            o.put("job", r.jobId);
            o.put("host", r.hostId);
            o.put("hostLabel", r.hostLabel);
            o.put("scope", r.scope);
            o.put("total", r.total);
            o.put("workers", r.workers);
            o.put("hashMb", r.hashMb);
            JSONObject p = new JSONObject();
            for (Map.Entry<String, Object> e : r.params.entrySet()) p.put(e.getKey(), e.getValue());
            o.put("params", p);
            o.put("state", r.state);
            if (r.error != null) o.put("error", r.error);
            o.put("lastSeq", r.lastSeq);
            o.put("scanned", r.scanned);
            o.put("found", r.found);
            o.put("dropped", r.dropped);
            o.put("eta", r.etaSeconds);
            o.put("cancel", r.cancelRequested);
            o.put("startedAt", r.startedAt);
            return o.toString();
        } catch (JSONException e) {
            return null;
        }
    }

    static Record parse(String json) {
        if (json == null) return null;
        try {
            JSONObject o = new JSONObject(json);
            Record r = new Record();
            r.jobId = o.getString("job");
            r.hostId = o.getString("host");
            r.hostLabel = o.optString("hostLabel", "PC");
            r.scope = o.optString("scope", Config.CHESS_PUZZLEGEN_SCOPE_ALL);
            r.total = o.optInt("total");
            r.workers = o.optInt("workers");
            r.hashMb = o.optInt("hashMb");
            JSONObject p = o.optJSONObject("params");
            if (p != null) {
                for (Iterator<String> it = p.keys(); it.hasNext(); ) {
                    String k = it.next();
                    r.params.put(k, p.get(k));
                }
            }
            r.state = o.optString("state", STATE_STARTING);
            r.error = o.has("error") ? o.getString("error") : null;
            r.lastSeq = o.optLong("lastSeq");
            r.scanned = o.optInt("scanned");
            r.found = o.optInt("found");
            r.dropped = o.optInt("dropped");
            r.etaSeconds = o.optInt("eta", -1);
            r.cancelRequested = o.optBoolean("cancel");
            r.startedAt = o.optLong("startedAt");
            return r;
        } catch (JSONException e) {
            return null;
        }
    }
}

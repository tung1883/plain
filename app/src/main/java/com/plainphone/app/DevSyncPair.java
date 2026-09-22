package com.plainphone.app;

import android.content.Context;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * One configured folder pair for a {@link DevHost}: a local SAF tree, a
 * remote absolute path on the daemon's filesystem, and how they should be
 * kept in sync. Persistence mirrors {@link DevHost} — a flat JSON array in
 * {@link Config#getDevSyncPairsJson}, each entry carrying its own
 * {@code host_id} rather than living nested inside the host's own record.
 */
final class DevSyncPair {

    static final String DIR_PUSH = "push";     // phone -> PC
    static final String DIR_PULL = "pull";     // PC -> phone
    static final String DIR_MIRROR = "mirror"; // both ways

    static final String DETECT_MTIME = "mtime";
    static final String DETECT_SIZE = "size";
    static final String DETECT_CHECKSUM = "checksum";

    static final String DELETE_OFF = "off";
    static final String DELETE_PROPAGATE = "propagate";

    static final int SCHEDULE_MANUAL = 0; // minutes; 0 = manual only

    final String id;
    String hostId;
    String label;
    String localTreeUri; // SAF tree, as a Uri string
    String remotePath;   // absolute path on the daemon's filesystem
    String direction = DIR_PUSH;
    String detectMode = DETECT_MTIME;
    String deleteMode = DELETE_OFF; // only meaningful for DIR_MIRROR
    int scheduleMinutes = SCHEDULE_MANUAL;

    // Last-run summary, for the pair list row.
    long lastRunAt;
    int lastSynced, lastFailed, lastConflicts;

    DevSyncPair(String id) {
        this.id = id;
    }

    Uri localTree() {
        return localTreeUri == null ? null : Uri.parse(localTreeUri);
    }

    boolean isMirror() {
        return DIR_MIRROR.equals(direction);
    }

    boolean deletesPropagate() {
        return isMirror() && DELETE_PROPAGATE.equals(deleteMode);
    }

    boolean needsHash() {
        return DETECT_CHECKSUM.equals(detectMode);
    }

    // --- persistence -----------------------------------------------------

    static List<DevSyncPair> all(Context context) {
        List<DevSyncPair> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(Config.getDevSyncPairsJson(context));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                DevSyncPair p = new DevSyncPair(o.getString("id"));
                p.hostId = o.optString("host_id", null);
                p.label = o.optString("label", "");
                p.localTreeUri = o.optString("local_tree", null);
                p.remotePath = o.optString("remote_path", null);
                p.direction = o.optString("direction", DIR_PUSH);
                p.detectMode = o.optString("detect_mode", DETECT_MTIME);
                p.deleteMode = o.optString("delete_mode", DELETE_OFF);
                p.scheduleMinutes = o.optInt("schedule_minutes", SCHEDULE_MANUAL);
                p.lastRunAt = o.optLong("last_run_at", 0);
                p.lastSynced = o.optInt("last_synced", 0);
                p.lastFailed = o.optInt("last_failed", 0);
                p.lastConflicts = o.optInt("last_conflicts", 0);
                out.add(p);
            }
        } catch (JSONException ignored) {
        }
        return out;
    }

    static List<DevSyncPair> forHost(Context context, String hostId) {
        List<DevSyncPair> out = new ArrayList<>();
        for (DevSyncPair p : all(context)) {
            if (p.hostId != null && p.hostId.equals(hostId)) out.add(p);
        }
        return out;
    }

    static DevSyncPair find(Context context, String id) {
        if (id == null) return null;
        for (DevSyncPair p : all(context)) {
            if (p.id.equals(id)) return p;
        }
        return null;
    }

    void save(Context context) {
        List<DevSyncPair> pairs = all(context);
        boolean replaced = false;
        for (int i = 0; i < pairs.size(); i++) {
            if (pairs.get(i).id.equals(id)) {
                pairs.set(i, this);
                replaced = true;
                break;
            }
        }
        if (!replaced) pairs.add(this);
        writeAll(context, pairs);
    }

    static void remove(Context context, String id) {
        List<DevSyncPair> pairs = all(context);
        pairs.removeIf(p -> p.id.equals(id));
        writeAll(context, pairs);
        DevSyncJobs.clearBaseline(context, id);
    }

    private static void writeAll(Context context, List<DevSyncPair> pairs) {
        JSONArray arr = new JSONArray();
        try {
            for (DevSyncPair p : pairs) {
                JSONObject o = new JSONObject();
                o.put("id", p.id);
                o.put("host_id", p.hostId);
                o.put("label", p.label);
                o.put("local_tree", p.localTreeUri);
                o.put("remote_path", p.remotePath);
                o.put("direction", p.direction);
                o.put("detect_mode", p.detectMode);
                o.put("delete_mode", p.deleteMode);
                o.put("schedule_minutes", p.scheduleMinutes);
                o.put("last_run_at", p.lastRunAt);
                o.put("last_synced", p.lastSynced);
                o.put("last_failed", p.lastFailed);
                o.put("last_conflicts", p.lastConflicts);
                arr.put(o);
            }
        } catch (JSONException ignored) {
        }
        Config.setDevSyncPairsJson(context, arr.toString());
    }

    static String newId() {
        return Long.toString(System.currentTimeMillis(), 36)
                + Integer.toString((int) (Math.random() * 1296), 36);
    }
}

package com.plainphone.app;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * One voice memo. A local recording keeps its audio in
 * {@code filesDir/recordings/<id>.<format>}; a vaulted one is transient with
 * {@code id = "vault:" + docId} (mirrors {@link Note#forVault}).
 *
 * <p>{@code envelope} is a comma-joined list of ~200 amplitude peaks (0–100) so
 * the player can draw the whole waveform without decoding the audio.
 */
class Recording {

    final String id;
    String name;
    String format;        // "wav" / "m4a" / "3gp"
    int sampleRate;
    long durationMs;
    long createdAt;
    String envelope;      // "12,40,7,..." or "" when unknown

    private Recording(String id) {
        this.id = id;
        this.name = "";
        this.format = "m4a";
        this.sampleRate = 44100;
        this.createdAt = System.currentTimeMillis();
        this.envelope = "";
    }

    static Recording create(String name, String format, int sampleRate,
                            long durationMs, String envelope) {
        Recording r = new Recording(UUID.randomUUID().toString());
        r.name = name;
        r.format = format;
        r.sampleRate = sampleRate;
        r.durationMs = durationMs;
        r.envelope = envelope == null ? "" : envelope;
        return r;
    }

    /** A transient recording backed by a vault file — id is {@code "vault:" + docId}. */
    static Recording forVault(String id, String name, String format, long mtime) {
        Recording r = new Recording(id);
        r.name = name;
        r.format = format;
        r.createdAt = mtime;
        return r;
    }

    static Recording findById(List<Recording> list, String id) {
        if (id == null) return null;
        for (Recording r : list) if (r.id.equals(id)) return r;
        return null;
    }

    String displayName() {
        String n = name == null ? "" : name.trim();
        return n.isEmpty() ? "Recording" : n;
    }

    String subtitle() {
        return durationLabel() + " · " + relative(createdAt);
    }

    String durationLabel() {
        long totalSec = Math.max(0, durationMs) / 1000L;
        return (totalSec / 60) + ":" + String.format(Locale.US, "%02d", totalSec % 60);
    }

    int[] envelopePeaks() {
        return peaksFrom(envelope);
    }

    static int[] peaksFrom(String envelope) {
        if (envelope == null || envelope.isEmpty()) return new int[0];
        String[] parts = envelope.split(",");
        int[] out = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                out[i] = Math.max(0, Math.min(100, Integer.parseInt(parts[i].trim())));
            } catch (NumberFormatException e) {
                out[i] = 0;
            }
        }
        return out;
    }

    static String peaksToString(int[] peaks) {
        StringBuilder sb = new StringBuilder();
        for (int p : peaks) {
            if (sb.length() > 0) sb.append(',');
            sb.append(Math.max(0, Math.min(100, p)));
        }
        return sb.toString();
    }

    private static String relative(long millis) {
        long diff = System.currentTimeMillis() - millis;
        long minute = 60_000L, hour = 60 * minute, day = 24 * hour;
        if (diff < 2 * minute) return "just now";
        if (diff < hour) return (diff / minute) + " min ago";
        if (diff < day) return (diff / hour) + " h ago";
        if (diff < 7 * day) return (diff / day) + " d ago";
        return new SimpleDateFormat("MMM d", Locale.US).format(millis);
    }

    JSONObject toJson() {
        try {
            JSONObject obj = new JSONObject();
            obj.put("id", id);
            obj.put("name", name);
            obj.put("format", format);
            obj.put("sampleRate", sampleRate);
            obj.put("durationMs", durationMs);
            obj.put("createdAt", createdAt);
            obj.put("envelope", envelope);
            return obj;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    static Recording fromJson(JSONObject obj) throws JSONException {
        Recording r = new Recording(obj.getString("id"));
        r.name = obj.optString("name", "");
        r.format = obj.optString("format", "m4a");
        r.sampleRate = obj.optInt("sampleRate", 44100);
        r.durationMs = obj.optLong("durationMs", 0);
        r.createdAt = obj.optLong("createdAt", System.currentTimeMillis());
        r.envelope = obj.optString("envelope", "");
        return r;
    }
}

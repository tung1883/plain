package com.plainphone.app;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

class Note {

    private static final int TITLE_CAP = 80;
    private static final int PREVIEW_CAP = 120;

    final String id;
    String title;
    String text;
    long createdAt;
    long updatedAt;

    private Note(String id) {
        this.id = id;
        this.title = "";
        this.text = "";
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
    }

    static Note create() {
        return new Note(UUID.randomUUID().toString());
    }

    static Note findById(List<Note> notes, String id) {
        if (id == null) return null;
        for (Note note : notes) {
            if (note.id.equals(id)) return note;
        }
        return null;
    }

    boolean isBlank() {
        return title.trim().isEmpty() && text.trim().isEmpty();
    }

    String title() {
        String explicit = title.trim();
        if (!explicit.isEmpty()) {
            return explicit.length() > TITLE_CAP ? explicit.substring(0, TITLE_CAP) : explicit;
        }
        for (String line : text.split("\n", -1)) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                return trimmed.length() > TITLE_CAP ? trimmed.substring(0, TITLE_CAP) : trimmed;
            }
        }
        return "New note";
    }

    String preview() {
        StringBuilder flat = new StringBuilder();
        for (String line : text.split("\n", -1)) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
            if (flat.length() > 0) flat.append(' ');
            flat.append(trimmed);
            if (flat.length() >= PREVIEW_CAP) break;
        }
        if (flat.length() == 0) return null;
        return flat.length() > PREVIEW_CAP ? flat.substring(0, PREVIEW_CAP) : flat.toString();
    }

    String editedLabel() {
        return "edited " + relative(updatedAt);
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
            obj.put("title", title);
            obj.put("text", text);
            obj.put("createdAt", createdAt);
            obj.put("updatedAt", updatedAt);
            return obj;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    static Note fromJson(JSONObject obj) throws JSONException {
        Note note = new Note(obj.getString("id"));
        note.title = obj.optString("title", "");
        note.text = obj.optString("text", "");
        long now = System.currentTimeMillis();
        note.createdAt = obj.optLong("createdAt", now);
        note.updatedAt = obj.optLong("updatedAt", note.createdAt);
        return note;
    }
}

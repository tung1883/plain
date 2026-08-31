package com.plainphone.app;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.UUID;

class Note {

    private static final int TITLE_CAP = 80;
    private static final int PREVIEW_CAP = 120;

    final String id;
    String text;
    long createdAt;
    long updatedAt;

    private Note(String id) {
        this.id = id;
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

    String title() {
        for (String line : text.split("\n", -1)) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                return trimmed.length() > TITLE_CAP ? trimmed.substring(0, TITLE_CAP) : trimmed;
            }
        }
        return "New note";
    }

    String preview() {
        boolean pastTitle = false;
        StringBuilder rest = new StringBuilder();
        for (String line : text.split("\n", -1)) {
            String trimmed = line.trim();
            if (!pastTitle) {
                if (!trimmed.isEmpty()) pastTitle = true;
                continue;
            }
            if (trimmed.isEmpty()) continue;
            if (rest.length() > 0) rest.append(' ');
            rest.append(trimmed);
            if (rest.length() >= PREVIEW_CAP) break;
        }
        if (rest.length() == 0) return null;
        return rest.length() > PREVIEW_CAP ? rest.substring(0, PREVIEW_CAP) : rest.toString();
    }

    JSONObject toJson() {
        try {
            JSONObject obj = new JSONObject();
            obj.put("id", id);
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
        note.text = obj.optString("text", "");
        long now = System.currentTimeMillis();
        note.createdAt = obj.optLong("createdAt", now);
        note.updatedAt = obj.optLong("updatedAt", note.createdAt);
        return note;
    }
}

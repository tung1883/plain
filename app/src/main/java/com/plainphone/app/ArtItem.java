package com.plainphone.app;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

/** One image / GIF in the home-art gallery, with its own crop and colour setting. */
final class ArtItem {

    String id;
    String kind;        // "photo" | "gif"
    String file;        // filename under ArtGallery.dir()
    String name;        // display name for the settings row
    float fx = 0.5f, fy = 0.5f, zoom = 1f;
    boolean gray = true;

    boolean isGif() {
        return "gif".equals(kind);
    }

    File file(Context context) {
        return new File(ArtGallery.dir(context), file);
    }

    JSONObject toJson() {
        JSONObject o = new JSONObject();
        try {
            o.put("id", id);
            o.put("kind", kind);
            o.put("file", file);
            o.put("name", name);
            o.put("fx", fx);
            o.put("fy", fy);
            o.put("zoom", zoom);
            o.put("gray", gray);
        } catch (JSONException ignored) {
        }
        return o;
    }

    static ArtItem fromJson(JSONObject o) {
        ArtItem it = new ArtItem();
        it.id = o.optString("id");
        it.kind = o.optString("kind", "photo");
        it.file = o.optString("file");
        it.name = o.optString("name", it.file);
        it.fx = (float) o.optDouble("fx", 0.5);
        it.fy = (float) o.optDouble("fy", 0.5);
        it.zoom = (float) o.optDouble("zoom", 1.0);
        it.gray = o.optBoolean("gray", true);
        return it;
    }
}

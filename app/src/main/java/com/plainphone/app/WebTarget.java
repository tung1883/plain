package com.plainphone.app;

import android.net.Uri;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.UUID;

class WebTarget {

    static final String WORD = "{text}";

    final String id;
    String name;
    String url;

    private WebTarget(String id, String name, String url) {
        this.id = id;
        this.name = name;
        this.url = url;
    }

    static WebTarget create(String name, String url) {
        return new WebTarget(UUID.randomUUID().toString(), name, url);
    }

    static WebTarget findById(List<WebTarget> targets, String id) {
        if (id == null) return null;
        for (WebTarget target : targets) {
            if (target.id.equals(id)) return target;
        }
        return null;
    }

    static boolean hasPlaceholder(String url) {
        return url != null && url.contains(WORD);
    }

    String urlFor(String query) {
        String encoded = Uri.encode(query);
        return url.replace(WORD, encoded);
    }

    static String nameOrHost(String name, String url) {
        if (name != null && !name.trim().isEmpty()) return name.trim();
        try {
            String host = Uri.parse(url).getHost();
            if (host != null) return host;
        } catch (Exception ignored) {
        }
        return url;
    }

    JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("name", name);
        json.put("url", url);
        return json;
    }

    static WebTarget fromJson(JSONObject json) throws JSONException {
        return new WebTarget(
                json.optString("id", UUID.randomUUID().toString()),
                json.optString("name", ""),
                json.getString("url"));
    }
}


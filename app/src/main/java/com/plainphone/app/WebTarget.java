package com.plainphone.app;

import android.net.Uri;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.UUID;

/**
 * A user-defined web search: a name and a URL with a placeholder where the query goes,
 * e.g. "https://de.wiktionary.org/wiki/{word}". Each one becomes its own row under the Web
 * heading, so a single typed word can be sent straight to a dictionary, a wiki, or a
 * translator without opening the site first and searching there.
 */
class WebTarget {

    /** Placeholder for the query in a URL template. */
    static final String WORD = "{word}";
    /** Also accepted, since printf-style templates are the other common convention. */
    private static final String LEGACY_WORD = "%s";

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

    /** True once the template says where the query belongs — without that it's just a link. */
    static boolean hasPlaceholder(String url) {
        return url != null && (url.contains(WORD) || url.contains(LEGACY_WORD));
    }

    /**
     * The address to open for a query. The query is percent-encoded, so spaces and accented
     * characters survive the trip — "Sài Gòn" has to reach the site intact rather than
     * truncating the URL at the first space.
     */
    String urlFor(String query) {
        String encoded = Uri.encode(query);
        return url.replace(WORD, encoded).replace(LEGACY_WORD, encoded);
    }

    /** Falls back to the URL's host when no name was given, which is usually recognisable. */
    static String nameOrHost(String name, String url) {
        if (name != null && !name.trim().isEmpty()) return name.trim();
        try {
            String host = Uri.parse(url).getHost();
            if (host != null) return host;
        } catch (Exception ignored) {
            // Not parseable as a Uri yet — the raw text is still a usable label.
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

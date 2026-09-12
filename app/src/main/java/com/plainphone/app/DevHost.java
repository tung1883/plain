package com.plainphone.app;

import android.content.Context;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * One paired device. The pairing token is a secret and never lives here — it
 * is stored under {@link SecretStore} keyed by {@link #id} and fetched only when
 * a connection is opened. Everything else is plain config, a JSON array in
 * {@link Config#getDevHostsJson}.
 */
final class DevHost {

    static final String CLIP_AUTO = "auto";     // synced automatically both ways
    static final String CLIP_MANUAL = "manual"; // only via an explicit button
    static final String CLIP_OFF = "off";

    final String id;
    String label;
    String host;
    int port;
    String clipMode = CLIP_AUTO;

    DevHost(String id, String label, String host, int port) {
        this.id = id;
        this.label = label;
        this.host = host;
        this.port = port;
    }

    String address() {
        return host + ":" + port;
    }

    /** Parse a {@code plaind://host:port/<token>} pairing link. Returns null if malformed. */
    static Parsed parseLink(String link) {
        if (link == null) return null;
        String trimmed = link.trim();
        if (!trimmed.startsWith("plaind://")) return null;
        Uri uri = Uri.parse(trimmed);
        String host = uri.getHost();
        int port = uri.getPort();
        if (port <= 0) port = DevProtocol.DEFAULT_PORT;
        String path = uri.getPath();
        String token = path == null ? null : path.replaceFirst("^/", "");
        if (host == null || host.isEmpty() || token == null || token.isEmpty()) return null;
        return new Parsed(host, port, token);
    }

    static final class Parsed {
        final String host;
        final int port;
        final String token;

        Parsed(String host, int port, String token) {
            this.host = host;
            this.port = port;
            this.token = token;
        }
    }

    // --- persistence -------------------------------------------------------

    static List<DevHost> all(Context context) {
        List<DevHost> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(Config.getDevHostsJson(context));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                DevHost h = new DevHost(o.getString("id"), o.getString("label"),
                        o.getString("host"), o.optInt("port", DevProtocol.DEFAULT_PORT));
                if (o.has("clip_mode")) {
                    h.clipMode = o.optString("clip_mode", CLIP_AUTO);
                } else {
                    // migrate the old boolean toggle
                    h.clipMode = o.optBoolean("clip_sync", true) ? CLIP_AUTO : CLIP_OFF;
                }
                out.add(h);
            }
        } catch (JSONException ignored) {
        }
        return out;
    }

    static DevHost find(Context context, String id) {
        if (id == null) return null;
        for (DevHost h : all(context)) {
            if (h.id.equals(id)) return h;
        }
        return null;
    }

    /** Add or replace this host and persist its token. */
    void save(Context context, String token) {
        List<DevHost> hosts = all(context);
        boolean replaced = false;
        for (int i = 0; i < hosts.size(); i++) {
            if (hosts.get(i).id.equals(id)) {
                hosts.set(i, this);
                replaced = true;
                break;
            }
        }
        if (!replaced) hosts.add(this);
        writeAll(context, hosts);
        if (token != null) SecretStore.put(context, tokenKey(), token);
    }

    static void remove(Context context, String id) {
        List<DevHost> hosts = all(context);
        hosts.removeIf(h -> h.id.equals(id));
        writeAll(context, hosts);
        SecretStore.remove(context, "dev_host_" + id);
    }

    String token(Context context) {
        return SecretStore.get(context, tokenKey());
    }

    private String tokenKey() {
        return "dev_host_" + id;
    }

    private static void writeAll(Context context, List<DevHost> hosts) {
        JSONArray arr = new JSONArray();
        try {
            for (DevHost h : hosts) {
                JSONObject o = new JSONObject();
                o.put("id", h.id);
                o.put("label", h.label);
                o.put("host", h.host);
                o.put("port", h.port);
                o.put("clip_mode", h.clipMode);
                arr.put(o);
            }
        } catch (JSONException ignored) {
        }
        Config.setDevHostsJson(context, arr.toString());
    }

    static String newId() {
        return Long.toString(System.currentTimeMillis(), 36)
                + Integer.toString((int) (Math.random() * 1296), 36);
    }
}

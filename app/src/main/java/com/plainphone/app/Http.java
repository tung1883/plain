package com.plainphone.app;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HttpsURLConnection;

/**
 * A tiny zero-dependency HTTPS client for the Dev-plugin service panels
 * (GitHub / Vercel / Supabase). {@link HttpsURLConnection} + {@code org.json},
 * nothing else. Blocking — always call from {@link NetIo#POOL}, never the main
 * thread.
 *
 * <p>Every call takes an {@code allowHosts} list: the bearer token is attached
 * only when the URL host matches one of them, so a malformed account base can
 * never send the token somewhere else.
 */
final class Http {

    private Http() {}

    private static final int CONNECT_TIMEOUT_MS = 8000;
    private static final int READ_TIMEOUT_MS = 12000;

    /** A non-2xx response. {@link #body} is the (possibly empty) error payload. */
    static final class HttpException extends IOException {
        final int status;
        final String body;
        final Map<String, List<String>> headers;

        HttpException(int status, String body, Map<String, List<String>> headers) {
            super("HTTP " + status);
            this.status = status;
            this.body = body == null ? "" : body;
            this.headers = headers;
        }
    }

    /** The bytes + headers of a 2xx response. */
    static final class Response {
        final String body;
        final Map<String, List<String>> headers;

        Response(String body, Map<String, List<String>> headers) {
            this.body = body;
            this.headers = headers;
        }

        JSONObject asObject() throws JSONException {
            return new JSONObject(new JSONTokener(body));
        }

        JSONArray asArray() throws JSONException {
            return new JSONArray(new JSONTokener(body));
        }

        /** First value of a response header, case-insensitive; null if absent. */
        String header(String name) {
            for (Map.Entry<String, List<String>> e : headers.entrySet()) {
                if (e.getKey() != null && e.getKey().equalsIgnoreCase(name)
                        && e.getValue() != null && !e.getValue().isEmpty()) {
                    return e.getValue().get(0);
                }
            }
            return null;
        }
    }

    static JSONObject getObject(String url, String bearer, String[] allowHosts,
                                Map<String, String> headers) throws IOException, JSONException {
        return request("GET", url, bearer, allowHosts, null, headers).asObject();
    }

    static JSONArray getArray(String url, String bearer, String[] allowHosts,
                              Map<String, String> headers) throws IOException, JSONException {
        return request("GET", url, bearer, allowHosts, null, headers).asArray();
    }

    static Response get(String url, String bearer, String[] allowHosts,
                        Map<String, String> headers) throws IOException {
        return request("GET", url, bearer, allowHosts, null, headers);
    }

    static JSONObject postObject(String url, String bearer, String[] allowHosts,
                                 JSONObject body, Map<String, String> headers)
            throws IOException, JSONException {
        byte[] payload = body == null ? null : body.toString().getBytes(StandardCharsets.UTF_8);
        return request("POST", url, bearer, allowHosts, payload, headers).asObject();
    }

    static JSONArray postArray(String url, String bearer, String[] allowHosts,
                               JSONObject body, Map<String, String> headers)
            throws IOException, JSONException {
        byte[] payload = body == null ? null : body.toString().getBytes(StandardCharsets.UTF_8);
        return request("POST", url, bearer, allowHosts, payload, headers).asArray();
    }

    private static Response request(String method, String url, String bearer, String[] allowHosts,
                                    byte[] payload, Map<String, String> extraHeaders)
            throws IOException {
        URL parsed = new URL(url);
        if (!"https".equalsIgnoreCase(parsed.getProtocol())) {
            throw new IOException("refusing non-https url");
        }
        HttpsURLConnection c = (HttpsURLConnection) parsed.openConnection();
        try {
            c.setConnectTimeout(CONNECT_TIMEOUT_MS);
            c.setReadTimeout(READ_TIMEOUT_MS);
            c.setRequestMethod(method);
            c.setInstanceFollowRedirects(true);
            c.setRequestProperty("Accept-Encoding", "gzip");
            c.setRequestProperty("User-Agent", "plain-dev/1");
            c.setRequestProperty("Accept", "application/json");
            if (bearer != null && !bearer.isEmpty() && hostAllowed(parsed.getHost(), allowHosts)) {
                c.setRequestProperty("Authorization", "Bearer " + bearer);
            }
            if (extraHeaders != null) {
                for (Map.Entry<String, String> e : extraHeaders.entrySet()) {
                    c.setRequestProperty(e.getKey(), e.getValue());
                }
            }
            if (payload != null) {
                c.setDoOutput(true);
                c.setFixedLengthStreamingMode(payload.length);
                if (c.getRequestProperty("Content-Type") == null) {
                    c.setRequestProperty("Content-Type", "application/json");
                }
                try (OutputStream os = c.getOutputStream()) {
                    os.write(payload);
                }
            }

            int status = c.getResponseCode();
            boolean ok = status >= 200 && status < 300;
            InputStream raw = ok ? c.getInputStream() : c.getErrorStream();
            String text = readAll(raw, "gzip".equalsIgnoreCase(c.getContentEncoding()));
            if (!ok) throw new HttpException(status, text, c.getHeaderFields());
            return new Response(text, c.getHeaderFields());
        } finally {
            c.disconnect();
        }
    }

    private static boolean hostAllowed(String host, String[] allowHosts) {
        if (host == null || allowHosts == null) return false;
        for (String allowed : allowHosts) {
            if (allowed == null) continue;
            if (allowed.startsWith("*.")) {
                String suffix = allowed.substring(1); // ".supabase.co"
                if (host.endsWith(suffix)) return true;
            } else if (host.equalsIgnoreCase(allowed)) {
                return true;
            }
        }
        return false;
    }

    private static String readAll(InputStream in, boolean gzip) throws IOException {
        if (in == null) return "";
        InputStream stream = gzip ? new GZIPInputStream(in) : in;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        try {
            while ((n = stream.read(buf)) != -1) out.write(buf, 0, n);
        } finally {
            try { stream.close(); } catch (IOException ignored) {}
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    /** URL-encode one query component. */
    static String q(String raw) {
        if (TextUtils.isEmpty(raw)) return "";
        try {
            return java.net.URLEncoder.encode(raw, "UTF-8");
        } catch (Exception e) {
            return raw;
        }
    }

    // kept so callers can reference the same constant name as the framework
    static final int HTTP_TOO_MANY_REQUESTS = 429;
    static final int HTTP_FORBIDDEN = HttpURLConnection.HTTP_FORBIDDEN;
}

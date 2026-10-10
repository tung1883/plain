package com.plainphone.app;

import android.content.Context;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Downloadable packs. Two kinds, stored apart:
 * <ul>
 * <li><b>Toolchain packs</b> ({@link #C}, {@link #JS}): headers, linter and runner for the Sandbox languages.</li>
 * <li><b>Doc packs</b>: the man-style manuals of the pman project (c, js, py, rust, linux, ...), listed by
 * pman's {@code registry.json} ({@link #REGISTRY_URL}) and read by {@link ManIndex}.</li>
 * </ul>
 * Both are downloaded on demand instead of shipped in the APK. The compilers/engines themselves stay in {@code jniLibs} (Android 10+
 * will not exec a binary downloaded into app data).
 *
 * <p>A pack lives in {@code files/sandbox/packs/<id>/} and counts as installed only when its
 * {@code .version} marker matches. Install is download to {@code <id>.zip.part} (resumable),
 * verify sha256, unzip into {@code <id>.tmp}, then rename, so a failure at any step leaves the
 * pack "not installed" and never half-installed. Zips are built by {@code sandbox-packs/build.py}.
 */
final class PackStore {

    static final class Pack {
        final String id;
        final String label;
        final int version;
        final long zipBytes;
        final long rawBytes;
        final String sha256;
        /** Doc packs: pman registry entries; the zip holds {@code man/} text that is bundled on install. */
        final boolean docs;
        final String desc;
        final List<String> topics;
        private final String fixedUrl;

        Pack(String id, String label, int version, long zipBytes, long rawBytes, String sha256) {
            this(id, label, version, zipBytes, rawBytes, sha256, false, "", new ArrayList<>(), null);
        }

        Pack(String id, String label, int version, long zipBytes, long rawBytes, String sha256, boolean docs,
             String desc, List<String> topics, String fixedUrl) {
            this.id = id;
            this.label = label;
            this.version = version;
            this.zipBytes = zipBytes;
            this.rawBytes = rawBytes;
            this.sha256 = sha256;
            this.docs = docs;
            this.desc = desc;
            this.topics = topics;
            this.fixedUrl = fixedUrl;
        }

        String url() {
            if (fixedUrl != null) return fixedUrl;
            return "https://github.com/tung1883/plain/releases/download/sandbox-pack-" + id + "-v" + version
                    + "/" + id + ".zip";
        }
    }

    /** Values printed by {@code python sandbox-packs/build.py}; bump {@code version} when a zip changes. */
    static final Pack C = new Pack("c", "C", 1, 4431729L, 17777193L,
            "5bc3ad9410cecda0450b9e7af966312eb0f918dfc856a8de72153ac79aa34a01");
    static final Pack JS = new Pack("js", "JS and TS", 1, 306820L, 1791040L,
            "29afc1bf1c824f7921c2fb8ab8100248755857017c46aca5e885c0b11a58e156");
    interface Progress {
        /** {@code total} is the number of bytes for the phase; {@code phase} is "Downloading" or "Installing". */
        void onProgress(long done, long total, String phase);
    }

    /** An error whose message is meant for the user, as is. */
    static final class Failure extends IOException {
        Failure(String message) { super(message); }
    }

    static final class Cancelled extends IOException {
        Cancelled() { super("cancelled"); }
    }

    private static final long STALE_PART_MS = 7L * 24 * 60 * 60 * 1000;
    private static final Set<String> ACTIVE = new HashSet<>();

    private PackStore() {}

    /** The pack a language needs, or null when it needs none (Python is built in). */
    static Pack forLang(SandboxRunner.Lang lang) {
        switch (lang) {
            case C: return C;
            case JS:
            case TS: return JS;
            default: return null;
        }
    }

    static File dir(Context ctx, Pack p) {
        return new File(root(ctx, p), p.id);
    }

    private static File root(Context ctx, Pack p) {
        return p.docs ? docsRoot(ctx) : toolRoot(ctx);
    }

    private static File toolRoot(Context ctx) {
        return new File(ctx.getFilesDir(), "sandbox/packs");
    }

    private static File docsRoot(Context ctx) {
        return new File(ctx.getFilesDir(), "pman/packs");
    }

    private static String key(Pack p) {
        return (p.docs ? "d:" : "t:") + p.id;
    }

    static boolean installed(Context ctx, Pack p) {
        if (p == null) return true;
        File marker = new File(dir(ctx, p), ".version");
        try {
            return marker.isFile() && String.valueOf(p.version).equals(read(marker).trim())
                    && (!p.docs || new File(dir(ctx, p), "bundle.json").isFile());
        } catch (IOException e) {
            return false;
        }
    }

    static void remove(Context ctx, Pack p) {
        deleteRecursively(dir(ctx, p));
        ManIndex.invalidate();
    }

    /** Drops interrupted installs: leftover unzip folders and download files older than a week. */
    static void cleanStale(Context ctx) {
        // Before packs existed the toolchain and JS helpers were copied out of assets into sandbox/.
        File legacy = new File(ctx.getFilesDir(), "sandbox");
        deleteRecursively(new File(legacy, "tcc"));
        //noinspection ResultOfMethodCallIgnored
        new File(legacy, "lint-js.js").delete();
        //noinspection ResultOfMethodCallIgnored
        new File(legacy, "ts-run.js").delete();
        cleanRoot(toolRoot(ctx));
        cleanRoot(docsRoot(ctx));
    }

    private static void cleanRoot(File root) {
        File[] kids = root.listFiles();
        if (kids == null) return;
        synchronized (ACTIVE) {
            for (File f : kids) {
                String n = f.getName();
                if (n.endsWith(".tmp") && !ACTIVE.contains("t:" + n.substring(0, n.length() - 4))
                        && !ACTIVE.contains("d:" + n.substring(0, n.length() - 4))) {
                    deleteRecursively(f);
                } else if (n.endsWith(".zip.part") && System.currentTimeMillis() - f.lastModified() > STALE_PART_MS) {
                    //noinspection ResultOfMethodCallIgnored
                    f.delete();
                }
            }
        }
    }

    /**
     * Downloads and installs {@code p}. Blocking; call off the main thread. Throws {@link Cancelled}
     * (keeping the partial download for a later resume) or an IOException whose message is fit to
     * show the user.
     */
    static void install(Context ctx, Pack p, Progress cb, AtomicBoolean cancel) throws IOException {
        synchronized (ACTIVE) {
            if (!ACTIVE.add(key(p))) throw new Failure(p.label + " is already downloading");
        }
        try {
            cleanStale(ctx);
            File root = root(ctx, p);
            if (!root.isDirectory() && !root.mkdirs()) throw new IOException("cannot create " + root);
            File part = new File(root, p.id + ".zip.part");
            File tmp = new File(root, p.id + ".tmp");

            long have = part.isFile() ? part.length() : 0;
            if (have > p.zipBytes) { // wrong file; start over
                //noinspection ResultOfMethodCallIgnored
                part.delete();
                have = 0;
            }
            long needed = (p.zipBytes - have) + p.rawBytes + 4L * 1024 * 1024;
            if (root.getUsableSpace() < needed) {
                throw new Failure(String.format(Locale.US, "Not enough space (needs %s free)", mb(needed)));
            }

            if (have < p.zipBytes) download(p, part, have, cb, cancel);

            if (part.length() != p.zipBytes || !p.sha256.equalsIgnoreCase(sha256(part))) {
                //noinspection ResultOfMethodCallIgnored
                part.delete(); // resuming a corrupt file would fail the same way
                throw new Failure("Download was corrupted, try again");
            }

            deleteRecursively(tmp);
            unzip(part, tmp, cb, cancel);
            if (p.docs) ManIndex.buildBundle(tmp);
            SandboxRunner.write(new File(tmp, ".version"), String.valueOf(p.version));

            File dest = dir(ctx, p);
            deleteRecursively(dest);
            if (!tmp.renameTo(dest)) throw new IOException("cannot install into " + dest);
            //noinspection ResultOfMethodCallIgnored
            part.delete();
            ManIndex.invalidate();
        } catch (IOException e) {
            deleteRecursively(new File(root(ctx, p), p.id + ".tmp"));
            throw e;
        } finally {
            synchronized (ACTIVE) {
                ACTIVE.remove(key(p));
            }
        }
    }

    private static void download(Pack p, File part, long have, Progress cb, AtomicBoolean cancel) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(p.url()).openConnection();
        c.setConnectTimeout(15_000);
        c.setReadTimeout(30_000);
        if (have > 0) c.setRequestProperty("Range", "bytes=" + have + "-");
        try {
            int code = c.getResponseCode();
            boolean resumed = code == HttpURLConnection.HTTP_PARTIAL;
            if (code != HttpURLConnection.HTTP_OK && !resumed) {
                throw new Failure(code == 404 ? "Download not available yet (HTTP 404)" : "Server error (HTTP " + code + ")");
            }
            long done = resumed ? have : 0; // a plain 200 ignores Range: restart from zero
            try (InputStream in = c.getInputStream();
                 OutputStream out = new FileOutputStream(part, resumed)) {
                byte[] buf = new byte[32 * 1024];
                int n;
                while ((n = in.read(buf)) != -1) {
                    if (cancel.get()) throw new Cancelled();
                    out.write(buf, 0, n);
                    done += n;
                    cb.onProgress(done, p.zipBytes, "Downloading");
                }
            }
        } catch (Cancelled | Failure e) {
            throw e;
        } catch (IOException e) {
            throw new Failure("No connection. Nothing was installed.");
        } finally {
            c.disconnect();
        }
    }

    private static void unzip(File zip, File dest, Progress cb, AtomicBoolean cancel) throws IOException {
        String destPath = dest.getCanonicalPath() + File.separator;
        long total = 0;
        try (ZipInputStream z = new ZipInputStream(new FileInputStream(zip))) {
            ZipEntry e;
            byte[] buf = new byte[64 * 1024];
            while ((e = z.getNextEntry()) != null) {
                if (cancel.get()) throw new Cancelled();
                File f = new File(dest, e.getName());
                if (!f.getCanonicalPath().startsWith(destPath)) throw new IOException("bad entry " + e.getName());
                if (e.isDirectory()) {
                    //noinspection ResultOfMethodCallIgnored
                    f.mkdirs();
                    continue;
                }
                File parent = f.getParentFile();
                if (parent != null) //noinspection ResultOfMethodCallIgnored
                    parent.mkdirs();
                try (OutputStream out = new FileOutputStream(f)) {
                    int n;
                    while ((n = z.read(buf)) != -1) {
                        out.write(buf, 0, n);
                        total += n;
                    }
                }
                cb.onProgress(total, 0, "Installing");
            }
        }
    }

    private static String sha256(File f) throws IOException {
        try (InputStream in = new FileInputStream(f)) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] buf = new byte[64 * 1024];
            int n;
            while ((n = in.read(buf)) != -1) md.update(buf, 0, n);
            StringBuilder sb = new StringBuilder();
            for (byte b : md.digest()) sb.append(String.format(Locale.US, "%02x", b));
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IOException(e);
        }
    }

    // ---- doc packs (pman registry) --------------------------------------------------------------

    static final String REGISTRY_URL = "https://github.com/tung1883/pman/releases/latest/download/registry.json";
    private static final String REGISTRY_BASE = REGISTRY_URL.substring(0, REGISTRY_URL.lastIndexOf('/') + 1);

    private static List<Pack> registry;

    private static File registryFile(Context ctx) {
        return new File(ctx.getFilesDir(), "pman/registry.json");
    }

    /** The doc packs of the last registry download (empty before the first one). */
    static synchronized List<Pack> docPacks(Context ctx) {
        if (registry == null) {
            registry = new ArrayList<>();
            File f = registryFile(ctx);
            if (f.isFile()) {
                try {
                    registry = parseRegistry(read(f));
                } catch (IOException | org.json.JSONException ignored) {
                    // a bad cache is refetched on demand
                }
            }
        }
        return registry;
    }

    static boolean hasRegistry(Context ctx) {
        return !docPacks(ctx).isEmpty();
    }

    /** True when the saved list is under a day old; an older one is refetched (and kept if offline). */
    static boolean registryFresh(Context ctx) {
        return hasRegistry(ctx) && System.currentTimeMillis() - registryFile(ctx).lastModified() < 24L * 3600 * 1000;
    }

    /** Downloads the registry (blocking; call off the main thread) and keeps a copy for offline use. */
    static void refreshRegistry(Context ctx) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(REGISTRY_URL).openConnection();
        c.setConnectTimeout(15_000);
        c.setReadTimeout(30_000);
        try {
            if (c.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new Failure("Cannot load the list of manuals (HTTP " + c.getResponseCode() + ")");
            }
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            try (InputStream in = c.getInputStream()) {
                byte[] b = new byte[16 * 1024];
                int n;
                while ((n = in.read(b)) != -1) buf.write(b, 0, n);
            }
            String text = new String(buf.toByteArray(), StandardCharsets.UTF_8);
            List<Pack> parsed = parseRegistry(text);
            File f = registryFile(ctx);
            //noinspection ResultOfMethodCallIgnored
            f.getParentFile().mkdirs();
            SandboxRunner.write(f, text);
            synchronized (PackStore.class) {
                registry = parsed;
            }
        } catch (org.json.JSONException e) {
            throw new Failure("Bad list of manuals");
        } catch (Failure e) {
            throw e;
        } catch (IOException e) {
            throw new Failure("No connection. Cannot load the list of manuals.");
        } finally {
            c.disconnect();
        }
    }

    private static List<Pack> parseRegistry(String text) throws org.json.JSONException {
        List<Pack> out = new ArrayList<>();
        JSONObject packs = new JSONObject(text).getJSONObject("packs");
        java.util.Iterator<String> ids = packs.keys();
        while (ids.hasNext()) {
            String id = ids.next();
            JSONObject o = packs.getJSONObject(id);
            List<String> topics = new ArrayList<>();
            JSONArray t = o.optJSONArray("topics");
            if (t != null) for (int i = 0; i < t.length(); i++) topics.add(t.getString(i));
            String url = o.getString("url");
            if (!url.startsWith("http")) url = REGISTRY_BASE + url;
            long size = o.getLong("size");
            out.add(new Pack(id, id, o.getInt("version"), size, size * 4, o.getString("sha256"), true,
                    o.optString("desc", ""), topics, url));
        }
        java.util.Collections.sort(out, (a, b) -> a.id.compareTo(b.id));
        return out;
    }

    static Pack docPack(Context ctx, String id) {
        for (Pack p : docPacks(ctx)) if (p.id.equals(id)) return p;
        return null;
    }

    /** The doc pack that provides {@code topic}: the pack of that name, else the first listing it. */
    static Pack docPackForTopic(Context ctx, String topic) {
        Pack byId = docPack(ctx, topic);
        if (byId != null) return byId;
        for (Pack p : docPacks(ctx)) if (p.topics.contains(topic)) return p;
        return null;
    }

    /** Doc packs on disk (whatever the registry says now). */
    static List<Pack> installedDocs(Context ctx) {
        List<Pack> out = new ArrayList<>();
        File[] kids = docsRoot(ctx).listFiles();
        if (kids == null) return out;
        java.util.Arrays.sort(kids);
        for (File d : kids) {
            if (!d.isDirectory() || d.getName().endsWith(".tmp") || !new File(d, ".version").isFile()) continue;
            Pack known = docPack(ctx, d.getName());
            out.add(known != null ? known : new Pack(d.getName(), d.getName(), 0, 0, 0, "", true, "",
                    new ArrayList<>(), null));
        }
        return out;
    }

    /** Version of an installed doc pack, or -1. */
    static int installedVersion(Context ctx, Pack p) {
        try {
            File marker = new File(dir(ctx, p), ".version");
            return marker.isFile() ? Integer.parseInt(read(marker).trim()) : -1;
        } catch (IOException | NumberFormatException e) {
            return -1;
        }
    }

    static String mb(long bytes) {
        return String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0));
    }

    private static String read(File f) throws IOException {
        try (InputStream in = new FileInputStream(f)) {
            byte[] b = new byte[(int) f.length()];
            int off = 0;
            while (off < b.length) {
                int n = in.read(b, off, b.length - off);
                if (n < 0) break;
                off += n;
            }
            return new String(b, 0, off, StandardCharsets.UTF_8);
        }
    }

    private static void deleteRecursively(File f) {
        File[] kids = f.listFiles();
        if (kids != null) for (File k : kids) deleteRecursively(k);
        //noinspection ResultOfMethodCallIgnored
        f.delete();
    }
}

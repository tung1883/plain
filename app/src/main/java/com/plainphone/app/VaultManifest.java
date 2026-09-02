package com.plainphone.app;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKey;

/**
 * The vault's encrypted index — {@code m/index.pv} — decrypted into RAM on
 * unlock. Every entry's {@code docId} IS its path in encrypted-name-space (as
 * built by {@link VaultStore}), so a listing is a RAM filter and metadata
 * (name / size / mtime) never touches a filename cipher again after load.
 *
 * <p>Renaming or moving a folder changes the text of its own {@code docId} —
 * {@link #rekey} rewrites that entry and every descendant's prefix in one pass.
 * If the file is missing, unreadable, or from a different key, the caller
 * rebuilds it by walking the real (encrypted) filesystem once — see
 * {@link #loadOrRebuild}. A failed write leaves the file at its last good
 * version; the in-memory copy stays authoritative for the rest of the session.
 */
final class VaultManifest {

    private static final String TAG = "VaultManifest";
    private static final String FILE_NAME = "index.pv";

    static class Entry {
        String docId;
        String name;
        boolean isDir;
        long size;
        long mtime;

        Entry(String docId, String name, boolean isDir, long size, long mtime) {
            this.docId = docId;
            this.name = name;
            this.isDir = isDir;
            this.size = size;
            this.mtime = mtime;
        }
    }

    private final Map<String, Entry> byId = new LinkedHashMap<>();

    private VaultManifest() {}

    static String parentOf(String docId) {
        int i = docId.lastIndexOf('/');
        return i < 0 ? null : docId.substring(0, i);
    }

    synchronized List<Entry> children(String parentDocId) {
        List<Entry> out = new ArrayList<>();
        for (Entry e : byId.values()) {
            if (parentDocId.equals(parentOf(e.docId))) out.add(e);
        }
        return out;
    }

    synchronized Entry get(String docId) {
        return byId.get(docId);
    }

    synchronized void put(Entry entry) {
        byId.put(entry.docId, entry);
    }

    /** Remove an entry and, if it's a folder, every descendant. */
    synchronized void remove(String docId) {
        byId.remove(docId);
        String prefix = docId + "/";
        byId.keySet().removeIf(id -> id.startsWith(prefix));
    }

    /** A rename or move: the docId text changed. Rewrites the entry and every descendant. */
    synchronized void rekey(String oldDocId, String newDocId, String newName) {
        Entry entry = byId.remove(oldDocId);
        if (entry == null) return;
        entry.docId = newDocId;
        entry.name = newName;
        byId.put(newDocId, entry);

        String oldPrefix = oldDocId + "/";
        String newPrefix = newDocId + "/";
        List<Entry> descendants = new ArrayList<>();
        for (String id : new ArrayList<>(byId.keySet())) {
            if (id.startsWith(oldPrefix)) descendants.add(byId.remove(id));
        }
        for (Entry d : descendants) {
            d.docId = newPrefix + d.docId.substring(oldPrefix.length());
            byId.put(d.docId, d);
        }
    }

    /** Content changed in place (same docId) — refresh its exact size / mtime. */
    synchronized void updateContent(String docId, long size, long mtime) {
        Entry e = byId.get(docId);
        if (e != null) {
            e.size = size;
            e.mtime = mtime;
        }
    }

    // --- persistence -------------------------------------------------

    private static File dir(Context context) {
        return new File(VaultSession.vaultRoot(context), "m");
    }

    static VaultManifest loadOrRebuild(Context context, SecretKey manifestKey, SecretKey nameKey) {
        VaultManifest loaded = tryLoad(context, manifestKey);
        if (loaded != null) return loaded;
        VaultManifest rebuilt = rebuild(context, nameKey);
        try {
            rebuilt.save(context, manifestKey);
        } catch (IOException e) {
            Log.w(TAG, "couldn't persist a freshly rebuilt manifest", e);
        }
        return rebuilt;
    }

    private static VaultManifest tryLoad(Context context, SecretKey manifestKey) {
        File file = new File(dir(context), FILE_NAME);
        if (!file.isFile() || file.length() == 0) return null;
        try {
            byte[] wrapped = readAll(file);
            byte[] json = VaultCrypto.unwrap(manifestKey, wrapped);
            return fromJson(new String(json, StandardCharsets.UTF_8));
        } catch (Exception e) {
            Log.w(TAG, "manifest unreadable, will rebuild from the filesystem", e);
            return null;
        }
    }

    private static byte[] readAll(File file) throws IOException {
        try (java.io.FileInputStream in = new java.io.FileInputStream(file);
             java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
            return out.toByteArray();
        }
    }

    private static VaultManifest fromJson(String text) throws JSONException {
        VaultManifest manifest = new VaultManifest();
        JSONArray arr = new JSONArray(text);
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.getJSONObject(i);
            String id = o.getString("id");
            manifest.byId.put(id, new Entry(id, o.getString("name"), o.getBoolean("dir"),
                    o.optLong("size", 0), o.optLong("mtime", 0)));
        }
        return manifest;
    }

    private synchronized String toJson() throws JSONException {
        JSONArray arr = new JSONArray();
        for (Entry e : byId.values()) {
            JSONObject o = new JSONObject();
            o.put("id", e.docId);
            o.put("name", e.name);
            o.put("dir", e.isDir);
            o.put("size", e.size);
            o.put("mtime", e.mtime);
            arr.put(o);
        }
        return arr.toString();
    }

    /** Atomic write: temp + rename, keeping the previous version as {@code .bak}. */
    void save(Context context, SecretKey manifestKey) throws IOException {
        File dir = dir(context);
        if (!dir.isDirectory() && !dir.mkdirs()) throw new IOException("can't create " + dir);
        File target = new File(dir, FILE_NAME);
        File bak = new File(dir, FILE_NAME + ".bak");
        File tmp = new File(dir, FILE_NAME + ".tmp");

        byte[] wrapped;
        try {
            wrapped = VaultCrypto.wrap(manifestKey, toJson().getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException | JSONException e) {
            throw new IOException(e);
        }

        writeAll(tmp, wrapped);
        if (target.isFile()) {
            bak.delete();
            target.renameTo(bak);
        }
        if (!tmp.renameTo(target)) {
            // Some FUSE-backed volumes reject cross-name rename — write in place instead.
            writeAll(target, wrapped);
            tmp.delete();
        }
    }

    private static void writeAll(File file, byte[] data) throws IOException {
        try (java.io.FileOutputStream out = new java.io.FileOutputStream(file)) {
            out.write(data);
            out.getFD().sync();
        }
    }

    /** Walk the real (encrypted-name) filesystem once — migration / corruption recovery. */
    private static VaultManifest rebuild(Context context, SecretKey nameKey) {
        VaultManifest manifest = new VaultManifest();
        File vaultRoot = VaultSession.vaultRoot(context);
        walk(vaultRoot, VaultStore.ROOT_DOC_ID, nameKey, manifest);
        return manifest;
    }

    private static void walk(File vaultRoot, String docId, SecretKey nameKey, VaultManifest manifest) {
        File[] kids = new File(vaultRoot, docId).listFiles();
        if (kids == null) return;
        for (File kid : kids) {
            String name = VaultCrypto.decryptName(nameKey, kid.getName());
            if (name == null) continue;   // not one of ours (e.g. our own .tmp/.bak litter)
            String kidId = docId + "/" + kid.getName();
            boolean isDir = kid.isDirectory();
            long size = isDir ? 0 : VaultCrypto.plaintextSize(kid.length());
            manifest.byId.put(kidId, new Entry(kidId, name, isDir, size, kid.lastModified()));
            if (isDir) walk(vaultRoot, kidId, nameKey, manifest);
        }
    }
}

package com.plainphone.app;

import android.content.Context;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared recorder actions: the app-private store and the bridge that keeps
 * recordings encrypted in {@code vault/Plain Recorder/}. Mirrors {@link Notes}.
 */
final class Recorder {

    private Recorder() {}

    static final String VAULT_FOLDER = "Plain Recorder";
    private static final String VAULT_PREFIX = "vault:";
    private static final String[] AUDIO_EXTS = {".wav", ".m4a", ".3gp", ".mp3", ".aac", ".ogg"};

    // --- local store ------------------------------------------------------

    static File dir(Context c) {
        File d = new File(c.getFilesDir(), "recordings");
        if (!d.isDirectory()) d.mkdirs();
        return d;
    }

    static File fileFor(Context c, Recording r) {
        return new File(dir(c), r.id + "." + r.format);
    }

    static List<Recording> all(Context c) {
        return Config.getRecordings(c);
    }

    static void save(Context c, List<Recording> list) {
        Config.setRecordings(c, list);
    }

    /** Sequential auto-name: "Recording 1", "Recording 2", … (consumes the number). */
    static String nextName(Context c) {
        int n = Config.getRecorderNextNumber(c);
        Config.setRecorderNextNumber(c, n + 1);
        return "Recording " + n;
    }

    /** Prepend a freshly captured recording. */
    static void add(Context c, Recording r) {
        List<Recording> list = all(c);
        list.add(0, r);
        save(c, list);
    }

    /** Fill in a missing duration once the player has discovered the real one. */
    static void healDuration(Context c, String id, long durationMs) {
        if (id == null || isVaulted(id) || durationMs <= 0) return;
        List<Recording> list = all(c);
        boolean changed = false;
        for (Recording r : list) {
            if (r.id.equals(id) && r.durationMs <= 0) {
                r.durationMs = durationMs;
                changed = true;
            }
        }
        if (changed) save(c, list);
    }

    static void rename(Context c, String id, String newName) {
        List<Recording> list = all(c);
        for (Recording r : list) {
            if (r.id.equals(id)) {
                r.name = newName;
                break;
            }
        }
        save(c, list);
    }

    static void deleteLocal(Context c, Recording r) {
        fileFor(c, r).delete();
        List<Recording> list = all(c);
        list.removeIf(x -> x.id.equals(r.id));
        save(c, list);
    }

    // --- vault bridge ---------------------------------------------------

    static boolean isVaulted(String id) {
        return id != null && id.startsWith(VAULT_PREFIX);
    }

    static String docIdOf(String id) {
        return id.substring(VAULT_PREFIX.length());
    }

    static boolean vaultReady(Context c) {
        return VaultFormat.exists(VaultSession.vaultRoot(c)) && VaultSession.get().isUnlocked();
    }

    private static String vaultFolderIfPresent(Context c) {
        if (!vaultReady(c)) return null;
        try {
            return VaultStore.findChild(c, VaultStore.ROOT_DOC_ID, VAULT_FOLDER);
        } catch (Exception e) {
            return null;
        }
    }

    private static String vaultFolder(Context c) {
        if (!vaultReady(c)) return null;
        try {
            String existing = VaultStore.findChild(c, VaultStore.ROOT_DOC_ID, VAULT_FOLDER);
            return existing != null ? existing : VaultStore.createDocument(c,
                    VaultStore.ROOT_DOC_ID, VAULT_FOLDER, DocumentsContract.Document.MIME_TYPE_DIR);
        } catch (Exception e) {
            return null;
        }
    }

    static void ensureVaultFolder(Context c) {
        vaultFolder(c);
    }

    private static boolean isAudioFile(String name) {
        String n = name.toLowerCase();
        for (String ext : AUDIO_EXTS) if (n.endsWith(ext)) return true;
        return false;
    }

    private static String extOf(String name) {
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot + 1).toLowerCase() : "m4a";
    }

    private static String baseOf(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    /** Vault recordings as transient {@link Recording}s; empty when locked. */
    static List<Recording> vaultRecordings(Context c) {
        List<Recording> out = new ArrayList<>();
        String folder = vaultFolderIfPresent(c);
        if (folder == null) return out;
        try {
            org.json.JSONObject meta = Config.getVaultRecMeta(c);
            for (VaultStore.Entry e : VaultStore.list(c, folder)) {
                if (e.isDir || !isAudioFile(e.name)) continue;
                Recording r = Recording.forVault(VAULT_PREFIX + e.docId, baseOf(e.name),
                        extOf(e.name), e.lastModified);
                r.durationMs = meta.optLong(e.docId, 0L);
                out.add(r);
            }
        } catch (Exception ignored) {
        }
        Config.setRecordingCount(c, out.size());
        return out;
    }

    static String vaultRecordingName(Context c, String id) {
        try {
            return VaultStore.stat(c, docIdOf(id)).name;
        } catch (Exception e) {
            return "recording";
        }
    }

    /** Move one local recording into the vault. */
    static boolean moveToVault(Context c, Recording r) {
        String folder = vaultFolder(c);
        if (folder == null) return false;
        File local = fileFor(c, r);
        if (!local.isFile()) return false;
        try (FileInputStream in = new FileInputStream(local)) {
            String name = safeName(r.displayName()) + "." + r.format;
            long duration = r.durationMs > 0 ? r.durationMs : probeDuration(local);
            String docId = VaultStore.importStream(c, folder, name, in);
            Config.setVaultRecDuration(c, docId, duration);
            local.delete();
            List<Recording> list = all(c);
            list.removeIf(x -> x.id.equals(r.id));
            save(c, list);
            Config.setRecordingCount(c, Config.getRecordingCount(c) + 1);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Move a vaulted recording back to the local store. */
    static boolean moveOutOfVault(Context c, String id) {
        if (!isVaulted(id) || !vaultReady(c)) return false;
        String docId = docIdOf(id);
        try {
            String name = VaultStore.stat(c, docId).name;
            String ext = extOf(name);
            Recording r = Recording.create(baseOf(name), ext,
                    Config.getRecorderSampleRate(c), 0, "");
            File dest = fileFor(c, r);
            VaultStore.decryptToFile(c, docId, dest);
            long known = Config.getVaultRecDuration(c, docId);
            r.durationMs = known > 0 ? known : probeDuration(dest);
            List<Recording> list = all(c);
            list.add(0, r);
            save(c, list);
            VaultStore.delete(c, docId);
            Config.removeVaultRecDuration(c, docId);
            Config.setRecordingCount(c, Math.max(0, Config.getRecordingCount(c) - 1));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    static void deleteVaultRecording(Context c, String id) {
        if (!isVaulted(id) || !vaultReady(c)) return;
        try {
            VaultStore.delete(c, docIdOf(id));
            Config.removeVaultRecDuration(c, docIdOf(id));
            Config.setRecordingCount(c, Math.max(0, Config.getRecordingCount(c) - 1));
        } catch (Exception ignored) {
        }
    }

    /** Move every local recording into the vault. Returns how many moved. */
    static int moveAllToVault(Context c) {
        int moved = 0;
        for (Recording r : new ArrayList<>(all(c))) {
            if (moveToVault(c, r)) moved++;
        }
        return moved;
    }

    // --- import: copy picked audio files into the local store --------------

    /** Copy one picked audio file into the local store. Returns true if it landed. */
    static boolean importOne(Context c, Uri uri) {
        String name = queryName(c, uri);
        String ext = extFor(c, uri, name);
        Recording r = Recording.create(
                name != null ? baseOf(name) : "Imported recording",
                ext, Config.getRecorderSampleRate(c), 0, "");
        File dest = fileFor(c, r);
        if (!copyUri(c, uri, dest)) {
            dest.delete();
            return false;
        }
        r.durationMs = probeDuration(dest);
        List<Recording> list = all(c);
        list.add(0, r);
        save(c, list);
        return true;
    }

    private static String extFor(Context c, Uri uri, String name) {
        if (name != null) {
            int dot = name.lastIndexOf('.');
            if (dot >= 0 && dot < name.length() - 1) return name.substring(dot + 1).toLowerCase();
        }
        String mime = c.getContentResolver().getType(uri);
        if (mime == null) return "m4a";
        switch (mime) {
            case "audio/mpeg": return "mp3";
            case "audio/aac": return "aac";
            case "audio/wav": case "audio/x-wav": return "wav";
            case "audio/3gpp": return "3gp";
            case "audio/ogg": return "ogg";
            default: return "m4a";
        }
    }

    private static String queryName(Context c, Uri uri) {
        try (Cursor cur = c.getContentResolver().query(uri,
                new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cur != null && cur.moveToFirst() && !cur.isNull(0)) return cur.getString(0);
        } catch (Exception ignored) {
        }
        return null;
    }

    private static boolean copyUri(Context c, Uri uri, File dest) {
        try (InputStream in = c.getContentResolver().openInputStream(uri);
             FileOutputStream out = new FileOutputStream(dest)) {
            if (in == null) return false;
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    static long probeDuration(File file) {
        long ms = mmrDuration(file);
        // MediaMetadataRetriever routinely reports 0 for PCM WAV, and occasionally
        // for MediaRecorder m4a/3gp — fall back to the WAV header, then to MediaPlayer
        // (which is what the player screen itself uses, so it always agrees).
        if (ms <= 0) ms = wavDurationMs(file);
        if (ms <= 0) ms = mediaPlayerDuration(file);
        return Math.max(0, ms);
    }

    private static long mmrDuration(File file) {
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        try {
            mmr.setDataSource(file.getAbsolutePath());
            String d = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            return d == null ? 0 : Long.parseLong(d);
        } catch (Exception e) {
            return 0;
        } finally {
            try {
                mmr.release();
            } catch (Exception ignored) {
            }
        }
    }

    private static long mediaPlayerDuration(File file) {
        android.media.MediaPlayer mp = new android.media.MediaPlayer();
        try {
            mp.setDataSource(file.getAbsolutePath());
            mp.prepare();
            int d = mp.getDuration();
            return d > 0 ? d : 0;
        } catch (Exception e) {
            return 0;
        } finally {
            try {
                mp.release();
            } catch (Exception ignored) {
            }
        }
    }

    /** Duration of a canonical PCM WAV from its fmt chunk + data size, or 0 if not a WAV. */
    private static long wavDurationMs(File file) {
        try (FileInputStream in = new FileInputStream(file)) {
            byte[] h = new byte[44];
            if (in.read(h) < 44) return 0;
            if (h[0] != 'R' || h[1] != 'I' || h[2] != 'F' || h[3] != 'F'
                    || h[8] != 'W' || h[9] != 'A' || h[10] != 'V' || h[11] != 'E') return 0;
            int channels = le16(h, 22);
            int sampleRate = le32(h, 24);
            int bits = le16(h, 34);
            long declared = le32(h, 40) & 0xFFFFFFFFL;
            long actual = Math.max(0, file.length() - 44);
            long dataBytes = declared > 0 ? Math.min(declared, actual) : actual;
            long bytesPerSec = (long) sampleRate * channels * (bits / 8);
            return bytesPerSec > 0 ? dataBytes * 1000L / bytesPerSec : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private static int le16(byte[] b, int off) {
        return (b[off] & 0xFF) | ((b[off + 1] & 0xFF) << 8);
    }

    private static int le32(byte[] b, int off) {
        return (b[off] & 0xFF) | ((b[off + 1] & 0xFF) << 8)
                | ((b[off + 2] & 0xFF) << 16) | ((b[off + 3] & 0xFF) << 24);
    }

    private static String safeName(String s) {
        String base = s.replaceAll("[^A-Za-z0-9-_ ]", "").trim().replaceAll("\\s+", "-");
        return base.isEmpty() ? "recording" : base;
    }
}

package com.plainphone.app;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.provider.DocumentsContract;

import java.io.File;
import java.io.FileInputStream;
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
            for (VaultStore.Entry e : VaultStore.list(c, folder)) {
                if (e.isDir || !isAudioFile(e.name)) continue;
                out.add(Recording.forVault(VAULT_PREFIX + e.docId, baseOf(e.name),
                        extOf(e.name), e.lastModified));
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
            VaultStore.importStream(c, folder, name, in);
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
            r.durationMs = probeDuration(dest);
            List<Recording> list = all(c);
            list.add(0, r);
            save(c, list);
            VaultStore.delete(c, docId);
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

    static long probeDuration(File file) {
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

    private static String safeName(String s) {
        String base = s.replaceAll("[^A-Za-z0-9-_ ]", "").trim().replaceAll("\\s+", "-");
        return base.isEmpty() ? "recording" : base;
    }
}

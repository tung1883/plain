package com.plainphone.app;

import android.content.Context;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.SecureRandom;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks the plaintext temp files handed out by {@link VaultDocumentsProvider#openDocument}.
 * On close of a writable descriptor the temp is re-encrypted back into the blob;
 * every temp is then overwritten and deleted. {@link VaultSession#lock} shreds
 * whatever is still open.
 */
final class VaultOpenFiles {

    private static final VaultOpenFiles INSTANCE = new VaultOpenFiles();

    static VaultOpenFiles get() {
        return INSTANCE;
    }

    private final ConcurrentHashMap<File, Entry> open = new ConcurrentHashMap<>();

    private static class Entry {
        Context appContext;
        String docId;
        boolean writable;
    }

    private VaultOpenFiles() {}

    static File openDir(Context context) {
        File dir = new File(context.getNoBackupFilesDir(), "vault-open");
        dir.mkdirs();
        return dir;
    }

    File track(Context context, String docId, File plainTmp, boolean writable) {
        Entry entry = new Entry();
        entry.appContext = context.getApplicationContext();
        entry.docId = docId;
        entry.writable = writable;
        open.put(plainTmp, entry);
        return plainTmp;
    }

    void onClosed(File plainTmp) {
        Entry entry = open.remove(plainTmp);
        if (entry == null) {
            shred(plainTmp);
            return;
        }
        try {
            if (entry.writable && VaultSession.get().isUnlocked()) {
                VaultStore.encryptFromFile(entry.appContext, plainTmp, entry.docId);
            }
        } catch (Exception e) {
            android.util.Log.w("VaultOpenFiles", "re-encrypt failed for " + entry.docId, e);
        } finally {
            shred(plainTmp);
        }
    }

    void shredAll() {
        for (File f : open.keySet()) {
            open.remove(f);
            shred(f);
        }
    }

    private static void shred(File f) {
        if (f == null || !f.isFile()) return;
        try (RandomAccessFile raf = new RandomAccessFile(f, "rw")) {
            long len = raf.length();
            byte[] junk = new byte[4096];
            new SecureRandom().nextBytes(junk);
            long written = 0;
            while (written < len) {
                int n = (int) Math.min(junk.length, len - written);
                raf.write(junk, 0, n);
                written += n;
            }
            raf.getFD().sync();
        } catch (IOException ignored) {
        }
        f.delete();
    }
}

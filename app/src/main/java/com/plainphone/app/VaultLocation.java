package com.plainphone.app;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.Settings;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Phase 5: the vault can live in a user-picked folder on shared storage instead
 * of the app-private default. plainphone already holds all-files access (for
 * file search), so the store still works with plain {@link File} I/O — only the
 * root moves. The vault always sits in a {@code PlainPhoneVault/} subfolder so it
 * never litters the chosen directory, and carries a {@code .nomedia} so the
 * blobs stay out of the gallery.
 */
final class VaultLocation {

    private VaultLocation() {}

    static final String DIR_NAME = "PlainPhoneVault";

    static boolean hasStorageAccess() {
        return Build.VERSION.SDK_INT < 30 || Environment.isExternalStorageManager();
    }

    static Intent storageAccessSettingsIntent(Context context) {
        return new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION,
                Uri.parse("package:" + context.getPackageName()));
    }

    /** The folder an {@code ACTION_OPEN_DOCUMENT_TREE} result points at, or null if unsupported. */
    static File treeUriToDir(Uri treeUri) {
        String docId;
        try {
            docId = DocumentsContract.getTreeDocumentId(treeUri);
        } catch (Exception e) {
            return null;
        }
        String[] parts = docId.split(":", 2);
        String volume = parts[0];
        String relative = parts.length > 1 ? parts[1] : "";
        File base = "primary".equalsIgnoreCase(volume)
                ? Environment.getExternalStorageDirectory()
                : new File("/storage/" + volume);
        if (!base.isDirectory()) return null;
        return relative.isEmpty() ? base : new File(base, relative);
    }

    static File vaultDirIn(File pickedFolder) {
        return new File(pickedFolder, DIR_NAME);
    }

    static void ensureNoMedia(File vaultDir) {
        File nomedia = new File(vaultDir, ".nomedia");
        if (vaultDir.isDirectory() && !nomedia.exists()) {
            try {
                nomedia.createNewFile();
            } catch (IOException ignored) {
            }
        }
    }

    /** Human-readable current location for a settings row. */
    static String label(Context context) {
        String path = Config.getVaultLocationPath(context);
        if (path == null) return "Internal (app-private)";
        String ext = Environment.getExternalStorageDirectory().getAbsolutePath();
        return path.startsWith(ext + "/") ? path.substring(ext.length() + 1) : path;
    }

    /** Move the whole vault directory. Rename if same filesystem, else copy + delete. */
    static void moveVault(File src, File dst) throws IOException {
        if (!src.isDirectory()) throw new IOException("no vault at " + src);
        if (dst.exists()) throw new IOException("already something at " + dst);
        File parent = dst.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("can't create " + parent);
        }
        if (src.renameTo(dst)) return;
        copyDir(src, dst);
        if (!deleteDir(src)) {
            throw new IOException("vault copied to " + dst + " but the old copy at " + src
                    + " couldn't be removed — delete it by hand");
        }
    }

    private static void copyDir(File src, File dst) throws IOException {
        if (src.isDirectory()) {
            if (!dst.mkdirs() && !dst.isDirectory()) throw new IOException("mkdir " + dst);
            File[] kids = src.listFiles();
            if (kids != null) {
                for (File kid : kids) copyDir(kid, new File(dst, kid.getName()));
            }
        } else {
            try (InputStream in = new FileInputStream(src);
                 OutputStream out = new FileOutputStream(dst)) {
                byte[] buf = new byte[64 * 1024];
                int n;
                while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
            }
        }
    }

    private static boolean deleteDir(File dir) {
        File[] kids = dir.listFiles();
        if (kids != null) {
            for (File kid : kids) deleteDir(kid);
        }
        return dir.delete();
    }
}

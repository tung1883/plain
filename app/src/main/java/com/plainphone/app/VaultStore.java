package com.plainphone.app;

import android.content.Context;
import android.provider.DocumentsContract.Document;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.crypto.SecretKey;

/**
 * The filesystem layer over {@code filesDir/vault/}. Folders are real
 * subdirectories under {@code d/} with GCM-encrypted names; a {@code docId} is the
 * vault-relative encrypted path (e.g. {@code d/AbC.pv} or {@code d/Sub9.pv/AbC.pv}).
 * Phase 1: no manifest — a listing is {@code File.listFiles()} plus a name decrypt
 * per entry; size / mtime come from the blob.
 */
final class VaultStore {

    private VaultStore() {}

    static final String ROOT_DOC_ID = "d";

    static class Entry {
        String docId;
        String name;
        boolean isDir;
        long size;       // plaintext size for files, 0 for dirs
        long lastModified;
        String mimeType;
    }

    // --- path plumbing -------------------------------------------------

    static File resolve(Context context, String docId) throws FileNotFoundException {
        if (docId == null || docId.isEmpty()) throw new FileNotFoundException("empty docId");
        for (String part : docId.split("/")) {
            if (part.equals("..") || part.equals(".") || part.isEmpty()) {
                throw new FileNotFoundException("bad docId " + docId);
            }
        }
        if (!docId.equals(ROOT_DOC_ID) && !docId.startsWith(ROOT_DOC_ID + "/")) {
            throw new FileNotFoundException("docId outside vault: " + docId);
        }
        return new File(VaultSession.vaultRoot(context), docId);
    }

    static boolean isChild(String parentDocId, String docId) {
        return docId.startsWith(parentDocId + "/");
    }

    private static SecretKey nameKey() {
        SecretKey key = VaultSession.get().nameKey();
        if (key == null) throw new IllegalStateException("vault locked");
        return key;
    }

    private static SecretKey contentKey() {
        SecretKey key = VaultSession.get().contentKey();
        if (key == null) throw new IllegalStateException("vault locked");
        return key;
    }

    // --- queries -----------------------------------------------------

    static Entry stat(Context context, String docId) throws FileNotFoundException {
        File file = resolve(context, docId);
        if (!file.exists()) throw new FileNotFoundException(docId);
        Entry entry = new Entry();
        entry.docId = docId;
        entry.isDir = file.isDirectory();
        entry.lastModified = file.lastModified();
        if (docId.equals(ROOT_DOC_ID)) {
            entry.name = "PlainPhone Vault";
            entry.isDir = true;
        } else {
            String decoded = VaultCrypto.decryptName(nameKey(), file.getName());
            entry.name = decoded != null ? decoded : file.getName();
        }
        if (entry.isDir) {
            entry.mimeType = Document.MIME_TYPE_DIR;
        } else {
            entry.size = VaultCrypto.plaintextSize(file.length());
            entry.mimeType = mimeOf(entry.name);
        }
        return entry;
    }

    static List<Entry> list(Context context, String parentDocId) throws FileNotFoundException {
        File dir = resolve(context, parentDocId);
        File[] children = dir.listFiles();
        List<Entry> out = new ArrayList<>();
        if (children == null) return out;
        for (File child : children) {
            String decoded = VaultCrypto.decryptName(nameKey(), child.getName());
            if (decoded == null) continue;   // not one of ours / not this key
            Entry entry = new Entry();
            entry.docId = parentDocId + "/" + child.getName();
            entry.name = decoded;
            entry.isDir = child.isDirectory();
            entry.lastModified = child.lastModified();
            if (entry.isDir) {
                entry.mimeType = Document.MIME_TYPE_DIR;
            } else {
                entry.size = VaultCrypto.plaintextSize(child.length());
                entry.mimeType = mimeOf(decoded);
            }
            out.add(entry);
        }
        out.sort(Comparator.comparing((Entry e) -> !e.isDir)
                .thenComparing(e -> e.name.toLowerCase()));
        return out;
    }

    // --- mutations --------------------------------------------------

    static String createDocument(Context context, String parentDocId, String displayName,
                                 String mimeType) throws IOException {
        File parent = resolve(context, parentDocId);
        if (!parent.isDirectory()) throw new IOException("parent not a dir");
        String enc = VaultCrypto.encryptName(nameKey(), uniqueName(context, parentDocId, displayName));
        File target = new File(parent, enc);
        boolean dir = Document.MIME_TYPE_DIR.equals(mimeType);
        try {
            if (dir) {
                if (!target.mkdir()) throw new IOException("mkdir failed");
            } else {
                try (OutputStream out = new FileOutputStream(target)) {
                    VaultCrypto.encryptStream(emptyStream(), out, contentKey());
                }
            }
        } catch (GeneralSecurityException e) {
            throw new IOException(e);
        }
        return parentDocId + "/" + enc;
    }

    static void delete(Context context, String docId) throws FileNotFoundException {
        deleteRecursively(resolve(context, docId));
    }

    static String rename(Context context, String docId, String newName) throws IOException {
        File file = resolve(context, docId);
        String parentDocId = docId.substring(0, docId.lastIndexOf('/'));
        String enc = VaultCrypto.encryptName(nameKey(),
                uniqueName(context, parentDocId, newName));
        File target = new File(file.getParentFile(), enc);
        if (!file.renameTo(target)) throw new IOException("rename failed");
        return parentDocId + "/" + enc;
    }

    // --- content ---------------------------------------------------

    static void decryptToFile(Context context, String docId, File dest) throws IOException {
        File blob = resolve(context, docId);
        try (InputStream in = new FileInputStream(blob);
             OutputStream out = new FileOutputStream(dest)) {
            out.write(VaultCrypto.decryptFully(in, contentKey()));
        } catch (GeneralSecurityException e) {
            throw new IOException("decrypt failed", e);
        }
    }

    static byte[] decryptToMemory(Context context, String docId) throws IOException {
        try (InputStream in = new FileInputStream(resolve(context, docId))) {
            return VaultCrypto.decryptFully(in, contentKey());
        } catch (GeneralSecurityException e) {
            throw new IOException("decrypt failed", e);
        }
    }

    static void encryptFromFile(Context context, File src, String docId) throws IOException {
        File blob = resolve(context, docId);
        File tmp = new File(blob.getParentFile(), blob.getName() + ".wtmp");
        try (InputStream in = new FileInputStream(src);
             OutputStream out = new FileOutputStream(tmp)) {
            VaultCrypto.encryptStream(in, out, contentKey());
        } catch (GeneralSecurityException e) {
            tmp.delete();
            throw new IOException("encrypt failed", e);
        }
        if (!tmp.renameTo(blob)) {
            tmp.delete();
            throw new IOException("swap failed");
        }
    }

    static String importStream(Context context, String parentDocId, String displayName,
                               InputStream plaintext) throws IOException {
        File parent = resolve(context, parentDocId);
        String enc = VaultCrypto.encryptName(nameKey(),
                uniqueName(context, parentDocId, displayName));
        File target = new File(parent, enc);
        try (OutputStream out = new FileOutputStream(target)) {
            VaultCrypto.encryptStream(plaintext, out, contentKey());
        } catch (GeneralSecurityException e) {
            target.delete();
            throw new IOException("encrypt failed", e);
        }
        return parentDocId + "/" + enc;
    }

    static void exportStream(Context context, String docId, OutputStream dest) throws IOException {
        dest.write(decryptToMemory(context, docId));
    }

    // --- helpers --------------------------------------------------

    private static String uniqueName(Context context, String parentDocId, String desired)
            throws FileNotFoundException {
        List<Entry> siblings = list(context, parentDocId);
        String base = desired;
        String ext = "";
        int dot = desired.lastIndexOf('.');
        if (dot > 0) {
            base = desired.substring(0, dot);
            ext = desired.substring(dot);
        }
        String candidate = desired;
        int n = 1;
        while (nameTaken(siblings, candidate)) {
            candidate = base + " (" + (++n) + ")" + ext;
        }
        return candidate;
    }

    private static boolean nameTaken(List<Entry> siblings, String name) {
        for (Entry e : siblings) {
            if (e.name.equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    private static void deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] kids = file.listFiles();
            if (kids != null) {
                for (File kid : kids) deleteRecursively(kid);
            }
        }
        file.delete();
    }

    private static InputStream emptyStream() {
        return new InputStream() {
            @Override
            public int read() {
                return -1;
            }
        };
    }

    static String mimeOf(String name) {
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) return "application/octet-stream";
        String ext = name.substring(dot + 1).toLowerCase();
        String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
        return mime != null ? mime : "application/octet-stream";
    }
}

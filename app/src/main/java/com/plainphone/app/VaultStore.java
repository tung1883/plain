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

    private static Entry toEntry(VaultManifest.Entry me) {
        Entry entry = new Entry();
        entry.docId = me.docId;
        entry.name = me.name;
        entry.isDir = me.isDir;
        entry.size = me.size;
        entry.lastModified = me.mtime;
        entry.mimeType = me.isDir ? Document.MIME_TYPE_DIR : mimeOf(me.name);
        return entry;
    }

    static Entry stat(Context context, String docId) throws FileNotFoundException {
        resolve(context, docId);   // validate the docId shape
        if (docId.equals(ROOT_DOC_ID)) {
            Entry root = new Entry();
            root.docId = ROOT_DOC_ID;
            root.name = "PlainPhone Vault";
            root.isDir = true;
            root.mimeType = Document.MIME_TYPE_DIR;
            return root;
        }
        VaultManifest.Entry me = VaultSession.get().manifest().get(docId);
        if (me == null) throw new FileNotFoundException(docId);
        return toEntry(me);
    }

    static List<Entry> list(Context context, String parentDocId) throws FileNotFoundException {
        resolve(context, parentDocId);   // validate the docId shape
        List<Entry> out = new ArrayList<>();
        for (VaultManifest.Entry me : VaultSession.get().manifest().children(parentDocId)) {
            out.add(toEntry(me));
        }
        sort(out, "name", false);
        return out;
    }

    /** Folders always first; {@code by} is "name" / "size" / "date". */
    static void sort(List<Entry> entries, String by, boolean desc) {
        Comparator<Entry> c;
        switch (by) {
            case "size":
                c = Comparator.comparingLong(e -> e.size);
                break;
            case "date":
                c = Comparator.comparingLong(e -> e.lastModified);
                break;
            default:
                c = Comparator.comparing(e -> e.name.toLowerCase());
        }
        if (desc) c = c.reversed();
        entries.sort(Comparator.comparing((Entry e) -> !e.isDir).thenComparing(c));
    }

    /** Recursive name search over the whole vault. */
    static List<Entry> searchAll(Context context, String needle) {
        List<Entry> hits = new ArrayList<>();
        String lower = needle.toLowerCase();
        collect(context, ROOT_DOC_ID, lower, hits);
        return hits;
    }

    private static void collect(Context context, String docId, String lower, List<Entry> hits) {
        List<Entry> children;
        try {
            children = list(context, docId);
        } catch (FileNotFoundException e) {
            return;
        }
        for (Entry entry : children) {
            if (entry.name.toLowerCase().contains(lower)) hits.add(entry);
            if (entry.isDir) collect(context, entry.docId, lower, hits);
        }
    }

    /** Move an entry under a new parent folder. Returns the new docId. */
    static String move(Context context, String docId, String destParentDocId) throws IOException {
        File file = resolve(context, docId);
        File destParent = resolve(context, destParentDocId);
        if (!destParent.isDirectory()) throw new IOException("destination not a folder");
        if (destParentDocId.equals(docId) || isChild(docId, destParentDocId)) {
            throw new IOException("can't move a folder into itself");
        }
        VaultManifest.Entry me = VaultSession.get().manifest().get(docId);
        String name = me != null ? me.name : VaultCrypto.decryptName(nameKey(), file.getName());
        if (name == null) throw new IOException("not a vault entry");
        String finalName = uniqueName(context, destParentDocId, name);
        String enc = VaultCrypto.encryptName(nameKey(), finalName);
        File target = new File(destParent, enc);
        if (!file.renameTo(target)) throw new IOException("move failed");
        String newDocId = destParentDocId + "/" + enc;
        VaultSession.get().manifest().rekey(docId, newDocId, finalName);
        VaultSession.get().saveManifest(context);
        return newDocId;
    }

    /** Every folder in the vault, depth-first, for a move destination picker. */
    static List<Entry> allFolders(Context context) {
        List<Entry> out = new ArrayList<>();
        collectFolders(context, ROOT_DOC_ID, out);
        return out;
    }

    private static void collectFolders(Context context, String docId, List<Entry> out) {
        List<Entry> children;
        try {
            children = list(context, docId);
        } catch (FileNotFoundException e) {
            return;
        }
        for (Entry entry : children) {
            if (entry.isDir) {
                out.add(entry);
                collectFolders(context, entry.docId, out);
            }
        }
    }

    // --- mutations --------------------------------------------------

    static String createDocument(Context context, String parentDocId, String displayName,
                                 String mimeType) throws IOException {
        File parent = resolve(context, parentDocId);
        if (!parent.isDirectory()) throw new IOException("parent not a dir");
        String finalName = uniqueName(context, parentDocId, displayName);
        String enc = VaultCrypto.encryptName(nameKey(), finalName);
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
        String docId = parentDocId + "/" + enc;
        VaultSession.get().manifest().put(new VaultManifest.Entry(docId, finalName, dir,
                dir ? 0 : VaultCrypto.plaintextSize(target.length()), target.lastModified()));
        VaultSession.get().saveManifest(context);
        return docId;
    }

    static void delete(Context context, String docId) throws FileNotFoundException {
        deleteRecursively(resolve(context, docId));
        VaultSession.get().manifest().remove(docId);
        VaultSession.get().saveManifest(context);
    }

    static String rename(Context context, String docId, String newName) throws IOException {
        File file = resolve(context, docId);
        String parentDocId = docId.substring(0, docId.lastIndexOf('/'));
        String finalName = uniqueName(context, parentDocId, newName);
        String enc = VaultCrypto.encryptName(nameKey(), finalName);
        File target = new File(file.getParentFile(), enc);
        if (!file.renameTo(target)) throw new IOException("rename failed");
        String newDocId = parentDocId + "/" + enc;
        VaultSession.get().manifest().rekey(docId, newDocId, finalName);
        VaultSession.get().saveManifest(context);
        return newDocId;
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

    static void writeText(Context context, String docId, String text) throws IOException {
        byte[] bytes = text.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        File blob = resolve(context, docId);
        File tmp = new File(blob.getParentFile(), blob.getName() + ".wtmp");
        try (InputStream in = new java.io.ByteArrayInputStream(bytes);
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
        touchManifest(context, docId, blob);
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
        touchManifest(context, docId, blob);
    }

    private static void touchManifest(Context context, String docId, File blob) {
        VaultSession.get().manifest().updateContent(docId,
                VaultCrypto.plaintextSize(blob.length()), blob.lastModified());
        VaultSession.get().saveManifest(context);
    }

    // --- bulk import fast path (phase 8) --------------------------

    static void beginBulkImport() {
        VaultSession.get().beginManifestBatch();
    }

    static void endBulkImport(Context context) {
        VaultSession.get().endManifestBatch(context);
    }

    /** Checkpoint the manifest during a bulk import without ending the batch. */
    static void flushBulkImport(Context context) {
        VaultSession.get().flushManifest(context);
    }

    /** Snapshot of the lowercased names already under {@code parentDocId}. */
    static java.util.Set<String> takenNames(Context context, String parentDocId)
            throws FileNotFoundException {
        resolve(context, parentDocId);
        java.util.HashSet<String> set = new java.util.HashSet<>();
        for (VaultManifest.Entry e : VaultSession.get().manifest().children(parentDocId)) {
            set.add(e.name.toLowerCase());
        }
        return set;
    }

    /**
     * Import one file, deduping against {@code taken} (mutated) instead of re-listing.
     * Returns the new docId, or null when the name was already present (caller skips it).
     * The manifest save is the caller's job — wrap a run in {@link #beginBulkImport} /
     * {@link #endBulkImport}.
     */
    static String importStreamInto(Context context, String parentDocId, String name,
                                   InputStream plaintext, java.util.Set<String> taken)
            throws IOException {
        if (taken.contains(name.toLowerCase())) return null;
        File parent = resolve(context, parentDocId);
        String enc = VaultCrypto.encryptName(nameKey(), name);
        File target = new File(parent, enc);
        try (OutputStream out = new FileOutputStream(target)) {
            VaultCrypto.encryptStream(plaintext, out, contentKey());
        } catch (GeneralSecurityException e) {
            target.delete();
            throw new IOException("encrypt failed", e);
        }
        String docId = parentDocId + "/" + enc;
        VaultSession.get().manifest().put(new VaultManifest.Entry(docId, name, false,
                VaultCrypto.plaintextSize(target.length()), target.lastModified()));
        taken.add(name.toLowerCase());
        return docId;
    }

    /** Folder under {@code parentDocId} by name, creating it if absent. Uses {@code taken}. */
    static String ensureDirInto(Context context, String parentDocId, String name,
                                java.util.Set<String> taken) throws IOException {
        if (taken.contains(name.toLowerCase())) {
            String existing = findChild(context, parentDocId, name);
            if (existing != null) return existing;
        }
        File parent = resolve(context, parentDocId);
        String enc = VaultCrypto.encryptName(nameKey(), name);
        File target = new File(parent, enc);
        if (!target.mkdir() && !target.isDirectory()) throw new IOException("mkdir failed");
        String docId = parentDocId + "/" + enc;
        VaultSession.get().manifest().put(new VaultManifest.Entry(docId, name, true, 0,
                target.lastModified()));
        taken.add(name.toLowerCase());
        return docId;
    }

    static String importStream(Context context, String parentDocId, String displayName,
                               InputStream plaintext) throws IOException {
        File parent = resolve(context, parentDocId);
        String finalName = uniqueName(context, parentDocId, displayName);
        String enc = VaultCrypto.encryptName(nameKey(), finalName);
        File target = new File(parent, enc);
        try (OutputStream out = new FileOutputStream(target)) {
            VaultCrypto.encryptStream(plaintext, out, contentKey());
        } catch (GeneralSecurityException e) {
            target.delete();
            throw new IOException("encrypt failed", e);
        }
        String docId = parentDocId + "/" + enc;
        VaultSession.get().manifest().put(new VaultManifest.Entry(docId, finalName, false,
                VaultCrypto.plaintextSize(target.length()), target.lastModified()));
        VaultSession.get().saveManifest(context);
        return docId;
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

    /** The docId of a child named {@code name} under {@code parentDocId}, or null. */
    static String findChild(Context context, String parentDocId, String name)
            throws FileNotFoundException {
        for (Entry e : list(context, parentDocId)) {
            if (e.name.equalsIgnoreCase(name)) return e.docId;
        }
        return null;
    }

    /** Delete a child by name (for "replace on import"). Returns true if something went. */
    static boolean deleteChild(Context context, String parentDocId, String name) {
        try {
            String child = findChild(context, parentDocId, name);
            if (child == null) return false;
            delete(context, child);
            return true;
        } catch (Exception e) {
            return false;
        }
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

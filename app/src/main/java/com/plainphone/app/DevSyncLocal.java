package com.plainphone.app;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Local (phone-side) half of a folder pair: everything is a SAF tree, walked
 * and written through {@link DocumentsContract} directly (no
 * {@code androidx.documentfile} — this app ships zero third-party
 * dependencies) rather than {@code java.io.File}, so the chosen folder can be
 * anywhere the user granted access to, not just primary storage (see
 * {@code VaultLocation}, which takes that File-based shortcut and can't
 * reach non-primary storage — deliberately not reused here).
 */
final class DevSyncLocal {

    private DevSyncLocal() {}

    static final class Entry {
        final String path; // relative, forward-slashed
        final long size;
        final long mtimeMs;
        final String sha256; // null unless requested
        Entry(String path, long size, long mtimeMs, String sha256) {
            this.path = path;
            this.size = size;
            this.mtimeMs = mtimeMs;
            this.sha256 = sha256;
        }
    }

    /** Every file under {@code treeUri}, relative path from the tree root. */
    static List<Entry> walk(Context context, Uri treeUri, boolean hash) {
        List<Entry> out = new ArrayList<>();
        ContentResolver cr = context.getContentResolver();
        String rootDocId = DocumentsContract.getTreeDocumentId(treeUri);
        Deque<String[]> queue = new ArrayDeque<>(); // {docId, relPrefix}
        queue.add(new String[]{rootDocId, ""});
        while (!queue.isEmpty()) {
            String[] dir = queue.poll();
            Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, dir[0]);
            try (Cursor c = cr.query(childrenUri, new String[]{
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                    DocumentsContract.Document.COLUMN_SIZE,
                    DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            }, null, null, null)) {
                if (c == null) continue;
                while (c.moveToNext()) {
                    String docId = c.getString(0);
                    String name = c.getString(1);
                    String mime = c.getString(2);
                    long size = c.getLong(3);
                    long mtime = c.getLong(4);
                    String rel = dir[1].isEmpty() ? name : dir[1] + "/" + name;
                    if (DocumentsContract.Document.MIME_TYPE_DIR.equals(mime)) {
                        queue.add(new String[]{docId, rel});
                    } else {
                        String sha = hash ? hashOf(cr, DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)) : null;
                        out.add(new Entry(rel, size, mtime, sha));
                    }
                }
            }
        }
        return out;
    }

    private static String hashOf(ContentResolver cr, Uri fileUri) {
        try (InputStream in = cr.openInputStream(fileUri)) {
            if (in == null) return null;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buf = new byte[64 * 1024];
            int n;
            while ((n = in.read(buf)) > 0) digest.update(buf, 0, n);
            StringBuilder sb = new StringBuilder();
            for (byte b : digest.digest()) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (IOException | NoSuchAlgorithmException e) {
            return null;
        }
    }

    /** Opens an existing file's content for reading (upload), or {@code null} if missing. */
    static InputStream open(Context context, Uri treeUri, String relPath) {
        Uri fileUri = findFile(context, treeUri, relPath);
        if (fileUri == null) return null;
        try {
            return context.getContentResolver().openInputStream(fileUri);
        } catch (IOException e) {
            return null;
        }
    }

    /** Finds {@code relPath} under the tree, or {@code null} if any segment is missing. */
    static Uri findFile(Context context, Uri treeUri, String relPath) {
        ContentResolver cr = context.getContentResolver();
        String docId = DocumentsContract.getTreeDocumentId(treeUri);
        String[] parts = relPath.split("/");
        for (int i = 0; i < parts.length; i++) {
            String childId = findChild(cr, treeUri, docId, parts[i]);
            if (childId == null) return null;
            docId = childId;
        }
        return DocumentsContract.buildDocumentUriUsingTree(treeUri, docId);
    }

    /** Creates (or reuses) every missing directory segment of {@code relPath}, returning the
     *  parent directory's document Uri the final file segment should be created under. */
    private static Uri ensureParentDir(Context context, Uri treeUri, String relPath) throws IOException {
        ContentResolver cr = context.getContentResolver();
        String docId = DocumentsContract.getTreeDocumentId(treeUri);
        String[] parts = relPath.split("/");
        for (int i = 0; i < parts.length - 1; i++) {
            String childId = findChild(cr, treeUri, docId, parts[i]);
            if (childId == null) {
                Uri parentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId);
                Uri created = DocumentsContract.createDocument(cr, parentUri,
                        DocumentsContract.Document.MIME_TYPE_DIR, parts[i]);
                if (created == null) return null;
                childId = DocumentsContract.getDocumentId(created);
            }
            docId = childId;
        }
        return DocumentsContract.buildDocumentUriUsingTree(treeUri, docId);
    }

    private static String findChild(ContentResolver cr, Uri treeUri, String parentDocId, String name) {
        Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId);
        try (Cursor c = cr.query(childrenUri, new String[]{
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        }, null, null, null)) {
            if (c == null) return null;
            while (c.moveToNext()) {
                if (name.equals(c.getString(1))) return c.getString(0);
            }
        }
        return null;
    }

    /** Creates {@code relPath} (replacing it if it already exists) and returns an output
     *  stream to write its content — the caller closes it. */
    static OutputStream create(Context context, Uri treeUri, String relPath) throws IOException {
        ContentResolver cr = context.getContentResolver();
        Uri existing = findFile(context, treeUri, relPath);
        if (existing != null) {
            DocumentsContract.deleteDocument(cr, existing);
        }
        Uri parent = ensureParentDir(context, treeUri, relPath);
        if (parent == null) throw new IOException("couldn't create parent directories for " + relPath);
        String name = relPath.substring(relPath.lastIndexOf('/') + 1);
        Uri created = DocumentsContract.createDocument(cr, parent, "application/octet-stream", name);
        if (created == null) throw new IOException("couldn't create " + relPath);
        OutputStream out = cr.openOutputStream(created);
        if (out == null) throw new IOException("couldn't open " + relPath + " for writing");
        return out;
    }

    static void delete(Context context, Uri treeUri, String relPath) throws IOException {
        Uri fileUri = findFile(context, treeUri, relPath);
        if (fileUri != null) DocumentsContract.deleteDocument(context.getContentResolver(), fileUri);
    }
}

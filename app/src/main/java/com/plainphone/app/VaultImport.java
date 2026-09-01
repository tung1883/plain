package com.plainphone.app;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.provider.DocumentsContract.Document;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * One-shot recursive import of a user-picked SAF folder tree into the vault.
 * Not a sync: a repeat run copies files that aren't already in the target vault
 * folder by name, and leaves edited / source-deleted files alone. Real
 * change-detection waits for the phase-3 manifest.
 */
final class VaultImport {

    private VaultImport() {}

    static class Result {
        int filesAdded;
        int filesSkipped;
        int errors;
        final List<Uri> importedSources = new ArrayList<>();  // for optional shred
    }

    interface Progress {
        void onProgress(int filesDone, int filesTotal, long bytesDone, long bytesTotal);
    }

    private static class Counter {
        int filesDone;
        int filesTotal;
        long bytesDone;
        long bytesTotal;
    }

    /** Walk {@code treeUri} into {@code destVaultDocId}. Runs on the caller's thread. */
    static Result run(Context context, Uri treeUri, String destVaultDocId, Progress progress) {
        Result result = new Result();
        String rootDocId = DocumentsContract.getTreeDocumentId(treeUri);
        Counter counter = new Counter();
        measure(context, treeUri, rootDocId, counter);
        if (progress != null) {
            progress.onProgress(0, counter.filesTotal, 0, counter.bytesTotal);
        }
        importDir(context, treeUri, rootDocId, destVaultDocId, result, progress, counter);
        return result;
    }

    private static void measure(Context context, Uri treeUri, String dirDocId, Counter counter) {
        Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, dirDocId);
        try (Cursor c = context.getContentResolver().query(childrenUri, new String[]{
                Document.COLUMN_DOCUMENT_ID, Document.COLUMN_MIME_TYPE, Document.COLUMN_SIZE
        }, null, null, null)) {
            if (c == null) return;
            while (c.moveToNext()) {
                if (Document.MIME_TYPE_DIR.equals(c.getString(1))) {
                    measure(context, treeUri, c.getString(0), counter);
                } else {
                    counter.filesTotal++;
                    if (!c.isNull(2)) counter.bytesTotal += c.getLong(2);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static void importDir(Context context, Uri treeUri, String srcDirDocId,
                                  String destVaultDocId, Result result, Progress progress,
                                  Counter counter) {
        ContentResolver resolver = context.getContentResolver();
        Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, srcDirDocId);
        try (Cursor c = resolver.query(childrenUri, new String[]{
                Document.COLUMN_DOCUMENT_ID, Document.COLUMN_DISPLAY_NAME, Document.COLUMN_MIME_TYPE,
                Document.COLUMN_SIZE
        }, null, null, null)) {
            if (c == null) return;
            while (c.moveToNext()) {
                String docId = c.getString(0);
                String name = c.getString(1);
                String mime = c.getString(2);
                long size = c.isNull(3) ? 0 : c.getLong(3);
                if (name == null) continue;

                if (Document.MIME_TYPE_DIR.equals(mime)) {
                    String childVaultDocId = ensureVaultDir(context, destVaultDocId, name);
                    if (childVaultDocId == null) {
                        result.errors++;
                        continue;
                    }
                    importDir(context, treeUri, docId, childVaultDocId, result, progress, counter);
                } else {
                    importFile(context, treeUri, docId, name, destVaultDocId, result);
                    counter.filesDone++;
                    counter.bytesDone += size;
                    if (progress != null) {
                        progress.onProgress(counter.filesDone, counter.filesTotal,
                                counter.bytesDone, counter.bytesTotal);
                    }
                }
            }
        } catch (Exception e) {
            result.errors++;
        }
    }

    private static void importFile(Context context, Uri treeUri, String srcDocId, String name,
                                   String destVaultDocId, Result result) {
        try {
            if (VaultStore.findChild(context, destVaultDocId, name) != null) {
                result.filesSkipped++;
                return;
            }
            Uri fileUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, srcDocId);
            try (InputStream in = context.getContentResolver().openInputStream(fileUri)) {
                if (in == null) {
                    result.errors++;
                    return;
                }
                VaultStore.importStream(context, destVaultDocId, name, in);
            }
            result.filesAdded++;
            result.importedSources.add(DocumentsContract.buildDocumentUriUsingTree(treeUri, srcDocId));
        } catch (Exception e) {
            result.errors++;
        }
    }

    private static String ensureVaultDir(Context context, String parentVaultDocId, String name) {
        try {
            String existing = VaultStore.findChild(context, parentVaultDocId, name);
            if (existing != null) return existing;
            return VaultStore.createDocument(context, parentVaultDocId, name,
                    Document.MIME_TYPE_DIR);
        } catch (Exception e) {
            return null;
        }
    }

    /** Best-effort delete of the source files a run copied in. */
    static int shredSources(Context context, List<Uri> sources) {
        int deleted = 0;
        for (Uri uri : sources) {
            try {
                if (DocumentsContract.deleteDocument(context.getContentResolver(), uri)) deleted++;
            } catch (Exception ignored) {
            }
        }
        return deleted;
    }
}

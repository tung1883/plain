package com.plainphone.app;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.provider.DocumentsContract.Document;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Recursive import of a picked SAF folder, or a flat set of picked files, into a
 * vault folder. Driven by {@link VaultJobService} as a persistent, resumable job:
 * source paths already recorded in {@code done} are skipped, transient failures
 * retry, and the manifest is checkpointed so a kill leaves no orphans.
 */
final class VaultImport {

    private VaultImport() {}

    private static final int RETRIES = 3;
    private static final long[] BACKOFF_MS = {250, 1000, 3000};
    private static final int FLUSH_EVERY = 25;

    /** What to do when an imported name already exists in the destination. */
    enum DupPolicy { KEEP_BOTH, SKIP, REPLACE }

    /** One picked file (from the multi-file picker — a direct document Uri). */
    static final class FileRef {
        final Uri uri;
        final String name;

        FileRef(Uri uri, String name) {
            this.uri = uri;
            this.name = name;
        }
    }

    static class Result {
        int filesAdded;
        int filesSkipped;
        int filesFailed;
    }

    interface Hooks {
        void onScan(int filesFound);
        void onProgress(int filesDone, int filesTotal, long bytesDone, long bytesTotal);
        void onFileSettled(String srcRelPath, boolean failed);
        boolean cancelled();
    }

    private static class Counter {
        int filesDone, filesTotal;
        long bytesDone, bytesTotal;
        int sinceFlush;
        long lastTick;
    }

    /** Display name of the picked tree's root folder — used as the wrapper subfolder name. */
    static String pickedFolderName(Context context, Uri treeUri) {
        String name = nameOf(context,
                DocumentsContract.buildDocumentUriUsingTree(treeUri,
                        DocumentsContract.getTreeDocumentId(treeUri)));
        return name != null ? name : "Imported folder";
    }

    // --- folder import ---------------------------------------

    static Result runFolder(Context context, Uri treeUri, String destParentDocId, String wrapperName,
                            DupPolicy dup, Set<String> done, Hooks hooks) {
        Result result = new Result();
        String rootDocId = DocumentsContract.getTreeDocumentId(treeUri);

        Counter counter = new Counter();
        scanTree(context, treeUri, rootDocId, counter, hooks);
        hooks.onProgress(0, counter.filesTotal, 0, counter.bytesTotal);

        VaultStore.beginBulkImport();
        try {
            String wrapDocId = ensureDestDir(context, destParentDocId, wrapperName, dup);
            if (wrapDocId == null) {
                result.filesFailed = counter.filesTotal;
                return result;
            }
            importDir(context, treeUri, rootDocId, wrapDocId, "", dup, done, result, hooks, counter);
        } finally {
            VaultStore.endBulkImport(context);
        }
        return result;
    }

    private static void scanTree(Context context, Uri treeUri, String dirDocId,
                                 Counter counter, Hooks hooks) {
        Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, dirDocId);
        try (Cursor c = context.getContentResolver().query(childrenUri, new String[]{
                Document.COLUMN_DOCUMENT_ID, Document.COLUMN_MIME_TYPE, Document.COLUMN_SIZE
        }, null, null, null)) {
            if (c == null) return;
            while (c.moveToNext()) {
                if (hooks.cancelled()) return;
                if (Document.MIME_TYPE_DIR.equals(c.getString(1))) {
                    scanTree(context, treeUri, c.getString(0), counter, hooks);
                } else {
                    counter.filesTotal++;
                    if (!c.isNull(2)) counter.bytesTotal += c.getLong(2);
                    if ((counter.filesTotal & 0x3F) == 0) hooks.onScan(counter.filesTotal);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static void importDir(Context context, Uri treeUri, String srcDirDocId,
                                  String destVaultDocId, String relBase, DupPolicy dup,
                                  Set<String> done, Result result, Hooks hooks, Counter counter) {
        List<String[]> dirs = new ArrayList<>();
        List<Object[]> files = new ArrayList<>();

        Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, srcDirDocId);
        try (Cursor c = context.getContentResolver().query(childrenUri, new String[]{
                Document.COLUMN_DOCUMENT_ID, Document.COLUMN_DISPLAY_NAME, Document.COLUMN_MIME_TYPE,
                Document.COLUMN_SIZE
        }, null, null, null)) {
            if (c == null) return;
            while (c.moveToNext()) {
                String docId = c.getString(0);
                String name = c.getString(1);
                if (name == null) continue;
                long size = c.isNull(3) ? 0 : c.getLong(3);
                if (Document.MIME_TYPE_DIR.equals(c.getString(2))) {
                    dirs.add(new String[]{docId, name});
                } else {
                    files.add(new Object[]{docId, name, size});
                }
            }
        } catch (Exception e) {
            return;
        }

        for (Object[] f : files) {
            if (hooks.cancelled()) return;
            String docId = (String) f[0];
            String name = (String) f[1];
            long size = (Long) f[2];
            String rel = relBase.isEmpty() ? name : relBase + "/" + name;
            if (!done.contains(rel)) {
                Uri src = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId);
                boolean failed = !importOne(context, src, name, destVaultDocId, dup, result);
                if (!failed) {
                    done.add(rel);
                    if (++counter.sinceFlush >= FLUSH_EVERY) {
                        counter.sinceFlush = 0;
                        VaultStore.flushBulkImport(context);
                    }
                }
                hooks.onFileSettled(rel, failed);
            }
            counter.filesDone++;
            counter.bytesDone += size;
            tick(hooks, counter);
        }

        for (String[] d : dirs) {
            if (hooks.cancelled()) return;
            String rel = relBase.isEmpty() ? d[1] : relBase + "/" + d[1];
            String childDocId = ensureDestDir(context, destVaultDocId, d[1], dup);
            if (childDocId == null) continue;
            importDir(context, treeUri, d[0], childDocId, rel, dup, done, result, hooks, counter);
        }
        hooks.onProgress(counter.filesDone, counter.filesTotal,
                counter.bytesDone, counter.bytesTotal);
    }

    // --- files import ---------------------------------------

    static Result runFiles(Context context, List<FileRef> refs, String destDocId,
                           DupPolicy dup, Set<String> done, Hooks hooks) {
        Result result = new Result();
        Counter counter = new Counter();
        counter.filesTotal = refs.size();
        for (FileRef r : refs) counter.bytesTotal += sizeOf(context, r.uri);
        hooks.onScan(refs.size());
        hooks.onProgress(0, counter.filesTotal, 0, counter.bytesTotal);

        VaultStore.beginBulkImport();
        try {
            for (FileRef r : refs) {
                if (hooks.cancelled()) break;
                long size = sizeOf(context, r.uri);
                if (!done.contains(r.name)) {
                    boolean failed = !importOne(context, r.uri, r.name, destDocId, dup, result);
                    if (!failed) {
                        done.add(r.name);
                        if (++counter.sinceFlush >= FLUSH_EVERY) {
                            counter.sinceFlush = 0;
                            VaultStore.flushBulkImport(context);
                        }
                    }
                    hooks.onFileSettled(r.name, failed);
                }
                counter.filesDone++;
                counter.bytesDone += size;
                tick(hooks, counter);
            }
        } finally {
            VaultStore.endBulkImport(context);
        }
        return result;
    }

    // --- shared ---------------------------------------------

    private static String ensureDestDir(Context context, String destVaultDocId, String name,
                                        DupPolicy dup) {
        try {
            Set<String> taken = VaultStore.takenNames(context, destVaultDocId);
            if (taken.contains(name.toLowerCase()) && dup == DupPolicy.KEEP_BOTH) {
                name = uniqueAgainst(taken, name);
            }
            return VaultStore.ensureDirInto(context, destVaultDocId, name, taken);
        } catch (Exception e) {
            return null;
        }
    }

    /** @return true if handled (added or intentionally skipped), false if it failed. */
    private static boolean importOne(Context context, Uri src, String name, String destVaultDocId,
                                     DupPolicy dup, Result result) {
        Set<String> taken;
        try {
            taken = VaultStore.takenNames(context, destVaultDocId);
        } catch (Exception e) {
            taken = new HashSet<>();
        }

        if (taken.contains(name.toLowerCase())) {
            switch (dup) {
                case SKIP:
                    result.filesSkipped++;
                    return true;
                case KEEP_BOTH:
                    name = uniqueAgainst(taken, name);
                    break;
                case REPLACE:
                    VaultStore.deleteChild(context, destVaultDocId, name);
                    taken.remove(name.toLowerCase());
                    break;
            }
        }

        IOException last = null;
        for (int attempt = 0; attempt < RETRIES; attempt++) {
            if (attempt > 0) sleep(BACKOFF_MS[attempt - 1]);
            try (InputStream in = context.getContentResolver().openInputStream(src)) {
                if (in == null) { last = new IOException("no stream"); continue; }
                if (VaultStore.importStreamInto(context, destVaultDocId, name, in, taken) == null) {
                    result.filesSkipped++;
                    return true;
                }
                result.filesAdded++;
                return true;
            } catch (IOException e) {
                last = e;
            } catch (Exception e) {
                last = new IOException(e);
            }
        }
        android.util.Log.w("VaultImport", "gave up on " + name, last);
        result.filesFailed++;
        return false;
    }

    static long sizeOf(Context context, Uri docUri) {
        try (Cursor c = context.getContentResolver().query(docUri,
                new String[]{Document.COLUMN_SIZE}, null, null, null)) {
            if (c != null && c.moveToFirst() && !c.isNull(0)) return c.getLong(0);
        } catch (Exception ignored) {
        }
        return 0;
    }

    static String nameOf(Context context, Uri docUri) {
        try (Cursor c = context.getContentResolver().query(docUri,
                new String[]{Document.COLUMN_DISPLAY_NAME}, null, null, null)) {
            if (c != null && c.moveToFirst() && !c.isNull(0)) return c.getString(0);
        } catch (Exception ignored) {
        }
        return null;
    }

    private static String uniqueAgainst(Set<String> takenLower, String name) {
        if (!takenLower.contains(name.toLowerCase())) return name;
        String base = name, ext = "";
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            base = name.substring(0, dot);
            ext = name.substring(dot);
        }
        for (int n = 2; ; n++) {
            String cand = base + " (" + n + ")" + ext;
            if (!takenLower.contains(cand.toLowerCase())) return cand;
        }
    }

    private static void tick(Hooks hooks, Counter counter) {
        long now = android.os.SystemClock.uptimeMillis();
        if (now - counter.lastTick >= 200) {
            counter.lastTick = now;
            hooks.onProgress(counter.filesDone, counter.filesTotal,
                    counter.bytesDone, counter.bytesTotal);
        }
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}

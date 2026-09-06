package com.plainphone.app;

import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Vault job facade over the global {@link JobQueue}. Kept as the vault UI API.
 */
final class VaultJobs {

    private VaultJobs() {}

    static final String TYPE_RESET = "vault.reset";
    static final String TYPE_IMPORT = "vault.import";
    static final String TYPE_IMPORT_FOLDER = "vault.import.folder";
    static final String TYPE_IMPORT_FILES = "vault.import.files";

    static volatile boolean cancelImportRequested;

    // --- listeners ------------------------------------------------

    interface Listener { void onVaultJobChanged(); }

    private static final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();

    static void addListener(Listener l) { listeners.addIfAbsent(l); }
    static void removeListener(Listener l) { listeners.remove(l); }

    // --- live snapshot (UI + notification) ------------------------

    static class Snapshot {
        String type;
        String importId;
        String folderName;
        String destParentDocId;
        boolean scanning;
        long doneBytes, totalBytes;
        int doneFiles, totalFiles;
        int failed;
        int deleted, total;
    }

    static volatile Snapshot snapshot;
    static volatile VaultImport.Result lastImport;
    private static long lastFanout;

    static void publish(Snapshot s) {
        snapshot = s;
        long now = android.os.SystemClock.uptimeMillis();
        if (now - lastFanout < 350) return;
        lastFanout = now;
        for (Listener l : listeners) l.onVaultJobChanged();
        JobQueue.publish();
    }

    static void publishNow(Snapshot s) {
        snapshot = s;
        lastFanout = android.os.SystemClock.uptimeMillis();
        for (Listener l : listeners) l.onVaultJobChanged();
        JobQueue.publish();
    }

    static void clearSnapshot() {
        snapshot = null;
        lastFanout = 0;
        for (Listener l : listeners) l.onVaultJobChanged();
        JobQueue.publish();
    }

    // --- state queries -------------------------------------------

    static boolean resetPending(Context c) {
        migrateLegacy(c);
        return JobQueue.anyOfType(c, TYPE_RESET);
    }

    static boolean importPending(Context c) {
        return !pendingImports(c).isEmpty();
    }

    static boolean anyPending(Context c) {
        migrateLegacy(c);
        return JobQueue.anyOfType(c, TYPE_RESET) || JobQueue.anyWithPrefix(c, TYPE_IMPORT);
    }

    static String activeLabel(Context c) {
        java.util.List<Import> q = pendingImports(c);
        if (q.isEmpty()) return resetPending(c) ? "resetting" : null;
        Snapshot s = snapshot;
        String running = s != null && TYPE_IMPORT.equals(s.type) ? s.folderName : q.get(0).folderName;
        if (running == null) running = "a folder";
        return q.size() == 1 ? "importing " + running
                : "importing " + running + " (+" + (q.size() - 1) + " more)";
    }

    // --- start / resume -----------------------------------------

    static void startReset(Context context) {
        cancelImportRequested = importPending(context);
        VaultReset.lockForWipe(context);
        JobQueue.enqueue(context, new JobQueue.Spec(TYPE_RESET)
                .label("Resetting the vault")
                .priority(100)
                .keep(JobQueue.AREA_VAULT));
    }

    static void startImport(Context context, Uri treeUri, String destParentDocId,
                            VaultImport.DupPolicy dup) {
        String label = VaultImport.pickedFolderName(context, treeUri);
        JobQueue.enqueue(context, new JobQueue.Spec(TYPE_IMPORT_FOLDER)
                .label(label)
                .keep(JobQueue.AREA_VAULT)
                .require(JobQueue.AREA_VAULT)
                .put("tree", treeUri.toString())
                .put("dest", destParentDocId)
                .put("name", label)
                .put("dup", dup.name()));
    }

    static void startImportFiles(Context context, String destParentDocId,
                                 java.util.List<VaultImport.FileRef> files,
                                 VaultImport.DupPolicy dup, String label) {
        if (files == null || files.isEmpty()) return;
        StringBuilder sb = new StringBuilder();
        for (VaultImport.FileRef f : files) {
            sb.append(f.uri).append('\t').append(f.name).append('\n');
        }
        JobQueue.enqueue(context, new JobQueue.Spec(TYPE_IMPORT_FILES)
                .label(label)
                .keep(JobQueue.AREA_VAULT)
                .require(JobQueue.AREA_VAULT)
                .put("dest", destParentDocId)
                .put("name", label)
                .put("dup", dup.name())
                .file("files", sb.toString()));
    }

    static void resumeIfPending(Context context) {
        migrateLegacy(context);
        JobQueue.resumeIfPending(context);
    }

    // --- import queue ------------------------------------------

    static class Import {
        String id;
        Uri treeUri;
        String destParentDocId;
        String folderName;
        VaultImport.DupPolicy dup = VaultImport.DupPolicy.KEEP_BOTH;
        java.util.List<VaultImport.FileRef> files;
    }

    static boolean isImportType(String type) {
        return TYPE_IMPORT_FOLDER.equals(type) || TYPE_IMPORT_FILES.equals(type);
    }

    static java.util.List<Import> pendingImports(Context context) {
        migrateLegacy(context);
        java.util.List<Import> out = new java.util.ArrayList<>();
        for (JobQueue.Job job : JobQueue.pendingByPrefix(context, TYPE_IMPORT)) {
            Import im = importFrom(context, job);
            if (im != null) out.add(im);
        }
        return out;
    }

    static Import importFrom(Context context, JobQueue.Job job) {
        if (job == null || !isImportType(job.type)) return null;
        Import out = new Import();
        out.id = job.id;
        out.destParentDocId = job.data.get("dest");
        out.folderName = job.data.get("name");
        String dup = job.data.get("dup");
        if (dup != null) {
            try { out.dup = VaultImport.DupPolicy.valueOf(dup); } catch (Exception ignored) {}
        }
        String tree = job.data.get("tree");
        if (tree != null) out.treeUri = Uri.parse(tree);
        if (TYPE_IMPORT_FILES.equals(job.type)) {
            out.files = new java.util.ArrayList<>();
            for (String line : JobQueue.readText(context, job.id, "files").split("\n")) {
                String[] p = line.split("\t", 2);
                if (p.length == 2) out.files.add(new VaultImport.FileRef(Uri.parse(p[0]), p[1]));
            }
        }
        if (out.destParentDocId == null) return null;
        if (out.treeUri == null && out.files == null) return null;
        return out;
    }

    static Set<String> readDone(Context context, String importId) {
        Set<String> set = new LinkedHashSet<>();
        for (String line : JobQueue.readText(context, importId, "done").split("\n")) {
            if (!line.isEmpty()) set.add(line);
        }
        return set;
    }

    static void writeDone(Context context, String importId, Set<String> done) {
        StringBuilder sb = new StringBuilder();
        for (String rel : done) sb.append(rel).append('\n');
        JobQueue.writeText(context, importId, "done", sb.toString());
    }

    static void clearImport(Context context, String importId) {
        JobQueue.clear(context, importId);
    }

    static void clearReset(Context context) {
        for (JobQueue.Job job : JobQueue.pendingByType(context, TYPE_RESET)) {
            JobQueue.clear(context, job.id);
        }
    }

    static void migrateLegacy(Context context) {
        File old = new File(context.getFilesDir(), "vault-jobs");
        if (!old.exists()) return;
        if (new File(old, "reset.job").exists() && !JobQueue.anyOfType(context, TYPE_RESET)) {
            JobQueue.enqueueMigrated(context, new JobQueue.Spec(TYPE_RESET)
                    .label("Resetting the vault")
                    .priority(100)
                    .keep(JobQueue.AREA_VAULT));
        }
        File importDir = new File(old, "import");
        File[] kids = importDir.listFiles();
        if (kids != null) {
            java.util.Arrays.sort(kids, (a, b) -> Long.compare(a.lastModified(), b.lastModified()));
            for (File d : kids) {
                if (!d.isDirectory()) continue;
                LegacyImport legacy = readLegacyImport(d);
                if (legacy == null) continue;
                JobQueue.Spec spec = new JobQueue.Spec(legacy.filesText == null
                        ? TYPE_IMPORT_FOLDER : TYPE_IMPORT_FILES)
                        .label(legacy.name)
                        .keep(JobQueue.AREA_VAULT)
                        .require(JobQueue.AREA_VAULT)
                        .put("tree", legacy.tree)
                        .put("dest", legacy.dest)
                        .put("name", legacy.name)
                        .put("dup", legacy.dup);
                if (legacy.filesText != null) spec.file("files", legacy.filesText);
                JobQueue.Job job = JobQueue.enqueueMigrated(context, spec);
                String done = readAll(new File(d, "done"));
                if (!done.isEmpty()) JobQueue.writeText(context, job.id, "done", done);
            }
        }
        deleteRecursively(old);
    }

    private static final class LegacyImport {
        String tree;
        String dest;
        String name;
        String dup = VaultImport.DupPolicy.KEEP_BOTH.name();
        String filesText;
    }

    private static LegacyImport readLegacyImport(File dir) {
        File jobFile = new File(dir, "job");
        if (!jobFile.exists()) return null;
        LegacyImport out = new LegacyImport();
        for (String line : readAll(jobFile).split("\n")) {
            int eq = line.indexOf('=');
            if (eq < 0) continue;
            String k = line.substring(0, eq);
            String v = line.substring(eq + 1);
            if ("tree".equals(k)) out.tree = v;
            else if ("dest".equals(k)) out.dest = v;
            else if ("name".equals(k)) out.name = v;
            else if ("dup".equals(k)) out.dup = v;
        }
        File filesFile = new File(dir, "files");
        if (filesFile.exists()) out.filesText = readAll(filesFile);
        if (out.dest == null) return null;
        if (out.tree == null && out.filesText == null) return null;
        if (out.name == null) out.name = "Import";
        return out;
    }

    private static String readAll(File f) {
        return JobQueue.readAll(f);
    }

    private static void deleteRecursively(File file) {
        if (file == null || !file.exists()) return;
        if (file.isDirectory()) {
            File[] kids = file.listFiles();
            if (kids != null) for (File kid : kids) deleteRecursively(kid);
        }
        file.delete();
    }
}

package com.plainphone.app;

import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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
    static final String TYPE_MOVE_LOCATION = "vault.move-location";
    static final String TYPE_DELETE = "vault.delete";
    static final String TYPE_MOVE = "vault.move";
    static final String TYPE_EXPORT_FILE = "vault.export.file";
    static final String TYPE_EXPORT_TREE = "vault.export.tree";
    static final String TYPE_CHANGE_PASSWORD = "vault.change-password";

    static volatile boolean cancelImportRequested;
    private static final ConcurrentHashMap<String, char[]> passphraseJobs = new ConcurrentHashMap<>();

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
        String label;
        int done;
    }

    static volatile Snapshot snapshot;
    static volatile VaultImport.Result lastImport;
    static volatile Result lastResult;
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
        return JobQueue.anyWithPrefix(c, "vault.");
    }

    static String activeLabel(Context c) {
        java.util.List<Import> q = pendingImports(c);
        if (q.isEmpty()) {
            if (resetPending(c)) return "resetting";
            for (JobQueue.Job job : JobQueue.pendingByPrefix(c, "vault.")) {
                return job.label != null ? job.label.toLowerCase() : job.type;
            }
            return null;
        }
        Snapshot s = snapshot;
        String running = s != null && TYPE_IMPORT.equals(s.type) ? s.folderName : q.get(0).folderName;
        if (running == null) running = "a folder";
        return q.size() == 1 ? "importing " + running
                : "importing " + running + " (+" + (q.size() - 1) + " more)";
    }

    static Result takeResult() {
        Result r = lastResult;
        lastResult = null;
        return r;
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

    static void startMoveLocation(Context context, File current, File target, String newConfigPath,
                                  boolean moveExistingVault) {
        JobQueue.Spec spec = new JobQueue.Spec(TYPE_MOVE_LOCATION)
                .label(moveExistingVault ? "Moving the vault" : "Setting vault location")
                .put("current", current.getAbsolutePath())
                .put("target", target.getAbsolutePath())
                .put("move", Boolean.toString(moveExistingVault));
        if (newConfigPath != null) spec.put("config", newConfigPath);
        JobQueue.enqueue(context, spec);
    }

    static void startDelete(Context context, java.util.List<String> docIds, String label) {
        if (docIds == null || docIds.isEmpty()) return;
        JobQueue.enqueue(context, new JobQueue.Spec(TYPE_DELETE)
                .label(label)
                .keep(JobQueue.AREA_VAULT)
                .require(JobQueue.AREA_VAULT)
                .file("ids", lines(docIds)));
    }

    static void startMove(Context context, java.util.List<String> docIds, String destParent,
                          String label) {
        if (docIds == null || docIds.isEmpty()) return;
        JobQueue.enqueue(context, new JobQueue.Spec(TYPE_MOVE)
                .label(label)
                .keep(JobQueue.AREA_VAULT)
                .require(JobQueue.AREA_VAULT)
                .put("dest", destParent)
                .file("ids", lines(docIds)));
    }

    static void startExportFile(Context context, String docId, Uri dest, String label) {
        if (docId == null || dest == null) return;
        JobQueue.enqueue(context, new JobQueue.Spec(TYPE_EXPORT_FILE)
                .label(label)
                .keep(JobQueue.AREA_VAULT)
                .require(JobQueue.AREA_VAULT)
                .put("doc", docId)
                .put("dest", dest.toString()));
    }

    static void startExportTree(Context context, java.util.List<String> docIds, Uri treeUri,
                                String label) {
        if (docIds == null || docIds.isEmpty() || treeUri == null) return;
        JobQueue.enqueue(context, new JobQueue.Spec(TYPE_EXPORT_TREE)
                .label(label)
                .keep(JobQueue.AREA_VAULT)
                .require(JobQueue.AREA_VAULT)
                .put("tree", treeUri.toString())
                .file("ids", lines(docIds)));
    }

    static void startChangePassword(Context context, char[] newPassphrase) {
        if (newPassphrase == null || newPassphrase.length == 0) return;
        JobQueue.Job job = JobQueue.enqueuePrepared(context, new JobQueue.Spec(TYPE_CHANGE_PASSWORD)
                .label("Changing vault password")
                .keep(JobQueue.AREA_VAULT)
                .require(JobQueue.AREA_VAULT));
        passphraseJobs.put(job.id, java.util.Arrays.copyOf(newPassphrase, newPassphrase.length));
        JobQueue.kick(context);
    }

    static char[] takePassphrase(String jobId) {
        return passphraseJobs.remove(jobId);
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

    static java.util.List<String> readIds(Context context, String jobId) {
        java.util.List<String> out = new java.util.ArrayList<>();
        for (String line : JobQueue.readText(context, jobId, "ids").split("\n")) {
            if (!line.isEmpty()) out.add(line);
        }
        return out;
    }

    static Set<String> readDoneIds(Context context, String jobId) {
        Set<String> out = new LinkedHashSet<>();
        for (String line : JobQueue.readText(context, jobId, "done").split("\n")) {
            if (!line.isEmpty()) out.add(line);
        }
        return out;
    }

    static void writeDoneIds(Context context, String jobId, Set<String> done) {
        StringBuilder sb = new StringBuilder();
        for (String id : done) sb.append(id).append('\n');
        JobQueue.writeText(context, jobId, "done", sb.toString());
    }

    static void finish(Context context, JobQueue.Job job, boolean ok, String message) {
        lastResult = new Result(job.type, ok, message);
        JobQueue.clear(context, job.id);
    }

    static final class Result {
        final String type;
        final boolean ok;
        final String message;

        Result(String type, boolean ok, String message) {
            this.type = type;
            this.ok = ok;
            this.message = message;
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

    private static String lines(java.util.List<String> values) {
        StringBuilder sb = new StringBuilder();
        for (String value : values) sb.append(value).append('\n');
        return sb.toString();
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

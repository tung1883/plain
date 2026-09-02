package com.plainphone.app;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Persistent, resumable background jobs for the vault — a full {@link VaultReset}
 * and a recursive {@link VaultImport}. At most one runs at a time, executed by
 * {@link VaultJobService}. Each job's state lives in a small file under
 * {@code files/vault-jobs/}; the file exists for exactly as long as the job is
 * unfinished, so an interrupted job is picked up and completed on next launch.
 */
final class VaultJobs {

    private VaultJobs() {}

    static final String TYPE_RESET = "reset";
    static final String TYPE_IMPORT = "import";

    // --- listeners ------------------------------------------------

    interface Listener { void onVaultJobChanged(); }

    private static final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();

    static void addListener(Listener l) { listeners.addIfAbsent(l); }
    static void removeListener(Listener l) { listeners.remove(l); }

    // --- live snapshot (UI + notification) ------------------------

    static class Snapshot {
        String type;
        String importId;            // import — which queued import is running
        String folderName;          // import
        String destParentDocId;     // import
        boolean scanning;           // import — still measuring, total not final
        long doneBytes, totalBytes; // import
        int doneFiles, totalFiles;  // import
        int failed;                 // import
        int deleted, total;         // reset
    }

    static volatile Snapshot snapshot;

    /** Result of the last finished import, for a one-time summary in the UI. */
    static volatile VaultImport.Result lastImport;

    private static long lastFanout;

    /** Progress tick: snapshot is always current, but listeners are woken at most ~3×/s. */
    static void publish(Snapshot s) {
        snapshot = s;
        long now = android.os.SystemClock.uptimeMillis();
        if (now - lastFanout < 350) return;
        lastFanout = now;
        for (Listener l : listeners) l.onVaultJobChanged();
    }

    /** Start / state-change tick: wake listeners now. */
    static void publishNow(Snapshot s) {
        snapshot = s;
        lastFanout = android.os.SystemClock.uptimeMillis();
        for (Listener l : listeners) l.onVaultJobChanged();
    }

    static void clearSnapshot() {
        snapshot = null;
        lastFanout = 0;
        for (Listener l : listeners) l.onVaultJobChanged();
    }

    // --- state queries -------------------------------------------

    static boolean resetPending(Context c) { return file(c, "reset.job").exists(); }

    static boolean importPending(Context c) { return !pendingImports(c).isEmpty(); }

    static boolean anyPending(Context c) { return resetPending(c) || importPending(c); }

    /** Short "what's running" phrase for the lock-confirm dialog, or null when idle. */
    static String activeLabel(Context c) {
        java.util.List<Import> q = pendingImports(c);
        if (q.isEmpty()) return null;
        Snapshot s = snapshot;
        String running = s != null && TYPE_IMPORT.equals(s.type) ? s.folderName : q.get(0).folderName;
        if (running == null) running = "a folder";
        return q.size() == 1 ? "importing " + running
                : "importing " + running + " (+" + (q.size() - 1) + " more)";
    }

    // --- start / resume -----------------------------------------

    static void startReset(Context context) {
        VaultJobService.cancelImportRequested = importPending(context);
        VaultReset.lockForWipe(context);
        writeLines(file(context, "reset.job"), "type=reset");
        kick(context);
    }

    /** Queue a recursive folder import (into a subfolder named after the source). */
    static void startImport(Context context, Uri treeUri, String destParentDocId,
                            VaultImport.DupPolicy dup) {
        String label = VaultImport.pickedFolderName(context, treeUri);
        File d = newImportDir(context);
        writeLines(new File(d, "job"),
                "tree=" + treeUri,
                "dest=" + destParentDocId,
                "name=" + label,
                "dup=" + dup.name());
        kick(context);
    }

    /** Queue a flat import of picked files into {@code destParentDocId}. */
    static void startImportFiles(Context context, String destParentDocId,
                                 java.util.List<VaultImport.FileRef> files,
                                 VaultImport.DupPolicy dup, String label) {
        if (files == null || files.isEmpty()) return;
        File d = newImportDir(context);
        writeLines(new File(d, "job"),
                "dest=" + destParentDocId,
                "name=" + label,
                "dup=" + dup.name());
        StringBuilder sb = new StringBuilder();
        for (VaultImport.FileRef f : files) {
            sb.append(f.uri).append('\t').append(f.name).append('\n');
        }
        try {
            atomicWrite(new File(d, "files"), sb.toString());
        } catch (IOException e) {
            android.util.Log.w("VaultJobs", "files write failed", e);
        }
        kick(context);
    }

    private static File newImportDir(Context context) {
        long stamp = System.currentTimeMillis();
        File d = new File(importQueueDir(context), Long.toString(stamp));
        for (int i = 1; d.exists(); i++) d = new File(importQueueDir(context), stamp + "-" + i);
        d.mkdirs();
        return d;
    }

    static void resumeIfPending(Context context) {
        if (anyPending(context)) kick(context);
    }

    private static void kick(Context context) {
        context.startForegroundService(new Intent(context, VaultJobService.class));
    }

    // --- import queue ------------------------------------------

    static class Import {
        String id;
        Uri treeUri;                 // folder import; null for a files import
        String destParentDocId;
        String folderName;           // label for the progress row
        VaultImport.DupPolicy dup = VaultImport.DupPolicy.KEEP_BOTH;
        java.util.List<VaultImport.FileRef> files;   // files import; null for a folder import
    }

    private static File importQueueDir(Context context) {
        File d = new File(dir(context), "import");
        d.mkdirs();
        return d;
    }

    /** Queued imports, oldest first. */
    static java.util.List<Import> pendingImports(Context context) {
        java.util.List<Import> out = new java.util.ArrayList<>();
        File[] kids = importQueueDir(context).listFiles();
        if (kids == null) return out;
        java.util.Arrays.sort(kids, (a, b) -> Long.compare(a.lastModified(), b.lastModified()));
        for (File d : kids) {
            if (!d.isDirectory()) continue;
            Import im = readImport(d);
            if (im != null) {
                im.id = d.getName();
                out.add(im);
            }
        }
        return out;
    }

    private static Import readImport(File dir) {
        File jobFile = new File(dir, "job");
        if (!jobFile.exists()) return null;
        Import out = new Import();
        for (String line : readAll(jobFile).split("\n")) {
            int eq = line.indexOf('=');
            if (eq < 0) continue;
            String k = line.substring(0, eq);
            String v = line.substring(eq + 1);
            if ("tree".equals(k)) out.treeUri = Uri.parse(v);
            else if ("dest".equals(k)) out.destParentDocId = v;
            else if ("name".equals(k)) out.folderName = v;
            else if ("dup".equals(k)) {
                try { out.dup = VaultImport.DupPolicy.valueOf(v); } catch (Exception ignored) {}
            }
        }
        File filesFile = new File(dir, "files");
        if (filesFile.exists()) {
            out.files = new java.util.ArrayList<>();
            for (String line : readAll(filesFile).split("\n")) {
                String[] p = line.split("\t", 2);
                if (p.length == 2) {
                    out.files.add(new VaultImport.FileRef(Uri.parse(p[0]), p[1]));
                }
            }
        }
        if (out.destParentDocId == null) return null;
        if (out.treeUri == null && out.files == null) return null;
        return out;
    }

    static Set<String> readDone(Context context, String importId) {
        Set<String> set = new LinkedHashSet<>();
        File f = new File(new File(importQueueDir(context), importId), "done");
        if (!f.exists()) return set;
        for (String line : readAll(f).split("\n")) {
            if (!line.isEmpty()) set.add(line);
        }
        return set;
    }

    /** Persist the whole done-set (called on checkpoints, not per file). */
    static void writeDone(Context context, String importId, Set<String> done) {
        StringBuilder sb = new StringBuilder();
        for (String rel : done) sb.append(rel).append('\n');
        try {
            atomicWrite(new File(new File(importQueueDir(context), importId), "done"), sb.toString());
        } catch (IOException e) {
            android.util.Log.w("VaultJobs", "done-set write failed", e);
        }
    }

    static void clearImport(Context context, String importId) {
        File d = new File(importQueueDir(context), importId);
        File[] kids = d.listFiles();
        if (kids != null) for (File f : kids) f.delete();
        d.delete();
    }

    static void clearReset(Context context) {
        file(context, "reset.job").delete();
    }

    // --- files -------------------------------------------------

    private static File dir(Context context) {
        File d = new File(context.getFilesDir(), "vault-jobs");
        d.mkdirs();
        return d;
    }

    private static File file(Context context, String name) {
        return new File(dir(context), name);
    }

    private static void writeLines(File f, String... lines) {
        try {
            atomicWrite(f, String.join("\n", lines));
        } catch (IOException e) {
            android.util.Log.w("VaultJobs", "job record write failed", e);
        }
    }

    private static void atomicWrite(File f, String content) throws IOException {
        File tmp = new File(f.getParentFile(), f.getName() + ".tmp");
        try (FileOutputStream out = new FileOutputStream(tmp)) {
            out.write(content.getBytes(StandardCharsets.UTF_8));
            out.getFD().sync();
        }
        if (!tmp.renameTo(f)) {
            try (FileOutputStream out = new FileOutputStream(f)) {
                out.write(content.getBytes(StandardCharsets.UTF_8));
                out.getFD().sync();
            }
            tmp.delete();
        }
    }

    private static String readAll(File f) {
        try (java.io.FileInputStream in = new java.io.FileInputStream(f)) {
            byte[] buf = new byte[(int) f.length()];
            int n = 0;
            while (n < buf.length) {
                int r = in.read(buf, n, buf.length - n);
                if (r < 0) break;
                n += r;
            }
            return new String(buf, 0, n, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }
}

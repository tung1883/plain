package com.plainphone.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.DocumentsContract;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/** Runs the global persistent queue one job at a time. */
public class JobService extends Service {

    private static final String CHANNEL_ID = "plain_jobs";
    private static final int NOTIF_ID = 0x4a51; // 'JQ'
    private static final int IMPORT_CHECKPOINT_EVERY = 5;

    private final Handler main = new Handler(Looper.getMainLooper());
    private volatile boolean running;
    private VaultSession.Listener unlockWatcher;
    private long lastNotif;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIF_ID, notif("PlainPhone", "Working...", 0));
        maybeStart();
        return START_STICKY;
    }

    private void maybeStart() {
        if (running) return;
        Context app = getApplicationContext();
        VaultJobs.migrateLegacy(app);
        ImportJobs.migrateLegacy(app);
        JobQueue.Job job = JobQueue.first(app);
        if (job == null) {
            finishAll();
            return;
        }

        if (requiresLockedVault(job) && !VaultSession.get().isUnlocked()) {
            parkForVaultUnlock(job);
            return;
        }

        running = true;
        JobQueue.setStatus(app, job.id, JobQueue.STATUS_RUNNING);
        if (VaultJobs.TYPE_RESET.equals(job.type)) {
            new Thread(() -> runVaultReset(job), "job-vault-reset").start();
        } else if (VaultJobs.isImportType(job.type)) {
            new Thread(() -> runVaultImport(job), "job-vault-import").start();
        } else if (VaultJobs.TYPE_MOVE_LOCATION.equals(job.type)) {
            new Thread(() -> runVaultMoveLocation(job), "job-vault-location").start();
        } else if (VaultJobs.TYPE_DELETE.equals(job.type)) {
            new Thread(() -> runVaultDelete(job), "job-vault-delete").start();
        } else if (VaultJobs.TYPE_MOVE.equals(job.type)) {
            new Thread(() -> runVaultMove(job), "job-vault-move").start();
        } else if (VaultJobs.TYPE_EXPORT_FILE.equals(job.type)) {
            new Thread(() -> runVaultExportFile(job), "job-vault-export").start();
        } else if (VaultJobs.TYPE_EXPORT_TREE.equals(job.type)) {
            new Thread(() -> runVaultExportTree(job), "job-vault-export").start();
        } else if (VaultJobs.TYPE_CHANGE_PASSWORD.equals(job.type)) {
            new Thread(() -> runVaultChangePassword(job), "job-vault-password").start();
        } else if (ImportJobs.isImportType(job.type)) {
            new Thread(() -> runPluginImport(job), "job-import").start();
        } else if (SectionJobs.isType(job.type)) {
            new Thread(() -> runSectionJob(job), "job-section").start();
        } else if (SearchJobs.isType(job.type)) {
            new Thread(() -> runSearchJob(job), "job-search").start();
        } else if (ChessPuzzleJobs.TYPE_PUZZLEGEN.equals(job.type)) {
            new Thread(() -> runChessPuzzleGen(job), "job-chess-puzzlegen").start();
        } else if (ChessImportJobs.TYPE_PGN_IMPORT.equals(job.type)) {
            new Thread(() -> runChessPgnImport(job), "job-chess-pgnimport").start();
        } else if (DevSyncJobs.TYPE_SYNC.equals(job.type)) {
            new Thread(() -> runDevSync(job), "job-dev-sync").start();
        } else {
            android.util.Log.w("JobService", "Dropping unknown job type " + job.type);
            JobQueue.clear(app, job.id);
            running = false;
            main.post(this::maybeStart);
        }
    }

    private boolean requiresLockedVault(JobQueue.Job job) {
        return job.requiredUnlockedAreas.contains(JobQueue.AREA_VAULT);
    }

    private void parkForVaultUnlock(JobQueue.Job job) {
        JobQueue.setStatus(getApplicationContext(), job.id, JobQueue.STATUS_PAUSED);
        push(notif("Job paused", "Unlock the vault to continue", 0));
        if (unlockWatcher == null) {
            unlockWatcher = () -> main.post(this::maybeStart);
            VaultSession.get().addListener(unlockWatcher);
        }
    }

    private void keepAlive(JobQueue.Job job) {
        for (String area : job.keepUnlockedAreas) {
            if (JobQueue.AREA_NOTES.equals(area)) Lock.NOTES.keepUnlocked(this);
            else if (JobQueue.AREA_TODOS.equals(area)) Lock.TODOS.keepUnlocked(this);
            else if (JobQueue.AREA_RECORDER.equals(area)) Lock.RECORDER.keepUnlocked(this);
            else if (JobQueue.AREA_VAULT.equals(area) && VaultSession.get().isUnlocked()) {
                VaultUnlockService.touch(this);
            }
        }
    }

    // --- vault reset ------------------------------------------------------

    private void runVaultReset(JobQueue.Job job) {
        Context app = getApplicationContext();
        VaultJobs.Snapshot snap = new VaultJobs.Snapshot();
        snap.type = VaultJobs.TYPE_RESET;

        try {
            VaultReset.wipe(app, new VaultReset.Progress() {
                @Override public void onProgress(int deleted, int total) {
                    snap.deleted = deleted;
                    snap.total = total;
                    VaultJobs.publish(snap);
                    int pct = total > 0 ? (int) (100L * deleted / total) : 0;
                    throttledNotif(notif("Resetting the vault", pct + "%", pct));
                }
                @Override public boolean cancelled() { return false; }
            });
        } catch (Exception e) {
            android.util.Log.e("JobService", "vault reset failed", e);
        }

        VaultJobs.clearReset(app);
        running = false;
        main.post(this::maybeStart);
    }

    // --- section vault bridge jobs ----------------------------------------

    private void runSectionJob(JobQueue.Job job) {
        Context app = getApplicationContext();
        List<String> ids = SectionJobs.readIds(app, job.id);
        Set<String> done = SectionJobs.readDone(app, job.id);
        int moved = 0;
        int total = ids.size();
        ImportJobs.Snapshot snap = new ImportJobs.Snapshot();
        snap.plugin = job.type.startsWith("notes.") ? HomeMode.NOTES : HomeMode.RECORDER;
        snap.label = job.label;
        snap.total = total;
        snap.done = done.size();
        ImportJobs.snapshot = snap;
        ImportJobs.publishNow();

        for (String id : ids) {
            if (done.contains(id)) {
                moved++;
                continue;
            }
            keepAlive(job);
            if (runSectionOne(app, job.type, id)) moved++;
            done.add(id);
            snap.done = done.size();
            snap.added = moved;
            SectionJobs.writeDone(app, job.id, done);
            ImportJobs.publish();
            throttledNotif(notif(job.label, snap.done + " of " + total, pct(snap.done, total)));
        }

        SectionJobs.finish(app, job, moved);
        ImportJobs.clearSnapshot();
        running = false;
        main.post(this::maybeStart);
    }

    // --- global search jobs -----------------------------------------------

    private void runSearchJob(JobQueue.Job job) {
        Context app = getApplicationContext();
        try {
            if (SearchJobs.TYPE_FILE_INDEX.equals(job.type)) {
                push(notif("Indexing files", "Scanning...", 0));
                int count = FileIndex.rebuildNow(app);
                push(notif("Indexing files", count + " entries", 100));
            }
        } catch (Exception e) {
            android.util.Log.w("JobService", "search job failed", e);
        }
        JobQueue.clear(app, job.id);
        running = false;
        main.post(this::maybeStart);
    }

    // --- chess puzzle generator --------------------------------------------

    private static final int PUZZLEGEN_CHECKPOINT_EVERY = 5;

    private void runChessPuzzleGen(JobQueue.Job job) {
        Context app = getApplicationContext();
        String scope = job.data.getOrDefault("scope", Config.CHESS_PUZZLEGEN_SCOPE_ALL);
        boolean scoped = !Config.CHESS_PUZZLEGEN_SCOPE_ALL.equals(scope);
        int startCursor = Config.getChessPuzzlegenCursor(app, scope);

        // Scoped runs (one PGN source) iterate that source's games directly, oldest-first —
        // small enough not to need scanFrom's streaming. The whole-library run keeps using
        // scanFrom so it never holds the full file resident.
        List<ChessLibrary.Entry> scopedEntries = null;
        int total;
        if (scoped) {
            scopedEntries = ChessLibrary.loadBySourceWithSans(app, scope);
            Collections.reverse(scopedEntries); // newest-first -> oldest-first, matches scanFrom's order
            total = scopedEntries.size();
        } else {
            total = ChessLibrary.lineCount(app);
        }
        if (startCursor >= total) {
            JobQueue.clear(app, job.id);
            ChessPuzzleJobs.clearSnapshot();
            running = false;
            main.post(this::maybeStart);
            return;
        }

        StockfishEngine engine;
        try {
            engine = StockfishEngine.getGenerator(app);
        } catch (IOException e) {
            android.util.Log.w("JobService", "chess puzzlegen: no engine available", e);
            JobQueue.clear(app, job.id);
            running = false;
            main.post(this::maybeStart);
            return;
        }
        PuzzleGenerator generator = new PuzzleGenerator(engine, () -> ChessPuzzleJobs.cancelRequested);

        ChessPuzzleJobs.Snapshot snap = new ChessPuzzleJobs.Snapshot();
        snap.scope = scope;
        snap.gamesTotal = total;
        snap.gamesScanned = startCursor;
        snap.puzzlesFound = scoped ? ChessPuzzles.countBySource(app, scope) : ChessPuzzles.count(app);
        ChessPuzzleJobs.publish(snap);

        // Cross-scope dedup: a game already analyzed under one scope (say a whole-library
        // run) is skipped — not re-run through the engine — if a later scoped run for its
        // own source's PGN reaches it too, and vice versa. The per-scope cursor above only
        // decides where each run starts *looking*; this id set is what actually guarantees
        // no game's (expensive) analysis ever runs twice no matter which scope(s) triggered
        // it or in what order.
        Set<String> scannedIds = ChessPuzzleJobs.loadScannedIds(app);
        List<String> pendingScannedIds = new ArrayList<>();

        int[] sinceCheckpoint = {0};
        ChessLibrary.LineCallback callback = (lineNumber, entry) -> {
            if (ChessPuzzleJobs.cancelRequested) return false;
            keepAlive(job);
            if (scannedIds.add(entry.id)) {
                try {
                    Optional<ChessPuzzles.Puzzle> found = generator.analyzeGame(entry);
                    if (found.isPresent()) {
                        ChessPuzzles.appendPuzzle(app, found.get());
                        snap.puzzlesFound++;
                    }
                } catch (Exception e) {
                    android.util.Log.w("JobService", "chess puzzlegen: game failed, skipping", e);
                }
                pendingScannedIds.add(entry.id);
            }
            snap.gamesScanned = lineNumber;
            if (++sinceCheckpoint[0] >= PUZZLEGEN_CHECKPOINT_EVERY) {
                sinceCheckpoint[0] = 0;
                Config.setChessPuzzlegenCursor(app, scope, lineNumber);
                ChessPuzzleJobs.appendScannedIds(app, pendingScannedIds);
                pendingScannedIds.clear();
            }
            ChessPuzzleJobs.publish(snap);
            String label = scoped ? ("Generating puzzles — " + scope) : "Generating chess puzzles";
            throttledNotif(notif(label,
                    snap.gamesScanned + " of " + snap.gamesTotal + " games · " + snap.puzzlesFound + " found",
                    pct(snap.gamesScanned, snap.gamesTotal)));
            return !ChessPuzzleJobs.cancelRequested;
        };

        if (scoped) {
            int n = 0;
            for (ChessLibrary.Entry entry : scopedEntries) {
                n++;
                if (n <= startCursor) continue;
                if (!callback.onEntry(n, entry)) break;
            }
        } else {
            ChessLibrary.scanFrom(app, startCursor, callback);
        }
        Config.setChessPuzzlegenCursor(app, scope, snap.gamesScanned);
        ChessPuzzleJobs.appendScannedIds(app, pendingScannedIds);
        boolean stoppedEarly = ChessPuzzleJobs.cancelRequested;
        ChessPuzzleJobs.cancelRequested = false;

        // Either caught up or stopped — either way the job row is done; a future
        // ChessPuzzleJobs.start() re-enqueues and picks up from Config's cursor for this scope.
        JobQueue.clear(app, job.id);
        if (stoppedEarly) {
            ChessPuzzleJobs.clearSnapshot();
        } else {
            // Scanned everything without being stopped — keep the snapshot published a few
            // seconds longer in its "done" state, so the Puzzles tab's job card gets to show
            // it actually finished instead of just vanishing the instant the last game did.
            snap.done = true;
            ChessPuzzleJobs.publish(snap);
            try { Thread.sleep(2500); } catch (InterruptedException ignored) { }
            ChessPuzzleJobs.clearSnapshot();
        }
        running = false;
        main.post(this::maybeStart);
    }

    // --- chess PGN import ---------------------------------------------------

    private static final int PGNIMPORT_CHECKPOINT_EVERY = 25;

    /** Streams a PGN file straight into the library, same {@link Pgn#parse}/
     *  {@link ChessLibrary.AppendSession} pipeline {@code MainActivity} used to run inline on
     *  a background {@code Thread} — moved here so a big import (thousands of games) survives
     *  leaving the screen, or the app dying and this job restarting it. Silent: no "choose a
     *  game" picker or auto-load onto the board when it finishes, since nothing guarantees the
     *  app is even open by then — the games just land in the library like any other import. */
    private void runChessPgnImport(JobQueue.Job job) {
        Context app = getApplicationContext();
        String uriString = job.data.get("uri");
        String sourceLabel = job.data.getOrDefault("label", "Imported PGN");
        if (uriString == null) {
            JobQueue.clear(app, job.id);
            running = false;
            main.post(this::maybeStart);
            return;
        }
        Uri uri = Uri.parse(uriString);

        ChessImportJobs.Snapshot snap = new ChessImportJobs.Snapshot();
        snap.sourceLabel = sourceLabel;
        ChessImportJobs.publish(snap);

        // Cheap first pass — just game boundaries, no tag map or SAN extraction — so the real
        // pass below can show "N of total" instead of only a running count with no
        // denominator. Best-effort: a provider that can't reopen the same uri twice, or any
        // other read failure here, just leaves gamesTotal at 0 (activeLabel's fallback).
        try (java.io.InputStream in = app.getContentResolver().openInputStream(uri)) {
            if (in != null) {
                try (java.io.Reader reader = new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)) {
                    snap.gamesTotal = Pgn.countGames(reader);
                    ChessImportJobs.publish(snap);
                }
            }
        } catch (IOException e) {
            android.util.Log.w("JobService", "chess pgn import: game count failed, no total shown", e);
        }

        int[] sinceCheckpoint = {0};
        boolean ok = false;
        try (java.io.InputStream in = app.getContentResolver().openInputStream(uri)) {
            if (in != null) {
                try (java.io.Reader reader = new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8);
                     ChessLibrary.AppendSession session = ChessLibrary.beginAppend(app, sourceLabel)) {
                    Pgn.parse(reader, game -> {
                        if (ChessImportJobs.cancelRequested) return false;
                        keepAlive(job);
                        try {
                            session.append(game);
                            snap.gamesImported++;
                        } catch (IOException e) {
                            android.util.Log.w("JobService", "chess pgn import: game write failed, skipping", e);
                        }
                        if (++sinceCheckpoint[0] >= PGNIMPORT_CHECKPOINT_EVERY) {
                            sinceCheckpoint[0] = 0;
                            ChessImportJobs.publish(snap);
                            int pct = snap.gamesTotal > 0 ? (int) (100L * snap.gamesImported / snap.gamesTotal) : 0;
                            throttledNotif(notif("Importing " + sourceLabel,
                                    ChessImportJobs.activeLabel(app), pct));
                        }
                        return true;
                    });
                    ok = true;
                }
            }
        } catch (IOException e) {
            android.util.Log.w("JobService", "chess pgn import: read failed", e);
        }

        boolean stoppedEarly = ChessImportJobs.cancelRequested;
        ChessImportJobs.cancelRequested = false;
        JobQueue.clear(app, job.id);
        if (stoppedEarly || !ok) {
            ChessImportJobs.clearSnapshot();
        } else {
            // Finished reading the whole file without being stopped — keep the snapshot
            // published a few seconds longer in its "done" state, so the Library tab's job
            // card gets to show it actually completed instead of just vanishing.
            snap.done = true;
            ChessImportJobs.publish(snap);
            try { Thread.sleep(2500); } catch (InterruptedException ignored) { }
            ChessImportJobs.clearSnapshot();
        }
        running = false;
        main.post(this::maybeStart);
    }

    private boolean runSectionOne(Context app, String type, String id) {
        if (SectionJobs.TYPE_NOTES_TO_VAULT.equals(type)) {
            Note note = findNote(id);
            return note != null && Notes.moveToVault(app, note);
        }
        if (SectionJobs.TYPE_NOTES_FROM_VAULT.equals(type)) {
            return Notes.moveOutOfVault(app, id);
        }
        if (SectionJobs.TYPE_RECORDER_TO_VAULT.equals(type)) {
            Recording recording = findRecording(app, id);
            return recording != null && Recorder.moveToVault(app, recording);
        }
        if (SectionJobs.TYPE_RECORDER_FROM_VAULT.equals(type)) {
            return Recorder.moveOutOfVault(app, id);
        }
        if (SectionJobs.TYPE_RECORDER_HEAL.equals(type)) {
            return Recorder.healVaultMeta(app, id);
        }
        return false;
    }

    private Note findNote(String id) {
        for (Note note : Config.getNotes(this)) {
            if (note.id.equals(id)) return note;
        }
        return null;
    }

    private Recording findRecording(Context app, String id) {
        for (Recording recording : Recorder.all(app)) {
            if (recording.id.equals(id)) return recording;
        }
        return null;
    }

    // --- vault operations --------------------------------------------------

    private void runVaultMoveLocation(JobQueue.Job job) {
        Context app = getApplicationContext();
        boolean ok = false;
        String message;
        VaultJobs.Snapshot snap = opSnapshot(job, 0, 1);
        VaultJobs.publishNow(snap);
        try {
            boolean move = Boolean.parseBoolean(job.data.get("move"));
            File current = new File(job.data.get("current"));
            File target = new File(job.data.get("target"));
            if (move) VaultLocation.moveVault(current, target);
            Config.setVaultLocationPath(app, job.data.get("config"));
            VaultLocation.ensureNoMedia(target);
            snap.done = 1;
            VaultJobs.publishNow(snap);
            ok = true;
            message = move ? "Vault moved" : "Location set";
            push(notif("Vault", message, 100));
        } catch (Exception e) {
            message = "Failed: " + e.getMessage();
            android.util.Log.e("JobService", "vault location move failed", e);
        }
        VaultJobs.finish(app, job, ok, message);
        running = false;
        main.post(this::maybeStart);
    }

    private void runVaultDelete(JobQueue.Job job) {
        Context app = getApplicationContext();
        List<String> ids = VaultJobs.readIds(app, job.id);
        Set<String> done = VaultJobs.readDoneIds(app, job.id);
        VaultJobs.Snapshot snap = opSnapshot(job, done.size(), ids.size());
        VaultJobs.publishNow(snap);
        int ok = done.size();
        for (String id : ids) {
            if (done.contains(id)) continue;
            keepAlive(job);
            try {
                VaultStore.delete(app, id);
                ok++;
            } catch (Exception e) {
                android.util.Log.w("JobService", "vault delete failed", e);
            }
            done.add(id);
            snap.done = done.size();
            VaultJobs.writeDoneIds(app, job.id, done);
            VaultJobs.publish(snap);
            throttledNotif(notif("Deleting from vault", snap.done + " of " + snap.total,
                    pct(snap.done, snap.total)));
        }
        VaultJobs.finish(app, job, true, "Deleted " + ok + " item(s)");
        running = false;
        main.post(this::maybeStart);
    }

    private void runVaultMove(JobQueue.Job job) {
        Context app = getApplicationContext();
        List<String> ids = VaultJobs.readIds(app, job.id);
        Set<String> done = VaultJobs.readDoneIds(app, job.id);
        String dest = job.data.get("dest");
        VaultJobs.Snapshot snap = opSnapshot(job, done.size(), ids.size());
        VaultJobs.publishNow(snap);
        int ok = done.size();
        for (String id : ids) {
            if (done.contains(id)) continue;
            keepAlive(job);
            try {
                VaultStore.move(app, id, dest);
                ok++;
            } catch (Exception e) {
                android.util.Log.w("JobService", "vault move failed", e);
            }
            done.add(id);
            snap.done = done.size();
            VaultJobs.writeDoneIds(app, job.id, done);
            VaultJobs.publish(snap);
            throttledNotif(notif("Moving vault items", snap.done + " of " + snap.total,
                    pct(snap.done, snap.total)));
        }
        VaultJobs.finish(app, job, true, "Moved " + ok + " item(s)");
        running = false;
        main.post(this::maybeStart);
    }

    private void runVaultExportFile(JobQueue.Job job) {
        Context app = getApplicationContext();
        boolean ok = false;
        String message = "Export failed";
        VaultJobs.Snapshot snap = opSnapshot(job, 0, 1);
        VaultJobs.publishNow(snap);
        keepAlive(job);
        try (OutputStream out = getContentResolver().openOutputStream(Uri.parse(job.data.get("dest")))) {
            if (out != null) {
                VaultStore.exportStream(app, job.data.get("doc"), out);
                ok = true;
                message = "Exported";
                snap.done = 1;
                VaultJobs.publishNow(snap);
            }
        } catch (Exception e) {
            android.util.Log.w("JobService", "vault export failed", e);
        }
        VaultJobs.finish(app, job, ok, message);
        running = false;
        main.post(this::maybeStart);
    }

    private void runVaultExportTree(JobQueue.Job job) {
        Context app = getApplicationContext();
        List<String> ids = VaultJobs.readIds(app, job.id);
        Set<String> done = VaultJobs.readDoneIds(app, job.id);
        Uri treeUri = Uri.parse(job.data.get("tree"));
        String treeDocId = DocumentsContract.getTreeDocumentId(treeUri);
        Uri dirUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, treeDocId);
        VaultJobs.Snapshot snap = opSnapshot(job, done.size(), ids.size());
        VaultJobs.publishNow(snap);
        int ok = 0;
        for (String id : ids) {
            if (done.contains(id)) {
                ok++;
                continue;
            }
            keepAlive(job);
            try {
                VaultStore.Entry entry = VaultStore.stat(app, id);
                if (!entry.isDir) {
                    Uri fileUri = DocumentsContract.createDocument(getContentResolver(), dirUri,
                            entry.mimeType == null ? "application/octet-stream" : entry.mimeType,
                            entry.name);
                    if (fileUri != null) {
                        try (OutputStream out = getContentResolver().openOutputStream(fileUri)) {
                            if (out != null) {
                                VaultStore.exportStream(app, id, out);
                                ok++;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                android.util.Log.w("JobService", "vault export failed", e);
            }
            done.add(id);
            snap.done = done.size();
            VaultJobs.writeDoneIds(app, job.id, done);
            VaultJobs.publish(snap);
            throttledNotif(notif("Exporting vault items", snap.done + " of " + snap.total,
                    pct(snap.done, snap.total)));
        }
        VaultJobs.finish(app, job, true, "Exported " + ok + " file(s)");
        running = false;
        main.post(this::maybeStart);
    }

    private void runVaultChangePassword(JobQueue.Job job) {
        Context app = getApplicationContext();
        char[] passphrase = VaultJobs.takePassphrase(job.id);
        boolean ok = false;
        String message = "Couldn't change password: interrupted";
        VaultJobs.Snapshot snap = opSnapshot(job, 0, 1);
        VaultJobs.publishNow(snap);
        try {
            if (passphrase == null) throw new IllegalStateException("password no longer available");
            byte[] masterKey = VaultSession.get().masterKey();
            if (masterKey == null) throw new IllegalStateException("vault locked");
            VaultCrypto.Progress progress = (done, total) -> {
                snap.done = done;
                snap.total = total;
                VaultJobs.publish(snap);
                throttledNotif(notif("Changing vault password", done + " of " + total,
                        pct(done, total)));
            };
            VaultFormat.changePassphrase(VaultSession.vaultRoot(app), masterKey, passphrase, progress);
            ok = true;
            message = "Password changed";
        } catch (Exception e) {
            message = "Couldn't change password: " + e.getMessage();
            android.util.Log.w("JobService", "vault password change failed", e);
        } finally {
            if (passphrase != null) java.util.Arrays.fill(passphrase, '\0');
        }
        VaultJobs.finish(app, job, ok, message);
        running = false;
        main.post(this::maybeStart);
    }

    // --- dev-plugin file sync ----------------------------------------------

    private static final long DEV_SYNC_CONNECT_TIMEOUT_MS = 15_000;

    private void runDevSync(JobQueue.Job job) {
        Context app = getApplicationContext();
        String pairId = job.data.get("pair");
        DevSyncPair pair = pairId == null ? null : DevSyncPair.find(app, pairId);
        if (pair == null || pair.localTree() == null || pair.remotePath == null) {
            DevSyncJobs.finish(app, job);
            running = false;
            main.post(this::maybeStart);
            return;
        }

        DevService.connect(app, pair.hostId);
        long deadline = android.os.SystemClock.uptimeMillis() + DEV_SYNC_CONNECT_TIMEOUT_MS;
        while (!DevService.isConnected(pair.hostId) && android.os.SystemClock.uptimeMillis() < deadline) {
            try { Thread.sleep(250); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
        }

        if (!DevService.isConnected(pair.hostId)) {
            android.util.Log.w("JobService", "dev sync: " + pair.hostId + " not reachable, skipping this run");
            pair.lastRunAt = System.currentTimeMillis();
            pair.lastFailed = 1;
            pair.save(app);
            DevSyncJobs.finish(app, job);
            running = false;
            main.post(this::maybeStart);
            return;
        }

        java.util.concurrent.CountDownLatch bound = new java.util.concurrent.CountDownLatch(1);
        DevService[] holder = new DevService[1];
        ServiceConnection sc = new ServiceConnection() {
            @Override public void onServiceConnected(ComponentName name, IBinder binder) {
                holder[0] = ((DevService.LocalBinder) binder).service();
                bound.countDown();
            }
            @Override public void onServiceDisconnected(ComponentName name) { }
        };
        bindService(new Intent(app, DevService.class), sc, Context.BIND_AUTO_CREATE);
        try { bound.await(5, java.util.concurrent.TimeUnit.SECONDS); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }

        DevConnection connection = holder[0] != null ? holder[0].connection(pair.hostId) : null;
        if (connection == null) {
            pair.lastRunAt = System.currentTimeMillis();
            pair.lastFailed = 1;
            pair.save(app);
            try { unbindService(sc); } catch (IllegalArgumentException ignored) { }
            DevSyncJobs.finish(app, job);
            running = false;
            main.post(this::maybeStart);
            return;
        }

        DevSyncClient client = new DevSyncClient(connection);
        try {
            keepAlive(job);
            DevSyncRunner.run(app, job, pair, client, () -> keepAlive(job));
        } finally {
            client.close();
            try { unbindService(sc); } catch (IllegalArgumentException ignored) { }
        }
        DevSyncJobs.finish(app, job);
        running = false;
        main.post(this::maybeStart);
    }

    private VaultJobs.Snapshot opSnapshot(JobQueue.Job job, int done, int total) {
        VaultJobs.Snapshot snap = new VaultJobs.Snapshot();
        snap.type = job.type;
        snap.label = job.label;
        snap.done = done;
        snap.total = total;
        return snap;
    }

    private static int pct(int done, int total) {
        return total > 0 ? (int) (100L * done / total) : 0;
    }

    // --- vault import -----------------------------------------------------

    private void runVaultImport(JobQueue.Job job) {
        Context app = getApplicationContext();
        VaultJobs.Import rec = VaultJobs.importFrom(app, job);
        if (rec == null) {
            JobQueue.clear(app, job.id);
            running = false;
            main.post(this::maybeStart);
            return;
        }

        Set<String> done = VaultJobs.readDone(app, rec.id);
        VaultJobs.Snapshot snap = new VaultJobs.Snapshot();
        snap.type = VaultJobs.TYPE_IMPORT;
        snap.importId = rec.id;
        snap.folderName = rec.folderName;
        snap.destParentDocId = rec.destParentDocId;
        snap.scanning = true;
        VaultJobs.publishNow(snap);

        final int[] sinceSave = {0};
        VaultImport.Result result = null;
        try {
            VaultImport.Hooks hooks = new VaultImport.Hooks() {
                @Override public void onScan(int filesFound) {
                    snap.totalFiles = filesFound;
                    VaultJobs.publish(snap);
                    throttledNotif(notif("Preparing import of " + rec.folderName,
                            filesFound + " files", 0));
                }
                @Override public void onProgress(int fd, int ft, long bd, long bt) {
                    keepAlive(job);
                    snap.scanning = false;
                    snap.doneFiles = fd;
                    snap.totalFiles = ft;
                    snap.doneBytes = bd;
                    snap.totalBytes = bt;
                    VaultJobs.publish(snap);
                    int pct = bt > 0 ? (int) (100L * bd / bt) : 0;
                    throttledNotif(notif("Importing to Vault / " + rec.folderName,
                            human(bd) + " / " + human(bt), pct));
                }
                @Override public void onFileSettled(String relPath, boolean failed) {
                    if (failed) snap.failed++;
                    if (++sinceSave[0] >= 25) {
                        sinceSave[0] = 0;
                        VaultJobs.writeDone(app, rec.id, done);
                    }
                }
                @Override public boolean cancelled() {
                    return VaultJobs.cancelImportRequested || !VaultSession.get().isUnlocked();
                }
            };
            if (rec.files != null) {
                result = VaultImport.runFiles(app, rec.files, rec.destParentDocId, rec.dup, done, hooks);
            } else {
                result = VaultImport.runFolder(app, rec.treeUri, rec.destParentDocId,
                        rec.folderName, rec.dup, done, hooks);
            }
        } catch (Exception e) {
            android.util.Log.e("JobService", "vault import failed", e);
        }
        VaultJobs.writeDone(app, rec.id, done);

        running = false;

        if (VaultJobs.cancelImportRequested) {
            for (VaultJobs.Import q : VaultJobs.pendingImports(app)) VaultJobs.clearImport(app, q.id);
            main.post(this::maybeStart);
            return;
        }
        if (!VaultSession.get().isUnlocked()) {
            main.post(() -> parkForVaultUnlock(job));
            return;
        }

        VaultJobs.clearImport(app, rec.id);
        if (result != null) VaultJobs.lastImport = result;
        main.post(this::maybeStart);
    }

    // --- notes / todos / recorder imports --------------------------------

    private void runPluginImport(JobQueue.Job job) {
        Context app = getApplicationContext();
        ImportJobs.Job rec = ImportJobs.jobFrom(app, job);
        if (rec == null) {
            JobQueue.clear(app, job.id);
            running = false;
            main.post(this::maybeStart);
            return;
        }

        Set<String> done = ImportJobs.readDone(app, rec.id);
        int added = ImportJobs.readAdded(app, rec.id);

        ImportJobs.Snapshot snap = new ImportJobs.Snapshot();
        snap.plugin = rec.plugin;
        snap.label = rec.label;
        snap.total = rec.uris.size();
        snap.done = done.size();
        snap.added = added;
        ImportJobs.snapshot = snap;
        ImportJobs.publishNow();

        int sinceSave = 0;
        for (Uri uri : rec.uris) {
            String key = uri.toString();
            if (done.contains(key)) continue;
            keepAlive(job);

            int add = 0;
            try {
                add = ImportJobs.importOne(app, rec.plugin, uri);
            } catch (Exception e) {
                android.util.Log.w("JobService", "import failed", e);
            }
            added += add;
            done.add(key);
            snap.added = added;
            snap.done = done.size();

            if (++sinceSave >= IMPORT_CHECKPOINT_EVERY) {
                sinceSave = 0;
                ImportJobs.writeDone(app, rec.id, done, added);
            }
            ImportJobs.publish();
            pushImportProgress(snap);
        }

        ImportJobs.writeDone(app, rec.id, done, added);
        keepAlive(job);
        for (Uri uri : rec.uris) {
            try {
                app.getContentResolver().releasePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (Exception ignored) {
            }
        }
        ImportJobs.setResult(rec.plugin, added);
        ImportJobs.clearJob(app, rec.id);

        running = false;
        main.post(this::maybeStart);
    }

    // --- lifecycle --------------------------------------------------------

    private void finishAll() {
        VaultJobs.clearSnapshot();
        ImportJobs.clearSnapshot();
        if (unlockWatcher != null) {
            VaultSession.get().removeListener(unlockWatcher);
            unlockWatcher = null;
        }
        if (VaultSession.get().isUnlocked()) VaultUnlockService.touch(getApplicationContext());
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (unlockWatcher != null) VaultSession.get().removeListener(unlockWatcher);
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    // --- notification -----------------------------------------------------

    private void throttledNotif(Notification n) {
        long now = android.os.SystemClock.uptimeMillis();
        if (now - lastNotif < 900) return;
        lastNotif = now;
        push(n);
    }

    private void pushImportProgress(ImportJobs.Snapshot snap) {
        long now = android.os.SystemClock.uptimeMillis();
        if (now - lastNotif < 600) return;
        lastNotif = now;
        int pct = snap.total > 0 ? (int) (100L * snap.done / snap.total) : 0;
        String text = snap.total <= 1 ? "1 file"
                : Math.min(snap.done + 1, snap.total) + " of " + snap.total;
        push(notif(snap.plugin.label + " - importing", text, pct));
    }

    private void push(Notification n) {
        getSystemService(NotificationManager.class).notify(NOTIF_ID, n);
    }

    private Notification notif(String title, String text, int pct) {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Jobs",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setShowBadge(false);
            manager.createNotificationChannel(channel);
        }
        PendingIntent open = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder b = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(android.R.drawable.stat_sys_upload)
                .setOngoing(true)
                .setContentIntent(open);
        if (pct > 0) b.setProgress(100, pct, false);
        return b.build();
    }

    private static String human(long bytes) {
        if (bytes < 1024) return bytes + " B";
        String[] u = {"KB", "MB", "GB", "TB"};
        double v = bytes / 1024.0;
        int i = 0;
        while (v >= 1024 && i < u.length - 1) {
            v /= 1024;
            i++;
        }
        return String.format(Locale.US, v < 10 ? "%.1f %s" : "%.0f %s", v, u[i]);
    }
}

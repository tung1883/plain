package com.plainphone.app;

import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/** Facade over the global {@link JobQueue} for the puzzle-generator job — mirrors
 *  {@link VaultJobs}/{@link ImportJobs}'s shape. Chess isn't lockable yet (see
 *  {@link PluginLock}), so unlike vault jobs this carries no {@code keep}/{@code require}
 *  area — it just runs, and {@link PluginTasks} surfaces it as "still running" for the lock /
 *  Lock-all dialogs regardless. */
final class ChessPuzzleJobs {

    private ChessPuzzleJobs() {}

    static final String TYPE_PUZZLEGEN = "chess.puzzlegen";

    interface Listener { void onChanged(); }

    private static final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();

    static void addListener(Listener l) { listeners.addIfAbsent(l); }
    static void removeListener(Listener l) { listeners.remove(l); }

    static final class Snapshot {
        int gamesScanned, gamesTotal, puzzlesFound;
        /** {@link Config#CHESS_PUZZLEGEN_SCOPE_ALL} or a single PGN source label. */
        String scope = Config.CHESS_PUZZLEGEN_SCOPE_ALL;
        /** Set once a run has scanned everything (not stopped/cancelled) — {@link JobService}
         *  keeps the snapshot published in this state for a few seconds afterward instead of
         *  clearing it the instant the last game finishes, so the job card gets to show it
         *  actually completed rather than just vanishing. */
        boolean done;
    }

    static volatile Snapshot snapshot;

    static void publish(Snapshot s) {
        snapshot = s;
        for (Listener l : listeners) l.onChanged();
        JobQueue.publish();
    }

    static void clearSnapshot() {
        snapshot = null;
        for (Listener l : listeners) l.onChanged();
        JobQueue.publish();
    }

    /** Starts (or, if one's already queued/running, leaves alone) a scan of the whole
     *  imported-games library. Safe to call repeatedly — e.g. every time a new PGN import
     *  finishes, so freshly-added games eventually get scanned without the user having to
     *  remember to re-trigger it by hand. */
    static void start(Context context) {
        start(context, Config.CHESS_PUZZLEGEN_SCOPE_ALL);
    }

    /** Same as {@link #start(Context)}, but scoped to a single PGN source label (as it appears
     *  on {@link ChessLibrary.Entry#src}) instead of the whole library. */
    static void start(Context context, String scopeSourceLabel) {
        if (JobQueue.anyOfType(context, TYPE_PUZZLEGEN)) return;
        cancelRequested = false;
        String scope = scopeSourceLabel == null ? Config.CHESS_PUZZLEGEN_SCOPE_ALL : scopeSourceLabel;
        String label = Config.CHESS_PUZZLEGEN_SCOPE_ALL.equals(scope)
                ? "Generating chess puzzles" : "Generating puzzles — " + scope;
        JobQueue.enqueue(context, new JobQueue.Spec(TYPE_PUZZLEGEN)
                .label(label)
                .put("scope", scope));
    }

    /** Checked by {@link JobService}'s scan loop and by {@link PuzzleGenerator}'s per-ply
     *  loop — the run stops at the next checkpoint rather than instantly, since it's
     *  mid-engine-call most of the time; a checkpoint is written for whatever's actually
     *  done before the job row is cleared. */
    static volatile boolean cancelRequested;

    static void stop(Context context) {
        cancelRequested = true;
        for (JobQueue.Job job : JobQueue.pendingByType(context, TYPE_PUZZLEGEN)) {
            JobQueue.clear(context, job.id);
        }
        clearSnapshot();
    }

    static boolean isRunning(Context context) {
        return JobQueue.anyOfType(context, TYPE_PUZZLEGEN);
    }

    /** e.g. "generating puzzles — 4,213 of 30,419 games — 37 found" for the lock-dialog line
     *  and the notification text. */
    static String activeLabel(Context context) {
        Snapshot s = snapshot;
        if (s == null || s.gamesTotal <= 0) return "generating puzzles";
        String scoped = Config.CHESS_PUZZLEGEN_SCOPE_ALL.equals(s.scope) ? "" : " (" + s.scope + ")";
        return String.format(java.util.Locale.US, "generating puzzles%s — %,d of %,d games — %,d found",
                scoped, s.gamesScanned, s.gamesTotal, s.puzzlesFound);
    }

    /** e.g. "4,213 games scanned — 37 found" for the job card's brief post-completion state. */
    static String doneLabel(Context context) {
        Snapshot s = snapshot;
        if (s == null) return "Done";
        return String.format(java.util.Locale.US, "%,d games scanned — %,d found",
                s.gamesScanned, s.puzzlesFound);
    }

    // --- cross-scope dedup --------------------------------------------------

    /** Every game id the generator has ever finished analyzing (puzzle found or not) — one
     *  set, shared by the whole-library run and every per-source scoped run alike. The
     *  resume cursor {@link Config#getChessPuzzlegenCursor} keeps is tracked independently
     *  per scope (a whole-library scan and a single source's scan walk in different orders,
     *  so their positions aren't comparable), but that only decides where each run starts
     *  looking; this set is what stops the same game's actual (expensive, engine-driven)
     *  analysis from ever running twice just because it was reached through two different
     *  scopes. */
    private static File scannedIdsFile(Context context) {
        return new File(context.getFilesDir(), "chess_puzzlegen_scanned.txt");
    }

    static Set<String> loadScannedIds(Context context) {
        Set<String> out = new HashSet<>();
        File f = scannedIdsFile(context);
        if (f.exists()) {
            try (BufferedReader r = new BufferedReader(new FileReader(f))) {
                String line;
                while ((line = r.readLine()) != null) if (!line.isEmpty()) out.add(line);
            } catch (IOException ignored) { }
        }
        return out;
    }

    /** Appends just the ids analyzed since the last call (not the whole set) — same
     *  append-only pattern as {@link ChessLibrary}'s own storage. Safe to call off the UI
     *  thread. */
    static void appendScannedIds(Context context, List<String> ids) {
        if (ids.isEmpty()) return;
        try (FileOutputStream out = new FileOutputStream(scannedIdsFile(context), true)) {
            StringBuilder sb = new StringBuilder();
            for (String id : ids) sb.append(id).append('\n');
            out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException ignored) { }
    }

}

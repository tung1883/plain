package com.plainphone.app;

import android.content.Context;

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
        if (JobQueue.anyOfType(context, TYPE_PUZZLEGEN)) return;
        cancelRequested = false;
        JobQueue.enqueue(context, new JobQueue.Spec(TYPE_PUZZLEGEN)
                .label("Generating chess puzzles"));
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
        return String.format(java.util.Locale.US, "generating puzzles — %,d of %,d games — %,d found",
                s.gamesScanned, s.gamesTotal, s.puzzlesFound);
    }

}

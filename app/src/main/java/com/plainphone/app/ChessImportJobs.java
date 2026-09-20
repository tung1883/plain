package com.plainphone.app;

import android.content.Context;
import android.net.Uri;

import java.util.concurrent.CopyOnWriteArrayList;

/** Facade over the global {@link JobQueue} for the PGN-import job — same shape as
 *  {@link ChessPuzzleJobs}, which it mirrors closely: a foreground-service job that survives
 *  leaving the screen (even the app dying and {@link JobService} restarting it), instead of an
 *  inline background {@code Thread} tied to the Activity that started it. */
final class ChessImportJobs {

    private ChessImportJobs() {}

    static final String TYPE_PGN_IMPORT = "chess.pgnimport";

    interface Listener { void onChanged(); }

    private static final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();

    static void addListener(Listener l) { listeners.addIfAbsent(l); }
    static void removeListener(Listener l) { listeners.remove(l); }

    static final class Snapshot {
        String sourceLabel;
        int gamesImported;
        /** Counted by a cheap first pass ({@link Pgn#countGames}) before the real import
         *  starts — 0 until that finishes (or if it failed), in which case the labels below
         *  fall back to just the running count with no denominator. */
        int gamesTotal;
        /** Set once a run has finished reading the whole file (not stopped) — kept published
         *  in this state a few seconds afterward (see {@link JobService}) so the job card
         *  gets to show it actually completed rather than just vanishing. */
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

    /** Starts a background import of {@code uri} into the library under {@code sourceLabel}.
     *  The caller must already hold (or have just taken, via
     *  {@code takePersistableUriPermission}) a persistable read grant on {@code uri} — the job
     *  may run well after this call returns, even across an app restart. */
    static void start(Context context, Uri uri, String sourceLabel) {
        cancelRequested = false;
        JobQueue.enqueue(context, new JobQueue.Spec(TYPE_PGN_IMPORT)
                .label("Importing " + sourceLabel)
                .put("uri", uri.toString())
                .put("label", sourceLabel));
    }

    /** Checked by {@link JobService}'s per-game callback — same checkpoint-not-instant
     *  semantics as {@link ChessPuzzleJobs#cancelRequested}. */
    static volatile boolean cancelRequested;

    static void stop(Context context) {
        cancelRequested = true;
        for (JobQueue.Job job : JobQueue.pendingByType(context, TYPE_PGN_IMPORT)) {
            JobQueue.clear(context, job.id);
        }
        clearSnapshot();
    }

    static boolean isRunning(Context context) {
        return JobQueue.anyOfType(context, TYPE_PGN_IMPORT);
    }

    /** e.g. "1,204 of 3,500 games imported" once the total's known, else just "1,204 games
     *  imported" — for the job card and notification text while it runs. */
    static String activeLabel(Context context) {
        Snapshot s = snapshot;
        if (s == null) return "Importing…";
        if (s.gamesTotal > 0) {
            return String.format(java.util.Locale.US, "%,d of %,d games imported", s.gamesImported, s.gamesTotal);
        }
        return String.format(java.util.Locale.US, "%,d games imported", s.gamesImported);
    }

    /** Same text, for the job card's brief post-completion state. */
    static String doneLabel(Context context) {
        Snapshot s = snapshot;
        if (s == null) return "Done";
        return String.format(java.util.Locale.US, "%,d games imported", s.gamesImported);
    }

}

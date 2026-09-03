package com.plainphone.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import java.util.List;
import java.util.Set;

/**
 * Runs the queued {@link ImportJobs} one at a time on a worker thread with an
 * ongoing progress notification. START_STICKY + a job record per import means a
 * process kill mid-import is resumed (from the checkpointed done-set) on next
 * launch.
 */
public class ImportJobService extends Service {

    private static final String CHANNEL_ID = "import_jobs";
    private static final int NOTIF_ID = 0x494a; // 'IJ'
    private static final int CHECKPOINT_EVERY = 5;

    private final Handler main = new Handler(Looper.getMainLooper());
    private volatile boolean running;
    private long lastNotif;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIF_ID, notif("Importing", "Working…", 0));
        maybeStart();
        return START_STICKY;
    }

    private void maybeStart() {
        if (running) return;
        if (!ImportJobs.anyPending(getApplicationContext())) {
            finishAll();
            return;
        }
        running = true;
        new Thread(this::runNext, "import-job").start();
    }

    private void runNext() {
        Context app = getApplicationContext();
        List<ImportJobs.Job> queue = ImportJobs.pendingJobs(app);
        if (queue.isEmpty()) {
            running = false;
            main.post(this::maybeStart);
            return;
        }
        ImportJobs.Job job = queue.get(0);          // oldest first

        Set<String> done = ImportJobs.readDone(app, job.id);
        int added = ImportJobs.readAdded(app, job.id);

        ImportJobs.Snapshot snap = new ImportJobs.Snapshot();
        snap.plugin = job.plugin;
        snap.label = job.label;
        snap.total = job.uris.size();
        snap.done = done.size();
        snap.added = added;
        ImportJobs.snapshot = snap;
        ImportJobs.publishNow();

        Lock lock = lockFor(job.plugin);
        int sinceSave = 0;

        for (Uri uri : job.uris) {
            String key = uri.toString();
            if (done.contains(key)) continue;
            if (lock != null) lock.keepUnlocked(app);   // hold the grace window open

            int add = 0;
            try {
                add = importOne(app, job.plugin, uri);
            } catch (Exception e) {
                android.util.Log.w("ImportJobService", "import failed", e);
            }
            added += add;
            done.add(key);
            snap.added = added;
            snap.done = done.size();

            if (++sinceSave >= CHECKPOINT_EVERY) {
                sinceSave = 0;
                ImportJobs.writeDone(app, job.id, done, added);
            }
            ImportJobs.publish();
            pushProgress(snap);
        }

        ImportJobs.writeDone(app, job.id, done, added);
        if (lock != null) lock.keepUnlocked(app);
        for (Uri uri : job.uris) {
            try {
                app.getContentResolver().releasePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (Exception ignored) {
            }
        }
        ImportJobs.setResult(job.plugin, added);
        ImportJobs.clearJob(app, job.id);

        running = false;
        main.post(this::maybeStart);                // next queued import, if any
    }

    /** @return items added from this file (notes/recordings 0 or 1; to-do: tasks). */
    private static int importOne(Context c, HomeMode plugin, Uri uri) {
        switch (plugin) {
            case NOTES:    return Notes.importOne(c, uri) ? 1 : 0;
            case RECORDER: return Recorder.importOne(c, uri) ? 1 : 0;
            case TODOS:    return Math.max(0, Todos.importFromFile(c, uri));
            default:       return 0;
        }
    }

    private static Lock lockFor(HomeMode plugin) {
        switch (plugin) {
            case NOTES:    return Lock.NOTES;
            case TODOS:    return Lock.TODOS;
            case RECORDER: return Lock.RECORDER;
            default:       return null;
        }
    }

    private void finishAll() {
        running = false;
        ImportJobs.clearSnapshot();
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    // --- notification ----------------------------------------------------

    private void pushProgress(ImportJobs.Snapshot snap) {
        long now = android.os.SystemClock.uptimeMillis();
        if (now - lastNotif < 600) return;
        lastNotif = now;
        int pct = snap.total > 0 ? (int) (100L * snap.done / snap.total) : 0;
        String text = snap.total <= 1 ? "1 file"
                : Math.min(snap.done + 1, snap.total) + " of " + snap.total;
        push(notif(snap.plugin.label + " — importing", text, pct));
    }

    private void push(Notification n) {
        getSystemService(NotificationManager.class).notify(NOTIF_ID, n);
    }

    private Notification notif(String title, String text, int pct) {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Imports",
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
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setOngoing(true)
                .setContentIntent(open);
        if (pct > 0) b.setProgress(100, pct, false);
        return b.build();
    }
}

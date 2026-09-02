package com.plainphone.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import java.util.Locale;
import java.util.Set;

/**
 * Runs the vault's persistent jobs ({@link VaultReset}, {@link VaultImport}) on a
 * worker thread with an ongoing progress notification. One job at a time; if the
 * process dies, Android restarts the service (START_STICKY) and it resumes from
 * the job record left by {@link VaultJobs}.
 */
public class VaultJobService extends Service {

    private static final String CHANNEL_ID = "vault_jobs";
    private static final int NOTIF_ID = 0x5642; // 'VB'

    /** Set by {@link VaultJobs#startReset} so a running import bows out for a reset. */
    static volatile boolean cancelImportRequested;

    private final Handler main = new Handler(Looper.getMainLooper());
    private volatile boolean running;
    private VaultSession.Listener unlockWatcher;
    private long lastNotif;

    private void throttledNotif(Notification n) {
        long now = android.os.SystemClock.uptimeMillis();
        if (now - lastNotif < 900) return;
        lastNotif = now;
        push(n);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIF_ID, notif("Vault", "Working…", 0));
        maybeStart();
        return START_STICKY;
    }

    private void maybeStart() {
        if (running) return;
        Context app = getApplicationContext();

        if (VaultJobs.resetPending(app)) {
            cancelImportRequested = false;
            running = true;
            new Thread(this::runReset, "vault-reset").start();
            return;
        }
        if (VaultJobs.importPending(app)) {
            if (!VaultSession.get().isUnlocked()) {
                parkForUnlock();
                return;
            }
            running = true;
            new Thread(this::runImport, "vault-import").start();
            return;
        }
        finishAll();
    }

    private void parkForUnlock() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.notify(NOTIF_ID, notif("Import paused",
                "Resumes when you unlock the vault", 0));
        if (unlockWatcher == null) {
            unlockWatcher = () -> main.post(this::maybeStart);
            VaultSession.get().addListener(unlockWatcher);
        }
    }

    // --- reset --------------------------------------------------

    private void runReset() {
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
            android.util.Log.e("VaultJobService", "reset failed", e);
        }

        VaultJobs.clearReset(app);
        running = false;
        main.post(this::maybeStart);
    }

    // --- import ------------------------------------------------

    private void runImport() {
        Context app = getApplicationContext();
        java.util.List<VaultJobs.Import> queue = VaultJobs.pendingImports(app);
        if (queue.isEmpty()) {
            running = false;
            main.post(this::maybeStart);
            return;
        }
        VaultJobs.Import rec = queue.get(0);   // oldest first

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
                            snap.scanning = false;
                            snap.doneFiles = fd; snap.totalFiles = ft;
                            snap.doneBytes = bd; snap.totalBytes = bt;
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
                            return cancelImportRequested || !VaultSession.get().isUnlocked();
                        }
            };
            if (rec.files != null) {
                result = VaultImport.runFiles(app, rec.files, rec.destParentDocId, rec.dup, done, hooks);
            } else {
                result = VaultImport.runFolder(app, rec.treeUri, rec.destParentDocId,
                        rec.folderName, rec.dup, done, hooks);
            }
        } catch (Exception e) {
            android.util.Log.e("VaultJobService", "import failed", e);
        }
        VaultJobs.writeDone(app, rec.id, done);

        running = false;

        if (cancelImportRequested) {
            // a reset preempts the whole queue; it will wipe any partial folders
            for (VaultJobs.Import q : VaultJobs.pendingImports(app)) VaultJobs.clearImport(app, q.id);
            main.post(this::maybeStart);
            return;
        }
        if (!VaultSession.get().isUnlocked()) {  // locked mid-run — keep the record, resume later
            main.post(this::parkForUnlock);
            return;
        }

        VaultJobs.clearImport(app, rec.id);
        if (result != null) VaultJobs.lastImport = result;
        main.post(this::maybeStart);   // next queued import, if any
    }

    // --- lifecycle --------------------------------------------

    private void finishAll() {
        VaultJobs.clearSnapshot();
        if (unlockWatcher != null) {
            VaultSession.get().removeListener(unlockWatcher);
            unlockWatcher = null;
        }
        // Auto-lock was suspended while the job ran — start a clean idle window now.
        if (VaultSession.get().isUnlocked()) {
            VaultUnlockService.touch(getApplicationContext());
        }
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

    // --- notification ----------------------------------------

    private void push(Notification n) {
        getSystemService(NotificationManager.class).notify(NOTIF_ID, n);
    }

    private Notification notif(String title, String text, int pct) {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Vault jobs",
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
        while (v >= 1024 && i < u.length - 1) { v /= 1024; i++; }
        return String.format(Locale.US, v < 10 ? "%.1f %s" : "%.0f %s", v, u[i]);
    }
}

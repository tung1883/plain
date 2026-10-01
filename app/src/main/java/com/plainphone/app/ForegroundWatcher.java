package com.plainphone.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;

/**
 * Notices which app came to the front without an accessibility service: polls usage events
 * while the screen is on. In Noob mode it drives {@link GateEngine}; in either mode it feeds
 * {@link ForegroundExtras}. Runs only while something needs it, see {@link #needed}.
 */
public class ForegroundWatcher extends Service {

    // Importance can't be lowered on an existing channel, so the quiet one has its own id.
    private static final String CHANNEL_ID = "foreground_watcher_quiet";
    private static final String OLD_CHANNEL_ID = "foreground_watcher";
    private static final int NOTIF_ID = 0x4657; // 'FW'
    private static final long POLL_MILLIS = 400L;
    private static final long OVERLAP_MILLIS = 1000L;
    private static final long LOOKBACK_MILLIS = 2 * 60_000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable poll = this::pollOnce;

    private UsageStatsManager usm;
    private BroadcastReceiver screenReceiver;
    private boolean polling;
    private long queryFrom;
    private long lastEventTime;

    private static volatile boolean running;

    /**
     * Start or stop the watcher to match what is configured right now. Stopping goes through
     * onStartCommand too: a stopService that lands before a pending start makes Android kill the
     * app for a foreground service that never called startForeground.
     */
    static void sync(Context context) {
        Context app = context.getApplicationContext();
        Intent intent = new Intent(app, ForegroundWatcher.class);
        try {
            if (needed(app) || running) {
                app.startForegroundService(intent);
            }
        } catch (RuntimeException e) {
            // Started from the background where Android won't allow a foreground service;
            // the next foreground visit syncs again.
        }
    }

    static boolean needed(Context context) {
        if (!AllAppsUsage.hasUsageAccess(context)) return false;
        if (ForegroundExtras.needed(context)) return true;
        return !Config.isNerdMode(context) && gatesConfigured(context);
    }

    private static boolean gatesConfigured(Context context) {
        if (!Config.getFlaggedPackages(context).isEmpty()) return true;
        if (Config.isLocksEnabled(context) && Config.isApplockEnabled(context)
                && !Config.getLockedPackages(context).isEmpty()) return true;
        return !Config.getTimeBlocks(context).isEmpty();
    }

    /** After a reboot or update no banking app is open, so undo any switch-off left behind. */
    public static class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            SecureSettings.resumeAccessibility(context);
            sync(context);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        running = true;
        usm = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        GateEngine.attachNoob(this, new NoobBackend(this));

        screenReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent i) {
                if (Intent.ACTION_SCREEN_ON.equals(i.getAction())) {
                    startPolling();
                } else {
                    stopPolling();
                }
            }
        };
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(screenReceiver, filter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification n = buildNotification();
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTIF_ID, n);
        }
        if (!needed(this)) {   // asked to stop, or restarted by the system after a kill
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf();
            return START_NOT_STICKY;
        }
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm.isInteractive()) startPolling();
        return START_STICKY;
    }

    private void startPolling() {
        if (polling) return;
        polling = true;
        long now = System.currentTimeMillis();
        queryFrom = now - OVERLAP_MILLIS;
        lastEventTime = now;
        // Usage events lag a moment behind, so right after a blocking app opened the last one on
        // record is still the previous app; seeding then would switch accessibility straight back on.
        if (!Config.isA11ySuspended(this)) ForegroundExtras.seedFromRecent(this);
        handler.post(poll);
    }

    private void stopPolling() {
        polling = false;
        handler.removeCallbacks(poll);
    }

    @SuppressWarnings("deprecation")
    private void pollOnce() {
        if (!polling) return;
        if (!needed(this)) {   // e.g. the reconnect window after accessibility came back is over
            polling = false;
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf();
            return;
        }
        try {
            long now = System.currentTimeMillis();
            UsageEvents events = usm.queryEvents(queryFrom, now);
            queryFrom = now - OVERLAP_MILLIS;

            boolean gate = true;   // duplicates of accessibility events are dropped by the engine
            GateEngine engine = GateEngine.get(this);
            UsageEvents.Event e = new UsageEvents.Event();
            while (events.hasNextEvent()) {
                events.getNextEvent(e);
                if (e.getEventType() != UsageEvents.Event.MOVE_TO_FOREGROUND) continue;
                long at = e.getTimeStamp();
                if (at <= lastEventTime) continue;
                lastEventTime = at;

                String pkg = e.getPackageName();
                String cls = e.getClassName();
                ForegroundExtras.onForeground(this, pkg, cls);
                if (gate) engine.onForeground(pkg, cls);
            }
        } catch (RuntimeException ignored) {
            // usage access revoked mid-poll, or the event stream hiccuped; try again next tick
        } finally {
            if (polling) handler.postDelayed(poll, POLL_MILLIS);
        }
    }

    private Notification buildNotification() {
        boolean nerd = Config.isNerdMode(this);
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.deleteNotificationChannel(OLD_CHANNEL_ID);
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            // MIN: no status-bar icon, no sound, tucked at the bottom of the shade. Android
            // insists a foreground service has a notification; this is the quietest it gets.
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "App watcher",
                    NotificationManager.IMPORTANCE_MIN);
            channel.setShowBadge(false);
            channel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
            manager.createNotificationChannel(channel);
        }
        PendingIntent open = PendingIntent.getActivity(this, 0,
                new Intent(this, AppAccessActivity.class),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        return new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle(nerd ? "Plain is waiting for you to leave a blocking app"
                        : "Plain is watching app opens")
                .setContentText(nerd ? "Then it switches accessibility back on"
                        : "Needed for wait screens, PINs and time blocks")
                .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
                .setOngoing(true)
                .setShowWhen(false)
                .setVisibility(Notification.VISIBILITY_SECRET)
                .setContentIntent(open)
                .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        running = false;
        stopPolling();
        if (screenReceiver != null) unregisterReceiver(screenReceiver);
        GateEngine.detachNoob();
        ForegroundExtras.reset(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

package com.plainphone.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

/**
 * Runs while the vault is unlocked: a persistent "Vault unlocked" notification,
 * a screen-off auto-lock, and an inactivity timeout ({@code vault_autolock_seconds}).
 * Keeping the process foregrounded stops Android reclaiming it — and losing the
 * in-memory key — mid-use.
 */
public class VaultUnlockService extends Service {

    static final String ACTION_LOCK = "com.plainphone.app.vault.LOCK";
    private static final String CHANNEL_ID = "vault_unlocked";
    private static final int NOTIF_ID = 0x5641; // 'VA'

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable autoLock = this::maybeAutoLock;

    /** Idle timeout fired — but don't lock out from under a running background job. */
    private void maybeAutoLock() {
        if (VaultJobs.anyPending(getApplicationContext())) {
            armTimeout();
            return;
        }
        lockAndStop();
    }

    private BroadcastReceiver screenOff;

    static void start(Context context) {
        context.startForegroundService(new Intent(context, VaultUnlockService.class));
    }

    static void stop(Context context) {
        context.stopService(new Intent(context, VaultUnlockService.class));
    }

    /** Reset the inactivity timeout — call on provider calls and vault UI interaction. */
    static void touch(Context context) {
        context.sendBroadcast(new Intent("com.plainphone.app.vault.TOUCH").setPackage(
                context.getPackageName()));
    }

    private BroadcastReceiver touchReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        screenOff = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent i) {
                if (VaultJobs.anyPending(c)) return;   // let a background job keep running
                lockAndStop();
            }
        };
        registerReceiver(screenOff, new IntentFilter(Intent.ACTION_SCREEN_OFF));

        touchReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent i) {
                armTimeout();
            }
        };
        IntentFilter touchFilter = new IntentFilter("com.plainphone.app.vault.TOUCH");
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            registerReceiver(touchReceiver, touchFilter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(touchReceiver, touchFilter);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_LOCK.equals(intent.getAction())) {
            if (VaultJobs.anyPending(this)) {
                startActivity(new Intent(this, PluginLockPromptActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } else {
                lockAndStop();
            }
            return START_NOT_STICKY;
        }
        startForeground(NOTIF_ID, buildNotification());
        armTimeout();
        return START_NOT_STICKY;
    }

    private void armTimeout() {
        handler.removeCallbacks(autoLock);
        int seconds = Config.getVaultAutoLockSeconds(this);
        if (seconds > 0) handler.postDelayed(autoLock, seconds * 1000L);
    }

    private void lockAndStop() {
        handler.removeCallbacks(autoLock);
        VaultSession.get().lock(getApplicationContext());
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    private Notification buildNotification() {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Vault",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setShowBadge(false);
            manager.createNotificationChannel(channel);
        }

        PendingIntent open = PendingIntent.getActivity(this, 0,
                new Intent(this, VaultActivity.class),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent lock = PendingIntent.getService(this, 1,
                new Intent(this, VaultUnlockService.class).setAction(ACTION_LOCK),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        return new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Vault unlocked")
                .setContentText("Tap to open — files stay readable until you lock")
                .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
                .setOngoing(true)
                .setContentIntent(open)
                .addAction(new Notification.Action.Builder(null, "Lock now", lock).build())
                .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(autoLock);
        if (screenOff != null) unregisterReceiver(screenOff);
        if (touchReceiver != null) unregisterReceiver(touchReceiver);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

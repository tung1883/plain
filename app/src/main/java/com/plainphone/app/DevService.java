package com.plainphone.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Owns the single live {@link DevConnection} while the Dev plugin is in use, so
 * a shell or screen session survives the activity leaving the screen. Modelled
 * on {@link RecorderService}: {@code volatile static} state the UI and
 * {@link PluginTasks} read, an ongoing notification, and a binder the Dev
 * activities grab to open channels.
 *
 * <p>Never auto-connects on a cold process start — the user re-opens the Dev
 * section to reconnect. Reconnects with backoff only while a session is live and
 * the section is unlocked.
 */
public class DevService extends Service {

    static final String ACTION_CONNECT = "com.plainphone.app.dev.CONNECT";
    static final String ACTION_DISCONNECT = "com.plainphone.app.dev.DISCONNECT";
    static final String EXTRA_HOST_ID = "hostId";

    private static final String CHANNEL_ID = "dev";
    private static final int NOTIF_ID = 0x4445; // 'DE'
    private static final long[] BACKOFF_MS = {1000, 2000, 4000, 8000, 15000, 30000};

    enum State { DISCONNECTED, CONNECTING, CONNECTED }

    interface StateListener {
        void onDevState();
    }

    private static volatile boolean connected;
    private static volatile String hostLabel;
    private static volatile String hostId;
    private static volatile String detail;
    private static volatile String lastError;
    private static final CopyOnWriteArrayList<StateListener> listeners = new CopyOnWriteArrayList<>();

    static boolean isConnected() {
        return connected;
    }

    static String connectedHostLabel() {
        return hostLabel;
    }

    static String connectedHostId() {
        return hostId;
    }

    /** "prod-1" / "prod-1 — shell", for the lock-all confirmation and the hub. */
    static String activeDetail() {
        return detail != null ? detail : hostLabel;
    }

    static String lastError() {
        return lastError;
    }

    static void addStateListener(StateListener l) {
        listeners.addIfAbsent(l);
    }

    static void removeStateListener(StateListener l) {
        listeners.remove(l);
    }

    static void connect(Context context, String hostId) {
        context.getApplicationContext().startForegroundService(
                new Intent(context, DevService.class)
                        .setAction(ACTION_CONNECT)
                        .putExtra(EXTRA_HOST_ID, hostId));
    }

    static void disconnect(Context context) {
        if (!connected && DevService.hostId == null) return;
        context.getApplicationContext().startService(
                new Intent(context, DevService.class).setAction(ACTION_DISCONNECT));
    }

    // --- instance -------------------------------------------------------

    private final Handler main = new Handler(Looper.getMainLooper());
    private final Binder binder = new LocalBinder();
    private DevConnection connection;
    private DevHost target;
    private State state = State.DISCONNECTED;
    private int backoffStep;
    private boolean userStopped;

    class LocalBinder extends Binder {
        DevService service() {
            return DevService.this;
        }
    }

    DevConnection connection() {
        return state == State.CONNECTED ? connection : null;
    }

    State state() {
        return state;
    }

    /** Called by an activity to label what the session is doing right now. */
    void setActivityDetail(String what) {
        detail = (what == null || target == null) ? hostLabel : target.label + " — " + what;
        updateNotification();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? null : intent.getAction();
        if (ACTION_DISCONNECT.equals(action) || action == null) {
            userStopped = true;
            teardown("disconnected");
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf();
            return START_NOT_STICKY;
        }
        if (ACTION_CONNECT.equals(action)) {
            String id = intent.getStringExtra(EXTRA_HOST_ID);
            DevHost host = DevHost.find(this, id);
            if (host == null) {
                stopSelf();
                return START_NOT_STICKY;
            }
            if (state != State.DISCONNECTED && target != null && target.id.equals(id)) {
                goForeground();
                return START_NOT_STICKY; // already on it
            }
            userStopped = false;
            target = host;
            hostId = host.id;
            hostLabel = host.label;
            detail = host.label;
            backoffStep = 0;
            Config.setDevLastHostId(this, host.id);
            goForeground();
            openConnection();
        }
        return START_NOT_STICKY;
    }

    private void openConnection() {
        if (userStopped || target == null) return;
        String token = target.token(this);
        if (token == null) {
            lastError = "no saved key for " + target.label + " — pair again";
            setState(State.DISCONNECTED);
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf();
            return;
        }
        setState(State.CONNECTING);
        connection = new DevConnection(target.host, target.port, token,
                android.os.Build.MODEL == null ? "phone" : android.os.Build.MODEL,
                new DevConnection.Listener() {
                    @Override
                    public void onConnected(String host, String os, List<Object> caps) {
                        backoffStep = 0;
                        lastError = null;
                        setState(State.CONNECTED);
                    }

                    @Override
                    public void onDisconnected(String reason) {
                        lastError = reason;
                        if (userStopped) {
                            setState(State.DISCONNECTED);
                            stopForeground(STOP_FOREGROUND_REMOVE);
                            stopSelf();
                            return;
                        }
                        setState(State.CONNECTING);
                        scheduleRetry();
                    }
                });
        connection.start();
    }

    private void scheduleRetry() {
        long delay = BACKOFF_MS[Math.min(backoffStep, BACKOFF_MS.length - 1)];
        backoffStep++;
        main.postDelayed(() -> {
            if (!userStopped) openConnection();
        }, delay);
    }

    private void teardown(String reason) {
        DevConnection c = connection;
        connection = null;
        target = null;
        hostId = null;
        if (c != null) c.close();
        setState(State.DISCONNECTED);
    }

    private void setState(State s) {
        state = s;
        connected = s == State.CONNECTED;
        if (!connected && s == State.DISCONNECTED) {
            hostLabel = null;
            detail = null;
        }
        updateNotification();
        main.post(() -> {
            for (StateListener l : listeners) l.onDevState();
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        teardown("service stopped");
    }

    // --- notification --------------------------------------------------

    private void goForeground() {
        Notification n = buildNotification();
        if (Build.VERSION.SDK_INT >= 30) {
            startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(NOTIF_ID, n);
        }
    }

    private void updateNotification() {
        if (state == State.DISCONNECTED) return;
        getSystemService(NotificationManager.class).notify(NOTIF_ID, buildNotification());
    }

    private Notification buildNotification() {
        NotificationManager m = getSystemService(NotificationManager.class);
        if (m.getNotificationChannel(CHANNEL_ID) == null) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Dev",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setShowBadge(false);
            m.createNotificationChannel(channel);
        }
        String label = hostLabel == null ? "a computer" : hostLabel;
        String title = state == State.CONNECTED
                ? "Connected · " + label
                : "Connecting to " + label + "…";
        String text = state == State.CONNECTED && detail != null ? detail : "Dev";
        Intent open = new Intent(this, DevHostActivity.class)
                .putExtra(DevHostActivity.EXTRA_HOST_ID, hostId)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        Notification.Builder b = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_stat_dev)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(PendingIntent.getActivity(this, 0, open,
                        PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));
        b.addAction(new Notification.Action.Builder((android.graphics.drawable.Icon) null,
                "Disconnect",
                PendingIntent.getService(this, 1,
                        new Intent(this, DevService.class).setAction(ACTION_DISCONNECT),
                        PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT))
                .build());
        return b.build();
    }
}

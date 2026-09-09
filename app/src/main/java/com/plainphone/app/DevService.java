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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Owns the live {@link DevConnection}s while the Dev plugin is in use — one per
 * paired device — so shells and screen sessions survive an activity leaving the
 * screen, and a workspace can hold panels from several devices at once. Modelled
 * on {@link RecorderService}: {@code volatile static} state the UI and
 * {@link PluginTasks} read, an ongoing notification, and a binder the Dev
 * activities grab to open channels.
 *
 * <p>Never auto-connects on a cold process start — the user re-opens a Dev
 * screen. Reconnects with backoff while a link is wanted and the section is
 * unlocked.
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

    // Snapshot of link state for the UI (host id -> connected?).
    private static volatile Map<String, Boolean> linkState = Collections.emptyMap();
    private static volatile String primaryId;   // a connected host, for legacy single-host screens
    private static volatile String primaryLabel;
    private static volatile String detail;
    private static volatile String lastError;
    private static final CopyOnWriteArrayList<StateListener> listeners = new CopyOnWriteArrayList<>();

    static boolean isConnected() {
        for (Boolean v : linkState.values()) if (Boolean.TRUE.equals(v)) return true;
        return false;
    }

    static boolean isConnected(String hostId) {
        return hostId != null && Boolean.TRUE.equals(linkState.get(hostId));
    }

    /** A link for this device exists (connected or reconnecting). */
    static boolean isLinked(String hostId) {
        return hostId != null && linkState.containsKey(hostId);
    }

    static List<String> connectedHostIds() {
        List<String> out = new ArrayList<>();
        for (Map.Entry<String, Boolean> e : linkState.entrySet()) {
            if (Boolean.TRUE.equals(e.getValue())) out.add(e.getKey());
        }
        return out;
    }

    /** Any connected host id (or null) — for the older single-host Dev screens. */
    static String connectedHostId() {
        return primaryId;
    }

    static String connectedHostLabel() {
        return primaryLabel;
    }

    static String activeDetail() {
        return detail != null ? detail : primaryLabel;
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

    /** Drop one device's link. */
    static void disconnect(Context context, String hostId) {
        context.getApplicationContext().startService(
                new Intent(context, DevService.class)
                        .setAction(ACTION_DISCONNECT)
                        .putExtra(EXTRA_HOST_ID, hostId));
    }

    /** Drop every link and stop the service. */
    static void disconnect(Context context) {
        if (linkState.isEmpty()) return;
        context.getApplicationContext().startService(
                new Intent(context, DevService.class).setAction(ACTION_DISCONNECT));
    }

    // --- instance -------------------------------------------------------

    private final Handler main = new Handler(Looper.getMainLooper());
    private final Binder binder = new LocalBinder();
    private final Map<String, Link> links = new LinkedHashMap<>();

    private static final class Link {
        final DevHost host;
        DevConnection conn;
        State state = State.CONNECTING;
        int backoff;
        boolean stopped;
        String detail;

        Link(DevHost host) { this.host = host; }
    }

    class LocalBinder extends Binder {
        DevService service() {
            return DevService.this;
        }
    }

    /** Any connected link (legacy). */
    DevConnection connection() {
        for (Link l : links.values()) {
            if (l.state == State.CONNECTED && l.conn != null) return l.conn;
        }
        return null;
    }

    DevConnection connection(String hostId) {
        Link l = links.get(hostId);
        return (l != null && l.state == State.CONNECTED) ? l.conn : null;
    }

    State state() {
        Link l = primaryId != null ? links.get(primaryId) : null;
        if (l != null) return l.state;
        return links.isEmpty() ? State.DISCONNECTED : State.CONNECTING;
    }

    /** Label what a device's link is doing right now (for the notification). */
    void setActivityDetail(String hostId, String what) {
        Link l = links.get(hostId);
        if (l == null) return;
        l.detail = what;
        publish();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? null : intent.getAction();
        String id = intent == null ? null : intent.getStringExtra(EXTRA_HOST_ID);

        if (ACTION_DISCONNECT.equals(action) || action == null) {
            if (id != null) {
                stopLink(id);
                if (links.isEmpty()) { stopForeground(STOP_FOREGROUND_REMOVE); stopSelf(); }
                else publish();
            } else {
                for (String k : new ArrayList<>(links.keySet())) stopLink(k);
                stopForeground(STOP_FOREGROUND_REMOVE);
                stopSelf();
            }
            return START_NOT_STICKY;
        }

        if (ACTION_CONNECT.equals(action)) {
            DevHost host = DevHost.find(this, id);
            if (host == null) {
                if (links.isEmpty()) stopSelf();
                return START_NOT_STICKY;
            }
            Link existing = links.get(host.id);
            if (existing != null && !existing.stopped) {
                goForeground();
                return START_NOT_STICKY; // already on it
            }
            Link link = new Link(host);
            links.put(host.id, link);
            Config.setDevLastHostId(this, host.id);
            goForeground();
            openLink(link);
        }
        return START_NOT_STICKY;
    }

    private void openLink(Link link) {
        if (link.stopped) return;
        String token = link.host.token(this);
        if (token == null) {
            lastError = "no saved key for " + link.host.label + " — pair again";
            links.remove(link.host.id);
            if (links.isEmpty()) { stopForeground(STOP_FOREGROUND_REMOVE); stopSelf(); }
            publish();
            return;
        }
        link.state = State.CONNECTING;
        publish();
        link.conn = new DevConnection(link.host.host, link.host.port, token,
                Build.MODEL == null ? "phone" : Build.MODEL,
                new DevConnection.Listener() {
                    @Override
                    public void onConnected(String host, String os, List<Object> caps) {
                        link.backoff = 0;
                        lastError = null;
                        link.state = State.CONNECTED;
                        publish();
                    }

                    @Override
                    public void onDisconnected(String reason) {
                        lastError = reason;
                        if (link.stopped || !links.containsKey(link.host.id)) {
                            publish();
                            return;
                        }
                        link.state = State.CONNECTING;
                        publish();
                        scheduleRetry(link);
                    }
                });
        link.conn.start();
    }

    private void scheduleRetry(Link link) {
        long delay = BACKOFF_MS[Math.min(link.backoff, BACKOFF_MS.length - 1)];
        link.backoff++;
        main.postDelayed(() -> {
            if (!link.stopped && links.containsKey(link.host.id)) openLink(link);
        }, delay);
    }

    private void stopLink(String hostId) {
        Link l = links.remove(hostId);
        if (l == null) return;
        l.stopped = true;
        if (l.conn != null) l.conn.close();
    }

    private void publish() {
        Map<String, Boolean> snap = new LinkedHashMap<>();
        String pId = null, pLabel = null;
        StringBuilder d = new StringBuilder();
        for (Link l : links.values()) {
            boolean up = l.state == State.CONNECTED;
            snap.put(l.host.id, up);
            if (up && pId == null) { pId = l.host.id; pLabel = l.host.label; }
            if (up && l.detail != null) {
                if (d.length() > 0) d.append("  ·  ");
                d.append(l.host.label).append(" — ").append(l.detail);
            }
        }
        linkState = snap;
        primaryId = pId;
        primaryLabel = pLabel;
        detail = d.length() > 0 ? d.toString() : null;
        updateNotification();
        main.post(() -> {
            for (StateListener l : listeners) l.onDevState();
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        for (String k : new ArrayList<>(links.keySet())) stopLink(k);
        publish();
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
        if (links.isEmpty()) return;
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
        int total = links.size();
        int up = connectedHostIds().size();
        String title;
        if (total == 1) {
            Link only = links.values().iterator().next();
            title = only.state == State.CONNECTED
                    ? "Connected · " + only.host.label
                    : "Connecting to " + only.host.label + "…";
        } else {
            title = up + " of " + total + " devices connected";
        }
        String text = detail != null ? detail : "Dev";
        Intent open = new Intent(this, DevHostsActivity.class)
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
                "Disconnect all",
                PendingIntent.getService(this, 1,
                        new Intent(this, DevService.class).setAction(ACTION_DISCONNECT),
                        PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT))
                .build());
        return b.build();
    }
}

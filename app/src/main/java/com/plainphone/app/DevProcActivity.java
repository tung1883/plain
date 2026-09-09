package com.plainphone.app;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** A live process list over the {@code proc} channel — sorted by CPU, tap to signal. */
public class DevProcActivity extends Activity implements DevService.StateListener {

    private String hostId;
    private LinearLayout root;
    private Typeface font;
    private DevService service;
    private DevConnection connection;
    private long channel = -1;
    private boolean opening;

    private final DevConnection.Sink sink = this::onChannelMessage;

    private final ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            service = ((DevService.LocalBinder) binder).service();
            tryOpen();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            service = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hostId = getIntent().getStringExtra(DevHostActivity.EXTRA_HOST_ID);
        DevHost host = DevHost.find(this, hostId);
        if (host == null) {
            finish();
            return;
        }
        font = Fonts.cascadiaMono(this);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);
        ScrollView scroller = new ScrollView(this);
        scroller.setBackgroundColor(Color.BLACK);
        scroller.addView(root, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        UiKit.screen(this, host.label + " · processes", scroller);
        message("Connecting…");
    }

    @Override
    protected void onStart() {
        super.onStart();
        DevService.addStateListener(this);
        bindService(new Intent(this, DevService.class), conn, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        DevService.connect(this, hostId);
        tryOpen();
        refresh();
    }

    @Override
    protected void onStop() {
        super.onStop();
        DevService.removeStateListener(this);
        if (channel >= 0 && connection != null) connection.closeChannel(channel);
        channel = -1;
        opening = false;
        if (service != null) service.setActivityDetail(null);
        try {
            unbindService(conn);
        } catch (IllegalArgumentException ignored) {
        }
    }

    @Override
    public void onDevState() {
        runOnUiThread(() -> {
            DevConnection live = service != null ? service.connection() : null;
            if (connection != null && connection != live) {
                connection = null;
                channel = -1;
                opening = false;
            }
            tryOpen();
            refresh();
        });
    }

    private void tryOpen() {
        if (opening || channel >= 0 || service == null || !DevService.isConnected()) return;
        connection = service.connection();
        if (connection == null) return;
        opening = true;
        channel = connection.openChannel(sink);
        service.setActivityDetail("processes");
        message("Loading…");
        refresh();
    }

    private void refresh() {
        if (channel >= 0 && connection != null) {
            connection.send(DevProtocol.procList(channel));
        }
    }

    private void onChannelMessage(Map<String, Object> msg) {
        String type = DevProtocol.type(msg);
        if (DevProtocol.T_PROC_LIST.equals(type)) {
            render(DevProtocol.list(msg, "procs"));
        } else if (DevProtocol.T_PROC_KILLED.equals(type)) {
            refresh();
        }
    }

    private void render(List<Object> procs) {
        root.removeAllViews();
        if (procs == null || procs.isEmpty()) {
            message("No processes reported.");
            return;
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Object o : procs) {
            if (o instanceof Map) rows.add(asStringMap(o));
        }
        rows.sort((a, b) -> Double.compare(
                DevProtocol.dbl(b, "cpu", 0), DevProtocol.dbl(a, "cpu", 0)));

        root.addView(headerRow());
        for (Map<String, Object> p : rows) {
            long pid = DevProtocol.num(p, "pid", 0);
            String name = DevProtocol.str(p, "name");
            double cpu = DevProtocol.dbl(p, "cpu", 0);
            long memKb = DevProtocol.num(p, "mem_kb", 0);
            root.addView(procRow(pid, name == null ? "?" : name, cpu, memKb));
        }
    }

    private View headerRow() {
        TextView t = new TextView(this);
        t.setText(String.format(Locale.US, "%-7s %5s %8s  %s", "PID", "CPU%", "MEM", "NAME"));
        t.setTextColor(Color.GRAY);
        t.setTextSize(12);
        t.setTypeface(font);
        t.setPadding(32, 20, 32, 12);
        return t;
    }

    private View procRow(long pid, String name, double cpu, long memKb) {
        TextView t = new TextView(this);
        t.setText(String.format(Locale.US, "%-7d %5.1f %8s  %s", pid, cpu, mem(memKb), name));
        t.setTextColor(Color.WHITE);
        t.setTextSize(13);
        t.setTypeface(font);
        t.setPadding(32, 18, 32, 18);
        StateListDrawable bg = new StateListDrawable();
        bg.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Color.DKGRAY));
        bg.addState(new int[]{}, new ColorDrawable(Color.BLACK));
        t.setBackground(bg);
        t.setOnClickListener(v -> VaultUi.tasksDialog(this,
                "Signal " + name + " (" + pid + ")?",
                new ArrayList<>(),
                new String[]{"SIGTERM", "SIGKILL", "Cancel"},
                new VaultUi.Choice[]{
                        () -> kill(pid, "TERM"),
                        () -> kill(pid, "KILL"),
                        () -> {},
                }));
        return t;
    }

    private void kill(long pid, String sig) {
        if (channel >= 0 && connection != null) {
            connection.send(DevProtocol.procKill(channel, pid, sig));
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asStringMap(Object o) {
        return (Map<String, Object>) o;
    }

    private static String mem(long kb) {
        if (kb >= 1024 * 1024) return String.format(Locale.US, "%.1fG", kb / 1024f / 1024f);
        if (kb >= 1024) return String.format(Locale.US, "%.0fM", kb / 1024f);
        return kb + "K";
    }

    private void message(String text) {
        root.removeAllViews();
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextColor(Color.GRAY);
        t.setTextSize(14);
        t.setTypeface(font);
        t.setPadding(48, 40, 48, 40);
        root.addView(t);
    }
}

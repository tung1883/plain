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
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The list of shells running on a host. Each is a persistent session in
 * {@code plaind} — it keeps running while the phone is away. Tap to (re)attach,
 * long-press to kill.
 */
public class DevShellsActivity extends Activity implements DevService.StateListener {

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
        font = Fonts.current(this);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);
        ScrollView scroller = new ScrollView(this);
        scroller.setBackgroundColor(Color.BLACK);
        scroller.addView(root, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        UiKit.screen(this, host.label + " · Shells", scroller);
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
        try {
            unbindService(conn);
        } catch (IllegalArgumentException ignored) {
        }
    }

    @Override
    public void onDevState() {
        runOnUiThread(() -> {
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
        message("Loading…");
        refresh();
    }

    private void refresh() {
        if (channel >= 0 && connection != null) {
            connection.send(DevProtocol.sessionList(channel));
        }
    }

    private void onChannelMessage(Map<String, Object> msg) {
        if (DevProtocol.T_SESSION_LIST.equals(DevProtocol.type(msg))) {
            render(DevProtocol.list(msg, "sessions"));
        }
    }

    private void render(List<Object> sessions) {
        root.removeAllViews();
        root.addView(newShellRow());
        if (sessions == null || sessions.isEmpty()) {
            root.addView(hint("No shells running. Start one above."));
            return;
        }
        for (Object o : sessions) {
            if (!(o instanceof Map)) continue;
            @SuppressWarnings("unchecked")
            Map<String, Object> s = (Map<String, Object>) o;
            long id = DevProtocol.num(s, "id", 0);
            String name = DevProtocol.str(s, "name");
            boolean alive = DevProtocol.bool(s, "alive");
            root.addView(shellRow(id, name == null ? ("shell " + id) : name, alive));
        }
    }

    private View newShellRow() {
        TextView t = row("+  New Shell", Color.WHITE);
        t.setOnClickListener(v -> open(-1));
        return t;
    }

    private View shellRow(long id, String name, boolean alive) {
        TextView t = row((alive ? "● " : "○ ") + name, Color.WHITE);
        t.setOnClickListener(v -> open(id));
        t.setOnLongClickListener(v -> {
            VaultUi.tasksDialog(this, "Kill " + name + "?", new ArrayList<>(),
                    new String[]{"Kill", "Cancel"},
                    new VaultUi.Choice[]{() -> kill(id), () -> {}});
            return true;
        });
        return t;
    }

    private void open(long id) {
        startActivity(new Intent(this, DevTerminalActivity.class)
                .putExtra(DevHostActivity.EXTRA_HOST_ID, hostId)
                .putExtra(DevTerminalActivity.EXTRA_SESSION_ID, id));
    }

    private void kill(long id) {
        if (channel >= 0 && connection != null) {
            connection.send(DevProtocol.sessionKill(channel, id));
            connection.send(DevProtocol.sessionList(channel));
        }
    }

    private TextView row(String text, int color) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextColor(color);
        t.setTextSize(18);
        t.setTypeface(font);
        t.setPadding(40, 26, 40, 26);
        StateListDrawable bg = new StateListDrawable();
        bg.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Color.DKGRAY));
        bg.addState(new int[]{}, new ColorDrawable(Color.BLACK));
        t.setBackground(bg);
        return t;
    }

    private TextView hint(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextColor(Color.GRAY);
        t.setTextSize(13);
        t.setTypeface(font);
        t.setPadding(40, 24, 40, 24);
        return t;
    }

    private void message(String text) {
        root.removeAllViews();
        root.addView(hint(text));
    }
}

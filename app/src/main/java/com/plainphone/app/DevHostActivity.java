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

/** Per-host hub: connection state plus Shell / Screen / Processes and Disconnect. */
public class DevHostActivity extends Activity implements DevService.StateListener {

    static final String EXTRA_HOST_ID = "hostId";

    private String hostId;
    private DevHost host;
    private LinearLayout root;
    private Typeface font;
    private DevService service;

    private final ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            service = ((DevService.LocalBinder) binder).service();
            render();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            service = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hostId = getIntent().getStringExtra(EXTRA_HOST_ID);
        host = DevHost.find(this, hostId);
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
        UiKit.screen(this, host.label, scroller);
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
        render();
    }

    @Override
    protected void onStop() {
        super.onStop();
        DevService.removeStateListener(this);
        try {
            unbindService(conn);
        } catch (IllegalArgumentException ignored) {
        }
    }

    @Override
    public void onDevState() {
        runOnUiThread(this::render);
    }

    private void render() {
        root.removeAllViews();

        boolean live = host.id.equals(DevService.connectedHostId()) && DevService.isConnected();
        String stateText;
        if (live) {
            stateText = "Connected · " + host.address();
        } else if (host.id.equals(DevService.connectedHostId())) {
            stateText = "Connecting to " + host.address() + "…";
        } else {
            String err = DevService.lastError();
            stateText = err != null ? err : "Not connected";
        }
        root.addView(status(stateText));

        boolean enabled = live;
        root.addView(big("Shell", "interactive shell — vim / htop / tmux", enabled,
                v -> startActivity(new Intent(this, DevTerminalActivity.class)
                        .putExtra(EXTRA_HOST_ID, hostId))));
        root.addView(big("Screen", "mirror the desktop — trackpad + keyboard", enabled,
                v -> startActivity(new Intent(this, DevScreenActivity.class)
                        .putExtra(EXTRA_HOST_ID, hostId))));
        root.addView(big("Processes", "list and signal running processes", enabled,
                v -> startActivity(new Intent(this, DevProcActivity.class)
                        .putExtra(EXTRA_HOST_ID, hostId))));

        root.addView(divider());
        root.addView(action("Disconnect", 0xFFC88F87, v -> {
            DevService.disconnect(this);
            render();
        }));
    }

    private TextView status(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextColor(Color.GRAY);
        t.setTextSize(13);
        t.setTypeface(font);
        t.setPadding(48, 24, 48, 24);
        return t;
    }

    private View big(String title, String sub, boolean enabled, View.OnClickListener tap) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(48, 28, 48, 28);
        StateListDrawable bg = new StateListDrawable();
        bg.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Color.DKGRAY));
        bg.addState(new int[]{}, new ColorDrawable(Color.BLACK));
        row.setBackground(bg);
        row.setEnabled(enabled);
        row.setAlpha(enabled ? 1f : 0.4f);
        if (enabled) row.setOnClickListener(tap);

        TextView t = new TextView(this);
        t.setText(title);
        t.setTextColor(Color.WHITE);
        t.setTextSize(19);
        t.setTypeface(font);
        row.addView(t);

        TextView s = new TextView(this);
        s.setText(sub);
        s.setTextColor(Color.GRAY);
        s.setTextSize(13);
        s.setTypeface(font);
        s.setPadding(0, 6, 0, 0);
        row.addView(s);
        return row;
    }

    private View divider() {
        View v = new View(this);
        v.setBackgroundColor(0xFF1C1C1C);
        v.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 1));
        return v;
    }

    private TextView action(String label, int color, View.OnClickListener tap) {
        TextView t = new TextView(this);
        t.setText(label);
        t.setTextColor(color);
        t.setTextSize(16);
        t.setTypeface(font);
        t.setPadding(48, 32, 48, 32);
        StateListDrawable bg = new StateListDrawable();
        bg.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Color.DKGRAY));
        bg.addState(new int[]{}, new ColorDrawable(Color.BLACK));
        t.setBackground(bg);
        t.setOnClickListener(tap);
        return t;
    }
}

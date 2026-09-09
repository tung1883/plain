package com.plainphone.app;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Interactive shell: a {@link ShellSurface} over a persistent session on the
 * daemon. The pty lives in {@code plaind} and keeps running while the phone is
 * away — {@link #onStop} only detaches, and reopening (with the same
 * {@link #EXTRA_SESSION_ID}) reattaches and replays the buffered output.
 */
public class DevTerminalActivity extends Activity implements DevService.StateListener {

    static final String EXTRA_SESSION_ID = "sessionId";

    private String hostId;
    private ShellSurface surface;
    private View headerSpinner;
    private DevService service;

    private final ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            service = ((DevService.LocalBinder) binder).service();
            pushConnection();
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
        long sessionId = getIntent().getLongExtra(EXTRA_SESSION_ID, -1);
        DevHost host = DevHost.find(this, hostId);
        if (host == null) {
            finish();
            return;
        }
        setTaskDescription(new android.app.ActivityManager.TaskDescription(host.label + " · shell"));

        surface = new ShellSurface(this);
        surface.setSessionId(sessionId);
        surface.setCallbacks(new ShellSurface.Callbacks() {
            @Override public void onReconnecting(boolean on) {
                headerSpinner.setVisibility(on ? View.VISIBLE : View.GONE);
            }
            @Override public void onTitle(String name) {
                if (service != null) {
                    service.setActivityDetail(hostId, "shell" + (name != null ? " · " + name : ""));
                }
            }
            @Override public void onExit(int code) {
                Toast.makeText(DevTerminalActivity.this, "Shell exited (" + code + ")",
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        LinearLayout head = UiKit.header(this, host.label + " · shell");
        headerSpinner = UiKit.spinner(this);
        headerSpinner.setVisibility(View.GONE);
        int sp = UiKit.dp(this, 18);
        LinearLayout.LayoutParams splp = new LinearLayout.LayoutParams(sp, sp);
        splp.rightMargin = UiKit.dp(this, 6);
        splp.gravity = Gravity.CENTER_VERTICAL;
        head.addView(headerSpinner, splp);
        head.addView(surface.keyboardButton(this), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);
        root.addView(head, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        View hair = new View(this);
        hair.setBackgroundColor(0xFF1C1C1C);
        root.addView(hair, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 1));
        root.addView(surface, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
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
        surface.focusKeyboardSoon();
        pushConnection();
    }

    @Override
    protected void onStop() {
        super.onStop();
        DevService.removeStateListener(this);
        surface.detachKeepAlive();
        if (service != null) service.setActivityDetail(hostId, null);
        try {
            unbindService(conn);
        } catch (IllegalArgumentException ignored) {
        }
    }

    @Override
    public void onDevState() {
        runOnUiThread(this::pushConnection);
    }

    private void pushConnection() {
        DevConnection live = (service != null && DevService.isConnected(hostId)) ? service.connection(hostId) : null;
        surface.attach(live);
        if (live != null && service != null) service.setActivityDetail(hostId, "shell");
    }
}

package com.plainphone.app;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

/** Remote GUI: a {@link ScreenSurface} over the daemon's {@code screen} channel. */
public class DevScreenActivity extends Activity implements DevService.StateListener {

    private String hostId;
    private ScreenSurface surface;
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
        DevHost host = DevHost.find(this, hostId);
        if (host == null) {
            finish();
            return;
        }
        setTaskDescription(new android.app.ActivityManager.TaskDescription(host.label + " · screen"));

        surface = new ScreenSurface(this);

        LinearLayout head = UiKit.header(this, host.label + " · screen");
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
        pushConnection();
    }

    @Override
    protected void onStop() {
        super.onStop();
        DevService.removeStateListener(this);
        surface.detach();
        if (service != null) service.setActivityDetail(hostId, null);
        try {
            unbindService(conn);
        } catch (IllegalArgumentException ignored) {
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        surface.release();
    }

    @Override
    public void onDevState() {
        runOnUiThread(this::pushConnection);
    }

    private void pushConnection() {
        DevConnection live = (service != null && DevService.isConnected(hostId)) ? service.connection(hostId) : null;
        surface.attach(live);
        if (live != null && service != null) service.setActivityDetail(hostId, "screen");
    }
}

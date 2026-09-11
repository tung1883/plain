package com.plainphone.app;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;

/**
 * Full-screen {@link PerfPanel} over a live host — processes / stats / network
 * / storage behind one tab strip. Reached from {@link DevHostActivity}: bind
 * {@link DevService}, connect the host, and push the live {@link DevConnection}
 * into the panel as it comes and goes.
 */
public class DevPerfActivity extends Activity implements DevService.StateListener {

    private String hostId;
    private PerfPanel panel;
    private DevService service;

    private final ServiceConnection conn = new ServiceConnection() {
        @Override public void onServiceConnected(ComponentName name, IBinder binder) {
            service = ((DevService.LocalBinder) binder).service();
            pushConnection();
        }
        @Override public void onServiceDisconnected(ComponentName name) {
            service = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hostId = getIntent().getStringExtra(DevHostActivity.EXTRA_HOST_ID);
        DevHost host = DevHost.find(this, hostId);
        if (host == null) { finish(); return; }

        panel = new PerfPanel(host.label, hostId);
        View body = panel.onCreate(this);
        setTaskDescription(new ActivityManager.TaskDescription(panel.title()));
        UiKit.screen(this, panel.title(), body);
    }

    @Override protected void onStart() {
        super.onStart();
        DevService.addStateListener(this);
        bindService(new Intent(this, DevService.class), conn, Context.BIND_AUTO_CREATE);
    }

    @Override protected void onResume() {
        super.onResume();
        DevService.connect(this, hostId);
        panel.onShow();
        pushConnection();
    }

    @Override protected void onStop() {
        super.onStop();
        DevService.removeStateListener(this);
        panel.onLeave();   // pause polling; keep the channels for a quick resume
        if (service != null) service.setActivityDetail(hostId, null);
        try { unbindService(conn); } catch (IllegalArgumentException ignored) {}
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        panel.onClose();
    }

    @Override
    public void onDevState() {
        runOnUiThread(this::pushConnection);
    }

    private void pushConnection() {
        if (panel == null) return;
        DevConnection live = (service != null && DevService.isConnected(hostId)) ? service.connection(hostId) : null;
        panel.onConnection(live);
        if (live != null && service != null) service.setActivityDetail(hostId, "perf");
    }
}

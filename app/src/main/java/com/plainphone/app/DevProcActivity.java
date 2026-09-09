package com.plainphone.app;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;

/**
 * A live process view over the {@code proc} channel: an htop-style stats panel,
 * a name/pid filter, and a table that scrolls sideways for long argv lines.
 * The UI is {@link ProcSurface}, shared with the {@link ProcPanel} window.
 */
public class DevProcActivity extends Activity implements DevService.StateListener {

    private String hostId;
    private ProcSurface surface;
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
        setTaskDescription(new android.app.ActivityManager.TaskDescription(host.label + " · processes"));

        surface = new ProcSurface(this);
        UiKit.screen(this, host.label + " · processes", surface);
    }

    @Override protected void onStart() {
        super.onStart();
        DevService.addStateListener(this);
        bindService(new Intent(this, DevService.class), conn, Context.BIND_AUTO_CREATE);
    }

    @Override protected void onResume() {
        super.onResume();
        DevService.connect(this, hostId);
        pushConnection();
    }

    @Override protected void onStop() {
        super.onStop();
        DevService.removeStateListener(this);
        surface.detach();
        if (service != null) service.setActivityDetail(hostId, null);
        try { unbindService(conn); } catch (IllegalArgumentException ignored) {}
    }

    @Override
    public void onDevState() {
        runOnUiThread(this::pushConnection);
    }

    private void pushConnection() {
        DevConnection live = (service != null && DevService.isConnected(hostId)) ? service.connection(hostId) : null;
        surface.attach(live);
        if (live != null && service != null) service.setActivityDetail(hostId, "processes");
    }
}

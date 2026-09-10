package com.plainphone.app;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The list of shells running on a host — each a persistent {@code plaind}
 * session. Tap to (re)attach, long-press for multi-select (rename / kill).
 */
public class DevShellsActivity extends Activity implements DevService.StateListener, SelBarHost {

    private String hostId;
    private SectionListView list;
    private SelectionBar bar;
    private DevService service;
    private DevConnection connection;
    private long channel = -1;
    private boolean opening;

    private List<Map<String, Object>> shells = new ArrayList<>();
    private final LinkedHashSet<String> sel = new LinkedHashSet<>();
    private boolean selecting;

    private final DevConnection.Sink sink = this::onChannelMessage;

    private final ServiceConnection conn = new ServiceConnection() {
        @Override public void onServiceConnected(ComponentName name, IBinder binder) {
            service = ((DevService.LocalBinder) binder).service();
            tryOpen();
        }
        @Override public void onServiceDisconnected(ComponentName name) { service = null; }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hostId = getIntent().getStringExtra(DevHostActivity.EXTRA_HOST_ID);
        DevHost host = DevHost.find(this, hostId);
        if (host == null) { finish(); return; }
        setTaskDescription(new android.app.ActivityManager.TaskDescription(host.label + " · shells"));

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setBackgroundColor(Color.BLACK);

        // Selection bar on top, like the home Workspace section.
        bar = new SelectionBar(this);
        col.addView(bar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        list = new SectionListView(this);
        list.setLongPress(row -> {
            if (!(row.payload instanceof Long)) return false;
            selecting = true;
            sel.add(String.valueOf((long) (Long) row.payload));
            list.refresh();
            return true;
        });
        col.addView(list, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        list.setProvider(this::fill);
        UiKit.screen(this, host.label + " · Shells", col);
    }

    @Override protected void onStart() {
        super.onStart();
        DevService.addStateListener(this);
        bindService(new Intent(this, DevService.class), conn, Context.BIND_AUTO_CREATE);
    }

    @Override protected void onResume() {
        super.onResume();
        DevService.connect(this, hostId);
        tryOpen();
        refresh();
    }

    @Override protected void onStop() {
        super.onStop();
        DevService.removeStateListener(this);
        if (channel >= 0 && connection != null) connection.closeChannel(channel);
        channel = -1;
        opening = false;
        try { unbindService(conn); } catch (IllegalArgumentException ignored) {}
    }

    @Override
    public void onDevState() {
        runOnUiThread(() -> {
            DevConnection live = service != null ? service.connection(hostId) : null;
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
        if (opening || channel >= 0 || service == null || !DevService.isConnected(hostId)) return;
        connection = service.connection(hostId);
        if (connection == null) return;
        opening = true;
        channel = connection.openChannel(sink);
        refresh();
    }

    private void refresh() {
        if (channel >= 0 && connection != null) connection.send(DevProtocol.sessionList(channel));
    }

    private void onChannelMessage(Map<String, Object> msg) {
        if (!DevProtocol.T_SESSION_LIST.equals(DevProtocol.type(msg))) return;
        shells = new ArrayList<>();
        List<Object> raw = DevProtocol.list(msg, "sessions");
        if (raw != null) {
            for (Object o : raw) {
                if (o instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> m = (Map<String, Object>) o;
                    shells.add(m);
                }
            }
        }
        // drop stale selections
        Set<String> live = new java.util.HashSet<>();
        for (Map<String, Object> m : shells) live.add(String.valueOf(DevProtocol.num(m, "id", 0)));
        sel.retainAll(live);
        list.refresh();
    }

    // --- rows ------------------------------------------------------

    private void fill(List<Object> rows) {
        bar.setVisibility(selecting ? android.view.View.VISIBLE : android.view.View.GONE);
        if (selecting) {
            List<String> ids = new ArrayList<>();
            for (Map<String, Object> m : shells) ids.add(String.valueOf(DevProtocol.num(m, "id", 0)));

            List<BarAction> actions = new ArrayList<>();
            if (sel.size() == 1) {
                long id = Long.parseLong(sel.iterator().next());
                actions.add(new BarAction("Rename", () -> UiKit.textPrompt(this, "Rename shell",
                        nameOf(id), "Save", name -> {
                            if (channel >= 0 && connection != null) {
                                connection.send(DevProtocol.sessionRename(channel, id, name));
                            }
                        })));
            }
            actions.add(new BarAction("Kill", () -> VaultUi.confirm(this,
                    "Kill " + sel.size() + " shell" + (sel.size() == 1 ? "" : "s") + "?",
                    null, "Kill", () -> {
                        if (channel >= 0 && connection != null) {
                            for (String s : sel) {
                                connection.send(DevProtocol.sessionKill(channel, Long.parseLong(s)));
                            }
                            connection.send(DevProtocol.sessionList(channel));
                        }
                        exitSelection();
                    }, "Cancel", null)));
            bar.bind(this, ids, actions);

            for (Map<String, Object> m : shells) {
                long id = DevProtocol.num(m, "id", 0);
                rows.add(new SearchResult(SearchResult.Kind.DEV, label(m), sub(m), -1,
                        () -> toggle(String.valueOf(id)), id)
                        .check(sel.contains(String.valueOf(id))));
            }
            return;
        }

        rows.add(new SearchResult(SearchResult.Kind.DEV, "+  New Shell", null, -1, () -> open(-1)));
        for (Map<String, Object> m : shells) {
            long id = DevProtocol.num(m, "id", 0);
            rows.add(new SearchResult(SearchResult.Kind.DEV, label(m), sub(m), -1,
                    () -> open(id), id));
        }
        if (shells.isEmpty()) {
            rows.add(new SearchResult(SearchResult.Kind.DEV, "No shells running", "Start one above",
                    -1, () -> {}));
        }
    }

    private String label(Map<String, Object> m) {
        long id = DevProtocol.num(m, "id", 0);
        String name = DevProtocol.str(m, "name");
        return name == null || name.isEmpty() ? "shell " + id : name;
    }

    private String sub(Map<String, Object> m) {
        return DevProtocol.bool(m, "alive") ? null : "ended";
    }

    private String nameOf(long id) {
        for (Map<String, Object> m : shells) {
            if (DevProtocol.num(m, "id", 0) == id) {
                String n = DevProtocol.str(m, "name");
                return n == null ? "shell " + id : n;
            }
        }
        return "";
    }

    private void open(long id) {
        startActivity(new Intent(this, DevTerminalActivity.class)
                .putExtra(DevHostActivity.EXTRA_HOST_ID, hostId)
                .putExtra(DevTerminalActivity.EXTRA_SESSION_ID, id));
    }

    // --- SelBarHost ----------------------------------------------

    @Override public Set<String> selection() { return sel; }

    @Override public void setSelected(Collection<String> ids) {
        sel.clear();
        sel.addAll(ids);
        list.refresh();
    }

    @Override public void exitSelection() {
        selecting = false;
        sel.clear();
        bar.setVisibility(android.view.View.GONE);
        list.refresh();
    }

    private void toggle(String id) {
        if (!sel.remove(id)) sel.add(id);
        list.refresh();
    }
}

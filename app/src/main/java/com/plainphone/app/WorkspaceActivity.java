package com.plainphone.app;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * A tiling / floating workspace spanning all paired devices: shell, screen and
 * process windows (and plain web windows) that can be dragged, resized,
 * minimised and stacked. Each window keeps its own {@link DevService} link; the
 * daemon sessions outlive the panels, and the layout is saved so leaving and
 * reopening the workspace brings everything back.
 */
public class WorkspaceActivity extends Activity implements DevService.StateListener {

    private PanelHost panelHost;
    private View spinner;
    private LinearLayout taskbar;
    private HorizontalScrollView taskbarScroller;

    private DevService service;
    private final Handler saveDebounce = new Handler();
    private boolean restoring;

    private final ServiceConnection conn = new ServiceConnection() {
        @Override public void onServiceConnected(ComponentName n, IBinder b) {
            service = ((DevService.LocalBinder) b).service();
            syncConnections();
        }
        @Override public void onServiceDisconnected(ComponentName n) {
            service = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTaskDescription(new android.app.ActivityManager.TaskDescription("Workspace"));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);

        LinearLayout head = UiKit.header(this, "Workspace");
        TextView add = new TextView(this);
        add.setText("+");
        add.setTextColor(Color.WHITE);
        add.setTextSize(24);
        add.setTypeface(Fonts.cascadiaMono(this));
        add.setGravity(Gravity.CENTER);
        add.setPadding(dp(24), 0, dp(24), 0);
        add.setOnClickListener(this::showAddMenu);
        head.addView(add, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(head, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        View hair = new View(this);
        hair.setBackgroundColor(0xFF1C1C1C);
        root.addView(hair, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));

        android.widget.FrameLayout stage = new android.widget.FrameLayout(this);
        panelHost = new PanelHost(this);
        panelHost.setListener(this::onPanelsChanged);
        stage.addView(panelHost, new android.widget.FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        spinner = UiKit.spinner(this);
        android.widget.FrameLayout.LayoutParams slp = new android.widget.FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        stage.addView(spinner, slp);
        root.addView(stage, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        taskbar = new LinearLayout(this);
        taskbar.setOrientation(LinearLayout.HORIZONTAL);
        taskbar.setPadding(dp(8), dp(6), dp(8), dp(6));
        taskbarScroller = new HorizontalScrollView(this);
        taskbarScroller.setHorizontalScrollBarEnabled(false);
        taskbarScroller.setBackgroundColor(0xFF0C0C0C);
        taskbarScroller.addView(taskbar);
        taskbarScroller.setVisibility(View.GONE);
        root.addView(taskbarScroller, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        setContentView(root);

        // Rebuild the saved layout once the host has a size.
        panelHost.post(this::restore);
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
        for (String id : neededHosts()) DevService.connect(this, id);
        syncConnections();
    }

    @Override
    protected void onStop() {
        super.onStop();
        DevService.removeStateListener(this);
        panelHost.leaveAll();
        try { unbindService(conn); } catch (IllegalArgumentException ignored) {}
        saveNow();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (panelHost != null) {
            panelHost.leaveAll();
            saveNow();
        }
    }

    @Override
    public void onDevState() {
        runOnUiThread(this::syncConnections);
    }

    private void syncConnections() {
        panelHost.rebind(hostId ->
                (service != null && DevService.isConnected(hostId)) ? service.connection(hostId) : null);
    }

    private List<String> neededHosts() {
        List<String> ids = new ArrayList<>();
        for (Panel p : panelHost.panels()) {
            String h = p.content.hostId();
            if (h != null && !ids.contains(h)) ids.add(h);
        }
        return ids;
    }

    // --- restore / save ---------------------------------------------

    private void restore() {
        restoring = true;
        for (WorkspaceStore.Rec r : WorkspaceStore.load(this)) {
            PanelContent c = build(r);
            if (c == null) continue;
            Panel p = panelHost.addAt(c, r.x, r.y, r.w, r.h, r.minimized);
            c.setTitleListener(p::refreshTitle);
            p.refreshTitle();
        }
        restoring = false;
        for (String id : neededHosts()) DevService.connect(this, id);
        syncConnections();
        spinner.setVisibility(View.GONE);
    }

    private PanelContent build(WorkspaceStore.Rec r) {
        if ("web".equals(r.kind)) return new WebPanel(r.extra);
        String hostId = r.hostId.isEmpty() ? null : r.hostId;
        if (hostId == null || DevHost.find(this, hostId) == null) return null; // device gone
        String label = labelFor(hostId);
        switch (r.kind) {
            case "shell":
                long sid = -1;
                try { sid = Long.parseLong(r.extra); } catch (NumberFormatException ignored) {}
                return new ShellPanel(label, hostId, sid);
            case "screen": return new ScreenPanel(label, hostId);
            case "proc":   return new ProcPanel(label, hostId);
            default: return null;
        }
    }

    private void onPanelsChanged() {
        refreshTaskbar();
        if (restoring) return;
        saveDebounce.removeCallbacksAndMessages(null);
        saveDebounce.postDelayed(this::saveNow, 500);
    }

    private void saveNow() {
        if (panelHost == null) return;
        List<WorkspaceStore.Rec> recs = new ArrayList<>();
        for (Panel p : panelHost.panels()) {
            int[] b = p.bounds();
            recs.add(new WorkspaceStore.Rec(p.content.kind(), p.content.hostId(),
                    b[0], b[1], b[2], b[3], p.minimized(), p.content.saveExtra()));
        }
        WorkspaceStore.save(this, recs);
    }

    // --- add / taskbar --------------------------------------------

    private void showAddMenu(View anchor) {
        popup(anchor, new String[]{"Shell", "Screen", "Processes", "Web"}, choice -> {
            switch (choice) {
                case "Shell":     pickHost(h -> open(new ShellPanel(labelFor(h), h, -1))); break;
                case "Screen":    pickHost(h -> open(new ScreenPanel(labelFor(h), h))); break;
                case "Processes": pickHost(h -> open(new ProcPanel(labelFor(h), h))); break;
                case "Web":       open(new WebPanel()); break;
            }
        });
    }

    /** Resolve a device for a new panel: the only one, or a chooser. */
    private void pickHost(HostPick then) {
        List<DevHost> hosts = DevHost.all(this);
        if (hosts.isEmpty()) {
            Toast.makeText(this, "Pair a device first", Toast.LENGTH_SHORT).show();
            return;
        }
        if (hosts.size() == 1) {
            DevService.connect(this, hosts.get(0).id);
            then.on(hosts.get(0).id);
            return;
        }
        String[] labels = new String[hosts.size()];
        for (int i = 0; i < hosts.size(); i++) {
            DevHost h = hosts.get(i);
            labels[i] = (DevService.isConnected(h.id) ? "● " : "○ ") + h.label;
        }
        popupAt(labels, choice -> {
            for (int i = 0; i < labels.length; i++) {
                if (labels[i].equals(choice)) {
                    DevHost h = hosts.get(i);
                    DevService.connect(this, h.id);
                    then.on(h.id);
                    return;
                }
            }
        });
    }

    private interface HostPick { void on(String hostId); }

    private void open(PanelContent content) {
        Panel p = panelHost.add(content);
        content.setTitleListener(p::refreshTitle);
        if (content.needsConnection() && service != null) {
            DevConnection c = DevService.isConnected(content.hostId())
                    ? service.connection(content.hostId()) : null;
            if (c != null) content.onConnection(c);
        }
        p.refreshTitle();
    }

    private String labelFor(String hostId) {
        DevHost h = DevHost.find(this, hostId);
        return h != null ? h.label : hostId;
    }

    private void refreshTaskbar() {
        taskbar.removeAllViews();
        int hidden = 0;
        for (Panel p : panelHost.panels()) {
            if (p.getVisibility() == View.VISIBLE) continue;
            hidden++;
            TextView chip = new TextView(this);
            chip.setText(p.content.title());
            chip.setTextColor(0xFFD0D0D0);
            chip.setTextSize(12);
            chip.setTypeface(Fonts.cascadiaMono(this));
            chip.setSingleLine(true);
            chip.setBackground(UiKit.rounded(this, 0xFF1A1A1A, 0xFF2C2C2C, 1f, UiKit.R_XS));
            chip.setPadding(dp(14), dp(8), dp(14), dp(8));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.rightMargin = dp(8);
            chip.setLayoutParams(lp);
            final Panel panel = p;
            chip.setOnClickListener(v -> panelHost.restore(panel));
            taskbar.addView(chip);
        }
        taskbarScroller.setVisibility(hidden > 0 ? View.VISIBLE : View.GONE);
    }

    // --- dark popup menu -----------------------------------------

    private interface Pick { void on(String choice); }

    private void popup(View anchor, String[] items, Pick pick) {
        buildPopup(anchor, items, pick, 0, Gravity.END);
    }

    private void popupAt(String[] items, Pick pick) {
        buildPopup(getWindow().getDecorView(), items, pick, 0, Gravity.CENTER);
    }

    private void buildPopup(View anchor, String[] items, Pick pick, int xoff, int grav) {
        LinearLayout menu = new LinearLayout(this);
        menu.setOrientation(LinearLayout.VERTICAL);
        menu.setBackground(UiKit.rounded(this, Color.BLACK, 0xFF3A3A3A, 1.5f, UiKit.R_SM));
        UiKit.clipRounded(this, menu, UiKit.R_SM);
        int pad = dp(6);
        menu.setPadding(pad, pad, pad, pad);

        android.widget.PopupWindow pw = new android.widget.PopupWindow(menu,
                dp(200), ViewGroup.LayoutParams.WRAP_CONTENT, true);
        pw.setElevation(dp(10));

        for (String item : items) {
            TextView row = new TextView(this);
            row.setText(item);
            row.setTextColor(0xFFE6E6E6);
            row.setTextSize(14);
            row.setTypeface(Fonts.cascadiaMono(this));
            row.setPadding(dp(16), dp(12), dp(16), dp(12));
            row.setSingleLine(true);
            final String choice = item;
            row.setOnClickListener(v -> { pw.dismiss(); pick.on(choice); });
            menu.addView(row, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        if (grav == Gravity.CENTER) {
            pw.showAtLocation(anchor, Gravity.CENTER, 0, 0);
        } else {
            pw.showAsDropDown(anchor, xoff, dp(4), grav);
        }
    }

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}

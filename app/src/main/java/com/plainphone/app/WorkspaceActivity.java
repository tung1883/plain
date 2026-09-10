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

    static final String EXTRA_ID = "workspaceId";

    private String workspaceId;
    private PanelHost panelHost;
    private View spinner;
    private TextView titleView;
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
        workspaceId = resolveId(getIntent());

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);

        LinearLayout head = UiKit.header(this, "Workspace");
        if (head.getChildAt(1) instanceof TextView) {
            titleView = (TextView) head.getChildAt(1);
            titleView.setOnClickListener(v -> showWorkspaceMenu(titleView));
        }
        applyName();

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
        panelHost.setListener(new PanelHost.Listener() {
            @Override public void onPanelsChanged() { WorkspaceActivity.this.onPanelsChanged(); }
            @Override public void confirmClose(String title, Runnable doClose) {
                VaultUi.confirm(WorkspaceActivity.this, "Close " + title + "?",
                        "It has unsaved changes.", "Close", doClose::run, "Keep", null);
            }
        });
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
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        SectionImports.onResult(this, req, res, data, () -> panelHost.notifyResumed());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        String want = resolveId(intent);
        if (!want.equals(workspaceId)) switchTo(want);
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
        panelHost.notifyResumed();  // re-check plugin/vault locks after returning from a gate
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

    // --- workspaces -----------------------------------------------

    private String resolveId(Intent intent) {
        String id = intent != null ? intent.getStringExtra(EXTRA_ID) : null;
        if (id != null) {
            for (Workspaces.Meta m : Workspaces.list(this)) {
                if (m.id.equals(id)) { Workspaces.setCurrent(this, id); return id; }
            }
        }
        return Workspaces.currentId(this);
    }

    private void applyName() {
        String name = "Workspace";
        for (Workspaces.Meta m : Workspaces.list(this)) {
            if (m.id.equals(workspaceId)) name = m.name;
        }
        if (titleView != null) titleView.setText(name + "  ▾");
        setTaskDescription(new android.app.ActivityManager.TaskDescription(name));
    }

    private void switchTo(String id) {
        saveNow();
        panelHost.clear();
        workspaceId = id;
        Workspaces.setCurrent(this, id);
        applyName();
        spinner.setVisibility(View.VISIBLE);
        panelHost.post(this::restore);
    }

    private void showWorkspaceMenu(View anchor) {
        List<Workspaces.Meta> all = Workspaces.list(this);
        List<String> items = new ArrayList<>();
        for (Workspaces.Meta m : all) {
            items.add((m.id.equals(workspaceId) ? "● " : "○ ") + m.name);
        }
        items.add("+ New workspace");
        items.add("Rename…");
        if (all.size() > 1) items.add("Delete workspace");
        buildPopup(anchor, items.toArray(new String[0]), choice -> {
            if (choice.equals("+ New workspace")) {
                switchTo(Workspaces.create(this, null).id);
            } else if (choice.equals("Rename…")) {
                promptText("Rename workspace", currentName(), name -> {
                    Workspaces.rename(this, workspaceId, name);
                    applyName();
                });
            } else if (choice.equals("Delete workspace")) {
                VaultUi.confirm(this, "Delete " + currentName() + "?", null,
                        "Delete", () -> {
                            String gone = workspaceId;
                            Workspaces.delete(this, gone);
                            switchTo(Workspaces.currentId(this));
                        }, "Cancel", null);
            } else {
                for (Workspaces.Meta m : all) {
                    if (choice.endsWith(m.name) && !m.id.equals(workspaceId)) {
                        switchTo(m.id);
                        return;
                    }
                }
            }
        }, 0, Gravity.START);
    }

    private String currentName() {
        for (Workspaces.Meta m : Workspaces.list(this)) {
            if (m.id.equals(workspaceId)) return m.name;
        }
        return "Workspace";
    }

    private void promptText(String title, String initial, java.util.function.Consumer<String> onOk) {
        final android.widget.EditText f = new android.widget.EditText(this);
        f.setText(initial == null ? "" : initial);
        f.setSelectAllOnFocus(true);
        f.setTextColor(Color.WHITE);
        int p = dp(16);
        f.setPadding(p, p, p, p);
        new android.app.AlertDialog.Builder(this)
                .setTitle(title)
                .setView(f)
                .setPositiveButton("OK", (d, w) -> onOk.accept(f.getText().toString()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    // --- restore / save ---------------------------------------------

    private void restore() {
        restoring = true;
        for (WorkspaceStore.Rec r : WorkspaceStore.load(this, workspaceId)) {
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
        PanelKind k = PanelKind.byId(r.kind);
        if (k == null) return null;
        String hostId = r.hostId.isEmpty() ? null : r.hostId;
        if (k.needsDevice && (hostId == null || DevHost.find(this, hostId) == null)) {
            return null; // device gone
        }
        return k.factory.create(this, hostId, r.extra);
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
        WorkspaceStore.save(this, workspaceId, recs);
    }

    // --- add / taskbar --------------------------------------------

    private void showAddMenu(View anchor) {
        String[] labels = new String[PanelKind.ALL.size()];
        for (int i = 0; i < labels.length; i++) labels[i] = PanelKind.ALL.get(i).label;
        popup(anchor, labels, choice -> {
            for (PanelKind k : PanelKind.ALL) {
                if (!k.label.equals(choice)) continue;
                if (k.needsDevice) {
                    pickHost(h -> open(k.factory.create(this, h, "")));
                } else {
                    open(k.factory.create(this, null, ""));
                }
                return;
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

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
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A live process view over the {@code proc} channel: an htop-style stats panel,
 * a name/pid filter, and a table that scrolls sideways for long argv lines.
 * Tap a column header to sort by it (tap again to flip); tap a row to signal it.
 */
public class DevProcActivity extends Activity implements DevService.StateListener {

    private enum Sort { PID, CPU, MEM, STATE, USER, NAME }

    private String hostId;
    private Typeface font;
    private DevService service;
    private DevConnection connection;
    private long channel = -1;
    private boolean opening;

    private LinearLayout statsPanel;
    private EditText searchBox;
    private LinearLayout headerRow;
    private LinearLayout rowsBox;
    private HorizontalScrollView hsv;
    private ScrollView vs;
    private TextView emptyLabel;

    private final android.os.Handler poll = new android.os.Handler();
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            refresh();
            poll.postDelayed(this, 2000);
        }
    };

    private List<Map<String, Object>> procs = new ArrayList<>();
    private Map<String, Object> sys;
    private String query = "";
    private Sort sort = Sort.CPU;
    private boolean descending = true;

    private final DevConnection.Sink sink = this::onChannelMessage;

    private final ServiceConnection conn = new ServiceConnection() {
        @Override public void onServiceConnected(ComponentName name, IBinder binder) {
            service = ((DevService.LocalBinder) binder).service();
            tryOpen();
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
        font = Fonts.cascadiaMono(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);

        statsPanel = new LinearLayout(this);
        statsPanel.setOrientation(LinearLayout.VERTICAL);
        statsPanel.setBackground(UiKit.rounded(this, 0xFF0B0B0B, 0xFF1C1C1C, 1f, UiKit.R_MD));
        statsPanel.setPadding(dp(18), dp(14), dp(18), dp(14));
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sp.setMargins(dp(20), dp(12), dp(20), dp(4));
        root.addView(statsPanel, sp);

        root.addView(buildSearch());

        headerRow = tableRow();
        headerRow.setBackgroundColor(Color.BLACK);
        hsv = new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);
        hsv.setFillViewport(true);
        LinearLayout tableCol = new LinearLayout(this);
        tableCol.setOrientation(LinearLayout.VERTICAL);
        tableCol.addView(headerRow, wrap());
        tableCol.addView(rule());
        vs = new ScrollView(this);
        vs.setFillViewport(true);
        rowsBox = new LinearLayout(this);
        rowsBox.setOrientation(LinearLayout.VERTICAL);
        vs.addView(rowsBox, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        tableCol.addView(vs, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, 0, 1f));
        hsv.addView(tableCol);
        root.addView(hsv, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        emptyLabel = new TextView(this);
        emptyLabel.setTextColor(Color.GRAY);
        emptyLabel.setTextSize(13);
        emptyLabel.setTypeface(font);
        emptyLabel.setPadding(dp(20), dp(30), dp(20), dp(30));
        emptyLabel.setText("Connecting…");
        rowsBox.addView(emptyLabel);

        renderHeader();
        UiKit.screen(this, host.label + " · processes", root);
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
        poll.removeCallbacks(tick);
        poll.postDelayed(tick, 1200);
    }

    @Override protected void onStop() {
        super.onStop();
        DevService.removeStateListener(this);
        poll.removeCallbacks(tick);
        if (channel >= 0 && connection != null) connection.closeChannel(channel);
        channel = -1;
        opening = false;
        if (service != null) service.setActivityDetail(null);
        try { unbindService(conn); } catch (IllegalArgumentException ignored) {}
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
        refresh();
    }

    private void refresh() {
        if (channel >= 0 && connection != null) {
            connection.send(DevProtocol.procList(channel));
        }
    }

    @SuppressWarnings("unchecked")
    private void onChannelMessage(Map<String, Object> msg) {
        String type = DevProtocol.type(msg);
        if (DevProtocol.T_PROC_LIST.equals(type)) {
            procs = new ArrayList<>();
            List<Object> raw = DevProtocol.list(msg, "procs");
            if (raw != null) {
                for (Object o : raw) if (o instanceof Map) procs.add((Map<String, Object>) o);
            }
            Object s = msg.get("sys");
            sys = s instanceof Map ? (Map<String, Object>) s : null;
            renderStats();
            renderRows();
        } else if (DevProtocol.T_PROC_KILLED.equals(type)) {
            refresh();
        }
    }

    // --- stats panel ----------------------------------------------------

    private void renderStats() {
        statsPanel.removeAllViews();
        if (sys == null) return;
        long memU = DevProtocol.num(sys, "mem_used_kb", 0);
        long memT = DevProtocol.num(sys, "mem_total_kb", 1);
        long swU = DevProtocol.num(sys, "swap_used_kb", 0);
        long swT = DevProtocol.num(sys, "swap_total_kb", 0);
        statsPanel.addView(bar("CPU", DevProtocol.dbl(sys, "cpu", 0) / 100.0, 0xFFD0D0D0));
        statsPanel.addView(bar("MEM", memT > 0 ? (double) memU / memT : 0, 0xFF9A9A9A));
        statsPanel.addView(bar("SWP", swT > 0 ? (double) swU / swT : 0, 0xFF4A4A4A));

        List<Object> load = DevProtocol.list(sys, "load");
        StringBuilder meta = new StringBuilder();
        // load average is a Unix concept — Windows reports 0/0/0, so hide it then.
        if (load != null && load.size() == 3
                && (num1(load.get(0)) > 0 || num1(load.get(1)) > 0 || num1(load.get(2)) > 0)) {
            meta.append("load ").append(fmt1(load.get(0))).append("  ")
                    .append(fmt1(load.get(1))).append("  ").append(fmt1(load.get(2)));
        }
        long up = DevProtocol.num(sys, "uptime_s", 0);
        if (up > 0) {
            if (meta.length() > 0) meta.append("   ·   ");
            meta.append("up ").append(uptime(up));
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> tasks = sys.get("tasks") instanceof Map
                ? (Map<String, Object>) sys.get("tasks") : null;
        if (tasks != null) {
            if (meta.length() > 0) meta.append("   ·   ");
            meta.append(DevProtocol.num(tasks, "total", 0)).append(" tasks");
        }
        statsPanel.addView(metaText(meta.toString()));
        if (tasks != null) {
            statsPanel.addView(metaText(String.format(Locale.US,
                    "run %d   sleep %d   stopped %d   zombie %d",
                    DevProtocol.num(tasks, "running", 0),
                    DevProtocol.num(tasks, "sleeping", 0),
                    DevProtocol.num(tasks, "stopped", 0),
                    DevProtocol.num(tasks, "zombie", 0))));
        }
    }

    private View bar(String label, double frac, int fillColor) {
        final double f = Math.max(0, Math.min(1, frac));
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(4), 0, dp(4));

        TextView l = new TextView(this);
        l.setText(label);
        l.setTextColor(0xFF8B8B8B);
        l.setTextSize(11);
        l.setTypeface(font);
        l.setWidth(dp(34));
        row.addView(l);

        android.widget.FrameLayout fl = new android.widget.FrameLayout(this);
        fl.setBackground(UiKit.rounded(this, 0xFF161616, 0, 0f, 4f));
        fl.setClipToOutline(true);
        LinearLayout.LayoutParams flp = new LinearLayout.LayoutParams(0, dp(8), 1f);
        flp.rightMargin = dp(10);
        View fill = new View(this);
        fill.setBackground(UiKit.rounded(this, fillColor, 0, 0f, 4f));
        fl.addView(fill, new android.widget.FrameLayout.LayoutParams(0, dp(8)));
        row.addView(fl, flp);
        fl.post(() -> {
            android.widget.FrameLayout.LayoutParams lp =
                    (android.widget.FrameLayout.LayoutParams) fill.getLayoutParams();
            lp.width = (int) (fl.getWidth() * f);
            fill.setLayoutParams(lp);
        });

        TextView pct = new TextView(this);
        pct.setText(Math.round(frac * 100) + "%");
        pct.setTextColor(Color.WHITE);
        pct.setTextSize(11);
        pct.setTypeface(font);
        pct.setWidth(dp(42));
        pct.setGravity(Gravity.END);
        row.addView(pct);
        return row;
    }

    private TextView metaText(String t) {
        TextView v = new TextView(this);
        v.setText(t);
        v.setTextColor(0xFF8B8B8B);
        v.setTextSize(11);
        v.setTypeface(font);
        v.setPadding(0, dp(6), 0, 0);
        return v;
    }

    // --- search -------------------------------------------------------

    private View buildSearch() {
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.HORIZONTAL);
        wrap.setPadding(dp(20), dp(8), dp(20), dp(8));
        wrap.setBackgroundColor(Color.BLACK);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(6), dp(12), dp(6));
        row.setBackground(UiKit.rounded(this, 0xFF0E0E0E, 0xFF262626, 1f, UiKit.R_SM));
        wrap.addView(row, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView mag = new TextView(this);
        mag.setText("⌕");
        mag.setTextColor(0xFF484848);
        mag.setTextSize(16);
        mag.setTypeface(font);
        mag.setPadding(0, 0, dp(12), 0);
        row.addView(mag);

        searchBox = new EditText(this);
        searchBox.setHint("filter by name or pid");
        searchBox.setHintTextColor(0xFF5A5A5A);
        searchBox.setTextColor(Color.WHITE);
        searchBox.setTextSize(13);
        searchBox.setTypeface(font);
        searchBox.setBackgroundColor(Color.TRANSPARENT);
        searchBox.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        searchBox.setSingleLine(true);
        searchBox.setPadding(0, dp(8), 0, dp(8));
        searchBox.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) {
                query = s.toString().trim();
                renderRows();
            }
            public void afterTextChanged(Editable s) {}
        });
        row.addView(searchBox, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView clr = new TextView(this);
        clr.setText("✕");
        clr.setTextColor(0xFF484848);
        clr.setTextSize(13);
        clr.setTypeface(font);
        clr.setPadding(dp(12), dp(8), 0, dp(8));
        clr.setOnClickListener(v -> searchBox.setText(""));
        row.addView(clr);
        return wrap;
    }

    // --- table ------------------------------------------------------

    private static final int[] COL_DP = {64, 56, 28, 64, 76}; // PID CPU ST MEM USER
    private static final String[] COL_LABEL = {"PID", "CPU%", "ST", "MEM", "USER", "NAME"};
    private static final Sort[] COL_SORT =
            {Sort.PID, Sort.CPU, Sort.STATE, Sort.MEM, Sort.USER, Sort.NAME};

    private LinearLayout tableRow() {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setPadding(dp(20), dp(10), dp(20), dp(10));
        return r;
    }

    private void renderHeader() {
        headerRow.removeAllViews();
        for (int i = 0; i < COL_LABEL.length; i++) {
            String tag = COL_LABEL[i];
            if (COL_SORT[i] == sort) tag += descending ? " ▾" : " ▴";
            TextView t = new TextView(this);
            t.setText(tag);
            t.setTextColor(COL_SORT[i] == sort ? Color.WHITE : 0xFF484848);
            t.setTextSize(11);
            t.setTypeface(font);
            t.setSingleLine(true);
            if (i < COL_DP.length) t.setWidth(dp(COL_DP[i]));
            final int idx = i;
            t.setOnClickListener(v -> {
                if (sort == COL_SORT[idx]) {
                    descending = !descending;
                } else {
                    sort = COL_SORT[idx];
                    descending = sort == Sort.CPU || sort == Sort.MEM || sort == Sort.PID;
                }
                renderHeader();
                renderRows();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.rightMargin = dp(14);
            headerRow.addView(t, lp);
        }
    }

    private void renderRows() {
        int sx = hsv != null ? hsv.getScrollX() : 0;
        int sy = vs != null ? vs.getScrollY() : 0;
        rowsBox.removeAllViews();
        List<Map<String, Object>> shown = new ArrayList<>();
        String q = query.toLowerCase(Locale.US);
        for (Map<String, Object> p : procs) {
            if (q.isEmpty()) { shown.add(p); continue; }
            String name = str(p, "name").toLowerCase(Locale.US);
            String pid = String.valueOf(DevProtocol.num(p, "pid", 0));
            if (name.contains(q) || pid.contains(q)) shown.add(p);
        }
        shown.sort(this::compare);

        if (shown.isEmpty()) {
            emptyLabel.setText(procs.isEmpty() ? "No processes reported." : "No match.");
            rowsBox.addView(emptyLabel);
            return;
        }
        for (Map<String, Object> p : shown) rowsBox.addView(procRow(p));
        rowsBox.post(() -> {
            if (hsv != null) hsv.scrollTo(sx, 0);
            if (vs != null) vs.scrollTo(0, sy);
        });
    }

    private int compare(Map<String, Object> a, Map<String, Object> b) {
        int c;
        switch (sort) {
            case PID: c = Long.compare(DevProtocol.num(a, "pid", 0), DevProtocol.num(b, "pid", 0)); break;
            case MEM: c = Long.compare(DevProtocol.num(a, "mem_kb", 0), DevProtocol.num(b, "mem_kb", 0)); break;
            case STATE: c = str(a, "state").compareTo(str(b, "state")); break;
            case USER: c = str(a, "user").compareToIgnoreCase(str(b, "user")); break;
            case NAME: c = str(a, "name").compareToIgnoreCase(str(b, "name")); break;
            case CPU:
            default: c = Double.compare(DevProtocol.dbl(a, "cpu", 0), DevProtocol.dbl(b, "cpu", 0));
        }
        return descending ? -c : c;
    }

    private View procRow(Map<String, Object> p) {
        LinearLayout r = tableRow();
        r.setPadding(dp(20), dp(9), dp(20), dp(9));
        StateListDrawable bg = new StateListDrawable();
        bg.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Color.DKGRAY));
        bg.addState(new int[]{}, new ColorDrawable(Color.BLACK));
        r.setBackground(bg);

        long pid = DevProtocol.num(p, "pid", 0);
        String name = str(p, "name");
        if (name.isEmpty()) name = "?";
        cell(r, String.valueOf(pid), COL_DP[0], Color.WHITE);
        cell(r, String.format(Locale.US, "%.1f", DevProtocol.dbl(p, "cpu", 0)), COL_DP[1], Color.WHITE);
        cell(r, str(p, "state"), COL_DP[2], 0xFF8B8B8B);
        cell(r, mem(DevProtocol.num(p, "mem_kb", 0)), COL_DP[3], 0xFF8B8B8B);
        cell(r, str(p, "user"), COL_DP[4], 0xFF8B8B8B);
        cell(r, name, -1, Color.WHITE); // NAME: no fixed width, grows the row

        final String fname = name;
        r.setOnClickListener(v -> VaultUi.tasksDialog(this,
                "Signal " + fname + " (" + pid + ")?",
                new ArrayList<>(),
                new String[]{"SIGTERM", "SIGKILL", "Cancel"},
                new VaultUi.Choice[]{
                        () -> kill(pid, "TERM"),
                        () -> kill(pid, "KILL"),
                        () -> {},
                }));
        return r;
    }

    private void cell(LinearLayout row, String text, int widthDp, int color) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextColor(color);
        t.setTextSize(13);
        t.setTypeface(font);
        t.setSingleLine(true);
        if (widthDp > 0) t.setWidth(dp(widthDp));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.rightMargin = dp(14);
        row.addView(t, lp);
    }

    private void kill(long pid, String sig) {
        if (channel >= 0 && connection != null) {
            connection.send(DevProtocol.procKill(channel, pid, sig));
        }
    }

    // --- helpers ----------------------------------------------------

    private View rule() {
        View v = new View(this);
        v.setBackgroundColor(0xFF1C1C1C);
        v.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 1));
        return v;
    }

    private LinearLayout.LayoutParams wrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private static double num1(Object o) {
        return o instanceof Number ? ((Number) o).doubleValue() : 0;
    }

    private static String fmt1(Object o) {
        return String.format(Locale.US, "%.2f", num1(o));
    }

    private static String uptime(long s) {
        long d = s / 86400, h = (s % 86400) / 3600, m = (s % 3600) / 60;
        return d > 0 ? String.format(Locale.US, "%dd %02d:%02d", d, h, m)
                : String.format(Locale.US, "%02d:%02d", h, m);
    }

    private static String mem(long kb) {
        if (kb >= 1024 * 1024) return String.format(Locale.US, "%.1fG", kb / 1024f / 1024f);
        if (kb >= 1024) return String.format(Locale.US, "%.0fM", kb / 1024f);
        return kb + "K";
    }

    private String str(Map<String, Object> m, String key) {
        String v = DevProtocol.str(m, key);
        return v == null ? "" : v;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}

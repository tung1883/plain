package com.plainphone.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Handler;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The whole live process view — htop-style stats panel, name/pid filter, and a
 * sideways-scrolling sortable table with tap-to-signal — plus the {@code proc}
 * channel, as one reusable view. Shared by {@link DevProcActivity} and
 * {@link ProcPanel} so the two look identical.
 */
@SuppressLint("ViewConstructor")
final class ProcSurface extends LinearLayout {

    private enum Sort { PID, CPU, MEM, STATE, USER, NAME }

    private final Context ctx;
    private final Typeface font;

    private DevConnection connection;
    private long channel = -1;
    private boolean opening;

    private final LinearLayout statsPanel;
    private EditText searchBox;
    private final LinearLayout headerRow;
    private final android.widget.ListView procList;
    private final ProcAdapter adapter = new ProcAdapter();
    private final TextView emptyLabel;
    private final View statsSpinner;
    private final View listSpinner;

    private static final long POLL_MS = 2500;
    private final Handler poll = new Handler();
    private boolean polling;
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            refresh();
            poll.postDelayed(this, POLL_MS);
        }
    };

    private List<Map<String, Object>> procs = new ArrayList<>();
    private Map<String, Object> sys;
    private String query = "";
    private Sort sort = Sort.CPU;
    private boolean descending = true;

    private final DevConnection.Sink sink = this::onChannelMessage;

    ProcSurface(Context ctx) {
        super(ctx);
        this.ctx = ctx;
        this.font = Fonts.cascadiaMono(ctx);
        setOrientation(VERTICAL);
        setBackgroundColor(Color.BLACK);

        statsPanel = new LinearLayout(ctx);
        statsPanel.setOrientation(LinearLayout.VERTICAL);
        statsPanel.setGravity(Gravity.CENTER);
        statsPanel.setBackground(UiKit.rounded(ctx, 0xFF0B0B0B, 0xFF1C1C1C, 1f, UiKit.R_MD));
        statsPanel.setPadding(dp(18), dp(14), dp(18), dp(14));
        statsPanel.setMinimumHeight(dp(150));   // hold height so it doesn't jump on load
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(150));
        sp.setMargins(dp(20), dp(12), dp(20), dp(4));
        addView(statsPanel, sp);
        statsSpinner = UiKit.spinner(ctx);
        statsPanel.addView(statsSpinner, new LinearLayout.LayoutParams(dp(24), dp(24)));

        addView(buildSearch());

        headerRow = tableRow();
        headerRow.setBackgroundColor(Color.BLACK);
        addView(headerRow, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        addView(rule(), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));

        emptyLabel = new TextView(ctx);
        emptyLabel.setTextColor(Color.GRAY);
        emptyLabel.setTextSize(13);
        emptyLabel.setTypeface(font);
        emptyLabel.setPadding(dp(20), dp(30), dp(20), dp(30));

        FrameLayout tableWrap = new FrameLayout(ctx);
        procList = new android.widget.ListView(ctx);
        procList.setBackgroundColor(Color.BLACK);
        procList.setDivider(null);
        procList.setDividerHeight(0);
        procList.setVerticalScrollBarEnabled(false);
        procList.setOverScrollMode(OVER_SCROLL_NEVER);
        procList.setSelector(new ColorDrawable(Color.TRANSPARENT));
        procList.setAdapter(adapter);
        procList.setOnItemClickListener((parent, v, pos, id) -> signal(adapter.at(pos)));
        tableWrap.addView(procList, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        tableWrap.addView(emptyLabel, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER_HORIZONTAL));
        emptyLabel.setVisibility(View.GONE);
        listSpinner = UiKit.spinner(ctx);
        tableWrap.addView(listSpinner, new FrameLayout.LayoutParams(dp(24), dp(24), Gravity.CENTER));
        addView(tableWrap, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        renderHeader();
    }

    // --- connection -----------------------------------------------

    private boolean shown = true;

    void attach(DevConnection conn) {
        if (conn == null) {
            connection = null;
            channel = -1;
            opening = false;
            stopPoll();
            return;
        }
        if (connection != null && connection != conn) {
            connection = null;
            channel = -1;
            opening = false;
        }
        if (!opening && channel < 0) {
            connection = conn;
            opening = true;
            channel = conn.openChannel(sink);
        } else {
            connection = conn;
        }
        startPoll();
    }

    void detach() {
        stopPoll();
        if (channel >= 0 && connection != null) connection.closeChannel(channel);
        channel = -1;
        opening = false;
    }

    /** Panel minimised / activity stopped — pause the 2.5 s poll, keep the channel. */
    void setShown(boolean visible) {
        shown = visible;
        if (visible) startPoll();
        else stopPoll();
    }

    private void startPoll() {
        if (polling || !shown || channel < 0 || connection == null) return;
        polling = true;
        poll.removeCallbacks(tick);
        poll.post(tick);
    }

    private void stopPoll() {
        polling = false;
        poll.removeCallbacks(tick);
    }

    private void refresh() {
        if (channel >= 0 && connection != null) connection.send(DevProtocol.procList(channel));
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

    // --- stats panel ---------------------------------------------------

    private void renderStats() {
        statsPanel.removeAllViews();
        if (sys == null) {
            statsPanel.addView(statsSpinner, new LinearLayout.LayoutParams(dp(24), dp(24)));
            return;
        }
        long memU = DevProtocol.num(sys, "mem_used_kb", 0);
        long memT = DevProtocol.num(sys, "mem_total_kb", 1);
        long swU = DevProtocol.num(sys, "swap_used_kb", 0);
        long swT = DevProtocol.num(sys, "swap_total_kb", 0);
        statsPanel.addView(bar("CPU", DevProtocol.dbl(sys, "cpu", 0) / 100.0, 0xFFD0D0D0));
        statsPanel.addView(bar("MEM", memT > 0 ? (double) memU / memT : 0, 0xFF9A9A9A));
        statsPanel.addView(bar("SWP", swT > 0 ? (double) swU / swT : 0, 0xFF4A4A4A));

        List<Object> load = DevProtocol.list(sys, "load");
        StringBuilder meta = new StringBuilder();
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
        final float f = (float) Math.max(0, Math.min(1, frac));
        LinearLayout row = new LinearLayout(ctx);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(4), 0, dp(4));

        TextView l = new TextView(ctx);
        l.setText(label);
        l.setTextColor(0xFF8B8B8B);
        l.setTextSize(11);
        l.setTypeface(font);
        l.setWidth(dp(34));
        row.addView(l);

        // Weighted fill + spacer — no post-layout callback, never renders empty.
        LinearLayout track = new LinearLayout(ctx);
        track.setOrientation(LinearLayout.HORIZONTAL);
        track.setBackground(UiKit.rounded(ctx, 0xFF161616, 0, 0f, 4f));
        UiKit.clipRounded(ctx, track, 4f);
        LinearLayout.LayoutParams trackP = new LinearLayout.LayoutParams(0, dp(8), 1f);
        trackP.rightMargin = dp(10);
        View fill = new View(ctx);
        fill.setBackgroundColor(fillColor);
        track.addView(fill, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, f));
        if (f < 1f) {
            View spacer = new View(ctx);
            track.addView(spacer, new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.MATCH_PARENT, 1f - f));
        }
        row.addView(track, trackP);

        TextView pct = new TextView(ctx);
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
        TextView v = new TextView(ctx);
        v.setText(t);
        v.setTextColor(0xFF8B8B8B);
        v.setTextSize(11);
        v.setTypeface(font);
        v.setPadding(0, dp(6), 0, 0);
        return v;
    }

    // --- search ------------------------------------------------------

    private View buildSearch() {
        LinearLayout wrap = new LinearLayout(ctx);
        wrap.setOrientation(LinearLayout.HORIZONTAL);
        wrap.setPadding(dp(20), dp(8), dp(20), dp(8));
        wrap.setBackgroundColor(Color.BLACK);

        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(6), dp(12), dp(6));
        row.setBackground(UiKit.rounded(ctx, Color.BLACK, 0xFF262626, 1f, UiKit.R_SM));
        wrap.addView(row, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView mag = new TextView(ctx);
        mag.setText("⌕");
        mag.setTextColor(0xFF484848);
        mag.setTextSize(16);
        mag.setTypeface(font);
        mag.setPadding(0, 0, dp(12), 0);
        row.addView(mag);

        searchBox = new EditText(ctx);
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

        TextView clr = new TextView(ctx);
        clr.setText("✕");
        clr.setTextColor(0xFF484848);
        clr.setTextSize(13);
        clr.setTypeface(font);
        clr.setPadding(dp(12), dp(8), 0, dp(8));
        clr.setOnClickListener(v -> searchBox.setText(""));
        row.addView(clr);
        return wrap;
    }

    // --- table -----------------------------------------------------

    private static final int[] COL_DP = {64, 56, 28, 64};
    private static final String[] COL_LABEL = {"PID", "CPU%", "ST", "MEM", "NAME"};
    private static final Sort[] COL_SORT =
            {Sort.PID, Sort.CPU, Sort.STATE, Sort.MEM, Sort.NAME};

    private LinearLayout tableRow() {
        LinearLayout r = new LinearLayout(ctx);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setPadding(dp(20), dp(10), dp(20), dp(10));
        return r;
    }

    private void renderHeader() {
        headerRow.removeAllViews();
        for (int i = 0; i < COL_LABEL.length; i++) {
            String tag = COL_LABEL[i];
            if (COL_SORT[i] == sort) tag += descending ? " ▾" : " ▴";
            TextView t = new TextView(ctx);
            t.setText(tag);
            t.setTextColor(COL_SORT[i] == sort ? Color.WHITE : 0xFF484848);
            t.setTextSize(11);
            t.setTypeface(font);
            t.setSingleLine(true);
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
            LinearLayout.LayoutParams lp = cellParams(i);
            headerRow.addView(t, lp);
        }
    }

    /** Column layout: fixed widths for PID..USER, the rest for NAME. */
    private LinearLayout.LayoutParams cellParams(int col) {
        LinearLayout.LayoutParams lp = col < COL_DP.length
                ? new LinearLayout.LayoutParams(dp(COL_DP[col]), ViewGroup.LayoutParams.WRAP_CONTENT)
                : new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.rightMargin = dp(14);
        return lp;
    }

    private void renderRows() {
        listSpinner.setVisibility(View.GONE);
        List<Map<String, Object>> shown = new ArrayList<>();
        String q = query.toLowerCase(Locale.US);
        for (Map<String, Object> p : procs) {
            if (q.isEmpty()) { shown.add(p); continue; }
            String name = str(p, "name").toLowerCase(Locale.US);
            String pid = String.valueOf(DevProtocol.num(p, "pid", 0));
            if (name.contains(q) || pid.contains(q)) shown.add(p);
        }
        shown.sort(this::compare);
        adapter.setData(shown);
        emptyLabel.setText(procs.isEmpty() ? "No processes reported." : "No match.");
        emptyLabel.setVisibility(shown.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private int compare(Map<String, Object> a, Map<String, Object> b) {
        int c;
        switch (sort) {
            case PID: c = Long.compare(DevProtocol.num(a, "pid", 0), DevProtocol.num(b, "pid", 0)); break;
            case MEM: c = Long.compare(DevProtocol.num(a, "mem_kb", 0), DevProtocol.num(b, "mem_kb", 0)); break;
            case STATE: c = str(a, "state").compareTo(str(b, "state")); break;
            case NAME: c = str(a, "name").compareToIgnoreCase(str(b, "name")); break;
            case CPU:
            default: c = Double.compare(DevProtocol.dbl(a, "cpu", 0), DevProtocol.dbl(b, "cpu", 0));
        }
        return descending ? -c : c;
    }

    private void signal(Map<String, Object> p) {
        if (p == null || !(ctx instanceof Activity)) return;
        long pid = DevProtocol.num(p, "pid", 0);
        String name = str(p, "name");
        final String fn = name.isEmpty() ? "?" : name;
        VaultUi.tasksDialog((Activity) ctx, "Signal " + fn + " (" + pid + ")?",
                new ArrayList<>(), new String[]{"SIGTERM", "SIGKILL", "Cancel"},
                new VaultUi.Choice[]{
                        () -> kill(pid, "TERM"),
                        () -> kill(pid, "KILL"),
                        () -> {},
                });
    }

    /** Recycling adapter for the process rows — the whole point of not rebuilding
     *  hundreds of views every poll. */
    private final class ProcAdapter extends android.widget.BaseAdapter {
        private List<Map<String, Object>> data = new ArrayList<>();

        void setData(List<Map<String, Object>> d) {
            data = d;
            notifyDataSetChanged();
        }

        Map<String, Object> at(int i) { return i >= 0 && i < data.size() ? data.get(i) : null; }

        @Override public int getCount() { return data.size(); }
        @Override public Object getItem(int i) { return at(i); }
        @Override public long getItemId(int i) { return i; }

        @Override
        public View getView(int pos, View convert, ViewGroup parent) {
            TextView[] cells;
            LinearLayout row;
            if (convert instanceof LinearLayout && convert.getTag() instanceof TextView[]) {
                row = (LinearLayout) convert;
                cells = (TextView[]) convert.getTag();
            } else {
                row = new LinearLayout(ctx);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(dp(20), dp(9), dp(20), dp(9));
                StateListDrawable bg = new StateListDrawable();
                bg.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Color.DKGRAY));
                bg.addState(new int[]{}, new ColorDrawable(Color.BLACK));
                row.setBackground(bg);
                cells = new TextView[COL_LABEL.length];
                int[] colors = {Color.WHITE, Color.WHITE, 0xFF8B8B8B, 0xFF8B8B8B, Color.WHITE};
                for (int i = 0; i < cells.length; i++) {
                    TextView t = new TextView(ctx);
                    t.setTextColor(colors[i]);
                    t.setTextSize(13);
                    t.setTypeface(font);
                    t.setSingleLine(true);
                    if (i == COL_LABEL.length - 1) t.setEllipsize(android.text.TextUtils.TruncateAt.END);
                    row.addView(t, cellParams(i));
                    cells[i] = t;
                }
                row.setTag(cells);
            }
            Map<String, Object> p = data.get(pos);
            String name = str(p, "name");
            cells[0].setText(String.valueOf(DevProtocol.num(p, "pid", 0)));
            cells[1].setText(String.format(Locale.US, "%.1f", DevProtocol.dbl(p, "cpu", 0)));
            cells[2].setText(str(p, "state"));
            cells[3].setText(mem(DevProtocol.num(p, "mem_kb", 0)));
            cells[4].setText(name.isEmpty() ? "?" : name);
            return row;
        }
    }

    private void kill(long pid, String sig) {
        if (channel >= 0 && connection != null) {
            connection.send(DevProtocol.procKill(channel, pid, sig));
        }
    }

    // --- helpers --------------------------------------------------

    private View rule() {
        View v = new View(ctx);
        v.setBackgroundColor(0xFF1C1C1C);
        v.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
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

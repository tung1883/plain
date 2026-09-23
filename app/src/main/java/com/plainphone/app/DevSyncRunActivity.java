package com.plainphone.app;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** One pair's detail + run screen — see the approved mockup's run-detail screen. */
public class DevSyncRunActivity extends Activity {

    static final String EXTRA_PAIR_ID = "pairId";

    private String hostId, pairId;
    private Typeface font;
    private LinearLayout root;
    private ScrollView scroller;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hostId = getIntent().getStringExtra(DevHostActivity.EXTRA_HOST_ID);
        pairId = getIntent().getStringExtra(EXTRA_PAIR_ID);
        font = Fonts.current(this);
        DevSyncPair pair = DevSyncPair.find(this, pairId);
        if (pair == null) { finish(); return; }

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(48, 24, 48, 24);
        scroller = new ScrollView(this);
        scroller.setBackgroundColor(Color.BLACK);
        scroller.addView(root, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        UiKit.screen(this, pair.label != null && !pair.label.isEmpty() ? pair.label : "Sync pair", scroller);
        render();
    }

    @Override
    protected void onResume() {
        super.onResume();
        DevSyncJobs.addListener(this::onChanged);
        render();
    }

    @Override
    protected void onPause() {
        super.onPause();
        DevSyncJobs.removeListener(this::onChanged);
    }

    private void onChanged() {
        runOnUiThread(this::render);
    }

    private void render() {
        DevSyncPair pair = DevSyncPair.find(this, pairId);
        if (pair == null) { finish(); return; }
        root.removeAllViews();

        String dir = DevSyncPair.DIR_PUSH.equals(pair.direction) ? "Push"
                : DevSyncPair.DIR_PULL.equals(pair.direction) ? "Pull" : "Mirror";
        String sched = DevSyncActivity.scheduleLabel(pair);
        root.addView(meta(dir + " · " + sched));

        boolean running = DevSyncJobs.pending(this, pairId);
        DevSyncJobs.Snapshot snap = DevSyncJobs.snapshot;
        boolean liveSnap = running && snap != null && pairId.equals(snap.pairId);

        if (liveSnap) {
            root.addView(progressCard(snap));
        }

        root.addView(statGrid(liveSnap ? snap.synced : pair.lastSynced,
                liveSnap ? 0 : Math.max(0, 0),
                liveSnap ? snap.conflicts : pair.lastConflicts,
                liveSnap ? snap.failed : pair.lastFailed));

        root.addView(syncNowButton(running));

        if (pair.isMirror()) {
            root.addView(spacer(20));
            root.addView(meta(pair.deletesPropagate()
                    ? "Deletions propagate between phone and PC."
                    : "Deletions never propagate (default)."));
        }
    }

    private View progressCard(DevSyncJobs.Snapshot snap) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(UiKit.rounded(this, Color.BLACK, 0xFF2C2C2C, 2f, UiKit.R_MD));
        card.setPadding(UiKit.dp(this, 18), UiKit.dp(this, 18), UiKit.dp(this, 18), UiKit.dp(this, 18));
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cp.topMargin = UiKit.dp(this, 18);
        card.setLayoutParams(cp);

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        TextView title = new TextView(this);
        title.setText("Syncing…");
        title.setTextColor(Color.WHITE);
        title.setTextSize(14);
        title.setTypeface(font);
        head.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView count = new TextView(this);
        count.setText(snap.doneFiles + " / " + snap.totalFiles + " files");
        count.setTextColor(0xFF888888);
        count.setTextSize(12);
        count.setTypeface(font);
        head.addView(count);
        card.addView(head);

        LinearLayout track = new LinearLayout(this);
        track.setBackgroundColor(0xFF1C1C1C);
        LinearLayout.LayoutParams trp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, UiKit.dp(this, 6));
        trp.topMargin = UiKit.dp(this, 12);
        track.setLayoutParams(trp);
        View fill = new View(this);
        fill.setBackgroundColor(Color.WHITE);
        int pct = snap.totalFiles > 0 ? Math.max(2, (int) (100L * snap.doneFiles / snap.totalFiles)) : 5;
        track.addView(fill, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, pct));
        View rest = new View(this);
        track.addView(rest, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 100 - pct));
        card.addView(track);

        if (snap.currentPath != null) {
            TextView cur = new TextView(this);
            String arrow = "up".equals(snap.currentDirection) ? "↑ " : "down".equals(snap.currentDirection) ? "↓ " : "";
            cur.setText(arrow + snap.currentPath);
            cur.setTextColor(0xFF666666);
            cur.setTextSize(11.5f);
            cur.setTypeface(font);
            cur.setSingleLine(true);
            cur.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
            LinearLayout.LayoutParams curp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            curp.topMargin = UiKit.dp(this, 10);
            cur.setLayoutParams(curp);
            card.addView(cur);
        }
        return card;
    }

    private View statGrid(int synced, int unchanged, int conflicts, int failed) {
        android.widget.GridLayout grid = new android.widget.GridLayout(this);
        grid.setColumnCount(2);
        LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        gp.topMargin = UiKit.dp(this, 18);
        grid.setLayoutParams(gp);
        grid.addView(statTile(String.valueOf(synced), "Synced", false));
        grid.addView(statTile(String.valueOf(unchanged), "Unchanged", false));
        grid.addView(statTile(String.valueOf(conflicts), "Conflict" + (conflicts == 1 ? "" : "s"), conflicts > 0));
        grid.addView(statTile(String.valueOf(failed), "Failed", failed > 0));
        return grid;
    }

    private View statTile(String value, String label, boolean warn) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setBackground(UiKit.rounded(this, warn ? 0xFF1C1414 : Color.BLACK,
                warn ? 0xFF3A2A28 : 0xFF2C2C2C, 2f, UiKit.R_MD));
        tile.setPadding(UiKit.dp(this, 14), UiKit.dp(this, 14), UiKit.dp(this, 14), UiKit.dp(this, 14));
        android.widget.GridLayout.LayoutParams lp = new android.widget.GridLayout.LayoutParams();
        lp.width = 0;
        lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        lp.columnSpec = android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED, 1f);
        lp.setMargins(0, 0, UiKit.dp(this, 10), UiKit.dp(this, 10));
        tile.setLayoutParams(lp);
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextColor(warn ? 0xFFC88F87 : Color.WHITE);
        v.setTextSize(22);
        v.setTypeface(font, Typeface.BOLD);
        tile.addView(v);
        TextView l = new TextView(this);
        l.setText(label);
        l.setTextColor(warn ? 0xFFC9A870 : 0xFF888888);
        l.setTextSize(12);
        l.setTypeface(font);
        tile.addView(l);
        return tile;
    }

    private View syncNowButton(boolean running) {
        TextView button = new TextView(this);
        button.setText(running ? "Syncing…" : "Sync now");
        button.setTextColor(running ? 0xFF888888 : Color.BLACK);
        button.setTextSize(15);
        button.setTypeface(font, Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        button.setBackground(UiKit.rounded(this, running ? 0xFF1C1C1C : Color.WHITE, 0, 0, UiKit.R_MD));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, UiKit.dp(this, 50));
        p.topMargin = UiKit.dp(this, 20);
        button.setLayoutParams(p);
        if (!running) {
            button.setOnClickListener(v -> {
                DevSyncPair pair = DevSyncPair.find(this, pairId);
                if (pair != null) {
                    DevSyncJobs.enqueue(this, pair);
                    render();
                }
            });
        }
        return button;
    }

    private TextView meta(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextColor(0xFF888888);
        t.setTextSize(12.5f);
        t.setTypeface(font);
        return t;
    }

    private View spacer(int dp) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, UiKit.dp(this, dp)));
        return v;
    }
}

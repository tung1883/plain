package com.plainphone.app;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/**
 * The box that shows the content index: how many files, progress, what it is reading, and
 * Pause/Resume, Files, Clear. It has the same rows in every state (idle, running, paused, done) so
 * it never changes height. Shown on the Search settings and on the Indexed files screen.
 */
final class IndexStatusCard {

    private static final int DIM = 0xFF666666, GRAY = 0xFF808080, EDGE = 0xFF2C2C2C, HAIR = 0xFF1C1C1C, PAUSED_FILL = 0xFF777777;

    private IndexStatusCard() {}

    /**
     * @param onFiles true on the Indexed files screen: the Files button is dimmed there
     * @param changed called after Pause, Resume or Index now, so the screen starts redrawing the card
     * @param cleared called after the index was cleared, so the screen can reload
     */
    static View build(Activity a, boolean onFiles, Runnable changed, Runnable cleared) {
        Typeface font = Fonts.current(a);
        ContentDb.Stats stats = ContentDb.get(a).stats();
        boolean running = ContentIndexer.isRunning();
        boolean userPaused = Config.isContentUserPaused(a);
        boolean pausing = running && userPaused;
        boolean stopped = !running && userPaused;
        int maxMb = Config.getContentMaxMb(a);

        LinearLayout card = new LinearLayout(a);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(UiKit.rounded(a, Color.BLACK, EDGE, 1f, UiKit.R_MD));
        card.setPadding(1, 1, 1, 1);     // keep the children off the border

        LinearLayout body = new LinearLayout(a);
        body.setOrientation(LinearLayout.VERTICAL);
        int pad = UiKit.dp(a, 16);
        body.setPadding(pad, pad, pad, pad);

        LinearLayout top = new LinearLayout(a);
        top.setGravity(Gravity.BOTTOM);
        top.addView(text(a, font, String.valueOf(stats.count("ok")), 30, Color.WHITE));
        TextView small = text(a, font, "files indexed", 14, GRAY);
        small.setPadding(UiKit.dp(a, 6), 0, 0, UiKit.dp(a, 4));
        top.addView(small);
        body.addView(top);

        ThinBar bar = new ThinBar(a);
        float fraction;
        if (running && ContentIndexer.progressTotal() > 0) {
            fraction = ContentIndexer.progressDone() / (float) ContentIndexer.progressTotal();
        } else if (stopped && Config.getContentPausedTotal(a) > 0) {
            fraction = Config.getContentPausedDone(a) / (float) Config.getContentPausedTotal(a);
        } else {
            fraction = SearchSettingsText.usedFraction(stats.bytes, maxMb);
        }
        bar.setFraction(fraction);
        bar.setFill(userPaused ? PAUSED_FILL : Color.WHITE);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, UiKit.dp(a, 6));
        bp.topMargin = UiKit.dp(a, 12);
        body.addView(bar, bp);

        String left, right;
        if (pausing) {
            left = "Pausing…";
            right = "";
        } else if (running) {
            left = SearchSettingsText.progressLine(ContentIndexer.progressDone(), ContentIndexer.progressTotal());
            right = SearchSettingsText.eta(ContentIndexer.progressDone(), ContentIndexer.progressTotal(), ContentIndexer.elapsedMs());
        } else if (stopped) {
            left = SearchSettingsText.pausedLine(Config.getContentPausedDone(a), Config.getContentPausedTotal(a));
            right = "Stopped by you";
        } else {
            if (Config.isContentCapped(a)) {
                left = "Full at " + SearchSettingsText.limitLabel(maxMb) + ", more files are waiting";
            } else if (!ContentIndexer.policyAllows(a, Config.isContentAllowLowBattery(a))) {
                left = "Waiting: battery is low";
            } else {
                left = SearchSettingsText.sizeLine(stats.bytes, maxMb);
            }
            right = SearchSettingsText.lastRun(Config.getContentLastRun(a), System.currentTimeMillis());
        }
        LinearLayout meta = new LinearLayout(a);
        meta.setPadding(0, UiKit.dp(a, 6), 0, 0);
        TextView leftView = text(a, font, left, 12, GRAY);
        leftView.setSingleLine(true);
        leftView.setEllipsize(TextUtils.TruncateAt.END);
        meta.addView(leftView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView rightView = text(a, font, right, 12, GRAY);
        rightView.setSingleLine(true);
        rightView.setPadding(UiKit.dp(a, 8), 0, 0, 0);
        meta.addView(rightView);
        body.addView(meta);

        // one line of fixed height: what is being read, or the reasons some files are skipped
        FrameLayout slot = new FrameLayout(a);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, UiKit.dp(a, 26));
        sp.topMargin = UiKit.dp(a, 12);
        if (running || stopped) {
            String what = pausing ? "finishing" : running ? "reading" : "next";
            String name = stopped ? Config.getContentPausedNext(a) : ContentIndexer.currentFile();
            LinearLayout line = new LinearLayout(a);
            line.setGravity(Gravity.CENTER_VERTICAL);
            line.addView(text(a, font, what, 12, DIM));
            TextView file = text(a, font, name, 12, 0xFFB0B0B0);
            file.setSingleLine(true);
            file.setEllipsize(TextUtils.TruncateAt.MIDDLE);
            file.setPadding(UiKit.dp(a, 6), 0, 0, 0);
            line.addView(file, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            slot.addView(line, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        } else {
            List<String> skipped = SearchSettingsText.skippedPills(stats.byStatus);
            HorizontalScrollView scroll = new HorizontalScrollView(a);
            scroll.setHorizontalScrollBarEnabled(false);
            LinearLayout pills = new LinearLayout(a);
            pills.setGravity(Gravity.CENTER_VERTICAL);
            for (String s : skipped) {
                TextView p = text(a, font, s, 12, 0xFFB0B0B0);
                p.setPadding(UiKit.dp(a, 10), UiKit.dp(a, 3), UiKit.dp(a, 10), UiKit.dp(a, 3));
                p.setBackground(UiKit.rounded(a, Color.TRANSPARENT, EDGE, 1f, 12));
                LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                pp.rightMargin = UiKit.dp(a, 6);
                pills.addView(p, pp);
            }
            scroll.addView(pills);
            slot.addView(scroll, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
        body.addView(slot, sp);
        card.addView(body);

        card.addView(hairline(a), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
        LinearLayout actions = new LinearLayout(a);
        if (pausing) {
            actions.addView(action(a, font, "Pausing…", DIM, null));
        } else if (running) {
            actions.addView(action(a, font, "Pause", Color.WHITE, () -> {
                SearchJobs.pauseContentIndex(a);
                changed.run();
            }));
        } else {
            actions.addView(action(a, font, stopped ? "Resume" : "Index now", Color.WHITE, () -> start(a, changed)));
        }
        actions.addView(hairline(a), new LinearLayout.LayoutParams(1, ViewGroup.LayoutParams.MATCH_PARENT));
        actions.addView(action(a, font, "Files", onFiles ? DIM : Color.WHITE,
                onFiles ? null : () -> a.startActivity(new android.content.Intent(a, IndexedFilesActivity.class))));
        actions.addView(hairline(a), new LinearLayout.LayoutParams(1, ViewGroup.LayoutParams.MATCH_PARENT));
        actions.addView(action(a, font, "Clear", Color.WHITE, () -> confirmClear(a, cleared)));
        card.addView(actions);

        LinearLayout wrap = new LinearLayout(a);
        wrap.setPadding(48, UiKit.dp(a, 10), 48, UiKit.dp(a, 10));
        wrap.addView(card, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return wrap;
    }

    private static void start(Activity a, Runnable changed) {
        if (ContentIndexer.isRunning() && !Config.isContentUserPaused(a)) return;
        if (!FileIndex.canWalk(a)) {
            DeviceSearch.requestFullFileAccess(a);
            return;
        }
        SearchJobs.resumeContentIndex(a);     // also what "Index now" does: it clears a pause
        Toast.makeText(a, "Reading documents in the background", Toast.LENGTH_SHORT).show();
        changed.run();
    }

    private static void confirmClear(Activity a, Runnable cleared) {
        VaultUi.confirm(a, "Clear the index?", "Search in files will be empty until it reads again.",
                "Clear", () -> new Thread(() -> {
                    ContentIndexer.stopAndWait();   // a run in progress would keep writing to the old file
                    try {
                        ContentDb.get(a).clearAll();
                        Config.setContentUserPaused(a, false);     // nothing is left to resume
                    } finally {
                        ContentIndexer.allowRuns();
                    }
                    a.runOnUiThread(() -> {
                        Toast.makeText(a, "Index cleared", Toast.LENGTH_SHORT).show();
                        cleared.run();
                    });
                }).start(), "Cancel", null);
    }

    private static TextView action(Activity a, Typeface font, String label, int color, Runnable run) {
        TextView t = text(a, font, label, 16, color);
        t.setGravity(Gravity.CENTER);
        t.setSingleLine(true);
        t.setPadding(0, UiKit.dp(a, 14), 0, UiKit.dp(a, 14));
        // no resting fill and no clipping: a fill or a clip would cut the box border at its corners
        android.graphics.drawable.StateListDrawable bg = new android.graphics.drawable.StateListDrawable();
        if (run != null) bg.addState(new int[]{android.R.attr.state_pressed}, UiKit.pressedFill());
        t.setBackground(bg);
        if (run != null) t.setOnClickListener(v -> run.run());
        t.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return t;
    }

    private static View hairline(Activity a) {
        View v = new View(a);
        v.setBackgroundColor(HAIR);
        return v;
    }

    private static TextView text(Activity a, Typeface font, String s, float sp, int color) {
        TextView t = new TextView(a);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setTypeface(font);
        return t;
    }

    /** Redraws a card in {@code holder} every 500 ms while a run goes on, then once more when it ends. */
    static final class Ticker implements Runnable {
        private final Handler ui = new Handler(Looper.getMainLooper());
        private final FrameLayout holder;
        private final java.util.function.Supplier<View> make;
        private boolean ticking;
        private int graceTicks;

        Ticker(FrameLayout holder, java.util.function.Supplier<View> make) {
            this.holder = holder;
            this.make = make;
        }

        /** Draw now and keep drawing while indexing runs. */
        void start() {
            graceTicks = 2;
            if (!ticking) {
                ticking = true;
                ui.post(this);
            }
        }

        void stop() {
            ui.removeCallbacks(this);
            ticking = false;
        }

        @Override
        public void run() {
            holder.removeAllViews();
            holder.addView(make.get());
            if (ContentIndexer.isRunning()) graceTicks = 2;
            if (graceTicks-- > 0) {
                ui.postDelayed(this, 500);
            } else {
                ticking = false;
            }
        }
    }
}

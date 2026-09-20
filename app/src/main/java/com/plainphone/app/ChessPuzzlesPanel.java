package com.plainphone.app;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** The Puzzles tab: a "home" state (Play / Generate puzzles / Puzzle list, plus the current
 *  generation job's live progress) and a "solving" state (board + moves grid + prev/next
 *  through whatever queue Play picked, Auto-next, Analyze, Retry, a "Solved" badge) — the two
 *  swapped by visibility inside one {@link FrameLayout}, same pattern
 *  {@link MainActivity#renderRows} already uses for the home screen's own sections. Owns its
 *  own {@link ChessBoardView}, entirely separate from the Board tab's — solving a puzzle never
 *  disturbs whatever game is on the Board tab, and vice versa. */
final class ChessPuzzlesPanel {

    interface Listener {
        void onOpenPuzzleList();
        /** Same dialogs the Board tab's resize/settings icons open — shared Config-backed
         *  board scale/theme/engine settings, so the two tabs' boards stay consistent. */
        void onResizeRequested();
        void onOpenSettings();
    }

    private final Activity host;
    private final Listener listener;
    private final FrameLayout root;
    private final View homeView;
    private final View solveView;

    // --- home state -----------------------------------------------------
    private View jobCard;
    private TextView jobTitle, jobStat, jobStop;
    private TextView statLine;

    // --- solving state ----------------------------------------------------
    private ChessBoardView board;
    private FrameLayout boardWrap;
    private MovesGrid movesGrid;
    private TextView solveTitle, solveMeta, solveCounter, statusLine, autoNextPill;
    private TextView[] engineLines;
    private List<ChessPuzzles.Puzzle> queue = new ArrayList<>();
    private int queueIndex;
    private boolean engineLinesShown;

    ChessPuzzlesPanel(Activity host, Listener listener) {
        this.host = host;
        this.listener = listener;
        root = new FrameLayout(host);
        homeView = buildHome();
        solveView = buildSolve();
        root.addView(homeView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(solveView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        solveView.setVisibility(View.GONE);
        ChessPuzzleJobs.addListener(this::refreshJobCardIfShown);
    }

    View view() { return root; }

    /** Call whenever the Puzzles tab becomes the visible one — the job card and puzzle count
     *  can both be stale from whatever happened while another tab was showing. */
    void onShown() {
        if (homeView.getVisibility() == View.VISIBLE) refreshHome();
        else applyBoardScale();
    }

    private void refreshJobCardIfShown() {
        if (homeView.getVisibility() == View.VISIBLE) host.runOnUiThread(this::refreshHome);
    }

    // --- home -------------------------------------------------------------

    private View buildHome() {
        LinearLayout col = new LinearLayout(host);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setBackgroundColor(Color.BLACK);

        jobCard = new LinearLayout(host);
        ((LinearLayout) jobCard).setOrientation(LinearLayout.VERTICAL);
        jobCard.setBackground(UiKit.rounded(host, Color.BLACK, 0xFF262626, 2f, UiKit.R_MD));
        jobCard.setPadding(UiKit.dp(host, 16), UiKit.dp(host, 14), UiKit.dp(host, 16), UiKit.dp(host, 14));
        LinearLayout.LayoutParams jobLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        jobLp.setMargins(UiKit.dp(host, 20), UiKit.dp(host, 20), UiKit.dp(host, 20), 0);
        jobTitle = new TextView(host);
        jobTitle.setText("Generating puzzles");
        jobTitle.setTextColor(Color.WHITE);
        jobTitle.setTypeface(Fonts.current(host));
        jobTitle.setTextSize(13);
        ((LinearLayout) jobCard).addView(jobTitle);
        jobStat = new TextView(host);
        jobStat.setTextColor(0xFF8A8A8A);
        jobStat.setTypeface(Fonts.current(host));
        jobStat.setTextSize(12);
        jobStat.setPadding(0, UiKit.dp(host, 6), 0, 0);
        ((LinearLayout) jobCard).addView(jobStat);
        jobStop = new TextView(host);
        jobStop.setText("Stop");
        jobStop.setTextColor(Color.WHITE);
        jobStop.setTypeface(Fonts.current(host));
        jobStop.setTextSize(12);
        jobStop.setPadding(0, UiKit.dp(host, 10), 0, 0);
        jobStop.setOnClickListener(v -> { ChessPuzzleJobs.stop(host); refreshHome(); });
        ((LinearLayout) jobCard).addView(jobStop);
        col.addView(jobCard, jobLp);

        col.addView(actionRow("Play", v -> openPlayFrom()));
        col.addView(actionRow("Generate puzzles", v -> openGenerateFrom()));
        col.addView(actionRow("Puzzle list", v -> listener.onOpenPuzzleList()));

        statLine = new TextView(host);
        statLine.setTextColor(0xFF6E6E6E);
        statLine.setTypeface(Fonts.current(host));
        statLine.setTextSize(12);
        statLine.setGravity(Gravity.CENTER);
        statLine.setPadding(UiKit.dp(host, 20), UiKit.dp(host, 20), UiKit.dp(host, 20), 0);
        col.addView(statLine);
        return col;
    }

    private View actionRow(String label, View.OnClickListener onClick) {
        TextView row = new TextView(host);
        row.setText(label);
        row.setTextColor(Color.WHITE);
        row.setTypeface(Fonts.current(host), android.graphics.Typeface.BOLD);
        row.setTextSize(16);
        row.setGravity(Gravity.CENTER);
        row.setBackground(UiKit.pressable(host, Color.BLACK, Color.DKGRAY, 0xFF2C2C2C, 2f, UiKit.R_SM));
        row.setOnClickListener(onClick);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, UiKit.dp(host, 56));
        lp.setMargins(UiKit.dp(host, 20), UiKit.dp(host, 14), UiKit.dp(host, 20), 0);
        row.setLayoutParams(lp);
        return row;
    }

    private void refreshHome() {
        // ChessPuzzleJobs.snapshot (not JobQueue/isRunning) drives visibility — a finished
        // run keeps its snapshot around a few seconds in "done" state (see JobService) so
        // this card gets to show a completion line instead of just vanishing the instant
        // the last game finishes scanning.
        ChessPuzzleJobs.Snapshot snap = ChessPuzzleJobs.snapshot;
        jobCard.setVisibility(snap != null ? View.VISIBLE : View.GONE);
        if (snap != null) {
            jobTitle.setText(snap.done ? "Puzzle generation complete" : "Generating puzzles");
            jobStat.setText(snap.done ? ChessPuzzleJobs.doneLabel(host) : ChessPuzzleJobs.activeLabel(host));
            jobStop.setVisibility(snap.done ? View.GONE : View.VISIBLE);
        }
        new Thread(() -> {
            int total = ChessPuzzles.count(host);
            host.runOnUiThread(() -> statLine.setText(total + (total == 1 ? " puzzle" : " puzzles")));
        }).start();
    }

    // --- Play puzzles from… ------------------------------------------------

    private void openPlayFrom() {
        new Thread(() -> {
            List<ChessLibrary.SourceSummary> sources = ChessLibrary.listSources(host);
            int total = ChessPuzzles.count(host);
            host.runOnUiThread(() -> showPlayFromDialog(sources, total));
        }).start();
    }

    private void showPlayFromDialog(List<ChessLibrary.SourceSummary> sources, int totalPuzzles) {
        java.util.Set<String> selected = new java.util.LinkedHashSet<>();
        boolean[] randomize = {true};

        LinearLayout box = new LinearLayout(host);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackground(UiKit.dialogBackground(host));
        UiKit.clipRounded(host, box, UiKit.R_MD);
        box.setPadding(2, 32, 2, UiKit.dp(host, UiKit.R_MD));
        box.addView(UiKit.dialogTitle(host, "Play puzzles from"));

        LinearLayout rows = new LinearLayout(host);
        rows.setOrientation(LinearLayout.VERTICAL);
        ScrollView scroller = new ScrollView(host);
        scroller.addView(rows, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        scroller.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, UiKit.dp(host, 280)));
        box.addView(scroller);

        Runnable[] render = new Runnable[1];
        render[0] = () -> {
            rows.removeAllViews();
            boolean allSelected = selected.isEmpty();
            rows.addView(pickRow("All games (" + totalPuzzles + ")", allSelected, v -> {
                selected.clear();
                render[0].run();
            }));
            for (ChessLibrary.SourceSummary s : sources) {
                int n = ChessPuzzles.countBySource(host, s.label);
                if (n == 0) continue;
                boolean sel = selected.contains(s.label);
                rows.addView(pickRow(s.label + " (" + n + ")", sel, v -> {
                    if (!selected.remove(s.label)) selected.add(s.label);
                    render[0].run();
                }));
            }
        };
        render[0].run();

        box.addView(UiKit.dialogTitle(host, " "));
        TextView randomizeRow = new TextView(host);
        randomizeRow.setTextColor(Color.WHITE);
        randomizeRow.setTypeface(Fonts.current(host));
        randomizeRow.setTextSize(15);
        randomizeRow.setPadding(48, 28, 48, 28);
        Runnable[] renderRandomize = new Runnable[1];
        renderRandomize[0] = () -> randomizeRow.setText("Randomize puzzles: " + (randomize[0] ? "On" : "Off"));
        renderRandomize[0].run();
        randomizeRow.setOnClickListener(v -> { randomize[0] = !randomize[0]; renderRandomize[0].run(); });
        box.addView(randomizeRow);

        android.widget.FrameLayout scrim = UiKit.wrapScrim(host, box, 0.85f);
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(
                host, R.style.Theme_PlainPhone_RoundedDialog).setView(scrim).create();
        TextView play = new TextView(host);
        play.setText("Play");
        play.setTextColor(Color.WHITE);
        play.setTextSize(20);
        play.setTypeface(Fonts.current(host));
        play.setPadding(48, 32, 48, 32);
        StateListDrawable playBg = new StateListDrawable();
        playBg.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Color.DKGRAY));
        playBg.addState(new int[]{}, new ColorDrawable(Color.BLACK));
        play.setBackground(playBg);
        play.setOnClickListener(v -> {
            dialog.dismiss();
            startQueueFor(selected.isEmpty() ? null : selected, randomize[0]);
        });
        box.addView(play);
        UiKit.finishCentered(dialog, scrim);
    }

    private View pickRow(String label, boolean selected, View.OnClickListener onClick) {
        TextView row = new TextView(host);
        row.setText((selected ? "[x] " : "[ ] ") + label);
        row.setTextColor(selected ? Color.WHITE : 0xFFB7B7B7);
        row.setTypeface(Fonts.current(host), selected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        row.setTextSize(15);
        row.setPadding(48, 24, 48, 24);
        row.setOnClickListener(onClick);
        return row;
    }

    private void startQueueFor(java.util.Set<String> sources, boolean randomize) {
        new Thread(() -> {
            List<ChessPuzzles.Puzzle> puzzles = ChessPuzzles.loadBySources(host, sources);
            if (randomize) Collections.shuffle(puzzles);
            host.runOnUiThread(() -> {
                if (puzzles.isEmpty()) { host.runOnUiThread(() -> android.widget.Toast.makeText(host, "No puzzles for that selection", android.widget.Toast.LENGTH_SHORT).show()); return; }
                queue = puzzles;
                queueIndex = 0;
                showSolve();
                loadCurrentPuzzle();
            });
        }).start();
    }

    // --- Generate puzzles from… ---------------------------------------------

    private void openGenerateFrom() {
        new Thread(() -> {
            List<ChessLibrary.SourceSummary> sources = ChessLibrary.listSources(host);
            host.runOnUiThread(() -> showGenerateFromDialog(sources));
        }).start();
    }

    private void showGenerateFromDialog(List<ChessLibrary.SourceSummary> sources) {
        LinearLayout box = new LinearLayout(host);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackground(UiKit.dialogBackground(host));
        UiKit.clipRounded(host, box, UiKit.R_MD);
        box.setPadding(2, 32, 2, UiKit.dp(host, UiKit.R_MD));
        box.addView(UiKit.dialogTitle(host, "Generate puzzles from"));

        android.widget.FrameLayout scrim = UiKit.wrapScrim(host, box, 0.85f);
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(
                host, R.style.Theme_PlainPhone_RoundedDialog).setView(scrim).create();

        box.addView(genRow("All games", v -> {
            dialog.dismiss();
            ChessPuzzleJobs.start(host);
            refreshHome();
        }));
        for (ChessLibrary.SourceSummary s : sources) {
            box.addView(genRow(s.label + " (" + s.gameCount + ")", v -> {
                dialog.dismiss();
                ChessPuzzleJobs.start(host, s.label);
                refreshHome();
            }));
        }
        box.addView(genRow("Cancel", v -> dialog.dismiss()));

        UiKit.finishCentered(dialog, scrim);
    }

    private View genRow(String label, View.OnClickListener onClick) {
        TextView row = new TextView(host);
        row.setText(label);
        row.setTextColor(Color.WHITE);
        row.setTextSize(20);
        row.setTypeface(Fonts.current(host));
        row.setPadding(48, 32, 48, 32);
        StateListDrawable bg = new StateListDrawable();
        bg.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Color.DKGRAY));
        bg.addState(new int[]{}, new ColorDrawable(Color.BLACK));
        row.setBackground(bg);
        row.setOnClickListener(onClick);
        return row;
    }

    // --- solving ------------------------------------------------------------

    private View buildSolve() {
        ScrollView panel = new ScrollView(host);
        panel.setFillViewport(true);
        panel.setBackgroundColor(Color.BLACK);
        LinearLayout content = new LinearLayout(host);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        content.setPadding(0, UiKit.dp(host, 8), 0, UiKit.dp(host, 16));
        panel.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Two explicit rows (player names + Auto-next, then date + puzzle counter) rather
        // than two independently-margined columns — sharing one row per line is what
        // actually guarantees the left and right sides line up; matching margins by hand
        // on separate columns drifts as soon as one side's line height differs.
        LinearLayout header = new LinearLayout(host);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(48, UiKit.dp(host, 12), 48, 0);

        LinearLayout row1 = new LinearLayout(host);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setGravity(Gravity.CENTER_VERTICAL);
        // Just "White vs Black" — whose move it is lives in the left-aligned status row
        // under the board instead, same split as the Board tab (meta block vs. status row).
        solveTitle = new TextView(host);
        solveTitle.setTextColor(Color.WHITE);
        solveTitle.setTypeface(Fonts.current(host), android.graphics.Typeface.BOLD);
        solveTitle.setTextSize(15);
        row1.addView(solveTitle, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        autoNextPill = new TextView(host);
        autoNextPill.setTextColor(Color.WHITE);
        autoNextPill.setTypeface(Fonts.current(host));
        autoNextPill.setTextSize(11);
        // START, not CENTER — a fixed-width box still re-centers shorter text ("On") at a
        // different x than longer text ("Off"), so the "Auto-next:" prefix itself visibly
        // slides on every toggle even though the box around it doesn't move.
        autoNextPill.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        autoNextPill.setBackground(UiKit.rounded(host, Color.BLACK, 0xFF262626, 2f, UiKit.R_SM));
        autoNextPill.setPadding(UiKit.dp(host, 10), UiKit.dp(host, 5), UiKit.dp(host, 10), UiKit.dp(host, 5));
        autoNextPill.setOnClickListener(v -> {
            Config.setChessAutoNextPuzzle(host, !Config.getChessAutoNextPuzzle(host));
            refreshAutoNextPill();
        });
        // A fixed LayoutParams width (not WRAP_CONTENT + setMinWidth) — sized to fit the
        // wider of "On"/"Off" up front, so measuring "On" can't ever come out narrower than
        // measuring "Off" did and nudge solveTitle's own width side to side on every tap.
        row1.addView(autoNextPill, new LinearLayout.LayoutParams(
                autoNextPillWidth(), ViewGroup.LayoutParams.WRAP_CONTENT));
        header.addView(row1);

        LinearLayout row2 = new LinearLayout(host);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams row2Lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        row2Lp.topMargin = UiKit.dp(host, 4);
        solveMeta = new TextView(host);
        solveMeta.setTextColor(0xFF8A8A8A);
        solveMeta.setTypeface(Fonts.current(host));
        solveMeta.setTextSize(12);
        row2.addView(solveMeta, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        solveCounter = new TextView(host);
        solveCounter.setTextColor(0xFF6E6E6E);
        solveCounter.setTypeface(Fonts.current(host));
        solveCounter.setTextSize(11);
        row2.addView(solveCounter, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        header.addView(row2, row2Lp);
        content.addView(header);

        board = new ChessBoardView(host, this::onBoardChanged, this::onAnalysisChanged);
        board.setPieceTheme("alpha");
        board.setBoardTheme("grey");
        UiKit.clipRounded(host, board, UiKit.R_SM);
        boardWrap = new FrameLayout(host);
        boardWrap.addView(board, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL));
        LinearLayout.LayoutParams boardWrapLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        boardWrapLp.topMargin = UiKit.dp(host, 18);
        content.addView(boardWrap, boardWrapLp);

        // Status row: left-aligned status text + flip/resize/settings icons on the right —
        // the exact layout the Board tab's own status row uses, not centered puzzle-specific
        // phrasing ("White to find the best move").
        LinearLayout statusRow = new LinearLayout(host);
        statusRow.setGravity(Gravity.CENTER_VERTICAL);
        statusRow.setPadding(48, UiKit.dp(host, 16), 48, UiKit.dp(host, 12));
        statusLine = new TextView(host);
        statusLine.setTextColor(Color.WHITE);
        statusLine.setTypeface(Fonts.current(host));
        statusLine.setTextSize(17);
        statusRow.addView(statusLine, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        int iconBoxDp = 18;
        android.widget.ImageView flipIcon = new android.widget.ImageView(host);
        flipIcon.setImageDrawable(host.getResources().getDrawable(R.drawable.ic_chess_flip, host.getTheme()));
        flipIcon.setScaleType(android.widget.ImageView.ScaleType.CENTER_INSIDE);
        flipIcon.setContentDescription("Flip board");
        flipIcon.setOnClickListener(v -> board.toggleFlip());
        LinearLayout.LayoutParams flipLp = new LinearLayout.LayoutParams(UiKit.dp(host, iconBoxDp), UiKit.dp(host, iconBoxDp));
        flipLp.rightMargin = UiKit.dp(host, 14);
        statusRow.addView(flipIcon, flipLp);
        android.widget.ImageView resizeIcon = new android.widget.ImageView(host);
        resizeIcon.setImageDrawable(host.getResources().getDrawable(R.drawable.ic_chess_resize, host.getTheme()));
        resizeIcon.setScaleType(android.widget.ImageView.ScaleType.CENTER_INSIDE);
        resizeIcon.setContentDescription("Resize board");
        resizeIcon.setOnClickListener(v -> listener.onResizeRequested());
        LinearLayout.LayoutParams resizeLp = new LinearLayout.LayoutParams(UiKit.dp(host, iconBoxDp), UiKit.dp(host, iconBoxDp));
        resizeLp.rightMargin = UiKit.dp(host, 14);
        statusRow.addView(resizeIcon, resizeLp);
        android.widget.ImageView settingsIcon = new android.widget.ImageView(host);
        settingsIcon.setImageDrawable(host.getResources().getDrawable(R.drawable.ic_chess_settings, host.getTheme()));
        settingsIcon.setScaleType(android.widget.ImageView.ScaleType.CENTER_INSIDE);
        settingsIcon.setContentDescription("Chess settings");
        settingsIcon.setOnClickListener(v -> listener.onOpenSettings());
        statusRow.addView(settingsIcon, new LinearLayout.LayoutParams(UiKit.dp(host, iconBoxDp), UiKit.dp(host, iconBoxDp)));
        content.addView(statusRow);

        // Same green multi-line format as the Board tab's engine summary — hidden until
        // "Analyze" is tapped, so a puzzle's answer isn't spoiled just by having Stockfish
        // running in the background the whole time.
        engineLines = new TextView[3];
        for (int i = 0; i < engineLines.length; i++) {
            TextView line = new TextView(host);
            line.setTextColor(0xFF8FBF8F);
            line.setTypeface(Fonts.current(host));
            line.setTextSize(13);
            line.setPadding(48, 0, 48, 0);
            line.setSingleLine(true);
            line.setEllipsize(android.text.TextUtils.TruncateAt.END);
            line.setVisibility(View.GONE);
            engineLines[i] = line;
            content.addView(line);
        }

        LinearLayout navRow = new LinearLayout(host);
        navRow.setOrientation(LinearLayout.HORIZONTAL);
        navRow.setPadding(UiKit.dp(host, 20), UiKit.dp(host, 16), UiKit.dp(host, 20), 0);
        TextView prev = navButton("‹ Prev", false);
        prev.setOnClickListener(v -> { if (queueIndex > 0) { queueIndex--; loadCurrentPuzzle(); } });
        TextView next = navButton("Next ›", true);
        next.setOnClickListener(v -> { if (queueIndex < queue.size() - 1) { queueIndex++; loadCurrentPuzzle(); } });
        LinearLayout.LayoutParams prevLp = new LinearLayout.LayoutParams(0, UiKit.dp(host, 48), 1f);
        prevLp.rightMargin = UiKit.dp(host, 10);
        navRow.addView(prev, prevLp);
        navRow.addView(next, new LinearLayout.LayoutParams(0, UiKit.dp(host, 48), 1f));
        content.addView(navRow);

        LinearLayout toolGroup = new LinearLayout(host);
        toolGroup.setOrientation(LinearLayout.HORIZONTAL);
        toolGroup.setBackground(UiKit.rounded(host, Color.BLACK, 0xFF262626, 2f, UiKit.R_SM));
        UiKit.clipRounded(host, toolGroup, UiKit.R_SM);
        LinearLayout.LayoutParams toolLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        toolLp.setMargins(UiKit.dp(host, 20), UiKit.dp(host, 14), UiKit.dp(host, 20), 0);
        toolGroup.addView(toolButton("Analyze", v -> {
            engineLinesShown = !engineLinesShown;
            if (engineLinesShown) board.requestAnalysis();
            else for (TextView line : engineLines) line.setVisibility(View.GONE);
        }), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        // 2 raw px — same width as toolGroup's own 2f-stroke border above.
        View divider = new View(host);
        divider.setBackgroundColor(0xFF1C1C1C);
        toolGroup.addView(divider, new LinearLayout.LayoutParams(2, ViewGroup.LayoutParams.MATCH_PARENT));
        toolGroup.addView(toolButton("Retry", v -> loadCurrentPuzzle()), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        content.addView(toolGroup, toolLp);

        content.addView(moveTransportRow());

        movesGrid = new MovesGrid(host, board, this::onBoardChanged);
        movesGrid.setPadding(48, UiKit.dp(host, 16), 48, UiKit.dp(host, 12));
        content.addView(movesGrid, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView back = new TextView(host);
        back.setText("← Puzzles home");
        back.setTextColor(0xFF8A8A8A);
        back.setTypeface(Fonts.current(host));
        back.setTextSize(13);
        back.setGravity(Gravity.CENTER);
        back.setPadding(48, UiKit.dp(host, 18), 48, 0);
        back.setOnClickListener(v -> showHome());
        content.addView(back);

        return panel;
    }

    private TextView navButton(String label, boolean primary) {
        TextView t = new TextView(host);
        t.setText(label);
        t.setGravity(Gravity.CENTER);
        t.setTextColor(primary ? Color.BLACK : Color.WHITE);
        t.setTypeface(Fonts.current(host), primary ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        t.setTextSize(14);
        t.setBackground(primary
                ? UiKit.rounded(host, Color.WHITE, 0, 0f, UiKit.R_SM)
                : UiKit.pressable(host, Color.BLACK, Color.DKGRAY, 0xFF333333, 2f, UiKit.R_SM));
        return t;
    }

    /** The wider of "Auto-next: On"/"Auto-next: Off", plus the pill's own horizontal padding
     *  — a fixed {@code minWidth} for {@link #autoNextPill} so the text swap on tap never
     *  changes the pill's measured width. */
    private int autoNextPillWidth() {
        android.graphics.Paint p = new android.graphics.Paint();
        p.setTypeface(Fonts.current(host));
        p.setTextSize(android.util.TypedValue.applyDimension(
                android.util.TypedValue.COMPLEX_UNIT_SP, 11f, host.getResources().getDisplayMetrics()));
        float widest = Math.max(p.measureText("Auto-next: On"), p.measureText("Auto-next: Off"));
        return Math.round(widest) + UiKit.dp(host, 20);
    }

    /** Same "|‹ ‹ › ›|" move transport as the Board tab's — first/prev/next/last through
     *  whatever's actually on the board (the puzzle's played-out line), separate from the
     *  "‹ Prev / Next ›" row above, which steps between puzzles in the queue instead. */
    private View moveTransportRow() {
        LinearLayout transport = new LinearLayout(host);
        transport.setGravity(Gravity.CENTER);
        transport.setPadding(UiKit.dp(host, 20), UiKit.dp(host, 12), UiKit.dp(host, 20), 0);
        String[] labels = {"|‹", "‹", "›", "›|"};
        for (int i = 0; i < labels.length; i++) {
            final int action = i;
            TextView button = new TextView(host);
            button.setText(labels[i]);
            button.setTextSize(18);
            button.setTextColor(Color.WHITE);
            button.setTypeface(Fonts.current(host));
            button.setIncludeFontPadding(false);
            button.setGravity(Gravity.CENTER);
            button.setOnClickListener(v -> {
                if (action == 0) board.first();
                else if (action == 1) board.previous();
                else if (action == 2) board.next();
                else board.last();
            });
            transport.addView(button, new LinearLayout.LayoutParams(0, UiKit.dp(host, 34), 1f));
        }
        return transport;
    }

    private View toolButton(String label, View.OnClickListener onClick) {
        TextView t = new TextView(host);
        t.setText(label);
        t.setGravity(Gravity.CENTER);
        t.setTextColor(0xFFDADADA);
        t.setTypeface(Fonts.current(host));
        t.setTextSize(13);
        t.setPadding(0, UiKit.dp(host, 14), 0, UiKit.dp(host, 14));
        t.setOnClickListener(onClick);
        return t;
    }

    private void showSolve() {
        homeView.setVisibility(View.GONE);
        solveView.setVisibility(View.VISIBLE);
    }

    private void showHome() {
        solveView.setVisibility(View.GONE);
        homeView.setVisibility(View.VISIBLE);
        refreshHome();
    }

    private void loadCurrentPuzzle() {
        if (queue.isEmpty()) { showHome(); return; }
        ChessPuzzles.Puzzle p = queue.get(queueIndex);
        engineLinesShown = false;
        for (TextView line : engineLines) line.setVisibility(View.GONE);
        // Just "White vs Black" — no "to move" here, that's the status row's job now.
        solveTitle.setText(ChessBoardView.shortName(p.white) + " vs " + ChessBoardView.shortName(p.black));
        // Date only — no event/tournament name here, unlike the Board tab's own meta line.
        solveMeta.setText(p.date);
        solveCounter.setText("Puzzle " + (queueIndex + 1) + " of " + queue.size());
        refreshAutoNextPill();
        applyBoardScale();
        board.loadPuzzle(p.startFen, p.solutionUci, this::onWrongMove, this::onPuzzleSolved);
    }

    private void refreshAutoNextPill() {
        autoNextPill.setText("Auto-next: " + (Config.getChessAutoNextPuzzle(host) ? "On" : "Off"));
    }

    /** Same Config-backed scale the Board tab's resize dialog writes to — one board-size
     *  preference shared by both tabs. The 96px (48 each side) inset matches
     *  {@code MainActivity.chessAutoFitFor}'s own {@code maxW} exactly — that's the "margin"
     *  the Board tab's board has that this one was missing, not a coincidence: every other
     *  row in both tabs (status, engine lines, transport) already pads 48px each side, and
     *  the board is meant to line up with them, not run edge-to-edge. The Board tab's own
     *  header-drag interpolation doesn't apply here (this tab has no collapsible header),
     *  so this is a flat percentage of that same inset ceiling, not a re-derivation of the
     *  vertical auto-fit math. Re-applied on every puzzle load and tab-show so a resize made
     *  on the Board tab is picked up next time this one's visible. */
    private void applyBoardScale() {
        int maxW = host.getResources().getDisplayMetrics().widthPixels - UiKit.dp(host, 96);
        float scale = Math.max(0.55f, Math.min(1f, Config.getChessBoardScale(host)));
        int widthPx = Math.max(UiKit.dp(host, 160), Math.round(maxW * scale));
        ViewGroup.LayoutParams lp = board.getLayoutParams();
        if (lp == null) lp = new FrameLayout.LayoutParams(widthPx, ViewGroup.LayoutParams.WRAP_CONTENT);
        else lp.width = widthPx;
        board.setLayoutParams(lp);
    }

    private void onBoardChanged() {
        // Plain "White/Black to move" (or "Solved!" once it is) — not
        // puzzleStatusText()'s "White to find the best move" wording; the Board tab never
        // says anything but "White/Black to move" and this screen now matches it. Any real
        // move (this only fires on one) overwrites whatever onWrongMove left showing.
        if (board.puzzleStatusText() != null && board.puzzleStatusText().startsWith("Puzzle solved")) {
            statusLine.setText("Solved!");
        } else {
            statusLine.setText(board.whiteToMove() ? "White to move" : "Black to move");
        }
        if (engineLinesShown) {
            List<String> lines = board.engineSummary();
            for (int i = 0; i < engineLines.length; i++) {
                boolean has = i < lines.size() && !lines.get(i).isEmpty();
                engineLines[i].setVisibility(has ? View.VISIBLE : View.GONE);
                if (has) engineLines[i].setText(lines.get(i));
            }
        }
        movesGrid.refresh();
    }

    /** Just the engine-eval lines, when shown — {@link ChessBoardView}'s dedicated callback
     *  for a pure analysis-progress tick, as opposed to {@link #onBoardChanged}'s full
     *  refresh (status line + moves grid) for when the position/move tree itself actually
     *  changed. See {@code MainActivity#updateChessEngineLinesOnly} for why rebuilding the
     *  whole moves grid on every one of those ticks matters. */
    private void onAnalysisChanged() {
        if (!engineLinesShown) return;
        List<String> lines = board.engineSummary();
        for (int i = 0; i < engineLines.length; i++) {
            boolean has = i < lines.size() && !lines.get(i).isEmpty();
            engineLines[i].setVisibility(has ? View.VISIBLE : View.GONE);
            if (has) engineLines[i].setText(lines.get(i));
        }
    }

    /** A wrong move never calls {@link #onBoardChanged} (no move was actually committed — see
     *  {@link ChessBoardView#onTouchEvent}), so this is the only place that needs to touch
     *  {@code statusLine} for it: swaps the normal "White/Black to move" text for "Not quite —
     *  try again" until the next real move overwrites it in {@link #onBoardChanged}. */
    private void onWrongMove() {
        statusLine.setText("Not quite - Try again");
    }

    private void onPuzzleSolved() {
        ChessPuzzles.Puzzle p = queue.get(queueIndex);
        Config.markChessPuzzleSolved(host, p.id);
        if (Config.getChessAutoNextPuzzle(host) && queueIndex < queue.size() - 1) {
            board.postDelayed(() -> { queueIndex++; loadCurrentPuzzle(); }, 900);
        }
    }
}

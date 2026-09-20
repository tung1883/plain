package com.plainphone.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Chess in a Workspace window — same {@link ChessBoardView}/{@link MovesGrid} the Home
 *  chess panel uses, just framed as a {@link PanelContent} instead of a Home section. */
final class ChessPanel implements PanelContent {
    private Activity host;
    private ChessBoardView board;
    private TextView turnLine, moveLine, themeLine;
    private TextView[] engineLines;
    private MovesGrid movesGrid;
    private String selectedBoard = "Slate Study";
    private String selectedPieces = "neo";

    @Override public String kind() { return "chess"; }
    @Override public String title() { return "Chess"; }

    @Override public View onCreate(Context ctx) {
        host = (Activity) ctx;

        LinearLayout content = new LinearLayout(host);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(UiKit.dp(host, 18), UiKit.dp(host, 8), UiKit.dp(host, 18), UiKit.dp(host, 22));
        content.setBackgroundColor(Color.BLACK);

        board = new ChessBoardView(host, this::updateUi, this::updateEngineLinesOnly);
        board.setPieceTheme(selectedPieces);
        content.addView(board, matchWrap());

        LinearLayout status = new LinearLayout(host);
        status.setGravity(Gravity.CENTER_VERTICAL);
        status.setPadding(48, UiKit.dp(host, 16), 48, UiKit.dp(host, 12));
        turnLine = text("White to move", 17, Color.WHITE);
        status.addView(turnLine, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        moveLine = text("Move 0 / 0", 15, 0xFFDDDDDD);
        status.addView(moveLine, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        content.addView(status, matchWrap());

        engineLines = new TextView[3];
        for (int i = 0; i < engineLines.length; i++) {
            TextView line = text("", 13, 0xFF8FBF8F);
            line.setPadding(48, 0, 48, i == engineLines.length - 1 ? UiKit.dp(host, 10) : 0);
            line.setSingleLine(true);
            line.setEllipsize(android.text.TextUtils.TruncateAt.END);
            line.setVisibility(View.GONE);
            engineLines[i] = line;
            content.addView(line, matchWrap());
        }

        content.addView(rule(), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));

        LinearLayout transport = new LinearLayout(host);
        transport.setGravity(Gravity.CENTER);
        transport.setPadding(48, UiKit.dp(host, 12), 48, UiKit.dp(host, 12));
        String[] labels = {"|‹", "‹", "›", "›|"};
        for (int i = 0; i < labels.length; i++) {
            final int action = i;
            TextView button = text(labels[i], 25, Color.WHITE);
            // First/last icons line up with the moves grid's own left/right margin;
            // the two middle ones stay centered in their share of the row.
            if (i == 0) {
                button.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
                button.setTranslationX(-inkLeadIn(labels[i], 25f));
            } else if (i == labels.length - 1) {
                button.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
                button.setTranslationX(inkTrailOut(labels[i], 25f));
            } else {
                button.setGravity(Gravity.CENTER);
            }
            button.setOnClickListener(v -> {
                if (action == 0) board.first();
                else if (action == 1) board.previous();
                else if (action == 2) board.next();
                else board.last();
            });
            transport.addView(button, new LinearLayout.LayoutParams(0, UiKit.dp(host, 42), 1f));
        }
        content.addView(transport, matchWrap());

        movesGrid = new MovesGrid(host, board, this::updateUi);
        movesGrid.setPadding(48, UiKit.dp(host, 12), 48, UiKit.dp(host, 12));
        content.addView(movesGrid, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        content.addView(rule(), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));

        LinearLayout actions = new LinearLayout(host);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        actions.setPadding(0, UiKit.dp(host, 12), 0, 0);
        TextView palette = text("◉", 25, Color.WHITE);
        palette.setGravity(Gravity.CENTER);
        palette.setContentDescription("Choose chess theme");
        palette.setOnClickListener(v -> chooseThemeType());
        actions.addView(palette, new LinearLayout.LayoutParams(UiKit.dp(host, 44), UiKit.dp(host, 48)));
        content.addView(actions, matchWrap());

        themeLine = text(selectedBoard + " · " + ChessBoardView.pretty(selectedPieces), 12, 0xFFAAAAAA);
        themeLine.setGravity(Gravity.RIGHT);
        themeLine.setPadding(0, UiKit.dp(host, 2), UiKit.dp(host, 6), 0);
        content.addView(themeLine, matchWrap());

        updateUi();

        // Cold-starting Stockfish (process spawn + loading its bundled network) is the
        // expensive part, not the search itself — pay that cost now, off the UI thread,
        // so it's already paid by the time a double-tap needs an answer.
        new Thread(() -> {
            try { StockfishEngine.get(host); } catch (Exception ignored) { }
        }).start();

        ScrollView scroll = new ScrollView(host);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.BLACK);
        scroll.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return scroll;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private TextView text(String value, float size, int color) {
        TextView v = new TextView(host);
        v.setText(value);
        v.setTextSize(size);
        v.setTextColor(color);
        v.setTypeface(Fonts.current(host));
        v.setIncludeFontPadding(false);
        return v;
    }

    private float inkLeadIn(String text, float sp) {
        android.graphics.Paint p = new android.graphics.Paint();
        p.setTypeface(Fonts.current(host));
        p.setTextSize(android.util.TypedValue.applyDimension(
                android.util.TypedValue.COMPLEX_UNIT_SP, sp, host.getResources().getDisplayMetrics()));
        android.graphics.Rect bounds = new android.graphics.Rect();
        p.getTextBounds(text, 0, text.length(), bounds);
        return bounds.left;
    }

    private float inkTrailOut(String text, float sp) {
        android.graphics.Paint p = new android.graphics.Paint();
        p.setTypeface(Fonts.current(host));
        p.setTextSize(android.util.TypedValue.applyDimension(
                android.util.TypedValue.COMPLEX_UNIT_SP, sp, host.getResources().getDisplayMetrics()));
        android.graphics.Rect bounds = new android.graphics.Rect();
        p.getTextBounds(text, 0, text.length(), bounds);
        return p.measureText(text) - bounds.right;
    }

    private View rule() {
        View v = new View(host);
        v.setBackgroundColor(0xFF303030);
        return v;
    }

    private void chooseThemeType() {
        new AlertDialog.Builder(host)
                .setTitle("Themes")
                .setItems(new String[]{"Boards (37)", "Pieces (36 + Fritz)"}, (d, which) -> {
                    if (which == 0) chooseBoard(); else choosePieces();
                }).show();
    }

    private void chooseBoard() {
        List<String[]> options = new ArrayList<>(); // {slug, label}
        try {
            for (String name : host.getAssets().list("chess_theme/board")) {
                if (!name.endsWith(".png")) continue;
                String slug = name.substring(6, name.length() - 4);
                options.add(new String[]{slug, ChessBoardView.pretty(slug)});
            }
        } catch (Exception ignored) { }
        Collections.sort(options, (a, b) -> a[1].compareTo(b[1]));
        options.add(0, new String[]{"default", "Slate Study"});
        String[] labels = new String[options.size()];
        for (int i = 0; i < labels.length; i++) labels[i] = options.get(i)[1];
        new AlertDialog.Builder(host).setTitle("Board theme")
                .setItems(labels, (d, which) -> {
                    selectedBoard = options.get(which)[1];
                    board.setBoardTheme(options.get(which)[0]);
                    updateThemeLine();
                }).show();
    }

    private void choosePieces() {
        List<String> folders = new ArrayList<>();
        try { folders.addAll(Arrays.asList(host.getAssets().list("chess_theme/pieces"))); } catch (Exception ignored) { }
        Collections.sort(folders);
        String[] labels = new String[folders.size()];
        for (int i = 0; i < labels.length; i++) labels[i] = ChessBoardView.pretty(folders.get(i));
        new AlertDialog.Builder(host).setTitle("Piece theme")
                .setItems(labels, (d, which) -> {
                    selectedPieces = folders.get(which);
                    board.setPieceTheme(selectedPieces);
                    updateThemeLine();
                }).show();
    }

    private void updateThemeLine() {
        themeLine.setText(selectedBoard + " · " + ChessBoardView.pretty(selectedPieces));
    }

    private void updateUi() {
        if (board == null || turnLine == null) return;
        String gameOver = board.gameOverText();
        turnLine.setText(gameOver != null ? gameOver : board.whiteToMove() ? "White to move" : "Black to move");
        moveLine.setText("Move " + board.cursor() + " / " + board.totalMoves());
        List<String> lines = board.engineSummary();
        for (int i = 0; i < engineLines.length; i++) {
            boolean has = i < lines.size();
            engineLines[i].setVisibility(has ? View.VISIBLE : View.GONE);
            if (has) engineLines[i].setText(lines.get(i));
        }
        movesGrid.refresh();
    }

    /** Just the engine-eval lines — {@link ChessBoardView}'s dedicated callback for a pure
     *  analysis-progress tick, as opposed to {@link #updateUi}'s full refresh (status lines
     *  + moves grid) for when the position/move tree itself actually changed. See
     *  {@code MainActivity#updateChessEngineLinesOnly} for why rebuilding the whole moves
     *  grid on every one of those ticks matters. */
    private void updateEngineLinesOnly() {
        if (board == null) return;
        List<String> lines = board.engineSummary();
        for (int i = 0; i < engineLines.length; i++) {
            boolean has = i < lines.size();
            engineLines[i].setVisibility(has ? View.VISIBLE : View.GONE);
            if (has) engineLines[i].setText(lines.get(i));
        }
    }
}

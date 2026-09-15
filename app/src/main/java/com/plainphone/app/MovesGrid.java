package com.plainphone.app;

import android.app.Activity;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/** Move list as a grid, packed move-by-move into rows based on the container's actual
 *  measured width — however many full moves fit is however many land on a row, so
 *  there's no leftover blank strip from a hardcoded per-row count that happens not to
 *  match the available width. Every cell hugs its own text; the gap between the two
 *  halves of one move is a small fixed margin, the gap before the next move a clearly
 *  bigger one, both sized off the text's own line height rather than a raw dp constant.
 *  Current ply highlighted white, the rest greyed out; tap any half-move to jump the
 *  board there. Shared by the Home chess panel and the Workspace chess panel. */
final class MovesGrid extends LinearLayout {
    private static final float TEXT_SP = 14f;

    private final ChessBoardView board;
    private final Runnable afterJump;

    MovesGrid(Activity host, ChessBoardView board, Runnable afterJump) {
        super(host);
        setOrientation(VERTICAL);
        this.board = board;
        this.afterJump = afterJump;
    }

    void refresh() {
        Activity host = (Activity) getContext();
        removeAllViews();
        List<String> moves = board.movesList();
        if (moves.isEmpty()) return;
        // One text-line's own rendered height stands in for "1 unit" of space, so
        // gaps scale with whatever font size/scale the device is actually using.
        android.graphics.Paint metrics = new android.graphics.Paint();
        metrics.setTypeface(Fonts.current(host));
        metrics.setTextSize(android.util.TypedValue.applyDimension(
                android.util.TypedValue.COMPLEX_UNIT_SP, TEXT_SP, host.getResources().getDisplayMetrics()));
        int unit = Math.round(-metrics.ascent() + metrics.descent());
        int tightGap = Math.round(unit * 0.3f);   // between the two halves of one move
        int moveGap = Math.round(unit * 0.9f);    // between one move and the next
        int vPad = Math.round(unit * 0.28f);
        // Reserve enough width for the longest realistic move up front (a disambiguated,
        // promoting capture like "Nbxd7" or "bxa1=Q") so a cell never resizes/reflows
        // once a longer move than seen so far actually gets played.
        int moveW = Math.round(Math.max(metrics.measureText("Nbxd7"), metrics.measureText("bxa1=Q")));

        // Full screen width is too generous a guess: it ignores this view's own padding
        // and its ancestors', so a row packed against it can end up wider than what's
        // actually visible and get clipped at the edge. Wait for a real measured width
        // instead of guessing.
        int availWidth = getWidth() - getPaddingLeft() - getPaddingRight();
        if (availWidth <= 0) {
            View parent = getParent() instanceof View ? (View) getParent() : null;
            if (parent != null && parent.getWidth() > 0) {
                availWidth = parent.getWidth() - parent.getPaddingLeft() - parent.getPaddingRight();
            }
        }
        if (availWidth <= 0) {
            post(this::refresh);
            return;
        }

        // How many full moves fit per row, from a fixed per-pair width (move number +
        // two fixed-width move slots) rather than each row's own actual content — that
        // fixed budget is what lets a *full* row be justified to fill the width exactly
        // (leftover distributed into its inter-move gaps) instead of leaving one
        // dangling blank chunk wherever the last move that fit happens to end.
        int numW = Math.round(metrics.measureText("88."));
        int pairWidth = numW + moveW + tightGap + moveW;
        int movesPerRow = Math.max(1, (availWidth + moveGap) / (pairWidth + moveGap));

        int fullMoves = (moves.size() + 1) / 2;
        int currentPly = board.cursor();
        LinearLayout row = null;
        int posInRow = 0;
        int rowMoveGap = moveGap;
        for (int m = 0; m < fullMoves; m++) {
            if (m % movesPerRow == 0) {
                int countThisRow = Math.min(movesPerRow, fullMoves - m);
                boolean fullRow = countThisRow == movesPerRow;
                int leftover = fullRow ? Math.max(0, availWidth
                        - (countThisRow * pairWidth + (countThisRow - 1) * moveGap)) : 0;
                rowMoveGap = moveGap + (countThisRow > 1 ? leftover / (countThisRow - 1) : 0);
                row = new LinearLayout(host);
                row.setOrientation(HORIZONTAL);
                addView(row, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                posInRow = 0;
            }
            int i0 = m * 2, i1 = m * 2 + 1;
            boolean hasBlack = i1 < moves.size();
            View whiteCell = buildCell(host, moves, i0, currentPly, moveW);
            View blackCell = hasBlack ? buildCell(host, moves, i1, currentPly, moveW) : null;
            whiteCell.setPadding(0, vPad, 0, vPad);

            LinearLayout.LayoutParams wlp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            wlp.leftMargin = posInRow > 0 ? rowMoveGap : 0;
            wlp.rightMargin = hasBlack ? tightGap : 0;
            row.addView(whiteCell, wlp);
            if (hasBlack) {
                blackCell.setPadding(0, vPad, 0, vPad);
                row.addView(blackCell, new LinearLayout.LayoutParams(moveW, ViewGroup.LayoutParams.WRAP_CONTENT));
            }
            posInRow++;
        }
    }

    private View buildCell(Activity host, List<String> moves, int i, int currentPly, int moveW) {
        boolean white = (i % 2) == 0;
        boolean current = i == currentPly - 1;
        int textColor = current ? Color.WHITE : 0xFF6E6E6E;
        final int ply = i + 1;
        View.OnClickListener jump = v -> {
            board.jumpTo(ply);
            if (afterJump != null) afterJump.run();
        };

        View cell;
        if (white) {
            LinearLayout box = new LinearLayout(host);
            box.setOrientation(HORIZONTAL);
            TextView num = new TextView(host);
            num.setText(((i / 2) + 1) + ".");
            num.setTypeface(Fonts.current(host));
            num.setTextSize(TEXT_SP);
            num.setSingleLine(true);
            num.setTextColor(textColor);
            TextView move = new TextView(host);
            move.setText(moves.get(i));
            move.setTypeface(Fonts.current(host));
            move.setTextSize(TEXT_SP);
            move.setSingleLine(true);
            move.setGravity(Gravity.END);
            move.setTextColor(textColor);
            box.addView(num);
            box.addView(move, new LinearLayout.LayoutParams(moveW, ViewGroup.LayoutParams.WRAP_CONTENT));
            cell = box;
        } else {
            TextView move = new TextView(host);
            move.setText(moves.get(i));
            move.setTypeface(Fonts.current(host));
            move.setTextSize(TEXT_SP);
            move.setSingleLine(true);
            move.setGravity(Gravity.END);
            move.setTextColor(textColor);
            move.setLayoutParams(new LinearLayout.LayoutParams(moveW, ViewGroup.LayoutParams.WRAP_CONTENT));
            cell = move;
        }
        cell.setOnClickListener(jump);
        return cell;
    }
}

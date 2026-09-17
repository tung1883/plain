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
 *  board there. Below the grid, any recorded variation at a ply on this line gets its
 *  own dimmed row (tap to jump into it). Long-press a move for delete / promote-to-mainline.
 *  Shared by the Home chess panel and the Workspace chess panel. */
final class MovesGrid extends LinearLayout {
    private static final float TEXT_SP = 14f;
    private static final float VARIATION_SP = 13f;

    private final ChessBoardView board;
    private final Runnable afterJump;
    // Set by whichever cell/row turns out to be the current one, mid-refresh(), so the
    // caller can scroll it into view once the whole grid is (re)built.
    private View currentCellView;
    // Which node the grid last auto-scrolled to — background analysis finishing calls
    // refresh() too (same position, new eval text), and re-scrolling every one of those
    // fights any manual scroll the user is mid-gesture on. Only actually moving to a
    // different move re-triggers the scroll.
    private ChessBoardView.MoveNode lastAutoScrolledNode;

    MovesGrid(Activity host, ChessBoardView board, Runnable afterJump) {
        super(host);
        setOrientation(VERTICAL);
        this.board = board;
        this.afterJump = afterJump;
    }

    void refresh() {
        Activity host = (Activity) getContext();
        removeAllViews();
        currentCellView = null;
        List<ChessBoardView.DisplayLine> lines = board.displayLines();
        if (lines.isEmpty()) return;

        ChessBoardView.MoveNode currentNode = board.currentNode();
        ChessBoardView.DisplayLine mainLine = lines.get(0);
        addMainLineGrid(host, mainLine.nodes, currentNode);

        for (int i = 1; i < lines.size(); i++) {
            addView(buildVariationRow(host, lines.get(i), currentNode));
        }

        // Scrolls only this grid's own immediate ScrollView — never the panel around it.
        // requestRectangleOnScreen looked like the obvious tool here, but it walks and
        // scrolls *every* scrollable ancestor, not just the nearest one, so a new move was
        // also dragging the whole panel around; that's what made scrolling back up by hand
        // feel like it kept getting fought.
        if (currentCellView != null && currentNode != lastAutoScrolledNode) {
            lastAutoScrolledNode = currentNode;
            View target = currentCellView;
            post(() -> {
                if (!(getParent() instanceof android.widget.ScrollView)) return;
                android.widget.ScrollView scroll = (android.widget.ScrollView) getParent();
                // post() delays this to the next frame — with analysis streaming several
                // refresh() calls a second, a newer one can removeAllViews() (detaching
                // `target` from the tree entirely) before this runs. Walking up would then
                // hit a null parent and NPE on getTop(); bail instead if that's happened.
                int y = 0;
                View v = target;
                while (v != null && v != this) {
                    y += v.getTop();
                    Object p = v.getParent();
                    v = (p instanceof View) ? (View) p : null;
                }
                if (v != this) return;
                // Bottom-align: shows the new move plus whatever context fits above it,
                // rather than pinning it right at the top edge of the small window.
                int dest = Math.max(0, y - scroll.getHeight() + target.getHeight());
                scroll.smoothScrollTo(0, dest);
            });
        }
    }

    private void addMainLineGrid(Activity host, List<ChessBoardView.MoveNode> nodes, ChessBoardView.MoveNode currentNode) {
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

        int fullMoves = (nodes.size() + 1) / 2;
        LinearLayout row = null;
        int posInRow = 0;
        int rowMoveGap = moveGap;
        // A commented move breaks the row right after it, ChessBase-style — the comment
        // prints as its own line directly under the move it's on, not batched into one
        // "comments" block detached from the grid. The next pair starts a fresh row below.
        boolean forceNewRow = false;
        for (int m = 0; m < fullMoves; m++) {
            if (forceNewRow || m % movesPerRow == 0) {
                int countThisRow = Math.min(movesPerRow, fullMoves - m);
                boolean fullRow = !forceNewRow && countThisRow == movesPerRow;
                int leftover = fullRow ? Math.max(0, availWidth
                        - (countThisRow * pairWidth + (countThisRow - 1) * moveGap)) : 0;
                rowMoveGap = moveGap + (countThisRow > 1 ? leftover / (countThisRow - 1) : 0);
                row = new LinearLayout(host);
                row.setOrientation(HORIZONTAL);
                addView(row, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                posInRow = 0;
                forceNewRow = false;
            }
            int i0 = m * 2, i1 = m * 2 + 1;
            boolean hasBlack = i1 < nodes.size();
            ChessBoardView.MoveNode whiteNode = nodes.get(i0);
            ChessBoardView.MoveNode blackNode = hasBlack ? nodes.get(i1) : null;
            View whiteCell = buildCell(host, whiteNode, whiteNode == currentNode, moveW, numW);
            View blackCell = hasBlack ? buildCell(host, blackNode, blackNode == currentNode, moveW, numW) : null;
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

            if (whiteNode.comment != null) { addView(commentRow(host, moveLabel(whiteNode), whiteNode.comment)); forceNewRow = true; }
            if (blackNode != null && blackNode.comment != null) { addView(commentRow(host, moveLabel(blackNode), blackNode.comment)); forceNewRow = true; }
        }
    }

    /** One mainline move's comment, right under the row it's on — "5. Nc3 — A model
     *  Najdorf." Not clickable; the move cell right above it already jumps there. */
    private View commentRow(Activity host, String label, String comment) {
        TextView t = new TextView(host);
        t.setText(label + " — " + comment);
        t.setTypeface(Fonts.current(host), android.graphics.Typeface.ITALIC);
        t.setTextSize(VARIATION_SP);
        t.setTextColor(0xFFB7B7B7);
        int pad = UiKit.dp(host, 18);
        t.setPadding(pad, UiKit.dp(host, 2), pad, UiKit.dp(host, 10));
        return t;
    }

    private View buildCell(Activity host, ChessBoardView.MoveNode node, boolean current, int moveW, int numW) {
        int textColor = current ? Color.WHITE : 0xFF6E6E6E;
        int ply = node.ply();
        boolean white = (ply % 2) == 1;
        View.OnClickListener jump = v -> {
            board.jumpToNode(node);
            if (afterJump != null) afterJump.run();
        };
        View.OnLongClickListener menu = v -> {
            showMoveMenu(host, node, false);
            return true;
        };

        View cell;
        if (white) {
            LinearLayout box = new LinearLayout(host);
            box.setOrientation(HORIZONTAL);
            TextView num = new TextView(host);
            num.setText(((ply + 1) / 2) + ".");
            num.setTypeface(Fonts.current(host));
            num.setTextSize(TEXT_SP);
            num.setSingleLine(true);
            num.setTextColor(textColor);
            TextView move = new TextView(host);
            move.setText(node.san);
            move.setTypeface(Fonts.current(host));
            move.setTextSize(TEXT_SP);
            move.setSingleLine(true);
            move.setGravity(Gravity.END);
            move.setTextColor(textColor);
            // Fixed width, same as the packing math assumed — otherwise a 1-digit move
            // number ("8.") sits narrower than a 2-digit one ("18.") once that many moves
            // are on the board, and every column after it in the row drifts out of line.
            box.addView(num, new LinearLayout.LayoutParams(numW, ViewGroup.LayoutParams.WRAP_CONTENT));
            box.addView(move, new LinearLayout.LayoutParams(moveW, ViewGroup.LayoutParams.WRAP_CONTENT));
            cell = box;
        } else {
            TextView move = new TextView(host);
            move.setText(node.san);
            move.setTypeface(Fonts.current(host));
            move.setTextSize(TEXT_SP);
            move.setSingleLine(true);
            move.setGravity(Gravity.END);
            move.setTextColor(textColor);
            move.setLayoutParams(new LinearLayout.LayoutParams(moveW, ViewGroup.LayoutParams.WRAP_CONTENT));
            cell = move;
        }
        cell.setOnClickListener(jump);
        cell.setOnLongClickListener(menu);
        if (current) currentCellView = cell;
        return cell;
    }

    /** "(2...Nf6)" or "(3.Nc3 Bc5)" — a dimmed, indented, parenthesized flattening of a
     *  variation's own moves. Every move in it is its own tap target (jumps straight to
     *  that exact ply, not just the line's tip) via a {@link android.text.style.ClickableSpan}
     *  per move; long-press maps the touch position back to whichever move it landed on for
     *  its own delete/promote menu. If the board is currently showing a position inside this
     *  exact variation, that one move (and only that one) turns white — the row itself
     *  never moves or changes position for it. */
    private View buildVariationRow(Activity host, ChessBoardView.DisplayLine line, ChessBoardView.MoveNode currentNode) {
        TextView text = new TextView(host);
        text.setText(formatVariation(line, currentNode));
        text.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
        text.setHighlightColor(Color.TRANSPARENT); // no link-press flash — matches the plain cells
        text.setTypeface(Fonts.current(host));
        text.setTextSize(VARIATION_SP);
        text.setTextColor(0xFF4A4A4A);
        text.setTypeface(text.getTypeface(), android.graphics.Typeface.ITALIC);
        int pad = UiKit.dp(host, 18);
        text.setPadding(pad, UiKit.dp(host, 4), 0, UiKit.dp(host, 4));
        if (line.nodes.contains(currentNode)) currentCellView = text;

        android.view.GestureDetector longPress = new android.view.GestureDetector(host,
                new android.view.GestureDetector.SimpleOnGestureListener() {
            @Override public void onLongPress(android.view.MotionEvent e) {
                int offset = text.getOffsetForPosition(e.getX(), e.getY());
                ChessBoardView.MoveNode node = nodeAtOffset(text, offset);
                showMoveMenu(host, node != null ? node : line.nodes.get(0), true);
            }
        });
        text.setOnTouchListener((v, e) -> {
            longPress.onTouchEvent(e);
            return false; // ClickableSpan taps still go through LinkMovementMethod normally
        });
        return text;
    }

    /** Which move's {@link MoveSpan} (if any) covers a character offset — used to turn a
     *  long-press's raw touch coordinates into "the move the finger landed on". */
    private ChessBoardView.MoveNode nodeAtOffset(TextView text, int offset) {
        CharSequence cs = text.getText();
        if (!(cs instanceof android.text.Spanned)) return null;
        android.text.Spanned spanned = (android.text.Spanned) cs;
        for (MoveSpan span : spanned.getSpans(offset, offset, MoveSpan.class)) return span.node;
        return null;
    }

    /** A tap on exactly this move's text jumps the board straight to it — one span per move
     *  in a variation, so unlike the row's old single whole-string tap target, tapping move
     *  2 of a 3-move sideline lands on move 2, not the sideline's tip. */
    private final class MoveSpan extends android.text.style.ClickableSpan {
        final ChessBoardView.MoveNode node;
        MoveSpan(ChessBoardView.MoveNode node) { this.node = node; }
        @Override public void onClick(View widget) {
            board.jumpToNode(node);
            if (afterJump != null) afterJump.run();
        }
        @Override public void updateDrawState(android.text.TextPaint ds) {
            // Leave color/underline alone — formatVariation's own ForegroundColorSpan (and
            // the surrounding dimmed italic style) already says everything this needs to.
        }
    }

    private CharSequence formatVariation(ChessBoardView.DisplayLine line, ChessBoardView.MoveNode currentNode) {
        android.text.SpannableStringBuilder sb = new android.text.SpannableStringBuilder("(");
        int ply = line.startPly;
        for (int i = 0; i < line.nodes.size(); i++) {
            ChessBoardView.MoveNode node = line.nodes.get(i);
            boolean whiteMove = (ply % 2) == 1;
            int moveNum = (ply + 1) / 2;
            if (whiteMove) {
                if (i > 0) sb.append(' ');
                sb.append(String.valueOf(moveNum)).append('.');
            } else if (i == 0) {
                sb.append(String.valueOf(moveNum)).append("...");
            } else {
                sb.append(' ');
            }
            int start = sb.length();
            sb.append(node.san);
            int end = sb.length();
            sb.setSpan(new MoveSpan(node), start, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            if (node == currentNode) {
                sb.setSpan(new android.text.style.ForegroundColorSpan(Color.WHITE),
                        start, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            ply++;
        }
        sb.append(')');
        return sb;
    }

    /** "6...dxc6" / "6.e4" — the move's own SAN with its move number and dots, the same
     *  numbering {@link #formatVariation} already uses for the line it sits in. */
    private String moveLabel(ChessBoardView.MoveNode node) {
        int ply = node.ply();
        boolean whiteMove = (ply % 2) == 1;
        int moveNum = (ply + 1) / 2;
        return moveNum + (whiteMove ? "." : "...") + node.san;
    }

    /** Standard rounded plainphone popup — same chrome as the chess settings sheet — with
     *  "Promote to mainline" (variations only) and "Delete this move". */
    private void showMoveMenu(Activity host, ChessBoardView.MoveNode node, boolean allowPromote) {
        LinearLayout root = new LinearLayout(host);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(UiKit.dialogBackground(host));
        UiKit.clipRounded(host, root, UiKit.R_MD);
        // Top and bottom insets must both be at least the corner radius, or the title/last
        // row's own opaque, square-cornered background paints over the rounded corner's
        // curve there — reads as part of the border being square instead of round.
        int edgeInset = UiKit.dp(host, UiKit.R_MD);
        root.setPadding(2, edgeInset, 2, edgeInset);
        root.addView(UiKit.dialogTitleExact(host, moveLabel(node)));

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(host).setView(root).create();
        UiKit.clearDialogChrome(dialog);
        // Both already trigger the board's own onChanged callback (updateChessHomeUi, which
        // calls this.refresh() itself), so nothing further is needed here.
        if (allowPromote) {
            root.addView(menuRow(host, "Promote to mainline", v -> {
                dialog.dismiss();
                board.promoteToMainline(node);
            }));
        }
        root.addView(menuRow(host, node.comment == null ? "Add comment" : "Edit comment", v -> {
            dialog.dismiss();
            UiKit.textPrompt(host, "Comment", node.comment, "Save",
                    text -> board.setComment(node, text));
        }));
        if (node.comment != null) {
            root.addView(menuRow(host, "Remove comment", v -> {
                dialog.dismiss();
                board.setComment(node, null);
            }));
        }
        root.addView(menuRow(host, "Delete this move", v -> {
            dialog.dismiss();
            board.deleteNode(node);
        }));
        root.addView(menuRow(host, "Cancel", v -> dialog.dismiss()));

        dialog.show();
        UiKit.unboxDialog(root);
        if (dialog.getWindow() != null) {
            android.view.WindowManager.LayoutParams p = dialog.getWindow().getAttributes();
            p.width = (int) (host.getResources().getDisplayMetrics().widthPixels * 0.85);
            dialog.getWindow().setAttributes(p);
        }
    }

    private TextView menuRow(Activity host, String label, View.OnClickListener listener) {
        TextView row = new TextView(host);
        row.setText(label);
        row.setTextColor(Color.WHITE);
        row.setTextSize(20);
        row.setTypeface(Fonts.current(host));
        row.setPadding(48, 32, 48, 32);
        android.graphics.drawable.StateListDrawable bg = new android.graphics.drawable.StateListDrawable();
        bg.addState(new int[]{android.R.attr.state_pressed},
                new android.graphics.drawable.ColorDrawable(Color.DKGRAY));
        bg.addState(new int[]{}, new android.graphics.drawable.ColorDrawable(Color.BLACK));
        row.setBackground(bg);
        row.setOnClickListener(listener);
        return row;
    }

}

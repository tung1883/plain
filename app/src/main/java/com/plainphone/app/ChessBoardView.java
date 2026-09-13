package com.plainphone.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.View;

/** Monochrome, coordinate-labelled chessboard. The study view owns navigation. */
final class ChessBoardView extends View {
    interface SquareTap { void onSquare(int square); }
    private final Paint square = new Paint(), piece = new Paint(Paint.ANTI_ALIAS_FLAG), label = new Paint(Paint.ANTI_ALIAS_FLAG);
    private char[] board = new char[64]; private boolean flipped; private int selected = -1; private SquareTap tap;
    ChessBoardView(Context c) {
        super(c); setBackgroundColor(Color.BLACK); java.util.Arrays.fill(board, '.');
        piece.setTypeface(Typeface.create(Typeface.SERIF, Typeface.NORMAL)); piece.setTextAlign(Paint.Align.CENTER);
        label.setColor(0xFF8A8A8A); label.setTextAlign(Paint.Align.CENTER); label.setTypeface(Fonts.cascadiaMono(c));
    }
    void show(ChessGame g, int ply) { board = g.positionAt(ply); selected = -1; invalidate(); }
    void flip() { flipped = !flipped; invalidate(); }
    void setTap(SquareTap tap) { this.tap = tap; }
    void select(int square) { selected = square; invalidate(); }
    @Override protected void onDraw(Canvas c) {
        super.onDraw(c); float pad = getWidth() * .055f, size = Math.min(getWidth() - pad, getHeight() - pad), cell = size / 8f;
        float left = (getWidth() - size) / 2f, top = (getHeight() - size) / 2f;
        for (int rank = 0; rank < 8; rank++) for (int file = 0; file < 8; file++) {
            int displayFile = flipped ? 7 - file : file, displayRank = flipped ? rank : 7 - rank;
            square.setColor(((file + rank) & 1) == 0 ? 0xFFDFDFD5 : 0xFF555650);
            c.drawRect(left + file * cell, top + rank * cell, left + (file + 1) * cell, top + (rank + 1) * cell, square);
            if (selected == displayRank * 8 + displayFile) {
                square.setStyle(Paint.Style.STROKE); square.setStrokeWidth(Math.max(3, cell * .08f)); square.setColor(0xFF111111);
                c.drawRect(left + file * cell + 1, top + rank * cell + 1, left + (file + 1) * cell - 1, top + (rank + 1) * cell - 1, square); square.setStyle(Paint.Style.FILL);
            }
            char p = board[displayRank * 8 + displayFile]; if (p == '.') continue;
            piece.setTextSize(cell * .76f); piece.setColor(Character.isUpperCase(p) ? 0xFFF8F8F2 : 0xFF171717);
            piece.setShadowLayer(1.5f, 0, 1, Character.isUpperCase(p) ? 0xFF333333 : 0xFFAAAAAA);
            c.drawText(symbol(p), left + (file + .5f) * cell, top + (rank + .75f) * cell, piece); piece.clearShadowLayer();
        }
        label.setTextSize(Math.max(10, cell * .20f));
        for (int i = 0; i < 8; i++) {
            int file = flipped ? 7 - i : i, rank = flipped ? i : 7 - i;
            c.drawText(String.valueOf((char)('a' + file)), left + (i + .5f) * cell, top + size + label.getTextSize() * 1.2f, label);
            c.drawText(String.valueOf(rank + 1), left - label.getTextSize() * .7f, top + (i + .63f) * cell, label);
        }
    }
    @Override public boolean performClick() { super.performClick(); return true; }
    @Override public boolean onTouchEvent(android.view.MotionEvent e) {
        if (e.getAction() != android.view.MotionEvent.ACTION_UP) return true;
        performClick(); float pad = getWidth() * .055f, size = Math.min(getWidth() - pad, getHeight() - pad), left=(getWidth()-size)/2f, top=(getHeight()-size)/2f;
        int df=(int)((e.getX()-left)/(size/8f)), dr=(int)((e.getY()-top)/(size/8f));
        if (df < 0 || df > 7 || dr < 0 || dr > 7 || tap == null) return true;
        tap.onSquare((flipped ? dr : 7-dr) * 8 + (flipped ? 7-df : df)); return true;
    }
    private String symbol(char p) {
        switch (Character.toUpperCase(p)) { case 'K': return Character.isUpperCase(p) ? "♔" : "♚"; case 'Q': return Character.isUpperCase(p) ? "♕" : "♛"; case 'R': return Character.isUpperCase(p) ? "♖" : "♜"; case 'B': return Character.isUpperCase(p) ? "♗" : "♝"; case 'N': return Character.isUpperCase(p) ? "♘" : "♞"; default: return Character.isUpperCase(p) ? "♙" : "♟"; }
    }
}

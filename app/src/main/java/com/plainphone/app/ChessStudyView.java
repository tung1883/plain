package com.plainphone.app;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** Reusable study surface for the full-screen Chess plugin and the workspace panel. */
final class ChessStudyView {
    interface Listener { void changed(String id, int ply, String title); }
    private final Activity activity; private final Listener listener; private final Runnable exportAction;
    private ChessGame game = ChessGame.newGame(); private String gameId; private int ply, selected = -1;
    private ChessBoardView board; private TextView meta, status; private LinearLayout moves;
    ChessStudyView(Activity activity, Listener listener) { this(activity, listener, null); }
    ChessStudyView(Activity activity, Listener listener, Runnable exportAction) { this.activity = activity; this.listener = listener; this.exportAction = exportAction; }
    View create() {
        ScrollView scroll = new ScrollView(activity); scroll.setFillViewport(true); scroll.setBackgroundColor(Color.BLACK);
        LinearLayout root = new LinearLayout(activity); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(16), dp(14), dp(16), dp(24)); scroll.addView(root);
        LinearLayout actions = new LinearLayout(activity); actions.setGravity(Gravity.CENTER_VERTICAL);
        actions.addView(button("NEW BOARD", this::newBoard)); actions.addView(button("PASTE PGN", this::paste)); actions.addView(button("SAVE PGN", this::savePgn)); actions.addView(button("FLIP", () -> board.flip()));
        root.addView(actions);
        meta = text(15, 0xFFE6E6E6); meta.setPadding(dp(4), dp(18), dp(4), dp(3)); root.addView(meta);
        status = text(12, 0xFF8A8A8A); status.setPadding(dp(4), 0, dp(4), dp(12)); root.addView(status);
        board = new ChessBoardView(activity); board.setTap(this::tapSquare); root.addView(board, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(320)));
        LinearLayout nav = new LinearLayout(activity); nav.setGravity(Gravity.CENTER); nav.setPadding(0, dp(12), 0, dp(8));
        nav.addView(button("|◀", () -> setPly(0))); nav.addView(button("◀", () -> setPly(ply - 1))); nav.addView(button("▶", () -> setPly(ply + 1))); nav.addView(button("▶|", () -> setPly(game.plies()))); nav.addView(button("COPY FEN", this::copyFen));
        root.addView(nav);
        TextView caption = text(12, 0xFF8A8A8A); caption.setText("MOVES"); caption.setLetterSpacing(.14f); caption.setPadding(dp(4), dp(12), 0, dp(8)); root.addView(caption);
        moves = new LinearLayout(activity); moves.setOrientation(LinearLayout.VERTICAL); moves.setBackground(UiKit.rounded(activity, Color.BLACK, 0xFF2C2C2C, 1, UiKit.R_MD)); UiKit.clipRounded(activity, moves, UiKit.R_MD); root.addView(moves);
        render(); return scroll;
    }
    void load(String id, int wantedPly) { ChessStore.Entry e = id == null ? null : ChessStore.find(activity, id); if (e != null) { gameId=e.id; game=e.game(); ply=Math.max(0,Math.min(wantedPly,game.plies())); } render(); }
    void loadPgn(String pgn) { ChessStore.Entry e = ChessStore.save(activity, gameId, pgn); gameId=e.id; game=e.game(); ply=0; selected=-1; render(); }
    String gameId() { return gameId == null ? "" : gameId; } int ply() { return ply; }
    String pgn() { return game.toPgn(); }
    private void paste() {
        EditText input = new EditText(activity); input.setHint("Paste PGN"); input.setHintTextColor(Color.GRAY); input.setTextColor(Color.WHITE); input.setTypeface(Fonts.current(activity)); input.setTextSize(15); input.setGravity(Gravity.TOP); input.setMinLines(8); input.setBackgroundColor(Color.BLACK); input.setPadding(dp(20),dp(10),dp(20),dp(10));
        android.app.AlertDialog d = new android.app.AlertDialog.Builder(activity).setTitle("Paste PGN").setView(input).setPositiveButton("Open", null).setNegativeButton("Cancel", null).create();
        d.setOnShowListener(x -> d.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> { String p=input.getText().toString().trim(); if (p.isEmpty()) { input.setError("Paste a PGN first"); return; } loadPgn(p); d.dismiss(); })); d.show();
    }
    private void newBoard() { game = ChessGame.newGame(); gameId = null; ply = 0; selected = -1; render(); }
    private void tapSquare(int square) {
        if (ply != game.plies()) { Toast.makeText(activity, "Go to the end of the game to play from the board", Toast.LENGTH_SHORT).show(); return; }
        char piece = game.positionAt(ply)[square];
        if (selected < 0) {
            if (piece == '.' || Character.isUpperCase(piece) != game.whiteAt(ply)) return;
            selected = square; board.select(square); return;
        }
        if (selected == square) { selected = -1; board.select(-1); return; }
        if (game.move(selected, square)) { ply = game.plies(); selected = -1; persist(); render(); }
        else if (piece != '.' && Character.isUpperCase(piece) == game.whiteAt(ply)) { selected = square; board.select(square); }
        else Toast.makeText(activity, "Illegal move", Toast.LENGTH_SHORT).show();
    }
    private void persist() { ChessStore.Entry e = ChessStore.save(activity, gameId, game.toPgn()); gameId = e.id; }
    private void savePgn() {
        persist();
        if (exportAction != null) { exportAction.run(); return; }
        ((ClipboardManager)activity.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("PGN", game.toPgn()));
        Toast.makeText(activity, "PGN copied", Toast.LENGTH_SHORT).show();
    }
    private void setPly(int next) { ply=Math.max(0,Math.min(next,game.plies())); render(); }
    private void render() {
        if (board == null) return; board.show(game, ply); meta.setText(game.title()); status.setText(game.subtitle() + "  ·  " + (ply == 0 ? "White to move" : (ply + ". " + game.moves.get(ply - 1)))); moves.removeAllViews();
        if (game.moves.isEmpty()) { TextView empty=text(14,0xFF8A8A8A); empty.setText("Tap a piece, then its destination. New games promote pawns to queens."); empty.setPadding(dp(18),dp(18),dp(18),dp(18)); moves.addView(empty); }
        for (int i=0;i<game.moves.size();i+=2) { LinearLayout row=new LinearLayout(activity); TextView no=text(13,0xFF8A8A8A); no.setText((i/2+1)+"."); no.setGravity(Gravity.CENTER_VERTICAL); no.setPadding(dp(14),dp(10),0,dp(10)); row.addView(no,new LinearLayout.LayoutParams(dp(42),ViewGroup.LayoutParams.WRAP_CONTENT)); row.addView(moveButton(i)); if(i+1<game.moves.size()) row.addView(moveButton(i+1)); moves.addView(row); }
        if(listener!=null) listener.changed(gameId(),ply,game.title());
    }
    private TextView moveButton(int index) { TextView v=text(15,index+1==ply?Color.BLACK:0xFFE6E6E6); v.setText(game.moves.get(index)); v.setGravity(Gravity.CENTER); v.setPadding(dp(10),dp(10),dp(10),dp(10)); v.setBackground(index+1==ply?UiKit.rounded(activity,0xFFE0E0D5,0xFFE0E0D5,0,UiKit.R_XS):null); v.setOnClickListener(x->setPly(index+1)); v.setLayoutParams(new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1)); return v; }
    private void copyFen() { ((ClipboardManager)activity.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("FEN",game.fenAt(ply))); Toast.makeText(activity,"FEN copied",Toast.LENGTH_SHORT).show(); }
    private TextView button(String label, Runnable action) { TextView b=text(12,Color.WHITE); b.setText(label); b.setGravity(Gravity.CENTER); b.setPadding(dp(12),dp(9),dp(12),dp(9)); b.setBackground(UiKit.pressable(activity,0xFF161616,0xFF353535,0xFF565656,1,UiKit.R_XS)); b.setOnClickListener(v->action.run()); LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT); lp.rightMargin=dp(7); b.setLayoutParams(lp); return b; }
    private TextView text(float size,int color){TextView t=new TextView(activity);t.setTextColor(color);t.setTextSize(size);t.setTypeface(Fonts.current(activity));return t;} private int dp(float n){return UiKit.dp(activity,n);}
}

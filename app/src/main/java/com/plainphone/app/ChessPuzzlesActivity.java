package com.plainphone.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;

/** Puzzles the generator has found — tap one to load it onto the board in solving mode
 *  ({@link ChessBoardView#loadPuzzle}). Same {@link ListView}/{@link BaseAdapter} recycling as
 *  {@link ChessLibraryActivity}, though this list is realistically much shorter (one puzzle
 *  per game found, at most). */
public class ChessPuzzlesActivity extends Activity {

    static final String EXTRA_FEN = "fen";
    static final String EXTRA_SOLUTION = "solution";
    static final String EXTRA_WHITE = "white";
    static final String EXTRA_BLACK = "black";

    private List<ChessPuzzles.Puzzle> puzzles;
    private BaseAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ListView list = new ListView(this);
        list.setDivider(new ColorDrawable(0xFF303030));
        list.setDividerHeight(1);
        list.setCacheColorHint(Color.BLACK);
        list.setBackgroundColor(Color.BLACK);
        adapter = new BaseAdapter() {
            @Override public int getCount() { return puzzles == null ? 0 : puzzles.size(); }
            @Override public Object getItem(int position) { return puzzles.get(position); }
            @Override public long getItemId(int position) { return position; }
            @Override public View getView(int position, View recycled, ViewGroup parent) {
                View row = recycled != null ? recycled : row();
                bindRow(row, puzzles.get(position));
                return row;
            }
        };
        list.setAdapter(adapter);
        list.setOnItemClickListener((parent, view, position, id) -> pick(puzzles.get(position)));

        UiKit.screen(this, "Puzzles", list);
        load();
    }

    private void load() {
        new Thread(() -> {
            List<ChessPuzzles.Puzzle> loaded = ChessPuzzles.loadAll(this);
            runOnUiThread(() -> {
                puzzles = loaded;
                adapter.notifyDataSetChanged();
            });
        }).start();
    }

    private void pick(ChessPuzzles.Puzzle p) {
        Intent result = new Intent();
        result.putExtra(EXTRA_FEN, p.startFen);
        result.putExtra(EXTRA_SOLUTION, String.join(" ", p.solutionUci));
        result.putExtra(EXTRA_WHITE, p.white);
        result.putExtra(EXTRA_BLACK, p.black);
        setResult(RESULT_OK, result);
        finish();
    }

    private View row() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(UiKit.dp(this, 24), UiKit.dp(this, 12), UiKit.dp(this, 24), UiKit.dp(this, 12));
        StateListDrawable bg = new StateListDrawable();
        bg.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Color.DKGRAY));
        bg.addState(new int[]{}, new ColorDrawable(Color.BLACK));
        row.setBackground(bg);
        row.addView(text(16, Color.WHITE));
        TextView meta = text(12, 0xFF6E6E6E);
        meta.setPadding(0, UiKit.dp(this, 3), 0, 0);
        row.addView(meta);
        return row;
    }

    private void bindRow(View row, ChessPuzzles.Puzzle p) {
        LinearLayout box = (LinearLayout) row;
        String toMove = p.winnerWhite ? "White" : "Black";
        ((TextView) box.getChildAt(0)).setText(toMove + " to move — " + p.white + " vs " + p.black);
        ((TextView) box.getChildAt(1)).setText(p.category + " · " + p.event + " · " + p.date);
    }

    private TextView text(float size, int color) {
        TextView view = new TextView(this);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setTypeface(Fonts.current(this));
        view.setIncludeFontPadding(false);
        return view;
    }
}

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Every imported PGN file, one row per distinct source label — the "manage" counterpart to
 *  {@link ChessLibraryActivity}'s flat game list. Tap a row to browse its games (same screen
 *  {@link ChessLibraryActivity} shows from the Board tab's PGN chip); long-press to select one
 *  or more for deletion, which cascades to that source's games and any puzzles generated from
 *  them — a PGN and "the puzzles that came from it" aren't two things a user thinks to
 *  separately clean up. */
public class ChessPgnFilesActivity extends Activity implements SelBarHost {

    private List<ChessLibrary.SourceSummary> all = new ArrayList<>();
    private BaseAdapter adapter;
    private ListView list;
    private View headerRow;
    private SelectionBar selectionBar;
    private final Set<String> selection = new LinkedHashSet<>();
    private boolean selecting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);

        headerRow = UiKit.header(this, "PGN files");
        root.addView(headerRow, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        selectionBar = new SelectionBar(this);
        root.addView(selectionBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        list = new ListView(this);
        list.setDivider(new ColorDrawable(0xFF1C1C1C));
        list.setDividerHeight(1);
        list.setCacheColorHint(Color.BLACK);
        adapter = new BaseAdapter() {
            @Override public int getCount() { return all.size(); }
            @Override public Object getItem(int position) { return all.get(position); }
            @Override public long getItemId(int position) { return position; }
            @Override public View getView(int position, View recycled, ViewGroup parent) {
                View row = recycled != null ? recycled : row();
                bindRow(row, all.get(position));
                return row;
            }
        };
        list.setAdapter(adapter);
        list.setOnItemClickListener((parent, view, position, id) -> onRowTap(all.get(position)));
        list.setOnItemLongClickListener((parent, view, position, id) -> {
            startSelecting(all.get(position).label);
            return true;
        });
        root.addView(list, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);
        refreshHeader();
        load();
    }

    private void load() {
        new Thread(() -> {
            List<ChessLibrary.SourceSummary> loaded = ChessLibrary.listSources(this);
            runOnUiThread(() -> {
                all = loaded;
                adapter.notifyDataSetChanged();
            });
        }).start();
    }

    private void onRowTap(ChessLibrary.SourceSummary source) {
        if (selecting) {
            toggle(source.label);
            return;
        }
        Intent i = new Intent(this, ChessLibraryActivity.class);
        i.putExtra(ChessLibraryActivity.EXTRA_SOURCE_FILTER, source.label);
        startActivity(i);
    }

    private void startSelecting(String label) {
        selecting = true;
        selection.add(label);
        refreshHeader();
        adapter.notifyDataSetChanged();
    }

    private void toggle(String label) {
        if (!selection.remove(label)) selection.add(label);
        if (selection.isEmpty()) selecting = false;
        refreshHeader();
        adapter.notifyDataSetChanged();
    }

    private void refreshHeader() {
        headerRow.setVisibility(selecting ? View.GONE : View.VISIBLE);
        selectionBar.setVisibility(selecting ? View.VISIBLE : View.GONE);
        if (!selecting) return;
        List<String> ids = new ArrayList<>();
        for (ChessLibrary.SourceSummary s : all) ids.add(s.label);
        List<BarAction> actions = new ArrayList<>();
        actions.add(new BarAction("Delete", this::confirmDeleteSelected));
        selectionBar.bind(this, ids, actions);
    }

    private void confirmDeleteSelected() {
        int n = selection.size();
        VaultUi.confirm(this, "Delete " + n + (n == 1 ? " PGN" : " PGNs"),
                "This also removes any puzzles generated from " + (n == 1 ? "it" : "them") + ".",
                "Delete", this::deleteSelected, "Cancel", () -> { });
    }

    private void deleteSelected() {
        Set<String> toDelete = new LinkedHashSet<>(selection);
        exitSelection();
        new Thread(() -> {
            for (String src : toDelete) {
                ChessLibrary.deleteSource(this, src);
                ChessPuzzles.deleteBySource(this, src);
                Config.clearChessPuzzlegenCursor(this, src);
            }
            List<ChessLibrary.SourceSummary> loaded = ChessLibrary.listSources(this);
            runOnUiThread(() -> {
                all = loaded;
                adapter.notifyDataSetChanged();
            });
        }).start();
    }

    @Override public Set<String> selection() { return selection; }

    @Override public void setSelected(Collection<String> ids) {
        selection.clear();
        selection.addAll(ids);
        if (selection.isEmpty()) selecting = false;
        refreshHeader();
        adapter.notifyDataSetChanged();
    }

    @Override public void exitSelection() {
        selecting = false;
        selection.clear();
        refreshHeader();
        adapter.notifyDataSetChanged();
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
        TextView sub = text(12, 0xFF8A8A8A);
        sub.setPadding(0, UiKit.dp(this, 3), 0, 0);
        row.addView(sub);
        TextView puzzles = text(12, 0xFF6E6E6E);
        puzzles.setPadding(0, UiKit.dp(this, 2), 0, 0);
        row.addView(puzzles);
        return row;
    }

    private void bindRow(View row, ChessLibrary.SourceSummary s) {
        LinearLayout box = (LinearLayout) row;
        boolean sel = selection.contains(s.label);
        TextView name = (TextView) box.getChildAt(0);
        name.setText(s.label + "  (" + s.gameCount + (s.gameCount == 1 ? " game)" : " games)")
                + (sel ? "  ✓" : ""));
        name.setTypeface(Fonts.current(this), sel ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        String imported = s.importedAt > 0
                ? android.text.format.DateFormat.getMediumDateFormat(this).format(new java.util.Date(s.importedAt))
                : "";
        ((TextView) box.getChildAt(1)).setText(imported.isEmpty() ? "" : "Imported " + imported);
        int puzzleCount = ChessPuzzles.countBySource(this, s.label);
        ((TextView) box.getChildAt(2)).setText(puzzleCount + (puzzleCount == 1 ? " puzzle generated" : " puzzles generated"));
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

package com.plainphone.app;

import android.app.Activity;
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

/** Every puzzle the generator has found, across every PGN — reached only from the Puzzles
 *  home screen (not from {@link ChessPgnFilesActivity}, which only shows a puzzle count per
 *  source). Long-press to select one or more for deletion; deleting the PGN they came from
 *  (see {@link ChessPgnFilesActivity}) removes them from here too, automatically. */
public class ChessPuzzleListActivity extends Activity implements SelBarHost {

    private List<ChessPuzzles.Puzzle> all = new ArrayList<>();
    private BaseAdapter adapter;
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

        headerRow = UiKit.header(this, "All puzzles");
        root.addView(headerRow, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        selectionBar = new SelectionBar(this);
        root.addView(selectionBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        ListView list = new ListView(this);
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
        list.setOnItemClickListener((parent, view, position, id) -> {
            if (selecting) toggle(all.get(position).id);
        });
        list.setOnItemLongClickListener((parent, view, position, id) -> {
            startSelecting(all.get(position).id);
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
            List<ChessPuzzles.Puzzle> loaded = ChessPuzzles.loadAll(this);
            runOnUiThread(() -> {
                all = loaded;
                adapter.notifyDataSetChanged();
            });
        }).start();
    }

    private void startSelecting(String id) {
        selecting = true;
        selection.add(id);
        refreshHeader();
        adapter.notifyDataSetChanged();
    }

    private void toggle(String id) {
        if (!selection.remove(id)) selection.add(id);
        if (selection.isEmpty()) selecting = false;
        refreshHeader();
        adapter.notifyDataSetChanged();
    }

    private void refreshHeader() {
        headerRow.setVisibility(selecting ? View.GONE : View.VISIBLE);
        selectionBar.setVisibility(selecting ? View.VISIBLE : View.GONE);
        if (!selecting) return;
        List<String> ids = new ArrayList<>();
        for (ChessPuzzles.Puzzle p : all) ids.add(p.id);
        List<BarAction> actions = new ArrayList<>();
        actions.add(new BarAction("Delete", this::confirmDeleteSelected));
        selectionBar.bind(this, ids, actions);
    }

    private void confirmDeleteSelected() {
        int n = selection.size();
        VaultUi.confirm(this, "Delete " + n + (n == 1 ? " puzzle" : " puzzles"), null,
                "Delete", this::deleteSelected, "Cancel", () -> { });
    }

    private void deleteSelected() {
        Set<String> toDelete = new LinkedHashSet<>(selection);
        exitSelection();
        new Thread(() -> {
            ChessPuzzles.deleteByIds(this, toDelete);
            List<ChessPuzzles.Puzzle> loaded = ChessPuzzles.loadAll(this);
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
        return row;
    }

    private void bindRow(View row, ChessPuzzles.Puzzle p) {
        LinearLayout box = (LinearLayout) row;
        boolean sel = selection.contains(p.id);
        TextView title = (TextView) box.getChildAt(0);
        title.setText(ChessBoardView.shortName(p.white) + " vs " + ChessBoardView.shortName(p.black) + (sel ? "  ✓" : ""));
        title.setTypeface(Fonts.current(this), sel ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        ((TextView) box.getChildAt(1)).setText(p.category + " · " + p.gameSrc);
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

package com.plainphone.app;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/** The Library tab: {@link ChessLibraryActivity}'s search-and-browse, embedded inline instead
 *  of as a separate full-screen Activity, plus header icons for import/export and the new
 *  "PGN files" manage screen, plus a Filters dialog (players / results / PGN source — multi-
 *  select, ANDed across categories) on top of the existing free-text search. */
final class ChessLibraryPanel {

    interface Listener {
        void onGameChosen(ChessLibrary.Entry entry);
        void onOpenPgnFiles();
        void onImportPgn();
        void onExportPgn();
    }

    private final Activity host;
    private final Listener listener;
    private final LinearLayout root;
    private final EditText search;
    private final TextView countLine;
    private BaseAdapter adapter;

    private List<ChessLibrary.Entry> all = new ArrayList<>();
    private List<ChessLibrary.Entry> shown = new ArrayList<>();
    private final Handler searchHandler = new Handler();
    private Runnable pendingSearch;

    private final Set<String> filterPlayers = new LinkedHashSet<>();
    private final Set<String> filterResults = new LinkedHashSet<>();
    private final Set<String> filterSources = new LinkedHashSet<>();

    ChessLibraryPanel(Activity host, Listener listener) {
        this.host = host;
        this.listener = listener;

        root = new LinearLayout(host);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);

        LinearLayout headIcons = new LinearLayout(host);
        headIcons.setOrientation(LinearLayout.HORIZONTAL);
        headIcons.setGravity(Gravity.END);
        headIcons.setPadding(UiKit.dp(host, 20), UiKit.dp(host, 14), UiKit.dp(host, 20), 0);
        headIcons.addView(headIcon(R.drawable.ic_chess_folder, "PGN files", v -> listener.onOpenPgnFiles()));
        headIcons.addView(headIcon(R.drawable.ic_chess_import, "Import PGN", v -> listener.onImportPgn()));
        headIcons.addView(headIcon(R.drawable.ic_chess_export, "Export PGN", v -> listener.onExportPgn()));
        root.addView(headIcons);

        // The filter button lives inside the search box itself (trailing icon), not as its
        // own header row — one search-and-filter control instead of two separate ones.
        LinearLayout searchBox = new LinearLayout(host);
        searchBox.setOrientation(LinearLayout.HORIZONTAL);
        searchBox.setGravity(Gravity.CENTER_VERTICAL);
        searchBox.setBackground(UiKit.rounded(host, Color.BLACK, Color.WHITE, 2f, UiKit.R_SM));
        search = new EditText(host);
        search.setBackground(null);
        search.setTextColor(Color.WHITE);
        search.setTypeface(Fonts.current(host));
        search.setHint("Search players, event…");
        search.setHintTextColor(0xFF6E6E6E);
        search.setSingleLine(true);
        search.setPadding(UiKit.dp(host, 16), UiKit.dp(host, 14), UiKit.dp(host, 8), UiKit.dp(host, 14));
        searchBox.addView(search, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        android.widget.ImageView filterIcon = new android.widget.ImageView(host);
        filterIcon.setImageDrawable(host.getResources().getDrawable(R.drawable.ic_chess_filter, host.getTheme()));
        filterIcon.setScaleType(android.widget.ImageView.ScaleType.CENTER_INSIDE);
        filterIcon.setContentDescription("Filters");
        filterIcon.setOnClickListener(v -> openFilters());
        LinearLayout.LayoutParams filterLp = new LinearLayout.LayoutParams(UiKit.dp(host, 40), UiKit.dp(host, 40));
        filterLp.rightMargin = UiKit.dp(host, 6);
        searchBox.addView(filterIcon, filterLp);
        LinearLayout searchWrap = new LinearLayout(host);
        searchWrap.setPadding(UiKit.dp(host, 20), UiKit.dp(host, 10), UiKit.dp(host, 20), UiKit.dp(host, 8));
        searchWrap.addView(searchBox, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(searchWrap);

        countLine = new TextView(host);
        countLine.setTextColor(0xFF6E6E6E);
        countLine.setTextSize(12);
        countLine.setTypeface(Fonts.current(host));
        countLine.setPadding(UiKit.dp(host, 24), 0, UiKit.dp(host, 24), UiKit.dp(host, 12));
        root.addView(countLine);

        ListView list = new ListView(host);
        list.setDivider(new ColorDrawable(0xFF1C1C1C));
        list.setDividerHeight(1);
        list.setCacheColorHint(Color.BLACK);
        adapter = new BaseAdapter() {
            @Override public int getCount() { return shown.size(); }
            @Override public Object getItem(int position) { return shown.get(position); }
            @Override public long getItemId(int position) { return position; }
            @Override public View getView(int position, View recycled, ViewGroup parent) {
                View row = recycled != null ? recycled : row();
                bindRow(row, shown.get(position));
                return row;
            }
        };
        list.setAdapter(adapter);
        list.setOnItemClickListener((parent, view, position, id) -> listener.onGameChosen(shown.get(position)));
        root.addView(list, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { scheduleFilter(); }
        });
    }

    View view() { return root; }

    /** Re-reads the library from disk — call whenever the Library tab becomes visible again,
     *  not just once at construction, since a PGN import/delete elsewhere may have changed it. */
    void refresh() {
        countLine.setText("Loading…");
        new Thread(() -> {
            List<ChessLibrary.Entry> loaded = ChessLibrary.loadAll(host);
            host.runOnUiThread(() -> {
                all = loaded;
                applyFilter();
            });
        }).start();
    }

    private void scheduleFilter() {
        if (pendingSearch != null) searchHandler.removeCallbacks(pendingSearch);
        pendingSearch = this::applyFilter;
        searchHandler.postDelayed(pendingSearch, 150);
    }

    // Bumped on every applyFilter() call — a slow background pass for a stale query/filter
    // set can finish after a newer one started (fast typing, or Filters closing right after
    // a keystroke); this lets it recognize itself as stale and drop its result instead of
    // momentarily flashing outdated rows.
    private int filterGeneration;

    private void applyFilter() {
        int gen = ++filterGeneration;
        String q = search.getText().toString().trim().toLowerCase(Locale.ROOT);
        // A big library (tens of thousands of games) makes this a real O(n) pass — off the
        // UI thread, same as the initial load, so typing in the search box never stutters.
        new Thread(() -> {
            List<ChessLibrary.Entry> matched = new ArrayList<>();
            for (ChessLibrary.Entry e : all) {
                if (!q.isEmpty() && !e.white.toLowerCase(Locale.ROOT).contains(q)
                        && !e.black.toLowerCase(Locale.ROOT).contains(q)
                        && !e.event.toLowerCase(Locale.ROOT).contains(q)) continue;
                if (!filterPlayers.isEmpty() && !filterPlayers.contains(e.white) && !filterPlayers.contains(e.black)) continue;
                if (!filterResults.isEmpty() && !filterResults.contains(e.result)) continue;
                if (!filterSources.isEmpty() && !filterSources.contains(e.src)) continue;
                matched.add(e);
            }
            host.runOnUiThread(() -> {
                if (gen != filterGeneration) return;
                shown = matched;
                adapter.notifyDataSetChanged();
                int activeFilters = (filterPlayers.isEmpty() ? 0 : 1) + (filterResults.isEmpty() ? 0 : 1) + (filterSources.isEmpty() ? 0 : 1);
                countLine.setText(shown.size() + " of " + all.size() + " games"
                        + (activeFilters > 0 ? " · " + activeFilters + " filter" + (activeFilters == 1 ? "" : "s") : ""));
            });
        }).start();
    }

    // --- Filters dialog -----------------------------------------------------

    private void openFilters() {
        countLine.setText("Loading filters…");
        new Thread(() -> {
            Set<String> players = new TreeSet<>();
            Set<String> results = new TreeSet<>();
            Set<String> sources = new TreeSet<>();
            for (ChessLibrary.Entry e : all) {
                players.add(e.white);
                players.add(e.black);
                results.add(e.result);
                sources.add(e.src);
            }
            FilterTab[] tabs = {
                    new FilterTab("Players", players, filterPlayers),
                    new FilterTab("Results", results, filterResults),
                    new FilterTab("PGN", sources, filterSources),
            };
            host.runOnUiThread(() -> {
                applyFilter(); // restore countLine's real text before the dialog opens on top
                showFilterDialog(tabs);
            });
        }).start();
    }

    private static final class FilterTab {
        final String label;
        final Set<String> values;
        final Set<String> selected;
        FilterTab(String label, Set<String> values, Set<String> selected) {
            this.label = label; this.values = values; this.selected = selected;
        }
    }

    private void showFilterDialog(FilterTab[] tabs) {
        LinearLayout box = new LinearLayout(host);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackground(UiKit.dialogBackground(host));
        UiKit.clipRounded(host, box, UiKit.R_MD);
        box.setPadding(2, 32, 2, UiKit.dp(host, UiKit.R_MD));

        LinearLayout titleRow = new LinearLayout(host);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = UiKit.dialogTitle(host, "Filters");
        titleRow.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView clear = UiKit.dialogTitle(host, "Clear");
        titleRow.addView(clear);
        box.addView(titleRow);

        EditText filterSearch = new EditText(host);
        filterSearch.setBackground(UiKit.rounded(host, Color.BLACK, Color.WHITE, 2f, UiKit.R_SM));
        filterSearch.setTextColor(Color.WHITE);
        filterSearch.setTypeface(Fonts.current(host));
        filterSearch.setHint("Search");
        filterSearch.setHintTextColor(0xFF6E6E6E);
        filterSearch.setSingleLine(true);
        filterSearch.setTextSize(14);
        filterSearch.setPadding(UiKit.dp(host, 14), UiKit.dp(host, 10), UiKit.dp(host, 14), UiKit.dp(host, 10));
        LinearLayout.LayoutParams filterSearchLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        filterSearchLp.leftMargin = UiKit.dp(host, 16);
        filterSearchLp.rightMargin = UiKit.dp(host, 16);
        filterSearchLp.bottomMargin = UiKit.dp(host, 10);
        box.addView(filterSearch, filterSearchLp);

        LinearLayout tabRow = new LinearLayout(host);
        tabRow.setOrientation(LinearLayout.HORIZONTAL);
        tabRow.setPadding(UiKit.dp(host, 16), 0, UiKit.dp(host, 16), UiKit.dp(host, 8));
        LinearLayout listWrap = new LinearLayout(host);
        listWrap.setOrientation(LinearLayout.VERTICAL);
        android.widget.ScrollView scroller = new android.widget.ScrollView(host);
        scroller.addView(listWrap, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        scroller.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, UiKit.dp(host, 320)));

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(host).setView(box).create();
        UiKit.clearDialogChrome(dialog);

        int[] activeTab = {0};
        Runnable[] renderList = new Runnable[1];
        Runnable[] renderTabs = new Runnable[1];
        renderList[0] = () -> {
            listWrap.removeAllViews();
            FilterTab t = tabs[activeTab[0]];
            String q = filterSearch.getText().toString().trim().toLowerCase(Locale.ROOT);
            for (String value : t.values) {
                if (value == null || value.isEmpty() || value.equals("?") || value.equals("*")) continue;
                if (!q.isEmpty() && !value.toLowerCase(Locale.ROOT).contains(q)) continue;
                boolean sel = t.selected.contains(value);
                TextView row = new TextView(host);
                row.setText((sel ? "[x] " : "[ ] ") + value);
                row.setTextColor(sel ? Color.WHITE : 0xFFB7B7B7);
                row.setTypeface(Fonts.current(host), sel ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
                row.setTextSize(15);
                row.setPadding(UiKit.dp(host, 16), UiKit.dp(host, 10), UiKit.dp(host, 16), UiKit.dp(host, 10));
                row.setOnClickListener(v -> {
                    if (!t.selected.remove(value)) t.selected.add(value);
                    renderList[0].run();
                });
                listWrap.addView(row);
            }
        };
        renderTabs[0] = () -> {
            tabRow.removeAllViews();
            for (int i = 0; i < tabs.length; i++) {
                final int idx = i;
                boolean on = idx == activeTab[0];
                TextView t = new TextView(host);
                t.setText(tabs[i].label);
                t.setTypeface(Fonts.current(host));
                t.setTextSize(13);
                t.setGravity(Gravity.CENTER);
                t.setPadding(0, UiKit.dp(host, 8), 0, UiKit.dp(host, 8));
                t.setBackground(on
                        ? UiKit.rounded(host, Color.WHITE, 0, 0f, UiKit.R_SM)
                        : UiKit.rounded(host, Color.BLACK, 0xFF2C2C2C, 2f, UiKit.R_SM));
                t.setTextColor(on ? Color.BLACK : Color.WHITE);
                t.setOnClickListener(v -> { activeTab[0] = idx; renderTabs[0].run(); renderList[0].run(); });
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                if (idx > 0) lp.leftMargin = UiKit.dp(host, 6);
                tabRow.addView(t, lp);
            }
        };
        renderTabs[0].run();
        renderList[0].run();
        filterSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { renderList[0].run(); }
        });
        clear.setOnClickListener(v -> {
            for (FilterTab t : tabs) t.selected.clear();
            renderList[0].run();
        });

        box.addView(tabRow);
        box.addView(scroller);
        box.addView(applyRow(dialog));

        dialog.setOnDismissListener(d -> applyFilter());
        dialog.show();
        UiKit.unboxDialog(box);
        if (dialog.getWindow() != null) {
            android.view.WindowManager.LayoutParams p = dialog.getWindow().getAttributes();
            p.width = (int) (host.getResources().getDisplayMetrics().widthPixels * 0.85);
            dialog.getWindow().setAttributes(p);
        }
    }

    private View applyRow(android.app.AlertDialog dialog) {
        TextView row = new TextView(host);
        row.setText("Done");
        row.setTextColor(Color.WHITE);
        row.setTextSize(18);
        row.setTypeface(Fonts.current(host));
        row.setGravity(Gravity.CENTER);
        row.setPadding(48, 28, 48, 28);
        StateListDrawable bg = new StateListDrawable();
        bg.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Color.DKGRAY));
        bg.addState(new int[]{}, new ColorDrawable(Color.BLACK));
        row.setBackground(bg);
        // Dismissing (any way — this row, back button, outside tap) applies the filter via
        // the dialog's onDismissListener set in showFilterDialog; this row just closes it.
        row.setOnClickListener(v -> dialog.dismiss());
        return row;
    }

    private View headIcon(int drawableRes, String description, View.OnClickListener onClick) {
        android.widget.ImageView icon = new android.widget.ImageView(host);
        icon.setImageDrawable(host.getResources().getDrawable(drawableRes, host.getTheme()));
        icon.setScaleType(android.widget.ImageView.ScaleType.CENTER_INSIDE);
        icon.setContentDescription(description);
        icon.setBackground(UiKit.pressable(host, Color.BLACK, Color.DKGRAY, 0xFF262626, 2f, UiKit.R_SM));
        icon.setOnClickListener(onClick);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(UiKit.dp(host, 36), UiKit.dp(host, 36));
        lp.leftMargin = UiKit.dp(host, 6);
        icon.setLayoutParams(lp);
        return icon;
    }

    private View row() {
        LinearLayout row = new LinearLayout(host);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(UiKit.dp(host, 24), UiKit.dp(host, 12), UiKit.dp(host, 24), UiKit.dp(host, 12));
        StateListDrawable bg = new StateListDrawable();
        bg.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Color.DKGRAY));
        bg.addState(new int[]{}, new ColorDrawable(Color.BLACK));
        row.setBackground(bg);
        LinearLayout lines = new LinearLayout(host);
        lines.setOrientation(LinearLayout.VERTICAL);
        lines.addView(text(16, Color.WHITE));
        TextView meta = text(12, 0xFF8A8A8A);
        meta.setPadding(0, UiKit.dp(host, 3), 0, 0);
        lines.addView(meta);
        row.addView(lines, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView result = text(13, 0xFF8A8A8A);
        result.setPadding(UiKit.dp(host, 10), 0, 0, 0);
        row.addView(result);
        return row;
    }

    private void bindRow(View row, ChessLibrary.Entry e) {
        LinearLayout box = (LinearLayout) row;
        LinearLayout lines = (LinearLayout) box.getChildAt(0);
        ((TextView) lines.getChildAt(0)).setText(e.white + " vs " + e.black);
        ((TextView) lines.getChildAt(1)).setText(e.event + " · " + e.date);
        ((TextView) box.getChildAt(1)).setText(e.result);
    }

    private TextView text(float size, int color) {
        TextView view = new TextView(host);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setTypeface(Fonts.current(host));
        view.setIncludeFontPadding(false);
        return view;
    }
}

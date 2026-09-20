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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
        /** "Export this PGN" from a source group's options menu — every game under
         *  {@code source}, not just whatever's on the board. */
        void onExportSource(String source);
        /** "Export" from the multi-select toolbar — an arbitrary set of games, possibly
         *  spanning several PGNs, as opposed to {@link #onExportSource}'s whole-PGN export. */
        void onExportSelected(Set<String> ids);
    }

    /** One row in the grouped (unfiltered) list — a collapsible PGN source header, standing
     *  in for that source's games in {@link #rows} until it's expanded. Never appears when a
     *  search/filter is active (see {@link #rebuildRows}): a query result is a flat list of
     *  {@link ChessLibrary.Entry}, same as before grouping existed. */
    private static final class HeaderRow {
        final String source;
        final int count;
        final long importedAt;
        HeaderRow(String source, int count, long importedAt) {
            this.source = source; this.count = count; this.importedAt = importedAt;
        }
    }

    private final Activity host;
    private final Listener listener;
    private final LinearLayout root;
    private final EditText search;
    private final TextView countLine;
    private BaseAdapter adapter;

    private View jobCard;
    private TextView jobTitle, jobStat, jobStop;

    private List<ChessLibrary.Entry> all = new ArrayList<>();
    private List<ChessLibrary.Entry> shown = new ArrayList<>();
    // Object: either a HeaderRow or a ChessLibrary.Entry — see rebuildRows().
    private List<Object> rows = new ArrayList<>();
    private final Set<String> expandedSources = new LinkedHashSet<>();
    private final Handler searchHandler = new Handler();
    private Runnable pendingSearch;

    private final Set<String> filterPlayers = new LinkedHashSet<>();
    private final Set<String> filterResults = new LinkedHashSet<>();
    private final Set<String> filterSources = new LinkedHashSet<>();

    // --- multi-select (move / export / delete games) ---------------------------------
    private boolean selectMode = false;
    private final Set<String> selectedIds = new LinkedHashSet<>();
    private View selectBar;
    private TextView selectCountLabel;

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
        headIcons.addView(headIcon(R.drawable.ic_chess_new_pgn, "New PGN", v -> promptCreatePgn()));
        root.addView(headIcons);

        root.addView(buildJobCard());

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
            @Override public int getCount() { return rows.size(); }
            @Override public Object getItem(int position) { return rows.get(position); }
            @Override public long getItemId(int position) { return position; }
            @Override public int getViewTypeCount() { return 2; }
            @Override public int getItemViewType(int position) { return rows.get(position) instanceof HeaderRow ? 0 : 1; }
            @Override public View getView(int position, View recycled, ViewGroup parent) {
                Object item = rows.get(position);
                if (item instanceof HeaderRow) {
                    View row = recycled != null ? recycled : headerRow();
                    bindHeaderRow(row, (HeaderRow) item);
                    return row;
                }
                View row = recycled != null ? recycled : row();
                bindRow(row, (ChessLibrary.Entry) item);
                return row;
            }
        };
        list.setAdapter(adapter);
        // A header tap toggles that source's collapse state; a game tap re-fetches its full
        // entry (moves included) by id, off the UI thread — rows/shown/all only ever carry
        // the lightweight rows loadAll() returns (see ChessLibrary.loadAll), never the moves.
        list.setOnItemClickListener((parent, view, position, id) -> {
            Object item = rows.get(position);
            if (item instanceof HeaderRow) {
                String source = ((HeaderRow) item).source;
                if (!expandedSources.remove(source)) expandedSources.add(source);
                rebuildRows(true); // a HeaderRow only ever appears in the grouped view
                return;
            }
            ChessLibrary.Entry entry = (ChessLibrary.Entry) item;
            if (selectMode) { toggleSelected(entry.id); return; }
            new Thread(() -> {
                ChessLibrary.Entry full = ChessLibrary.findById(host, entry.id);
                if (full != null) host.runOnUiThread(() -> listener.onGameChosen(full));
            }).start();
        });
        // Long-press enters multi-select (for move/export/delete): a game row selects just
        // itself, a PGN header selects every game currently shown under it.
        list.setOnItemLongClickListener((parent, view, position, id) -> {
            Object item = rows.get(position);
            enterSelectMode();
            if (item instanceof HeaderRow) {
                String source = ((HeaderRow) item).source;
                for (ChessLibrary.Entry e : shown) if (e.src.equals(source)) selectedIds.add(e.id);
                expandedSources.add(source);
                rebuildRows(true);
            } else {
                selectedIds.add(((ChessLibrary.Entry) item).id);
                adapter.notifyDataSetChanged();
            }
            updateSelectBar();
            return true;
        });
        root.addView(list, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        selectBar = buildSelectBar();
        root.addView(selectBar);

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { scheduleFilter(); }
        });

        ChessImportJobs.addListener(this::refreshJobCard);
    }

    View view() { return root; }

    /** Re-reads the library from disk — call whenever the Library tab becomes visible again,
     *  not just once at construction, since a PGN import/delete elsewhere may have changed it. */
    void refresh() {
        refreshJobCard();
        countLine.setText("Loading…");
        new Thread(() -> {
            List<ChessLibrary.Entry> loaded = ChessLibrary.loadAll(host);
            host.runOnUiThread(() -> {
                all = loaded;
                applyFilter();
            });
        }).start();
    }

    // --- import job card ------------------------------------------------------

    /** Same card/Stop/"done" pattern as the Puzzles tab's own job card (see
     *  {@code ChessPuzzlesPanel}) — {@link ChessImportJobs#snapshot} (not
     *  {@link JobQueue}/{@code isRunning}) drives visibility, so a finished import gets to
     *  hold its "done" state on screen for a few seconds instead of vanishing the instant the
     *  file's last game is read. */
    private View buildJobCard() {
        LinearLayout card = new LinearLayout(host);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(UiKit.rounded(host, Color.BLACK, 0xFF262626, 2f, UiKit.R_MD));
        card.setPadding(UiKit.dp(host, 16), UiKit.dp(host, 14), UiKit.dp(host, 16), UiKit.dp(host, 14));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(UiKit.dp(host, 20), UiKit.dp(host, 12), UiKit.dp(host, 20), 0);

        jobTitle = new TextView(host);
        jobTitle.setText("Importing PGN");
        jobTitle.setTextColor(Color.WHITE);
        jobTitle.setTypeface(Fonts.current(host));
        jobTitle.setTextSize(13);
        card.addView(jobTitle);

        jobStat = new TextView(host);
        jobStat.setTextColor(0xFF8A8A8A);
        jobStat.setTypeface(Fonts.current(host));
        jobStat.setTextSize(12);
        jobStat.setPadding(0, UiKit.dp(host, 6), 0, 0);
        card.addView(jobStat);

        jobStop = new TextView(host);
        jobStop.setText("Stop");
        jobStop.setTextColor(Color.WHITE);
        jobStop.setTypeface(Fonts.current(host));
        jobStop.setTextSize(12);
        jobStop.setPadding(0, UiKit.dp(host, 10), 0, 0);
        jobStop.setOnClickListener(v -> { ChessImportJobs.stop(host); refreshJobCard(); });
        card.addView(jobStop);

        jobCard = card;
        card.setLayoutParams(lp);
        refreshJobCard();
        return card;
    }

    private void refreshJobCard() {
        ChessImportJobs.Snapshot snap = ChessImportJobs.snapshot;
        host.runOnUiThread(() -> {
            jobCard.setVisibility(snap != null ? View.VISIBLE : View.GONE);
            if (snap == null) return;
            jobTitle.setText(snap.done ? "Import complete" : "Importing " + snap.sourceLabel);
            jobStat.setText(snap.done ? ChessImportJobs.doneLabel(host) : ChessImportJobs.activeLabel(host));
            jobStop.setVisibility(snap.done ? View.GONE : View.VISIBLE);
        });
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
        boolean noQuery = q.isEmpty();
        boolean noFilters = filterPlayers.isEmpty() && filterResults.isEmpty() && filterSources.isEmpty();
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
                // Grouped-by-PGN headers only make sense browsing the whole library — a
                // search/filter result is a flat list of matches, same as before grouping
                // existed (a header for a source with 1 match out of 3,000 games would be
                // pure noise, and "how many of this source's games matched" isn't the same
                // number as HeaderRow's own count anyway).
                rebuildRows(noQuery && noFilters);
                int activeFilters = (filterPlayers.isEmpty() ? 0 : 1) + (filterResults.isEmpty() ? 0 : 1) + (filterSources.isEmpty() ? 0 : 1);
                countLine.setText(shown.size() + " of " + all.size() + " games"
                        + (activeFilters > 0 ? " · " + activeFilters + " filter" + (activeFilters == 1 ? "" : "s") : ""));
            });
        }).start();
    }

    /** Rebuilds {@link #rows} from {@link #shown} — grouped into per-source {@link HeaderRow}s
     *  (each followed by its games only if {@link #expandedSources} has it) when
     *  {@code grouped}, otherwise the flat entry list as-is. Call directly (no new background
     *  pass needed) after something that only changes grouping/expand state, not the
     *  underlying data — a header tap, or a rename/delete's {@link #refresh}. */
    private void rebuildRows(boolean grouped) {
        List<Object> out = new ArrayList<>();
        if (!grouped) {
            out.addAll(shown);
        } else {
            Map<String, List<ChessLibrary.Entry>> bySource = new LinkedHashMap<>();
            for (ChessLibrary.Entry e : shown) bySource.computeIfAbsent(e.src, k -> new ArrayList<>()).add(e);
            for (Map.Entry<String, List<ChessLibrary.Entry>> group : bySource.entrySet()) {
                String source = group.getKey();
                List<ChessLibrary.Entry> games = group.getValue();
                long importedAt = Long.MAX_VALUE;
                for (ChessLibrary.Entry e : games) if (e.importedAt > 0) importedAt = Math.min(importedAt, e.importedAt);
                if (importedAt == Long.MAX_VALUE) importedAt = 0;
                out.add(new HeaderRow(source, games.size(), importedAt));
                if (expandedSources.contains(source)) out.addAll(games);
            }
        }
        rows = out;
        adapter.notifyDataSetChanged();
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

        android.widget.FrameLayout scrim = UiKit.wrapScrim(host, box, 0.85f);
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(
                host, R.style.Theme_PlainPhone_RoundedDialog).setView(scrim).create();

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
        UiKit.finishCentered(dialog, scrim);
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
        String marker = selectMode ? (selectedIds.contains(e.id) ? "[x] " : "[ ] ") : "";
        ((TextView) lines.getChildAt(0)).setText(marker + ChessBoardView.shortName(e.white) + " vs " + ChessBoardView.shortName(e.black));
        ((TextView) lines.getChildAt(1)).setText(e.event + " · " + e.date);
        ((TextView) box.getChildAt(1)).setText(e.result);
    }

    // --- multi-select: move / export / delete games ---------------------------------

    private void enterSelectMode() {
        if (selectMode) return;
        selectMode = true;
        selectedIds.clear();
        selectBar.setVisibility(View.VISIBLE);
    }

    private void exitSelectMode() {
        selectMode = false;
        selectedIds.clear();
        selectBar.setVisibility(View.GONE);
        adapter.notifyDataSetChanged();
    }

    private void toggleSelected(String id) {
        if (!selectedIds.remove(id)) selectedIds.add(id);
        if (selectedIds.isEmpty()) exitSelectMode();
        else { updateSelectBar(); adapter.notifyDataSetChanged(); }
    }

    private void updateSelectBar() {
        selectCountLabel.setText(selectedIds.size() + " selected");
    }

    private View buildSelectBar() {
        LinearLayout bar = new LinearLayout(host);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setBackgroundColor(0xFF0A0A0A);
        bar.setPadding(UiKit.dp(host, 20), UiKit.dp(host, 12), UiKit.dp(host, 20), UiKit.dp(host, 12));

        selectCountLabel = text(14, Color.WHITE);
        bar.addView(selectCountLabel, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        bar.addView(selectBarAction("Move", Color.WHITE, v -> promptMoveSelected()));
        bar.addView(selectBarAction("Export", Color.WHITE, v -> exportSelected()));
        bar.addView(selectBarAction("Delete", 0xFFE05C5C, v -> confirmDeleteSelected()));
        bar.addView(selectBarAction("Cancel", 0xFF8A8A8A, v -> exitSelectMode()));

        bar.setVisibility(View.GONE);
        return bar;
    }

    private TextView selectBarAction(String label, int color, View.OnClickListener onClick) {
        TextView t = text(14, color);
        t.setText(label);
        t.setPadding(UiKit.dp(host, 14), UiKit.dp(host, 8), UiKit.dp(host, 14), UiKit.dp(host, 8));
        t.setOnClickListener(onClick);
        return t;
    }

    private void promptMoveSelected() {
        Set<String> ids = new LinkedHashSet<>(selectedIds);
        if (ids.isEmpty()) return;
        new Thread(() -> {
            List<ChessLibrary.SourceSummary> sources = ChessLibrary.listSources(host);
            host.runOnUiThread(() -> showMoveDialog(ids, sources));
        }).start();
    }

    private void showMoveDialog(Set<String> ids, List<ChessLibrary.SourceSummary> sources) {
        LinearLayout box = new LinearLayout(host);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackground(UiKit.dialogBackground(host));
        UiKit.clipRounded(host, box, UiKit.R_MD);
        box.setPadding(2, 32, 2, UiKit.dp(host, UiKit.R_MD));
        box.addView(UiKit.dialogTitle(host, "Move " + ids.size() + (ids.size() == 1 ? " game to" : " games to")));

        android.widget.FrameLayout scrim = UiKit.wrapScrim(host, box, 0.85f);
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(
                host, R.style.Theme_PlainPhone_RoundedDialog).setView(scrim).create();

        for (ChessLibrary.SourceSummary s : sources) {
            box.addView(optionRow(s.label + " (" + s.gameCount + ")", Color.WHITE, v -> {
                dialog.dismiss();
                moveSelectedTo(ids, s.label);
            }));
        }
        box.addView(optionRow("+ New PGN…", 0xFF4A9EFF, v -> {
            dialog.dismiss();
            UiKit.textPrompt(host, "New PGN", "", "Create", true, name -> {
                new Thread(() -> ChessLibrary.createSource(host, name)).start();
                moveSelectedTo(ids, name);
            });
        }));
        box.addView(optionRow("Cancel", 0xFF8A8A8A, v -> dialog.dismiss()));

        UiKit.finishCentered(dialog, scrim);
    }

    private void moveSelectedTo(Set<String> ids, String toSource) {
        new Thread(() -> {
            ChessLibrary.moveGames(host, ids, toSource);
            host.runOnUiThread(() -> { exitSelectMode(); refresh(); });
        }).start();
    }

    private void exportSelected() {
        Set<String> ids = new LinkedHashSet<>(selectedIds);
        if (ids.isEmpty()) return;
        exitSelectMode();
        listener.onExportSelected(ids);
    }

    private void confirmDeleteSelected() {
        Set<String> ids = new LinkedHashSet<>(selectedIds);
        if (ids.isEmpty()) return;
        VaultUi.confirm(host, "Delete " + ids.size() + (ids.size() == 1 ? " game?" : " games?"), null,
                "Delete", () -> new Thread(() -> {
                    ChessLibrary.deleteGames(host, ids);
                    host.runOnUiThread(() -> { exitSelectMode(); refresh(); });
                }).start(),
                "Cancel", () -> { });
    }

    private void promptCreatePgn() {
        LinearLayout box = new LinearLayout(host);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackground(UiKit.dialogBackground(host));
        UiKit.clipRounded(host, box, UiKit.R_MD);
        box.setPadding(2, 32, 2, UiKit.dp(host, UiKit.R_MD));
        box.addView(UiKit.dialogTitle(host, "New PGN"));

        EditText input = new EditText(host);
        input.setBackground(UiKit.rounded(host, Color.BLACK, Color.WHITE, 2f, UiKit.R_SM));
        input.setTextColor(Color.WHITE);
        input.setTypeface(Fonts.current(host));
        input.setSingleLine(true);
        input.setPadding(UiKit.dp(host, 14), UiKit.dp(host, 10), UiKit.dp(host, 14), UiKit.dp(host, 10));
        LinearLayout.LayoutParams inputLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        inputLp.leftMargin = UiKit.dp(host, 16);
        inputLp.rightMargin = UiKit.dp(host, 16);
        inputLp.bottomMargin = UiKit.dp(host, 10);
        box.addView(input, inputLp);

        android.widget.FrameLayout scrim = UiKit.wrapScrim(host, box, 0.85f);
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(
                host, R.style.Theme_PlainPhone_RoundedDialog).setView(scrim).create();

        box.addView(optionRow("Create", Color.WHITE, v -> {
            String name = input.getText().toString().trim();
            dialog.dismiss();
            if (name.isEmpty()) return;
            new Thread(() -> {
                ChessLibrary.createSource(host, name);
                host.runOnUiThread(this::refresh);
            }).start();
        }));
        box.addView(optionRow("Cancel", 0xFF8A8A8A, v -> dialog.dismiss()));

        UiKit.finishCentered(dialog, scrim);
    }

    // --- PGN group headers ----------------------------------------------------

    /** Outer vertical wrapper — [top border, the actual horizontal content row, bottom
     *  border] — so the header visually separates from the plain game rows above and below
     *  it, not just via its own fill color. {@link #bindHeaderRow} reaches through to the
     *  content row (index 1) for chevron/lines/kebab. */
    private View headerRow() {
        LinearLayout wrap = new LinearLayout(host);
        wrap.setOrientation(LinearLayout.VERTICAL);

        View topBorder = new View(host);
        topBorder.setBackgroundColor(0xFF333333);
        wrap.addView(topBorder, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));

        LinearLayout row = new LinearLayout(host);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackgroundColor(0xFF1C1C1C);
        row.setPadding(UiKit.dp(host, 20), UiKit.dp(host, 14), UiKit.dp(host, 10), UiKit.dp(host, 14));

        TextView chevron = text(12, 0xFF8A8A8A);
        row.addView(chevron, new LinearLayout.LayoutParams(UiKit.dp(host, 16), ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout lines = new LinearLayout(host);
        lines.setOrientation(LinearLayout.VERTICAL);
        TextView title = text(15, Color.WHITE);
        title.setTypeface(Fonts.current(host), android.graphics.Typeface.BOLD);
        lines.addView(title);
        TextView meta = text(11, 0xFF6E6E6E);
        meta.setPadding(0, UiKit.dp(host, 3), 0, 0);
        lines.addView(meta);
        row.addView(lines, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView kebab = text(18, 0xFF8A8A8A);
        kebab.setText("⋮");
        kebab.setGravity(Gravity.CENTER);
        kebab.setBackground(UiKit.pressable(host, 0xFF1C1C1C, Color.DKGRAY, 0, 0f, UiKit.R_SM));
        row.addView(kebab, new LinearLayout.LayoutParams(UiKit.dp(host, 40), UiKit.dp(host, 40)));

        wrap.addView(row);

        View bottomBorder = new View(host);
        bottomBorder.setBackgroundColor(0xFF333333);
        wrap.addView(bottomBorder, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));

        return wrap;
    }

    private void bindHeaderRow(View row, HeaderRow h) {
        LinearLayout content = (LinearLayout) ((LinearLayout) row).getChildAt(1);
        ((TextView) content.getChildAt(0)).setText(expandedSources.contains(h.source) ? "v" : ">");
        LinearLayout lines = (LinearLayout) content.getChildAt(1);
        ((TextView) lines.getChildAt(0)).setText(h.source);
        String imported = h.importedAt > 0
                ? " · imported " + new java.text.SimpleDateFormat("MMM d", Locale.US).format(new java.util.Date(h.importedAt))
                : "";
        ((TextView) lines.getChildAt(1)).setText(h.count + (h.count == 1 ? " game" : " games") + imported);
        content.getChildAt(2).setOnClickListener(v -> openSourceOptions(h.source));
    }

    /** The "⋮" bottom-sheet-style popup for one PGN source: rename it, export just its games,
     *  generate puzzles from just it, or delete it (with its puzzles) entirely. */
    private void openSourceOptions(String source) {
        LinearLayout box = new LinearLayout(host);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackground(UiKit.dialogBackground(host));
        UiKit.clipRounded(host, box, UiKit.R_MD);
        box.setPadding(2, 32, 2, UiKit.dp(host, UiKit.R_MD));
        box.addView(UiKit.dialogTitle(host, source));

        android.widget.FrameLayout scrim = UiKit.wrapScrim(host, box, 0.85f);
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(
                host, R.style.Theme_PlainPhone_RoundedDialog).setView(scrim).create();

        box.addView(optionRow("Rename", Color.WHITE, v -> { dialog.dismiss(); promptRename(source); }));
        box.addView(optionRow("Export this PGN", Color.WHITE, v -> { dialog.dismiss(); listener.onExportSource(source); }));
        box.addView(optionRow("Generate puzzles", Color.WHITE, v -> {
            dialog.dismiss();
            ChessPuzzleJobs.start(host, source);
            android.widget.Toast.makeText(host, "Generating puzzles — " + source, android.widget.Toast.LENGTH_SHORT).show();
        }));
        box.addView(optionRow("Delete this PGN", 0xFFE05C5C, v -> { dialog.dismiss(); confirmDeleteSource(source); }));
        box.addView(optionRow("Cancel", 0xFF8A8A8A, v -> dialog.dismiss()));

        UiKit.finishCentered(dialog, scrim);
    }

    private View optionRow(String label, int color, View.OnClickListener onClick) {
        TextView row = text(17, color);
        row.setText(label);
        row.setPadding(48, 28, 48, 28);
        StateListDrawable bg = new StateListDrawable();
        bg.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Color.DKGRAY));
        bg.addState(new int[]{}, new ColorDrawable(Color.BLACK));
        row.setBackground(bg);
        row.setOnClickListener(onClick);
        return row;
    }

    private void promptRename(String source) {
        LinearLayout box = new LinearLayout(host);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackground(UiKit.dialogBackground(host));
        UiKit.clipRounded(host, box, UiKit.R_MD);
        box.setPadding(2, 32, 2, UiKit.dp(host, UiKit.R_MD));
        box.addView(UiKit.dialogTitle(host, "Rename PGN"));

        EditText input = new EditText(host);
        input.setBackground(UiKit.rounded(host, Color.BLACK, Color.WHITE, 2f, UiKit.R_SM));
        input.setTextColor(Color.WHITE);
        input.setTypeface(Fonts.current(host));
        input.setSingleLine(true);
        input.setText(source);
        input.setSelection(source.length());
        input.setPadding(UiKit.dp(host, 14), UiKit.dp(host, 10), UiKit.dp(host, 14), UiKit.dp(host, 10));
        LinearLayout.LayoutParams inputLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        inputLp.leftMargin = UiKit.dp(host, 16);
        inputLp.rightMargin = UiKit.dp(host, 16);
        inputLp.bottomMargin = UiKit.dp(host, 10);
        box.addView(input, inputLp);

        android.widget.FrameLayout scrim = UiKit.wrapScrim(host, box, 0.85f);
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(
                host, R.style.Theme_PlainPhone_RoundedDialog).setView(scrim).create();

        box.addView(optionRow("Rename", Color.WHITE, v -> {
            String newName = input.getText().toString().trim();
            dialog.dismiss();
            if (newName.isEmpty() || newName.equals(source)) return;
            new Thread(() -> {
                ChessLibrary.renameSource(host, source, newName);
                ChessPuzzles.renameSource(host, source, newName);
                Config.renameChessPuzzlegenCursor(host, source, newName);
                if (expandedSources.remove(source)) expandedSources.add(newName);
                host.runOnUiThread(this::refresh);
            }).start();
        }));
        box.addView(optionRow("Cancel", 0xFF8A8A8A, v -> dialog.dismiss()));

        UiKit.finishCentered(dialog, scrim);
    }

    private void confirmDeleteSource(String source) {
        VaultUi.confirm(host, "Delete " + source,
                "This also removes any puzzles generated from it.",
                "Delete", () -> new Thread(() -> {
                    ChessLibrary.deleteSource(host, source);
                    ChessPuzzles.deleteBySource(host, source);
                    Config.clearChessPuzzlegenCursor(host, source);
                    expandedSources.remove(source);
                    host.runOnUiThread(this::refresh);
                }).start(),
                "Cancel", () -> { });
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

package com.plainphone.app;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The Nav section's "Areas" tab: the whole {@link AreaCatalog}, one row per area, with its status
 * on the right — Installed, Download, or the percent while it downloads.
 *
 * <p>Tap a Download row and a pop-up asks once more before it starts. Hold any row to select
 * several (the same {@link SelectionBar} flow as the PGN files screen): DOWNLOAD fetches the
 * ones you picked that are not installed, DELETE removes the installed ones. Every change calls
 * {@code onChanged} so {@link MapPanel} rebuilds itself from the areas now on the phone.
 */
final class AreasPanel implements SelBarHost {

    private static final int WAITING = -1;

    /** Called after an area was downloaded or deleted. */
    interface OnChanged {
        void run();
    }

    private static final class Row {
        final String id;
        final String name;
        final int sizeMb;              // 0 when unknown (installed but not in the catalog)
        final boolean installed;
        final AreaCatalog.Entry entry; // null when not in the catalog

        Row(String id, String name, int sizeMb, boolean installed, AreaCatalog.Entry entry) {
            this.id = id;
            this.name = name;
            this.sizeMb = sizeMb;
            this.installed = installed;
            this.entry = entry;
        }
    }

    private final Activity activity;
    private final OnChanged onChanged;
    private final Typeface font;
    private final LinearLayout outer;
    private final SelectionBar bar;
    private final LinearLayout list;

    private List<AreaCatalog.Entry> catalog;      // null until fetched, or if the fetch failed
    private boolean loading;
    private boolean selecting;
    private final Set<String> selection = new LinkedHashSet<>();
    private final Map<String, Integer> progress = new LinkedHashMap<>();   // id -> percent, or WAITING

    AreasPanel(Activity activity, OnChanged onChanged) {
        this.activity = activity;
        this.onChanged = onChanged;
        this.font = Fonts.current(activity);

        outer = new LinearLayout(activity);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.setBackgroundColor(Color.BLACK);

        bar = new SelectionBar(activity);
        outer.addView(bar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        list = new LinearLayout(activity);
        list.setOrientation(LinearLayout.VERTICAL);
        ScrollView scroller = new ScrollView(activity);
        scroller.setVerticalScrollBarEnabled(false);
        scroller.addView(list, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        outer.addView(scroller, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        render();
    }

    View view() {
        return outer;
    }

    /** Called each time the tab is shown: fetch the catalog if it has not loaded yet. */
    void refresh() {
        if (catalog == null && !loading) fetchCatalog();
        render();
    }

    private void fetchCatalog() {
        loading = true;
        render();
        AreaCatalog.fetch(entries -> {
            loading = false;
            catalog = entries;
            render();
        });
    }

    // ------------------------------------------------------------------- rows

    private List<Row> rows() {
        List<MapArea> have = MapAreas.all(activity);
        Set<String> haveIds = new LinkedHashSet<>();
        for (MapArea a : have) haveIds.add(a.id);

        List<Row> rows = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        if (catalog != null) {
            for (AreaCatalog.Entry e : catalog) {
                rows.add(new Row(e.id, e.displayName, e.sizeMb, haveIds.contains(e.id), e));
                seen.add(e.id);
            }
        }
        for (MapArea a : have) {          // installed but not in the catalog (or the catalog did not load)
            if (!seen.contains(a.id)) rows.add(new Row(a.id, a.displayName, 0, true, null));
        }
        return rows;
    }

    private void render() {
        List<Row> rows = rows();
        List<String> ids = new ArrayList<>();
        for (Row r : rows) ids.add(r.id);
        selection.retainAll(ids);
        if (selection.isEmpty()) selecting = false;

        bar.setVisibility(selecting ? View.VISIBLE : View.GONE);
        if (selecting) {
            boolean anyDownload = false, anyDelete = false;
            for (Row r : rows) {
                if (!selection.contains(r.id)) continue;
                if (r.installed) anyDelete = true;
                else if (r.entry != null && !progress.containsKey(r.id)) anyDownload = true;
            }
            List<BarAction> actions = new ArrayList<>();
            if (anyDownload) actions.add(new BarAction("Download", this::confirmDownloadSelected));
            if (anyDelete) actions.add(new BarAction("Delete", this::confirmDeleteSelected));
            bar.bind(this, ids, actions);
        }

        list.removeAllViews();
        for (Row r : rows) list.addView(rowView(r));

        if (loading && catalog == null) {
            list.addView(loadingRow());
        } else if (catalog == null) {
            TextView retry = message("Couldn't load the catalog. Tap to try again.");
            retry.setOnClickListener(v -> fetchCatalog());
            list.addView(retry);
        } else if (rows.isEmpty()) {
            list.addView(message("The catalog is empty."));
        }
    }

    private View rowView(Row r) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(UiKit.dp(activity, 6), UiKit.dp(activity, 15), UiKit.dp(activity, 6), UiKit.dp(activity, 15));

        StateListDrawable background = new StateListDrawable();
        background.addState(new int[]{android.R.attr.state_pressed}, UiKit.pressedFill());
        background.setEnterFadeDuration(120);
        background.setExitFadeDuration(120);
        background.addState(new int[]{}, new ColorDrawable(Color.BLACK));
        row.setBackground(background);

        if (selecting) {
            TextView box = text(selection.contains(r.id) ? "[x]" : "[ ]", 16,
                    selection.contains(r.id) ? Color.WHITE : 0xFF777777);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.rightMargin = UiKit.dp(activity, 12);
            row.addView(box, lp);
        }

        TextView name = text(r.name + (r.sizeMb > 0 ? "  (" + r.sizeMb + " MB)" : ""), 16, Color.WHITE);
        row.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Integer pct = progress.get(r.id);
        String status;
        int color;
        if (pct != null) {
            status = pct == WAITING ? "Waiting" : pct + "%";
            color = pct == WAITING ? 0xFF808080 : Color.WHITE;
        } else if (r.installed) {
            status = "Installed";
            color = 0xFF808080;
        } else {
            status = "Download";
            color = Color.WHITE;
        }
        row.addView(text(status, 13, color));

        row.setOnClickListener(v -> onTap(r));
        row.setOnLongClickListener(v -> {
            if (!selecting) {
                selecting = true;
                selection.clear();
            }
            toggle(r.id);
            return true;
        });
        return row;
    }

    private void onTap(Row r) {
        if (selecting) {
            toggle(r.id);
            return;
        }
        if (r.installed || r.entry == null || progress.containsKey(r.id)) return;
        VaultUi.confirm(activity, "Download " + r.name + "?", null,
                "Download", () -> startDownloads(single(r.entry)), "Cancel", () -> { });
    }

    private static List<AreaCatalog.Entry> single(AreaCatalog.Entry e) {
        List<AreaCatalog.Entry> one = new ArrayList<>();
        one.add(e);
        return one;
    }

    private void toggle(String id) {
        if (!selection.remove(id)) selection.add(id);
        if (selection.isEmpty()) selecting = false;
        render();
    }

    private TextView text(String s, float size, int color) {
        TextView t = new TextView(activity);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setTypeface(font);
        return t;
    }

    private TextView message(String s) {
        TextView t = text(s, 14, 0xFFAAAAAA);
        t.setPadding(UiKit.dp(activity, 6), UiKit.dp(activity, 16), UiKit.dp(activity, 6), UiKit.dp(activity, 16));
        return t;
    }

    private View loadingRow() {
        LinearLayout row = new LinearLayout(activity);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(UiKit.dp(activity, 6), UiKit.dp(activity, 16), UiKit.dp(activity, 6), UiKit.dp(activity, 16));
        ProgressBar spinner = UiKit.spinner(activity);
        row.addView(spinner, new LinearLayout.LayoutParams(UiKit.dp(activity, 18), UiKit.dp(activity, 18)));
        TextView label = text("Loading the catalog...", 14, 0xFFAAAAAA);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = UiKit.dp(activity, 12);
        row.addView(label, lp);
        return row;
    }

    // -------------------------------------------------------------- downloads

    private List<Row> pickedRows() {
        List<Row> picked = new ArrayList<>();
        for (Row r : rows()) if (selection.contains(r.id)) picked.add(r);
        return picked;
    }

    private void confirmDownloadSelected() {
        List<AreaCatalog.Entry> todo = new ArrayList<>();
        for (Row r : pickedRows()) {
            if (!r.installed && r.entry != null && !progress.containsKey(r.id)) todo.add(r.entry);
        }
        if (todo.isEmpty()) return;
        String title = todo.size() == 1 ? "Download " + todo.get(0).displayName + "?"
                : "Download " + todo.size() + " areas?";
        VaultUi.confirm(activity, title, null, "Download", () -> {
            selecting = false;
            selection.clear();
            startDownloads(todo);
        }, "Cancel", () -> { });
    }

    /** One after another, so a slow connection is not split across several big files. */
    private void startDownloads(List<AreaCatalog.Entry> entries) {
        ArrayDeque<AreaCatalog.Entry> queue = new ArrayDeque<>(entries);
        for (AreaCatalog.Entry e : entries) progress.put(e.id, WAITING);
        render();
        downloadNext(queue);
    }

    private void downloadNext(ArrayDeque<AreaCatalog.Entry> queue) {
        AreaCatalog.Entry entry = queue.poll();
        if (entry == null) return;
        progress.put(entry.id, 0);
        render();
        AreaDownloader.download(activity, entry,
                percent -> {
                    progress.put(entry.id, percent);
                    render();
                },
                success -> {
                    progress.remove(entry.id);
                    if (success) {
                        changed();
                    } else {
                        Toast.makeText(activity, "Couldn't download " + entry.displayName
                                + ". Check your connection.", Toast.LENGTH_LONG).show();
                    }
                    render();
                    downloadNext(queue);
                });
    }

    // ----------------------------------------------------------------- delete

    private void confirmDeleteSelected() {
        List<Row> picked = new ArrayList<>();
        for (Row r : pickedRows()) if (r.installed) picked.add(r);
        if (picked.isEmpty()) return;
        String title = picked.size() == 1 ? "Delete " + picked.get(0).name : "Delete " + picked.size() + " areas";
        VaultUi.confirm(activity, title, null, "Delete", () -> {
            Set<String> ids = new LinkedHashSet<>();
            for (Row r : picked) ids.add(r.id);
            for (MapArea a : MapAreas.all(activity)) {
                if (ids.contains(a.id)) AreaDownloader.deleteArea(activity, a);
            }
            selecting = false;
            selection.clear();
            changed();
            render();
        }, "Cancel", () -> { });
    }

    /** The areas on the phone changed: search and routing rebuild from what is left, and the map reloads. */
    private void changed() {
        PlaceIndex.invalidate();
        RoadGraph.invalidate();
        onChanged.run();
    }

    // ------------------------------------------------------------ SelBarHost

    @Override public Set<String> selection() { return selection; }

    @Override public void setSelected(Collection<String> ids) {
        selection.clear();
        selection.addAll(ids);
        if (selection.isEmpty()) selecting = false;
        render();
    }

    @Override public void exitSelection() {
        selecting = false;
        selection.clear();
        render();
    }
}

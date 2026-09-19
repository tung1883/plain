package com.plainphone.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Every game ever imported through "Import PGN" — a standing library, unlike the one-shot
 *  "choose a game" picker shown right after an import (that one forgets everything once a
 *  game is loaded or the dialog is cancelled). Same {@link ListView}/{@link BaseAdapter}
 *  recycling as that picker — a heavy PGN collection makes this routinely tens of thousands
 *  of rows. */
public class ChessLibraryActivity extends Activity {

    static final String EXTRA_ID = "id";
    static final String EXTRA_WHITE = "white";
    static final String EXTRA_BLACK = "black";
    static final String EXTRA_SANS = "sans";
    /** Optional: restricts the list to one PGN source's games ("Games in a PGN", opened from
     *  the Board tab's PGN chip or the PGN-files screen) instead of the whole library. */
    static final String EXTRA_SOURCE_FILTER = "source_filter";
    /** Optional, only meaningful with {@link #EXTRA_SOURCE_FILTER}: the currently-loaded
     *  game's id, shown bold in the list rather than picked out via any Intent result. */
    static final String EXTRA_CURRENT_ID = "current_id";

    private List<ChessLibrary.Entry> all = new ArrayList<>();
    private List<ChessLibrary.Entry> shown = new ArrayList<>();
    private BaseAdapter adapter;
    private TextView countLine;
    private final Handler searchHandler = new Handler();
    private Runnable pendingSearch;
    private String sourceFilter;
    private String currentEntryId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sourceFilter = getIntent().getStringExtra(EXTRA_SOURCE_FILTER);
        currentEntryId = getIntent().getStringExtra(EXTRA_CURRENT_ID);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setBackgroundColor(Color.BLACK);

        EditText search = new EditText(this);
        UiKit.style(this, search);
        search.setHint("Search players, event…");
        search.setHintTextColor(0xFF6E6E6E);
        search.setSingleLine(true);
        LinearLayout searchWrap = new LinearLayout(this);
        searchWrap.setPadding(UiKit.dp(this, 20), UiKit.dp(this, 14), UiKit.dp(this, 20), UiKit.dp(this, 8));
        searchWrap.addView(search, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        // "Games in a PGN" is a short, already-scoped list — no need for its own search.
        searchWrap.setVisibility(sourceFilter == null ? View.VISIBLE : View.GONE);
        content.addView(searchWrap);

        countLine = new TextView(this);
        countLine.setTextColor(0xFF6E6E6E);
        countLine.setTextSize(12);
        countLine.setTypeface(Fonts.current(this));
        countLine.setPadding(UiKit.dp(this, 24), 0, UiKit.dp(this, 24), UiKit.dp(this, 12));
        content.addView(countLine);

        ListView list = new ListView(this);
        list.setDivider(new ColorDrawable(0xFF303030));
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
        list.setOnItemClickListener((parent, view, position, id) -> pick(shown.get(position).id));
        content.addView(list, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { scheduleFilter(s.toString()); }
        });

        UiKit.screen(this, sourceFilter != null ? sourceFilter : "Imported games", content);
        load();
    }

    private void load() {
        countLine.setText("Loading…");
        new Thread(() -> {
            List<ChessLibrary.Entry> loaded = sourceFilter != null
                    ? ChessLibrary.loadBySource(this, sourceFilter)
                    : ChessLibrary.loadAll(this);
            runOnUiThread(() -> {
                all = loaded;
                shown = all;
                adapter.notifyDataSetChanged();
                updateCount();
            });
        }).start();
    }

    private void scheduleFilter(String query) {
        if (pendingSearch != null) searchHandler.removeCallbacks(pendingSearch);
        pendingSearch = () -> applyFilter(query);
        searchHandler.postDelayed(pendingSearch, 150);
    }

    private void applyFilter(String query) {
        String q = query.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) {
            shown = all;
        } else {
            List<ChessLibrary.Entry> matched = new ArrayList<>();
            for (ChessLibrary.Entry e : all) {
                if (e.white.toLowerCase(Locale.ROOT).contains(q)
                        || e.black.toLowerCase(Locale.ROOT).contains(q)
                        || e.event.toLowerCase(Locale.ROOT).contains(q)) {
                    matched.add(e);
                }
            }
            shown = matched;
        }
        adapter.notifyDataSetChanged();
        updateCount();
    }

    private void updateCount() {
        countLine.setText(shown.size() + " of " + all.size() + " games");
    }

    // shown/all are the lightweight rows loadAll() now returns (no move list — see
    // ChessLibrary.loadAll); the tapped entry's moves are only ever read back here, for the
    // one game actually picked, off the UI thread.
    private void pick(String id) {
        new Thread(() -> {
            ChessLibrary.Entry entry = ChessLibrary.findById(this, id);
            runOnUiThread(() -> {
                if (entry == null) { finish(); return; }
                Intent result = new Intent();
                result.putExtra(EXTRA_ID, entry.id);
                result.putExtra(EXTRA_WHITE, entry.white);
                result.putExtra(EXTRA_BLACK, entry.black);
                result.putExtra(EXTRA_SANS, entry.sansJoined);
                setResult(RESULT_OK, result);
                finish();
            });
        }).start();
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
        TextView source = text(11, 0xFF4A4A4A);
        source.setPadding(0, UiKit.dp(this, 2), 0, 0);
        row.addView(source);
        return row;
    }

    private void bindRow(View row, ChessLibrary.Entry e) {
        LinearLayout box = (LinearLayout) row;
        boolean current = e.id != null && e.id.equals(currentEntryId);
        TextView title = (TextView) box.getChildAt(0);
        title.setText(e.white + " vs " + e.black);
        title.setTypeface(Fonts.current(this), current ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        ((TextView) box.getChildAt(1)).setText(e.event + " · " + e.date + " · " + e.result);
        ((TextView) box.getChildAt(2)).setText(e.src);
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

package com.plainphone.app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

/**
 * A home-section list rendered exactly like the launcher's own — the same
 * {@link SearchResultsAdapter} over a list of {@link SearchResult} rows. Panels
 * (Notes / To-do / Recorder) use this so they match the normal UI.
 */
@SuppressLint("ViewConstructor")
final class SectionListView extends ListView {

    interface Provider {
        void fill(List<Object> rows);
    }

    interface OnLongPress {
        /** @return true to consume (e.g. enter selection mode). */
        boolean onLongPress(SearchResult row);
    }

    private final List<Object> rows = new ArrayList<>();
    private final SearchResultsAdapter adapter;
    private Provider provider;
    private OnLongPress longPress;

    SectionListView(Context ctx) {
        super(ctx);
        setBackgroundColor(Color.BLACK);
        setDivider(null);
        setDividerHeight(0);
        setVerticalScrollBarEnabled(false);
        setOverScrollMode(View.OVER_SCROLL_NEVER);
        setSelector(new ColorDrawable(Color.TRANSPARENT)); // no rounded long-press highlight
        setCacheColorHint(Color.BLACK);
        adapter = new SearchResultsAdapter(ctx, rows, Fonts.current(ctx));
        setAdapter(adapter);
        setOnItemClickListener((parent, view, pos, id) -> {
            SearchResult r = adapter.resultAt(pos);
            if (r != null) r.activate();
        });
        setOnItemLongClickListener((parent, view, pos, id) -> {
            SearchResult r = adapter.resultAt(pos);
            return r != null && longPress != null && longPress.onLongPress(r);
        });
    }

    void setLongPress(OnLongPress l) { this.longPress = l; }

    void setProvider(Provider p) {
        this.provider = p;
        refresh();
    }

    void refresh() {
        int y = getFirstVisiblePosition();
        rows.clear();
        if (provider != null) provider.fill(rows);
        adapter.notifyDataSetChanged();
        if (y > 0 && y < rows.size()) setSelection(y);
    }
}

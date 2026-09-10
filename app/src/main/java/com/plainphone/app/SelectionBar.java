package com.plainphone.app;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

/**
 * The two-row multi-select bar: {@code ✕ / "N selected" / SELECT ALL|NONE}, then
 * a scrollable action row. Shared by the home screen and workspace panels; a
 * caller {@link #bind}s it every render.
 */
final class SelectionBar extends LinearLayout {

    SelectionBar(Context ctx) {
        super(ctx);
        setOrientation(VERTICAL);
        setBackgroundColor(Color.BLACK);
        setVisibility(GONE);
    }

    void bind(SelectionHost host, List<String> allIds, List<BarAction> actions) {
        Context c = getContext();
        Typeface font = Fonts.current(c);
        removeAllViews();

        int n = host.selection().size();
        boolean all = !allIds.isEmpty() && n == allIds.size();

        LinearLayout top = new LinearLayout(c);
        top.setOrientation(HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(40, 28, 40, 14);

        TextView close = new TextView(c);
        close.setText("✕");
        close.setTextColor(Color.WHITE);
        close.setTextSize(16);
        close.setTypeface(font);
        close.setPadding(8, 8, 28, 8);
        close.setOnClickListener(v -> host.exitSelection());
        top.addView(close);

        TextView count = new TextView(c);
        count.setText(n + " selected");
        count.setTextColor(Color.WHITE);
        count.setTextSize(14);
        count.setTypeface(font);
        top.addView(count, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));

        TextView selAll = new TextView(c);
        selAll.setText(all ? "SELECT NONE" : "SELECT ALL");
        selAll.setTextColor(Color.GRAY);
        selAll.setTextSize(12);
        selAll.setLetterSpacing(0.1f);
        selAll.setTypeface(font);
        selAll.setPadding(16, 8, 8, 8);
        selAll.setOnClickListener(v ->
                host.setSelected(all ? java.util.Collections.emptyList() : allIds));
        top.addView(selAll);
        addView(top);

        LinearLayout actionRow = new LinearLayout(c);
        actionRow.setOrientation(HORIZONTAL);
        actionRow.setPadding(40, 6, 40, 22);
        boolean enabled = n > 0;
        for (BarAction a : actions) {
            TextView t = new TextView(c);
            t.setText(a.label.toUpperCase(Locale.US));
            t.setTextColor(enabled ? Color.WHITE : 0xFF555555);
            t.setTextSize(12);
            t.setLetterSpacing(0.08f);
            t.setTypeface(font);
            t.setPadding(0, 10, 44, 10);
            if (enabled) t.setOnClickListener(v -> a.run.run());
            actionRow.addView(t);
        }
        HorizontalScrollView scroller = new HorizontalScrollView(c);
        scroller.setHorizontalScrollBarEnabled(false);
        scroller.addView(actionRow);
        addView(scroller);

        View divider = new View(c);
        divider.setBackgroundColor(0xFF1C1C1C);
        addView(divider, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
    }
}

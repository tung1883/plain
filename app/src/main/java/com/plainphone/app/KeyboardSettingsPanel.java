package com.plainphone.app;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;

/** The keyboard's own settings: key size, dev row, auto-close. Applied immediately. */
final class KeyboardSettingsPanel extends LinearLayout {

    private final KeyboardRoot.Host host;
    private final LinearLayout list;

    KeyboardSettingsPanel(Context ctx, KeyboardRoot.Host host) {
        super(ctx);
        this.host = host;
        setOrientation(VERTICAL);

        ScrollView scroll = new ScrollView(ctx);
        scroll.setVerticalScrollBarEnabled(false);
        list = new LinearLayout(ctx);
        list.setOrientation(VERTICAL);
        scroll.addView(list, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        addView(scroll, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
    }

    void refresh() {
        Context ctx = getContext();
        list.removeAllViews();

        String size = Config.getKbSize(ctx);
        addGroup(ctx, "KEY SIZE",
                option(ctx, "Small", size.equals("small"), () -> Config.setKbSize(ctx, "small")),
                option(ctx, "Medium", size.equals("medium"), () -> Config.setKbSize(ctx, "medium")),
                option(ctx, "Large", size.equals("large"), () -> Config.setKbSize(ctx, "large")));

        boolean devRow = Config.isKbDevRow(ctx);
        addGroup(ctx, "DEV ROW  (ESC, TAB, CTRL, ALT, ARROWS)",
                option(ctx, "On", devRow, () -> Config.setKbDevRow(ctx, true)),
                option(ctx, "Off", !devRow, () -> Config.setKbDevRow(ctx, false)));

        boolean pair = Config.isKbAutoPair(ctx);
        addGroup(ctx, "AUTO-CLOSE  ( [ {",
                option(ctx, "On", pair, () -> Config.setKbAutoPair(ctx, true)),
                option(ctx, "Off", !pair, () -> Config.setKbAutoPair(ctx, false)));
    }

    private android.widget.TextView option(Context ctx, String text, boolean on, Runnable set) {
        android.widget.TextView t = PanelKit.chip(ctx, text, on, v -> {
            set.run();
            host.settingsChanged();
            refresh();
        });
        LayoutParams lp = new LayoutParams(0, PanelKit.dp(ctx, 36), 1f);
        lp.rightMargin = PanelKit.dp(ctx, 4);
        t.setLayoutParams(lp);
        return t;
    }

    private void addGroup(Context ctx, String title, View... options) {
        android.widget.TextView caption = PanelKit.caption(ctx, title);
        LayoutParams clp = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        clp.topMargin = PanelKit.dp(ctx, 8);
        clp.bottomMargin = PanelKit.dp(ctx, 4);
        clp.leftMargin = PanelKit.dp(ctx, 4);
        list.addView(caption, clp);
        LinearLayout row = new LinearLayout(ctx);
        for (View o : options) row.addView(o);
        list.addView(row, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }
}

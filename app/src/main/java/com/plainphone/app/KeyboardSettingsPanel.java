package com.plainphone.app;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** The keyboard's own settings: dev row, auto-close. Applied immediately; height is the toolbar's drag button. */
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

        boolean devRow = Config.isKbDevRow(ctx);
        addRow(ctx, "Dev row", "Esc Tab Ctrl Alt arrows", devRow ? 0 : 1,
                i -> Config.setKbDevRow(ctx, i == 0));

        boolean pair = Config.isKbAutoPair(ctx);
        addRow(ctx, "Auto-close", "( [ { \" ' type their pair", pair ? 0 : 1,
                i -> Config.setKbAutoPair(ctx, i == 0));

        TextView tip = PanelKit.caption(ctx, "HEIGHT: HOLD THE ↕ BUTTON ON THE KEYS AND DRAG");
        tip.setPadding(PanelKit.dp(ctx, 4), PanelKit.dp(ctx, 10), 0, 0);
        list.addView(tip);
    }

    /** One setting: label and hint on the left, an On/Off control on the right. */
    private void addRow(Context ctx, String title, String hint, int selected, java.util.function.IntConsumer set) {
        LinearLayout row = new LinearLayout(ctx);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(PanelKit.dp(ctx, 4), PanelKit.dp(ctx, 9), PanelKit.dp(ctx, 4), PanelKit.dp(ctx, 9));

        LinearLayout text = new LinearLayout(ctx);
        text.setOrientation(VERTICAL);
        TextView t = new TextView(ctx);
        t.setText(title);
        t.setTextSize(13);
        t.setTextColor(PanelKit.FG);
        t.setTypeface(Fonts.cascadiaMono(ctx));
        TextView h = new TextView(ctx);
        h.setText(hint);
        h.setTextSize(10.5f);
        h.setTextColor(PanelKit.DIM);
        h.setTypeface(Fonts.cascadiaMono(ctx));
        text.addView(t);
        text.addView(h);
        row.addView(text, new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        row.addView(PanelKit.segmented(ctx, new String[]{"On", "Off"}, selected, i -> {
            set.accept(i);
            host.settingsChanged();
            refresh();
        }));
        list.addView(row, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        View line = new View(ctx);
        line.setBackgroundColor(0xFF181818);
        list.addView(line, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
    }
}

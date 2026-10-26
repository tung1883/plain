package com.plainphone.app;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.IntConsumer;

/**
 * A bottom sheet: a panel that slides up over a dimmed screen, for pick-lists and confirmations. It is
 * an overlay inside the Activity's content, not a dialog window; tapping outside closes it, and so does
 * Back when the Activity calls {@link #dismissTop}. One sheet per Activity: opening another replaces it.
 */
final class Sheet {

    private static final Map<Activity, Sheet> OPEN = new WeakHashMap<>();
    private static final int PANEL = 0xFF000000, EDGE = 0xFF2C2C2C, DIM = 0xFF808080, FAINT = 0xFF666666;

    private final Activity activity;
    private final ViewGroup host;
    private final FrameLayout overlay;
    private final LinearLayout panel;
    private final LinearLayout body;
    private final Typeface typeface;
    private boolean closing;
    private Runnable onClose;

    private Sheet(Activity activity, String title, String subtitle) {
        this.activity = activity;
        this.typeface = Fonts.current(activity);
        this.host = activity.findViewById(android.R.id.content);

        overlay = new FrameLayout(activity);
        View scrim = new View(activity);
        scrim.setBackgroundColor(0xA0000000);
        scrim.setOnClickListener(v -> dismiss());
        overlay.addView(scrim, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        panel = new LinearLayout(activity);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackground(panelBackground(activity));
        panel.setClickable(true);                                  // taps on the panel must not reach the scrim
        panel.setPadding(0, UiKit.dp(activity, 8), 0, UiKit.dp(activity, 24));

        View grab = new View(activity);
        GradientDrawable grabDrawable = new GradientDrawable();
        grabDrawable.setColor(0xFF333333);
        grabDrawable.setCornerRadius(UiKit.dp(activity, 2));
        grab.setBackground(grabDrawable);
        LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(UiKit.dp(activity, 36), UiKit.dp(activity, 4));
        gp.gravity = Gravity.CENTER_HORIZONTAL;
        gp.bottomMargin = UiKit.dp(activity, 12);
        panel.addView(grab, gp);

        if (title != null && !title.isEmpty()) {
            TextView t = text(title, 18, Color.WHITE);
            t.setPadding(48, 0, 48, subtitle == null || subtitle.isEmpty() ? UiKit.dp(activity, 8) : 0);
            t.setSingleLine(true);
            t.setEllipsize(TextUtils.TruncateAt.END);
            panel.addView(t);
        }
        if (subtitle != null && !subtitle.isEmpty()) {
            TextView s = text(subtitle, 13, DIM);
            s.setPadding(48, UiKit.dp(activity, 4), 48, UiKit.dp(activity, 8));
            s.setSingleLine(true);
            s.setEllipsize(TextUtils.TruncateAt.MIDDLE);
            panel.addView(s);
        }

        body = new LinearLayout(activity);
        body.setOrientation(LinearLayout.VERTICAL);
        ScrollView scroll = new CappedScroll(activity);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.addView(body, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        panel.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM);
        overlay.addView(panel, lp);
    }

    /** Open a sheet (replacing any open one). Fill it with {@link #setContent}. */
    static Sheet show(Activity activity, String title, String subtitle) {
        Sheet old = OPEN.get(activity);
        if (old != null) old.remove();
        Sheet sheet = new Sheet(activity, title, subtitle);
        OPEN.put(activity, sheet);
        sheet.host.addView(sheet.overlay, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        sheet.panel.setAlpha(0f);
        sheet.panel.post(() -> {
            sheet.panel.setTranslationY(sheet.panel.getHeight());
            sheet.panel.setAlpha(1f);
            sheet.panel.animate().translationY(0).setDuration(180).start();
        });
        return sheet;
    }

    /** True when a sheet was open and has been closed (call from the Activity's onBackPressed). */
    static boolean dismissTop(Activity activity) {
        Sheet s = OPEN.get(activity);
        if (s == null) return false;
        s.dismiss();
        return true;
    }

    static boolean isOpen(Activity activity) {
        return OPEN.containsKey(activity);
    }

    /** Replace what the sheet shows below its title (the sheet stays put, so lists can redraw on a tap). */
    void setContent(View... views) {
        body.removeAllViews();
        for (View v : views) body.addView(v);
    }

    /** Called once when the sheet closes, however it closes. */
    void onClose(Runnable r) {
        onClose = r;
    }

    void dismiss() {
        if (closing) return;
        closing = true;
        panel.animate().translationY(panel.getHeight()).setDuration(140).withEndAction(this::remove).start();
    }

    private void remove() {
        closing = true;
        if (OPEN.get(activity) == this) OPEN.remove(activity);
        if (overlay.getParent() != null) host.removeView(overlay);
        if (onClose != null) {
            Runnable r = onClose;
            onClose = null;
            r.run();
        }
    }

    // ---------------------------------------------------------------- building blocks

    private TextView text(String s, float sp, int color) {
        TextView t = new TextView(activity);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setTypeface(typeface);
        return t;
    }

    private static Drawable panelBackground(Context c) {
        GradientDrawable box = new GradientDrawable();
        box.setColor(PANEL);
        box.setStroke(1, EDGE);
        float r = UiKit.dp(c, UiKit.R_LG);
        box.setCornerRadii(new float[]{r, r, r, r, 0, 0, 0, 0});
        return box;
    }

    private static Drawable rowBackground() {
        StateListDrawable d = new StateListDrawable();
        d.addState(new int[]{android.R.attr.state_pressed}, UiKit.pressedFill());
        d.setEnterFadeDuration(120);
        d.setExitFadeDuration(120);
        d.addState(new int[]{}, new ColorDrawable(PANEL));
        return d;
    }

    /** One tappable row: an optional icon, the label, an optional dim detail on the right. */
    View optionRow(String icon, String label, String detail, Runnable click) {
        LinearLayout row = baseRow();
        if (icon != null) {
            ImageView iv = new ImageView(activity);
            iv.setImageDrawable(MiniIcons.byName(icon, UiKit.dp(activity, 22), 0xFF9A9A9A));
            LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(UiKit.dp(activity, 22), UiKit.dp(activity, 22));
            ip.rightMargin = UiKit.dp(activity, 16);
            row.addView(iv, ip);
        }
        TextView l = text(label, 18, Color.WHITE);
        l.setSingleLine(true);
        l.setEllipsize(TextUtils.TruncateAt.END);
        row.addView(l, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        if (detail != null && !detail.isEmpty()) {
            TextView d = text(detail, 13, FAINT);
            d.setSingleLine(true);
            d.setEllipsize(TextUtils.TruncateAt.START);
            d.setPadding(UiKit.dp(activity, 12), 0, 0, 0);
            row.addView(d);
        }
        row.setOnClickListener(v -> click.run());
        return row;
    }

    /** A check box (or radio button) row; {@code click} should flip the state and redraw. */
    View checkRow(String label, String note, boolean checked, boolean radio, boolean dimmed, Runnable click) {
        LinearLayout row = baseRow();
        row.addView(new CheckView(activity, checked, radio));
        TextView l = text(label, 18, Color.WHITE);
        l.setSingleLine(true);
        l.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.leftMargin = UiKit.dp(activity, 16);
        row.addView(l, lp);
        if (note != null && !note.isEmpty()) {
            TextView n = text(note, 13, FAINT);
            n.setSingleLine(true);
            row.addView(n);
        }
        row.setAlpha(dimmed ? 0.4f : 1f);
        row.setOnClickListener(v -> click.run());
        return row;
    }

    /** A row of pill tabs (storage volumes); {@code counts[i] > 0} adds a small badge. */
    View tabs(String[] labels, int[] counts, int selected, IntConsumer pick) {
        HorizontalScrollView scroll = new HorizontalScrollView(activity);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(activity);
        row.setPadding(48, UiKit.dp(activity, 4), 48, UiKit.dp(activity, 10));
        for (int i = 0; i < labels.length; i++) {
            final int index = i;
            boolean on = i == selected;
            TextView t = text(labels[i] + (counts[i] > 0 ? "  " + counts[i] : ""), 14, on ? Color.BLACK : 0xFFB5B5B5);
            t.setGravity(Gravity.CENTER);
            t.setSingleLine(true);
            t.setPadding(UiKit.dp(activity, 14), 0, UiKit.dp(activity, 14), 0);
            t.setBackground(on ? UiKit.rounded(activity, Color.WHITE, Color.WHITE, 1f, 17)
                    : UiKit.pressable(activity, Color.BLACK, 0xFF1A1A1A, EDGE, 1f, 17));
            t.setOnClickListener(v -> pick.accept(index));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, UiKit.dp(activity, 34));
            lp.rightMargin = UiKit.dp(activity, 6);
            row.addView(t, lp);
        }
        scroll.addView(row);
        return scroll;
    }

    /** Buttons side by side. {@code styles}: 0 plain, 1 white (main action), 2 red (destructive). */
    View buttons(String[] labels, int[] styles, Runnable[] actions) {
        LinearLayout row = new LinearLayout(activity);
        row.setPadding(48, UiKit.dp(activity, 12), 48, 0);
        for (int i = 0; i < labels.length; i++) {
            final int index = i;
            int style = styles[i];
            TextView b = text(labels[i], 16, style == 1 ? Color.BLACK : style == 2 ? 0xFFE5484D : 0xFFE8E8E8);
            b.setGravity(Gravity.CENTER);
            b.setBackground(style == 1 ? UiKit.rounded(activity, Color.WHITE, Color.WHITE, 1f, 12)
                    : UiKit.pressable(activity, Color.BLACK, 0xFF1A1A1A, style == 2 ? 0xFF3A1C1E : EDGE, 1f, 12));
            b.setOnClickListener(v -> actions[index].run());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, UiKit.dp(activity, 46), 1f);
            if (i > 0) lp.leftMargin = UiKit.dp(activity, 8);
            row.addView(b, lp);
        }
        return row;
    }

    private LinearLayout baseRow() {
        LinearLayout row = new LinearLayout(activity);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(48, UiKit.dp(activity, 14), 48, UiKit.dp(activity, 14));
        row.setBackground(rowBackground());
        return row;
    }

    /** A scroll view that never grows taller than about two thirds of the screen. */
    private static final class CappedScroll extends ScrollView {
        CappedScroll(Context c) {
            super(c);
        }

        @Override
        protected void onMeasure(int widthSpec, int heightSpec) {
            int max = (int) (getResources().getDisplayMetrics().heightPixels * 0.62f);
            super.onMeasure(widthSpec, MeasureSpec.makeMeasureSpec(max, MeasureSpec.AT_MOST));
        }
    }
}

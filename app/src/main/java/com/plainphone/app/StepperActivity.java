package com.plainphone.app;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * A duration / value setting. Default UI is a keypad screen: a segmented preset
 * strip, a big typed value, and the app's own numeric pad — type any in-range
 * number, or tap a preset for the common ones. Subclasses that pick a time of day
 * ({@link #listStyle()} = true) keep the older full-screen scrollable list instead,
 * since "type a number" doesn't suit a clock.
 */
public abstract class StepperActivity extends Activity {

    protected abstract String title();

    /** Unused by either UI; kept so existing subclasses still compile. */
    protected abstract String stepLabel();

    protected abstract int step();

    protected abstract int min();

    protected abstract int max();

    protected abstract int currentValue();

    protected abstract void save(int value);

    protected abstract String format(int value);

    /** Short unit shown next to the typed number and in the range caption. */
    protected String unitLabel() {
        return "";
    }

    /** Preset values for the segmented strip, low to high. Empty = no strip. */
    protected int[] chips() {
        return new int[0];
    }

    /** A chip's label. Default: the value plus a one-letter unit ("5m"). */
    protected String chipText(int value) {
        String unit = unitLabel();
        return value + (unit.isEmpty() ? "" : unit.substring(0, 1));
    }

    /** An extra full-width row above the strip (e.g. "Use the default …"), or null. */
    protected String topRowLabel() {
        return null;
    }

    protected boolean topRowActive() {
        return false;
    }

    /** Run when {@link #topRowLabel()} is tapped; the screen then closes. */
    protected void onTopRow() {
    }

    /** True to keep the old scrollable value list (times of day: not a "type a number" fit). */
    protected boolean listStyle() {
        return false;
    }

    /** Values to list, low to high, for {@link #listStyle()}. Default: min..max by step. */
    protected int[] values() {
        int n = Math.max(1, (max() - min()) / step() + 1);
        int[] v = new int[n];
        for (int i = 0; i < n; i++) v[i] = min() + i * step();
        return v;
    }

    private static final int MAX_DIGITS = 6;

    private Typeface font;
    private final StringBuilder typed = new StringBuilder();
    private boolean fresh = true;
    private TextView valueText;
    private TextView okKey;
    private TextView[] chipViews;
    private TextView topRowView;
    private TextView[] listRows;
    private int[] listValues;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        font = Fonts.current(this);
        if (listStyle()) {
            renderList();
        } else {
            renderKeypad();
        }
    }

    // --- keypad UI ---------------------------------------------------

    private void renderKeypad() {
        typed.append(currentValue());

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);

        if (topRowLabel() != null) {
            root.addView(topRow());
        }

        int[] chipValues = chips();
        chipViews = new TextView[chipValues.length];
        if (chipValues.length > 0) {
            LinearLayout strip = new LinearLayout(this);
            strip.setOrientation(LinearLayout.HORIZONTAL);
            View bottomRule = null;
            for (int i = 0; i < chipValues.length; i++) {
                int value = chipValues[i];
                TextView cell = new TextView(this);
                cell.setText(chipText(value));
                cell.setTextSize(13);
                cell.setTypeface(font);
                cell.setGravity(Gravity.CENTER);
                cell.setPadding(4, 30, 4, 30);
                if (i < chipValues.length - 1) {
                    LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                    View rule = new View(this);
                    rule.setBackgroundColor(0xFF1C1C1C);
                    LinearLayout cellWrap = new LinearLayout(this);
                    cellWrap.setOrientation(LinearLayout.HORIZONTAL);
                    cellWrap.addView(cell, new LinearLayout.LayoutParams(
                            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                    cellWrap.addView(rule, new LinearLayout.LayoutParams(1,
                            ViewGroup.LayoutParams.MATCH_PARENT));
                    strip.addView(cellWrap, cp);
                } else {
                    strip.addView(cell, new LinearLayout.LayoutParams(
                            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                }
                cell.setOnClickListener(v -> {
                    save(value);
                    settle();
                });
                chipViews[i] = cell;
            }
            View rule = new View(this);
            rule.setBackgroundColor(0xFF1C1C1C);
            LinearLayout stripBlock = new LinearLayout(this);
            stripBlock.setOrientation(LinearLayout.VERTICAL);
            stripBlock.addView(strip);
            stripBlock.addView(rule, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 1));
            root.addView(stripBlock);
        }

        LinearLayout valueRow = new LinearLayout(this);
        valueRow.setOrientation(LinearLayout.HORIZONTAL);
        valueRow.setGravity(Gravity.CENTER);
        valueText = new TextView(this);
        valueText.setTextColor(Color.WHITE);
        valueText.setTextSize(44);
        valueText.setTypeface(font);
        valueRow.addView(valueText);
        if (!unitLabel().isEmpty()) {
            TextView unit = new TextView(this);
            unit.setText(" " + unitLabel());
            unit.setTextColor(0xFF8C8C93);
            unit.setTextSize(14);
            unit.setTypeface(font);
            LinearLayout.LayoutParams unitParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            unitParams.gravity = Gravity.BOTTOM;
            unitParams.bottomMargin = UiKit.dp(this, 8);
            valueRow.addView(unit, unitParams);
        }
        LinearLayout.LayoutParams valueRowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        valueRowParams.topMargin = UiKit.dp(this, 22);
        root.addView(valueRow, valueRowParams);

        TextView range = new TextView(this);
        range.setText(min() + "–" + max() + (unitLabel().isEmpty() ? "" : " " + unitLabel()));
        range.setTextColor(0xFF6F6F6F);
        range.setTextSize(11);
        range.setTypeface(font);
        range.setGravity(Gravity.CENTER);
        root.addView(range);

        View spacer = new View(this);
        root.addView(spacer, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setGravity(Gravity.CENTER);
        String[][] keys = {{"1", "2", "3"}, {"4", "5", "6"}, {"7", "8", "9"}, {"OK", "0", "←"}};
        for (String[] keyRow : keys) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER);
            for (String k : keyRow) row.addView(keypadKey(k));
            grid.addView(row);
        }
        LinearLayout.LayoutParams gridParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        gridParams.bottomMargin = UiKit.dp(this, 20);
        root.addView(grid, gridParams);

        UiKit.screen(this, title(), root);
        refreshKeypad();
    }

    private View topRow() {
        topRowView = new TextView(this);
        topRowView.setText(topRowLabel());
        topRowView.setTextSize(15);
        topRowView.setTypeface(font);
        topRowView.setPadding(48, 30, 48, 30);
        topRowView.setGravity(Gravity.START);
        refreshTopRowStyle();
        topRowView.setOnClickListener(v -> {
            onTopRow();
            settle();
        });
        return topRowView;
    }

    private void refreshTopRowStyle() {
        if (topRowView == null) return;
        if (topRowActive()) {
            topRowView.setTextColor(Color.BLACK);
            topRowView.setBackgroundColor(Color.WHITE);
        } else {
            topRowView.setTextColor(Color.WHITE);
            topRowView.setBackground(rowBackground());
        }
    }

    private TextView keypadKey(String label) {
        TextView key = new TextView(this);
        key.setText(label);
        key.setTypeface(font);
        key.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                UiKit.dp(this, 70), UiKit.dp(this, 70));
        params.setMargins(UiKit.dp(this, 6), UiKit.dp(this, 6), UiKit.dp(this, 6), UiKit.dp(this, 6));
        key.setLayoutParams(params);

        if ("OK".equals(label)) {
            key.setTextSize(15);
            key.setTextColor(Color.WHITE);
            okKey = key;
            key.setOnClickListener(v -> tapOk());
        } else if ("←".equals(label)) {
            key.setTextSize(18);
            key.setTextColor(0xFF8C8C93);
            key.setBackground(pressBackground());
            key.setOnClickListener(v -> tapBackspace());
        } else {
            key.setTextSize(24);
            key.setTextColor(Color.WHITE);
            key.setBackground(pressBackground());
            key.setOnClickListener(v -> tapDigit(label));
        }
        return key;
    }

    private void tapDigit(String digit) {
        if (fresh) {
            typed.setLength(0);
            fresh = false;
        }
        if (typed.length() < MAX_DIGITS) typed.append(digit);
        refreshKeypad();
    }

    private void tapBackspace() {
        if (fresh) {
            typed.setLength(0);
            fresh = false;
        } else if (typed.length() > 0) {
            typed.deleteCharAt(typed.length() - 1);
        }
        refreshKeypad();
    }

    private void tapOk() {
        if (typed.length() == 0) return;
        int value;
        try {
            value = Integer.parseInt(typed.toString());
        } catch (NumberFormatException e) {
            return;
        }
        value = Math.max(min(), Math.min(max(), value));
        save(value);
        settle();
    }

    /** After a value is saved: reload the display from the new state, stay on screen. */
    private void settle() {
        typed.setLength(0);
        typed.append(currentValue());
        fresh = true;
        refreshKeypad();
        refreshTopRowStyle();
    }

    private void refreshKeypad() {
        valueText.setText(typed.length() == 0 ? "0" : typed.toString());
        if (okKey != null) okKey.setAlpha(typed.length() > 0 ? 1f : 0.3f);

        Integer parsed = null;
        try {
            if (typed.length() > 0) parsed = Integer.parseInt(typed.toString());
        } catch (NumberFormatException ignored) {
        }
        int[] chipValues = chips();
        for (int i = 0; i < chipViews.length; i++) {
            boolean on = parsed != null && parsed == chipValues[i];
            chipViews[i].setTextColor(on ? Color.BLACK : Color.GRAY);
            chipViews[i].setBackgroundColor(on ? Color.WHITE : Color.BLACK);
        }
    }

    private StateListDrawable pressBackground() {
        StateListDrawable bg = new StateListDrawable();
        bg.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Color.DKGRAY));
        bg.addState(new int[]{}, new ColorDrawable(Color.BLACK));
        return bg;
    }

    private StateListDrawable rowBackground() {
        StateListDrawable bg = new StateListDrawable();
        bg.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Color.DKGRAY));
        bg.addState(new int[]{}, new ColorDrawable(Color.BLACK));
        return bg;
    }

    // --- list UI (time of day) ----------------------------------------

    private void renderList() {
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setBackgroundColor(Color.BLACK);

        listValues = values();
        listRows = new TextView[listValues.length];
        int current = currentValue();
        View[] selectedRow = {null};

        for (int i = 0; i < listValues.length; i++) {
            int value = listValues[i];
            TextView row = listRow(format(value));
            row.setOnClickListener(v -> {
                save(value);
                highlightListRow(value);
            });
            listRows[i] = row;
            if (value == current) selectedRow[0] = row;
            list.addView(row);
        }
        highlightListRow(current);

        ScrollView scroller = new ScrollView(this);
        scroller.setBackgroundColor(Color.BLACK);
        scroller.addView(list, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        UiKit.screen(this, title(), scroller);

        if (selectedRow[0] != null) {
            View target = selectedRow[0];
            scroller.post(() -> scroller.scrollTo(0,
                    Math.max(0, target.getTop() - UiKit.dp(this, 140))));
        }
    }

    private void highlightListRow(int value) {
        for (int i = 0; i < listValues.length; i++) {
            boolean selected = listValues[i] == value;
            listRows[i].setTextColor(selected ? Color.BLACK : Color.WHITE);
            if (selected) {
                listRows[i].setBackgroundColor(Color.WHITE);
            } else {
                listRows[i].setBackground(pressBackground());
            }
        }
    }

    private TextView listRow(String label) {
        TextView view = new TextView(this);
        view.setText(label);
        view.setTextSize(18);
        view.setTypeface(font);
        view.setPadding(48, 32, 48, 32);
        view.setGravity(Gravity.START);
        return view;
    }
}

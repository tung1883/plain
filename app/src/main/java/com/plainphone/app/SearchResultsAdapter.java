package com.plainphone.app;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

class SearchResultsAdapter extends BaseAdapter {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_RESULT = 1;

    static class Header {
        final SearchResult.Kind kind; // null when label is set directly (no collapse toggle)
        final String label;
        final boolean collapsed;

        final int hiddenCount;

        Header(SearchResult.Kind kind, boolean collapsed, int hiddenCount) {
            this.kind = kind;
            this.label = null;
            this.collapsed = collapsed;
            this.hiddenCount = hiddenCount;
        }

        /** A plain, non-collapsible section header — e.g. a {@link ServicePanel}'s
         *  "My open PRs" / "Actions" groupings. */
        Header(String label) {
            this.kind = null;
            this.label = label;
            this.collapsed = false;
            this.hiddenCount = 0;
        }
    }

    private final Context context;

    private final List<Object> rows;
    private Typeface typeface;

    SearchResultsAdapter(Context context, List<Object> rows, Typeface typeface) {
        this.context = context;
        this.rows = rows;
        this.typeface = typeface;
    }

    @Override
    public int getCount() {
        return rows.size();
    }

    @Override
    public Object getItem(int position) {
        return rows.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    SearchResult resultAt(int position) {
        Object row = rows.get(position);
        return row instanceof SearchResult ? (SearchResult) row : null;
    }

    Header headerAt(int position) {
        Object row = rows.get(position);
        return row instanceof Header ? (Header) row : null;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position) instanceof SearchResult ? TYPE_RESULT : TYPE_HEADER;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Object row = rows.get(position);
        return row instanceof SearchResult
                ? resultView((SearchResult) row, convertView)
                : headerView((Header) row, convertView);
    }

    private View headerView(Header header, View convertView) {
        LinearLayout view = convertView instanceof LinearLayout
                && convertView.getTag() == Boolean.TRUE
                ? (LinearLayout) convertView
                : newHeaderView();

        TextView label = (TextView) view.getChildAt(0);
        TextView toggle = (TextView) view.getChildAt(1);

        String text = (header.label != null ? header.label : header.kind.header).toUpperCase();
        if (header.collapsed && header.hiddenCount > 0) {
            text = text + "  (" + header.hiddenCount + ")";
        }
        label.setText(text);
        label.setTypeface(typeface);

        toggle.setVisibility(header.kind == null ? View.GONE : View.VISIBLE);
        toggle.setText(header.collapsed ? "+" : "−");
        toggle.setTypeface(typeface);
        return view;
    }

    private LinearLayout newHeaderView() {
        LinearLayout view = new LinearLayout(context);
        view.setOrientation(LinearLayout.HORIZONTAL);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setBackground(rowBackground());
        view.setPadding(48, 36, 48, 12);

        view.setTag(Boolean.TRUE);

        TextView label = new TextView(context);
        label.setTextColor(Color.GRAY);
        label.setTextSize(13);
        label.setLetterSpacing(0.15f);
        view.addView(label, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView toggle = new TextView(context);
        toggle.setTextColor(Color.GRAY);
        toggle.setTextSize(18);
        toggle.setGravity(Gravity.END);
        view.addView(toggle, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        return view;
    }

    private View resultView(SearchResult result, View convertView) {
        LinearLayout view = convertView instanceof LinearLayout
                && convertView.getTag() == null
                ? (LinearLayout) convertView
                : newResultView();

        TextView marker = (TextView) view.getChildAt(0);
        LinearLayout textCol = (LinearLayout) view.getChildAt(1);
        TextView title = (TextView) textCol.getChildAt(0);
        TextView subtitle = (TextView) textCol.getChildAt(1);
        Button action = (Button) view.getChildAt(2);
        Button action2 = (Button) view.getChildAt(3);

        if (result.showCheck) {
            marker.setVisibility(View.VISIBLE);
            marker.setText(result.checked ? "[x]" : "[ ]");
            marker.setTextColor(result.checked ? Color.WHITE : Color.GRAY);
            marker.setTypeface(typeface);
        } else if (result.liveDot) {
            marker.setVisibility(View.VISIBLE);
            marker.setText("●");
            marker.setTextColor(Color.WHITE);
            marker.setTypeface(typeface);
        } else {
            marker.setVisibility(View.GONE);
        }

        if (result.actionLabel != null) {
            action.setVisibility(View.VISIBLE);
            action.setText(result.actionLabel);
            action.setTypeface(typeface);
            action.setTextSize("❚❚".equals(result.actionLabel) ? 13 : 17);
            action.setTextColor("■".equals(result.actionLabel) ? 0xFFE84C3D : Color.WHITE);
            action.setOnClickListener(v -> result.runAction());
        } else {
            action.setVisibility(View.GONE);
            action.setOnClickListener(null);
        }
        if (result.actionLabel2 != null) {
            action2.setVisibility(View.VISIBLE);
            action2.setText(result.actionLabel2);
            action2.setTypeface(typeface);
            action2.setTextSize("❚❚".equals(result.actionLabel2) ? 13 : 17);
            action2.setTextColor("■".equals(result.actionLabel2) ? 0xFFE84C3D : Color.WHITE);
            action2.setOnClickListener(v -> result.runAction2());
        } else {
            action2.setVisibility(View.GONE);
            action2.setOnClickListener(null);
        }

        title.setText(result.title);
        title.setTypeface(typeface);
        int baseFlags = title.getPaintFlags() & ~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG;
        if (result.strike) {
            title.setPaintFlags(baseFlags | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
            title.setTextColor(Color.GRAY);
        } else {
            title.setPaintFlags(baseFlags);
            title.setTextColor(Color.WHITE);
        }
        if (result.subtitle == null) {
            subtitle.setVisibility(View.GONE);
        } else {
            subtitle.setText(result.subtitle);
            subtitle.setTypeface(typeface);
            // A "\n" in the subtitle means separate lines (e.g. a sync pair's
            // schedule, then its status); otherwise one line, cut in the middle.
            int lines = 1;
            for (int i = 0; i < result.subtitle.length(); i++) {
                if (result.subtitle.charAt(i) == '\n') lines++;
            }
            if (lines > 1) {
                subtitle.setSingleLine(false);
                subtitle.setMaxLines(lines);
                subtitle.setEllipsize(android.text.TextUtils.TruncateAt.END);
            } else {
                subtitle.setSingleLine(true);
                subtitle.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
            }
            subtitle.setVisibility(View.VISIBLE);
        }

        int pad = result.subtitle == null ? 40 : 28;
        view.setPadding(result.showCheck ? 30 : 48, pad, 48, pad);
        return view;
    }

    /** A row's inline control (Pause / Play / Stop) — drawn on canvas rather than a text
     *  glyph, in a fixed-size box so it lines up with its neighbor regardless of the app font. */
    private Button actionButton(Context context) {
        Button b = new Button(context);
        b.setTextColor(Color.WHITE);
        b.setTextSize(17);
        b.setAllCaps(false);
        b.setBackground(null);
        b.setPadding(0, 0, 0, 0);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        b.setIncludeFontPadding(false);
        b.setGravity(Gravity.CENTER);
        b.setVisibility(View.GONE);
        // A focusable descendant otherwise steals the whole row's click handling from
        // ListView's OnItemClickListener — the row stops responding everywhere but the button.
        b.setFocusable(false);
        b.setFocusableInTouchMode(false);
        return b;
    }

    private LinearLayout.LayoutParams actionParams(Context context, int leftMargin) {
        int box = UiKit.dp(context, 40);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(box, box);
        lp.leftMargin = leftMargin;
        return lp;
    }

    private LinearLayout newResultView() {
        LinearLayout view = new LinearLayout(context);
        view.setOrientation(LinearLayout.HORIZONTAL);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setBackground(rowBackground());

        TextView marker = new TextView(context);
        marker.setTextSize(16);
        marker.setGravity(Gravity.CENTER);
        marker.setVisibility(View.GONE);
        LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(
                72, LinearLayout.LayoutParams.WRAP_CONTENT);
        mp.rightMargin = 8;
        view.addView(marker, mp);

        LinearLayout textCol = new LinearLayout(context);
        textCol.setOrientation(LinearLayout.VERTICAL);

        TextView title = new TextView(context);
        title.setTextColor(Color.WHITE);
        title.setTextSize(20);
        title.setGravity(Gravity.START);
        title.setSingleLine(true);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        textCol.addView(title, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView subtitle = new TextView(context);
        subtitle.setTextColor(Color.GRAY);
        subtitle.setTextSize(14);
        subtitle.setSingleLine(true);
        subtitle.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
        textCol.addView(subtitle, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        view.addView(textCol, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        Button action = actionButton(context);
        view.addView(action, actionParams(context, 20));

        Button action2 = actionButton(context);
        view.addView(action2, actionParams(context, 4));

        return view;
    }

    void setTypeface(Typeface typeface) {
        this.typeface = typeface;
        notifyDataSetChanged();
    }

    private Drawable rowBackground() {
        StateListDrawable drawable = new StateListDrawable();
        drawable.addState(new int[]{android.R.attr.state_pressed}, UiKit.pressedFill());
        drawable.setEnterFadeDuration(120);
        drawable.setExitFadeDuration(120);
        drawable.addState(new int[]{}, new ColorDrawable(Color.BLACK));
        return drawable;
    }
}


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
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

class SearchResultsAdapter extends BaseAdapter {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_RESULT = 1;

    static class Header {
        final SearchResult.Kind kind;
        final boolean collapsed;

        final int hiddenCount;

        Header(SearchResult.Kind kind, boolean collapsed, int hiddenCount) {
            this.kind = kind;
            this.collapsed = collapsed;
            this.hiddenCount = hiddenCount;
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

        String text = header.kind.header.toUpperCase();
        if (header.collapsed && header.hiddenCount > 0) {
            text = text + "  (" + header.hiddenCount + ")";
        }
        label.setText(text);
        label.setTypeface(typeface);

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

        TextView title = (TextView) view.getChildAt(0);
        TextView subtitle = (TextView) view.getChildAt(1);

        title.setText(result.title);
        title.setTypeface(typeface);
        if (result.subtitle == null) {
            subtitle.setVisibility(View.GONE);
        } else {
            subtitle.setText(result.subtitle);
            subtitle.setTypeface(typeface);
            subtitle.setVisibility(View.VISIBLE);
        }

        view.setPadding(48, result.subtitle == null ? 40 : 28, 48,
                result.subtitle == null ? 40 : 28);
        return view;
    }

    private LinearLayout newResultView() {
        LinearLayout view = new LinearLayout(context);
        view.setOrientation(LinearLayout.VERTICAL);
        view.setBackground(rowBackground());

        TextView title = new TextView(context);
        title.setTextColor(Color.WHITE);
        title.setTextSize(20);
        title.setGravity(Gravity.START);
        title.setSingleLine(true);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        view.addView(title, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView subtitle = new TextView(context);
        subtitle.setTextColor(Color.GRAY);
        subtitle.setTextSize(14);
        subtitle.setSingleLine(true);
        subtitle.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
        view.addView(subtitle, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        return view;
    }

    void setTypeface(Typeface typeface) {
        this.typeface = typeface;
        notifyDataSetChanged();
    }

    private Drawable rowBackground() {
        StateListDrawable drawable = new StateListDrawable();
        drawable.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Color.DKGRAY));
        drawable.addState(new int[]{}, new ColorDrawable(Color.BLACK));
        return drawable;
    }
}


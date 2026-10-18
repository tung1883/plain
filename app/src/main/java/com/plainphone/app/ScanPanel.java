package com.plainphone.app;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Read text from a photo: choose one with the button, review what was found, then insert it. */
final class ScanPanel extends LinearLayout {

    private final KeyboardRoot.Host host;
    private final LinearLayout drop;
    private final TextView result, choose, insert;
    private String text = "";

    ScanPanel(Context ctx, KeyboardRoot.Host host) {
        super(ctx);
        this.host = host;
        setOrientation(VERTICAL);

        LayoutParams captionLp = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        captionLp.leftMargin = PanelKit.dp(ctx, 4);
        captionLp.bottomMargin = PanelKit.dp(ctx, 4);
        addView(PanelKit.caption(ctx, "TEXT FROM A PHOTO"), captionLp);

        // empty: a dashed drop zone with the scan icon
        drop = new LinearLayout(ctx);
        drop.setOrientation(VERTICAL);
        drop.setGravity(Gravity.CENTER);
        GradientDrawable dashed = new GradientDrawable();
        dashed.setColor(0x00000000);
        dashed.setCornerRadius(PanelKit.dp(ctx, 12));
        dashed.setStroke(PanelKit.dp(ctx, 1), 0xFF333333, PanelKit.dp(ctx, 5), PanelKit.dp(ctx, 4));
        drop.setBackground(dashed);
        PanelKit.IconView icon = new PanelKit.IconView(ctx, KeyIcons.SCAN);
        drop.addView(icon, new LayoutParams(PanelKit.dp(ctx, 64), PanelKit.dp(ctx, 44)));
        TextView hint = new TextView(ctx);
        hint.setText("Pick a photo and the text in it\nshows up here to review.");
        hint.setTextSize(12);
        hint.setGravity(Gravity.CENTER);
        hint.setTextColor(PanelKit.DIM);
        hint.setTypeface(Fonts.cascadiaMono(ctx));
        drop.addView(hint);
        addView(drop, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        // found: the text in a card
        result = new TextView(ctx);
        result.setTextSize(13);
        result.setTextColor(0xFFFFFFFF);
        result.setTypeface(Fonts.cascadiaMono(ctx));
        result.setPadding(PanelKit.dp(ctx, 12), PanelKit.dp(ctx, 10), PanelKit.dp(ctx, 12), PanelKit.dp(ctx, 10));
        result.setBackground(PanelKit.card(ctx));
        result.setMovementMethod(new android.text.method.ScrollingMovementMethod());
        addView(result, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout acts = new LinearLayout(ctx);
        choose = button(ctx, "Choose photo", true, v -> host.scanText());
        insert = button(ctx, "Insert", false, v -> {
            if (text.isEmpty()) return;
            host.insertText(text);
            setResult("");
            host.closePanel();
        });
        acts.addView(choose);
        acts.addView(insert);
        LayoutParams actsLp = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        actsLp.topMargin = PanelKit.dp(ctx, 6);
        addView(acts, actsLp);
        setResult("");
    }

    private TextView button(Context ctx, String label, boolean on, OnClickListener l) {
        TextView t = PanelKit.chip(ctx, label, on, l);
        t.setTextSize(13);
        LayoutParams lp = new LayoutParams(0, PanelKit.dp(ctx, 40), 1f);
        lp.rightMargin = PanelKit.dp(ctx, 4);
        t.setLayoutParams(lp);
        return t;
    }

    /** Show the text found in a photo; empty text shows the drop zone again. */
    void setResult(String found) {
        text = found == null ? "" : found;
        boolean has = !text.isEmpty();
        drop.setVisibility(has ? GONE : VISIBLE);
        result.setVisibility(has ? VISIBLE : GONE);
        result.setText(text);
        PanelKit.styleChip(getContext(), choose, !has);
        PanelKit.styleChip(getContext(), insert, has);
    }
}

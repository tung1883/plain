package com.plainphone.app;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Read text from a photo: choose one with the button, review what was found, then insert it. */
final class ScanPanel extends LinearLayout {

    private final KeyboardRoot.Host host;
    private final TextView result;
    private String text = "";

    ScanPanel(Context ctx, KeyboardRoot.Host host) {
        super(ctx);
        this.host = host;
        setOrientation(VERTICAL);

        LayoutParams captionLp = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        captionLp.leftMargin = PanelKit.dp(ctx, 4);
        captionLp.bottomMargin = PanelKit.dp(ctx, 4);
        addView(PanelKit.caption(ctx, "TEXT FROM A PHOTO"), captionLp);

        result = new TextView(ctx);
        result.setTextSize(13);
        result.setTypeface(Fonts.cascadiaMono(ctx));
        result.setPadding(PanelKit.dp(ctx, 10), PanelKit.dp(ctx, 8), PanelKit.dp(ctx, 10), PanelKit.dp(ctx, 8));
        result.setBackground(UiKit.rounded(ctx, PanelKit.KEY, PanelKit.LINE, 1f, 8));
        result.setMovementMethod(new android.text.method.ScrollingMovementMethod());
        addView(result, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout acts = new LinearLayout(ctx);
        acts.addView(button(ctx, "Choose photo", true, v -> host.scanText()));
        acts.addView(button(ctx, "Insert", false, v -> {
            if (text.isEmpty()) return;
            host.insertText(text);
            setResult("");
            host.closePanel();
        }));
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

    /** Show the text found in a photo; empty text shows the hint again. */
    void setResult(String found) {
        text = found == null ? "" : found;
        if (text.isEmpty()) {
            result.setText("Tap Choose photo, pick an image, and the text found in it shows here.");
            result.setTextColor(PanelKit.DIM);
        } else {
            result.setText(text);
            result.setTextColor(0xFFFFFFFF);
        }
    }
}

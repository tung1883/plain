package com.plainphone.app;

import android.content.Context;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.SuperscriptSpan;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;
import java.util.function.IntConsumer;

/**
 * The strip above the keys: the word being composed or the likely words, one tap to take one. A
 * fixed height, so the keyboard does not jump when it has nothing to show.
 */
final class CandidateBar extends HorizontalScrollView {

    static final int HEIGHT_DP = 36;

    private final LinearLayout row;
    private final IntConsumer pick;

    CandidateBar(Context ctx, IntConsumer pick) {
        super(ctx);
        this.pick = pick;
        setHorizontalScrollBarEnabled(false);
        row = new LinearLayout(ctx);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(PanelKit.dp(ctx, 3), 0, PanelKit.dp(ctx, 3), 0);
        addView(row, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    /**
     * Show {@code words} (with {@code tags} for words of another language); when there are none,
     * the dim {@code note}, else {@code hint}.
     */
    void set(List<String> words, List<String> tags, String note, String hint) {
        Context ctx = getContext();
        row.removeAllViews();
        scrollTo(0, 0);
        if (words.isEmpty()) {
            String t = note.isEmpty() ? hint : note;
            if (!t.isEmpty()) row.addView(label(ctx, t, PanelKit.DIM, 11.5f, 8));
            return;
        }
        for (int i = 0; i < words.size(); i++) {
            final int idx = i;
            String tag = i < tags.size() ? tags.get(i) : "";
            CharSequence text = words.get(i);
            if (!tag.isEmpty()) {
                SpannableString sp = new SpannableString(words.get(i) + " " + tag);
                int from = words.get(i).length() + 1;
                sp.setSpan(new RelativeSizeSpan(0.5f), from, sp.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                sp.setSpan(new SuperscriptSpan(), from, sp.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                text = sp;
            }
            TextView t = label(ctx, text, i == 0 ? Color.WHITE : PanelKit.FG, 17f, 11);
            if (i == 0) t.setBackground(UiKit.pressable(ctx, PanelKit.KEY, PanelKit.DOWN, PanelKit.KEY, 0f, 8));
            else t.setBackground(UiKit.pressable(ctx, 0x00000000, PanelKit.DOWN, 0x00000000, 0f, 8));
            t.setOnClickListener(v -> pick.accept(idx));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, PanelKit.dp(ctx, 30));
            lp.rightMargin = PanelKit.dp(ctx, 2);
            row.addView(t, lp);
        }
    }

    private static TextView label(Context ctx, CharSequence text, int color, float size, int padDp) {
        TextView t = new TextView(ctx);
        t.setText(text);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setTypeface(Fonts.cascadiaMono(ctx));
        t.setSingleLine(true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(PanelKit.dp(ctx, padDp), 0, PanelKit.dp(ctx, padDp), 0);
        return t;
    }
}

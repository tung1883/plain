package com.plainphone.app;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.languageid.LanguageIdentification;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Translate the field's text (or the selection) on the device with ML Kit. The source language is
 * detected by default; each language pair downloads its model once and then works offline.
 * Replace swaps the field text for the result, Insert adds it after.
 */
final class TranslatePanel extends FrameLayout {

    private static final String FROM_KEY = "tr_from", TO_KEY = "tr_to", DETECT_KEY = "tr_detect";

    private final KeyboardRoot.Host host;
    private final ComposeSession.Starter composer;
    private final LinearLayout main;
    private final ScrollView picker;
    private final LinearLayout pickerList;

    private final TextView fromChip, toChip, status, source, result, sourceCaption, fieldChip;
    private final PanelKit.IconView swap;

    private boolean detect;
    private String from, to, detected, translated;
    private String override;    // text typed into the panel itself; null = use the app's field
    private Translator translator;
    private int generation;     // drops results of a request that a newer one replaced

    TranslatePanel(Context ctx, KeyboardRoot.Host host, ComposeSession.Starter composer) {
        super(ctx);
        this.host = host;
        this.composer = composer;
        detect = PanelKit.prefs(ctx).getBoolean(DETECT_KEY, true);
        from = PanelKit.prefs(ctx).getString(FROM_KEY, TranslateLanguage.ENGLISH);
        to = PanelKit.prefs(ctx).getString(TO_KEY, TranslateLanguage.VIETNAMESE);

        main = new LinearLayout(ctx);
        main.setOrientation(LinearLayout.VERTICAL);

        LinearLayout langs = new LinearLayout(ctx);
        langs.setGravity(Gravity.CENTER_VERTICAL);
        fromChip = PanelKit.chip(ctx, "", false, v -> pick(true));
        toChip = PanelKit.chip(ctx, "", true, v -> pick(false));
        swap = new PanelKit.IconView(ctx, KeyIcons.SWAP);
        swap.setOnClickListener(v -> {
            String t = detect && detected != null ? detected : from;
            from = to;
            to = t;
            detect = false;
            save();
            run();
        });
        langs.addView(fromChip, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        langs.addView(swap, new LinearLayout.LayoutParams(PanelKit.dp(ctx, 40), PanelKit.dp(ctx, 32)));
        langs.addView(toChip, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        main.addView(langs, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // one card: the source on top, the translation below a hairline, the status last
        LinearLayout card = new LinearLayout(ctx);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(PanelKit.card(ctx));
        sourceCaption = PanelKit.caption(ctx, "");
        sourceCaption.setTextSize(9);
        sourceCaption.setPadding(PanelKit.dp(ctx, 12), PanelKit.dp(ctx, 6), PanelKit.dp(ctx, 12), 0);
        card.addView(sourceCaption);
        source = box(ctx, false);
        source.setOnClickListener(v -> composer.start(typing, override != null ? override : host.fieldText()));
        card.addView(source, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        LinearLayout divider = new LinearLayout(ctx);
        divider.setGravity(Gravity.CENTER_VERTICAL);
        View hair = new View(ctx);
        hair.setBackgroundColor(PanelKit.LINE);
        divider.addView(hair, new LinearLayout.LayoutParams(0, 1, 1f));
        TextView tcap = PanelKit.caption(ctx, "TRANSLATION");
        tcap.setTextSize(9);
        tcap.setPadding(PanelKit.dp(ctx, 8), 0, PanelKit.dp(ctx, 12), 0);
        divider.addView(tcap);
        card.addView(divider, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        result = box(ctx, true);
        card.addView(result, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.3f));
        status = PanelKit.caption(ctx, "");
        status.setTextSize(9.5f);
        status.setPadding(PanelKit.dp(ctx, 12), 0, PanelKit.dp(ctx, 12), PanelKit.dp(ctx, 6));
        card.addView(status);
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        cardLp.topMargin = PanelKit.dp(ctx, 4);
        main.addView(card, cardLp);

        LinearLayout acts = new LinearLayout(ctx);
        acts.addView(chipWide(ctx, "Insert", false, v -> apply(false)));
        acts.addView(chipWide(ctx, "Replace", true, v -> apply(true)));
        fieldChip = chipWide(ctx, "Field", false, v -> {
            override = null;
            run();
        });
        acts.addView(fieldChip);
        LinearLayout.LayoutParams actsLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        actsLp.topMargin = PanelKit.dp(ctx, 4);
        main.addView(acts, actsLp);
        addView(main, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        picker = new ScrollView(ctx);
        picker.setVerticalScrollBarEnabled(false);
        picker.setBackgroundColor(0xFF0A0A0A);
        pickerList = new LinearLayout(ctx);
        pickerList.setOrientation(LinearLayout.VERTICAL);
        picker.addView(pickerList, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        picker.setVisibility(GONE);
        addView(picker, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private LinearLayout.LayoutParams captionLp(Context ctx) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = PanelKit.dp(ctx, 4);
        lp.bottomMargin = PanelKit.dp(ctx, 2);
        return lp;
    }

    private TextView chipWide(Context ctx, String text, boolean on, OnClickListener l) {
        TextView t = PanelKit.chip(ctx, text, on, l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.rightMargin = PanelKit.dp(ctx, 4);
        t.setLayoutParams(lp);
        return t;
    }

    private TextView box(Context ctx, boolean strong) {
        TextView t = new TextView(ctx);
        t.setTextSize(strong ? 14 : 13);
        t.setTypeface(Fonts.cascadiaMono(ctx));
        t.setTextColor(strong ? 0xFFFFFFFF : 0xFF9A9A9A);
        t.setPadding(PanelKit.dp(ctx, 12), PanelKit.dp(ctx, 6), PanelKit.dp(ctx, 12), PanelKit.dp(ctx, 6));
        t.setMovementMethod(new android.text.method.ScrollingMovementMethod());
        return t;
    }

    /** Typing the text to translate: nothing happens until Done. */
    private final ComposeSession typing = new ComposeSession() {
        @Override
        public String hint() {
            return "type the text to translate";
        }

        @Override
        public void onChange(String text) { }

        @Override
        public void onDone(String text) {
            setOverride(text.isEmpty() ? null : text);
            refresh();
        }
    };

    // --------------------------------------------------------------- lifecycle

    /** Called each time the panel is shown. */
    void refresh() {
        picker.setVisibility(GONE);
        run();
    }

    void release() {
        generation++;
        closeTranslator();
    }

    /** Translate this text, typed on the keyboard, instead of the app's field. */
    void setOverride(String text) {
        override = text;
    }

    void clearOverride() {
        override = null;
    }

    private void closeTranslator() {
        if (translator != null) {
            translator.close();
            translator = null;
        }
    }

    private void save() {
        PanelKit.prefs(getContext()).edit().putString(FROM_KEY, from).putString(TO_KEY, to)
                .putBoolean(DETECT_KEY, detect).apply();
    }

    private void showChips() {
        fromChip.setText(detect ? (detected != null ? "Auto: " + name(detected) : "Detect") : name(from));
        toChip.setText(name(to));
    }

    // -------------------------------------------------------------- translating

    private void run() {
        detected = null;
        String text = override != null ? override : host.fieldText();
        source.setText(text);
        sourceCaption.setText(override != null ? "YOUR TEXT · TAP TO EDIT" : "FIELD TEXT · TAP TO WRITE YOUR OWN");
        fieldChip.setVisibility(override != null ? VISIBLE : GONE);
        translated = null;
        result.setText("");
        int mine = ++generation;
        closeTranslator();
        showChips();
        if (text == null || text.trim().isEmpty()) {
            status.setText("Type or select some text first.");
            return;
        }
        if (!detect) {
            translate(text, from, mine);
            return;
        }
        status.setText("Detecting language...");
        LanguageIdentification.getClient().identifyLanguage(text)
                .addOnSuccessListener(tag -> {
                    if (mine != generation) return;
                    String code = "und".equals(tag) ? null : TranslateLanguage.fromLanguageTag(tag);
                    if (code == null) {
                        // not sure, or not a language we can translate: keep the last manual source
                        status.setText("Could not detect the language; using " + name(from) + ".");
                        translate(text, from, mine);
                        return;
                    }
                    detected = code;
                    showChips();
                    translate(text, code, mine);
                })
                .addOnFailureListener(e -> {
                    if (mine != generation) return;
                    status.setText("Detection failed; using " + name(from) + ".");
                    translate(text, from, mine);
                });
    }

    private void translate(String text, String src, int mine) {
        if (src.equals(to)) {
            translated = text;
            result.setText(text);
            status.setText("Already in " + name(to) + ".");
            return;
        }
        status.setText("Loading " + name(src) + " to " + name(to) + " model...");
        Translator t = Translation.getClient(new TranslatorOptions.Builder()
                .setSourceLanguage(src).setTargetLanguage(to).build());
        closeTranslator();
        translator = t;
        t.downloadModelIfNeeded(new DownloadConditions.Builder().build())
                .addOnSuccessListener(v -> {
                    if (mine != generation) return;
                    status.setText("Translating...");
                    t.translate(text)
                            .addOnSuccessListener(out -> {
                                if (mine != generation) return;
                                translated = out;
                                result.setText(out);
                                status.setText("");
                            })
                            .addOnFailureListener(e -> {
                                if (mine == generation) status.setText("Translate failed.");
                            });
                })
                .addOnFailureListener(e -> {
                    if (mine == generation) status.setText("Model download failed. Check the connection.");
                });
    }

    private void apply(boolean replace) {
        if (translated == null || translated.isEmpty()) return;
        if (replace) host.replaceFieldText(translated);
        else host.insertText(translated);
        host.closePanel();
    }

    // ------------------------------------------------------------------ picker

    private static String name(String code) {
        String n = new Locale(code).getDisplayLanguage(Locale.ENGLISH);
        return n.isEmpty() ? code : Character.toUpperCase(n.charAt(0)) + n.substring(1);
    }

    private void pick(boolean source) {
        Context ctx = getContext();
        List<String> codes = new ArrayList<>(TranslateLanguage.getAllLanguages());
        Collections.sort(codes, (a, b) -> name(a).compareToIgnoreCase(name(b)));
        pickerList.removeAllViews();
        if (source) {
            pickerList.addView(pickerRow(ctx, "Detect", detect, v -> {
                detect = true;
                save();
                picker.setVisibility(GONE);
                run();
            }));
        }
        for (String code : codes) {
            boolean current = code.equals(source ? from : to) && (!source || !detect);
            pickerList.addView(pickerRow(ctx, name(code), current, (View v) -> {
                if (source) {
                    from = code;
                    detect = false;
                } else {
                    to = code;
                }
                save();
                picker.setVisibility(GONE);
                run();
            }));
        }
        picker.scrollTo(0, 0);
        picker.setVisibility(VISIBLE);
    }

    private View pickerRow(Context ctx, String label, boolean on, OnClickListener l) {
        TextView row = PanelKit.chip(ctx, label, on, l);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = PanelKit.dp(ctx, 3);
        row.setLayoutParams(lp);
        return row;
    }
}

package com.plainphone.app;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.mlkit.common.MlKitException;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.common.model.RemoteModelManager;
import com.google.mlkit.vision.digitalink.DigitalInkRecognition;
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModel;
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModelIdentifier;
import com.google.mlkit.vision.digitalink.DigitalInkRecognizer;
import com.google.mlkit.vision.digitalink.DigitalInkRecognizerOptions;
import com.google.mlkit.vision.digitalink.RecognitionCandidate;

/**
 * Handwriting input: write on the pad, pick a candidate, it is typed into the field. Recognition
 * runs on the device; each language downloads its model once.
 */
final class InkPanel extends LinearLayout {

    private static final String LANG_KEY = "ink_lang";
    private static final long PAUSE_MS = 600;

    /** label, then BCP-47 tags to try in order (the first the library knows is used). */
    private static final String[][] LANGS = {
            {"Japanese", "ja"},
            {"Chinese (Simplified)", "zh-Hani-CN", "zh-CN", "zh"},
            {"Chinese (Traditional)", "zh-Hani-TW", "zh-TW", "zh-Hant"},
            {"Korean", "ko"},
            {"Vietnamese", "vi"},
            {"English", "en"},
            {"German", "de"},
    };

    private final KeyboardRoot.Host host;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final HandwritingView pad;
    private final LinearLayout candidates;
    private final TextView langChip, status;

    private int lang;
    private DigitalInkRecognizer recognizer;
    private boolean modelReady;
    private int generation;
    private final Runnable recognize = this::recognizeNow;

    InkPanel(Context ctx, KeyboardRoot.Host host) {
        super(ctx);
        this.host = host;
        setOrientation(VERTICAL);
        lang = Math.max(0, Math.min(LANGS.length - 1, PanelKit.prefs(ctx).getInt(LANG_KEY, 0)));

        // one line: the language button, then the candidates (or the model status when there are none)
        LinearLayout head = new LinearLayout(ctx);
        head.setGravity(Gravity.CENTER_VERTICAL);
        langChip = PanelKit.chipSmall(ctx, "", true, v -> nextLanguage());
        head.addView(langChip, new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        FrameLayout slot = new FrameLayout(ctx);
        status = PanelKit.caption(ctx, "");
        status.setSingleLine(true);
        status.setEllipsize(android.text.TextUtils.TruncateAt.END);
        status.setPadding(PanelKit.dp(ctx, 8), 0, 0, 0);
        slot.addView(status, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER_VERTICAL));
        HorizontalScrollView strip = new HorizontalScrollView(ctx);
        strip.setHorizontalScrollBarEnabled(false);
        candidates = new LinearLayout(ctx);
        candidates.setGravity(Gravity.CENTER_VERTICAL);
        strip.addView(candidates, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, PanelKit.dp(ctx, 36)));
        slot.addView(strip, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, PanelKit.dp(ctx, 36)));
        LayoutParams slotLp = new LayoutParams(0, PanelKit.dp(ctx, 36), 1f);
        slotLp.leftMargin = PanelKit.dp(ctx, 4);
        head.addView(slot, slotLp);
        addView(head, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, PanelKit.dp(ctx, 38)));
        View under = new View(ctx);
        under.setBackgroundColor(PanelKit.LINE);
        addView(under, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));

        pad = new HandwritingView(ctx);
        pad.setListener(new HandwritingView.Listener() {
            @Override
            public void onStrokeStart() {
                handler.removeCallbacks(recognize);
            }

            @Override
            public void onStrokeEnd() {
                handler.postDelayed(recognize, PAUSE_MS);
            }
        });
        LayoutParams padLp = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        padLp.topMargin = PanelKit.dp(ctx, 2);
        addView(pad, padLp);

        LinearLayout acts = new LinearLayout(ctx);
        acts.addView(action(ctx, "Space", v -> host.insertText(" ")));
        PanelKit.IconView bksp = new PanelKit.IconView(ctx, KeyIcons.BACKSPACE);
        bksp.setBackground(UiKit.pressable(ctx, PanelKit.KEY, PanelKit.DOWN, PanelKit.LINE, 1f, 9));
        bksp.setOnClickListener(v -> {
            if (!pad.isEmpty()) clearInk();
            else host.sendKey(KeyEvent.KEYCODE_DEL);
        });
        LayoutParams b = new LayoutParams(0, PanelKit.dp(ctx, 34), 1f);
        b.rightMargin = PanelKit.dp(ctx, 4);
        acts.addView(bksp, b);
        PanelKit.IconView enter = new PanelKit.IconView(ctx, KeyIcons.ENTER);
        enter.setBackground(UiKit.pressable(ctx, PanelKit.KEY, PanelKit.DOWN, PanelKit.LINE, 1f, 9));
        enter.setOnClickListener(v -> host.sendKey(KeyEvent.KEYCODE_ENTER));
        LayoutParams eLp = new LayoutParams(0, PanelKit.dp(ctx, 34), 1f);
        eLp.rightMargin = PanelKit.dp(ctx, 4);
        acts.addView(enter, eLp);
        acts.addView(action(ctx, "Clear", v -> clearInk()));
        LayoutParams actsLp = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        actsLp.topMargin = PanelKit.dp(ctx, 4);
        addView(acts, actsLp);
    }

    private TextView action(Context ctx, String text, OnClickListener l) {
        TextView t = PanelKit.chip(ctx, text, false, l);
        LayoutParams lp = new LayoutParams(0, PanelKit.dp(ctx, 34), 1f);
        lp.rightMargin = PanelKit.dp(ctx, 4);
        t.setLayoutParams(lp);
        return t;
    }

    // --------------------------------------------------------------- lifecycle

    /** Called each time the panel is shown. */
    void refresh() {
        pad.clear();
        showCandidates(null);
        loadModel();
    }

    void release() {
        generation++;
        handler.removeCallbacks(recognize);
        closeRecognizer();
    }

    private void closeRecognizer() {
        if (recognizer != null) {
            recognizer.close();
            recognizer = null;
        }
        modelReady = false;
    }

    private void nextLanguage() {
        lang = (lang + 1) % LANGS.length;
        PanelKit.prefs(getContext()).edit().putInt(LANG_KEY, lang).apply();
        pad.clear();
        showCandidates(null);
        loadModel();
    }

    private void clearInk() {
        handler.removeCallbacks(recognize);
        generation++;
        pad.clear();
        showCandidates(null);
    }

    // ------------------------------------------------------------------- model

    private void loadModel() {
        closeRecognizer();
        langChip.setText(LANGS[lang][0] + " ▾");
        int mine = ++generation;
        DigitalInkRecognitionModel model = null;
        try {
            for (int i = 1; i < LANGS[lang].length && model == null; i++) {
                DigitalInkRecognitionModelIdentifier id =
                        DigitalInkRecognitionModelIdentifier.fromLanguageTag(LANGS[lang][i]);
                if (id != null) model = DigitalInkRecognitionModel.builder(id).build();
            }
        } catch (MlKitException e) {
            model = null;
        }
        if (model == null) {
            status.setText("Not supported.");
            return;
        }
        final DigitalInkRecognitionModel m = model;
        status.setText("Checking model...");
        RemoteModelManager manager = RemoteModelManager.getInstance();
        manager.isModelDownloaded(m).addOnSuccessListener(have -> {
            if (mine != generation) return;
            if (have) {
                ready(m, mine);
                return;
            }
            status.setText("Downloading model...");
            manager.download(m, new DownloadConditions.Builder().build())
                    .addOnSuccessListener(v -> {
                        if (mine == generation) ready(m, mine);
                    })
                    .addOnFailureListener(e -> {
                        if (mine == generation) status.setText("Download failed. Check the connection.");
                    });
        }).addOnFailureListener(e -> {
            if (mine == generation) status.setText("Model check failed.");
        });
    }

    private void ready(DigitalInkRecognitionModel model, int mine) {
        if (mine != generation) return;
        recognizer = DigitalInkRecognition.getClient(DigitalInkRecognizerOptions.builder(model).build());
        modelReady = true;
        status.setText("");
        if (!pad.isEmpty()) recognizeNow();
    }

    // ------------------------------------------------------------- recognizing

    private void recognizeNow() {
        if (!modelReady || recognizer == null || pad.isEmpty()) return;
        int mine = generation;
        recognizer.recognize(pad.ink())
                .addOnSuccessListener(result -> {
                    if (mine != generation) return;
                    showCandidates(result.getCandidates());
                })
                .addOnFailureListener(e -> {
                    if (mine == generation) status.setText("Recognition failed.");
                });
    }

    private void showCandidates(java.util.List<RecognitionCandidate> list) {
        Context ctx = getContext();
        candidates.removeAllViews();
        if (list == null) return;
        int n = 0;
        for (RecognitionCandidate c : list) {
            if (n++ == 5) break;
            String text = c.getText();
            TextView chip = new TextView(ctx);   // plain text; the best match sits on a key fill
            chip.setText(text);
            chip.setTextSize(12);   // same as the Space / Clear buttons
            chip.setTextColor(Color.WHITE);
            chip.setTypeface(Fonts.cascadiaMono(ctx));   // the keyboard's font; CJK falls back to the system
            chip.setGravity(Gravity.CENTER);
            chip.setMinWidth(PanelKit.dp(ctx, 38));
            chip.setPadding(PanelKit.dp(ctx, 12), 0, PanelKit.dp(ctx, 12), 0);
            if (n == 1) chip.setBackground(UiKit.rounded(ctx, PanelKit.KEY, PanelKit.KEY, 0f, 8));
            chip.setOnClickListener(v -> {
                host.insertText(text);
                clearInk();
            });
            LayoutParams lp = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, PanelKit.dp(ctx, 32));
            lp.rightMargin = PanelKit.dp(ctx, 2);
            candidates.addView(chip, lp);
        }
    }
}

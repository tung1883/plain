package com.plainphone.app;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * The keyboard's whole input view: a thin toolbar (clipboard, emoji, translate) above a body that
 * shows either the keys or one of those panels. The body keeps the keys' height, so the keyboard
 * never jumps in size when a panel opens.
 */
final class KeyboardRoot extends LinearLayout {

    /** What the panels need from the keyboard service. */
    interface Host {
        void insertText(String s);

        /** The selected text, or else the text around the cursor. */
        String fieldText();

        /** Replace {@link #fieldText()} with {@code s}. */
        void replaceFieldText(String s);

        void closePanel();

        /** A key press with no modifiers (Del, Enter ...). */
        void sendKey(int keyCode);

        /** Pick a photo and type the text found in it. */
        void scanText();

        /** Ask for the camera permission (the keyboard cannot show the prompt itself). */
        void requestCameraPermission();

        /** A keyboard setting changed; re-apply it now. */
        void settingsChanged();

        /** Pick an image to add to the keyboard's own images. */
        void addSticker(String collection);

        /** Send one of the keyboard's own images into the field (or to the clipboard if it cannot). */
        void sendSticker(java.io.File image);

        /** Put text on the system clipboard, so the next paste gives it. */
        void copyText(String text);

        /** Put one of the keyboard's own images on the system clipboard. */
        void copyImage(java.io.File image);
    }

    private final DevKeyboardView keys;
    private final FrameLayout body;
    private final ClipboardPanel clip;
    private final EmojiPanel emoji;
    private final TranslatePanel translate;
    private final InkPanel ink;
    private final ScanPanel scan;
    private final KeyboardSettingsPanel settings;
    private final PanelKit.IconView[] icons = new PanelKit.IconView[6];
    private final PanelKit.IconView abc, resize;
    private final TextView readout;
    private final Runnable hideReadout = this::clearReadout;
    private float dragY0;
    private int dragPct0, dragPct;
    private final LinearLayout composeBar;
    private final TextView composeText;
    private final StringBuilder compose = new StringBuilder();
    private boolean composing;     // typing text for the Translate panel instead of the app's field
    private int open = -1;     // 0 clipboard, 1 emoji, 2 translate, 3 handwriting, 4 scan, 5 settings, -1 keys

    KeyboardRoot(Context ctx, DevKeyboardView.Listener keyListener, Host host) {
        super(ctx);
        setOrientation(VERTICAL);
        setBackgroundColor(0xFF0A0A0A);

        keys = new DevKeyboardView(ctx, new DevKeyboardView.Listener() {
            @Override
            public void text(String s) {
                if (!composing) {
                    keyListener.text(s);
                    return;
                }
                compose.append(s);
                drawCompose();
            }

            @Override
            public void key(int keyCode, int meta) {
                if (!composing) {
                    keyListener.key(keyCode, meta);
                } else if (keyCode == android.view.KeyEvent.KEYCODE_DEL && compose.length() > 0) {
                    compose.setLength(compose.offsetByCodePoints(compose.length(), -1));
                    drawCompose();
                }
            }

            @Override
            public void enter() {
                if (composing) finishCompose();
                else keyListener.enter();
            }

            @Override
            public void switchKeyboard() {
                keyListener.switchKeyboard();
            }
        });
        clip = new ClipboardPanel(ctx, host);
        emoji = new EmojiPanel(ctx, host);
        translate = new TranslatePanel(ctx, host, this::startCompose);
        ink = new InkPanel(ctx, host);
        scan = new ScanPanel(ctx, host);
        settings = new KeyboardSettingsPanel(ctx, host);

        LinearLayout bar = new LinearLayout(ctx);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(PanelKit.dp(ctx, 4), PanelKit.dp(ctx, 3), PanelKit.dp(ctx, 8), PanelKit.dp(ctx, 3));
        abc = new PanelKit.IconView(ctx, KeyIcons.KEYS);
        abc.setOnClickListener(v -> show(-1));
        LayoutParams abcLp = new LayoutParams(PanelKit.dp(ctx, 40), PanelKit.dp(ctx, 30));
        abcLp.rightMargin = PanelKit.dp(ctx, 6);
        bar.addView(abc, abcLp);

        int[] kinds = {KeyIcons.CLIPBOARD, KeyIcons.EMOJI, KeyIcons.TRANSLATE, KeyIcons.PEN, KeyIcons.SCAN, KeyIcons.SLIDERS};
        for (int i = 0; i < icons.length; i++) {
            final int which = i;
            icons[i] = new PanelKit.IconView(ctx, kinds[i]);
            icons[i].setOnClickListener(v -> show(open == which ? -1 : which));
        }
        for (int i = 0; i < 5; i++) {
            LayoutParams lp = new LayoutParams(PanelKit.dp(ctx, 38), PanelKit.dp(ctx, 30));
            lp.rightMargin = PanelKit.dp(ctx, 2);
            bar.addView(icons[i], lp);
        }
        readout = PanelKit.caption(ctx, "");
        readout.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        readout.setTextColor(Color.WHITE);
        readout.setPadding(0, 0, PanelKit.dp(ctx, 8), 0);
        bar.addView(readout, new LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        resize = new PanelKit.IconView(ctx, KeyIcons.RESIZE);
        resize.setOnTouchListener(this::dragHeight);
        LayoutParams rLp = new LayoutParams(PanelKit.dp(ctx, 38), PanelKit.dp(ctx, 30));
        rLp.rightMargin = PanelKit.dp(ctx, 2);
        bar.addView(resize, rLp);
        bar.addView(icons[5], new LayoutParams(PanelKit.dp(ctx, 38), PanelKit.dp(ctx, 30)));
        addView(bar, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        View hair = new View(ctx);
        hair.setBackgroundColor(PanelKit.LINE);
        addView(hair, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));

        composeBar = new LinearLayout(ctx);
        composeBar.setGravity(Gravity.CENTER_VERTICAL);
        composeBar.setPadding(PanelKit.dp(ctx, 6), PanelKit.dp(ctx, 4), PanelKit.dp(ctx, 6), PanelKit.dp(ctx, 4));
        composeText = new TextView(ctx);
        composeText.setTextSize(13);
        composeText.setTypeface(Fonts.cascadiaMono(ctx));
        composeText.setMaxLines(2);
        composeText.setMinHeight(PanelKit.dp(ctx, 36));
        composeText.setGravity(Gravity.CENTER_VERTICAL);
        composeText.setPadding(PanelKit.dp(ctx, 10), 0, PanelKit.dp(ctx, 10), 0);
        composeText.setBackground(UiKit.rounded(ctx, PanelKit.KEY, 0xFF555555, 1f, 8));
        composeBar.addView(composeText, new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView done = PanelKit.chip(ctx, "Done", true, v -> finishCompose());
        LayoutParams doneLp = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, PanelKit.dp(ctx, 36));
        doneLp.leftMargin = PanelKit.dp(ctx, 6);
        composeBar.addView(done, doneLp);
        composeBar.setVisibility(GONE);
        addView(composeBar, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        body = new FrameLayout(ctx);
        body.setPadding(PanelKit.dp(ctx, 3), PanelKit.dp(ctx, 3), PanelKit.dp(ctx, 3), PanelKit.dp(ctx, 3));
        body.addView(keys, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        for (View p : new View[]{clip, emoji, translate, ink, scan, settings}) {
            p.setVisibility(GONE);
            body.addView(p, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
        addView(body, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    /** Re-read settings and go back to the keys; call each time the keyboard is shown. */
    void refresh() {
        keys.refresh();
        show(-1);
        applyBodyHeight();
    }

    /** A setting changed in the settings panel: rebuild the keys and resize, staying on the panel. */
    void applySettings() {
        keys.refresh();
        applyBodyHeight();
    }

    private void clearReadout() {
        readout.setText("");
    }

    private void applyBodyHeight() {
        body.getLayoutParams().height = keys.heightPx() + body.getPaddingTop() + body.getPaddingBottom();
        body.requestLayout();
    }

    /** Hold the height button and drag: up = taller, down = shorter, in 1% steps (75..130). */
    private boolean dragHeight(View v, android.view.MotionEvent e) {
        float density = getResources().getDisplayMetrics().density;
        switch (e.getActionMasked()) {
            case android.view.MotionEvent.ACTION_DOWN:
                dragY0 = e.getRawY();
                dragPct0 = dragPct = Config.getKbHeightPct(getContext());
                getParent().requestDisallowInterceptTouchEvent(true);
                resize.setActive(true);
                // Hold the window at its tallest while dragging: only the keys redraw, so the
                // toolbar (and the finger on it) never moves. The window settles on release.
                body.getLayoutParams().height = keys.heightPxAt(130) + body.getPaddingTop() + body.getPaddingBottom();
                body.requestLayout();
                removeCallbacks(hideReadout);
                readout.setText(dragPct + "%");
                return true;
            case android.view.MotionEvent.ACTION_MOVE: {
                // 1% steps (about 3dp of height each) so the keys follow the finger
                float raw = dragPct0 + (dragY0 - e.getRawY()) / density * 0.35f;
                int pct = Math.max(75, Math.min(130, Math.round(raw)));
                if (pct != dragPct) {
                    dragPct = pct;
                    keys.setHeightPct(pct);
                    readout.setText(pct + "%");
                }
                return true;
            }
            case android.view.MotionEvent.ACTION_UP:
            case android.view.MotionEvent.ACTION_CANCEL:
                Config.setKbHeightPct(getContext(), dragPct);
                applyBodyHeight();
                resize.setActive(false);
                postDelayed(hideReadout, 600);
                return true;
            default:
                return true;
        }
    }

    // ---------------------------------------------------- typing for Translate

    /** Show the keys with a text bar above them; what is typed goes there, not to the app. */
    private void startCompose(String initial) {
        composing = true;
        compose.setLength(0);
        if (initial != null) compose.append(initial);
        translate.release();
        translate.setVisibility(GONE);
        keys.setVisibility(VISIBLE);
        composeBar.setVisibility(VISIBLE);
        drawCompose();
    }

    private void drawCompose() {
        if (compose.length() == 0) {
            composeText.setText("type the text to translate");
            composeText.setTextColor(PanelKit.DIM);
        } else {
            composeText.setText(compose.toString());
            composeText.setTextColor(0xFFFFFFFF);
        }
    }

    private void finishCompose() {
        composing = false;
        composeBar.setVisibility(GONE);
        keys.setVisibility(INVISIBLE);
        translate.setVisibility(VISIBLE);
        translate.setOverride(compose.length() == 0 ? null : compose.toString());
        translate.refresh();
    }

    /** Open the emoji panel on its image tab (after an image was added). */
    void showImages() {
        show(1);
        emoji.showCategory("Img");
    }

    /** Open the scan panel with text read from a photo. */
    void showScanResult(String text) {
        show(4);
        scan.setResult(text);
    }

    void showPanel(int which) {
        show(which);
    }

    /** True when Back has a step inside the open panel (close a viewer, leave select mode ...). */
    boolean canGoBack() {
        switch (open) {
            case 0: return clip.canGoBack();
            case 1: return emoji.canGoBack();
            case 4: return scan.canGoBack();
            default: return false;
        }
    }

    void goBack() {
        switch (open) {
            case 0: clip.goBack(); break;
            case 1: emoji.goBack(); break;
            case 4: scan.goBack(); break;
            default:
        }
    }

    void release() {
        translate.release();
        ink.release();
        clip.release();
        scan.release();
        emoji.release();
    }

    private void show(int which) {
        open = which;
        composing = false;
        composeBar.setVisibility(GONE);
        if (which != 2) translate.clearOverride();
        keys.setVisibility(which < 0 ? VISIBLE : INVISIBLE);
        clip.setVisibility(which == 0 ? VISIBLE : GONE);
        emoji.setVisibility(which == 1 ? VISIBLE : GONE);
        translate.setVisibility(which == 2 ? VISIBLE : GONE);
        ink.setVisibility(which == 3 ? VISIBLE : GONE);
        scan.setVisibility(which == 4 ? VISIBLE : GONE);
        settings.setVisibility(which == 5 ? VISIBLE : GONE);
        for (int i = 0; i < icons.length; i++) icons[i].setActive(i == which);
        abc.setActive(which < 0);
        resize.setVisibility(which < 0 ? VISIBLE : INVISIBLE);
        if (which == 0) clip.refresh();
        if (which == 1) emoji.refresh();
        if (which == 2) translate.refresh();
        else translate.release();
        if (which == 3) ink.refresh();
        else ink.release();
        if (which != 0) clip.release();
        if (which != 4) scan.release();
        if (which != 1) emoji.release();
        if (which == 5) settings.refresh();
    }
}

package com.plainphone.app;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

class UiKit {

    // Corner-radius scale (dp). Everything rounds through these — nothing invents its own.
    static final float R_XS = 6f;   // key-bar keys, sheet options, small controls
    static final float R_SM = 10f;  // buttons, inputs
    static final float R_MD = 14f;  // list containers, cards
    static final float R_LG = 20f;  // the one sheet (top corners only)
    static final float R_PILL = 999f;

    static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    /** A filled, optionally stroked rounded rect. {@code strokePx} is raw pixels
     *  (as the app's older GradientDrawables used); {@code radiusDp} is scaled. */
    static GradientDrawable rounded(Context c, int fill, int strokeColor, float strokePx, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        if (strokePx > 0) g.setStroke(Math.round(strokePx), strokeColor);
        g.setCornerRadius(dp(c, radiusDp));
        return g;
    }

    /** Normal + pressed rounded fills, for a tappable control. */
    static StateListDrawable pressable(Context c, int normalFill, int pressedFill,
                                       int strokeColor, float strokePx, float radiusDp) {
        StateListDrawable s = new StateListDrawable();
        s.addState(new int[]{android.R.attr.state_pressed},
                rounded(c, pressedFill, strokeColor, strokePx, radiusDp));
        s.addState(new int[]{}, rounded(c, normalFill, strokeColor, strokePx, radiusDp));
        return s;
    }

    /**
     * A vertical container that rounds its own corners and clips the flat rows
     * inside it — the "grouped list" look. Rows stay square; only the group
     * rounds. Hairlines between rows are the caller's job.
     */
    static LinearLayout roundedGroup(Context c) {
        LinearLayout g = new LinearLayout(c);
        g.setOrientation(LinearLayout.VERTICAL);
        g.setBackground(rounded(c, Color.BLACK, 0xFF2C2C2C, 1f, R_MD));
        clipRounded(c, g, R_MD);
        return g;
    }

    /** Clip a view's children to a rounded-rect outline (so flat rows keep the corners). */
    static void clipRounded(Context c, View v, float radiusDp) {
        final float r = dp(c, radiusDp);
        v.setClipToOutline(true);
        v.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View vv, Outline o) {
                o.setRoundRect(0, 0, vv.getWidth(), vv.getHeight(), r);
            }
        });
    }

    /** The left inset of body text on plain screens — the arrow lines up with it. */
    static final int BODY_INSET_PX = 48;

    /** One back affordance for every sub-screen: the "←" glyph flush with body text
     *  (the same arrow the PIN keypad uses for backspace). Same size everywhere. */
    static android.widget.TextView backButton(Context c, Runnable onClick) {
        android.widget.TextView b = new android.widget.TextView(c);
        b.setText("←");
        b.setTextColor(Color.WHITE);
        b.setTextSize(24);
        b.setTypeface(Fonts.current(c));
        b.setGravity(android.view.Gravity.CENTER_VERTICAL);
        b.setIncludeFontPadding(false);
        b.setPadding(BODY_INSET_PX - dp(c, 1), 0, dp(c, 12), 0);
        StateListDrawable press = new StateListDrawable();
        press.addState(new int[]{android.R.attr.state_pressed},
                new android.graphics.drawable.ColorDrawable(Color.DKGRAY));
        press.addState(new int[]{}, new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        b.setBackground(press);
        b.setOnClickListener(v -> onClick.run());
        return b;
    }

    static int backWidth(Context c) { return BODY_INSET_PX + dp(c, 44); }
    static int backHeight(Context c) { return dp(c, 52); }

    /** A "← Title" bar: left arrow (calls {@code onBackPressed()}) + screen title.
     *  The arrow's tip sits at {@link #BODY_INSET_PX}, flush with row text. */
    static android.widget.LinearLayout header(android.app.Activity a, String title) {
        android.widget.LinearLayout bar = new android.widget.LinearLayout(a);
        bar.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        bar.setGravity(android.view.Gravity.CENTER_VERTICAL);
        bar.setBackgroundColor(Color.BLACK);
        bar.setMinimumHeight(dp(a, 56));

        bar.addView(backButton(a, a::onBackPressed),
                new android.widget.LinearLayout.LayoutParams(backWidth(a), backHeight(a)));

        android.widget.TextView t = new android.widget.TextView(a);
        t.setText(title);
        t.setTextColor(Color.WHITE);
        t.setTextSize(19);
        t.setTypeface(Fonts.current(a));
        t.setSingleLine(true);
        t.setEllipsize(android.text.TextUtils.TruncateAt.END);
        t.setPadding(dp(a, 4), 0, dp(a, 16), 0);
        bar.addView(t, new android.widget.LinearLayout.LayoutParams(
                0, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return bar;
    }

    /** Wrap {@code content} under a "← Title" bar + hairline and set it as the content view. */
    static void screen(android.app.Activity a, String title, View content) {
        android.widget.LinearLayout col = new android.widget.LinearLayout(a);
        col.setOrientation(android.widget.LinearLayout.VERTICAL);
        col.setBackgroundColor(Color.BLACK);
        col.addView(header(a, title), new android.widget.LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
        View hair = new View(a);
        hair.setBackgroundColor(0xFF1C1C1C);
        col.addView(hair, new android.widget.LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT, 1));
        col.addView(content, new android.widget.LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        a.setContentView(col);
    }

    /**
     * A white circular indeterminate spinner that actually animates. Built against
     * the Material theme so it uses the thin rotating-arc drawable rather than the
     * flat holo asset the app's legacy {@code Theme.Black} would hand out; tinted
     * white via {@code indeterminateTintList} (a colour filter freezes it).
     */
    static ProgressBar spinner(Context context) {
        ProgressBar bar = new ProgressBar(
                new ContextThemeWrapper(context, android.R.style.Theme_Material),
                null, android.R.attr.progressBarStyle);   // medium — not the oversized "large"
        bar.setIndeterminate(true);
        bar.setIndeterminateTintList(ColorStateList.valueOf(Color.WHITE));
        int s = Math.round(24 * context.getResources().getDisplayMetrics().density);
        bar.setLayoutParams(new ViewGroup.LayoutParams(s, s));
        return bar;
    }

    private static final int IMMERSIVE_FLAGS =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

    /** Hide the status and navigation bars for a full-screen gate. Call again on focus. */
    static void hideSystemBars(android.app.Activity activity) {
        activity.getWindow().getDecorView().setSystemUiVisibility(IMMERSIVE_FLAGS);
    }

    static void hideSystemBars(View decorView) {
        decorView.setSystemUiVisibility(IMMERSIVE_FLAGS);
    }

    static void style(Context context, Button button) {
        button.setTextColor(Color.WHITE);
        button.setTypeface(Fonts.current(context));
        button.setAllCaps(false);
        button.setPadding(48, 28, 48, 28);
        button.setBackground(buttonBackground(context));
    }

    static void style(Context context, EditText input) {
        input.setTextColor(Color.WHITE);
        input.setTypeface(Fonts.current(context));
        input.setPadding(32, 20, 32, 20);
        input.setBackground(inputBackground(context));
    }

    private static GradientDrawable inputBackground(Context c) {
        return rounded(c, Color.BLACK, Color.WHITE, 2f, R_SM);
    }

    static GradientDrawable frameBorder(Context c) {
        return rounded(c, Color.TRANSPARENT, Color.WHITE, 2f, 0f);
    }

    static GradientDrawable dialogBackground(Context c) {
        return rounded(c, Color.BLACK, 0xFF2C2C2C, 2f, R_MD);
    }

    /** Top corners only — for a sheet that sits flush on the bottom edge. */
    static GradientDrawable sheetBackground(Context c) {
        GradientDrawable box = new GradientDrawable();
        box.setColor(Color.BLACK);
        float r = dp(c, R_LG);
        box.setCornerRadii(new float[]{r, r, r, r, 0, 0, 0, 0});
        return box;
    }

    /** The one popup header: gray, uppercase, letter-spaced — the Settings section-header look.
     *  Every dialog title in the app goes through this. */
    static android.widget.TextView dialogTitle(Context c, String text) {
        android.widget.TextView t = new android.widget.TextView(c);
        t.setText(text.toUpperCase());
        t.setTextColor(Color.GRAY);
        t.setTextSize(13);
        t.setLetterSpacing(0.15f);
        t.setTypeface(Fonts.current(c));
        t.setPadding(48, 16, 48, 14);
        return t;
    }

    /** Same look as {@link #dialogTitle}, but the text's own case is left alone — for content
     *  where case is meaningful (chess SAN: "dxc6" and "DXC6" are not the same thing), unlike
     *  a section header where forcing caps is exactly the point. */
    static android.widget.TextView dialogTitleExact(Context c, String text) {
        android.widget.TextView t = new android.widget.TextView(c);
        t.setText(text);
        t.setTextColor(Color.GRAY);
        t.setTextSize(13);
        t.setLetterSpacing(0.15f);
        t.setTypeface(Fonts.current(c));
        t.setPadding(48, 16, 48, 14);
        return t;
    }

    /** The black "title / input / Save / Cancel" prompt (rename, etc.) — single-line. */
    static void textPrompt(android.app.Activity host, String title, String initial,
                           String okLabel, java.util.function.Consumer<String> onOk) {
        textPrompt(host, title, initial, okLabel, true, onOk);
    }

    /** Same prompt, with {@code singleLine} false for free-form text (a comment, a note)
     *  that should wrap and grow instead of scrolling sideways in one line. */
    static void textPrompt(android.app.Activity host, String title, String initial,
                           String okLabel, boolean singleLine, java.util.function.Consumer<String> onOk) {
        android.graphics.Typeface font = Fonts.current(host);
        LinearLayout root = new LinearLayout(host);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(dialogBackground(host));
        clipRounded(host, root, R_MD);
        root.setPadding(2, 32, 2, dp(host, R_MD)); // inset so option rows clear the rounded border
        root.addView(dialogTitle(host, title));

        EditText input = new EditText(host);
        input.setText(initial == null ? "" : initial);
        input.setSelectAllOnFocus(true);
        input.setBackground(null);
        input.setTextColor(Color.WHITE);
        input.setTypeface(font);
        input.setTextSize(18);
        input.setSingleLine(singleLine);
        if (!singleLine) {
            input.setMinLines(3);
            // Caps growth at 3 lines' worth of height — past that it scrolls internally
            // instead of pushing the dialog taller with every extra line typed.
            input.setMaxLines(3);
            input.setVerticalScrollBarEnabled(true);
            input.setMovementMethod(new android.text.method.ScrollingMovementMethod());
            input.setGravity(android.view.Gravity.TOP | android.view.Gravity.START);
            input.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                    | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
                    | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        }
        input.setPadding(48, 8, 48, 16);
        root.addView(input);

        android.widget.FrameLayout scrim = wrapScrim(host, root, 0.85f);
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(
                host, R.style.Theme_PlainPhone_RoundedDialog).setView(scrim).create();
        root.addView(promptRow(host, font, okLabel, () -> {
            String s = input.getText().toString().trim();
            if (!s.isEmpty()) onOk.accept(s);
            dialog.dismiss();
        }));
        root.addView(promptRow(host, font, "Cancel", dialog::dismiss));
        finishCentered(dialog, scrim);
        focusAndShowKeyboard(host, input);
    }

    /** Grab focus and pop the soft keyboard for a freshly-shown dialog's input field. */
    static void focusAndShowKeyboard(android.app.Activity host, EditText input) {
        input.requestFocus();
        input.post(() -> {
            android.view.inputmethod.InputMethodManager imm =
                    (android.view.inputmethod.InputMethodManager)
                            host.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(input, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
        });
    }

    private static android.widget.TextView promptRow(Context c, android.graphics.Typeface font,
                                                     String label, Runnable action) {
        android.widget.TextView row = new android.widget.TextView(c);
        row.setText(label);
        row.setTextColor(Color.WHITE);
        row.setTextSize(20);
        row.setTypeface(font);
        row.setPadding(48, 32, 48, 32);
        StateListDrawable bg = new StateListDrawable();
        bg.addState(new int[]{android.R.attr.state_pressed},
                new android.graphics.drawable.ColorDrawable(Color.DKGRAY));
        bg.addState(new int[]{}, new android.graphics.drawable.ColorDrawable(Color.BLACK));
        row.setBackground(bg);
        row.setOnClickListener(v -> action.run());
        return row;
    }

    /**
     * Show {@code content} as a full-screen dialog: a hand-drawn scrim (not the system's
     * dim-behind) fills the entire window, with {@code content} centered over it at
     * {@code widthFraction} of the screen's width. A small floating AlertDialog window (the
     * old approach) sits centered within its OWN small rectangular bounds — the system's
     * dim-behind can only darken the screen OUTSIDE that rectangle, never the corner notches
     * INSIDE it where the rounded content doesn't paint, so those notches always show a
     * different shade (opaque window background) than the dimmed area just past the window's
     * edge — a square seam right behind the rounded curve. A full-screen window with the
     * scrim as part of our own content removes that seam entirely: there's only one
     * continuous canvas, no window-bounds edge inside the visible dialog area at all.
     * Tapping the scrim (outside {@code content}) dismisses, matching the old
     * setCanceledOnTouchOutside behavior a small floating window gave for free.
     *
     * <p>Step 1: {@link #wrapScrim} builds the scrim, ready to pass to
     * {@code new AlertDialog.Builder(host, R.style.Theme_PlainPhone_RoundedDialog).setView(...)}.
     * Split from {@link #finishCentered} so callers whose rows need a {@code dialog} reference
     * (to call dismiss()) can create the dialog from the returned scrim first, populate
     * {@code content} afterward, then call finishCentered.
     */
    static android.widget.FrameLayout wrapScrim(android.app.Activity host, View content, float widthFraction) {
        android.widget.FrameLayout scrim = new android.widget.FrameLayout(host);
        scrim.setBackgroundColor(0x99000000); // ~0.6 dim, matching the old dimAmount
        // Center within the status/nav-bar-safe area, not the raw full-screen window bounds —
        // otherwise the status bar's height alone visibly pushes content off vertical-center.
        scrim.setFitsSystemWindows(true);
        content.setClickable(true); // stop taps on content's own padding from reaching scrim
        int widthPx = (int) (host.getResources().getDisplayMetrics().widthPixels * widthFraction);
        android.widget.FrameLayout.LayoutParams lp = new android.widget.FrameLayout.LayoutParams(
                widthPx, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.gravity = android.view.Gravity.CENTER;
        scrim.addView(content, lp);
        return scrim;
    }

    /** Step 2, once {@code dialog} was created from {@link #wrapScrim}'s return value and
     *  {@code content} has been fully populated — shows it and finishes window setup. */
    static void finishCentered(android.app.AlertDialog dialog, android.widget.FrameLayout scrim) {
        scrim.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
        unboxDialog(scrim);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setElevation(0f);
            android.view.WindowManager.LayoutParams p = dialog.getWindow().getAttributes();
            p.width = ViewGroup.LayoutParams.MATCH_PARENT;
            p.height = ViewGroup.LayoutParams.MATCH_PARENT;
            dialog.getWindow().setAttributes(p);
            // The window itself being MATCH_PARENT doesn't guarantee AlertDialog's own
            // internal content wrapper stretches scrim to fill it — that wrapper defaults to
            // wrapping its child tightly for a "floating" dialog theme, which is why the whole
            // popup (scrim's own bounds, background included) was rendering pinned near
            // whatever position that wrapper's default gravity places a wrap-sized child,
            // instead of covering the full screen the window itself now spans.
            // scrim's actual immediate parent inside AlertDialog's decor is a FrameLayout,
            // which casts its child's LayoutParams to FrameLayout.LayoutParams specifically —
            // both the plain base type and generic MarginLayoutParams crash here.
            scrim.setLayoutParams(new android.widget.FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            // scrim's own parent isn't the only wrap-sized panel in the chain — the "floating"
            // dialog theme's chrome nests another panel or two above that, each wrapping
            // tightly around its child by default regardless of the window's own size. Widen
            // every one of them up to (not including) the DecorView, mutating each ancestor's
            // OWN LayoutParams object in place so its type always matches what that ancestor's
            // parent expects — a freshly constructed LayoutParams guesses wrong and crashes.
            android.view.ViewParent anc = scrim.getParent();
            while (anc instanceof View && !"DecorView".equals(anc.getClass().getSimpleName())) {
                View av = (View) anc;
                ViewGroup.LayoutParams lp = av.getLayoutParams();
                if (lp != null) {
                    lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
                    lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
                    av.setLayoutParams(lp);
                }
                anc = av.getParent();
            }
        }
    }

    /** Convenience for the common case where nothing inside {@code content} needs a
     *  {@code dialog} reference while being built (e.g. it was already wired via a
     *  self-contained click listener, or doesn't need to dismiss itself). */
    static android.app.AlertDialog showCentered(android.app.Activity host, View content, float widthFraction) {
        android.widget.FrameLayout scrim = wrapScrim(host, content, widthFraction);
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(
                host, R.style.Theme_PlainPhone_RoundedDialog).setView(scrim).create();
        finishCentered(dialog, scrim);
        return dialog;
    }

    static void clearDialogChrome(android.app.AlertDialog dialog) {
        if (dialog.getWindow() != null) {
            // Opaque, not transparent. Our content view only paints pixels inside its own
            // rounded-outline clip — the four little square notches between that curve and
            // the window's own rectangular bounds are otherwise never painted by anything.
            // A transparent window background leaves them showing raw, undimmed background
            // (a board tile, a divider) right at the popup's corner — reading as a square
            // border poking out from behind the curve. Opaque black paints those notches
            // black instead, so the box's own rounded stroke is the only edge visible.
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(
                    Color.BLACK));
            // Elevation would still leave the window's own Material shadow — a faint square
            // halo around the rounded box, on top of the box's own rounded stroke, reading as
            // "two borders" at once. Zeroing it removes that outer square entirely.
            dialog.getWindow().setElevation(0f);
            // Dim behind the window's own rectangular bounds too, so the live board/list past
            // the popup's edges doesn't stay at full brightness right next to a dimmed box.
            android.view.WindowManager.LayoutParams p = dialog.getWindow().getAttributes();
            p.dimAmount = 0.6f;
            p.flags |= android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND;
            dialog.getWindow().setAttributes(p);
        }
    }

    /**
     * Strip the AlertDialog's internal panel backgrounds + padding so a rounded
     * custom view isn't boxed by the theme's square {@code colorBackground}
     * panels behind it. Call after {@code dialog.show()}.
     */
    static void unboxDialog(View content) {
        android.view.ViewParent p = content.getParent();
        while (p instanceof View) {
            View v = (View) p;
            // The walk's top is the window's own DecorView — stripping ITS background undoes
            // clearDialogChrome's opaque black window background (set moments earlier, before
            // show()), leaving the four corner notches outside the rounded clip transparent
            // again and the raw screen behind showing through undimmed. Every other ancestor
            // panel in between still gets stripped as before; only the decor itself is spared.
            if ("DecorView".equals(v.getClass().getSimpleName())) break;
            v.setBackground(null);
            v.setPadding(0, 0, 0, 0);
            v.setElevation(0f); // each panel ancestor can carry its own shadow, not just the window
            p = v.getParent();
        }
    }

    private static StateListDrawable buttonBackground(Context c) {
        return pressable(c, Color.BLACK, Color.DKGRAY, Color.WHITE, 2f, R_SM);
    }
}


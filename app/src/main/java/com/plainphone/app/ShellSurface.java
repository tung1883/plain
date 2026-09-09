package com.plainphone.app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Map;

/**
 * The whole shell UI — {@link TerminalView} + the special-keys bar + the soft
 * keyboard plumbing + the {@code plaind} session channel — as one reusable view.
 * {@link DevTerminalActivity} wraps it in a "← host · shell" screen; a
 * {@link ShellPanel} drops it in a floating window. The two look identical.
 */
@SuppressLint("ViewConstructor")
final class ShellSurface extends LinearLayout {

    interface Callbacks {
        default void onReconnecting(boolean on) {}
        default void onTitle(String name) {}
        default void onExit(int code) {}
    }

    private final TerminalView term;
    private final View keyBar;
    private TextView ctrlKey, altKey, shiftKey;
    private boolean kbVisible;
    private long keyBarShownAt;

    private DevConnection connection;
    private long channel = -1;
    private long sessionId = -1;
    private boolean opening;
    private long statusHoldUntil;

    private Callbacks cb = new Callbacks() {};

    private final DevConnection.Sink sink = this::onChannelMessage;

    ShellSurface(Context ctx) {
        super(ctx);
        setOrientation(VERTICAL);
        setBackgroundColor(Color.BLACK);

        term = new TerminalView(ctx);
        term.setPadding(dp(10), 0, 0, 0); // breathing room on the left edge
        term.onInput = bytes -> {
            if (channel >= 0 && connection != null) connection.send(DevProtocol.ptyData(channel, bytes));
        };
        term.onResize = (cols, rows) -> {
            if (channel >= 0 && connection != null) connection.send(DevProtocol.ptyResize(channel, cols, rows));
        };
        addView(term, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        keyBar = buildKeyBar(ctx);
        keyBar.setVisibility(GONE);
        addView(keyBar, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        // The key bar mirrors the soft keyboard: up while it's up, gone when it's
        // dismissed. Keyed off the window's visible frame.
        getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            View rootView = getRootView();
            if (rootView == null) return;
            Rect r = new Rect();
            rootView.getWindowVisibleDisplayFrame(r);
            int screenH = rootView.getHeight();
            boolean up = screenH - r.bottom > screenH * 0.15f;
            if (up != kbVisible) {
                kbVisible = up;
                if (up) keyBarShownAt = SystemClock.uptimeMillis();
                keyBar.setVisibility(up ? VISIBLE : GONE);
            }
        });
    }

    void setCallbacks(Callbacks c) { this.cb = c != null ? c : new Callbacks() {}; }

    /** Reattach to a specific persistent session on the next {@link #attach}. */
    void setSessionId(long id) { this.sessionId = id; }

    long sessionId() { return sessionId; }

    /** A ⌨ button for a host's title bar / header. */
    TextView keyboardButton(Context ctx) {
        TextView kbd = new TextView(ctx);
        kbd.setText("⌨");
        kbd.setTextColor(Color.WHITE);
        kbd.setTextSize(18);
        kbd.setTypeface(Fonts.cascadiaMono(ctx));
        kbd.setGravity(Gravity.CENTER);
        kbd.setPadding(dp(20), 0, dp(20), 0);
        kbd.setOnClickListener(v -> toggleKeyboard());
        return kbd;
    }

    void toggleKeyboard() {
        InputMethodManager imm = (InputMethodManager) getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm == null) return;
        if (kbVisible) imm.hideSoftInputFromWindow(term.getWindowToken(), 0);
        else term.showKeyboard();
    }

    void focusKeyboardSoon() {
        term.requestFocus();
        term.postDelayed(term::showKeyboard, 150);
    }

    // --- connection --------------------------------------------------

    /** Link is up ({@code conn != null}) or gone ({@code null}). */
    void attach(DevConnection conn) {
        if (conn == null) {
            connection = null;
            channel = -1;
            opening = false;
            cb.onReconnecting(true);
            return;
        }
        if (connection != null && connection != conn) {
            connection = null;
            channel = -1;
            opening = false;
        }
        cb.onReconnecting(channel < 0);
        if (opening || channel >= 0) return;
        connection = conn;
        opening = true;
        channel = conn.openChannel(sink);
        term.reset();
        conn.send(DevProtocol.sessionOpen(channel,
                sessionId >= 0 ? sessionId : null, null,
                Math.max(term.cols(), 20), Math.max(term.rows(), 6)));
    }

    /** Activity stop / panel minimise — leave the shell running on the daemon. */
    void detachKeepAlive() {
        if (channel >= 0 && connection != null) {
            connection.send(DevProtocol.sessionDetach(channel));
            connection.closeChannel(channel);
        }
        channel = -1;
        opening = false;
    }

    /** Panel closed for good — kill the ephemeral shell. */
    void closeKill() {
        if (channel >= 0 && connection != null) {
            if (sessionId >= 0) connection.send(DevProtocol.sessionKill(channel, sessionId));
            connection.closeChannel(channel);
        }
        channel = -1;
        opening = false;
    }

    private void onChannelMessage(Map<String, Object> msg) {
        String type = DevProtocol.type(msg);
        if (DevProtocol.T_SESSION_OPENED.equals(type)) {
            sessionId = DevProtocol.num(msg, "id", sessionId);
            reconnecting(false);
            cb.onTitle(DevProtocol.str(msg, "name"));
        } else if (DevProtocol.T_SESSION_GONE.equals(type)) {
            reconnecting(true);
            sessionId = -1;
            term.reset();
            if (channel >= 0 && connection != null) {
                connection.send(DevProtocol.sessionOpen(channel, null, null,
                        Math.max(term.cols(), 20), Math.max(term.rows(), 6)));
            }
        } else if (DevProtocol.T_PTY_DATA.equals(type)) {
            byte[] data = DevProtocol.bin(msg, "data");
            if (data != null) term.feed(data, data.length);
        } else if (DevProtocol.T_PTY_EXIT.equals(type)) {
            cb.onExit((int) DevProtocol.num(msg, "code", 0));
        }
    }

    private void reconnecting(boolean on) {
        if (on) {
            statusHoldUntil = SystemClock.uptimeMillis() + 1200;
            cb.onReconnecting(true);
        } else {
            long wait = statusHoldUntil - SystemClock.uptimeMillis();
            if (wait > 0) {
                postDelayed(() -> { if (channel >= 0) cb.onReconnecting(false); }, wait);
            } else {
                cb.onReconnecting(false);
            }
        }
    }

    // --- key bar ----------------------------------------------------

    private View buildKeyBar(Context ctx) {
        LinearLayout bar = new LinearLayout(ctx);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setPadding(dp(8), dp(10), dp(8), dp(10));

        ctrlKey = key(ctx, "ctrl", () -> { term.armCtrl(!term.ctrlArmed()); paintMods(); });
        altKey = key(ctx, "alt", () -> { term.armAlt(!term.altArmed()); paintMods(); });
        shiftKey = key(ctx, "shift", () -> { term.armShift(!term.shiftArmed()); paintMods(); });
        term.onModsCleared = this::paintMods;
        bar.addView(ctrlKey);
        bar.addView(key(ctx, "tab", () -> term.barKey(new byte[]{'\t'})));
        bar.addView(altKey);
        bar.addView(key(ctx, "esc", () -> term.barKey(new byte[]{0x1b})));
        bar.addView(key(ctx, "^C", () -> term.sendBytes(new byte[]{0x03})));
        bar.addView(key(ctx, "del", () -> term.barKey(TerminalView.esc("[3~"))));
        bar.addView(shiftKey);
        bar.addView(key(ctx, "enter", () -> term.barKey(new byte[]{'\r'})));
        bar.addView(key(ctx, "↑", () -> term.barArrow('A')));
        bar.addView(key(ctx, "↓", () -> term.barArrow('B')));
        bar.addView(key(ctx, "←", () -> term.barArrow('D')));
        bar.addView(key(ctx, "→", () -> term.barArrow('C')));

        HorizontalScrollView scroller = new HorizontalScrollView(ctx);
        scroller.setHorizontalScrollBarEnabled(false);
        scroller.setBackgroundColor(0xFF0A0A0A);
        scroller.addView(bar);
        return scroller;
    }

    private TextView key(Context ctx, String label, Runnable action) {
        TextView k = new TextView(ctx);
        k.setText(label);
        k.setTextSize(14);
        k.setTypeface(Fonts.cascadiaMono(ctx));
        k.setGravity(Gravity.CENTER);
        k.setPadding(dp(13), dp(10), dp(13), dp(10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.rightMargin = dp(8);
        k.setLayoutParams(lp);
        paint(k, false);
        k.setOnClickListener(v -> action.run());
        return k;
    }

    private void paintMods() {
        paint(ctrlKey, term.ctrlArmed());
        paint(altKey, term.altArmed());
        paint(shiftKey, term.shiftArmed());
    }

    private void paint(TextView k, boolean on) {
        GradientDrawable box = new GradientDrawable();
        box.setColor(on ? Color.WHITE : Color.BLACK);
        box.setStroke(2, 0xFF2C2C2C);
        box.setCornerRadius(dp(6));
        k.setBackground(box);
        k.setTextColor(on ? Color.BLACK : 0xFF8B8B8B);
    }

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}

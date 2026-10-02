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
    private final KeyBar keyBar;
    private boolean kbVisible;
    private long keyBarShownAt;

    private DevConnection connection;
    private long channel = -1;
    private long sessionId = -1;
    private boolean opening;
    private long statusHoldUntil;
    /** When the oldest keystroke still waiting for any shell output was sent, 0 = none. */
    private long echoPendingSince;

    private Callbacks cb = new Callbacks() {};

    private final DevConnection.Sink sink = this::onChannelMessage;

    ShellSurface(Context ctx) {
        super(ctx);
        setOrientation(VERTICAL);
        setBackgroundColor(Color.BLACK);

        term = new TerminalView(ctx);
        term.setPadding(dp(10), 0, 0, 0); // breathing room on the left edge
        term.onInput = (bytes, seq) -> {
            if (channel >= 0 && connection != null) {
                if (echoPendingSince == 0) echoPendingSince = SystemClock.uptimeMillis();
                connection.send(connection.hasCap("echo_ack")
                        ? DevProtocol.ptyData(channel, bytes, seq)
                        : DevProtocol.ptyData(channel, bytes));
            }
        };
        term.rtt = () -> connection != null ? connection.srttMs() : -1;
        term.onResize = (cols, rows) -> {
            if (channel >= 0 && connection != null) connection.send(DevProtocol.ptyResize(channel, cols, rows));
        };
        addView(term, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        keyBar = new KeyBar(ctx, false, new KeyBar.Listener() {
            @Override
            public void onKeyDown(String id) {
                pressKey(id);
                startRepeat(id);
            }

            @Override
            public void onKeyUp(String id) {
                stopRepeat();
            }

            @Override
            public void onMods(java.util.Set<String> active) {
                term.armCtrl(active.contains("Ctrl"));
                term.armAlt(active.contains("Alt"));
                term.armShift(active.contains("Shift"));
            }
        });
        term.onModsCleared = keyBar::consumeMods;   // locked ones are re-armed from there
        keyBar.setVisibility(GONE);
        addView(keyBar, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        // The key bar shows only while the soft keyboard is up AND this terminal
        // holds focus — the IME frame is window-global, so without the focus
        // check every shell panel in a workspace would pop its key bar when any
        // one of them (or a web URL bar) raised the keyboard.
        getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            View rootView = getRootView();
            if (rootView == null) return;
            Rect r = new Rect();
            rootView.getWindowVisibleDisplayFrame(r);
            int screenH = rootView.getHeight();
            if (SystemClock.uptimeMillis() - keyBarShownAt < 800) return; // let the IME settle
            boolean imeUp = screenH - r.bottom > screenH * 0.15f;
            if (kbVisible && !imeUp) {                       // keyboard dismissed → hide the bar
                kbVisible = false;
                keyBar.setVisibility(GONE);
            } else if (!kbVisible && imeUp && term.hasFocus()) { // this terminal raised it
                kbVisible = true;
                keyBarShownAt = SystemClock.uptimeMillis();
                keyBar.setVisibility(VISIBLE);
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
        if (kbVisible) {
            imm.hideSoftInputFromWindow(term.getWindowToken(), 0);
            kbVisible = false;
            keyBar.setVisibility(GONE);
        } else {
            term.showKeyboard();
            kbVisible = true;
            keyBarShownAt = SystemClock.uptimeMillis();
            keyBar.setVisibility(VISIBLE);
        }
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
        term.setEchoAck(conn.hasCap("echo_ack"));
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
            if (echoPendingSince != 0) {
                // keystroke -> first output after it (the echo), end to end
                LatencyStats.record("shell.echo", SystemClock.uptimeMillis() - echoPendingSince);
                echoPendingSince = 0;
            }
            if (data != null) {
                long t0 = SystemClock.uptimeMillis();
                term.feed(data, data.length);
                LatencyStats.record("shell.feed", SystemClock.uptimeMillis() - t0);
            }
            long ack = DevProtocol.num(msg, "ack", -1);
            if (ack >= 0) term.ack(ack);
        } else if (DevProtocol.T_PTY_ACK.equals(type)) {
            term.ack(DevProtocol.num(msg, "seq", -1));
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

    private static final long REPEAT_DELAY_MS = 400;
    private static final long REPEAT_EVERY_MS = 40;
    private Runnable repeater;

    /**
     * A terminal only ever sees characters, so holding a key means sending it again and again:
     * after a short delay, then steadily, until the finger lifts (what a keyboard's auto-repeat does).
     */
    private void startRepeat(String id) {
        stopRepeat();
        repeater = new Runnable() {
            @Override
            public void run() {
                pressKey(id);
                postDelayed(this, REPEAT_EVERY_MS);
            }
        };
        postDelayed(repeater, REPEAT_DELAY_MS);
    }

    private void stopRepeat() {
        if (repeater != null) {
            removeCallbacks(repeater);
            repeater = null;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        stopRepeat();
        super.onDetachedFromWindow();
    }

    /** What a key-bar key sends to the shell. */
    private void pressKey(String id) {
        switch (id) {
            case "Esc": term.barKey(new byte[]{0x1b}); return;
            // Shift+Tab is "back tab", ESC [ Z (e.g. Claude Code uses it to cycle its mode).
            case "Tab": term.barKey(term.shiftArmed() ? TerminalView.esc("[Z") : new byte[]{'\t'}); return;
            case "Enter": term.barKey(new byte[]{'\r'}); return;
            case "Bksp": term.barKey(new byte[]{0x7f}); return;
            case "Left": term.barArrow('D'); return;
            case "Down": term.barArrow('B'); return;
            case "Up": term.barArrow('A'); return;
            case "Right": term.barArrow('C'); return;
            case "Home": term.barKey(TerminalView.esc("[H")); return;
            case "End": term.barKey(TerminalView.esc("[F")); return;
            case "PgUp": term.barKey(TerminalView.esc("[5~")); return;
            case "PgDn": term.barKey(TerminalView.esc("[6~")); return;
            case "Ins": term.barKey(TerminalView.esc("[2~")); return;
            case "Del": term.barKey(TerminalView.esc("[3~")); return;
            case "F1": term.barKey(TerminalView.esc("OP")); return;
            case "F2": term.barKey(TerminalView.esc("OQ")); return;
            case "F3": term.barKey(TerminalView.esc("OR")); return;
            case "F4": term.barKey(TerminalView.esc("OS")); return;
            case "F5": term.barKey(TerminalView.esc("[15~")); return;
            case "F6": term.barKey(TerminalView.esc("[17~")); return;
            case "F7": term.barKey(TerminalView.esc("[18~")); return;
            case "F8": term.barKey(TerminalView.esc("[19~")); return;
            case "F9": term.barKey(TerminalView.esc("[20~")); return;
            case "F10": term.barKey(TerminalView.esc("[21~")); return;
            case "F11": term.barKey(TerminalView.esc("[23~")); return;
            case "F12": term.barKey(TerminalView.esc("[24~")); return;
            default: break;
        }
        if (id.length() == 2 && id.charAt(0) == '^') {            // ^C and friends
            term.sendBytes(new byte[]{TerminalView.control(id.charAt(1))});
        } else {                                                   // a symbol
            term.barKey(id.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}

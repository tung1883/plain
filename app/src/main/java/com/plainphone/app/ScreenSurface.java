package com.plainphone.app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * The whole remote-GUI UI — {@link RemoteScreenView} (+ {@link TrackpadView} in
 * dedicated-pad mode), the special-keys bar, the hidden key input, and the
 * {@code plaind} screen channel — as one reusable view. Shared by
 * {@link DevScreenActivity} and {@link ScreenPanel} so the two look identical.
 */
@SuppressLint("ViewConstructor")
final class ScreenSurface extends LinearLayout {

    private final RemoteScreenView screen;
    private TrackpadView pad;
    private final EditText keyInput;
    private final KeyBar keyBar;
    private TextView chip;


    private DevConnection connection;
    private long channel = -1;
    private boolean opening;
    private int baseMaxW = 1280;
    private int curMaxW = 1280;
    private long keyBarShownAt;
    private final Handler restream = new Handler();

    private final DevConnection.Sink sink = this::onChannelMessage;

    ScreenSurface(Context ctx) {
        super(ctx);
        setOrientation(VERTICAL);
        setBackgroundColor(Color.BLACK);

        boolean padStyle = "pad".equals(Config.getDevTrackpadStyle(ctx));

        screen = new RemoteScreenView(ctx);
        screen.setFocusable(false);            // don't steal focus from keyInput on touch
        screen.setFocusableInTouchMode(false);
        screen.layout = padStyle ? RemoteScreenView.Layout.PAD : RemoteScreenView.Layout.WHOLE;
        screen.aspectLock = padStyle;
        screen.listener = new RemoteScreenView.Listener() {
            @Override public void move(float dx, float dy, float scroll) {
                send(DevProtocol.inputMove(dx, dy, scroll));
            }
            @Override public void click(String button, boolean doubleClick) {
                send(DevProtocol.inputClick(button, doubleClick));
            }
            @Override public void press(boolean down) {
                send(DevProtocol.msg(down ? DevProtocol.T_INPUT_DOWN : DevProtocol.T_INPUT_UP));
            }
            @Override public void point(float nx, float ny) {
                send(DevProtocol.inputPoint(nx, ny));
            }
            @Override public void zoom(float ticks) {
                send(DevProtocol.inputZoom(ticks));
            }
        };
        screen.onZoomSettle = this::scheduleRestream;
        screen.onModeChange = () -> {
            if (chip != null) {
                chip.setText(screen.mode() == RemoteScreenView.Mode.MOVE ? "MOVE" : "VIEW");
                paintChip();
            }
        };

        if (padStyle) {
            addView(screen, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            pad = new TrackpadView(ctx);
            pad.screen = screen;
            addView(pad, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        } else {
            FrameLayout stage = new FrameLayout(ctx);
            stage.addView(screen, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            chip = new TextView(ctx);
            chip.setText("MOVE");
            chip.setTypeface(Fonts.cascadiaMono(ctx));
            chip.setTextSize(11);
            chip.setPadding(dp(24), dp(12), dp(24), dp(12));
            paintChip();
            chip.setOnClickListener(v -> screen.toggleMode());
            FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cp.leftMargin = cp.topMargin = dp(20);
            stage.addView(chip, cp);
            addView(stage, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        }

        keyInput = new EditText(ctx);
        keyInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        keyInput.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        keyInput.setBackgroundColor(Color.TRANSPARENT);
        keyInput.setCursorVisible(false);
        keyInput.setTextColor(Color.TRANSPARENT);
        keyInput.setHeight(1);
        keyInput.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (count > before) {
                    send(DevProtocol.inputKey(
                            s.subSequence(start + before, start + count).toString(), null,
                            consumeMods()));
                } else if (before > count) {
                    send(DevProtocol.inputKey(null, "Backspace", consumeMods()));
                }
                if (s.length() > 64) keyInput.setText("");
            }
            public void afterTextChanged(Editable s) {}
        });
        keyInput.setOnEditorActionListener((v, id, ev) -> {
            send(DevProtocol.inputKey(null, "Enter"));
            return true;
        });
        keyInput.setOnKeyListener((v, code, ev) -> {
            if (ev.getAction() == KeyEvent.ACTION_DOWN && code == KeyEvent.KEYCODE_DEL) {
                send(DevProtocol.inputKey(null, "Backspace"));
                return true;
            }
            // A keyboard's Tab, Esc, arrows and so on never reach the text watcher, and Android
            // would use Tab and the arrows to move focus; send them to the host like the key bar does.
            String named = namedKey(code);
            if (named == null) return false;
            if (ev.getAction() == KeyEvent.ACTION_DOWN) {
                List<String> mods = consumeMods();
                if (ev.isCtrlPressed() || ev.isAltPressed() || ev.isShiftPressed()) {
                    mods = mods == null ? new ArrayList<>() : mods;
                    if (ev.isCtrlPressed() && !mods.contains("ctrl")) mods.add("ctrl");
                    if (ev.isAltPressed() && !mods.contains("alt")) mods.add("alt");
                    if (ev.isShiftPressed() && !mods.contains("shift")) mods.add("shift");
                }
                send(DevProtocol.inputKey(null, named, mods));
            }
            return true;
        });
        addView(keyInput, new LayoutParams(1, 1));

        keyBar = new KeyBar(ctx, true, new KeyBar.Listener() {
            @Override
            public void onKeyDown(String id) {
                holdKey(id);
            }

            @Override
            public void onKeyUp(String id) {
                releaseKey();
            }
        });
        keyBar.setVisibility(GONE);
        addView(keyBar, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            if (System.currentTimeMillis() - keyBarShownAt < 800) return;
            View rootView = getRootView();
            if (rootView == null) return;
            Rect r = new Rect();
            rootView.getWindowVisibleDisplayFrame(r);
            int screenH = rootView.getHeight();
            boolean kbShown = screenH - r.bottom > screenH * 0.15f;
            if (!kbShown && keyBar.getVisibility() == VISIBLE) {
                keyBar.setVisibility(GONE);
                keyInput.clearFocus();
            }
        });
    }

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
        boolean showing = keyBar.getVisibility() == VISIBLE;
        if (showing) {
            imm.hideSoftInputFromWindow(keyInput.getWindowToken(), 0);
            keyInput.clearFocus();
            keyBar.setVisibility(GONE);
            keyBarShownAt = 0;
        } else {
            keyInput.requestFocus();
            imm.showSoftInput(keyInput, InputMethodManager.SHOW_IMPLICIT);
            keyBar.setVisibility(VISIBLE);
            keyBarShownAt = System.currentTimeMillis();
        }
    }

    // --- connection -----------------------------------------------

    void attach(DevConnection conn) {
        if (conn == null) {
            connection = null;
            channel = -1;
            opening = false;
            return;
        }
        if (connection != null && connection != conn) {
            connection = null;
            channel = -1;
            opening = false;
        }
        if (opening || channel >= 0) { connection = conn; return; }
        connection = conn;
        opening = true;
        channel = conn.openChannel(sink);
        baseMaxW = Math.min(1280, getResources().getDisplayMetrics().widthPixels * 2);
        curMaxW = targetMaxW(screen.zoom());
        conn.send(DevProtocol.screenStart(channel, curMaxW, Config.getDevScreenFps(getContext()), false));
    }

    void detach() {
        restream.removeCallbacksAndMessages(null);
        if (channel >= 0 && connection != null) {
            connection.send(DevProtocol.screenStop(channel));
            connection.closeChannel(channel);
        }
        channel = -1;
        opening = false;
    }

    void release() {
        detach();
        screen.release();
    }

    private long lastFrameAt;

    private void onChannelMessage(Map<String, Object> msg) {
        if (!DevProtocol.T_SCREEN_FRAME.equals(DevProtocol.type(msg))) return;
        send(DevProtocol.screenAck(channel)); // frees the daemon to send the next one
        int sw = (int) DevProtocol.num(msg, "sw", 0);
        int sh = (int) DevProtocol.num(msg, "sh", 0);
        if (sw > 0) screen.setSourceSize(sw, sh);
        long now = android.os.SystemClock.uptimeMillis();
        if (lastFrameAt != 0) LatencyStats.record("screen.interval", now - lastFrameAt);
        lastFrameAt = now;
        List<Object> tiles = DevProtocol.list(msg, "tiles");
        if (tiles != null) {
            // partial update: changed rectangles to paint over the current image
            int w = (int) DevProtocol.num(msg, "w", 0), h = (int) DevProtocol.num(msg, "h", 0);
            List<RemoteScreenView.Tile> parts = new ArrayList<>(tiles.size());
            int bytes = 0;
            for (Object o : tiles) {
                if (!(o instanceof Map)) continue;
                @SuppressWarnings("unchecked") Map<String, Object> t = (Map<String, Object>) o;
                byte[] jpeg = DevProtocol.bin(t, "data");
                if (jpeg == null) continue;
                bytes += jpeg.length;
                parts.add(new RemoteScreenView.Tile((int) DevProtocol.num(t, "x", 0),
                        (int) DevProtocol.num(t, "y", 0), jpeg));
            }
            LatencyStats.record("screen.tiles_kb", bytes / 1024.0);
            screen.setTiles(w, h, parts);
            return;
        }
        byte[] data = DevProtocol.bin(msg, "data");
        if (data != null) {
            LatencyStats.record("screen.full_kb", data.length / 1024.0);
            screen.setFrame(data);
        }
    }

    private void send(Map<String, Object> message) {
        if (connection != null) connection.send(message);
    }

    private int targetMaxW(float zoom) {
        int want = Math.round(baseMaxW * Math.max(1f, zoom));
        want = Math.min(want, 2560);
        int bucket = Math.max(baseMaxW, ((want + 319) / 640) * 640);
        return Math.min(bucket, 2560);
    }

    private void scheduleRestream() {
        restream.removeCallbacksAndMessages(null);
        restream.postDelayed(() -> {
            if (channel < 0 || connection == null) return;
            int want = targetMaxW(screen.zoom());
            if (want == curMaxW) return;
            curMaxW = want;
            connection.send(DevProtocol.screenStart(
                    channel, curMaxW, Config.getDevScreenFps(getContext()), false));
        }, 280);
    }

    // --- key bar --------------------------------------------------

    private static final long REPEAT_DELAY_MS = 400;
    private static final long REPEAT_EVERY_MS = 40;
    private Runnable repeater;
    private boolean holding;

    /**
     * A key-bar key goes down on the host and stays down until the finger lifts. After a short
     * delay it is pressed again every few tens of ms, which is what makes an editor repeat it
     * (an injected key is not auto-repeated by Windows); the host lets go on its own if these stop.
     */
    private void holdKey(String id) {
        releaseKey();
        // An older plaind does not know held keys: tap once, as before.
        if (connection == null || !connection.hasCap("keyhold")) {
            tapKey(id);
            return;
        }
        List<String> mods = consumeMods();
        String key = hostKey(id);
        if (id.length() == 2 && id.charAt(0) == '^') {            // ^C and friends: Ctrl held with the letter
            mods = mods == null ? new ArrayList<>() : mods;
            if (!mods.contains("ctrl")) mods.add("ctrl");
        }
        final String k = key;
        final List<String> m = mods;
        holding = true;
        send(DevProtocol.inputKeyDown(k, m));
        repeater = new Runnable() {
            @Override
            public void run() {
                send(DevProtocol.inputKeyDown(k, m));
                postDelayed(this, REPEAT_EVERY_MS);
            }
        };
        postDelayed(repeater, REPEAT_DELAY_MS);
    }

    /** One press and release, for a host that cannot hold keys. */
    private void tapKey(String id) {
        String key = hostKey(id);
        List<String> mods = consumeMods();
        if (id.length() == 2 && id.charAt(0) == '^') {            // ^C and friends: Ctrl held with the letter
            mods = mods == null ? new ArrayList<>() : mods;
            if (!mods.contains("ctrl")) mods.add("ctrl");
            send(DevProtocol.inputKey(key, null, mods));
        } else if (key.length() == 1) {                            // a symbol, typed as text
            send(DevProtocol.inputKey(key, null, mods));
        } else {
            send(DevProtocol.inputKey(null, key, mods));
        }
    }

    private void releaseKey() {
        if (repeater != null) {
            removeCallbacks(repeater);
            repeater = null;
        }
        if (holding) {
            holding = false;
            send(DevProtocol.inputKeyUp());
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        releaseKey();
        super.onDetachedFromWindow();
    }

    /** The host's name for a key-bar key: a key name, or the one character it types. */
    private static String hostKey(String id) {
        switch (id) {
            case "Esc": return "Escape";
            case "Bksp": return "Backspace";
            case "PgUp": return "PageUp";
            case "PgDn": return "PageDown";
            case "Ins": return "Insert";
            case "Del": return "Delete";
            case "PrtSc": return "PrintScreen";
            case "Caps": return "CapsLock";
            default:
                if (id.length() == 2 && id.charAt(0) == '^') return String.valueOf(Character.toLowerCase(id.charAt(1)));
                return id;   // Tab, Enter, arrows, Home, End, Win, Menu, F1..F12, or a symbol
        }
    }

    /** The host's name for a non-text key, or null for keys the text field handles itself. */
    private static String namedKey(int code) {
        switch (code) {
            case KeyEvent.KEYCODE_TAB: return "Tab";
            case KeyEvent.KEYCODE_ESCAPE: return "Escape";
            case KeyEvent.KEYCODE_DPAD_UP: return "Up";
            case KeyEvent.KEYCODE_DPAD_DOWN: return "Down";
            case KeyEvent.KEYCODE_DPAD_LEFT: return "Left";
            case KeyEvent.KEYCODE_DPAD_RIGHT: return "Right";
            case KeyEvent.KEYCODE_FORWARD_DEL: return "Delete";
            case KeyEvent.KEYCODE_MOVE_HOME: return "Home";
            case KeyEvent.KEYCODE_MOVE_END: return "End";
            case KeyEvent.KEYCODE_PAGE_UP: return "PageUp";
            case KeyEvent.KEYCODE_PAGE_DOWN: return "PageDown";
            default: return null;
        }
    }

    private void sendKey(String named) {
        send(DevProtocol.inputKey(null, named, consumeMods()));
    }

    private List<String> consumeMods() {
        return keyBar.consumeMods();
    }

    private void paintChip() {
        boolean move = screen.mode() == RemoteScreenView.Mode.MOVE;
        GradientDrawable box = new GradientDrawable();
        box.setColor(move ? Color.WHITE : Color.BLACK);
        box.setStroke(1, move ? Color.WHITE : 0xFF2C2C2C);
        box.setCornerRadius(dp(999));
        chip.setBackground(box);
        chip.setTextColor(move ? Color.BLACK : 0xFF8B8B8B);
    }

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}

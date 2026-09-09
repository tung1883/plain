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
import android.widget.HorizontalScrollView;
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
    private final View keyBar;
    private TextView chip;

    private final LinkedHashSet<String> armedMods = new LinkedHashSet<>();
    private final Map<String, TextView> modKeys = new HashMap<>();

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
            return false;
        });
        addView(keyInput, new LayoutParams(1, 1));

        keyBar = buildKeyBar(ctx);
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

    private void onChannelMessage(Map<String, Object> msg) {
        if (!DevProtocol.T_SCREEN_FRAME.equals(DevProtocol.type(msg))) return;
        int sw = (int) DevProtocol.num(msg, "sw", 0);
        int sh = (int) DevProtocol.num(msg, "sh", 0);
        if (sw > 0) screen.setSourceSize(sw, sh);
        byte[] data = DevProtocol.bin(msg, "data");
        if (data != null) screen.setFrame(data);
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

    private View buildKeyBar(Context ctx) {
        LinearLayout bar = new LinearLayout(ctx);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setPadding(dp(8), dp(10), dp(8), dp(10));
        bar.addView(modKey(ctx, "ctrl"));
        bar.addView(specialKey(ctx, "tab", () -> sendKey("Tab")));
        bar.addView(modKey(ctx, "alt"));
        bar.addView(specialKey(ctx, "esc", () -> sendKey("Escape")));
        bar.addView(specialKey(ctx, "^C", () -> send(DevProtocol.inputKey("c", null,
                Collections.singletonList("ctrl")))));
        bar.addView(specialKey(ctx, "del", () -> sendKey("Delete")));
        bar.addView(modKey(ctx, "shift"));
        bar.addView(specialKey(ctx, "enter", () -> sendKey("Enter")));
        bar.addView(specialKey(ctx, "↑", () -> sendKey("Up")));
        bar.addView(specialKey(ctx, "↓", () -> sendKey("Down")));
        bar.addView(specialKey(ctx, "←", () -> sendKey("Left")));
        bar.addView(specialKey(ctx, "→", () -> sendKey("Right")));

        HorizontalScrollView scroller = new HorizontalScrollView(ctx);
        scroller.setHorizontalScrollBarEnabled(false);
        scroller.setBackgroundColor(0xFF0A0A0A);
        scroller.addView(bar);
        return scroller;
    }

    private void sendKey(String named) {
        send(DevProtocol.inputKey(null, named, consumeMods()));
    }

    private List<String> consumeMods() {
        if (armedMods.isEmpty()) return null;
        List<String> out = new ArrayList<>(armedMods);
        armedMods.clear();
        for (Map.Entry<String, TextView> e : modKeys.entrySet()) paintKey(e.getValue(), false);
        return out;
    }

    private TextView modKey(Context ctx, String mod) {
        TextView k = specialKey(ctx, mod, null);
        modKeys.put(mod, k);
        k.setOnClickListener(v -> {
            if (!armedMods.remove(mod)) armedMods.add(mod);
            paintKey(k, armedMods.contains(mod));
        });
        return k;
    }

    private TextView specialKey(Context ctx, String label, Runnable action) {
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
        paintKey(k, false);
        if (action != null) k.setOnClickListener(v -> action.run());
        return k;
    }

    private void paintKey(TextView k, boolean on) {
        GradientDrawable box = new GradientDrawable();
        box.setColor(on ? Color.WHITE : Color.BLACK);
        box.setStroke(2, 0xFF2C2C2C);
        box.setCornerRadius(dp(6));
        k.setBackground(box);
        k.setTextColor(on ? Color.BLACK : 0xFF8B8B8B);
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

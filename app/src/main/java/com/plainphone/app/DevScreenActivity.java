package com.plainphone.app;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;
import java.util.Map;

/** Remote GUI: {@code screen} frames + a trackpad. Two layouts (Dev setting). */
public class DevScreenActivity extends Activity implements DevService.StateListener {

    private String hostId;
    private RemoteScreenView screen;
    private TrackpadView pad;
    private EditText keyInput;
    private View keyBar;
    private TextView chip;
    private DevService service;
    private DevConnection connection;
    private long channel = -1;
    private boolean opening;
    private int baseMaxW = 1280;
    private int curMaxW = 1280;
    private final android.os.Handler restream = new android.os.Handler();

    private final DevConnection.Sink sink = this::onChannelMessage;

    private final ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            service = ((DevService.LocalBinder) binder).service();
            tryOpen();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            service = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hostId = getIntent().getStringExtra(DevHostActivity.EXTRA_HOST_ID);
        DevHost host = DevHost.find(this, hostId);
        if (host == null) {
            finish();
            return;
        }
        setTaskDescription(new android.app.ActivityManager.TaskDescription(host.label + " · screen"));

        boolean padStyle = "pad".equals(Config.getDevTrackpadStyle(this));

        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setBackgroundColor(Color.BLACK);

        screen = new RemoteScreenView(this);
        screen.layout = padStyle ? RemoteScreenView.Layout.PAD : RemoteScreenView.Layout.WHOLE;
        screen.aspectLock = padStyle; // dedicated pad: size the mirror to the desktop, no black bars
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
            column.addView(screen, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            pad = new TrackpadView(this);
            pad.screen = screen;
            column.addView(pad, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        } else {
            FrameLayout stage = new FrameLayout(this);
            stage.addView(screen, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            chip = new TextView(this);
            chip.setText("MOVE");
            chip.setTypeface(Fonts.cascadiaMono(this));
            chip.setTextSize(11);
            chip.setPadding(24, 12, 24, 12);
            paintChip();
            chip.setOnClickListener(v -> screen.toggleMode());
            FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cp.leftMargin = cp.topMargin = 20;
            stage.addView(chip, cp);
            column.addView(stage, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        }

        keyInput = new EditText(this);
        keyInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        keyInput.setImeOptions(android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI);
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
        column.addView(keyInput, new LinearLayout.LayoutParams(1, 1));

        // Screen chrome: the "← Title" bar with a keyboard toggle pinned right.
        LinearLayout head = UiKit.header(this, host.label + " · screen");
        TextView kbd = new TextView(this);
        kbd.setText("⌨");
        kbd.setTextColor(Color.WHITE);
        kbd.setTextSize(18);
        kbd.setTypeface(Fonts.cascadiaMono(this));
        kbd.setGravity(Gravity.CENTER);
        kbd.setPadding(28, 0, 28, 0);
        kbd.setOnClickListener(v -> toggleKeyboard());
        head.addView(kbd, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);
        root.addView(head, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        View hair = new View(this);
        hair.setBackgroundColor(0xFF1C1C1C);
        root.addView(hair, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 1));
        root.addView(column, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        keyBar = buildKeyBar();
        keyBar.setVisibility(View.GONE);
        root.addView(keyBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(root);

        // When the keyboard goes away (back press, swipe-down), drop the key bar
        // too — but not in the moment right after we asked for it, before the
        // IME has animated up (that race made the first ⌨ tap a no-op).
        root.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            if (System.currentTimeMillis() - keyBarShownAt < 800) return;
            android.graphics.Rect r = new android.graphics.Rect();
            root.getWindowVisibleDisplayFrame(r);
            int screenH = root.getRootView().getHeight();
            boolean kbShown = screenH - r.bottom > screenH * 0.15f;
            if (!kbShown && keyBar.getVisibility() == View.VISIBLE) {
                keyBar.setVisibility(View.GONE);
                keyInput.clearFocus();
            }
        });
    }

    private long keyBarShownAt;

    /** ctrl/alt/shift armed for the next keystroke (sticky, like a terminal Ctrl). */
    private final java.util.LinkedHashSet<String> armedMods = new java.util.LinkedHashSet<>();
    private final Map<String, TextView> modKeys = new java.util.HashMap<>();

    /** Special keys the soft keyboard lacks, shown only while the keyboard is up. */
    private View buildKeyBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setPadding(8, 10, 8, 10);
        bar.addView(modKey("ctrl", "ctrl"));
        bar.addView(specialKey("tab", () -> sendKey("Tab")));
        bar.addView(modKey("alt", "alt"));
        bar.addView(specialKey("esc", () -> sendKey("Escape")));
        bar.addView(specialKey("^C", () -> send(DevProtocol.inputKey("c", null,
                java.util.Collections.singletonList("ctrl")))));
        bar.addView(specialKey("del", () -> sendKey("Delete")));
        bar.addView(modKey("shift", "shift"));
        bar.addView(specialKey("enter", () -> sendKey("Enter")));
        bar.addView(specialKey("↑", () -> sendKey("Up")));
        bar.addView(specialKey("↓", () -> sendKey("Down")));
        bar.addView(specialKey("←", () -> sendKey("Left")));
        bar.addView(specialKey("→", () -> sendKey("Right")));

        HorizontalScrollView scroller = new HorizontalScrollView(this);
        scroller.setHorizontalScrollBarEnabled(false);
        scroller.setBackgroundColor(0xFF0A0A0A);
        scroller.addView(bar);
        return scroller;
    }

    private void sendKey(String named) {
        send(DevProtocol.inputKey(null, named, consumeMods()));
    }

    /** The armed modifiers as a list, then disarm and repaint the mod keys. */
    private List<String> consumeMods() {
        if (armedMods.isEmpty()) return null;
        List<String> out = new java.util.ArrayList<>(armedMods);
        armedMods.clear();
        for (Map.Entry<String, TextView> e : modKeys.entrySet()) paintKey(e.getValue(), false);
        return out;
    }

    private TextView modKey(String label, String mod) {
        TextView k = specialKey(label, null);
        modKeys.put(mod, k);
        k.setOnClickListener(v -> {
            if (!armedMods.remove(mod)) armedMods.add(mod);
            paintKey(k, armedMods.contains(mod));
        });
        return k;
    }

    private TextView specialKey(String label, Runnable action) {
        TextView k = new TextView(this);
        k.setText(label);
        k.setTextSize(14);
        k.setTypeface(Fonts.cascadiaMono(this));
        k.setGravity(Gravity.CENTER);
        k.setPadding(26, 20, 26, 20);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.rightMargin = 8;
        k.setLayoutParams(lp);
        paintKey(k, false);
        if (action != null) k.setOnClickListener(v -> action.run());
        return k;
    }

    private void paintKey(TextView k, boolean on) {
        GradientDrawable box = new GradientDrawable();
        box.setColor(on ? Color.WHITE : Color.BLACK);
        box.setStroke(2, 0xFF2C2C2C);
        box.setCornerRadius(UiKit.dp(this, 6));
        k.setBackground(box);
        k.setTextColor(on ? Color.BLACK : 0xFF8B8B8B);
    }

    @Override
    protected void onStart() {
        super.onStart();
        DevService.addStateListener(this);
        bindService(new Intent(this, DevService.class), conn, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        DevService.connect(this, hostId);
        tryOpen();
    }

    @Override
    protected void onStop() {
        super.onStop();
        DevService.removeStateListener(this);
        restream.removeCallbacksAndMessages(null);
        if (channel >= 0 && connection != null) {
            connection.send(DevProtocol.screenStop(channel));
            connection.closeChannel(channel);
        }
        channel = -1;
        opening = false;
        if (service != null) service.setActivityDetail(null);
        try {
            unbindService(conn);
        } catch (IllegalArgumentException ignored) {
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        screen.release();
    }

    @Override
    public void onDevState() {
        runOnUiThread(() -> {
            DevConnection live = service != null ? service.connection() : null;
            if (connection != null && connection != live) {
                connection = null;
                channel = -1;
                opening = false;
            }
            tryOpen();
        });
    }

    private void tryOpen() {
        if (opening || channel >= 0 || service == null || !DevService.isConnected()) return;
        connection = service.connection();
        if (connection == null) return;
        opening = true;
        channel = connection.openChannel(sink);
        baseMaxW = Math.min(1280, getResources().getDisplayMetrics().widthPixels * 2);
        curMaxW = targetMaxW(screen.zoom());
        connection.send(DevProtocol.screenStart(channel, curMaxW, Config.getDevScreenFps(this), false));
        service.setActivityDetail("screen");
    }

    private void send(Map<String, Object> message) {
        if (connection != null) connection.send(message);
    }

    private void onChannelMessage(Map<String, Object> msg) {
        if (DevProtocol.T_SCREEN_FRAME.equals(DevProtocol.type(msg))) {
            int sw = (int) DevProtocol.num(msg, "sw", 0);
            int sh = (int) DevProtocol.num(msg, "sh", 0);
            if (sw > 0) screen.setSourceSize(sw, sh);
            byte[] data = DevProtocol.bin(msg, "data");
            if (data != null) screen.setFrame(data);
        }
    }

    /** Higher stream resolution while zoomed in, so cropping stays sharp. */
    private int targetMaxW(float zoom) {
        int want = Math.round(baseMaxW * Math.max(1f, zoom));
        want = Math.min(want, 2560);
        // Bucket to 640-px steps so small zoom changes don't thrash the stream.
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
                    channel, curMaxW, Config.getDevScreenFps(this), false));
        }, 280);
    }

    // --- controls -------------------------------------------------

    private void toggleKeyboard() {
        InputMethodManager imm = getSystemService(InputMethodManager.class);
        if (imm == null) return;
        boolean showing = keyBar.getVisibility() == View.VISIBLE;
        if (showing) {
            imm.hideSoftInputFromWindow(keyInput.getWindowToken(), 0);
            keyInput.clearFocus();
            keyBar.setVisibility(View.GONE);
            keyBarShownAt = 0;
        } else {
            keyInput.requestFocus();
            imm.showSoftInput(keyInput, InputMethodManager.SHOW_IMPLICIT);
            keyBar.setVisibility(View.VISIBLE);
            keyBarShownAt = System.currentTimeMillis();
        }
    }

    private void paintChip() {
        boolean move = screen.mode() == RemoteScreenView.Mode.MOVE;
        GradientDrawable box = new GradientDrawable();
        box.setColor(move ? Color.WHITE : Color.BLACK);
        box.setStroke(1, move ? Color.WHITE : 0xFF2C2C2C);
        box.setCornerRadius(UiKit.dp(this, 999));
        chip.setBackground(box);
        chip.setTextColor(move ? Color.BLACK : 0xFF8B8B8B);
    }
}

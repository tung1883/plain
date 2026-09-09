package com.plainphone.app;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Map;

/**
 * Interactive shell: a {@link TerminalView} over a persistent session on the
 * daemon, plus a key bar. The pty lives in {@code plaind} and keeps running
 * while the phone is away — {@link #onStop} only detaches, and reopening (with
 * the same {@link #EXTRA_SESSION_ID}) reattaches and replays the buffered output.
 */
public class DevTerminalActivity extends Activity implements DevService.StateListener {

    static final String EXTRA_SESSION_ID = "sessionId";

    private String hostId;
    private long sessionId = -1; // daemon session id; -1 until opened / for a new shell
    private TerminalView term;
    private TextView status;
    private View keyBar;
    private boolean kbVisible;
    private TextView ctrlKey, altKey, shiftKey;
    private DevService service;
    private DevConnection connection;
    private long channel = -1;
    private boolean opening;

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
        sessionId = getIntent().getLongExtra(EXTRA_SESSION_ID, -1);
        DevHost host = DevHost.find(this, hostId);
        if (host == null) {
            finish();
            return;
        }

        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setBackgroundColor(Color.BLACK);

        term = new TerminalView(this);
        term.onInput = bytes -> {
            if (channel >= 0 && connection != null) {
                connection.send(DevProtocol.ptyData(channel, bytes));
            }
        };
        term.onResize = (cols, rows) -> {
            if (channel >= 0 && connection != null) {
                connection.send(DevProtocol.ptyResize(channel, cols, rows));
            }
        };
        status = new TextView(this);
        status.setTypeface(Fonts.cascadiaMono(this));
        status.setTextSize(11);
        status.setTextColor(0xFFB0B0B0);
        status.setBackgroundColor(0xFF161616);
        status.setPadding(24, 8, 24, 8);
        status.setVisibility(View.GONE);
        column.addView(status, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        column.addView(term, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        keyBar = buildKeyBar();
        keyBar.setVisibility(View.GONE);
        column.addView(keyBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // "← host · shell" bar with a keyboard toggle pinned right.
        LinearLayout head = UiKit.header(this, host.label + " · shell");
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
        setContentView(root);

        // The key bar mirrors the soft keyboard: shown while it's up (⌨ toggle
        // or tapping the terminal), hidden when it's dismissed.
        root.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            android.graphics.Rect r = new android.graphics.Rect();
            root.getWindowVisibleDisplayFrame(r);
            int screenH = root.getRootView().getHeight();
            boolean up = screenH - r.bottom > screenH * 0.15f;
            if (up != kbVisible) {
                kbVisible = up;
                keyBar.setVisibility(up ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void toggleKeyboard() {
        android.view.inputmethod.InputMethodManager imm =
                getSystemService(android.view.inputmethod.InputMethodManager.class);
        if (imm == null) return;
        if (kbVisible) {
            imm.hideSoftInputFromWindow(term.getWindowToken(), 0);
        } else {
            term.showKeyboard();
        }
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
        term.requestFocus();
        term.postDelayed(term::showKeyboard, 150);
        tryOpen();
    }

    @Override
    protected void onStop() {
        super.onStop();
        DevService.removeStateListener(this);
        if (channel >= 0 && connection != null) {
            connection.send(DevProtocol.sessionDetach(channel)); // keep the shell running
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
    public void onDevState() {
        runOnUiThread(() -> {
            DevConnection live = service != null ? service.connection() : null;
            // The link was replaced by a reconnect — our old channel is dead.
            if (connection != null && connection != live) {
                connection = null;
                channel = -1;
                opening = false;
            }
            if (!DevService.isConnected()) showStatus("Reconnecting…");
            tryOpen();
        });
    }

    private void showStatus(String text) {
        if (status == null) return;
        if (text == null) {
            status.setVisibility(View.GONE);
        } else {
            status.setText(text);
            status.setVisibility(View.VISIBLE);
        }
    }

    private void tryOpen() {
        if (opening || channel >= 0 || service == null || !DevService.isConnected()) return;
        connection = service.connection();
        if (connection == null) return;
        opening = true;
        channel = connection.openChannel(sink);
        term.reset(); // the daemon replays this session's buffer right after
        connection.send(DevProtocol.sessionOpen(channel,
                sessionId >= 0 ? sessionId : null, null,
                Math.max(term.cols(), 20), Math.max(term.rows(), 6)));
        service.setActivityDetail("shell");
    }

    private void onChannelMessage(Map<String, Object> msg) {
        String type = DevProtocol.type(msg);
        if (DevProtocol.T_SESSION_OPENED.equals(type)) {
            sessionId = DevProtocol.num(msg, "id", sessionId);
            String name = DevProtocol.str(msg, "name");
            showStatus(null);
            if (service != null) {
                service.setActivityDetail("shell" + (name != null ? " · " + name : ""));
            }
        } else if (DevProtocol.T_SESSION_GONE.equals(type)) {
            // The old shell is gone (daemon restarted / killed) — start fresh.
            Toast.makeText(this, "Shell ended — opening a new one", Toast.LENGTH_SHORT).show();
            showStatus("Previous shell ended — new shell");
            status.postDelayed(() -> showStatus(null), 2500);
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
            Toast.makeText(this, "Shell exited (" + DevProtocol.num(msg, "code", 0) + ")",
                    Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private View buildKeyBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setPadding(8, 10, 8, 10);

        // Same set as the Screen key bar, plus the shell's punctuation keys.
        ctrlKey = key("ctrl", () -> { term.armCtrl(!term.ctrlArmed()); paintMods(); });
        altKey = key("alt", () -> { term.armAlt(!term.altArmed()); paintMods(); });
        shiftKey = key("shift", () -> { term.armShift(!term.shiftArmed()); paintMods(); });
        term.onModsCleared = this::paintMods;
        bar.addView(ctrlKey);
        bar.addView(key("tab", () -> term.barKey(new byte[]{'\t'})));
        bar.addView(altKey);
        bar.addView(key("esc", () -> term.barKey(new byte[]{0x1b})));
        bar.addView(key("^C", () -> term.sendBytes(new byte[]{0x03})));
        bar.addView(key("del", () -> term.barKey(TerminalView.esc("[3~"))));
        bar.addView(shiftKey);
        bar.addView(key("enter", () -> term.barKey(new byte[]{'\r'})));
        bar.addView(key("↑", () -> term.barArrow('A')));
        bar.addView(key("↓", () -> term.barArrow('B')));
        bar.addView(key("←", () -> term.barArrow('D')));
        bar.addView(key("→", () -> term.barArrow('C')));
        bar.addView(key("/", () -> term.sendString("/")));
        bar.addView(key("|", () -> term.sendString("|")));
        bar.addView(key(":", () -> term.sendString(":")));
        bar.addView(key("-", () -> term.sendString("-")));
        bar.addView(key("~", () -> term.sendString("~")));

        HorizontalScrollView scroller = new HorizontalScrollView(this);
        scroller.setHorizontalScrollBarEnabled(false);
        scroller.setBackgroundColor(0xFF0A0A0A);
        scroller.addView(bar);
        return scroller;
    }

    private TextView key(String label, Runnable action) {
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
        k.setBackground(box);
        k.setTextColor(on ? Color.BLACK : 0xFF8B8B8B);
    }
}

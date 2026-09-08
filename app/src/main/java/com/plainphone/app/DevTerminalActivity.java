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
import android.view.inputmethod.InputMethodManager;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Map;

/** Interactive shell: a {@link TerminalView} over a {@code pty} channel plus a key bar. */
public class DevTerminalActivity extends Activity implements DevService.StateListener {

    private String hostId;
    private TerminalView term;
    private TextView ctrlKey;
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
        column.addView(term, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        column.addView(buildKeyBar());

        UiKit.screen(this, host.label + " · shell", column);
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
        term.postDelayed(() -> {
            InputMethodManager imm = getSystemService(InputMethodManager.class);
            if (imm != null) imm.showSoftInput(term, InputMethodManager.SHOW_IMPLICIT);
        }, 150);
        tryOpen();
    }

    @Override
    protected void onStop() {
        super.onStop();
        DevService.removeStateListener(this);
        if (channel >= 0 && connection != null) {
            connection.send(DevProtocol.ptyClose(channel));
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
        runOnUiThread(this::tryOpen);
    }

    private void tryOpen() {
        if (opening || channel >= 0 || service == null || !DevService.isConnected()) return;
        connection = service.connection();
        if (connection == null) return;
        opening = true;
        channel = connection.openChannel(sink);
        connection.send(DevProtocol.ptyOpen(channel,
                Math.max(term.cols(), 20), Math.max(term.rows(), 6), null));
        service.setActivityDetail("shell");
    }

    private void onChannelMessage(Map<String, Object> msg) {
        String type = DevProtocol.type(msg);
        if (DevProtocol.T_PTY_DATA.equals(type)) {
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

        bar.addView(key("esc", () -> term.sendBytes(new byte[]{0x1b})));
        bar.addView(key("tab", () -> term.sendBytes(new byte[]{'\t'})));
        ctrlKey = key("ctrl", () -> {
            term.armCtrl(!term.ctrlArmed());
            paint(ctrlKey, term.ctrlArmed());
        });
        bar.addView(ctrlKey);
        bar.addView(key("↑", () -> term.sendBytes(TerminalView.esc("[A"))));
        bar.addView(key("↓", () -> term.sendBytes(TerminalView.esc("[B"))));
        bar.addView(key("←", () -> term.sendBytes(TerminalView.esc("[D"))));
        bar.addView(key("→", () -> term.sendBytes(TerminalView.esc("[C"))));
        bar.addView(key("/", () -> term.sendString("/")));
        bar.addView(key("|", () -> term.sendString("|")));
        bar.addView(key(":", () -> term.sendString(":")));
        bar.addView(key("-", () -> term.sendString("-")));
        bar.addView(key("~", () -> term.sendString("~")));
        bar.addView(key("⌨", () -> {
            InputMethodManager imm = getSystemService(InputMethodManager.class);
            if (imm != null) imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);
        }));

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

    private void paint(TextView k, boolean on) {
        GradientDrawable box = new GradientDrawable();
        box.setColor(on ? Color.WHITE : Color.BLACK);
        box.setStroke(2, 0xFF2C2C2C);
        k.setBackground(box);
        k.setTextColor(on ? Color.BLACK : 0xFF8B8B8B);
    }
}

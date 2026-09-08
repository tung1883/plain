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
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Map;

/** Remote GUI: {@code screen} frames in a {@link RemoteScreenView} plus trackpad and keyboard. */
public class DevScreenActivity extends Activity implements DevService.StateListener {

    private String hostId;
    private RemoteScreenView screen;
    private EditText keyInput;
    private TextView rightBtn;
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

        screen = new RemoteScreenView(this);
        screen.listener = new RemoteScreenView.Listener() {
            @Override
            public void move(float dx, float dy, float scroll) {
                send(DevProtocol.inputMove(dx, dy, scroll));
            }

            @Override
            public void click(String button, boolean doubleClick) {
                send(DevProtocol.inputClick(button, doubleClick));
            }

            @Override
            public void press(boolean down) {
                send(DevProtocol.msg(down ? DevProtocol.T_INPUT_DOWN : DevProtocol.T_INPUT_UP));
            }
        };
        column.addView(screen, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

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
                    send(DevProtocol.inputKey(s.subSequence(start + before, start + count).toString(), null));
                } else if (before > count) {
                    send(DevProtocol.inputKey(null, "Backspace"));
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

        column.addView(buildControls());
        UiKit.screen(this, host.label + " · screen", column);
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
        runOnUiThread(this::tryOpen);
    }

    private void tryOpen() {
        if (opening || channel >= 0 || service == null || !DevService.isConnected()) return;
        connection = service.connection();
        if (connection == null) return;
        opening = true;
        channel = connection.openChannel(sink);
        int width = Math.min(1280, getResources().getDisplayMetrics().widthPixels * 2);
        connection.send(DevProtocol.screenStart(channel, width, Config.getDevScreenFps(this)));
        service.setActivityDetail("screen");
    }

    private void send(Map<String, Object> message) {
        if (connection != null) connection.send(message);
    }

    private void onChannelMessage(Map<String, Object> msg) {
        if (DevProtocol.T_SCREEN_FRAME.equals(DevProtocol.type(msg))) {
            byte[] data = DevProtocol.bin(msg, "data");
            if (data != null) screen.setFrame(data);
        }
    }

    private View buildControls() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setBackgroundColor(0xFF0A0A0A);
        bar.setPadding(12, 12, 12, 12);

        rightBtn = control("R-CLICK", () -> {
            screen.rightClickArmed = !screen.rightClickArmed;
            paint(rightBtn, screen.rightClickArmed);
        });
        bar.addView(rightBtn, weight());
        bar.addView(control("KEYBOARD", () -> {
            keyInput.requestFocus();
            InputMethodManager imm = getSystemService(InputMethodManager.class);
            if (imm != null) imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);
        }), weight());
        bar.addView(control("C-A-DEL", () ->
                send(DevProtocol.inputKey(null, "ctrl-alt-delete"))), weight());
        return bar;
    }

    private LinearLayout.LayoutParams weight() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.leftMargin = lp.rightMargin = 6;
        return lp;
    }

    private TextView control(String label, Runnable action) {
        TextView t = new TextView(this);
        t.setText(label);
        t.setTextSize(12);
        t.setGravity(Gravity.CENTER);
        t.setTypeface(Fonts.cascadiaMono(this));
        t.setPadding(0, 22, 0, 22);
        paint(t, false);
        t.setOnClickListener(v -> action.run());
        return t;
    }

    private void paint(TextView t, boolean on) {
        GradientDrawable box = new GradientDrawable();
        box.setColor(on ? Color.WHITE : Color.BLACK);
        box.setStroke(2, Color.WHITE);
        t.setBackground(box);
        t.setTextColor(on ? Color.BLACK : Color.WHITE);
    }
}

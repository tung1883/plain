package com.plainphone.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/**
 * Add a device from a {@code plaind://host:port/<token>} link. MVP is
 * paste-only — the QR scanner is a fast-follow. Pairing opens one connection to
 * confirm the token, then saves the host and its key.
 */
public class DevPairActivity extends Activity {

    private EditText link;
    private TextView status;
    private TextView go;
    private Typeface font;
    private DevConnection probe;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        font = Fonts.current(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);
        root.setPadding(48, 40, 48, 40);

        LinearLayout fieldRow = new LinearLayout(this);
        fieldRow.setOrientation(LinearLayout.HORIZONTAL);
        fieldRow.setGravity(Gravity.CENTER_VERTICAL);
        fieldRow.setBackground(UiKit.rounded(this, Color.BLACK, Color.WHITE, 2f, UiKit.R_SM));
        UiKit.clipRounded(this, fieldRow, UiKit.R_SM);

        link = new EditText(this);
        link.setHint("plaind://…");
        link.setHintTextColor(Color.GRAY);
        link.setTextColor(Color.WHITE);
        link.setTypeface(font);
        link.setBackground(null);
        link.setPadding(32, 24, 16, 24);
        link.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        link.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_GO);
        link.setOnEditorActionListener((v, id, e) -> { startPairing(); return true; });
        fieldRow.addView(link, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        go = new TextView(this);
        go.setText("→");
        go.setTextColor(Color.WHITE);
        go.setTextSize(22);
        go.setTypeface(font);
        go.setGravity(Gravity.CENTER);
        go.setPadding(16, 20, 28, 20);
        go.setOnClickListener(v -> startPairing());
        fieldRow.addView(go);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        root.addView(fieldRow, lp);

        status = label("");
        status.setPadding(0, 28, 0, 0);
        root.addView(status);

        UiKit.screen(this, "Add device", root);
    }

    private void startPairing() {
        DevHost.Parsed parsed = DevHost.parseLink(link.getText().toString());
        if (parsed == null) {
            status.setText("That doesn't look like a plaind:// link.");
            return;
        }
        go.setEnabled(false);
        status.setText("Connecting to " + parsed.host + "…");

        Handler main = new Handler(Looper.getMainLooper());
        probe = new DevConnection(parsed.host, parsed.port, parsed.token,
                android.os.Build.MODEL == null ? "phone" : android.os.Build.MODEL,
                new DevConnection.Listener() {
                    private boolean done;

                    @Override
                    public void onConnected(String host, String os, List<Object> caps) {
                        done = true;
                        String label = host != null && !host.isEmpty() ? host : parsed.host;
                        DevHost saved = new DevHost(DevHost.newId(), label, parsed.host, parsed.port);
                        saved.save(DevPairActivity.this, parsed.token);
                        probe.close();
                        main.post(() -> {
                            Toast.makeText(DevPairActivity.this, "Paired with " + label,
                                    Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(DevPairActivity.this, DevHostActivity.class)
                                    .putExtra(DevHostActivity.EXTRA_HOST_ID, saved.id));
                            finish();
                        });
                    }

                    @Override
                    public void onDisconnected(String reason) {
                        if (done) return;
                        main.post(() -> {
                            status.setText("Pairing failed: " + reason);
                            go.setEnabled(true);
                        });
                    }
                });
        probe.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (probe != null) probe.close();
    }

    private TextView label(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextColor(Color.GRAY);
        t.setTextSize(13);
        t.setTypeface(font);
        t.setLineSpacing(0, 1.4f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = 24;
        t.setLayoutParams(lp);
        return t;
    }
}

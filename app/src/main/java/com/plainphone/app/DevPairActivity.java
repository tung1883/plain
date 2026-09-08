package com.plainphone.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/**
 * Add a computer from a {@code plaind://host:port/<token>} link. MVP is
 * paste-only — the QR scanner is a fast-follow. Pairing opens one connection to
 * confirm the token, then saves the host and its key.
 */
public class DevPairActivity extends Activity {

    private EditText link;
    private TextView status;
    private Button pair;
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

        root.addView(label("On the computer, run  plaind pair  and paste the link it prints."));

        link = new EditText(this);
        link.setHint("plaind://…");
        link.setHintTextColor(Color.GRAY);
        link.setTextColor(Color.WHITE);
        link.setTypeface(font);
        link.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        UiKit.style(this, link);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = 28;
        root.addView(link, lp);

        pair = new Button(this);
        pair.setText("Pair");
        UiKit.style(this, pair);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bp.topMargin = 24;
        pair.setOnClickListener(v -> startPairing());
        root.addView(pair, bp);

        status = label("");
        status.setPadding(0, 28, 0, 0);
        root.addView(status);

        root.addView(label("The key is stored in the Android keystore. Dev unlocks with your PIN "
                + "and never connects on its own."));

        UiKit.screen(this, "Add computer", root);
    }

    private void startPairing() {
        DevHost.Parsed parsed = DevHost.parseLink(link.getText().toString());
        if (parsed == null) {
            status.setText("That doesn't look like a plaind:// link.");
            return;
        }
        pair.setEnabled(false);
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
                            pair.setEnabled(true);
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

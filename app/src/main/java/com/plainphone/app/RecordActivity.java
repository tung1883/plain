package com.plainphone.app;

import android.Manifest;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.content.pm.PackageManager;

/**
 * Records one memo, full screen (home Recorder section). The take UI is {@link RecordingSurface};
 * the capture lives in {@link RecorderService}, so leaving this screen keeps it running with a
 * notification. Only <b>Stop</b> — here or in the notification — ends the take.
 */
public class RecordActivity extends Activity {

    private static final int REQ_MIC = 7701;

    private Typeface font;
    private LinearLayout root;
    private RecordingSurface surface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        font = Fonts.current(this);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);
        root.setGravity(Gravity.CENTER);
        root.setPadding(60, 60, 60, 60);
        UiKit.screen(this, "Recording", root);

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_MIC);
            return;
        }
        begin();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode != REQ_MIC) return;
        if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
            begin();
        } else {
            showDenied();
        }
    }

    private void begin() {
        surface = new RecordingSurface(this, this::finish);
        root.removeAllViews();
        root.addView(surface, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        surface.begin();
    }

    private void showDenied() {
        root.removeAllViews();
        root.setGravity(Gravity.CENTER);
        TextView msg = new TextView(this);
        msg.setText("plainphone needs microphone access to record.");
        msg.setTextColor(0xFFB5B5B5);
        msg.setTextSize(15);
        msg.setTypeface(font);
        msg.setGravity(Gravity.CENTER);
        root.addView(msg);

        Button close = new Button(this);
        close.setText("Close");
        UiKit.style(this, close);
        close.setOnClickListener(v -> finish());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = 40;
        root.addView(close, lp);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (surface != null) surface.release();
    }
}

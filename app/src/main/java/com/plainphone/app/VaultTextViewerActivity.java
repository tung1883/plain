package com.plainphone.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.charset.StandardCharsets;

/** Decrypts one vault text entry into memory and shows it. FLAG_SECURE, no share. */
public class VaultTextViewerActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

        if (!VaultSession.get().isUnlocked()) {
            finish();
            return;
        }

        TextView text = new TextView(this);
        text.setTextColor(Color.WHITE);
        text.setTypeface(Fonts.current(this));
        text.setTextSize(15);
        text.setTextIsSelectable(false);
        text.setPadding(48, 40, 48, 40);

        try {
            byte[] plain = VaultStore.decryptToMemory(this, getIntent().getStringExtra("docId"));
            text.setText(new String(plain, StandardCharsets.UTF_8));
        } catch (Exception e) {
            Toast.makeText(this, "Couldn't open", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ScrollView scroller = new ScrollView(this);
        scroller.setBackgroundColor(Color.BLACK);
        scroller.addView(text);
        setContentView(scroller);
        VaultUnlockService.touch(this);
    }
}

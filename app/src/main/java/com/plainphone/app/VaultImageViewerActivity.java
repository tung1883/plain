package com.plainphone.app;

import android.app.Activity;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

/** Decrypts one vault image into memory and shows it. FLAG_SECURE, no share. */
public class VaultImageViewerActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

        if (!VaultSession.get().isUnlocked()) {
            finish();
            return;
        }

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        ImageView image = new ImageView(this);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setOnClickListener(v -> finish());
        root.addView(image, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        try {
            byte[] plain = VaultStore.decryptToMemory(this, getIntent().getStringExtra("docId"));
            image.setImageBitmap(BitmapFactory.decodeByteArray(plain, 0, plain.length));
        } catch (Exception e) {
            Toast.makeText(this, "Couldn't open", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        TextView close = new TextView(this);
        close.setText("✕");
        close.setTextColor(Color.WHITE);
        close.setTextSize(20);
        close.setTypeface(Fonts.current(this));
        close.setPadding(40, 32, 40, 32);
        close.setOnClickListener(v -> finish());
        FrameLayout.LayoutParams closeParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.END);
        root.addView(close, closeParams);

        setContentView(root);
        VaultUnlockService.touch(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!VaultSession.get().isUnlocked()) finish();
    }
}

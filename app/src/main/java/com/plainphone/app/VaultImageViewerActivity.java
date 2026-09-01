package com.plainphone.app;

import android.app.Activity;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.ImageView;
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

        ImageView image = new ImageView(this);
        image.setBackgroundColor(Color.BLACK);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);

        try {
            byte[] plain = VaultStore.decryptToMemory(this, getIntent().getStringExtra("docId"));
            image.setImageBitmap(BitmapFactory.decodeByteArray(plain, 0, plain.length));
        } catch (Exception e) {
            Toast.makeText(this, "Couldn't open", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setContentView(image);
        VaultUnlockService.touch(this);
    }
}

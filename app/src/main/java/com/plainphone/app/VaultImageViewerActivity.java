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
            image.setImageBitmap(decodeDownsampled(plain));
        } catch (Throwable e) {
            Toast.makeText(this, "Couldn't open", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String name = getIntent().getStringExtra("name");
        android.widget.LinearLayout headerBar = UiKit.header(this, name == null ? "Image" : name);
        FrameLayout.LayoutParams headerParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP);
        root.addView(headerBar, headerParams);

        setContentView(root);
        VaultUnlockService.touch(this);
    }

    /** Decode at most ~2x the screen resolution so a big photo can't OOM. */
    private android.graphics.Bitmap decodeDownsampled(byte[] data) {
        android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
        int target = Math.max(dm.widthPixels, dm.heightPixels) * 2;

        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, bounds);

        int sample = 1;
        int longest = Math.max(bounds.outWidth, bounds.outHeight);
        while (longest / sample > target) sample *= 2;

        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = sample;
        return BitmapFactory.decodeByteArray(data, 0, data.length, opts);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!VaultSession.get().isUnlocked()) finish();
    }
}

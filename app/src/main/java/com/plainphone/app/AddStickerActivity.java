package com.plainphone.app;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation;
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenter;
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions;

/** Opened from the keyboard's image tab: pick a picture, optionally cut its background out, add
 * it to the keyboard's own images. */
public class AddStickerActivity extends Activity {

    private static final int PICK = 1;

    private Bitmap original, current;
    private ImageView preview;
    private TextView bgToggle;
    private boolean bgRemoved;
    private SubjectSegmenter segmenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) { finish(); return; }
        Intent pick = new Intent(Intent.ACTION_GET_CONTENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(pick, PICK);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No photo picker found", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Uri uri = resultCode == RESULT_OK && data != null ? data.getData() : null;
        if (uri == null) { finish(); return; }
        original = StickerStore.scaledBitmap(this, uri);
        if (original == null) {
            Toast.makeText(this, "Could not read the image", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        current = original;
        showPreview();
    }

    private void showPreview() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        preview = new ImageView(this);
        preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        preview.setImageBitmap(current);
        root.addView(preview, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        FrameLayout.LayoutParams headerParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        headerParams.gravity = Gravity.TOP;
        root.addView(UiKit.header(this, "Add image"), headerParams);

        bgToggle = new TextView(this);
        bgToggle.setTextColor(Color.WHITE);
        bgToggle.setTextSize(14);
        bgToggle.setTypeface(Fonts.current(this));
        bgToggle.setPadding(24, 24, 24, 24);
        bgToggle.setText("Remove background");
        bgToggle.setOnClickListener(v -> toggleBackground());
        FrameLayout.LayoutParams toggleParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        toggleParams.gravity = Gravity.TOP | Gravity.END;
        toggleParams.topMargin = UiKit.dp(this, 6);
        toggleParams.rightMargin = UiKit.dp(this, 4);
        root.addView(bgToggle, toggleParams);

        Button save = new Button(this);
        save.setText("Save");
        UiKit.style(this, save);
        save.setOnClickListener(v -> {
            if (StickerStore.save(this, current)) {
                setResult(RESULT_OK);
            } else {
                Toast.makeText(this, "Could not save the image", Toast.LENGTH_SHORT).show();
            }
            finish();
        });
        FrameLayout.LayoutParams saveParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        saveParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        saveParams.bottomMargin = UiKit.dp(this, 32);
        root.addView(save, saveParams);

        setContentView(root);
    }

    /** Cuts the subject out on first tap (background pixels made transparent); tap again to go
     * back to the original photo. Runs on-device, no network. */
    private void toggleBackground() {
        if (bgRemoved) {
            bgRemoved = false;
            current = original;
            preview.setImageBitmap(current);
            bgToggle.setText("Remove background");
            return;
        }
        bgToggle.setText("Removing background...");
        if (segmenter == null) {
            segmenter = SubjectSegmentation.getClient(
                    new SubjectSegmenterOptions.Builder().enableForegroundBitmap().build());
        }
        segmenter.process(InputImage.fromBitmap(original, 0))
                .addOnSuccessListener(result -> {
                    Bitmap fg = result.getForegroundBitmap();
                    if (fg == null) {
                        bgToggle.setText("Remove background");
                        Toast.makeText(this, "No clear subject found", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    bgRemoved = true;
                    current = fg;
                    preview.setImageBitmap(current);
                    bgToggle.setText("Background removed");
                })
                .addOnFailureListener(e -> {
                    bgToggle.setText("Remove background");
                    Toast.makeText(this, "Could not remove the background", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onDestroy() {
        if (segmenter != null) segmenter.close();
        super.onDestroy();
    }
}

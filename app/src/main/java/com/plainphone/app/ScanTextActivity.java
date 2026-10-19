package com.plainphone.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.IOException;

/**
 * Opened from the keyboard's scan button: pick a photo, read its text on the device, and leave it
 * in {@link #pending} for the keyboard to type once it is back in front of the field.
 */
public class ScanTextActivity extends Activity {

    private static final int PICK = 1;

    /** Text waiting for the keyboard to insert, or null. */
    static volatile String pending;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) return;
        Intent pick = new Intent(Intent.ACTION_GET_CONTENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(pick, PICK);
        } catch (android.content.ActivityNotFoundException e) {
            Toast.makeText(this, "No photo picker found", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Uri uri = resultCode == RESULT_OK && data != null ? data.getData() : null;
        if (uri == null) {
            finish();
            return;
        }
        InputImage image;
        try {
            image = InputImage.fromFilePath(this, uri);
        } catch (IOException e) {
            Toast.makeText(this, "Could not open the image", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).process(image)
                .addOnSuccessListener(text -> {
                    String out = text.getText().trim();
                    if (out.isEmpty()) {
                        Toast.makeText(this, "No text found", Toast.LENGTH_SHORT).show();
                    } else {
                        pending = out;
                    }
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Could not read the image", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }
}

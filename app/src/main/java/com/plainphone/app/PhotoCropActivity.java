package com.plainphone.app;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

public class PhotoCropActivity extends Activity {

    private static final float MIN_ZOOM = 0.4f;
    private static final float MAX_ZOOM = 4f;
    private static final float DOUBLE_TAP_ZOOM = 2.5f;

    private PhotoArtView preview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String uriString = getIntent().getStringExtra("uri");
        Uri uri = Uri.parse(uriString);

        boolean adjustingExisting = uriString.equals(Config.getArtPhotoUri(this));

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        FrameLayout frame = new FrameLayout(this);
        frame.setForeground(UiKit.frameBorder());

        preview = new PhotoArtView(this, uri,
                adjustingExisting ? Config.getArtPhotoFocusX(this) : 0.5f,
                adjustingExisting ? Config.getArtPhotoFocusY(this) : 0.5f,
                adjustingExisting ? Config.getArtPhotoZoom(this) : 1f);
        frame.addView(preview, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        FrameLayout.LayoutParams frameParams = new FrameLayout.LayoutParams(640, 800);
        frameParams.gravity = Gravity.CENTER;
        root.addView(frame, frameParams);

        ScaleGestureDetector scaleDetector = new ScaleGestureDetector(this,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override
                    public boolean onScale(ScaleGestureDetector detector) {
                        preview.setZoom(clampZoom(preview.getZoom() * detector.getScaleFactor()));
                        preview.invalidate();
                        return true;
                    }
                });

        GestureDetector gestureDetector = new GestureDetector(this,
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onDoubleTap(MotionEvent e) {
                        preview.setZoom(preview.getZoom() > 1f ? 1f : DOUBLE_TAP_ZOOM);
                        preview.invalidate();
                        return true;
                    }
                });

        float[] lastTouch = new float[2];
        frame.setOnTouchListener((v, event) -> {
            scaleDetector.onTouchEvent(event);
            gestureDetector.onTouchEvent(event);

            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                lastTouch[0] = event.getRawX();
                lastTouch[1] = event.getRawY();
            } else if (event.getActionMasked() == MotionEvent.ACTION_MOVE
                    && event.getPointerCount() == 1) {
                float dx = event.getRawX() - lastTouch[0];
                float dy = event.getRawY() - lastTouch[1];
                lastTouch[0] = event.getRawX();
                lastTouch[1] = event.getRawY();
                preview.panBy(dx, dy);
                preview.invalidate();
            }
            return true;
        });

        Typeface georgia = Fonts.current(this);

        TextView hint = new TextView(this);
        hint.setText("Pinch to zoom, drag to reposition");
        hint.setTextColor(Color.GRAY);
        hint.setTextSize(14);
        hint.setTypeface(georgia);
        hint.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams hintParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        hintParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        hintParams.topMargin = 64;
        root.addView(hint, hintParams);

        Button set = new Button(this);
        set.setText("Set");
        UiKit.style(this, set);
        set.setOnClickListener(v -> {
            Config.setArtPhotoUri(this, uriString);
            Config.setArtPhotoCrop(this, preview.getFocusX(), preview.getFocusY(), preview.getZoom());
            Config.setPhotoArtSelected(this, true);
            Config.setPixelArtEnabled(this, true);
            setResult(RESULT_OK);
            finish();
        });
        FrameLayout.LayoutParams setParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        setParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        setParams.bottomMargin = 64;
        root.addView(set, setParams);

        Button cancel = new Button(this);
        cancel.setText("X");
        UiKit.style(this, cancel);
        cancel.setOnClickListener(v -> finish());
        FrameLayout.LayoutParams cancelParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        cancelParams.gravity = Gravity.TOP | Gravity.END;
        cancelParams.topMargin = 48;
        cancelParams.rightMargin = 48;
        root.addView(cancel, cancelParams);

        setContentView(root);
    }

    private float clampZoom(float zoom) {
        return Math.max(MIN_ZOOM, Math.min(zoom, MAX_ZOOM));
    }
}


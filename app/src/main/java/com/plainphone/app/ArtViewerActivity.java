package com.plainphone.app;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

/** A smaller, framed, pinch-to-zoom/pan view of whichever home-screen art is currently selected. */
public class ArtViewerActivity extends Activity {

    private static final float MIN_SCALE = 1f;
    private static final float MAX_SCALE = 6f;
    private static final float DOUBLE_TAP_SCALE = 2.5f;

    private View content;
    private float scaleFactor = 1f;
    private float translateX = 0f;
    private float translateY = 0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        FrameLayout artFrame = new FrameLayout(this);
        artFrame.setForeground(UiKit.frameBorder());

        content = new GifArtView(this, Config.getGifScene(this));
        artFrame.addView(content, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        FrameLayout.LayoutParams artFrameParams = new FrameLayout.LayoutParams(640, 800);
        artFrameParams.gravity = Gravity.CENTER;
        root.addView(artFrame, artFrameParams);

        ScaleGestureDetector scaleDetector = new ScaleGestureDetector(this,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override
                    public boolean onScale(ScaleGestureDetector detector) {
                        float newScale = clampScale(scaleFactor * detector.getScaleFactor());
                        // How much scale actually changed after clamping — keeps the focal
                        // point calculation correct even once MAX_SCALE/MIN_SCALE is hit.
                        float appliedFactor = newScale / scaleFactor;

                        float cx = content.getWidth() / 2f;
                        float cy = content.getHeight() / 2f;
                        float fx = detector.getFocusX();
                        float fy = detector.getFocusY();

                        // Keeps the content point under the fingers fixed on screen as the
                        // scale changes, instead of always zooming from the view's center.
                        translateX = fx - cx - appliedFactor * (fx - cx - translateX);
                        translateY = fy - cy - appliedFactor * (fy - cy - translateY);

                        scaleFactor = newScale;
                        clampTranslation();
                        applyTransform();
                        return true;
                    }
                });

        GestureDetector gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                scaleFactor = scaleFactor > 1f ? 1f : DOUBLE_TAP_SCALE;
                translateX = 0f;
                translateY = 0f;
                applyTransform();
                return true;
            }
        });

        float[] lastTouch = new float[2];
        artFrame.setOnTouchListener((v, event) -> {
            scaleDetector.onTouchEvent(event);
            gestureDetector.onTouchEvent(event);

            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                lastTouch[0] = event.getRawX();
                lastTouch[1] = event.getRawY();
            } else if (event.getActionMasked() == MotionEvent.ACTION_MOVE
                    && event.getPointerCount() == 1 && scaleFactor > 1f) {
                translateX += event.getRawX() - lastTouch[0];
                translateY += event.getRawY() - lastTouch[1];
                lastTouch[0] = event.getRawX();
                lastTouch[1] = event.getRawY();
                clampTranslation();
                applyTransform();
            }
            return true;
        });

        Typeface georgia = Fonts.current(this);
        Button close = new Button(this);
        close.setText("X");
        UiKit.style(this, close);
        close.setTypeface(georgia);
        close.setOnClickListener(v -> finish());
        FrameLayout.LayoutParams closeParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        closeParams.gravity = Gravity.TOP | Gravity.END;
        closeParams.topMargin = 48;
        closeParams.rightMargin = 48;
        root.addView(close, closeParams);

        setContentView(root);
    }

    private float clampScale(float scale) {
        return Math.max(MIN_SCALE, Math.min(scale, MAX_SCALE));
    }

    // Keeps the scaled content covering the whole frame — at scaleFactor 1 this pins
    // translation to exactly (0,0), so no blank space is ever visible past the content's edge.
    private void clampTranslation() {
        float maxX = content.getWidth() * (scaleFactor - 1) / 2f;
        float maxY = content.getHeight() * (scaleFactor - 1) / 2f;
        translateX = Math.max(-maxX, Math.min(translateX, maxX));
        translateY = Math.max(-maxY, Math.min(translateY, maxY));
    }

    private void applyTransform() {
        content.setScaleX(scaleFactor);
        content.setScaleY(scaleFactor);
        content.setTranslationX(translateX);
        content.setTranslationY(translateY);
    }
}

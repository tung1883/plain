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
import android.widget.TextView;

public class ArtViewerActivity extends Activity {

    private static final float MIN_SCALE = 1f;
    private static final float MAX_SCALE = 6f;
    private static final float DOUBLE_TAP_SCALE = 2.5f;

    private View content;
    private FrameLayout artFrame;
    private TextView counter;
    private float scaleFactor = 1f;
    private float translateX = 0f;
    private float translateY = 0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        artFrame = new FrameLayout(this);
        artFrame.setForeground(UiKit.frameBorder(this));

        content = ArtKit.homeArt(this);
        if (content == null) { finish(); return; }
        artFrame.addView(content, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        if (ArtGallery.selectedIds(this).size() >= 2) addArrows();

        FrameLayout.LayoutParams artFrameParams = new FrameLayout.LayoutParams(640, 800);
        artFrameParams.gravity = Gravity.CENTER;
        root.addView(artFrame, artFrameParams);

        ScaleGestureDetector scaleDetector = new ScaleGestureDetector(this,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override
                    public boolean onScale(ScaleGestureDetector detector) {
                        float newScale = clampScale(scaleFactor * detector.getScaleFactor());

                        float appliedFactor = newScale / scaleFactor;

                        float cx = content.getWidth() / 2f;
                        float cy = content.getHeight() / 2f;
                        float fx = detector.getFocusX();
                        float fy = detector.getFocusY();

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

            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN
                    || action == MotionEvent.ACTION_POINTER_DOWN
                    || action == MotionEvent.ACTION_POINTER_UP) {
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

        FrameLayout.LayoutParams closeParams = new FrameLayout.LayoutParams(
                UiKit.backWidth(this), UiKit.backHeight(this));
        closeParams.gravity = Gravity.TOP | Gravity.START;
        closeParams.topMargin = UiKit.dp(this, 8);
        root.addView(UiKit.backButton(this, this::finish), closeParams);

        setContentView(root);
    }

    /** Carousel only: ‹ › on the frame edges and a position counter. Touches on them skip the pan/zoom. */
    private void addArrows() {
        artFrame.addView(arrow("‹", Gravity.START, -1));
        artFrame.addView(arrow("›", Gravity.END, 1));
        counter = new TextView(this);
        counter.setTextColor(Color.WHITE);
        counter.setTextSize(14);
        counter.setTypeface(Fonts.current(this));
        counter.setBackgroundColor(0x99000000);
        counter.setPadding(UiKit.dp(this, 8), UiKit.dp(this, 2), UiKit.dp(this, 8), UiKit.dp(this, 2));
        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        cp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        cp.bottomMargin = UiKit.dp(this, 8);
        artFrame.addView(counter, cp);
        updateCounter();
    }

    private TextView arrow(String glyph, int side, int delta) {
        TextView t = new TextView(this);
        t.setText(glyph);
        t.setTextColor(Color.WHITE);
        t.setTextSize(30);
        t.setTypeface(Fonts.current(this));
        t.setGravity(Gravity.CENTER);
        t.setBackgroundColor(0x99000000);
        t.setContentDescription(delta < 0 ? "Previous image" : "Next image");
        t.setOnClickListener(v -> step(delta));
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(UiKit.dp(this, 40), UiKit.dp(this, 64));
        lp.gravity = Gravity.CENTER_VERTICAL | side;
        t.setLayoutParams(lp);
        return t;
    }

    private void step(int delta) {
        ArtGallery.step(this, delta);
        View next = ArtKit.homeArt(this);
        if (next == null) return;
        artFrame.removeView(content);
        content = next;
        artFrame.addView(content, 0, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        scaleFactor = 1f;
        translateX = 0f;
        translateY = 0f;
        updateCounter();
    }

    private void updateCounter() {
        if (counter == null) return;
        counter.setText((ArtGallery.currentPosition(this) + 1) + " / " + ArtGallery.selectedIds(this).size());
    }

    private float clampScale(float scale) {
        return Math.max(MIN_SCALE, Math.min(scale, MAX_SCALE));
    }

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


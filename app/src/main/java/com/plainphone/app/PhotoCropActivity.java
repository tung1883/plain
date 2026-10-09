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
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Fullscreen pan / zoom editor for one art target — a built-in scene
 * (<code>"scene:&lt;NAME&gt;"</code>) or a gallery item (<code>"item:&lt;id&gt;"</code>).
 * Pan works at every zoom (below cover-fit the image slides in the frame).
 */
public class PhotoCropActivity extends Activity {

    private static final float MIN_ZOOM = 0.4f;
    private static final float MAX_ZOOM = 4f;
    private static final float DOUBLE_TAP_ZOOM = 2.5f;

    private CropArt preview;
    private View previewView;

    private GifScene scene;      // set for a scene target
    private String itemId;       // set for a gallery target
    private boolean gray = true;
    private int turns;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String target = getIntent().getStringExtra("target");
        if (target == null) { finish(); return; }

        float fx = 0.5f, fy = 0.5f, z = 1f;

        if (target.startsWith("scene:")) {
            try {
                scene = GifScene.valueOf(target.substring(6));
            } catch (Exception e) { finish(); return; }
            float[] c = Config.getSceneCrop(this, scene);
            fx = c[0]; fy = c[1]; z = c[2]; gray = c[3] != 0f; turns = (int) c[4];
        } else if (target.startsWith("item:")) {
            itemId = target.substring(5);
            ArtItem it = ArtGallery.get(this, itemId);
            if (it == null) { finish(); return; }
            fx = it.fx; fy = it.fy; z = it.zoom; gray = it.gray; turns = it.turns;
        } else {
            finish();
            return;
        }

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        FrameLayout frame = new FrameLayout(this);
        frame.setForeground(UiKit.frameBorder(this));

        if (scene != null) {
            GifArtView g = new GifArtView(this, scene, fx, fy, z, gray);
            preview = g;
            previewView = g;
        } else {
            ArtItem it = ArtGallery.get(this, itemId);
            if (it.isGif()) {
                GifArtView g = new GifArtView(this, it.file(this), fx, fy, z, gray);
                preview = g;
                previewView = g;
            } else {
                PhotoArtView p = new PhotoArtView(this, Uri.fromFile(it.file(this)), fx, fy, z, gray);
                preview = p;
                previewView = p;
            }
        }
        preview.setQuarterTurns(turns);
        frame.addView(previewView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        FrameLayout.LayoutParams frameParams = new FrameLayout.LayoutParams(640, 800);
        frameParams.gravity = Gravity.CENTER;
        root.addView(frame, frameParams);

        ScaleGestureDetector scaleDetector = new ScaleGestureDetector(this,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override
                    public boolean onScale(ScaleGestureDetector detector) {
                        preview.setZoom(clampZoom(preview.getZoom() * detector.getScaleFactor()));
                        previewView.invalidate();
                        return true;
                    }
                });

        GestureDetector gestureDetector = new GestureDetector(this,
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onDoubleTap(MotionEvent e) {
                        preview.setZoom(preview.getZoom() > 1f ? 1f : DOUBLE_TAP_ZOOM);
                        previewView.invalidate();
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
                previewView.invalidate();
            }
            return true;
        });

        Typeface font = Fonts.current(this);

        LinearLayout headerBar = UiKit.header(this, "Crop");
        FrameLayout.LayoutParams headerParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        headerParams.gravity = Gravity.TOP;
        root.addView(headerBar, headerParams);

        TextView grayToggle = new TextView(this);
        grayToggle.setTextColor(Color.WHITE);
        grayToggle.setTextSize(14);
        grayToggle.setTypeface(font);
        grayToggle.setPadding(24, 24, 24, 24);
        grayToggle.setText(gray ? "Grayscale: On" : "Grayscale: Off");
        grayToggle.setOnClickListener(v -> {
            gray = !gray;
            grayToggle.setText(gray ? "Grayscale: On" : "Grayscale: Off");
            if (previewView instanceof GifArtView) ((GifArtView) previewView).setGrayscale(gray);
            else if (previewView instanceof PhotoArtView) ((PhotoArtView) previewView).setGrayscale(gray);
        });
        FrameLayout.LayoutParams grayParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        grayParams.gravity = Gravity.TOP | Gravity.END;
        grayParams.topMargin = UiKit.dp(this, 6);
        grayParams.rightMargin = UiKit.dp(this, 4);
        root.addView(grayToggle, grayParams);

        Button set = new Button(this);
        set.setText("Set");
        UiKit.style(this, set);
        set.setOnClickListener(v -> {
            if (scene != null) {
                Config.setSceneCrop(this, scene,
                        preview.getFocusX(), preview.getFocusY(), preview.getZoom());
                Config.setSceneGray(this, scene, gray);
                Config.setSceneTurns(this, scene, turns);
            } else {
                ArtGallery.updateCrop(this, itemId,
                        preview.getFocusX(), preview.getFocusY(), preview.getZoom());
                ArtGallery.setGray(this, itemId, gray);
                ArtGallery.setTurns(this, itemId, turns);
            }
            setResult(RESULT_OK);
            finish();
        });
        Button rotL = new Button(this);
        rotL.setText("\u21BA");
        UiKit.style(this, rotL);
        rotL.setOnClickListener(v -> rotate(-1));
        Button rotR = new Button(this);
        rotR.setText("\u21BB");
        UiKit.style(this, rotR);
        rotR.setOnClickListener(v -> rotate(1));

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams gap = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        gap.leftMargin = 24;
        gap.rightMargin = 24;
        bar.addView(rotL);
        bar.addView(set, gap);
        bar.addView(rotR);
        FrameLayout.LayoutParams barParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        barParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        barParams.bottomMargin = 64;
        root.addView(bar, barParams);

        setContentView(root);
    }

    private void rotate(int delta) {
        turns = Math.floorMod(turns + delta, 4);
        preview.setQuarterTurns(turns);
        previewView.invalidate();
    }

    private float clampZoom(float zoom) {
        return Math.max(MIN_ZOOM, Math.min(zoom, MAX_ZOOM));
    }
}

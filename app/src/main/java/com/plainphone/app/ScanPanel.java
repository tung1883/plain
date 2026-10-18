package com.plainphone.app;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.GradientDrawable;
import android.hardware.Camera;
import android.view.Gravity;
import android.view.Surface;
import android.view.TextureView;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.List;

/**
 * Read text from a photo: choose one, or take one right here with the camera, review what was
 * found, then insert it.
 */
@SuppressWarnings("deprecation")   // android.hardware.Camera: small, and needs no extra library
final class ScanPanel extends LinearLayout {

    private static final int MAX_SIDE = 2048;

    private final KeyboardRoot.Host host;
    private final LinearLayout drop, acts, camActs;
    private final FrameLayout camBox;
    private final TextureView preview;
    private final TextView hint, result, choose, camera, insert;
    private String text = "";

    private Camera cam;
    private int jpegRotation;
    private boolean capturing;

    ScanPanel(Context ctx, KeyboardRoot.Host host) {
        super(ctx);
        this.host = host;
        setOrientation(VERTICAL);

        LayoutParams captionLp = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        captionLp.leftMargin = PanelKit.dp(ctx, 4);
        captionLp.bottomMargin = PanelKit.dp(ctx, 4);
        addView(PanelKit.caption(ctx, "TEXT FROM A PHOTO"), captionLp);

        // empty: a dashed drop zone with the scan icon
        drop = new LinearLayout(ctx);
        drop.setOrientation(VERTICAL);
        drop.setGravity(Gravity.CENTER);
        GradientDrawable dashed = new GradientDrawable();
        dashed.setColor(0x00000000);
        dashed.setCornerRadius(PanelKit.dp(ctx, 12));
        dashed.setStroke(PanelKit.dp(ctx, 1), 0xFF333333, PanelKit.dp(ctx, 5), PanelKit.dp(ctx, 4));
        drop.setBackground(dashed);
        PanelKit.IconView icon = new PanelKit.IconView(ctx, KeyIcons.SCAN);
        drop.addView(icon, new LayoutParams(PanelKit.dp(ctx, 64), PanelKit.dp(ctx, 44)));
        hint = new TextView(ctx);
        hint.setTextSize(12);
        hint.setGravity(Gravity.CENTER);
        hint.setTextColor(PanelKit.DIM);
        hint.setTypeface(Fonts.cascadiaMono(ctx));
        drop.addView(hint);
        addView(drop, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        // found: the text in a card
        result = new TextView(ctx);
        result.setTextSize(13);
        result.setTextColor(0xFFFFFFFF);
        result.setTypeface(Fonts.cascadiaMono(ctx));
        result.setPadding(PanelKit.dp(ctx, 12), PanelKit.dp(ctx, 10), PanelKit.dp(ctx, 12), PanelKit.dp(ctx, 10));
        result.setBackground(PanelKit.card(ctx));
        result.setMovementMethod(new android.text.method.ScrollingMovementMethod());
        addView(result, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        // camera: a live preview in place of the drop zone / result
        camBox = new FrameLayout(ctx);
        camBox.setBackground(UiKit.rounded(ctx, 0xFF000000, PanelKit.LINE, 1f, 12));
        UiKit.clipRounded(ctx, camBox, 12);
        preview = new TextureView(ctx);
        camBox.addView(preview, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER));
        camBox.setVisibility(GONE);
        addView(camBox, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        preview.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture st, int w, int h) {
                openCamera(st);
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture st, int w, int h) {
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture st) {
                closeCamera();
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture st) {
            }
        });

        acts = new LinearLayout(ctx);
        choose = button(ctx, "Photo", true, v -> host.scanText());
        camera = button(ctx, "Camera", false, v -> startCamera());
        insert = button(ctx, "Insert", false, v -> {
            if (text.isEmpty()) return;
            host.insertText(text);
            setResult("");
            host.closePanel();
        });
        acts.addView(choose);
        acts.addView(camera);
        acts.addView(insert);
        LayoutParams actsLp = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        actsLp.topMargin = PanelKit.dp(ctx, 6);
        addView(acts, actsLp);

        camActs = new LinearLayout(ctx);
        camActs.addView(button(ctx, "Cancel", false, v -> stopCamera()));
        camActs.addView(button(ctx, "Capture", true, v -> capture()));
        camActs.setVisibility(GONE);
        addView(camActs, actsLp);

        setResult("");
    }

    private TextView button(Context ctx, String label, boolean on, OnClickListener l) {
        TextView t = PanelKit.chip(ctx, label, on, l);
        t.setTextSize(13);
        LayoutParams lp = new LayoutParams(0, PanelKit.dp(ctx, 40), 1f);
        lp.rightMargin = PanelKit.dp(ctx, 4);
        t.setLayoutParams(lp);
        return t;
    }

    /** Show the text found in a photo; empty text shows the drop zone again. */
    void setResult(String found) {
        text = found == null ? "" : found;
        boolean has = !text.isEmpty();
        hint.setText("Pick or take a photo and the text\nin it shows up here to review.");
        drop.setVisibility(has ? GONE : VISIBLE);
        result.setVisibility(has ? VISIBLE : GONE);
        result.setText(text);
        PanelKit.styleChip(getContext(), choose, !has);
        PanelKit.styleChip(getContext(), insert, has);
    }

    // ---------------------------------------------------------------- camera

    private void startCamera() {
        Context ctx = getContext();
        if (ctx.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            host.requestCameraPermission();   // then tap Camera again
            return;
        }
        drop.setVisibility(GONE);
        result.setVisibility(GONE);
        acts.setVisibility(GONE);
        camBox.setVisibility(VISIBLE);
        camActs.setVisibility(VISIBLE);
        if (preview.isAvailable()) openCamera(preview.getSurfaceTexture());
    }

    /** Close the camera and go back to the buttons. */
    private void stopCamera() {
        closeCamera();
        camBox.setVisibility(GONE);
        camActs.setVisibility(GONE);
        acts.setVisibility(VISIBLE);
        setResult(text);
    }

    /** Called when the keyboard leaves this panel. */
    void release() {
        capturing = false;
        if (cam != null || camBox.getVisibility() == VISIBLE) stopCamera();
    }

    private void closeCamera() {
        if (cam == null) return;
        try {
            cam.stopPreview();
        } catch (RuntimeException ignored) {
        }
        cam.release();
        cam = null;
    }

    private void openCamera(SurfaceTexture st) {
        if (cam != null) return;
        Context ctx = getContext();
        try {
            Camera.CameraInfo info = new Camera.CameraInfo();
            int id = 0;
            for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
                Camera.getCameraInfo(i, info);
                if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    id = i;
                    break;
                }
            }
            Camera.getCameraInfo(id, info);
            cam = Camera.open(id);

            int degrees = 0;
            switch (((WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE))
                    .getDefaultDisplay().getRotation()) {
                case Surface.ROTATION_90: degrees = 90; break;
                case Surface.ROTATION_180: degrees = 180; break;
                case Surface.ROTATION_270: degrees = 270; break;
                default:
            }
            int display = (info.orientation - degrees + 360) % 360;
            jpegRotation = (info.orientation + degrees) % 360;
            cam.setDisplayOrientation(display);

            Camera.Parameters p = cam.getParameters();
            Camera.Size prev = bestSize(p.getSupportedPreviewSizes(), 1280 * 960, 4f / 3f);
            Camera.Size pic = bestSize(p.getSupportedPictureSizes(), 12_000_000, (float) prev.width / prev.height);
            p.setPreviewSize(prev.width, prev.height);
            p.setPictureSize(pic.width, pic.height);
            if (p.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                p.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            }
            cam.setParameters(p);
            fitPreview(display % 180 == 90 ? prev.height : prev.width, display % 180 == 90 ? prev.width : prev.height);
            cam.setPreviewTexture(st);
            cam.startPreview();
        } catch (java.io.IOException | RuntimeException e) {
            Toast.makeText(ctx, "Camera not available", Toast.LENGTH_SHORT).show();
            stopCamera();
        }
    }

    /** The largest size with about the wanted shape, not above {@code maxPixels}. */
    private static Camera.Size bestSize(List<Camera.Size> sizes, int maxPixels, float ratio) {
        Camera.Size best = null;
        for (Camera.Size s : sizes) {
            if ((long) s.width * s.height > maxPixels) continue;
            if (Math.abs((float) s.width / s.height - ratio) > 0.03f) continue;
            if (best == null || s.width * s.height > best.width * best.height) best = s;
        }
        if (best != null) return best;
        for (Camera.Size s : sizes) {   // none with that shape: take the biggest allowed
            if ((long) s.width * s.height > maxPixels) continue;
            if (best == null || s.width * s.height > best.width * best.height) best = s;
        }
        return best != null ? best : sizes.get(0);
    }

    /** Size the preview view to the picture's shape, centered in the box, so it is never stretched. */
    private void fitPreview(int shownW, int shownH) {
        int boxW = camBox.getWidth(), boxH = camBox.getHeight();
        if (boxW == 0 || boxH == 0 || shownW == 0 || shownH == 0) return;
        float scale = Math.min((float) boxW / shownW, (float) boxH / shownH);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                Math.round(shownW * scale), Math.round(shownH * scale), Gravity.CENTER);
        preview.setLayoutParams(lp);
    }

    private void capture() {
        if (cam == null || capturing) return;
        capturing = true;
        try {
            cam.takePicture(null, null, (data, c) -> {
                capturing = false;
                read(data);
            });
        } catch (RuntimeException e) {
            capturing = false;
            Toast.makeText(getContext(), "Could not take the photo", Toast.LENGTH_SHORT).show();
        }
    }

    /** Turn the JPEG upright, close the camera and read the text in it. */
    private void read(byte[] jpeg) {
        int rotation = jpegRotation;
        stopCamera();
        hint.setText("Reading...");
        Bitmap bmp;
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length, bounds);
            int sample = 1;
            while (Math.max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2;
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inSampleSize = sample;
            bmp = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length, o);
        } catch (RuntimeException | OutOfMemoryError e) {
            bmp = null;
        }
        if (bmp == null) {
            setResult("");
            Toast.makeText(getContext(), "Could not read the photo", Toast.LENGTH_SHORT).show();
            return;
        }
        if (rotation != 0) {
            Matrix m = new Matrix();
            m.postRotate(rotation);
            bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), m, true);
        }
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                .process(InputImage.fromBitmap(bmp, 0))
                .addOnSuccessListener(found -> {
                    String out = found.getText().trim();
                    if (out.isEmpty()) {
                        setResult("");
                        Toast.makeText(getContext(), "No text found", Toast.LENGTH_SHORT).show();
                    } else {
                        setResult(out);
                    }
                })
                .addOnFailureListener(e -> {
                    setResult("");
                    Toast.makeText(getContext(), "Could not read the photo", Toast.LENGTH_SHORT).show();
                });
    }
}

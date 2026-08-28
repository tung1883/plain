package com.plainphone.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

public class PixelSceneActivity extends Activity {

    private static final int REQUEST_PICK_PHOTO = 9001;

    private final List<LinearLayout> rows = new ArrayList<>();
    private final List<TextView> labels = new ArrayList<>();
    private final List<BooleanSupplier> selectedChecks = new ArrayList<>();

    private LinearLayout list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setBackgroundColor(Color.BLACK);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(Color.BLACK);
        scrollView.addView(list, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(scrollView);
    }

    @Override
    protected void onResume() {
        super.onResume();

        render();
    }

    private void render() {
        list.removeAllViews();
        rows.clear();
        labels.clear();
        selectedChecks.clear();

        Typeface georgia = Fonts.current(this);

        addRow(list, georgia, "Off", null,
                () -> !Config.isPixelArtEnabled(this),
                v -> Config.setPixelArtEnabled(this, false));

        String photoUriString = Config.getArtPhotoUri(this);
        if (photoUriString != null) {
            Uri photoUri = Uri.parse(photoUriString);
            PhotoArtView photoPreview = new PhotoArtView(this, photoUri,
                    Config.getArtPhotoFocusX(this), Config.getArtPhotoFocusY(this),
                    Config.getArtPhotoZoom(this));
            LinearLayout photoRow = addRow(list, georgia, "My photo", photoPreview,
                    () -> Config.isPixelArtEnabled(this) && Config.isPhotoArtSelected(this),
                    v -> {
                        Config.setPhotoArtSelected(this, true);
                        Config.setPixelArtEnabled(this, true);
                    });

            photoRow.setOnLongClickListener(v -> {
                showPhotoOptions(photoUri);
                return true;
            });
        } else {
            addRow(list, georgia, "Choose a photo…", null, () -> false, v -> pickPhoto());
        }

        for (GifScene gif : GifScene.values()) {
            addRow(list, georgia, gif.label, new GifArtView(this, gif),
                    () -> Config.isPixelArtEnabled(this) && !Config.isPhotoArtSelected(this)
                            && Config.getGifScene(this) == gif,
                    v -> {
                        Config.setGifScene(this, gif);
                        Config.setPhotoArtSelected(this, false);
                        Config.setPixelArtEnabled(this, true);
                    });
        }

        refreshSelection();
    }

    private void pickPhoto() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_PICK_PHOTO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_PICK_PHOTO || resultCode != RESULT_OK
                || data == null || data.getData() == null) {
            return;
        }

        Uri uri = data.getData();

        getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);

        Intent crop = new Intent(this, PhotoCropActivity.class);
        crop.putExtra("uri", uri.toString());
        startActivity(crop);
    }

    private void showPhotoOptions(Uri currentUri) {
        Typeface georgia = Fonts.current(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(popupBackground());
        root.setPadding(0, 8, 0, 8);

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setView(root)
                .create();
        UiKit.clearDialogChrome(dialog);

        root.addView(optionRow(georgia, "Adjust crop", v -> {
            dialog.dismiss();
            Intent crop = new Intent(this, PhotoCropActivity.class);
            crop.putExtra("uri", currentUri.toString());
            startActivity(crop);
        }));

        root.addView(optionRow(georgia, "Change photo", v -> {
            dialog.dismiss();
            pickPhoto();
        }));

        dialog.show();
        if (dialog.getWindow() != null) {
            android.view.WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
            params.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.85);
            dialog.getWindow().setAttributes(params);
        }
    }

    private TextView optionRow(Typeface georgia, String label, View.OnClickListener listener) {
        TextView row = new TextView(this);
        row.setText(label);
        row.setTextColor(Color.WHITE);
        row.setTextSize(20);
        row.setTypeface(georgia);
        row.setPadding(48, 32, 48, 32);
        row.setGravity(Gravity.START);
        row.setBackground(rowBackground());
        row.setOnClickListener(listener);
        return row;
    }

    private Drawable popupBackground() {
        android.graphics.drawable.GradientDrawable box = new android.graphics.drawable.GradientDrawable();
        box.setColor(Color.BLACK);
        return box;
    }

    private LinearLayout addRow(LinearLayout list, Typeface georgia, String labelText, View preview,
                                 BooleanSupplier isSelected, View.OnClickListener onSelect) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(48, 24, 48, 24);

        if (preview != null) {
            LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(80, 100);
            previewParams.topMargin = 16;
            previewParams.bottomMargin = 16;
            previewParams.rightMargin = 16;
            row.addView(preview, previewParams);
        }

        TextView label = new TextView(this);
        label.setText(labelText);
        label.setTextSize(18);
        label.setTypeface(georgia);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        if (preview != null) {
            labelParams.leftMargin = 32;
        }
        row.addView(label, labelParams);

        row.setOnClickListener(v -> {
            onSelect.onClick(v);
            refreshSelection();
        });

        list.addView(row, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        rows.add(row);
        labels.add(label);
        selectedChecks.add(isSelected);
        return row;
    }

    private Drawable rowBackground() {
        StateListDrawable drawable = new StateListDrawable();
        drawable.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Color.DKGRAY));
        drawable.addState(new int[]{}, new ColorDrawable(Color.BLACK));
        return drawable;
    }

    private void refreshSelection() {
        for (int i = 0; i < rows.size(); i++) {
            boolean selected = selectedChecks.get(i).getAsBoolean();
            rows.get(i).setBackgroundColor(selected ? Color.WHITE : Color.BLACK);
            labels.get(i).setTextColor(selected ? Color.BLACK : Color.WHITE);
        }
    }
}


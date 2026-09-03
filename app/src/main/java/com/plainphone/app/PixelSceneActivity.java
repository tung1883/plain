package com.plainphone.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Home-screen art picker. Check any mix of gallery items and scenes — one checked
 * = static, two or more = a slideshow. Tap the row (or the <code>[ ]</code> box)
 * to add / remove from the rotation; tap the thumbnail to crop; long-press for
 * options.
 */
public class PixelSceneActivity extends Activity {

    private static final int REQ_PICK = 9001;

    private static final class Row {
        final String key;   // null for the plain rows (Off / Slideshow / Add)
        final LinearLayout view;
        final TextView marker;
        final TextView label;
        final View preview;
        Row(String key, LinearLayout view, TextView marker, TextView label, View preview) {
            this.key = key; this.view = view; this.marker = marker;
            this.label = label; this.preview = preview;
        }
    }

    private final List<Row> allRows = new ArrayList<>();
    private final Map<String, View> previewByKey = new HashMap<>();
    private String focusedKey;

    private LinearLayout list;
    private Typeface font;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Config.migrateArt(this);

        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setBackgroundColor(Color.BLACK);

        ScrollView scroller = new ScrollView(this);
        scroller.setBackgroundColor(Color.BLACK);
        scroller.addView(list, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        UiKit.screen(this, "Home screen art", scroller);
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    private void render() {
        font = Fonts.current(this);
        list.removeAllViews();
        allRows.clear();
        previewByKey.clear();
        focusedKey = ArtGallery.currentId(this);

        plainRow("Off", " ", v -> { Config.setArtMode(this, "off"); refresh(); });
        plainRow("Slideshow: " + slideshowLabel(), " ",
                v -> startActivity(new Intent(this, ArtSlideshowActivity.class)));

        list.addView(sectionHeader("My gallery"));
        for (ArtItem it : ArtGallery.all(this)) {
            final String key = it.id;
            View preview = it.isGif()
                    ? new GifArtView(this, it.file(this), it.fx, it.fy, it.zoom, it.gray)
                    : new PhotoArtView(this, Uri.fromFile(it.file(this)), it.fx, it.fy, it.zoom, it.gray);
            checkableRow(key, it.name != null ? it.name : "Image", preview,
                    v -> openCrop("item:" + key),
                    () -> showItemOptions(key));
        }
        plainRow("Add a photo", " + ", v -> pickImage());

        list.addView(sectionHeader("Scenes"));
        for (GifScene s : GifScene.values()) {
            final String key = "scene:" + s.name();
            float[] c = Config.getSceneCrop(this, s);
            GifArtView preview = new GifArtView(this, s, c[0], c[1], c[2], c[3] != 0f);
            checkableRow(key, s.label, preview,
                    v -> openCrop(key),
                    () -> showSceneOptions(s));
        }

        refresh();
    }

    private String slideshowLabel() {
        return "every " + Config.getArtSlideshowMinutes(this) + " min";
    }

    // --- selection ---------------------------------------

    private boolean selected(String key) {
        return ArtGallery.isSelected(this, key);
    }

    private void toggle(String key) {
        ArtGallery.toggle(this, key);
        Config.setArtMode(this, ArtGallery.selectedIds(this).isEmpty() ? "off" : "on");
        focusedKey = key;
        refresh();
    }

    private void refresh() {
        boolean off = "off".equals(Config.getArtMode(this));
        for (Row r : allRows) {
            boolean sel;
            if (r.key == null) {
                sel = "Off".contentEquals(r.label.getText()) && off;
            } else {
                sel = !off && selected(r.key);
            }
            r.view.setBackgroundColor(sel ? Color.WHITE : Color.BLACK);
            r.label.setTextColor(sel ? Color.BLACK : Color.WHITE);
            r.marker.setTextColor(sel ? Color.BLACK : Color.WHITE);
            String m = r.marker.getText().toString();
            if (m.equals("[ ]") || m.equals("[x]")) r.marker.setText(sel ? "[x]" : "[ ]");
        }
        for (Map.Entry<String, View> e : previewByKey.entrySet()) {
            if (e.getValue() instanceof GifArtView) {
                ((GifArtView) e.getValue()).setStaticPreview(!e.getKey().equals(focusedKey));
            }
        }
    }

    // --- pick / crop ------------------------------------

    private void pickImage() {
        startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE).setType("image/*"), REQ_PICK);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_PICK || resultCode != RESULT_OK || data == null
                || data.getData() == null) {
            return;
        }
        Uri uri = data.getData();
        boolean isGif = "image/gif".equals(getContentResolver().getType(uri))
                || uri.toString().toLowerCase().endsWith(".gif");
        ArtItem it = ArtGallery.addFromUri(this, uri, isGif);
        if (it == null) {
            Toast.makeText(this, "Couldn't load that image", Toast.LENGTH_SHORT).show();
            return;
        }
        ArtGallery.toggle(this, it.id);
        Config.setArtMode(this, "on");
        openCrop("item:" + it.id);
    }

    private void openCrop(String target) {
        startActivity(new Intent(this, PhotoCropActivity.class).putExtra("target", target));
    }

    // --- options dialogs -------------------------------

    private void showItemOptions(String id) {
        ArtItem it = ArtGallery.get(this, id);
        if (it == null) return;
        List<String[]> opts = new ArrayList<>();
        opts.add(new String[]{"Crop", "crop"});
        opts.add(new String[]{"Grayscale: " + (it.gray ? "On" : "Off"), "gray"});
        opts.add(new String[]{"Delete", "delete"});
        optionsDialog(it.name != null ? it.name : "Image", opts, choice -> {
            switch (choice) {
                case "crop":
                    openCrop("item:" + id);
                    break;
                case "gray":
                    ArtGallery.setGray(this, id, !it.gray);
                    applyGray(id, !it.gray);
                    break;
                case "delete":
                    VaultUi.confirm(this,
                            "Delete " + (it.name != null ? it.name : "this image") + "?", null,
                            "Delete", () -> { ArtGallery.delete(this, id); render(); }, "Cancel", null);
                    break;
            }
        });
    }

    private void showSceneOptions(GifScene s) {
        String key = "scene:" + s.name();
        boolean gray = Config.getSceneCrop(this, s)[3] != 0f;
        List<String[]> opts = new ArrayList<>();
        opts.add(new String[]{"Crop", "crop"});
        opts.add(new String[]{"Grayscale: " + (gray ? "On" : "Off"), "gray"});
        optionsDialog(s.label, opts, choice -> {
            if ("crop".equals(choice)) {
                openCrop(key);
            } else if ("gray".equals(choice)) {
                Config.setSceneGray(this, s, !gray);
                applyGray(key, !gray);
            }
        });
    }

    /** Re-tint just the one preview — no list rebuild, no re-decode. */
    private void applyGray(String key, boolean gray) {
        View p = previewByKey.get(key);
        if (p instanceof GifArtView) ((GifArtView) p).setGrayscale(gray);
        else if (p instanceof PhotoArtView) ((PhotoArtView) p).setGrayscale(gray);
    }

    private interface OptionPick { void picked(String key); }

    private void optionsDialog(String title, List<String[]> opts, OptionPick onPick) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.BLACK);
        root.setBackground(bg);
        root.setPadding(0, 8, 0, 8);

        TextView t = new TextView(this);
        t.setText(title.toUpperCase());
        t.setTextColor(Color.GRAY);
        t.setTextSize(12);
        t.setLetterSpacing(0.1f);
        t.setTypeface(font);
        t.setPadding(48, 24, 48, 12);
        root.addView(t);

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setView(root).create();
        UiKit.clearDialogChrome(dialog);

        for (String[] o : opts) {
            TextView row = new TextView(this);
            row.setText(o[0]);
            row.setTextColor(Color.WHITE);
            row.setTextSize(19);
            row.setTypeface(font);
            row.setPadding(48, 26, 48, 26);
            row.setBackground(rowBackground());
            row.setOnClickListener(v -> { dialog.dismiss(); onPick.picked(o[1]); });
            root.addView(row);
        }

        dialog.show();
        if (dialog.getWindow() != null) {
            android.view.WindowManager.LayoutParams p = dialog.getWindow().getAttributes();
            p.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.85);
            dialog.getWindow().setAttributes(p);
        }
    }

    // --- rows ------------------------------------------

    private interface Longable { void run(); }

    private void plainRow(String labelText, String marker, View.OnClickListener onTap) {
        LinearLayout row = newRow();
        TextView mk = markerView(marker);
        if (marker.trim().isEmpty()) {
            // Off / Slideshow: no marker box — the label sits flush with the ← arrow.
            mk.setVisibility(View.GONE);
            row.addView(mk, new LinearLayout.LayoutParams(0, 0));
        } else {
            row.addView(mk, markerParams());
        }
        TextView label = labelView(labelText);
        row.addView(label, labelParams());
        row.setOnClickListener(onTap);
        list.addView(row, rowParams());
        allRows.add(new Row(null, row, mk, label, null));
    }

    private void checkableRow(String key, String labelText, View preview,
                              View.OnClickListener onEdit, Longable onLong) {
        LinearLayout row = newRow();

        TextView mk = markerView("[ ]");
        mk.setOnClickListener(v -> toggle(key));
        row.addView(mk, markerParams());

        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(80, 100);
        pp.rightMargin = 24;
        preview.setOnClickListener(onEdit);
        row.addView(preview, pp);
        previewByKey.put(key, preview);

        TextView label = labelView(labelText);
        row.addView(label, labelParams());

        // Tap anywhere on the row (bar the thumbnail) to add / remove from the rotation.
        row.setOnClickListener(v -> toggle(key));
        row.setOnLongClickListener(v -> { onLong.run(); return true; });

        list.addView(row, rowParams());
        allRows.add(new Row(key, row, mk, label, preview));
    }

    private LinearLayout newRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(48, 22, 48, 22);
        row.setBackground(rowBackground());
        return row;
    }

    private TextView markerView(String text) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextColor(Color.WHITE);
        v.setTextSize(14);
        v.setTypeface(font);
        v.setSingleLine(true);
        v.setGravity(Gravity.CENTER);
        v.setMinEms(2);   // keep [ ] / + / (blank) the same width so labels line up
        v.setPadding(28, 26, 28, 26);   // fat hit target for the checkbox
        return v;
    }

    private TextView labelView(String text) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextSize(18);
        v.setTextColor(Color.WHITE);
        v.setTypeface(font);
        v.setSingleLine(true);
        v.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
        return v;
    }

    private LinearLayout.LayoutParams markerParams() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.leftMargin = -20;   // padding grew the view; pull it back toward the edge
        p.rightMargin = 4;
        return p;
    }

    private LinearLayout.LayoutParams labelParams() {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
    }

    private LinearLayout.LayoutParams rowParams() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private TextView sectionHeader(String text) {
        TextView h = new TextView(this);
        h.setText(text.toUpperCase());
        h.setTextColor(Color.GRAY);
        h.setTextSize(12);
        h.setLetterSpacing(0.15f);
        h.setTypeface(font);
        h.setPadding(48, 36, 48, 10);
        return h;
    }

    private StateListDrawable rowBackground() {
        StateListDrawable d = new StateListDrawable();
        d.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Color.DKGRAY));
        d.addState(new int[]{}, new ColorDrawable(Color.BLACK));
        return d;
    }
}

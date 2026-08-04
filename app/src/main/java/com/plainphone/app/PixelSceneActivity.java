package com.plainphone.app;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
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

/** Lets the user preview and pick which pixel-art scene shows on the home screen, or turn it off. */
public class PixelSceneActivity extends Activity {

    private final List<LinearLayout> rows = new ArrayList<>();
    private final List<TextView> labels = new ArrayList<>();
    private final List<BooleanSupplier> selectedChecks = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Typeface georgia = Fonts.current(this);

        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setBackgroundColor(Color.BLACK);

        addRow(list, georgia, "Off", null,
                () -> !Config.isPixelArtEnabled(this),
                v -> Config.setPixelArtEnabled(this, false));

        for (Scene scene : Scene.values()) {
            addRow(list, georgia, scene.label, new PixelArtView(this, scene),
                    () -> Config.isPixelArtEnabled(this) && !Config.isGifSceneSelected(this)
                            && Config.getPixelScene(this) == scene,
                    v -> {
                        Config.setPixelScene(this, scene);
                        Config.setPixelArtEnabled(this, true);
                    });
        }

        for (GifScene gif : GifScene.values()) {
            addRow(list, georgia, gif.label, new GifArtView(this, gif),
                    () -> Config.isPixelArtEnabled(this) && Config.isGifSceneSelected(this)
                            && Config.getGifScene(this) == gif,
                    v -> {
                        Config.setGifScene(this, gif);
                        Config.setPixelArtEnabled(this, true);
                    });
        }

        refreshSelection();

        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(list, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(scrollView);
    }

    private void addRow(LinearLayout list, Typeface georgia, String labelText, View preview,
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
    }

    private void refreshSelection() {
        for (int i = 0; i < rows.size(); i++) {
            boolean selected = selectedChecks.get(i).getAsBoolean();
            rows.get(i).setBackgroundColor(selected ? Color.WHITE : Color.BLACK);
            labels.get(i).setTextColor(selected ? Color.BLACK : Color.WHITE);
        }
    }
}

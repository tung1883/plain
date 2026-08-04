package com.plainphone.app;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** Lets the user preview and pick which pixel-art scene shows on the home screen, or turn it off. */
public class PixelSceneActivity extends Activity {

    private final List<LinearLayout> rows = new ArrayList<>();
    private final List<TextView> labels = new ArrayList<>();
    private LinearLayout offRow;
    private TextView offLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Typeface georgia = Fonts.georgia(this);

        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setBackgroundColor(Color.BLACK);

        offRow = new LinearLayout(this);
        offRow.setOrientation(LinearLayout.HORIZONTAL);
        offRow.setGravity(Gravity.CENTER_VERTICAL);
        offRow.setPadding(48, 24, 48, 24);

        offLabel = new TextView(this);
        offLabel.setText("Off");
        offLabel.setTextSize(18);
        offLabel.setTypeface(georgia);
        offRow.addView(offLabel);

        offRow.setOnClickListener(v -> {
            Config.setPixelArtEnabled(this, false);
            refreshSelection();
        });

        list.addView(offRow, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        for (Scene scene : Scene.values()) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(48, 24, 48, 24);

            PixelArtView preview = new PixelArtView(this, scene);
            LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(80, 100);
            previewParams.topMargin = 16;
            previewParams.bottomMargin = 16;
            previewParams.rightMargin = 16;
            row.addView(preview, previewParams);

            TextView label = new TextView(this);
            label.setText(scene.label);
            label.setTextSize(18);
            label.setTypeface(georgia);
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            labelParams.leftMargin = 32;
            row.addView(label, labelParams);

            row.setOnClickListener(v -> {
                Config.setPixelScene(this, scene);
                Config.setPixelArtEnabled(this, true);
                refreshSelection();
            });

            list.addView(row, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            rows.add(row);
            labels.add(label);
        }

        refreshSelection();

        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(list, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(scrollView);
    }

    private void refreshSelection() {
        boolean enabled = Config.isPixelArtEnabled(this);
        Scene current = Config.getPixelScene(this);

        offRow.setBackgroundColor(!enabled ? Color.WHITE : Color.BLACK);
        offLabel.setTextColor(!enabled ? Color.BLACK : Color.WHITE);

        Scene[] values = Scene.values();
        for (int i = 0; i < values.length; i++) {
            boolean selected = enabled && values[i] == current;
            rows.get(i).setBackgroundColor(selected ? Color.WHITE : Color.BLACK);
            labels.get(i).setTextColor(selected ? Color.BLACK : Color.WHITE);
        }
    }
}

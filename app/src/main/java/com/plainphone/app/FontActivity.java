package com.plainphone.app;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class FontActivity extends Activity {

    private final List<LinearLayout> rows = new ArrayList<>();
    private final List<TextView> labels = new ArrayList<>();
    private final List<TextView> samples = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setBackgroundColor(Color.BLACK);

        for (FontChoice font : FontChoice.values()) {
            addRow(list, font);
        }

        refreshSelection();

        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(list, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(scrollView);
    }

    private void addRow(LinearLayout list, FontChoice font) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(48, 32, 48, 32);

        TextView label = new TextView(this);
        label.setText(font.label);
        label.setTextSize(16);
        label.setTypeface(Fonts.current(this));
        row.addView(label);

        TextView sample = new TextView(this);
        sample.setText("The quick brown fox jumps 0123456789");
        sample.setTextSize(20);
        sample.setTypeface(fontFor(font));
        LinearLayout.LayoutParams sampleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        sampleParams.topMargin = 12;
        row.addView(sample, sampleParams);

        row.setOnClickListener(v -> {
            Config.setFontChoice(this, font);
            refreshSelection();
        });

        list.addView(row, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        rows.add(row);
        labels.add(label);
        samples.add(sample);
    }

    private Typeface fontFor(FontChoice font) {
        return switch (font) {
            case GEORGIA -> Fonts.georgia(this);
            case IBM_PLEX_MONO -> Fonts.ibmPlexMono(this);
        };
    }

    private void refreshSelection() {
        FontChoice current = Config.getFontChoice(this);
        FontChoice[] values = FontChoice.values();
        for (int i = 0; i < values.length; i++) {
            boolean selected = values[i] == current;
            rows.get(i).setBackgroundColor(selected ? Color.WHITE : Color.BLACK);
            labels.get(i).setTextColor(selected ? Color.BLACK : Color.WHITE);
            samples.get(i).setTextColor(selected ? Color.BLACK : Color.WHITE);
        }
    }
}


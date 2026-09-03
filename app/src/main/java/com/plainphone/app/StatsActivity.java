package com.plainphone.app;

import android.app.Activity;
import android.os.Bundle;

import java.util.Calendar;

public class StatsActivity extends Activity {

    enum Range {
        TODAY("Today", 0, "EEE"),
        WEEK("7 days", -6, "EEE"),
        MONTH("1 month", -29, "d/M");

        final String label;
        final int startOffset;
        final String dayLabelPattern;

        Range(String label, int startOffset, String dayLabelPattern) {
            this.label = label;
            this.startOffset = startOffset;
            this.dayLabelPattern = dayLabelPattern;
        }
    }

    private StatsPanel panel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        panel = new StatsPanel(this);

        String requested = getIntent().getStringExtra("range");
        if (requested != null) {
            try {
                panel.setRange(Range.valueOf(requested));
            } catch (IllegalArgumentException ignored) {
            }
        }

        UiKit.screen(this, "Stats", panel.view());
    }

    @Override
    protected void onResume() {
        super.onResume();
        panel.render();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        panel.shutdown();
    }

    static long rangeStartMillis(Range r) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, r.startOffset);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    static String formatDuration(long millis) {
        long totalMinutes = millis / 60000;
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        return hours > 0 ? (hours + "h " + minutes + "m") : (minutes + "m");
    }
}

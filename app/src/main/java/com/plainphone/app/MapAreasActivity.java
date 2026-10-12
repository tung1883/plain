package com.plainphone.app;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** "Map areas" screen: shows every {@link MapArea} in {@link MapAreas#ALL} — all of them are
 *  merged into the one map at once (see {@link MapPanel#buildMergedStyleJson}), not a list to
 *  pick one from. Purely informational — there's no on-device way to add a region (that's a
 *  developer-machine {@code tools/nav} build + a new {@link MapAreas#ALL} entry), so this
 *  screen doesn't pretend otherwise with a fake "+ Add area" action. */
public class MapAreasActivity extends Activity {

    private LinearLayout root;
    private Typeface font;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        font = Fonts.current(this);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);

        ScrollView scroller = new ScrollView(this);
        scroller.setBackgroundColor(Color.BLACK);
        scroller.addView(root, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        UiKit.screen(this, "Map areas", scroller);
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    private void render() {
        root.removeAllViews();

        TextView intro = new TextView(this);
        intro.setText("All merged into one map:");
        intro.setTextColor(0xFFAAAAAA);
        intro.setTextSize(13);
        intro.setTypeface(font);
        intro.setPadding(UiKit.dp(this, 20), UiKit.dp(this, 20), UiKit.dp(this, 20), UiKit.dp(this, 8));
        root.addView(intro);

        for (MapArea area : MapAreas.ALL) {
            root.addView(areaRow(area));
        }
    }

    private View areaRow(MapArea area) {
        TextView row = new TextView(this);
        row.setText(area.displayName);
        row.setTextColor(Color.WHITE);
        row.setTextSize(17);
        row.setTypeface(font);
        row.setPadding(UiKit.dp(this, 20), UiKit.dp(this, 14), UiKit.dp(this, 20), UiKit.dp(this, 14));
        return row;
    }
}

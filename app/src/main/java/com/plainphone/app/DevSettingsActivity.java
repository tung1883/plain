package com.plainphone.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Settings for the Dev plugin: manage hosts, screen frame rate, lock, hide from home. */
public class DevSettingsActivity extends Activity {

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
        UiKit.screen(this, "Dev", scroller);
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    private void render() {
        font = Fonts.current(this);
        root.removeAllViews();

        int count = DevHost.all(this).size();
        root.addView(row("Computers: " + count, v ->
                startActivity(new Intent(this, DevHostsActivity.class))));

        root.addView(row("Screen frame rate: " + Config.getDevScreenFps(this) + " fps", v -> {
            int cur = Config.getDevScreenFps(this);
            int next = cur < 8 ? 12 : cur < 12 ? 15 : cur < 15 ? 20 : 8;
            Config.setDevScreenFps(this, next);
            render();
        }));

        boolean pad = "pad".equals(Config.getDevTrackpadStyle(this));
        root.addView(row("Trackpad style: " + (pad ? "Dedicated pad" : "Whole screen"), v -> {
            Config.setDevTrackpadStyle(this, pad ? "whole" : "pad");
            render();
        }));

        root.addView(row("Locked: " + (Lock.DEV.isLocked(this) ? "On" : "Off"),
                v -> Lock.DEV.toggleLock(this, this::render)));

        root.addView(row("Show on home screen: "
                + (Config.isDevHiddenFromHome(this) ? "Off" : "On"), v -> {
            Config.setDevHiddenFromHome(this, !Config.isDevHiddenFromHome(this));
            render();
        }));
    }

    private View row(String label, View.OnClickListener listener) {
        TextView view = new TextView(this);
        view.setText(label);
        view.setTextColor(Color.WHITE);
        view.setTextSize(18);
        view.setPadding(48, 36, 48, 36);
        view.setTypeface(font);
        StateListDrawable bg = new StateListDrawable();
        bg.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Color.DKGRAY));
        bg.addState(new int[]{}, new ColorDrawable(Color.BLACK));
        view.setBackground(bg);
        view.setOnClickListener(listener);
        return view;
    }
}

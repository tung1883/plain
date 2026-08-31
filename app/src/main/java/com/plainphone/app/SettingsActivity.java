package com.plainphone.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class SettingsActivity extends Activity {

    static final String EXTRA_DESTINATION = "destination";

    private static final List<String> SECTION_ORDER =
            Arrays.asList("Appearance", "Notes", "To-do", "Apps", "Search", "Permissions");

    private LinearLayout root;
    private Typeface georgia;

    private static class Entry {
        final String section;
        final String sortKey;
        final View view;

        Entry(String section, String sortKey, View view) {
            this.section = section;
            this.sortKey = sortKey;
            this.view = view;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        georgia = Fonts.current(this);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);

        ScrollView scroller = new ScrollView(this);
        scroller.setBackgroundColor(Color.BLACK);
        scroller.addView(root, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(scroller);

        if (savedInstanceState == null) {
            openDeepLinkedScreen(getIntent().getStringExtra(EXTRA_DESTINATION));
        }
    }

    private void openDeepLinkedScreen(String className) {
        if (className == null) return;
        try {
            startActivity(new Intent(this, Class.forName(className)));
        } catch (ClassNotFoundException e) {

        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    private void render() {
        georgia = Fonts.current(this);
        root.removeAllViews();

        List<Entry> entries = new ArrayList<>();

        entries.add(new Entry("Appearance", "Change home app", row("Change home app",
                v -> startActivity(new Intent(Settings.ACTION_HOME_SETTINGS)))));

        entries.add(new Entry("Appearance", "Font", row("Font: " + Config.getFontChoice(this).label,
                v -> startActivity(new Intent(this, FontActivity.class)))));

entries.add(new Entry("Appearance", "Home screen art", row("Home screen art",
                v -> startActivity(new Intent(this, PixelSceneActivity.class)))));

        entries.add(new Entry("Notes", "Notes", row("Notes",
                v -> startActivity(new Intent(this, NoteSettingsActivity.class)))));

        entries.add(new Entry("To-do", "To-do", row("To-do",
                v -> startActivity(new Intent(this, TodoSettingsActivity.class)))));

        entries.add(new Entry("Apps", "Lock app list", row(
                "Lock app list: " + (Lock.APPS.isLocked(this) ? "On" : "Off"),
                v -> Lock.APPS.toggleLock(this, this::render))));

        entries.add(new Entry("Apps", "App lock", row("App lock",
                v -> startActivity(new Intent(this, LockedAppsActivity.class)))));

        entries.add(new Entry("Apps", "Flagged apps", row("Flagged apps",
                v -> startActivity(new Intent(this, FlaggedAppsActivity.class)))));

        entries.add(new Entry("Apps", "Hide apps from app list", row("Hide apps from app list",
                v -> startActivity(new Intent(this, HiddenAppsActivity.class)))));

        entries.add(new Entry("Apps", "Settings wait time", row(
                "Settings wait time: " + Config.getSettingsWaitSeconds(this) + "s",
                v -> startActivity(new Intent(this, SettingsWaitTimeActivity.class)))));

        entries.add(new Entry("Apps", "Time Blocks", row("Time Blocks",
                v -> startActivity(new Intent(this, TimeBlocksActivity.class)))));

        entries.add(new Entry("Search", "Search", row("Search",
                v -> startActivity(new Intent(this, SearchSettingsActivity.class)))));

        entries.add(new Entry("Permissions", "App access", row("App access",
                v -> startActivity(new Intent(this, AppAccessActivity.class)))));

        entries.sort(Comparator
                .comparingInt((Entry e) -> SECTION_ORDER.indexOf(e.section))
                .thenComparing(e -> e.sortKey, String.CASE_INSENSITIVE_ORDER));

        String currentSection = null;
        for (Entry entry : entries) {
            if (!entry.section.equals(currentSection)) {
                currentSection = entry.section;
                root.addView(sectionHeader(currentSection));
            }
            root.addView(entry.view);
        }
    }

private TextView sectionHeader(String label) {
        TextView header = new TextView(this);
        header.setText(label.toUpperCase());
        header.setTextColor(Color.GRAY);
        header.setTextSize(13);
        header.setLetterSpacing(0.15f);
        header.setTypeface(georgia);
        header.setPadding(48, 36, 48, 12);
        return header;
    }

    private TextView row(String label, View.OnClickListener listener) {
        TextView view = new TextView(this);
        view.setText(label);
        view.setTextColor(Color.WHITE);
        view.setTextSize(20);
        view.setPadding(48, 40, 48, 40);
        view.setGravity(Gravity.START);
        view.setTypeface(georgia);

        StateListDrawable background = new StateListDrawable();
        background.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Color.DKGRAY));
        background.addState(new int[]{}, new ColorDrawable(Color.BLACK));
        view.setBackground(background);

        view.setOnClickListener(listener);
        return view;
    }
}


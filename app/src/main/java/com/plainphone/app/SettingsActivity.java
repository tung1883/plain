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

    /**
     * Class name of a settings screen to open immediately, used when home-screen search
     * deep-links to one. It rides through the friction (and PIN) gates rather than being
     * launched directly, so a searched-for screen still costs the same wait as reaching
     * it by hand — Settings is left underneath so Back lands somewhere sensible.
     */
    static final String EXTRA_DESTINATION = "destination";

    /** Fixed top-to-bottom order for sections — a reading order, not alphabetical. */
    private static final List<String> SECTION_ORDER =
            Arrays.asList("Appearance", "Restrictions", "Search", "Permissions");

    private LinearLayout root;
    private Typeface georgia;

    /** A row plus the section and label it sorts by — the row's own text carries live
     *  state ("On"/"Off", a value) that shouldn't affect where it lands in the list. */
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

        // The list has outgrown the screen, so it has to scroll — without this the rows
        // past the bottom edge are simply unreachable rather than merely off-screen.
        ScrollView scroller = new ScrollView(this);
        scroller.setBackgroundColor(Color.BLACK);
        scroller.addView(root, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(scroller);

        // Only on a fresh create: after a rotation or a return from the deep-linked screen
        // this Activity is reused, and forwarding again would trap the user in a loop.
        if (savedInstanceState == null) {
            openDeepLinkedScreen(getIntent().getStringExtra(EXTRA_DESTINATION));
        }
    }

    private void openDeepLinkedScreen(String className) {
        if (className == null) return;
        try {
            startActivity(new Intent(this, Class.forName(className)));
        } catch (ClassNotFoundException e) {
            // The catalog in SearchTargets named a screen that no longer exists; showing
            // plain Settings is a fine landing spot, so there's nothing to report.
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        render(); // reflect any changes made via dialogs or returning from system Settings
    }

    private void render() {
        root.removeAllViews();

        List<Entry> entries = new ArrayList<>();

        entries.add(new Entry("Appearance", "Change home app", row("Change home app",
                v -> startActivity(new Intent(Settings.ACTION_HOME_SETTINGS)))));

        entries.add(new Entry("Appearance", "Font", row("Font: " + Config.getFontChoice(this).label,
                v -> startActivity(new Intent(this, FontActivity.class)))));

entries.add(new Entry("Appearance", "Home screen art", row("Home screen art",
                v -> startActivity(new Intent(this, PixelSceneActivity.class)))));

        entries.add(new Entry("Restrictions", "App lock", row("App lock",
                v -> startActivity(new Intent(this, LockedAppsActivity.class)))));

        entries.add(new Entry("Restrictions", "Flagged apps", row("Flagged apps",
                v -> startActivity(new Intent(this, FlaggedAppsActivity.class)))));

        entries.add(new Entry("Restrictions", "Hide apps from app list", row("Hide apps from app list",
                v -> startActivity(new Intent(this, HiddenAppsActivity.class)))));

        entries.add(new Entry("Restrictions", "Settings wait time", row(
                "Settings wait time: " + Config.getSettingsWaitSeconds(this) + "s",
                v -> startActivity(new Intent(this, SettingsWaitTimeActivity.class)))));

        entries.add(new Entry("Restrictions", "Time Blocks", row("Time Blocks",
                v -> startActivity(new Intent(this, TimeBlocksActivity.class)))));

        // Everything that shapes home-screen search — files, folders, contacts, the web,
        // and the user's own web searches — lives together on its own screen now; five
        // separate toggles interleaved among unrelated settings made none of them easy
        // to find, including from within Settings itself.
        entries.add(new Entry("Search", "Search", row("Search",
                v -> startActivity(new Intent(this, SearchSettingsActivity.class)))));

        // Replaces the old standalone "Turn off Accessibility Service" shortcut — that
        // screen is still one tap away from here, but now alongside every other permission
        // Plain holds, with its actual current state, instead of a bare link.
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

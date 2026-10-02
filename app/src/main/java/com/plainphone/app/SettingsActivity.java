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
            Arrays.asList("Appearance", "Apps", "Plugins", "Nerd extras", "Permissions");

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
        UiKit.screen(this, "Settings", scroller);

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

        entries.add(new Entry("Appearance", "Tips, quotes and warnings", row("Tips, quotes and warnings",
                v -> startActivity(new Intent(this, TipsSettingsActivity.class)))));

        entries.add(new Entry("Plugins", "1 Notes", row("Notes",
                v -> startActivity(new Intent(this, NoteSettingsActivity.class)))));

        entries.add(new Entry("Plugins", "2 To-do", row("To-do",
                v -> startActivity(new Intent(this, TodoSettingsActivity.class)))));

        entries.add(new Entry("Plugins", "3 Voice recorder", row("Voice recorder",
                v -> startActivity(new Intent(this, RecorderSettingsActivity.class)))));

        entries.add(new Entry("Plugins", "4 Vault", row("Vault",
                v -> startActivity(new Intent(this, VaultSettingsActivity.class)))));

        entries.add(new Entry("Plugins", "5 Locker", row("Locker",
                v -> startActivity(new Intent(this, LockSettingsActivity.class)))));

        entries.add(new Entry("Plugins", "6 Search", row("Search",
                v -> startActivity(new Intent(this, SearchSettingsActivity.class)))));

        entries.add(new Entry("Plugins", "7 Dev", row("Dev",
                v -> startActivity(new Intent(this, DevSettingsActivity.class)))));

        entries.add(new Entry("Apps", "Flagged apps", row("Flagged apps",
                v -> startActivity(new Intent(this, FlaggedAppsActivity.class)))));

        entries.add(new Entry("Apps", "Hide apps from app list", row("Hide apps from app list",
                v -> startActivity(new Intent(this, HiddenAppsActivity.class)))));

        entries.add(new Entry("Apps", "Time blocks", row("Time blocks",
                v -> startActivity(new Intent(this, TimeBlocksActivity.class)))));

        if (Config.isNerdMode(this)) addNerdExtras(entries);

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

    /** The extras that need WRITE_SECURE_SETTINGS; only offered in Nerd mode. */
    private void addNerdExtras(List<Entry> entries) {
        boolean granted = SecureSettings.canWrite(this);

        entries.add(new Entry("Nerd extras", "1 Accessibility service",
                row("Accessibility service: " + (AppMonitorService.isEnabled(this) ? "On" : "Off"),
                        v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)))));

        entries.add(new Entry("Nerd extras", "2 Permission",
                row("Permission: " + (granted ? "Granted" : "Not granted"),
                        granted ? null : v -> startActivity(new Intent(this, HowToActivity.class)))));

        entries.add(new Entry("Nerd extras", "3 Blocking apps",
                row("Blocking apps: " + Config.getBankingPackages(this).size(),
                        v -> openPicker(granted, AppPickerActivity.KIND_BANKING))));

        String monochrome = Config.monochromeLabel(this);
        entries.add(new Entry("Nerd extras", "4 Monochrome",
                row("Monochrome: " + monochrome,
                        v -> openPicker(granted, AppPickerActivity.KIND_MONOCHROME))));
    }

    private void openPicker(boolean granted, String kind) {
        if (!granted) {
            android.widget.Toast.makeText(this, "Grant the permission first",
                    android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        startActivity(new Intent(this, AppPickerActivity.class)
                .putExtra(AppPickerActivity.EXTRA_KIND, kind));
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
        background.addState(new int[]{android.R.attr.state_pressed}, UiKit.pressedFill());
        background.setEnterFadeDuration(120);
        background.setExitFadeDuration(120);
        background.addState(new int[]{}, new ColorDrawable(Color.BLACK));
        view.setBackground(background);

        view.setOnClickListener(listener);
        return view;
    }
}


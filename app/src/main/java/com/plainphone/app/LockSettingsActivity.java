package com.plainphone.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * The hub for every launcher lock. A master <b>Lock</b> switch on top: turning it
 * on requires a master PIN; turning it off wipes every lock setting. Below it, the
 * master PIN, the global re-lock delay, and one row per lock (opening
 * {@link LockDetailActivity}). The Vault is not here — it has its own password.
 */
public class LockSettingsActivity extends Activity {

    private static final int AMBER = 0xFFF0D98A;
    private static final int DIM = 0xFF6A6A6A;
    private static final int MUTED = 0xFF8C8C93;
    private static final int REQ_ENABLE = 1;

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
        UiKit.screen(this, "Lock settings", scroller);
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_ENABLE && resultCode == RESULT_OK) {
            Config.setLocksEnabled(this, true);
        }
    }

    private void render() {
        font = Fonts.current(this);
        root.removeAllViews();

        boolean on = Config.isLocksEnabled(this);
        root.addView(row("Lock", on ? "On" : "Off", on ? Color.WHITE : DIM, v -> toggleLock(on)));

        if (!on) return;

        root.addView(row("Master PIN",
                Config.isPinSet(this) ? "Set" : "Not set",
                Config.isPinSet(this) ? MUTED : DIM,
                v -> startActivity(new Intent(this, PinSetupActivity.class)
                        .putExtra(PinSetupActivity.EXTRA_LOCK_ID, "master"))));

        root.addView(row("Re-lock after",
                RelockPicker.format(Config.getRelockSeconds(this)), MUTED,
                v -> RelockPicker.show(this, null, null, this::render)));

        root.addView(sectionHeader("What's locked"));

        lockRow("App list", Lock.APPS.area, Lock.APPS.isLocked(this));

        int n = Config.getLockedPackages(this).size();
        lockRow("Apps (" + n + ")", "applock", Config.isApplockEnabled(this));

        lockRow("Notes", Lock.NOTES.area, Lock.NOTES.isLocked(this));
        lockRow("To-do", Lock.TODOS.area, Lock.TODOS.isLocked(this));
        lockRow("Voice recorder", Lock.RECORDER.area, Lock.RECORDER.isLocked(this));
        lockRow("Search", Lock.SEARCH.area, Lock.SEARCH.isLocked(this));

        lockRow("Plain Settings", "settings", Config.isSettingsLockEnabled(this));
    }

    private void toggleLock(boolean currentlyOn) {
        if (currentlyOn) {
            VaultUi.confirm(this, "Turn off lock?", null,
                    "Turn off", () -> {
                        Config.wipeLockSettings(this);
                        Config.setLocksEnabled(this, false);
                        render();
                    },
                    "Cancel", null);
        } else {
            startActivityForResult(new Intent(this, PinSetupActivity.class)
                    .putExtra(PinSetupActivity.EXTRA_LOCK_ID, "master"), REQ_ENABLE);
        }
    }

    private void lockRow(String label, String id, boolean enabled) {
        String state;
        int color;
        if (!enabled) {
            state = "Off";
            color = DIM;
        } else if ("settings".equals(id)) {
            state = "Master";
            color = MUTED;
        } else if (Config.hasCustomPin(this, id)) {
            state = "Custom";
            color = AMBER;
        } else {
            state = "Master";
            color = MUTED;
        }
        String name = "applock".equals(id) ? "App lock" : label;
        root.addView(row(label, state, color, v -> startActivity(
                new Intent(this, LockDetailActivity.class)
                        .putExtra(LockDetailActivity.EXTRA_LOCK_ID, id)
                        .putExtra(LockDetailActivity.EXTRA_LOCK_NAME, name))));
    }

    private View row(String label, String value, int valueColor, View.OnClickListener listener) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(48, 36, 48, 36);
        StateListDrawable bg = new StateListDrawable();
        bg.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Color.DKGRAY));
        bg.addState(new int[]{}, new ColorDrawable(Color.BLACK));
        row.setBackground(bg);
        row.setOnClickListener(listener);

        TextView l = new TextView(this);
        l.setText(label);
        l.setTextColor(Color.WHITE);
        l.setTextSize(18);
        l.setTypeface(font);
        row.addView(l, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView v = new TextView(this);
        v.setText(value);
        v.setTextColor(valueColor);
        v.setTextSize(14);
        v.setTypeface(font);
        row.addView(v);

        return row;
    }

    private TextView sectionHeader(String label) {
        TextView header = new TextView(this);
        header.setText(label.toUpperCase());
        header.setTextColor(Color.GRAY);
        header.setTextSize(13);
        header.setLetterSpacing(0.15f);
        header.setTypeface(font);
        header.setPadding(48, 36, 48, 12);
        return header;
    }
}

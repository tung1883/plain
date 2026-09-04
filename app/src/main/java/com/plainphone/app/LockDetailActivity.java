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
import android.widget.TextView;
import android.widget.Toast;

/**
 * One lock's settings: the On / Off switch, its PIN (master or a custom one), and
 * how long it stays unlocked after you leave it. {@code applock} also links to the
 * app picker. {@code settings} has no PIN or re-lock row — it always uses the master
 * PIN and always re-prompts. Named by {@code EXTRA_LOCK_ID} / {@code EXTRA_LOCK_NAME}.
 */
public class LockDetailActivity extends Activity {

    static final String EXTRA_LOCK_ID = "lock_id";
    static final String EXTRA_LOCK_NAME = "lock_name";

    private static final int AMBER = 0xFFF0D98A;
    private static final int DIM = 0xFF6A6A6A;
    private static final int MUTED = 0xFF8C8C93;
    private static final int WARN = 0xFFC88F87;

    private String id;
    private String name;
    private Lock section;   // null for applock / settings
    private LinearLayout root;
    private Typeface font;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        id = getIntent().getStringExtra(EXTRA_LOCK_ID);
        name = getIntent().getStringExtra(EXTRA_LOCK_NAME);
        section = Lock.byArea(id);
        font = Fonts.current(this);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);
        String title = name.toLowerCase().endsWith("lock") ? name : name + " lock";
        UiKit.screen(this, title, root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    private boolean isSettings() {
        return "settings".equals(id);
    }

    private boolean isApplock() {
        return "applock".equals(id);
    }

    private boolean enabled() {
        if (section != null) return section.isLocked(this);
        if (isApplock()) return Config.isApplockEnabled(this);
        return Config.isSettingsLockEnabled(this);
    }

    private void render() {
        font = Fonts.current(this);
        root.removeAllViews();

        boolean on = enabled();

        String verb = isApplock() ? "Lock chosen apps" : "Lock " + name;
        root.addView(row(verb, on ? "On" : "Off", on ? Color.WHITE : DIM, v -> toggle()));

        if (isSettings()) {
            root.addView(row("PIN", "Master", WARN, v -> Toast.makeText(this,
                    "This PIN can't be customized", Toast.LENGTH_SHORT).show()));
        } else {
            boolean custom = Config.hasCustomPin(this, id);
            root.addView(row("PIN", custom ? "Custom" : "Master", custom ? AMBER : MUTED,
                    v -> pinChoice()));

            boolean override = Config.hasRelockOverride(this, id);
            String relock = override
                    ? RelockPicker.format(Config.getRelockSeconds(this, id))
                    : "Default · " + RelockPicker.format(Config.getRelockSeconds(this));
            root.addView(row("Re-lock after", relock, override ? AMBER : MUTED,
                    v -> RelockPicker.show(this, id, name, this::render)));
        }

        if (isApplock()) {
            int n = Config.getLockedPackages(this).size();
            root.addView(row("Choose apps", n + "  ›", MUTED,
                    v -> startActivity(new Intent(this, LockedAppsActivity.class))));
        }
    }

    /** The PIN row: pick "Master" (fallback) or "Custom" (opens {@link PinSetupActivity}). */
    private void pinChoice() {
        VaultUi.confirm(this, name + " PIN", null,
                "Use master PIN", () -> {
                    if (!Config.isPinSet(this)) {
                        Toast.makeText(this, "Set a master PIN first (Lock settings)",
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    Config.clearCustomPin(this, id);
                    render();
                },
                "Custom PIN", () -> startActivity(new Intent(this, PinSetupActivity.class)
                        .putExtra(PinSetupActivity.EXTRA_LOCK_ID, id)
                        .putExtra(PinSetupActivity.EXTRA_LOCK_NAME, name)));
    }

    private void toggle() {
        if (section != null) {
            section.toggleLock(this, this::render);
            return;
        }
        boolean on = enabled();
        if (!on && !pinAvailable()) {
            Toast.makeText(this, "Set a master PIN first (Lock settings)",
                    Toast.LENGTH_LONG).show();
            return;
        }
        if (isApplock()) {
            Config.setApplockEnabled(this, !on);
        } else {
            Config.setSettingsLockEnabled(this, !on);
        }
        render();
    }

    private boolean pinAvailable() {
        return isSettings() ? Config.isPinSet(this) : Config.isPinSet(this, id);
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
}

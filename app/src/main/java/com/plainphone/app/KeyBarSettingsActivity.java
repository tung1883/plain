package com.plainphone.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.LinearLayout;

/** Dev settings > Key bar: how tall the special-keys bar is, how big its keys are, and a preview. */
public class KeyBarSettingsActivity extends Activity {

    private LinearLayout root;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        root = SettingsUi.scrollScreen(this, "Key bar");
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    private void render() {
        root.removeAllViews();
        int rows = Config.getKeyBarRows(this);
        String size = Config.getKeyBarSize(this);

        root.addView(SettingsUi.row(this, "Rows shown", rows == 0 ? "All" : String.valueOf(rows),
                Color.WHITE, null, false, v -> pickRows()));
        root.addView(SettingsUi.row(this, "Key size", capitalise(size), Color.WHITE, null, false,
                v -> pickSize()));
        root.addView(SettingsUi.row(this, "Section labels",
                Config.isKeyBarLabels(this) ? "On" : "Off", Color.WHITE, null, false, v -> {
                    Config.setKeyBarLabels(this, !Config.isKeyBarLabels(this));
                    render();
                }));

        root.addView(SettingsUi.row(this, "Preview", null, "Bar as it will look",
                false, null));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(24, 8, 24, 24);
        root.addView(new KeyBar(this, true, id -> { }), lp);
    }

    private void pickRows() {
        int cur = Config.getKeyBarRows(this);
        String[] names = {"1 row", "2 rows", "3 rows", "4 rows", "All"};
        int[] values = {1, 2, 3, 4, 0};
        String[] labels = new String[names.length];
        VaultUi.Choice[] choices = new VaultUi.Choice[names.length];
        for (int i = 0; i < names.length; i++) {
            final int value = values[i];
            labels[i] = (value == cur ? "\u25CF " : "\u25CB ") + names[i];
            choices[i] = () -> {
                Config.setKeyBarRows(this, value);
                render();
            };
        }
        VaultUi.menu(this, "Rows shown", labels, choices);
    }

    private void pickSize() {
        String cur = Config.getKeyBarSize(this);
        String[] values = {"small", "medium", "large"};
        String[] labels = new String[values.length];
        VaultUi.Choice[] choices = new VaultUi.Choice[values.length];
        for (int i = 0; i < values.length; i++) {
            final String value = values[i];
            labels[i] = (value.equals(cur) ? "\u25CF " : "\u25CB ") + capitalise(value);
            choices[i] = () -> {
                Config.setKeyBarSize(this, value);
                render();
            };
        }
        VaultUi.menu(this, "Key size", labels, choices);
    }

    private static String capitalise(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}

package com.plainphone.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Turn the PlainPhone keyboard on and pick it. Its own options live in the keyboard (gear icon). */
public class KeyboardSetupActivity extends Activity {

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
        UiKit.screen(this, "Keyboard", scroller);
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    private boolean enabled() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        for (InputMethodInfo i : imm.getEnabledInputMethodList()) {
            if (i.getPackageName().equals(getPackageName())) return true;
        }
        return false;
    }

    private void render() {
        root.removeAllViews();
        root.addView(row("Keyboard: " + (enabled() ? "On" : "Off"), v ->
                startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))));
        root.addView(row("Choose keyboard", v -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showInputMethodPicker();
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
        bg.addState(new int[]{android.R.attr.state_pressed}, UiKit.pressedFill());
        bg.setEnterFadeDuration(120);
        bg.setExitFadeDuration(120);
        bg.addState(new int[]{}, new ColorDrawable(Color.BLACK));
        view.setBackground(bg);
        view.setOnClickListener(listener);
        return view;
    }
}

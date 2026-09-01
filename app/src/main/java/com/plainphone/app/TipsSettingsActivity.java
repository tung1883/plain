package com.plainphone.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;
import android.widget.TextView;

public class TipsSettingsActivity extends Activity {

    private LinearLayout root;
    private Typeface font;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);

        ScrollView scroller = new ScrollView(this);
        scroller.setBackgroundColor(Color.BLACK);
        scroller.addView(root, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(scroller);
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    private void render() {
        font = Fonts.current(this);
        root.removeAllViews();

        root.addView(sectionHeader("Tips"));
        root.addView(row("Tips: " + (Config.isTipsEnabled(this) ? "On" : "Off"), v -> {
            Config.setTipsEnabled(this, !Config.isTipsEnabled(this));
            render();
        }));
        root.addView(row("Edit tips", v ->
                startActivity(TipsListEditActivity.forQuotes(this, false))));
        root.addView(row("Reset tips to default", v -> confirmReset(false)));

        root.addView(sectionHeader("Quotes"));
        root.addView(row("Quotes: " + (Config.isQuotesEnabled(this) ? "On" : "Off"), v -> {
            Config.setQuotesEnabled(this, !Config.isQuotesEnabled(this));
            render();
        }));
        root.addView(row("Edit quotes", v ->
                startActivity(TipsListEditActivity.forQuotes(this, true))));
        root.addView(row("Reset quotes to default", v -> confirmReset(true)));
    }

    private void confirmReset(boolean quotes) {
        LinearLayout popup = new LinearLayout(this);
        popup.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable box = new GradientDrawable();
        box.setColor(Color.BLACK);
        popup.setBackground(box);
        popup.setPadding(0, 32, 0, 8);

        TextView title = new TextView(this);
        title.setText("Reset " + (quotes ? "quotes" : "tips") + " to default?");
        title.setTextColor(Color.WHITE);
        title.setTextSize(18);
        title.setTypeface(font);
        title.setPadding(48, 0, 48, 16);
        popup.addView(title);

        AlertDialog dialog = new AlertDialog.Builder(this).setView(popup).create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        popup.addView(optionRow("Reset", v -> {
            dialog.dismiss();
            if (quotes) {
                Config.setQuotesText(this, null);
            } else {
                Config.setTipsText(this, null);
            }
            Toast.makeText(this, "Reset", Toast.LENGTH_SHORT).show();
        }));
        popup.addView(optionRow("Cancel", v -> dialog.dismiss()));

        dialog.show();
        if (dialog.getWindow() != null) {
            WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
            params.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.85);
            dialog.getWindow().setAttributes(params);
        }
    }

    private TextView optionRow(String label, View.OnClickListener listener) {
        TextView row = new TextView(this);
        row.setText(label);
        row.setTextColor(Color.WHITE);
        row.setTextSize(20);
        row.setTypeface(font);
        row.setPadding(48, 32, 48, 32);
        row.setGravity(Gravity.START);
        StateListDrawable bg = new StateListDrawable();
        bg.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Color.DKGRAY));
        bg.addState(new int[]{}, new ColorDrawable(Color.BLACK));
        row.setBackground(bg);
        row.setOnClickListener(listener);
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

    private TextView row(String label, View.OnClickListener listener) {
        TextView view = new TextView(this);
        view.setText(label);
        view.setTextColor(Color.WHITE);
        view.setTextSize(20);
        view.setPadding(48, 40, 48, 40);
        view.setGravity(Gravity.START);
        view.setTypeface(font);

        StateListDrawable background = new StateListDrawable();
        background.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Color.DKGRAY));
        background.addState(new int[]{}, new ColorDrawable(Color.BLACK));
        view.setBackground(background);

        view.setOnClickListener(listener);
        return view;
    }
}

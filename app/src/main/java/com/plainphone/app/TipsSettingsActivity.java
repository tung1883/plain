package com.plainphone.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class TipsSettingsActivity extends Activity {

    private static final int REQUEST_IMPORT_TIPS = 5401;
    private static final int REQUEST_IMPORT_QUOTES = 5402;

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
        UiKit.screen(this, "Tips, quotes and warnings", scroller);
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    private void render() {
        font = Fonts.current(this);
        root.removeAllViews();

        root.addView(sectionHeader("Rotation"));
        int rotate = Config.getTipRotateMinutes(this);
        root.addView(row("Rotate every: " + (rotate <= 0 ? "On tap only" : rotate + " min"),
                v -> startActivity(new Intent(this, TipRotateActivity.class))));

        root.addView(sectionHeader("Tips"));
        root.addView(row("Tips: " + (Config.isTipsEnabled(this) ? "On" : "Off"), v -> {
            Config.setTipsEnabled(this, !Config.isTipsEnabled(this));
            render();
        }));
        root.addView(row("Edit tips", v ->
                startActivity(TipsListEditActivity.forQuotes(this, false))));
        root.addView(row("Import tips from file", v -> pickImportFile(REQUEST_IMPORT_TIPS)));
        root.addView(row("Reset tips to default", v -> confirmReset(false)));

        root.addView(sectionHeader("Quotes"));
        root.addView(row("Quotes: " + (Config.isQuotesEnabled(this) ? "On" : "Off"), v -> {
            Config.setQuotesEnabled(this, !Config.isQuotesEnabled(this));
            render();
        }));
        root.addView(row("Edit quotes", v ->
                startActivity(TipsListEditActivity.forQuotes(this, true))));
        root.addView(row("Import quotes from file", v -> pickImportFile(REQUEST_IMPORT_QUOTES)));
        root.addView(row("Reset quotes to default", v -> confirmReset(true)));

        root.addView(sectionHeader("Warnings"));
        root.addView(row("Warning: " + (Config.isAccessWarnEnabled(this) ? "On" : "Off"), v -> {
            Config.setAccessWarnEnabled(this, !Config.isAccessWarnEnabled(this));
            render();
        }));
    }

    private void pickImportFile(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        if (requestCode != REQUEST_IMPORT_TIPS && requestCode != REQUEST_IMPORT_QUOTES) return;

        String fileText = readFile(data.getData());
        if (fileText == null) {
            Toast.makeText(this, "Couldn't read that file", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> imported = new ArrayList<>();
        for (String line : fileText.split("\n")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) imported.add(trimmed);
        }
        if (imported.isEmpty()) {
            Toast.makeText(this, "No lines found in that file", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean quotes = requestCode == REQUEST_IMPORT_QUOTES;
        List<String> combined = new ArrayList<>(quotes ? Tips.quotes(this) : Tips.tips(this));
        combined.addAll(imported);
        String joined = String.join("\n", combined);
        if (quotes) {
            Config.setQuotesText(this, joined);
        } else {
            Config.setTipsText(this, joined);
        }
        Toast.makeText(this, "Added " + imported.size() + " "
                + (quotes ? "quote" : "tip") + (imported.size() == 1 ? "" : "s"),
                Toast.LENGTH_SHORT).show();
        render();
    }

    private String readFile(Uri uri) {
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            if (in == null) return null;
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
            return out.toString(StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            return null;
        }
    }

    private void confirmReset(boolean quotes) {
        LinearLayout popup = new LinearLayout(this);
        popup.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable box = new GradientDrawable();
        box.setColor(Color.BLACK);
        popup.setBackground(box);
        popup.setPadding(0, 32, 0, 8);

        popup.addView(UiKit.dialogTitle(this, "Reset " + (quotes ? "quotes" : "tips")
                + " to default?"));

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

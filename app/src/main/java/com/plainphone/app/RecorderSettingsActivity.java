package com.plainphone.app;

import android.app.Activity;
import android.app.AlertDialog;
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
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class RecorderSettingsActivity extends Activity {

    private static final String[] FORMATS = {"wav", "m4a", "3gp"};
    private static final String[] FORMAT_LABELS = {
            "WAV — lossless, biggest", "M4A — AAC, small", "3GP — AMR, tiny (voice)"};
    private static final int[] RATES = {8000, 16000, 22050, 44100, 48000};

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
        UiKit.screen(this, "Voice recorder", scroller);
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    private void render() {
        font = Fonts.current(this);
        root.removeAllViews();

        root.addView(row("Format: " + Config.getRecorderFormat(this).toUpperCase(Locale.US),
                v -> pickFormat()));
        root.addView(row("Sample rate: " + Config.getRecorderSampleRate(this) + " Hz",
                v -> pickRate()));
        root.addView(row("Noise reduction: "
                + (Config.isRecorderNoiseReduction(this) ? "On" : "Off"), v -> {
            Config.setRecorderNoiseReduction(this, !Config.isRecorderNoiseReduction(this));
            render();
        }));

        root.addView(row("Locked: " + (Lock.RECORDER.isLocked(this) ? "On" : "Off"),
                v -> Lock.RECORDER.toggleLock(this, this::render)));

        int local = Recorder.all(this).size();
        if (local > 0 && VaultFormat.exists(VaultSession.vaultRoot(this))) {
            root.addView(row("Move all recordings to vault", v -> {
                if (!VaultSession.get().isUnlocked()) {
                    Toast.makeText(this, "Unlock the vault first", Toast.LENGTH_SHORT).show();
                    startActivity(new android.content.Intent(this, VaultActivity.class));
                    return;
                }
                VaultUi.confirm(this, "Move " + local + " recording"
                                + (local == 1 ? "" : "s") + " to the vault?",
                        "They'll be encrypted and only playable while the vault is unlocked.",
                        "Move", () -> {
                            java.util.List<String> ids = new java.util.ArrayList<>();
                            for (Recording r : Recorder.all(this)) ids.add(r.id);
                            SectionJobs.startRecorderToVault(this, ids);
                            Toast.makeText(this, "Move queued", Toast.LENGTH_SHORT).show();
                            render();
                        }, "Cancel", null);
            }));
        }
    }

    private void pickFormat() {
        LinearLayout box = popup("Format");
        AlertDialog dialog = dialog(box);
        for (int i = 0; i < FORMATS.length; i++) {
            String value = FORMATS[i];
            box.addView(option(FORMAT_LABELS[i], value.equals(Config.getRecorderFormat(this)), v -> {
                Config.setRecorderFormat(this, value);
                dialog.dismiss();
                render();
            }));
        }
        show(dialog);
    }

    private void pickRate() {
        LinearLayout box = popup("Sample rate");
        AlertDialog dialog = dialog(box);
        for (int rate : RATES) {
            box.addView(option(rate + " Hz", rate == Config.getRecorderSampleRate(this), v -> {
                Config.setRecorderSampleRate(this, rate);
                dialog.dismiss();
                render();
            }));
        }
        TextView caption = new TextView(this);
        caption.setText("3GP always records at 8000 Hz.");
        caption.setTextColor(0xFF6F6F6F);
        caption.setTextSize(12);
        caption.setTypeface(font);
        caption.setPadding(48, 8, 48, 16);
        box.addView(caption);
        show(dialog);
    }

    // --- popup plumbing ------------------------------------------------------

    private LinearLayout popup(String title) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackground(UiKit.dialogBackground(this));
        UiKit.clipRounded(this, box, UiKit.R_MD);
        box.setPadding(2, 16, 2, UiKit.dp(this, UiKit.R_MD));
        box.addView(UiKit.dialogTitle(this, title));
        return box;
    }

    // Set by dialog(), consumed by show() right after — sequential, never concurrent.
    private android.widget.FrameLayout lastPopupScrim;

    private AlertDialog dialog(LinearLayout box) {
        lastPopupScrim = UiKit.wrapScrim(this, box, 0.85f);
        return new AlertDialog.Builder(this, R.style.Theme_PlainPhone_RoundedDialog)
                .setView(lastPopupScrim).create();
    }

    private void show(AlertDialog dialog) {
        UiKit.finishCentered(dialog, lastPopupScrim);
    }

    private TextView option(String label, boolean selected, View.OnClickListener listener) {
        TextView view = new TextView(this);
        view.setText(label);
        view.setTextColor(selected ? Color.BLACK : Color.WHITE);
        view.setTextSize(16);
        view.setTypeface(font);
        view.setPadding(48, 30, 48, 30);
        view.setGravity(Gravity.START);
        if (selected) {
            view.setBackgroundColor(Color.WHITE);
        } else {
            StateListDrawable bg = new StateListDrawable();
            bg.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Color.DKGRAY));
            bg.addState(new int[]{}, new ColorDrawable(Color.BLACK));
            view.setBackground(bg);
        }
        view.setOnClickListener(listener);
        return view;
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

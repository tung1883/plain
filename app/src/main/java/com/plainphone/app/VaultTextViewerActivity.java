package com.plainphone.app;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.charset.StandardCharsets;

/**
 * Decrypts one vault entry into memory as UTF-8 text and shows it in an editable
 * field. Save re-encrypts in place. FLAG_SECURE, no share, plaintext never
 * written to disk here.
 */
public class VaultTextViewerActivity extends Activity {

    private String docId;
    private EditText editor;
    private TextView saveButton;
    private boolean dirty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        if (!VaultSession.get().isUnlocked()) {
            finish();
            return;
        }
        docId = getIntent().getStringExtra("docId");

        Typeface font = Fonts.current(this);
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setBackgroundColor(Color.BLACK);

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);

        TextView name = new TextView(this);
        name.setText(getIntent().getStringExtra("name"));
        name.setTextColor(Color.GRAY);
        name.setTextSize(13);
        name.setTypeface(font);
        name.setSingleLine(true);
        name.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
        name.setPadding(48, 36, 24, 20);
        bar.addView(name, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        saveButton = new TextView(this);
        saveButton.setText("SAVE");
        saveButton.setTextColor(Color.DKGRAY);
        saveButton.setTextSize(13);
        saveButton.setTypeface(font);
        saveButton.setPadding(24, 36, 24, 20);
        saveButton.setOnClickListener(v -> save());
        bar.addView(saveButton);

        TextView close = new TextView(this);
        close.setText("✕");
        close.setTextColor(Color.WHITE);
        close.setTextSize(16);
        close.setTypeface(font);
        close.setPadding(24, 32, 40, 20);
        close.setOnClickListener(v -> onBackPressed());
        bar.addView(close);
        column.addView(bar);

        editor = new EditText(this);
        editor.setBackgroundColor(Color.BLACK);
        editor.setTextColor(Color.WHITE);
        editor.setTypeface(font);
        editor.setTextSize(15);
        editor.setGravity(Gravity.TOP | Gravity.START);
        editor.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        editor.setPadding(48, 24, 48, 48);

        try {
            byte[] plain = VaultStore.decryptToMemory(this, docId);
            int cap = 2 * 1024 * 1024;                      // read-only EditText tolerates ~this much
            if (plain.length > cap) {
                editor.setText(new String(plain, 0, cap, StandardCharsets.UTF_8)
                        + "\n\n… (truncated — file is too large to show in full)");
                editor.setEnabled(false);                  // don't let a partial save corrupt it
                saveButton.setVisibility(android.view.View.GONE);
            } else {
                editor.setText(new String(plain, StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            Toast.makeText(this, "Couldn't open", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        editor.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) {}
            public void afterTextChanged(Editable s) {
                dirty = true;
                saveButton.setTextColor(Color.WHITE);
            }
        });

        ScrollView scroller = new ScrollView(this);
        scroller.setBackgroundColor(Color.BLACK);
        scroller.addView(editor, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.MATCH_PARENT));
        column.addView(scroller, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(column);
        VaultUnlockService.touch(this);
    }

    private void save() {
        if (!dirty) {
            finish();
            return;
        }
        try {
            VaultStore.writeText(this, docId, editor.getText().toString());
            dirty = false;
            saveButton.setTextColor(Color.DKGRAY);
            VaultUnlockService.touch(this);
            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Save failed", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        if (!dirty) {
            super.onBackPressed();
            return;
        }
        VaultUi.confirm(this, "Save changes?", null,
                "Save", () -> {
                    save();
                    finish();
                }, "Discard", this::finish);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!VaultSession.get().isUnlocked()) finish();
    }
}

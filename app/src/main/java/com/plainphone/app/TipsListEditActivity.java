package com.plainphone.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.widget.EditText;

/**
 * Raw editor for the tip list or the quote list — one entry per line. Reached
 * with {@code extra "quotes" == true} for the quote list. Autosave debounced,
 * flushed on pause.
 */
public class TipsListEditActivity extends Activity {

    static final String EXTRA_QUOTES = "quotes";

    private static final long SAVE_DEBOUNCE_MS = 400;

    private boolean quotes;
    private EditText input;
    private boolean bound;
    private boolean dirty;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable saveTask = this::save;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        quotes = getIntent().getBooleanExtra(EXTRA_QUOTES, false);

        Typeface font = Fonts.current(this);

        input = new EditText(this);
        input.setBackgroundColor(Color.BLACK);
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(Color.GRAY);
        input.setHint(quotes ? "One quote per line" : "One tip per line");
        input.setTypeface(font);
        input.setTextSize(16);
        input.setGravity(Gravity.TOP | Gravity.START);
        input.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setSingleLine(false);
        input.setPadding(48, 40, 48, 40);
        setContentView(input);

        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (!bound) return;
                dirty = true;
                handler.removeCallbacks(saveTask);
                handler.postDelayed(saveTask, SAVE_DEBOUNCE_MS);
            }
        });
    }

    static Intent forQuotes(Activity host, boolean quotes) {
        return new Intent(host, TipsListEditActivity.class).putExtra(EXTRA_QUOTES, quotes);
    }

    @Override
    protected void onResume() {
        super.onResume();
        bound = false;
        String stored = quotes ? Config.getQuotesText(this) : Config.getTipsText(this);
        String text = stored == null || stored.trim().isEmpty()
                ? (quotes ? Tips.defaultQuotesText() : Tips.defaultTipsText())
                : stored;
        if (!input.getText().toString().equals(text)) {
            input.setText(text);
            input.setSelection(input.getText().length());
        }
        bound = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(saveTask);
        save();
    }

    private void save() {
        if (!dirty) return;
        dirty = false;
        String text = input.getText().toString().trim();
        if (quotes) {
            Config.setQuotesText(this, text.isEmpty() ? null : text);
        } else {
            Config.setTipsText(this, text.isEmpty() ? null : text);
        }
    }
}

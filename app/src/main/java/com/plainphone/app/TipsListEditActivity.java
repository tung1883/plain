package com.plainphone.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Raw editor for the tip list or the quote list — one entry per line. Reached
 * with {@code extra "quotes" == true} for the quote list. Autosave debounced,
 * flushed on pause.
 */
public class TipsListEditActivity extends Activity {

    static final String EXTRA_QUOTES = "quotes";

    private static final long SAVE_DEBOUNCE_MS = 400;
    private static final long WARNING_MS = 1200;
    private static final int MAX_LINE = 60;

    private boolean quotes;
    private EditText input;
    private TextView header;
    private String baseHeader;
    private boolean bound;
    private boolean dirty;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable saveTask = this::save;
    private final Runnable clearWarning = this::showBaseHeader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        quotes = getIntent().getBooleanExtra(EXTRA_QUOTES, false);
        baseHeader = quotes ? "QUOTE LIST" : "TIP LIST";

        Typeface font = Fonts.current(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);

        header = new TextView(this);
        header.setTextColor(Color.GRAY);
        header.setTextSize(13);
        header.setLetterSpacing(0.15f);
        header.setTypeface(font);
        header.setPadding(48, 40, 48, 12);
        header.setText(baseHeader);
        root.addView(header, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

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
        input.setPadding(48, 12, 48, 40);
        input.setFilters(new InputFilter[]{maxLineFilter()});
        root.addView(input, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        UiKit.screen(this, quotes ? "Edit quotes" : "Edit tips", root);

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

    /** Caps every line at {@link #MAX_LINE} characters; newlines reset the count. */
    private InputFilter maxLineFilter() {
        return (source, start, end, dest, dstart, dend) -> {
            int lineStart = dstart;
            while (lineStart > 0 && dest.charAt(lineStart - 1) != '\n') lineStart--;
            int lineEnd = dend;
            while (lineEnd < dest.length() && dest.charAt(lineEnd) != '\n') lineEnd++;
            int kept = (dstart - lineStart) + (lineEnd - dend);

            StringBuilder out = new StringBuilder();
            int budget = MAX_LINE - kept;
            for (int i = start; i < end; i++) {
                char c = source.charAt(i);
                if (c == '\n') {
                    out.append(c);
                    budget = MAX_LINE;
                } else if (budget > 0) {
                    out.append(c);
                    budget--;
                }
            }
            if (out.length() == end - start) return null;
            if (bound) flashLimitWarning();
            return out.toString();
        };
    }

    private void flashLimitWarning() {
        header.setText("!No more than 60 characters");
        header.setTextColor(Color.WHITE);
        handler.removeCallbacks(clearWarning);
        handler.postDelayed(clearWarning, WARNING_MS);
    }

    private void showBaseHeader() {
        header.setText(baseHeader);
        header.setTextColor(Color.GRAY);
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

package com.plainphone.app;

import android.app.Activity;
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
 * The whole to-do list as raw todo.txt text — the power-user escape hatch. One
 * multiline field. No per-task form.
 *
 * <p>Autosave is debounced (and flushed on pause) because in file mode a save is
 * a SAF write, too costly to run on every keystroke.
 */
public class TodoListEditActivity extends Activity {

    private static final long SAVE_DEBOUNCE_MS = 500;

    private EditText input;
    private boolean bound;
    private boolean dirty;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable saveTask = this::save;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Typeface font = Fonts.current(this);

        input = new EditText(this);
        input.setBackgroundColor(Color.BLACK);
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(Color.GRAY);
        input.setHint("(A) 2025-08-31 Buy milk +groceries @store");
        input.setTypeface(font);
        input.setTextSize(16);
        input.setGravity(Gravity.TOP | Gravity.START);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setSingleLine(false);
        input.setPadding(48, 40, 48, 40);
        UiKit.screen(this, "Edit as text", input);

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

    @Override
    protected void onResume() {
        super.onResume();
        bound = false;
        String text = Todos.rawText(this);
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
        Todos.saveRawText(this, input.getText().toString());
    }
}

package com.plainphone.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.widget.EditText;

import java.util.Iterator;
import java.util.List;

public class NoteEditActivity extends Activity {

    private String noteId;
    private EditText editor;
    private boolean bound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        noteId = getIntent().getStringExtra("noteId");

        editor = new EditText(this);
        editor.setBackgroundColor(Color.BLACK);
        editor.setTextColor(Color.WHITE);
        editor.setHintTextColor(Color.GRAY);
        editor.setHint("Write a note");
        editor.setTypeface(Fonts.current(this));
        editor.setTextSize(18);
        editor.setGravity(Gravity.TOP | Gravity.START);
        editor.setPadding(48, 48, 48, 48);
        editor.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        editor.setSingleLine(false);

        setContentView(editor);

        editor.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (!bound) return;
                updateNote(s.toString());
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Note note = Note.findById(Config.getNotes(this), noteId);
        if (note == null) {
            finish();
            return;
        }
        bound = false;
        if (!editor.getText().toString().equals(note.text)) {
            editor.setText(note.text);
        }
        editor.setSelection(editor.getText().length());
        bound = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        List<Note> notes = Config.getNotes(this);
        Iterator<Note> it = notes.iterator();
        boolean changed = false;
        while (it.hasNext()) {
            Note note = it.next();
            if (note.id.equals(noteId) && note.text.trim().isEmpty()) {
                it.remove();
                changed = true;
            }
        }
        if (changed) Config.setNotes(this, notes);
    }

    private void updateNote(String text) {
        List<Note> notes = Config.getNotes(this);
        Note note = Note.findById(notes, noteId);
        if (note == null) return;
        note.text = text;
        note.updatedAt = System.currentTimeMillis();
        Config.setNotes(this, notes);
    }
}

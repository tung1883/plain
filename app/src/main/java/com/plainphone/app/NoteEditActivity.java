package com.plainphone.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Iterator;
import java.util.List;

public class NoteEditActivity extends Activity {

    private static final int REQUEST_EXPORT = 3101;

    private String noteId;
    private boolean autoExport;
    private boolean autoExportDone;
    private boolean bound;

    private EditText titleInput;
    private EditText bodyInput;
    private TextView editedLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        noteId = getIntent().getStringExtra("noteId");
        autoExport = getIntent().getBooleanExtra("autoExport", false);

        Typeface font = Fonts.current(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);

        titleInput = new EditText(this);
        titleInput.setBackground(null);
        titleInput.setTextColor(Color.WHITE);
        titleInput.setHintTextColor(Color.GRAY);
        titleInput.setHint("Title");
        titleInput.setTypeface(font);
        titleInput.setTextSize(24);
        titleInput.setSingleLine(true);
        titleInput.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        titleInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        titleInput.setPadding(48, 40, 48, 12);
        root.addView(titleInput, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        View divider = new View(this);
        divider.setBackgroundColor(Color.DKGRAY);
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 2);
        dividerParams.leftMargin = 48;
        dividerParams.rightMargin = 48;
        root.addView(divider, dividerParams);

        bodyInput = new EditText(this);
        bodyInput.setBackground(null);
        bodyInput.setTextColor(Color.WHITE);
        bodyInput.setHintTextColor(Color.GRAY);
        bodyInput.setHint("Note");
        bodyInput.setTypeface(font);
        bodyInput.setTextSize(18);
        bodyInput.setGravity(Gravity.TOP | Gravity.START);
        bodyInput.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        bodyInput.setSingleLine(false);
        bodyInput.setPadding(48, 16, 48, 16);
        root.addView(bodyInput, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout footer = new LinearLayout(this);
        footer.setOrientation(LinearLayout.HORIZONTAL);
        footer.setGravity(Gravity.CENTER_VERTICAL);
        footer.setPadding(48, 12, 48, 20);

        editedLabel = new TextView(this);
        editedLabel.setTextColor(Color.GRAY);
        editedLabel.setTextSize(12);
        editedLabel.setTypeface(font);
        footer.addView(editedLabel, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        footer.addView(footerAction(font, "Export", 0, v -> startExport()));
        footer.addView(footerAction(font, "Delete", 32,
                v -> Notes.confirmDelete(this, currentNote(), this::finish)));

        root.addView(footer, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        setContentView(root);

        titleInput.addTextChangedListener(new SaveWatcher());
        bodyInput.addTextChangedListener(new SaveWatcher());
    }

    private TextView footerAction(Typeface font, String label, int leftPad, View.OnClickListener l) {
        TextView view = new TextView(this);
        view.setText(label);
        view.setTextColor(Color.GRAY);
        view.setTextSize(13);
        view.setTypeface(font);
        view.setPadding(leftPad, 8, 0, 8);
        view.setOnClickListener(l);
        return view;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Note note = currentNote();
        if (note == null) {
            finish();
            return;
        }
        bound = false;
        if (!titleInput.getText().toString().equals(note.title)) {
            titleInput.setText(note.title);
        }
        if (!bodyInput.getText().toString().equals(note.text)) {
            bodyInput.setText(note.text);
        }
        bodyInput.setSelection(bodyInput.getText().length());
        editedLabel.setText(note.editedLabel());
        bound = true;

        if (autoExport && !autoExportDone) {
            autoExportDone = true;
            bodyInput.post(this::startExport);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        List<Note> notes = Config.getNotes(this);
        Iterator<Note> it = notes.iterator();
        boolean changed = false;
        while (it.hasNext()) {
            Note note = it.next();
            if (note.id.equals(noteId) && note.isBlank()) {
                it.remove();
                changed = true;
            }
        }
        if (changed) Config.setNotes(this, notes);
    }

    private Note currentNote() {
        return Note.findById(Config.getNotes(this), noteId);
    }

    private void save() {
        List<Note> notes = Config.getNotes(this);
        Note note = Note.findById(notes, noteId);
        if (note == null) return;
        note.title = titleInput.getText().toString();
        note.text = bodyInput.getText().toString();
        note.updatedAt = System.currentTimeMillis();
        Config.setNotes(this, notes);
        editedLabel.setText(note.editedLabel());
    }

    private void startExport() {
        Note note = currentNote();
        if (note == null) return;
        Notes.showExportOptions(this, note, REQUEST_EXPORT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_EXPORT || resultCode != RESULT_OK
                || data == null || data.getData() == null) {
            return;
        }
        Note note = currentNote();
        boolean ok = note != null && Notes.writeNote(this, data.getData(), note);
        Toast.makeText(this, ok ? "Exported" : "Export failed", Toast.LENGTH_SHORT).show();
    }

    private class SaveWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable s) {
            if (bound) save();
        }
    }
}

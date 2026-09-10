package com.plainphone.app;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/** A notes window: the note list (home-section style) with an inline editor. */
final class NotesPanel extends PluginPanel {

    private LinearLayout editor;
    private EditText body;
    private String editingId;
    private final Handler save = new Handler();

    @Override public String kind() { return "notes"; }
    @Override public String title() { return "Notes"; }
    @Override HomeMode section() { return HomeMode.NOTES; }

    @Override void renderNormal(List<Object> rows) { NotesSection.render(this, this::openInline, rows); }
    @Override void renderSelectionRows(List<Object> rows) { NotesSection.renderSelection(this, rows); }
    @Override String selectionIdOf(Object payload) { return NotesSection.selectionId(payload); }

    @Override
    void afterRoot() {
        editor = new LinearLayout(ctx);
        editor.setOrientation(LinearLayout.VERTICAL);
        editor.setBackgroundColor(Color.BLACK);
        editor.setVisibility(View.GONE);

        TextView back = new TextView(ctx);
        back.setText("←  Notes");
        back.setTextColor(Color.WHITE);
        back.setTextSize(14);
        back.setTypeface(Fonts.current(ctx));
        back.setPadding(dp(20), dp(14), dp(20), dp(14));
        back.setOnClickListener(v -> closeInline());
        editor.addView(back, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        View hair = new View(ctx);
        hair.setBackgroundColor(0xFF1C1C1C);
        editor.addView(hair, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));

        body = new EditText(ctx);
        body.setGravity(Gravity.TOP | Gravity.START);
        body.setBackground(null);
        body.setTextColor(Color.WHITE);
        body.setTextSize(16);
        body.setTypeface(Fonts.current(ctx));
        body.setPadding(dp(20), dp(16), dp(20), dp(20));
        body.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) {
                save.removeCallbacksAndMessages(null);
                save.postDelayed(NotesPanel.this::persist, 400);
            }
            public void afterTextChanged(Editable s) {}
        });
        editor.addView(body, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        root.addView(editor, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    @Override
    public void onShow() {
        if (editingId != null) return; // stay in the editor
        super.onShow();
    }

    @Override public void onLeave() { persist(); super.onLeave(); }
    @Override public void onClose() { persist(); super.onClose(); }

    private void openInline(String id) {
        editingId = id;
        Note n = Note.findById(Config.getNotes(ctx), id);
        body.setText(n != null ? n.text : "");
        editor.setVisibility(View.VISIBLE);
        editor.bringToFront();
        body.requestFocus();
    }

    private void closeInline() {
        persist();
        editingId = null;
        editor.setVisibility(View.GONE);
        list.refresh();
    }

    private void persist() {
        if (editingId == null) return;
        List<Note> all = Config.getNotes(ctx);
        Note n = Note.findById(all, editingId);
        if (n == null) return;
        n.text = body.getText().toString();
        Config.setNotes(ctx, all);
    }

    private int dp(float v) {
        return Math.round(v * ctx.getResources().getDisplayMetrics().density);
    }
}

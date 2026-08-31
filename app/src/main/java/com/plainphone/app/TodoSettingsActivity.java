package com.plainphone.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class TodoSettingsActivity extends Activity {

    private LinearLayout root;
    private Typeface georgia;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        georgia = Fonts.current(this);

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

    private static final int REQUEST_PICK_TODO_FILE = 4311;

    private void render() {
        georgia = Fonts.current(this);
        root.removeAllViews();

        root.addView(sectionHeader("Lock"));
        root.addView(row("Locked: " + (Lock.TODOS.isLocked(this) ? "On" : "Off"),
                v -> Lock.TODOS.toggleLock(this, this::render)));

        root.addView(sectionHeader("Tasks"));
        root.addView(row("Show completed: " + (Config.isTodosShowCompleted(this) ? "On" : "Off"),
                v -> {
                    Config.setTodosShowCompleted(this, !Config.isTodosShowCompleted(this));
                    render();
                }));

        int done = Todos.completedCount(this);
        root.addView(row("Archive completed (" + done + ")", v -> {
            if (done == 0) {
                Toast.makeText(this, "Nothing to archive", Toast.LENGTH_SHORT).show();
                return;
            }
            confirmArchive(done);
        }));

        root.addView(row("Edit as text", v ->
                startActivity(new android.content.Intent(this, TodoListEditActivity.class))));

        root.addView(sectionHeader("File"));
        root.addView(row("Todo file: " + Todos.fileLabel(this),
                v -> Todos.showFileOptions(this, REQUEST_PICK_TODO_FILE, this::render)));

        root.addView(row("Guide", v ->
                startActivity(new android.content.Intent(this, TodoGuideActivity.class))));
    }

    private TextView sectionHeader(String label) {
        TextView header = new TextView(this);
        header.setText(label.toUpperCase());
        header.setTextColor(Color.GRAY);
        header.setTextSize(13);
        header.setLetterSpacing(0.15f);
        header.setTypeface(georgia);
        header.setPadding(48, 36, 48, 12);
        return header;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_TODO_FILE && resultCode == RESULT_OK) {
            String message = Todos.handleFilePick(this, data);
            if (message != null) Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            render();
        }
    }

    private void confirmArchive(int done) {
        new AlertDialog.Builder(this)
                .setTitle("Archive " + done + " completed task" + (done == 1 ? "" : "s") + "?")
                .setMessage("They move out of the list into the done archive.")
                .setPositiveButton("Archive", (d, w) -> {
                    Todos.archiveCompleted(this);
                    render();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private TextView row(String label, View.OnClickListener listener) {
        TextView view = new TextView(this);
        view.setText(label);
        view.setTextColor(Color.WHITE);
        view.setTextSize(20);
        view.setPadding(48, 40, 48, 40);
        view.setGravity(Gravity.START);
        view.setTypeface(georgia);

        StateListDrawable background = new StateListDrawable();
        background.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Color.DKGRAY));
        background.addState(new int[]{}, new ColorDrawable(Color.BLACK));
        view.setBackground(background);

        view.setOnClickListener(listener);
        return view;
    }
}

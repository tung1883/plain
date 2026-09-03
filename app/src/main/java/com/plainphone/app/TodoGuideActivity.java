package com.plainphone.app;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class TodoGuideActivity extends Activity {

    private static final String[][] SECTIONS = {
            {"The format",
                    "Plain stores to-dos as todo.txt — one task per line, plain text, the same "
                    + "format todo.sh and many other apps use.\n\n"
                    + "    (A) 2026-08-31 Call plumber +house @phone due:2026-09-02\n"
                    + "    x 2026-08-30 Buy milk +groceries\n\n"
                    + "Leading x marks a task done. (A)–(Z) is priority. The first bare date is "
                    + "the creation date. +project, @context and key:value tags (like due:) are "
                    + "free text and are kept exactly as you type them."},
            {"Where tasks live",
                    "By default the list is stored inside Plain. It is private to this app and "
                    + "included in nothing that leaves the phone.\n\n"
                    + "\"Edit as text\" opens the whole list as raw todo.txt — the escape hatch "
                    + "for bulk edits."},
            {"Linking a file",
                    "\"Todo file\" → Link file lets you point Plain at a todo.txt file on the "
                    + "phone (pick an existing one, or create it in the picker).\n\n"
                    + "If that file already has tasks, it wins and becomes the list Plain shows. "
                    + "If it is empty, your current tasks are written into it.\n\n"
                    + "From then on Plain reads the file every time the list is shown and writes "
                    + "it on every change. Unlink copies the file's tasks back inside Plain."},
            {"Syncing",
                    "Plain does not sync. It only reads and writes that one file.\n\n"
                    + "To see the same list on a computer, use a separate sync tool — Syncthing, "
                    + "a Dropbox/Nextcloud folder, etc. — to mirror the file. Plain is then just "
                    + "one editor of a shared file, like todo.sh is another.\n\n"
                    + "Changes made elsewhere show up the next time the To-do list is redrawn "
                    + "(reopen it, or swipe away and back)."},
            {"Conflicts",
                    "There is no merge. Whichever side saves last wins. An edit made on a "
                    + "computer while Plain has the text editor open can be overwritten when "
                    + "Plain saves. Close the editor before editing the file elsewhere."},
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Typeface font = Fonts.current(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);
        root.setPadding(48, 40, 48, 56);

        TextView heading = new TextView(this);
        heading.setText("To-do & todo.txt");
        heading.setTextColor(Color.WHITE);
        heading.setTextSize(24);
        heading.setTypeface(font);
        heading.setPadding(0, 0, 0, 8);
        root.addView(heading);

        for (String[] section : SECTIONS) {
            TextView title = new TextView(this);
            title.setText(section[0].toUpperCase());
            title.setTextColor(Color.GRAY);
            title.setTextSize(13);
            title.setLetterSpacing(0.15f);
            title.setTypeface(font);
            title.setPadding(0, 36, 0, 10);
            title.setGravity(Gravity.START);
            root.addView(title);

            TextView body = new TextView(this);
            body.setText(section[1]);
            body.setTextColor(Color.WHITE);
            body.setTextSize(16);
            body.setLineSpacing(6f, 1f);
            body.setTypeface(font);
            root.addView(body);
        }

        ScrollView scroller = new ScrollView(this);
        scroller.setBackgroundColor(Color.BLACK);
        scroller.addView(root, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        UiKit.screen(this, "To-do guide", scroller);
    }
}

package com.plainphone.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class WebTargetsActivity extends Activity {

    private ScrollView scroller;
    private LinearLayout root;
    private LinearLayout list;
    private Typeface georgia;

    private List<WebTarget> working = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        georgia = Fonts.current(this);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);

        scroller = new ScrollView(this);
        scroller.setBackgroundColor(Color.BLACK);
        scroller.addView(root, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        UiKit.screen(this, "Web shortcuts", scroller);

        render();
    }

    private void render() {
        root.removeAllViews();

        TextView hint = new TextView(this);
        hint.setText("Use " + WebTarget.WORD + " for your searched text.\n"
                + "Tap a search to edit it.\n"
                + "Hold and drag to reorder.");
        hint.setTextColor(Color.GRAY);
        hint.setTextSize(14);
        hint.setTypeface(georgia);
        hint.setPadding(48, 40, 48, 24);
        root.addView(hint);

        working = Config.getWebTargets(this);

        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setOnDragListener(this::onRowDrag);
        root.addView(list, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        for (WebTarget target : working) {
            list.addView(targetRow(target));
        }

        if (working.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No web searches yet.");
            empty.setTextColor(Color.GRAY);
            empty.setTextSize(16);
            empty.setTypeface(georgia);
            empty.setPadding(48, 24, 48, 24);
            root.addView(empty);
        }

        root.addView(row("Add a web search", null, v -> showEditor(null)));
    }

    private void showEditor(WebTarget existing) {
        boolean editing = existing != null;

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(UiKit.dialogBackground());
        card.setPadding(56, 48, 56, 40);

        TextView title = new TextView(this);
        title.setText(editing ? "Edit" : "Add");
        title.setTextColor(Color.WHITE);
        title.setTextSize(20);
        title.setTypeface(Typeface.create(georgia, Typeface.BOLD));

        Button close = new Button(this);
        if (editing) {
            LinearLayout header = new LinearLayout(this);
            header.setOrientation(LinearLayout.HORIZONTAL);
            header.setGravity(Gravity.CENTER_VERTICAL);
            header.addView(title, new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            close.setText("×");
            close.setTextSize(24);
            close.setTextColor(Color.WHITE);
            close.setTypeface(georgia);
            close.setAllCaps(false);
            close.setPadding(28, 8, 28, 8);
            close.setBackgroundColor(Color.TRANSPARENT);
            header.addView(close, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

            card.addView(header);
        } else {
            card.addView(title);
        }

        EditText name = new EditText(this);
        name.setHint("Name");
        name.setHintTextColor(Color.GRAY);
        name.setSingleLine(true);
        UiKit.style(this, name);
        if (editing) name.setText(existing.name);
        card.addView(name, topMargin(28));

        EditText url = new EditText(this);
        url.setHint("https://example.com/search?q=" + WebTarget.WORD);
        url.setHintTextColor(Color.GRAY);
        url.setSingleLine(true);
        url.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
        UiKit.style(this, url);
        if (editing) url.setText(existing.url);
        card.addView(url, topMargin(16));

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setGravity(Gravity.END);

        Button remove = new Button(this);
        remove.setText("Remove");
        UiKit.style(this, remove);
        remove.setTypeface(georgia);

        Button cancel = new Button(this);
        cancel.setText("Cancel");
        UiKit.style(this, cancel);
        cancel.setTypeface(georgia);

        if (editing) {
            LinearLayout.LayoutParams removeParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            removeParams.rightMargin = 20;
            remove.setLayoutParams(removeParams);
            buttons.addView(remove);
        } else {
            buttons.addView(cancel);
        }

        Button save = new Button(this);
        save.setText("Save");
        UiKit.style(this, save);
        save.setTypeface(georgia);
        LinearLayout.LayoutParams saveParams;
        if (editing) {
            saveParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        } else {
            saveParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            saveParams.leftMargin = 20;
        }
        buttons.addView(save, saveParams);

        card.addView(buttons, topMargin(32));

        AlertDialog dialog = new AlertDialog.Builder(this).setView(card).create();
        UiKit.clearDialogChrome(dialog);

        close.setOnClickListener(v -> dialog.dismiss());
        cancel.setOnClickListener(v -> dialog.dismiss());
        remove.setOnClickListener(v -> {
            dialog.dismiss();
            confirmDelete(existing);
        });
        save.setOnClickListener(v -> {
            String urlText = url.getText().toString().trim();
            if (!WebTarget.hasPlaceholder(urlText)) {
                Toast.makeText(this, "The URL needs " + WebTarget.WORD + " where the words go",
                        Toast.LENGTH_LONG).show();
                return;
            }
            String nameText = name.getText().toString().trim();
            List<WebTarget> targets = Config.getWebTargets(this);
            if (editing) {
                WebTarget match = WebTarget.findById(targets, existing.id);
                if (match != null) {
                    match.name = nameText;
                    match.url = urlText;
                }
            } else {
                targets.add(WebTarget.create(nameText, urlText));
            }
            Config.setWebTargets(this, targets);
            dialog.dismiss();
            render();
        });
        dialog.show();
    }

    private void confirmDelete(WebTarget target) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(UiKit.dialogBackground());
        card.setPadding(56, 48, 56, 40);

        TextView title = new TextView(this);
        title.setText("Remove " + WebTarget.nameOrHost(target.name, target.url) + "?");
        title.setTextColor(Color.WHITE);
        title.setTextSize(18);
        title.setTypeface(georgia);
        card.addView(title);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setGravity(Gravity.END);

        Button cancel = new Button(this);
        cancel.setText("Cancel");
        UiKit.style(this, cancel);
        buttons.addView(cancel);

        Button remove = new Button(this);
        remove.setText("Remove");
        UiKit.style(this, remove);
        LinearLayout.LayoutParams removeParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        removeParams.leftMargin = 20;
        buttons.addView(remove, removeParams);

        card.addView(buttons, topMargin(28));

        AlertDialog dialog = new AlertDialog.Builder(this).setView(card).create();
        UiKit.clearDialogChrome(dialog);

        cancel.setOnClickListener(v -> dialog.dismiss());
        remove.setOnClickListener(v -> {
            List<WebTarget> targets = Config.getWebTargets(this);
            WebTarget existing = WebTarget.findById(targets, target.id);
            if (existing != null) targets.remove(existing);
            Config.setWebTargets(this, targets);
            dialog.dismiss();
            render();
        });
        dialog.show();
    }

    private boolean onRowDrag(View view, DragEvent event) {
        if (!(event.getLocalState() instanceof View)) return false;
        View dragged = (View) event.getLocalState();

        switch (event.getAction()) {
            case DragEvent.ACTION_DRAG_STARTED:
            case DragEvent.ACTION_DRAG_ENTERED:
                return true;
            case DragEvent.ACTION_DRAG_LOCATION: {
                int from = list.indexOfChild(dragged);
                if (from < 0) return true;
                int to = insertionIndex(event.getY());
                if (to != from) {
                    list.removeViewAt(from);
                    working.add(to, working.remove(from));
                    list.addView(dragged, to);
                }
                autoScroll(event.getY());
                return true;
            }
            case DragEvent.ACTION_DROP:
                return true;
            case DragEvent.ACTION_DRAG_ENDED:
                dragged.setVisibility(View.VISIBLE);
                Config.setWebTargets(this, working);
                render();
                return true;
            default:
                return false;
        }
    }

    private int insertionIndex(float y) {
        for (int i = 0; i < list.getChildCount(); i++) {
            View child = list.getChildAt(i);
            if (y < child.getTop() + child.getHeight() / 2f) return i;
        }
        return Math.max(0, list.getChildCount() - 1);
    }

    private void autoScroll(float yInList) {
        float yInScroller = yInList + list.getTop() - scroller.getScrollY();
        int edge = scroller.getHeight() / 8;
        if (yInScroller < edge) {
            scroller.smoothScrollBy(0, -edge);
        } else if (yInScroller > scroller.getHeight() - edge) {
            scroller.smoothScrollBy(0, edge);
        }
    }

    private LinearLayout.LayoutParams topMargin(int px) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = px;
        return params;
    }

    private View targetRow(WebTarget target) {
        View view = row(WebTarget.nameOrHost(target.name, target.url), target.url,
                v -> showEditor(target));
        view.setOnLongClickListener(v -> {
            v.startDragAndDrop(null, new View.DragShadowBuilder(v), v, 0);
            v.setVisibility(View.INVISIBLE);
            return true;
        });
        return view;
    }

    private View row(String label, String subtitle, View.OnClickListener listener) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(48, 32, 48, 32);

        StateListDrawable background = new StateListDrawable();
        background.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Color.DKGRAY));
        background.addState(new int[]{}, new ColorDrawable(Color.BLACK));
        row.setBackground(background);
        row.setOnClickListener(listener);

        TextView title = new TextView(this);
        title.setText(label);
        title.setTextColor(Color.WHITE);
        title.setTextSize(20);
        title.setGravity(Gravity.START);
        title.setTypeface(georgia);
        row.addView(title);

        if (subtitle != null) {
            TextView detail = new TextView(this);
            detail.setText(subtitle);
            detail.setTextColor(Color.GRAY);
            detail.setTextSize(13);
            detail.setSingleLine(true);
            detail.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
            detail.setTypeface(georgia);
            row.addView(detail);
        }
        return row;
    }
}


package com.plainphone.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/**
 * Manage the user's own web searches — a name and a URL template such as
 * "https://de.wiktionary.org/wiki/{word}". Each saved entry becomes a row under the Web
 * heading in home-screen search, sending the typed words straight to that site.
 */
public class WebTargetsActivity extends Activity {

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
        scroller.addView(root, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        setContentView(scroller);

        render();
    }

    private void render() {
        root.removeAllViews();

        TextView hint = new TextView(this);
        hint.setText("Searches you add appear under Web on the home screen.\n"
                + "Use " + WebTarget.WORD + " where the typed words should go.");
        hint.setTextColor(Color.GRAY);
        hint.setTextSize(14);
        hint.setTypeface(georgia);
        hint.setPadding(48, 40, 48, 24);
        root.addView(hint);

        List<WebTarget> targets = Config.getWebTargets(this);
        for (WebTarget target : targets) {
            root.addView(row(WebTarget.nameOrHost(target.name, target.url), target.url,
                    v -> confirmDelete(target)));
        }

        if (targets.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No web searches yet.");
            empty.setTextColor(Color.GRAY);
            empty.setTextSize(16);
            empty.setTypeface(georgia);
            empty.setPadding(48, 24, 48, 24);
            root.addView(empty);
        }

        root.addView(row("Add a web search", null, v -> showEditor()));
    }

    /**
     * One dialog with both fields, rather than the stepper flow the other settings use: a
     * URL template is only meaningful alongside its name, and splitting them across screens
     * would mean typing a long address with no idea what it's for.
     *
     * <p>Built entirely from scratch (title, fields, buttons) rather than through
     * AlertDialog.Builder's setTitle/setPositiveButton — those render with the system's
     * own dialog theme, which looks foreign against the rest of the app's plain black-and
     * white styling. This is the same card treatment MainActivity's app-options popup uses.
     */
    private void showEditor() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(UiKit.dialogBackground());
        card.setPadding(56, 48, 56, 40);

        TextView title = new TextView(this);
        title.setText("Add a web search");
        title.setTextColor(Color.WHITE);
        title.setTextSize(20);
        title.setTypeface(Typeface.create(georgia, Typeface.BOLD));
        card.addView(title);

        TextView hint = new TextView(this);
        hint.setText("Use " + WebTarget.WORD + " where the typed words should go.");
        hint.setTextColor(Color.GRAY);
        hint.setTextSize(13);
        hint.setTypeface(georgia);
        LinearLayout.LayoutParams hintParams = topMargin(8);
        card.addView(hint, hintParams);

        EditText name = new EditText(this);
        name.setHint("Name");
        name.setHintTextColor(Color.GRAY);
        name.setSingleLine(true);
        UiKit.style(this, name);
        card.addView(name, topMargin(28));

        EditText url = new EditText(this);
        url.setHint("https://example.com/search?q=" + WebTarget.WORD);
        url.setHintTextColor(Color.GRAY);
        url.setSingleLine(true);
        url.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
        UiKit.style(this, url);
        card.addView(url, topMargin(16));

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setGravity(Gravity.END);

        Button cancel = new Button(this);
        cancel.setText("Cancel");
        UiKit.style(this, cancel);
        buttons.addView(cancel);

        Button save = new Button(this);
        save.setText("Save");
        UiKit.style(this, save);
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        saveParams.leftMargin = 20;
        buttons.addView(save, saveParams);

        card.addView(buttons, topMargin(32));

        AlertDialog dialog = new AlertDialog.Builder(this).setView(card).create();
        UiKit.clearDialogChrome(dialog);

        cancel.setOnClickListener(v -> dialog.dismiss());
        save.setOnClickListener(v -> {
            String urlText = url.getText().toString().trim();
            if (!WebTarget.hasPlaceholder(urlText)) {
                // Without the placeholder the query has nowhere to go, so saving it would
                // produce a row that opens the same page whatever was typed.
                Toast.makeText(this, "The URL needs " + WebTarget.WORD + " where the words go",
                        Toast.LENGTH_LONG).show();
                return;
            }
            List<WebTarget> targets = Config.getWebTargets(this);
            targets.add(WebTarget.create(name.getText().toString().trim(), urlText));
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

    private LinearLayout.LayoutParams topMargin(int px) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = px;
        return params;
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

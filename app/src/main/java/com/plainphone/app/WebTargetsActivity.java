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
     */
    private void showEditor() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setBackgroundColor(Color.BLACK);
        form.setPadding(48, 40, 48, 24);

        EditText name = new EditText(this);
        name.setHint("Name (e.g. Wiktionary DE)");
        name.setHintTextColor(Color.GRAY);
        name.setSingleLine(true);
        UiKit.style(this, name);
        form.addView(name);

        EditText url = new EditText(this);
        url.setHint("https://example.com/search?q=" + WebTarget.WORD);
        url.setHintTextColor(Color.GRAY);
        url.setSingleLine(true);
        url.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
        UiKit.style(this, url);
        LinearLayout.LayoutParams urlParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        urlParams.topMargin = 24;
        form.addView(url, urlParams);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(form)
                .setPositiveButton("Save", null) // wired below so validation can keep it open
                .setNegativeButton("Cancel", null)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));
        }

        dialog.setOnShowListener(shown -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String urlText = url.getText().toString().trim();
                    if (!WebTarget.hasPlaceholder(urlText)) {
                        // Without the placeholder the query has nowhere to go, so saving it
                        // would produce a row that opens the same page whatever was typed.
                        Toast.makeText(this, "The URL needs " + WebTarget.WORD
                                + " where the words go", Toast.LENGTH_LONG).show();
                        return;
                    }
                    List<WebTarget> targets = Config.getWebTargets(this);
                    targets.add(WebTarget.create(name.getText().toString().trim(), urlText));
                    Config.setWebTargets(this, targets);
                    dialog.dismiss();
                    render();
                }));
        dialog.show();
    }

    private void confirmDelete(WebTarget target) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Remove " + WebTarget.nameOrHost(target.name, target.url) + "?")
                .setPositiveButton("Remove", (d, which) -> {
                    List<WebTarget> targets = Config.getWebTargets(this);
                    WebTarget existing = WebTarget.findById(targets, target.id);
                    if (existing != null) targets.remove(existing);
                    Config.setWebTargets(this, targets);
                    render();
                })
                .setNegativeButton("Cancel", null)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));
        }
        dialog.show();
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

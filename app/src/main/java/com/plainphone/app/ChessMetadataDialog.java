package com.plainphone.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.LinkedHashMap;
import java.util.Map;

/** The "White / Black / Event / Round+Date / ECO / Result" form shared by the Library's
 *  per-game "Edit metadata" and the board's Save sheet — editing a game's tags without
 *  touching its moves. */
final class ChessMetadataDialog {
    private ChessMetadataDialog() {}

    interface OnSave {
        void run(Map<String, String> tags, String result);
    }

    /** {@code onDismiss}, if given, fires however the dialog closes (Save or Cancel) — for a
     *  caller that needs to reopen whatever was behind it either way, same as
     *  {@link UiKit#textPrompt}'s own {@code onDismiss}. */
    static void show(Activity host, String white, String black, String event, String round,
                     String date, String eco, String result, OnSave onSave, Runnable onDismiss) {
        LinearLayout root = new LinearLayout(host);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(UiKit.dialogBackground(host));
        UiKit.clipRounded(host, root, UiKit.R_MD);
        root.setPadding(2, 32, 2, UiKit.dp(host, UiKit.R_MD));

        FrameLayout scrim = UiKit.wrapScrim(host, root, 0.85f);
        AlertDialog dialog = new AlertDialog.Builder(host, R.style.Theme_PlainPhone_RoundedDialog)
                .setView(scrim).create();

        LinearLayout headerRow = new LinearLayout(host);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        headerRow.setPadding(48, 20, 48, 20);
        TextView back = text(host, "←", 22, Color.WHITE);
        back.setOnClickListener(v -> {
            dialog.dismiss();
            if (onDismiss != null) onDismiss.run();
        });
        headerRow.addView(back);
        TextView title = text(host, "Edit", 17, Color.WHITE);
        title.setTypeface(Fonts.current(host), android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleLp.leftMargin = UiKit.dp(host, 12);
        headerRow.addView(title, titleLp);
        View spacer = new View(host);
        headerRow.addView(spacer, new LinearLayout.LayoutParams(0, 0, 1f));
        TextView save = text(host, "Save", 16, Color.WHITE);
        save.setTypeface(Fonts.current(host), android.graphics.Typeface.BOLD);
        headerRow.addView(save);
        root.addView(headerRow);

        LinearLayout fields = new LinearLayout(host);
        fields.setOrientation(LinearLayout.VERTICAL);

        EditText whiteInput = field(host, fields, "WHITE", white, "White player");
        EditText blackInput = field(host, fields, "BLACK", black, "Black player");
        EditText eventInput = field(host, fields, "EVENT", event, "Event name");

        LinearLayout roundDateRow = new LinearLayout(host);
        roundDateRow.setOrientation(LinearLayout.HORIZONTAL);
        roundDateRow.setPadding(UiKit.dp(host, 20), UiKit.dp(host, 14), UiKit.dp(host, 20), 0);
        LinearLayout roundCol = new LinearLayout(host);
        roundCol.setOrientation(LinearLayout.VERTICAL);
        EditText roundInput = fieldInto(host, roundCol, "ROUND", round, "1");
        LinearLayout dateCol = new LinearLayout(host);
        dateCol.setOrientation(LinearLayout.VERTICAL);
        EditText dateInput = fieldInto(host, dateCol, "DATE", date, "YYYY.MM.DD");
        LinearLayout.LayoutParams roundLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        roundDateRow.addView(roundCol, roundLp);
        LinearLayout.LayoutParams dateLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        dateLp.leftMargin = UiKit.dp(host, 16);
        roundDateRow.addView(dateCol, dateLp);
        fields.addView(roundDateRow);

        EditText ecoInput = field(host, fields, "ECO", eco, "e.g. B90");

        String[] resultValues = {"1-0", "0-1", "1/2-1/2", "*"};
        // Same literal "1/2-1/2" the moves grid/result row already show — no separate ½-½
        // unicode shorthand here.
        String[] resultLabels = {"1-0", "0-1", "1/2-1/2", "*"};
        String[] selectedResult = {(result == null || result.isEmpty()) ? "*" : result};
        LinearLayout resultWrap = new LinearLayout(host);
        resultWrap.setOrientation(LinearLayout.VERTICAL);
        resultWrap.setPadding(UiKit.dp(host, 20), UiKit.dp(host, 14), UiKit.dp(host, 20), UiKit.dp(host, 20));
        resultWrap.addView(text(host, "RESULT", 11, 0xFF6E6E6E));
        LinearLayout resultRow = new LinearLayout(host);
        resultRow.setOrientation(LinearLayout.HORIZONTAL);
        resultRow.setPadding(0, UiKit.dp(host, 6), 0, 0);
        TextView[] resultButtons = new TextView[resultValues.length];
        Runnable[] renderResults = new Runnable[1];
        for (int i = 0; i < resultValues.length; i++) {
            TextView btn = text(host, resultLabels[i], 13, Color.WHITE);
            btn.setGravity(Gravity.CENTER);
            btn.setPadding(0, UiKit.dp(host, 8), 0, UiKit.dp(host, 8));
            final String value = resultValues[i];
            btn.setOnClickListener(v -> { selectedResult[0] = value; renderResults[0].run(); });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            if (i > 0) lp.leftMargin = UiKit.dp(host, 6);
            resultRow.addView(btn, lp);
            resultButtons[i] = btn;
        }
        renderResults[0] = () -> {
            for (int i = 0; i < resultValues.length; i++) {
                boolean on = resultValues[i].equals(selectedResult[0]);
                resultButtons[i].setBackground(on
                        ? UiKit.rounded(host, Color.WHITE, 0, 0f, UiKit.R_SM)
                        : UiKit.rounded(host, Color.BLACK, 0xFF2C2C2C, 2f, UiKit.R_SM));
                resultButtons[i].setTextColor(on ? Color.BLACK : Color.WHITE);
            }
        };
        renderResults[0].run();
        resultWrap.addView(resultRow);
        fields.addView(resultWrap);

        // A fixed-height ScrollView always claims that full height even when the fields
        // themselves are shorter — an AT_MOST measure spec (capped, not fixed) only kicks in
        // once the content actually exceeds it, same as VaultUi's own BoundedScrollView.
        int maxScrollerPx = (int) (host.getResources().getDisplayMetrics().heightPixels * 0.55f);
        ScrollView scroller = new ScrollView(host) {
            @Override protected void onMeasure(int widthSpec, int heightSpec) {
                super.onMeasure(widthSpec, MeasureSpec.makeMeasureSpec(maxScrollerPx, MeasureSpec.AT_MOST));
            }
        };
        scroller.addView(fields, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(scroller, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        save.setOnClickListener(v -> {
            Map<String, String> tags = new LinkedHashMap<>();
            tags.put("White", whiteInput.getText().toString().trim());
            tags.put("Black", blackInput.getText().toString().trim());
            tags.put("Event", eventInput.getText().toString().trim());
            tags.put("Round", roundInput.getText().toString().trim());
            tags.put("Date", dateInput.getText().toString().trim());
            tags.put("ECO", ecoInput.getText().toString().trim());
            onSave.run(tags, selectedResult[0]);
            dialog.dismiss();
            if (onDismiss != null) onDismiss.run();
        });
        // onDismiss fires from ← and Save explicitly above, not from a generic
        // setOnDismissListener — tapping the scrim outside the box also dismisses (finishCentered's
        // own default), but that should just close everything, not reopen whatever was behind
        // it the way going "back" does.

        UiKit.finishCentered(dialog, scrim);
    }

    private static EditText field(Activity host, LinearLayout parent, String label, String value, String hint) {
        LinearLayout wrap = new LinearLayout(host);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setPadding(UiKit.dp(host, 20), UiKit.dp(host, 14), UiKit.dp(host, 20), 0);
        EditText input = fieldInto(host, wrap, label, value, hint);
        parent.addView(wrap);
        return input;
    }

    private static EditText fieldInto(Activity host, LinearLayout wrap, String label, String value, String hint) {
        wrap.addView(text(host, label, 11, 0xFF6E6E6E));
        EditText input = new EditText(host);
        input.setText(value == null ? "" : value);
        input.setHint(hint);
        input.setHintTextColor(0xFF5A5A5A);
        input.setBackground(null);
        input.setTextColor(Color.WHITE);
        input.setTypeface(Fonts.current(host));
        input.setTextSize(16);
        input.setSingleLine(true);
        input.setPadding(0, UiKit.dp(host, 4), 0, 0);
        wrap.addView(input);
        return input;
    }

    private static TextView text(Activity host, String value, float size, int color) {
        TextView v = new TextView(host);
        v.setText(value);
        v.setTextSize(size);
        v.setTextColor(color);
        v.setTypeface(Fonts.current(host));
        v.setIncludeFontPadding(false);
        return v;
    }
}

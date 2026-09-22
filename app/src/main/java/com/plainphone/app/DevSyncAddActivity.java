package com.plainphone.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * A short wizard for one new folder pair — deliberately several full screens,
 * not one crowded dialog, so direction, change detection, and (for a mirror
 * pair) the delete-propagation choice each get their own explained step
 * rather than a buried default. See the approved mockup for the exact shape
 * this follows.
 */
public class DevSyncAddActivity extends Activity {

    private static final int REQ_PICK_FOLDER = 9401;

    private String hostId;
    private Typeface font;

    // wizard state
    private Uri localTree;
    private String remotePath;
    private String direction = DevSyncPair.DIR_PUSH;
    private String detectMode = DevSyncPair.DETECT_MTIME;
    private String deleteMode = DevSyncPair.DELETE_OFF;
    private int scheduleMinutes = DevSyncPair.SCHEDULE_MANUAL;

    private int step = 0; // 0 = pick local, 1 = remote path, 2 = direction+detect, 3 = delete (mirror only), 4 = schedule

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hostId = getIntent().getStringExtra(DevHostActivity.EXTRA_HOST_ID);
        if (hostId == null) { finish(); return; }
        font = Fonts.current(this);
        pickLocalFolder();
    }

    private void pickLocalFolder() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        startActivityForResult(intent, REQ_PICK_FOLDER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_PICK_FOLDER) return;
        if (resultCode != RESULT_OK || data == null || data.getData() == null) {
            finish();
            return;
        }
        Uri tree = data.getData();
        getContentResolver().takePersistableUriPermission(tree,
                Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        localTree = tree;
        step = 1;
        renderRemotePath();
    }

    // --- step: remote path --------------------------------------------------

    private void renderRemotePath() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(48, 24, 48, 24);

        root.addView(sectionTitle("Remote folder"));
        root.addView(sectionSub("Absolute path on the PC — e.g. /Users/you/Design/assets"));

        android.widget.EditText input = new android.widget.EditText(this);
        UiKit.style(this, input);
        input.setSingleLine(true);
        input.setHint("/path/on/pc");
        input.setHintTextColor(0xFF555555);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ip.topMargin = UiKit.dp(this, 18);
        root.addView(input, ip);

        View spacer = new View(this);
        root.addView(spacer, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        root.addView(continueButton(() -> {
            String v = input.getText().toString().trim();
            if (v.isEmpty()) return;
            remotePath = v;
            step = 2;
            renderDirectionAndDetect();
        }));

        ScrollView scroller = new ScrollView(this);
        scroller.addView(root, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        UiKit.screen(this, "New sync pair", scroller);
    }

    // --- step: direction + detect mode --------------------------------------

    private void renderDirectionAndDetect() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(48, 24, 48, 24);

        root.addView(stepLabel("STEP 3 OF " + (direction.equals(DevSyncPair.DIR_MIRROR) ? "5" : "4")));
        root.addView(sectionTitle("Direction"));
        root.addView(sectionSub("Which way should this pair copy files?"));

        LinearLayout dirGroup = new LinearLayout(this);
        dirGroup.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams dgp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dgp.topMargin = UiKit.dp(this, 14);
        dgp.bottomMargin = UiKit.dp(this, 24);
        root.addView(dirGroup, dgp);

        String[][] dirs = {
                {DevSyncPair.DIR_PUSH, "Push", "Phone → PC only"},
                {DevSyncPair.DIR_PULL, "Pull", "PC → Phone only"},
                {DevSyncPair.DIR_MIRROR, "Mirror", "Both ways, newer file wins"},
        };
        for (String[] d : dirs) {
            dirGroup.addView(choiceRow(d[1], d[2], direction.equals(d[0]), v -> {
                direction = d[0];
                renderDirectionAndDetect();
            }));
        }

        root.addView(sectionTitle("Change detection"));
        root.addView(sectionSub("How to tell a file needs syncing."));

        LinearLayout detectGroup = new LinearLayout(this);
        detectGroup.setOrientation(LinearLayout.VERTICAL);
        detectGroup.setBackground(UiKit.rounded(this, Color.BLACK, 0xFF2C2C2C, 2f, UiKit.R_MD));
        UiKit.clipRounded(this, detectGroup, UiKit.R_MD);
        LinearLayout.LayoutParams detp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        detp.topMargin = UiKit.dp(this, 14);
        root.addView(detectGroup, detp);

        String[][] modes = {
                {DevSyncPair.DETECT_MTIME, "Modified time", "Fast. Compares last-changed timestamps."},
                {DevSyncPair.DETECT_SIZE, "Size", "Fastest. Only checks byte length."},
                {DevSyncPair.DETECT_CHECKSUM, "Checksum (content)", "Slower, exact — like rsync --checksum."},
        };
        for (int i = 0; i < modes.length; i++) {
            String[] m = modes[i];
            TextView row = flatChoiceRow(m[1], m[2], detectMode.equals(m[0]));
            row.setOnClickListener(v -> {
                detectMode = m[0];
                renderDirectionAndDetect();
            });
            detectGroup.addView(row);
            if (i < modes.length - 1) detectGroup.addView(hairline());
        }

        View spacer = new View(this);
        root.addView(spacer, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        root.addView(continueButton(() -> {
            step = direction.equals(DevSyncPair.DIR_MIRROR) ? 3 : 4;
            if (step == 3) renderMirrorCleanup(); else renderSchedule();
        }));

        ScrollView scroller = new ScrollView(this);
        scroller.addView(root, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        UiKit.screen(this, "New sync pair", scroller);
    }

    // --- step: mirror cleanup (delete propagation) --------------------------

    private void renderMirrorCleanup() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(48, 24, 48, 24);

        root.addView(stepLabel("STEP 4 OF 5 · MIRROR ONLY"));
        root.addView(sectionTitle("Mirror cleanup"));
        root.addView(sectionSub("This pair mirrors both ways."));

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setBackground(UiKit.rounded(this, Color.BLACK, 0xFF2C2C2C, 2f, UiKit.R_MD));
        card.setPadding(UiKit.dp(this, 18), UiKit.dp(this, 18), UiKit.dp(this, 18), UiKit.dp(this, 18));
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cp.topMargin = UiKit.dp(this, 16);
        root.addView(card, cp);

        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);
        TextView title = new TextView(this);
        title.setText("Delete files removed on the other side");
        title.setTextColor(Color.WHITE);
        title.setTextSize(15);
        title.setTypeface(font);
        textCol.addView(title);
        TextView sub = new TextView(this);
        sub.setText("When a file is deleted on the phone or the PC, remove it from the other side too on the next sync.");
        sub.setTextColor(0xFF888888);
        sub.setTextSize(12.5f);
        sub.setPadding(0, UiKit.dp(this, 4), 0, 0);
        sub.setTypeface(font);
        textCol.addView(sub);
        card.addView(textCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView toggle = new TextView(this);
        toggleText(toggle, deleteMode.equals(DevSyncPair.DELETE_PROPAGATE));
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tp.leftMargin = UiKit.dp(this, 14);
        card.addView(toggle, tp);
        card.setOnClickListener(v -> {
            deleteMode = deleteMode.equals(DevSyncPair.DELETE_PROPAGATE)
                    ? DevSyncPair.DELETE_OFF : DevSyncPair.DELETE_PROPAGATE;
            toggleText(toggle, deleteMode.equals(DevSyncPair.DELETE_PROPAGATE));
        });

        LinearLayout warn = new LinearLayout(this);
        warn.setOrientation(LinearLayout.VERTICAL);
        warn.setBackground(UiKit.rounded(this, 0xFF1C1414, 0xFF3A2A28, 2f, UiKit.R_MD));
        warn.setPadding(UiKit.dp(this, 16), UiKit.dp(this, 16), UiKit.dp(this, 16), UiKit.dp(this, 16));
        LinearLayout.LayoutParams wp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        wp.topMargin = UiKit.dp(this, 16);
        root.addView(warn, wp);
        TextView warnText = new TextView(this);
        warnText.setText("Off (default): deletions never propagate — a file only ever gets added or updated on the other side. On: a deletion is permanent on both devices. You can change this later from the pair's settings.");
        warnText.setTextColor(0xFFC88F87);
        warnText.setTextSize(12.5f);
        warnText.setTypeface(font);
        warn.addView(warnText);

        View spacer = new View(this);
        root.addView(spacer, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        root.addView(continueButton(() -> {
            step = 4;
            renderSchedule();
        }));

        ScrollView scroller = new ScrollView(this);
        scroller.addView(root, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        UiKit.screen(this, "New sync pair", scroller);
    }

    private void toggleText(TextView toggle, boolean on) {
        toggle.setText(on ? "On" : "Off");
        toggle.setTypeface(font);
        toggle.setTextSize(13);
        toggle.setTextColor(on ? Color.BLACK : Color.WHITE);
        toggle.setBackground(UiKit.rounded(this, on ? Color.WHITE : 0xFF2C2C2C, 0, 0, UiKit.R_PILL));
        toggle.setPadding(UiKit.dp(this, 14), UiKit.dp(this, 8), UiKit.dp(this, 14), UiKit.dp(this, 8));
    }

    // --- step: schedule ------------------------------------------------------

    private void renderSchedule() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(48, 24, 48, 24);

        root.addView(sectionTitle("Schedule"));
        root.addView(sectionSub("Run this pair by hand, or keep it ticking on its own."));

        LinearLayout group = new LinearLayout(this);
        group.setOrientation(LinearLayout.VERTICAL);
        group.setBackground(UiKit.rounded(this, Color.BLACK, 0xFF2C2C2C, 2f, UiKit.R_MD));
        UiKit.clipRounded(this, group, UiKit.R_MD);
        LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        gp.topMargin = UiKit.dp(this, 14);
        root.addView(group, gp);

        int[][] options = {{0, 0}, {1, 15}, {2, 30}, {3, 60}};
        String[] labels = {"Manual only", "Every 15 minutes", "Every 30 minutes", "Every hour"};
        for (int i = 0; i < options.length; i++) {
            int minutes = options[i][1];
            TextView row = flatChoiceRow(labels[i], null, scheduleMinutes == minutes);
            row.setOnClickListener(v -> {
                scheduleMinutes = minutes;
                renderSchedule();
            });
            group.addView(row);
            if (i < options.length - 1) group.addView(hairline());
        }

        View spacer = new View(this);
        root.addView(spacer, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        root.addView(continueButton(this::promptLabelAndSave));

        ScrollView scroller = new ScrollView(this);
        scroller.addView(root, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        UiKit.screen(this, "New sync pair", scroller);
    }

    private void promptLabelAndSave() {
        String suggested = suggestLabel();
        UiKit.textPrompt(this, "Name this pair", suggested, "Save", label -> {
            DevSyncPair pair = new DevSyncPair(DevSyncPair.newId());
            pair.hostId = hostId;
            pair.label = label;
            pair.localTreeUri = localTree.toString();
            pair.remotePath = remotePath;
            pair.direction = direction;
            pair.detectMode = detectMode;
            pair.deleteMode = deleteMode;
            pair.scheduleMinutes = scheduleMinutes;
            pair.save(this);
            if (scheduleMinutes > DevSyncPair.SCHEDULE_MANUAL) DevSyncScheduler.schedule(this);
            finish();
        });
    }

    private String suggestLabel() {
        String remoteLeaf = remotePath;
        int slash = Math.max(remoteLeaf.lastIndexOf('/'), remoteLeaf.lastIndexOf('\\'));
        if (slash >= 0 && slash < remoteLeaf.length() - 1) remoteLeaf = remoteLeaf.substring(slash + 1);
        String localLeaf = DocumentsContract.getTreeDocumentId(localTree);
        int colon = localLeaf.lastIndexOf(':');
        if (colon >= 0 && colon < localLeaf.length() - 1) localLeaf = localLeaf.substring(colon + 1);
        if (direction.equals(DevSyncPair.DIR_PUSH)) return localLeaf + " → " + remoteLeaf;
        if (direction.equals(DevSyncPair.DIR_PULL)) return remoteLeaf + " → " + localLeaf;
        return localLeaf + " ⇄ " + remoteLeaf;
    }

    // --- small view builders --------------------------------------------------

    private TextView stepLabel(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextColor(0xFF666666);
        t.setTextSize(12.5f);
        t.setLetterSpacing(0.05f);
        t.setTypeface(font);
        return t;
    }

    private TextView sectionTitle(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextColor(Color.WHITE);
        t.setTextSize(22);
        t.setTypeface(font, Typeface.BOLD);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = UiKit.dp(this, 6);
        t.setLayoutParams(p);
        return t;
    }

    private TextView sectionSub(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextColor(0xFF888888);
        t.setTextSize(13);
        t.setTypeface(font);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = UiKit.dp(this, 4);
        t.setLayoutParams(p);
        return t;
    }

    /** A bordered, selectable card row (direction options) — its own border turns white
     *  when selected, matching the mockup's radio-card look. */
    private View choiceRow(String title, String sub, boolean selected, View.OnClickListener tap) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(UiKit.rounded(this, Color.BLACK, selected ? Color.WHITE : 0xFF2C2C2C, 3f, UiKit.R_MD));
        row.setPadding(UiKit.dp(this, 16), UiKit.dp(this, 16), UiKit.dp(this, 16), UiKit.dp(this, 16));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = UiKit.dp(this, 8);
        row.setLayoutParams(p);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        TextView t = new TextView(this);
        t.setText(title);
        t.setTextColor(Color.WHITE);
        t.setTextSize(15);
        t.setTypeface(font);
        col.addView(t);
        TextView s = new TextView(this);
        s.setText(sub);
        s.setTextColor(0xFF999999);
        s.setTextSize(12);
        s.setTypeface(font);
        col.addView(s);
        row.addView(col, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView dot = new TextView(this);
        dot.setText(selected ? "●" : "○");
        dot.setTextColor(selected ? Color.WHITE : 0xFF444444);
        dot.setTextSize(16);
        row.addView(dot);

        row.setOnClickListener(tap);
        return row;
    }

    /** A flat radio row inside a bordered group (detect mode / schedule options). */
    private TextView flatChoiceRow(String title, String sub, boolean selected) {
        TextView row = new TextView(this);
        row.setTypeface(font);
        row.setTextColor(Color.WHITE);
        row.setTextSize(14.5f);
        row.setPadding(UiKit.dp(this, 16), UiKit.dp(this, 15), UiKit.dp(this, 16), UiKit.dp(this, 15));
        String prefix = selected ? "●  " : "○  ";
        if (sub != null) {
            android.text.SpannableStringBuilder sb = new android.text.SpannableStringBuilder();
            sb.append(prefix).append(title).append("\n");
            int subStart = sb.length();
            sb.append(sub);
            sb.setSpan(new android.text.style.ForegroundColorSpan(0xFF777777), subStart, sb.length(), 0);
            sb.setSpan(new android.text.style.RelativeSizeSpan(0.85f), subStart, sb.length(), 0);
            row.setText(sb);
        } else {
            row.setText(prefix + title);
        }
        StateListDrawable bg = new StateListDrawable();
        bg.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Color.DKGRAY));
        bg.addState(new int[]{}, new ColorDrawable(Color.BLACK));
        row.setBackground(bg);
        return row;
    }

    private View hairline() {
        View v = new View(this);
        v.setBackgroundColor(0xFF1C1C1C);
        v.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
        return v;
    }

    private View continueButton(Runnable onTap) {
        TextView button = new TextView(this);
        button.setText("Continue");
        button.setTextColor(Color.BLACK);
        button.setTextSize(15);
        button.setTypeface(font, Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        button.setBackground(UiKit.rounded(this, Color.WHITE, 0, 0, UiKit.R_MD));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, UiKit.dp(this, 50));
        p.topMargin = UiKit.dp(this, 24);
        p.bottomMargin = UiKit.dp(this, 12);
        button.setLayoutParams(p);
        button.setOnClickListener(v -> onTap.run());
        return button;
    }
}

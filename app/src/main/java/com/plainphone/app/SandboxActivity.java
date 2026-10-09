package com.plainphone.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.text.Editable;
import android.text.InputType;
import android.text.Spannable;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.Toast;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dev → Sandbox: write a program (C or JavaScript) and run it on the phone. Each language keeps its
 * own source between visits; runs go through {@link SandboxRunner}.
 */
public class SandboxActivity extends Activity {

    private static final float MIN_SP = 8, MAX_SP = 32;
    private static final int REQ_SAVE = 4501;
    private static final int REQ_MAN = 4502;

    private SandboxRunner.Lang lang = SandboxRunner.Lang.C;
    private EditText code;
    private Button langBtn;
    private Button lintBtn;
    private Button runBtn;
    private float codeSp = 13;
    /** True from the second finger down until the gesture ends; the editor gets no touches meanwhile. */
    private boolean pinching;
    private TextView output;
    private final Handler main = new Handler(Looper.getMainLooper());

    // ---- lint as you type ----
    private static final long AUTO_LINT_DELAY_MS = 800;
    /** {@code main.c:7: warning: msg}, {@code main.js:3:5: error: msg}, {@code main.py:3:1: msg}. */
    private static final Pattern PROBLEM = Pattern.compile("main\\.\\w+:(\\d+)(?::(\\d+))?:\\s*(?:(error|warning):\\s*)?(.*)");
    private static final int MARK_ERROR = 0x55E57373, MARK_WARNING = 0x44FFD27F;

    /** Background tint on a line with a problem; the only spans this class puts on the editor. */
    private static final class LintMark extends BackgroundColorSpan {
        LintMark(int color) { super(color); }
    }

    private boolean autoLint;
    private boolean linting;
    /** Bumped on every edit, so a lint result for older text is dropped. */
    private int editGen;
    /** True while the output pane shows lint results, which auto-lint may replace (never a program's output). */
    private boolean outputIsLint = true;
    private final Runnable autoLintTask = this::autoLint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);
        int pad = dp(12);
        root.setPadding(pad, pad, pad, pad);

        lang = SandboxRunner.Lang.valueOf(
                getSharedPreferences("sandbox", MODE_PRIVATE).getString("lang", SandboxRunner.Lang.C.name()));

        code = new EditText(this);
        code.setText(loadSource());
        // Multi-line, no suggestions/autocorrect, so typed code isn't rewritten by the keyboard.
        code.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        code.setTypeface(Typeface.MONOSPACE);
        code.setTextSize(TypedValue.COMPLEX_UNIT_SP, codeSp);
        code.setTextColor(0xFFEEEEEE);
        code.setHintTextColor(0xFF555555);
        GradientDrawable box = new GradientDrawable();
        box.setColor(0xFF2A2A2A);
        box.setCornerRadius(dp(14));
        code.setBackground(box);
        code.setGravity(Gravity.TOP | Gravity.START);
        code.setHorizontallyScrolling(true);
        code.setPadding(pad, pad, pad, pad);
        // Pinch to zoom the code; a non-pinch touch falls through to the editor as usual.
        ScaleGestureDetector pinch = new ScaleGestureDetector(this,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override public boolean onScale(ScaleGestureDetector d) {
                        float old = codeSp;
                        codeSp = Math.max(MIN_SP, Math.min(MAX_SP, codeSp * d.getScaleFactor()));
                        float ratio = codeSp / old;
                        // A new text size makes the editor re-scroll to the caret; keep the view
                        // where the pinch left it instead.
                        int sx = code.getScrollX(), sy = code.getScrollY();
                        code.setTextSize(TypedValue.COMPLEX_UNIT_SP, codeSp);
                        code.post(() -> code.scrollTo(Math.round(sx * ratio), Math.round(sy * ratio)));
                        return true;
                    }
                });
        // While two fingers are down, the editor gets no touches at all: otherwise it places the
        // caret on the first finger's lift and jumps. Single-finger touches go to the editor.
        code.setOnTouchListener((v, e) -> {
            int action = e.getActionMasked();
            if (e.getPointerCount() > 1) pinching = true;
            if (pinching) {
                pinch.onTouchEvent(e);
                if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) pinching = false;
                return true;
            }
            pinch.onTouchEvent(e);
            return pinch.isInProgress();
        });
        root.addView(code, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        autoLint = getSharedPreferences("sandbox", MODE_PRIVATE).getBoolean("autolint", true);
        code.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable e) {
                editGen++;
                scheduleLint();
            }
        });

        langBtn = flatButton(lang.label);
        langBtn.setOnClickListener(v -> PackUi.pickLanguage(this, lang, this::setLang));
        lintBtn = flatButton(autoLint ? "Lint ✓" : "Lint");
        lintBtn.setOnClickListener(v -> lint());
        lintBtn.setOnLongClickListener(v -> {
            autoLint = !autoLint;
            getSharedPreferences("sandbox", MODE_PRIVATE).edit().putBoolean("autolint", autoLint).apply();
            lintBtn.setText(autoLint ? "Lint ✓" : "Lint");
            Toast.makeText(this, autoLint ? "Lint as you type: on" : "Lint as you type: off", Toast.LENGTH_SHORT).show();
            if (autoLint) scheduleLint(); else clearMarks();
            return true;
        });
        Button manBtn = flatButton("Man");
        // The real pman on a pty when this build ships it; long-press for the older built-in reader.
        manBtn.setOnClickListener(v -> startActivityForResult(new Intent(this,
                PtyProcess.available(this) ? DocsShellActivity.class : TerminalActivity.class)
                .putExtra(TerminalActivity.EXTRA_TOPIC, lang.name().toLowerCase(java.util.Locale.US)), REQ_MAN));
        manBtn.setOnLongClickListener(v -> {
            startActivityForResult(new Intent(this, TerminalActivity.class)
                    .putExtra(TerminalActivity.EXTRA_TOPIC, lang.name().toLowerCase(java.util.Locale.US)), REQ_MAN);
            return true;
        });
        Button saveBtn = flatButton("Save");
        saveBtn.setOnClickListener(v -> pickSaveLocation());
        runBtn = flatButton("Run");
        runBtn.setOnClickListener(v -> run());

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.addView(langBtn);
        actions.addView(lintBtn);
        actions.addView(manBtn);
        actions.addView(saveBtn);
        actions.addView(runBtn, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        output = new TextView(this);
        output.setTypeface(Typeface.MONOSPACE);
        output.setTextSize(12);
        output.setTextColor(0xFFB5B5B5);
        output.setText("Output appears here.");
        ScrollView scroll = new ScrollView(this);
        scroll.addView(output);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        sp.topMargin = dp(10);
        root.addView(scroll, sp);

        UiKit.screen(this, "Sandbox", root, actions);
        scheduleLint();
    }

    /** Switch language: keep the current source, load the other language's. */
    private void setLang(SandboxRunner.Lang next) {
        saveSource(code.getText().toString());
        lang = next;
        getSharedPreferences("sandbox", MODE_PRIVATE).edit().putString("lang", lang.name()).apply();
        code.setText(loadSource());
        langBtn.setText(lang.label);
        output.setText("Output appears here.");
    }

    /** System "save as" sheet: the user picks the folder and file name. */
    private void pickSaveLocation() {
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType(lang.mime);
        i.putExtra(Intent.EXTRA_TITLE, lang.fileName);
        startActivityForResult(i, REQ_SAVE);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == REQ_MAN) {
            // Text picked in a manual page: drop it into the editor at the caret.
            String snippet = res == RESULT_OK && data != null
                    ? data.getStringExtra(TerminalActivity.EXTRA_SNIPPET) : null;
            if (snippet != null) {
                int at = Math.max(0, code.getSelectionStart());
                code.getText().insert(at, snippet);
            }
            return;
        }
        if (req != REQ_SAVE || res != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try (OutputStream out = getContentResolver().openOutputStream(uri)) {
            if (out == null) throw new IOException("cannot open " + uri);
            out.write(code.getText().toString().getBytes(StandardCharsets.UTF_8));
            Toast.makeText(this, "Saved " + uri.getLastPathSegment(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /** Borderless grey text button, no fill, in the app font. */
    private Button flatButton(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTypeface(Fonts.current(this));
        b.setAllCaps(false);
        b.setTextColor(0xFF9E9E9E);
        b.setBackground(null);
        b.setStateListAnimator(null);
        return b;
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveSource(code.getText().toString());
    }

    private void run() {
        execute(false);
    }

    private void lint() {
        execute(true);
    }

    /** Runs the program, or only the static checks when {@code lintOnly}. Result shows in the output pane. */
    private void execute(boolean lintOnly) {
        PackUi.ensure(this, PackStore.forLang(lang), () -> doExecute(lintOnly));
    }

    private void doExecute(boolean lintOnly) {
        String src = code.getText().toString();
        saveSource(src);
        SandboxRunner.Lang current = lang;
        outputIsLint = lintOnly;
        setBusy(true, lintOnly ? "Checking…"
                : current == SandboxRunner.Lang.C ? "Compiling…" : "Running…");
        final int gen = editGen;
        new Thread(() -> {
            SandboxRunner.Result r = lintOnly
                    ? SandboxRunner.lint(this, current, src)
                    : SandboxRunner.run(this, current, src);
            main.post(() -> {
                if (isFinishing()) return;
                setBusy(false, null);
                output.setText(r.output.isEmpty() ? (r.ok ? "[no output]" : "[failed]") : r.output);
                output.setTextColor(r.ok ? 0xFFB5B5B5 : 0xFFE57373);
                if (gen == editGen && (lintOnly || !r.ok)) markProblems(r.output, r.ok);
            });
        }, lintOnly ? "sandbox-lint" : "sandbox-run").start();
    }

    private void scheduleLint() {
        main.removeCallbacks(autoLintTask);
        if (autoLint) main.postDelayed(autoLintTask, AUTO_LINT_DELAY_MS);
    }

    /** Quiet check of the current text: marks the lines in the editor, never asks to download anything. */
    private void autoLint() {
        if (isFinishing() || !autoLint) return;
        if (!PackStore.installed(this, PackStore.forLang(lang))) return;
        if (linting) {
            scheduleLint();
            return;
        }
        final String src = code.getText().toString();
        final int gen = editGen;
        final SandboxRunner.Lang current = lang;
        linting = true;
        new Thread(() -> {
            SandboxRunner.Result r = SandboxRunner.lint(this, current, src);
            main.post(() -> {
                linting = false;
                if (isFinishing() || gen != editGen || current != lang) return;
                markProblems(r.output, r.ok);
                if (outputIsLint) {
                    output.setText(r.ok ? "no issues" : r.output);
                    output.setTextColor(r.ok ? 0xFFB5B5B5 : 0xFFE57373);
                }
            });
        }, "sandbox-autolint").start();
    }

    private void clearMarks() {
        Editable e = code.getText();
        for (LintMark m : e.getSpans(0, e.length(), LintMark.class)) e.removeSpan(m);
    }

    /** Tints the lines named in a compiler/linter report: warnings amber, errors red. */
    private void markProblems(String report, boolean ok) {
        clearMarks();
        if (ok) return;
        Editable e = code.getText();
        String text = e.toString();
        java.util.Map<Integer, Boolean> lines = new java.util.TreeMap<>(); // line -> is error
        Matcher m = PROBLEM.matcher(report);
        while (m.find()) {
            int line = Integer.parseInt(m.group(1));
            String sev = m.group(3);
            String msg = m.group(4).toLowerCase(java.util.Locale.ROOT);
            boolean error = "error".equals(sev) || (sev == null && (msg.contains("syntax") || msg.contains("invalid")
                    || msg.contains("unexpected") || msg.contains("expected") || msg.contains("indent")));
            Boolean prev = lines.get(line);
            lines.put(line, error || (prev != null && prev));
        }
        for (java.util.Map.Entry<Integer, Boolean> en : lines.entrySet()) {
            int start = 0;
            for (int i = 1; i < en.getKey(); i++) {
                int nl = text.indexOf('\n', start);
                if (nl < 0) { start = -1; break; }
                start = nl + 1;
            }
            if (start < 0 || start > text.length()) continue;
            int end = text.indexOf('\n', start);
            if (end < 0) end = text.length();
            if (end == start) end = Math.min(text.length(), start + 1);
            if (end > start) {
                e.setSpan(new LintMark(en.getValue() ? MARK_ERROR : MARK_WARNING), start, end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    private void setBusy(boolean busy, String status) {
        runBtn.setEnabled(!busy);
        lintBtn.setEnabled(!busy);
        if (status != null) output.setText(status);
    }

    /** Draft for the current language, e.g. {@code sandbox/main.js}. */
    private File sourceFile() {
        return new File(getFilesDir(), "sandbox/draft-" + lang.fileName);
    }

    private String loadSource() {
        File f = sourceFile();
        // C drafts used to be saved as sandbox/source.c; keep reading that once.
        if (!f.exists() && lang == SandboxRunner.Lang.C) {
            File legacy = new File(getFilesDir(), "sandbox/source.c");
            if (legacy.exists()) f = legacy;
        }
        if (!f.exists()) return lang.sample;
        try {
            return new String(java.nio.file.Files.readAllBytes(f.toPath()), java.nio.charset.StandardCharsets.UTF_8);
        } catch (IOException e) {
            return lang.sample;
        }
    }

    private void saveSource(String text) {
        try {
            SandboxRunner.write(sourceFile(), text);
        } catch (IOException ignored) {
            // keep the editor usable even if the disk write fails
        }
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}

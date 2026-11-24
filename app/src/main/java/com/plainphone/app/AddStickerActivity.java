package com.plainphone.app;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation;
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenter;
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Opened from the keyboard's image tab: pick one picture or several, then edit each one as a sticker on a
 * canvas: add text (bend it, move it anywhere), drawn items, emoji from the keyboard, each with its own
 * border; the picture has a border too. Then save it to a collection. One panel holds every control.
 */
public class AddStickerActivity extends Activity implements StickerCanvasView.Listener {

    private static final int PICK = 1;

    /** String extra: the collection the images go to unless another is chosen. */
    static final String EXTRA_COLLECTION = "collection";

    private static final int[] FILL_COLORS = {0xFFFF5C9A, 0xFFFF6B6B, 0xFFFFB84D, 0xFFFFE066, 0xFF7ED957, 0xFF6FE3B0, 0xFF5CC8FF, 0xFF3F7CFF,
            0xFFA78BFA, 0xFFE04FD8, 0xFFFFFFFF, 0xFF3A3A3A};
    private static final String[] FONT_NAMES = {"Round", "Hand", "Serif", "Bold"};
    private static final String[][] BORDER_STYLES = {{"white", "White"}, {"color", "Color"}, {"neon", "Neon"}, {"none", "None"}};
    private static final String[][] PIC_STYLES = {{"white", "White"}, {"color", "Color"}, {"neon", "Neon"}, {"dotted", "Dotted"}, {"none", "None"}};

    private static final class Pending {
        final Bitmap bmp;     // the picture (for a GIF: its first frame)
        final Uri gif;        // set for an animated GIF: saved as it is, never edited

        Pending(Bitmap bmp, Uri gif) {
            this.bmp = bmp;
            this.gif = gif;
        }
    }

    private final List<Pending> queue = new ArrayList<>();
    private int cur;
    private int saved;
    private String collection = StickerStore.DEFAULT;
    private boolean adding, focusName;
    private String newName = "";
    private SubjectSegmenter segmenter;
    private int seq;
    private String cat = "";

    private LinearLayout root;
    private TextView title, count;
    private StickerCanvasView canvas;
    private ScrollView scroll;
    private TextView gifNote;
    private LinearLayout panel;

    // text editor
    private LinearLayout textBox;
    private EditText textIn;
    private boolean settingText;
    private final List<TextView> fontChips = new ArrayList<>();
    private Dots textDots;
    // item colours
    private LinearLayout fillBox, solidBox, gradBox;
    private final List<TextView> fillChips = new ArrayList<>(), dirChips = new ArrayList<>();
    private Dots solidDots, fromDots, toDots;
    // item border
    private LinearLayout borderBox;
    private final List<TextView> borderChips = new ArrayList<>();
    private Dots borderDots;
    private Range borderWidth;
    // items
    private EditText emojiIn;
    private FlowLayout tray;
    private final List<TextView> catChips = new ArrayList<>();
    // picture
    private final List<TextView> picChips = new ArrayList<>();
    private Dots picDots;
    private Range picWidth;
    private LinearLayout modeRow;
    private final List<TextView> modeChips = new ArrayList<>();
    private boolean cutting;
    private LinearLayout saveHost;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) { finish(); return; }
        String start = getIntent().getStringExtra(EXTRA_COLLECTION);
        if (start != null && !start.isEmpty()) collection = start;
        StickerLib.load(this);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);
        setContentView(root);
        pick();
    }

    private void pick() {
        Intent pick = new Intent(Intent.ACTION_GET_CONTENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE)
                .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        try {
            startActivityForResult(pick, PICK);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No photo picker found", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        List<Uri> uris = new ArrayList<>();
        if (resultCode == RESULT_OK && data != null) {
            if (data.getClipData() != null) {
                for (int i = 0; i < data.getClipData().getItemCount(); i++) uris.add(data.getClipData().getItemAt(i).getUri());
            } else if (data.getData() != null) {
                uris.add(data.getData());
            }
        }
        if (uris.isEmpty()) { finish(); return; }
        Context app = getApplicationContext();
        new Thread(() -> {
            List<Pending> loaded = new ArrayList<>();
            int tooBig = 0;
            for (Uri u : uris) {
                boolean gif = StickerStore.isGif(app, u);
                if (gif && StickerStore.length(app, u) > StickerStore.MAX_GIF_BYTES) { tooBig++; continue; }
                Bitmap b = StickerStore.scaledBitmap(app, u);   // for a GIF: its first frame
                if (b != null) loaded.add(new Pending(b, gif ? u : null));
            }
            final int skipped = tooBig;
            runOnUiThread(() -> {
                if (skipped > 0) {
                    Toast.makeText(this, skipped + " GIF over " + (StickerStore.MAX_GIF_BYTES >> 20) + " MB skipped", Toast.LENGTH_LONG).show();
                }
                if (loaded.isEmpty()) {
                    Toast.makeText(this, "Could not read the image", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                queue.addAll(loaded);
                buildUi();
                showCurrent();
            });
        }).start();
    }

    private int dp(float v) {
        return UiKit.dp(this, v);
    }

    // ================================================================================================ screen

    private void buildUi() {
        root.removeAllViews();
        // header
        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setPadding(dp(8), dp(8), dp(14), dp(4));
        PanelKit.IconView back = new PanelKit.IconView(this, KeyIcons.LEFT);
        back.setOnClickListener(v -> finish());
        head.addView(back, new LinearLayout.LayoutParams(dp(40), dp(34)));
        title = new TextView(this);
        title.setTextSize(15);
        title.setTextColor(Color.WHITE);
        title.setTypeface(Fonts.cascadiaMono(this));
        head.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        count = PanelKit.caption(this, "");
        head.addView(count);
        root.addView(head, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // the canvas
        canvas = new StickerCanvasView(this);
        canvas.setListener(this);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        clp.setMargins(dp(12), dp(2), dp(12), 0);
        clp.gravity = Gravity.CENTER_HORIZONTAL;
        canvas.setBackground(UiKit.rounded(this, 0xFF0D0D0D, PanelKit.LINE, 1f, 14));
        UiKit.clipRounded(this, canvas, 14);
        root.addView(canvas, clp);

        // how the photo is used: as it is, with the background cut out, or cut out with a white outline
        modeRow = new LinearLayout(this);
        modeRow.setGravity(Gravity.CENTER);
        modeRow.setPadding(dp(12), dp(8), dp(12), 0);
        String[] modes = {"Original", "Cut out", "Outline"};
        for (int i = 0; i < modes.length; i++) {
            final int m = i;
            TextView c = chip(modes[i], i == 0, v -> setMode(m));
            modeChips.add(c);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(34), 1f);
            if (i > 0) lp.leftMargin = dp(6);
            modeRow.addView(c, lp);
        }
        root.addView(modeRow, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // the panel
        panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(12), dp(10), dp(12), dp(12));
        buildTextBox();
        buildFillBox();
        buildBorderBox();
        buildAddSection();
        buildItemsSection();
        buildPictureSection();
        scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.addView(panel);
        gifNote = PanelKit.caption(this, "GIF · SAVED AS IT IS");
        gifNote.setPadding(dp(14), dp(14), dp(14), dp(14));
        gifNote.setVisibility(View.GONE);
        root.addView(gifNote);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        // save
        saveHost = new LinearLayout(this);
        saveHost.setOrientation(LinearLayout.VERTICAL);
        saveHost.setBackgroundColor(0xFF0A0A0A);
        root.addView(saveHost, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        refreshSave();
        onSelect(null);
    }

    private void showCurrent() {
        Pending p = queue.get(cur);
        canvas.reset(p.bmp, p.gif != null);
        int n = queue.size();
        title.setText(n > 1 ? "Add " + n + " images" : "Add image");
        count.setText(n > 1 ? (cur + 1) + "/" + n : "");
        scroll.setVisibility(p.gif != null ? View.GONE : View.VISIBLE);
        modeRow.setVisibility(p.gif != null ? View.GONE : View.VISIBLE);
        gifNote.setVisibility(p.gif != null ? View.VISIBLE : View.GONE);
        updatePhotoChips();
        refreshSave();
        onSelect(null);
    }

    // ================================================================================================ controls

    private TextView cap(String text, int topDp) {
        TextView t = PanelKit.caption(this, text);
        t.setPadding(0, dp(topDp), 0, dp(7));
        return t;
    }

    private TextView chip(String text, boolean on, View.OnClickListener click) {
        TextView t = PanelKit.chip(this, text, on, click);
        t.setTextSize(12.5f);
        return t;
    }

    private FlowLayout flow() {
        return new FlowLayout(this, dp(6), dp(6));
    }

    private static void setOn(Context c, List<TextView> chips, int index) {
        for (int i = 0; i < chips.size(); i++) PanelKit.styleChip(c, chips.get(i), i == index);
    }

    /** A row of round colour swatches; the picked one gets a white ring. */
    private final class Dots extends FlowLayout {
        private final List<View> dots = new ArrayList<>();
        private final int[] colors;

        Dots(int[] colors, Consumer<Integer> pick) {
            super(AddStickerActivity.this, UiKit.dp(AddStickerActivity.this, 6), UiKit.dp(AddStickerActivity.this, 6));
            this.colors = colors;
            for (int i = 0; i < colors.length; i++) {
                final int c = colors[i];
                View d = new View(AddStickerActivity.this) {
                    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

                    @Override
                    protected void onDraw(Canvas cv) {
                        float r = Math.min(getWidth(), getHeight()) / 2f;
                        boolean on = getTag() != null;
                        p.setStyle(Paint.Style.FILL);
                        p.setColor(c);
                        cv.drawCircle(getWidth() / 2f, getHeight() / 2f, r - dp(2), p);
                        p.setStyle(Paint.Style.STROKE);
                        p.setStrokeWidth(on ? dp(2.2f) : dp(1));
                        p.setColor(on ? Color.WHITE : 0xFF555555);
                        cv.drawCircle(getWidth() / 2f, getHeight() / 2f, r - dp(2), p);
                    }
                };
                d.setOnClickListener(v -> pick.accept(c));
                addView(d, new ViewGroup.LayoutParams(dp(30), dp(30)));
                dots.add(d);
            }
        }

        void select(int color) {
            for (int i = 0; i < dots.size(); i++) {
                dots.get(i).setTag(colors[i] == color ? "on" : null);
                dots.get(i).invalidate();
            }
        }
    }

    /** A labelled slider: caption on the left, the value in a pill on the right, a dark track that fills white. */
    private final class Range extends LinearLayout {
        private final TextView pill;
        private final SeekBar bar;
        private final float min, step;

        Range(String label, float min, float max, float step, Consumer<Float> change) {
            super(AddStickerActivity.this);
            this.min = min;
            this.step = step;
            setOrientation(VERTICAL);
            setPadding(0, dp(14), 0, 0);
            LinearLayout head = new LinearLayout(AddStickerActivity.this);
            head.setGravity(Gravity.CENTER_VERTICAL);
            head.addView(PanelKit.caption(AddStickerActivity.this, label), new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            pill = new TextView(AddStickerActivity.this);
            pill.setTextSize(12);
            pill.setTextColor(Color.WHITE);
            pill.setTypeface(Fonts.cascadiaMono(AddStickerActivity.this));
            pill.setGravity(Gravity.CENTER);
            pill.setMinWidth(dp(46));
            pill.setPadding(dp(10), dp(3), dp(10), dp(3));
            pill.setBackground(UiKit.rounded(AddStickerActivity.this, PanelKit.KEY, PanelKit.LINE, 1f, 8));
            head.addView(pill);
            addView(head, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            bar = new SeekBar(AddStickerActivity.this);
            bar.setMax(Math.round((max - min) / step));
            GradientDrawable track = new GradientDrawable();
            track.setColor(0xFF2A2A2A);
            track.setCornerRadius(dp(3));
            GradientDrawable fill = new GradientDrawable();
            fill.setColor(Color.WHITE);
            fill.setCornerRadius(dp(3));
            LayerDrawable ld = new LayerDrawable(new Drawable[]{track, new ClipDrawable(fill, Gravity.LEFT, ClipDrawable.HORIZONTAL)});
            ld.setId(0, android.R.id.background);
            ld.setId(1, android.R.id.progress);
            for (int i = 0; i < 2; i++) {
                ld.setLayerHeight(i, dp(6));
                ld.setLayerGravity(i, Gravity.CENTER_VERTICAL);
            }
            bar.setProgressDrawable(ld);
            GradientDrawable thumb = new GradientDrawable();
            thumb.setShape(GradientDrawable.OVAL);
            thumb.setColor(Color.WHITE);
            thumb.setStroke(dp(4), Color.BLACK);
            thumb.setSize(dp(26), dp(26));
            bar.setThumb(thumb);
            bar.setThumbOffset(0);
            bar.setPadding(dp(13), 0, dp(13), 0);
            bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                    float v = Range.this.min + p * Range.this.step;
                    pill.setText(String.format(java.util.Locale.US, "%.1f", v));
                    if (fromUser) change.accept(v);
                }

                @Override public void onStartTrackingTouch(SeekBar s) { }

                @Override public void onStopTrackingTouch(SeekBar s) { }
            });
            addView(bar, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(32)));
        }

        void set(float v) {
            bar.setProgress(Math.round((v - min) / step));
            pill.setText(String.format(java.util.Locale.US, "%.1f", v));
        }
    }

    // ---- text editor (shown while a text is selected)

    private void buildTextBox() {
        textBox = section();
        textBox.addView(cap("TEXT", 0));
        textIn = new EditText(this);
        textIn.setHint("Cute text...");
        textIn.setHintTextColor(PanelKit.DIM);
        textIn.setTextColor(Color.WHITE);
        textIn.setTextSize(15);
        textIn.setSingleLine(true);
        textIn.setPadding(dp(11), 0, dp(11), 0);
        textIn.setBackground(UiKit.rounded(this, PanelKit.KEY, 0xFF555555, 1f, 9));
        textIn.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        textIn.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }

            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }

            @Override
            public void afterTextChanged(Editable s) {
                StickerCanvasView.Item it = canvas == null ? null : canvas.selected();
                if (!settingText && it != null && it.kind == StickerCanvasView.K_TEXT) canvas.setText(it, s.toString());
            }
        });
        textBox.addView(textIn, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(40)));
        FlowLayout fonts = flow();
        fonts.setPadding(0, dp(8), 0, 0);
        for (int i = 0; i < FONT_NAMES.length; i++) {
            final int idx = i;
            TextView c = chip(FONT_NAMES[i], false, v -> {
                StickerCanvasView.Item it = canvas.selected();
                if (it != null && it.kind == StickerCanvasView.K_TEXT) {
                    canvas.setFont(it, idx);
                    setOn(this, fontChips, idx);
                    textIn.setTypeface(Fonts.caption(this, idx));
                }
            });
            fontChips.add(c);
            fonts.addView(c);
        }
        textBox.addView(fonts);
        textDots = new Dots(StickerCanvasView.COLORS, color -> {
            StickerCanvasView.Item it = canvas.selected();
            if (it == null || it.kind != StickerCanvasView.K_TEXT) return;
            for (int i = 0; i < StickerCanvasView.COLORS.length; i++) if (StickerCanvasView.COLORS[i] == color) canvas.setTextColor(it, i);
            textDots.select(color);
        });
        LinearLayout.LayoutParams dl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dl.topMargin = dp(10);
        textBox.addView(textDots, dl);
        panel.addView(textBox);
    }

    private LinearLayout section() {
        LinearLayout s = new LinearLayout(this);
        s.setOrientation(LinearLayout.VERTICAL);
        s.setPadding(0, 0, 0, dp(12));
        return s;
    }

    // ---- colours of the selected drawn item: its own, one solid colour, or a gradient

    private void buildFillBox() {
        fillBox = section();
        fillBox.addView(cap("FILL", 0));
        FlowLayout modes = flow();
        String[] names = {"Original", "Solid", "Gradient"};
        for (int i = 0; i < names.length; i++) {
            final int m = i;
            TextView c = chip(names[i], false, v -> {
                StickerCanvasView.Item it = canvas.selected();
                if (it == null || it.kind != StickerCanvasView.K_ART) return;
                it.fill.mode = m;
                canvas.setFill(it);
                syncFill(it);
            });
            fillChips.add(c);
            modes.addView(c);
        }
        fillBox.addView(modes);

        solidBox = new LinearLayout(this);
        solidBox.setOrientation(LinearLayout.VERTICAL);
        solidDots = new Dots(FILL_COLORS, color -> {
            StickerCanvasView.Item it = canvas.selected();
            if (it == null || it.kind != StickerCanvasView.K_ART) return;
            it.fill.a = color;
            it.fill.mode = 1;
            canvas.setFill(it);
            syncFill(it);
        });
        solidBox.addView(solidDots);
        LinearLayout.LayoutParams sl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sl.topMargin = dp(10);
        fillBox.addView(solidBox, sl);

        gradBox = new LinearLayout(this);
        gradBox.setOrientation(LinearLayout.VERTICAL);
        gradBox.addView(cap("FROM", 0));
        fromDots = new Dots(FILL_COLORS, color -> {
            StickerCanvasView.Item it = canvas.selected();
            if (it == null || it.kind != StickerCanvasView.K_ART) return;
            it.fill.a = color;
            it.fill.mode = 2;
            canvas.setFill(it);
            syncFill(it);
        });
        gradBox.addView(fromDots);
        gradBox.addView(cap("TO", 8));
        toDots = new Dots(FILL_COLORS, color -> {
            StickerCanvasView.Item it = canvas.selected();
            if (it == null || it.kind != StickerCanvasView.K_ART) return;
            it.fill.b = color;
            it.fill.mode = 2;
            canvas.setFill(it);
            syncFill(it);
        });
        gradBox.addView(toDots);
        FlowLayout dirs = flow();
        dirs.setPadding(0, dp(10), 0, 0);
        String[] dn = {"Down", "Across", "Diagonal"};
        for (int i = 0; i < dn.length; i++) {
            final int d = i;
            TextView c = chip(dn[i], false, v -> {
                StickerCanvasView.Item it = canvas.selected();
                if (it == null || it.kind != StickerCanvasView.K_ART) return;
                it.fill.dir = d;
                canvas.setFill(it);
                syncFill(it);
            });
            dirChips.add(c);
            dirs.addView(c);
        }
        gradBox.addView(dirs);
        fillBox.addView(gradBox, sl);
        panel.addView(fillBox);
    }

    private void syncFill(StickerCanvasView.Item it) {
        setOn(this, fillChips, it.fill.mode);
        solidBox.setVisibility(it.fill.mode == 1 ? View.VISIBLE : View.GONE);
        gradBox.setVisibility(it.fill.mode == 2 ? View.VISIBLE : View.GONE);
        solidDots.select(it.fill.a);
        fromDots.select(it.fill.a);
        toDots.select(it.fill.b);
        setOn(this, dirChips, it.fill.dir);
    }

    // ---- border of the selected item or text

    private void buildBorderBox() {
        borderBox = section();
        borderBox.addView(cap("BORDER", 0));
        FlowLayout styles = flow();
        for (int i = 0; i < BORDER_STYLES.length; i++) {
            final String key = BORDER_STYLES[i][0];
            TextView c = chip(BORDER_STYLES[i][1], false, v -> {
                StickerCanvasView.Item it = canvas.selected();
                if (it == null) return;
                it.border.style = key;
                canvas.applyBorder(it);
                syncBorder(it);
            });
            borderChips.add(c);
            styles.addView(c);
        }
        borderBox.addView(styles);
        borderDots = new Dots(StickerCanvasView.COLORS, color -> {
            StickerCanvasView.Item it = canvas.selected();
            if (it == null) return;
            it.border.color = color;
            canvas.applyBorder(it);
            syncBorder(it);
        });
        LinearLayout.LayoutParams dl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dl.topMargin = dp(10);
        borderBox.addView(borderDots, dl);
        borderWidth = new Range("WIDTH", 0.5f, 14f, 0.5f, v -> {
            StickerCanvasView.Item it = canvas.selected();
            if (it == null) return;
            it.border.width = v;
            canvas.applyBorder(it);
        });
        borderBox.addView(borderWidth);
        panel.addView(borderBox);
    }

    private void syncBorder(StickerCanvasView.Item it) {
        int idx = 0;
        for (int i = 0; i < BORDER_STYLES.length; i++) if (BORDER_STYLES[i][0].equals(it.border.style)) idx = i;
        setOn(this, borderChips, idx);
        boolean coloured = it.border.style.equals("color") || it.border.style.equals("neon");
        borderDots.setVisibility(coloured ? View.VISIBLE : View.GONE);
        borderDots.select(it.border.color);
        borderWidth.setVisibility(it.border.style.equals("none") ? View.GONE : View.VISIBLE);
        borderWidth.set(it.border.width);
    }

    @Override
    public void onSelect(StickerCanvasView.Item it) {
        if (textBox == null) return;
        boolean isText = it != null && it.kind == StickerCanvasView.K_TEXT;
        textBox.setVisibility(isText ? View.VISIBLE : View.GONE);
        borderBox.setVisibility(it != null ? View.VISIBLE : View.GONE);
        boolean recolorable = it != null && it.kind == StickerCanvasView.K_ART && it.def != null && it.def.primary != null;
        fillBox.setVisibility(recolorable ? View.VISIBLE : View.GONE);
        if (recolorable) syncFill(it);
        if (isText) {
            settingText = true;
            if (!textIn.getText().toString().equals(it.text)) textIn.setText(it.text);
            settingText = false;
            textIn.setTypeface(Fonts.caption(this, it.font));
            setOn(this, fontChips, it.font);
            textDots.select(StickerCanvasView.COLORS[it.color]);
        }
        if (it != null) {
            syncBorder(it);
            // the text and border controls are at the top of the panel: show them when something gets selected
            scroll.post(() -> scroll.smoothScrollTo(0, 0));
        }
    }

    // ---- add: text, combos, items, emoji

    private void buildAddSection() {
        panel.addView(cap("ADD", 0));
        FlowLayout row = flow();
        row.addView(chip("+ Text", false, v -> {
            float[] p = spot(0, 0);
            StickerCanvasView.Item it = canvas.addText("new text", 0, 1, p[0], p[1] + 30, 1f, 0f);
            if (it != null) {
                textIn.requestFocus();
                textIn.selectAll();
                android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null) imm.showSoftInput(textIn, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
            }
        }));
        panel.addView(row);
        panel.addView(cap("COMBOS", 14));
        FlowLayout combos = flow();
        for (StickerLib.Combo cb : StickerLib.combos) {
            combos.addView(chip(cb.label, false, v -> {
                StickerLib.Def d = StickerLib.def(cb.def);
                if (d == null) return;
                float[] p = spot(40, 0);
                StickerCanvasView.Item it = canvas.addArt(d, p[0], p[1]);
                if (it == null) return;
                StickerCanvasView.Item t = canvas.addText(cb.text, 0, cb.color, it.x, it.y + 3, cb.ts, 0f);
                if (t != null) canvas.attach(t);
            }));
        }
        panel.addView(combos);
    }

    private float[] spot(float dx, float dy) {
        int n = seq++ % 5;
        return new float[]{120 + n * 22 + dx, 100 + n * 24 + dy};
    }

    private void buildItemsSection() {
        panel.addView(cap("ITEMS", 14));
        // emoji from the keyboard
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        emojiIn = new EditText(this);
        emojiIn.setHint("Emoji from the keyboard");
        emojiIn.setHintTextColor(PanelKit.DIM);
        emojiIn.setTextColor(Color.WHITE);
        emojiIn.setTextSize(16);
        emojiIn.setTypeface(Fonts.cascadiaMono(this));
        emojiIn.setSingleLine(true);
        emojiIn.setPadding(dp(11), 0, dp(11), 0);
        emojiIn.setBackground(UiKit.rounded(this, PanelKit.KEY, 0xFF555555, 1f, 9));
        emojiIn.setImeOptions(EditorInfo.IME_ACTION_DONE | EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        emojiIn.setOnEditorActionListener((v, a, e) -> { addTypedEmoji(); return true; });
        row.addView(emojiIn, new LinearLayout.LayoutParams(0, dp(40), 1f));
        TextView add = chip("Add", false, v -> addTypedEmoji());
        LinearLayout.LayoutParams al = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(40));
        al.leftMargin = dp(6);
        row.addView(add, al);
        LinearLayout.LayoutParams rl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rl.bottomMargin = dp(12);
        panel.addView(row, rl);
        // categories + the tray
        FlowLayout cats = flow();
        for (int i = 0; i < StickerLib.cats.size(); i++) {
            final String c = StickerLib.cats.get(i);
            if (cat.isEmpty()) cat = c;
            TextView t = chip(c, c.equals(cat), v -> {
                cat = c;
                for (int k = 0; k < catChips.size(); k++) PanelKit.styleChip(this, catChips.get(k), StickerLib.cats.get(k).equals(cat));
                renderTray();
            });
            catChips.add(t);
            cats.addView(t);
        }
        panel.addView(cats);
        tray = new FlowLayout(this, dp(7), dp(7));
        tray.setPadding(0, dp(10), 0, 0);
        panel.addView(tray);
        renderTray();
    }

    private void renderTray() {
        tray.removeAllViews();
        int cell = dp(54), icon = dp(44);
        for (StickerLib.Def d : StickerLib.items) {
            if (!d.cat.equals(cat)) continue;
            ImageView v = new ImageView(this);
            v.setScaleType(ImageView.ScaleType.CENTER);
            Bitmap b = StickerLib.icon(this, d, icon);
            if (b != null) v.setImageBitmap(b);
            v.setBackground(UiKit.pressable(this, PanelKit.KEY, PanelKit.DOWN, PanelKit.LINE, 1f, 11));
            v.setOnClickListener(x -> {
                float[] p = spot(0, 0);
                canvas.addArt(d, p[0], p[1]);
            });
            tray.addView(v, new ViewGroup.LayoutParams(cell, dp(50)));
        }
    }

    /** Emoji typed or pasted from the keyboard become stickers: each one gets its own border and handles. */
    private void addTypedEmoji() {
        String text = emojiIn.getText().toString();
        java.text.BreakIterator bi = java.text.BreakIterator.getCharacterInstance();
        bi.setText(text);
        int added = 0;
        StickerCanvasView.Item last = null;
        for (int s = bi.first(), e = bi.next(); e != java.text.BreakIterator.DONE && added < 12; s = e, e = bi.next()) {
            String g = text.substring(s, e);
            int cp = g.codePointAt(0);
            if (cp < 0x80 || Character.isLetterOrDigit(cp) || Character.isWhitespace(cp)) continue;   // plain text is not an emoji
            float[] p = spot(0, 0);
            StickerCanvasView.Item it = canvas.addEmoji(g, p[0], p[1]);
            if (it != null) {
                last = it;
                added++;
            }
        }
        if (last != null) {
            emojiIn.setText("");
            emojiIn.setHint("Emoji from the keyboard");
        } else {
            emojiIn.setHint("No emoji found, try another");
        }
    }

    // ---- the picture: cut-out and its border

    private void buildPictureSection() {
        panel.addView(cap("PICTURE BORDER", 14));
        FlowLayout styles = flow();
        for (int i = 0; i < PIC_STYLES.length; i++) {
            final String key = PIC_STYLES[i][0];
            TextView c = chip(PIC_STYLES[i][1], false, v -> {
                canvas.pic().style = key;
                canvas.picChanged();
                syncPicture();
            });
            picChips.add(c);
            styles.addView(c);
        }
        panel.addView(styles);
        picDots = new Dots(StickerCanvasView.COLORS, color -> {
            canvas.pic().color = color;
            canvas.picChanged();
            syncPicture();
        });
        LinearLayout.LayoutParams dl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dl.topMargin = dp(10);
        panel.addView(picDots, dl);
        picWidth = new Range("WIDTH", 1f, 16f, 0.5f, v -> {
            canvas.pic().width = v;
            canvas.picChanged();
        });
        panel.addView(picWidth);
        syncPicture();
    }

    private void syncPicture() {
        StickerCanvasView.PicBorder pb = canvas.pic();
        int idx = 0;
        for (int i = 0; i < PIC_STYLES.length; i++) if (PIC_STYLES[i][0].equals(pb.style)) idx = i;
        setOn(this, picChips, idx);
        boolean coloured = pb.style.equals("color") || pb.style.equals("neon") || pb.style.equals("dotted");
        picDots.setVisibility(coloured ? View.VISIBLE : View.GONE);
        picDots.select(pb.color);
        picWidth.setVisibility(pb.style.equals("none") ? View.GONE : View.VISIBLE);
        picWidth.set(pb.width);
    }

    /** 0 original, 1 cut out (transparent edges), 2 cut out with a white outline. */
    private int currentMode() {
        if (!canvas.hasCut()) return 0;
        return "none".equals(canvas.pic().style) ? 1 : 2;
    }

    private void updatePhotoChips() {
        if (modeChips.isEmpty()) return;
        int m = cutting ? -1 : currentMode();
        for (int i = 0; i < modeChips.size(); i++) PanelKit.styleChip(this, modeChips.get(i), i == m);
        modeChips.get(1).setText(cutting ? "Cutting..." : "Cut out");
        syncPicture();
    }

    private void setMode(int m) {
        if (cutting) return;
        if (m == 0) {
            canvas.setCut(null);
            if ("none".equals(canvas.pic().style)) canvas.pic().style = "white";
            canvas.picChanged();
            updatePhotoChips();
            return;
        }
        final int style = m;
        Runnable apply = () -> {
            canvas.pic().style = style == 1 ? "none" : "white";
            if (style == 2) canvas.pic().width = Math.max(canvas.pic().width, 5f);
            canvas.picChanged();
            updatePhotoChips();
        };
        if (canvas.hasCut()) {
            apply.run();
            return;
        }
        cutting = true;
        updatePhotoChips();
        if (segmenter == null) {
            segmenter = SubjectSegmentation.getClient(new SubjectSegmenterOptions.Builder().enableForegroundBitmap().build());
        }
        final Pending p = queue.get(cur);
        segmenter.process(InputImage.fromBitmap(p.bmp, 0))
                .addOnSuccessListener(result -> {
                    cutting = false;
                    Bitmap fg = result.getForegroundBitmap();
                    if (fg == null) {
                        Toast.makeText(this, "No clear subject in this image", Toast.LENGTH_SHORT).show();
                        updatePhotoChips();
                    } else if (queue.get(cur) == p) {
                        canvas.setCut(fg);
                        apply.run();
                    }
                })
                .addOnFailureListener(e -> {
                    cutting = false;
                    Toast.makeText(this, "Could not remove the background", Toast.LENGTH_SHORT).show();
                    updatePhotoChips();
                });
    }

    // ================================================================================================ saving

    /** Where the sticker goes (collection chips, or a name field for a new one) and the Save button. */
    private void refreshSave() {
        if (saveHost == null) return;
        saveHost.removeAllViews();
        View line = new View(this);
        line.setBackgroundColor(PanelKit.LINE);
        saveHost.addView(line, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
        LinearLayout sheet = new LinearLayout(this);
        sheet.setOrientation(LinearLayout.VERTICAL);
        sheet.setPadding(dp(12), dp(10), dp(12), dp(14));
        sheet.addView(cap("SAVE TO", 0));
        TextView save = new TextView(this);
        if (adding) {
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            EditText name = new EditText(this);
            name.setHint("New collection name");
            name.setHintTextColor(PanelKit.DIM);
            name.setTextColor(Color.WHITE);
            name.setTextSize(13);
            name.setTypeface(Fonts.cascadiaMono(this));
            name.setSingleLine(true);
            name.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(30)});
            name.setPadding(dp(11), 0, dp(11), 0);
            name.setBackground(UiKit.rounded(this, PanelKit.KEY, 0xFF555555, 1f, 9));
            name.setText(newName);
            name.setSelection(newName.length());
            name.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }

                @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }

                @Override
                public void afterTextChanged(Editable s) {
                    newName = s.toString();
                    save.setText(saveLabel());
                }
            });
            row.addView(name, new LinearLayout.LayoutParams(0, dp(34), 1f));
            TextView cancel = PanelKit.chipSmall(this, "Cancel", false, v -> {
                adding = false;
                newName = "";
                refreshSave();
            });
            LinearLayout.LayoutParams cl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(34));
            cl.leftMargin = dp(5);
            row.addView(cancel, cl);
            sheet.addView(row);
            if (focusName) {
                focusName = false;
                name.requestFocus();
                name.post(() -> ((android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                        .showSoftInput(name, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT));
            }
        } else {
            HorizontalScrollView hs = new HorizontalScrollView(this);
            hs.setHorizontalScrollBarEnabled(false);
            LinearLayout chips = new LinearLayout(this);
            for (String c : StickerStore.collections(this)) {
                int n = StickerStore.list(this, c).size();
                TextView chip = PanelKit.chipSmall(this, c + "  " + n, c.equals(collection), v -> {
                    collection = c;
                    refreshSave();
                });
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(32));
                lp.rightMargin = dp(5);
                chips.addView(chip, lp);
            }
            TextView add = PanelKit.chipSmall(this, "+ New", false, v -> {
                adding = true;
                focusName = true;
                refreshSave();
            });
            add.setTextColor(PanelKit.DIM);
            chips.addView(add, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(32)));
            hs.addView(chips);
            sheet.addView(hs);
        }
        save.setText(saveLabel());
        save.setTextSize(14);
        save.setTextColor(Color.BLACK);
        save.setGravity(Gravity.CENTER);
        save.setTypeface(Fonts.cascadiaMono(this));
        save.setBackground(UiKit.pressable(this, Color.WHITE, 0xFFBBBBBB, Color.WHITE, 1f, 12));
        save.setOnClickListener(v -> save());
        LinearLayout.LayoutParams sl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(44));
        sl.topMargin = dp(11);
        sheet.addView(save, sl);
        if (queue.size() > 1 && cur < queue.size() - 1) {
            TextView skip = PanelKit.link(this, "Skip this one", v -> next(false));
            skip.setGravity(Gravity.CENTER);
            sheet.addView(skip, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        saveHost.addView(sheet);
    }

    private String saveLabel() {
        String where = adding ? (newName.trim().isEmpty() ? "new collection" : newName.trim()) : collection;
        boolean more = queue.size() > 1 && cur < queue.size() - 1;
        return (more ? "Save and next" : "Save") + " to " + where;
    }

    private void save() {
        if (cutting || queue.isEmpty()) return;
        String target = collection;
        if (adding) {
            if (newName.trim().isEmpty()) {
                Toast.makeText(this, "Name the collection", Toast.LENGTH_SHORT).show();
                return;
            }
            target = newName.trim();
        }
        final Pending p = queue.get(cur);
        final Bitmap out = p.gif != null ? null : canvas.export(512);
        if (p.gif == null && out == null) {
            Toast.makeText(this, "Nothing to save", Toast.LENGTH_SHORT).show();
            return;
        }
        final String to = target;
        Context app = getApplicationContext();
        new Thread(() -> {
            boolean ok = p.gif != null ? StickerStore.addGif(app, p.gif, to) : StickerStore.save(app, out, to);
            runOnUiThread(() -> {
                if (!ok) {
                    Toast.makeText(this, "Could not save", Toast.LENGTH_SHORT).show();
                    return;
                }
                adding = false;
                collection = to;
                newName = "";
                next(true);
            });
        }).start();
    }

    /** Move on to the next picked image, or finish after the last. */
    private void next(boolean wasSaved) {
        if (wasSaved) saved++;
        if (cur < queue.size() - 1) {
            cur++;
            showCurrent();
            return;
        }
        if (saved > 0) setResult(RESULT_OK);
        finish();
    }

    @Override
    protected void onDestroy() {
        if (segmenter != null) segmenter.close();
        super.onDestroy();
    }
}

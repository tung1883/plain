package com.plainphone.app;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation;
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenter;
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * Opened from the keyboard's image tab: pick one picture or many, optionally cut each one's
 * background out (or all at once), choose the collection, and add them to the keyboard's own
 * images. One screen for a single image and a batch.
 */
public class AddStickerActivity extends Activity {

    private static final int PICK = 1, MORE = 2;

    /** String extra: the collection the images go to unless another is chosen. */
    static final String EXTRA_COLLECTION = "collection";

    private static final class Item {
        final Bitmap original;
        Bitmap cutBmp;      // the subject with a transparent background, once computed
        boolean cut;        // showing/saving the cut-out

        Item(Bitmap original) {
            this.original = original;
        }

        Bitmap shown() {
            return cut && cutBmp != null ? cutBmp : original;
        }
    }

    private final List<Item> items = new ArrayList<>();
    private int sel;
    private String collection = StickerStore.DEFAULT;
    private boolean adding, focusName;       // typing a new collection name
    private String newName = "";
    private int slideDir;                    // +1: the next picture comes in from the right, -1: from the left
    private String busy;                     // a message while backgrounds are being removed
    private SubjectSegmenter segmenter;
    private LinearLayout root;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) { finish(); return; }
        String start = getIntent().getStringExtra(EXTRA_COLLECTION);
        if (start != null && !start.isEmpty()) collection = start;
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);
        setContentView(root);
        pick(PICK);
    }

    private void pick(int code) {
        Intent pick = new Intent(Intent.ACTION_GET_CONTENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE)
                .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        try {
            startActivityForResult(pick, code);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No photo picker found", Toast.LENGTH_SHORT).show();
            if (items.isEmpty()) finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        List<Uri> uris = new ArrayList<>();
        if (resultCode == RESULT_OK && data != null) {
            if (data.getClipData() != null) {
                for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                    uris.add(data.getClipData().getItemAt(i).getUri());
                }
            } else if (data.getData() != null) {
                uris.add(data.getData());
            }
        }
        if (uris.isEmpty()) {
            if (items.isEmpty()) finish();
            return;
        }
        Context app = getApplicationContext();
        new Thread(() -> {
            List<Item> loaded = new ArrayList<>();
            for (Uri u : uris) {
                Bitmap b = StickerStore.scaledBitmap(app, u);
                if (b != null) loaded.add(new Item(b));
            }
            runOnUiThread(() -> {
                if (loaded.isEmpty()) {
                    Toast.makeText(this, "Could not read the image", Toast.LENGTH_SHORT).show();
                    if (items.isEmpty()) finish();
                    return;
                }
                if (loaded.size() < uris.size()) {
                    Toast.makeText(this, (uris.size() - loaded.size()) + " could not be read", Toast.LENGTH_SHORT).show();
                }
                sel = items.size();
                items.addAll(loaded);
                render();
            });
        }).start();
    }

    // ------------------------------------------------------------------- screen

    private int dp(float v) {
        return UiKit.dp(this, v);
    }

    private boolean allCut() {
        for (Item i : items) if (!i.cut) return false;
        return !items.isEmpty();
    }

    private void render() {
        root.removeAllViews();
        if (items.isEmpty()) return;
        sel = Math.max(0, Math.min(sel, items.size() - 1));
        Item cur = items.get(sel);
        int n = items.size();

        // header: back, title, Cut out all
        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setPadding(dp(8), dp(10), dp(12), dp(6));
        PanelKit.IconView back = new PanelKit.IconView(this, KeyIcons.LEFT);
        back.setOnClickListener(v -> finish());
        head.addView(back, new LinearLayout.LayoutParams(dp(40), dp(34)));
        TextView title = new TextView(this);
        title.setText(n > 1 ? "Add " + n + " images" : "Add image");
        title.setTextSize(15);
        title.setTextColor(Color.WHITE);
        title.setTypeface(Fonts.cascadiaMono(this));
        head.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        if (n > 1) {   // with one image the preview's own button does the same
            TextView all = PanelKit.chipSmall(this, "Cut out all", allCut(), v -> {
                if (busy == null) cutAll();
            });
            head.addView(all, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(30)));
        }
        root.addView(head, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // the big preview of the selected image
        FrameLayout stage = new FrameLayout(this);
        stage.setBackground(cur.cut && cur.cutBmp != null ? new CheckerDrawable(dp(10), true)
                : UiKit.rounded(this, 0xFF0D0D0D, PanelKit.LINE, 1f, 14));
        UiKit.clipRounded(this, stage, 14);
        // a bar over the picture (tag left, button right) so the image never runs under the button
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(10), dp(8), dp(10), 0);
        TextView tag = PanelKit.caption(this, (cur.cut ? "BACKGROUND REMOVED" : "ORIGINAL")
                + (n > 1 ? " · " + (sel + 1) + "/" + n : ""));
        tag.setTextSize(9.5f);
        tag.setSingleLine(true);
        bar.addView(tag, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView cutBtn = PanelKit.chipSmall(this, cur.cut ? "Cut out" : "Remove background", cur.cut, v -> {
            if (busy != null) return;
            if (cur.cut) {
                cur.cut = false;
                render();
            } else {
                busy = "Removing background...";
                render();
                cutItem(cur, () -> {
                    busy = null;
                    render();
                });
            }
        });
        LinearLayout.LayoutParams cutLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(30));
        cutLp.leftMargin = dp(8);
        bar.addView(cutBtn, cutLp);
        col.addView(bar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        ImageView preview = new ImageView(this);
        preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        preview.setPadding(dp(16), dp(8), dp(16), dp(16));
        preview.setImageBitmap(cur.shown());
        col.addView(preview, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        stage.addView(col, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        if (slideDir != 0) {   // slide the new picture in from the side it was swiped to
            col.setTranslationX(slideDir * dp(140));
            col.setAlpha(0f);
            col.animate().translationX(0).alpha(1f).setDuration(220)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
            slideDir = 0;
        }
        if (busy != null) {
            LinearLayout veil = new LinearLayout(this);
            veil.setOrientation(LinearLayout.VERTICAL);
            veil.setGravity(Gravity.CENTER);
            veil.setBackgroundColor(0xBB000000);
            veil.setClickable(true);
            android.widget.ProgressBar spin = new android.widget.ProgressBar(this);
            veil.addView(spin, new LinearLayout.LayoutParams(dp(26), dp(26)));
            TextView msg = new TextView(this);
            msg.setText(busy);
            msg.setTextSize(12);
            msg.setTextColor(PanelKit.FG);
            msg.setTypeface(Fonts.cascadiaMono(this));
            msg.setGravity(Gravity.CENTER);   // a vertical LinearLayout gives children the full width
            msg.setPadding(0, dp(10), 0, 0);
            veil.addView(msg, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            stage.addView(veil, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
        // swipe the picture left / right to move through the batch
        if (n > 1) {
            GestureDetector swipe = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onDown(MotionEvent e) {
                    return true;
                }

                @Override
                public boolean onFling(MotionEvent a, MotionEvent b, float vx, float vy) {
                    if (busy != null || a == null || b == null) return false;
                    float dx = b.getX() - a.getX(), dy = b.getY() - a.getY();
                    if (Math.abs(dx) < dp(40) || Math.abs(dx) < Math.abs(dy) * 1.5f || Math.abs(vx) < 500) return false;
                    int next = sel + (dx < 0 ? 1 : -1);   // swipe left = next
                    if (next < 0 || next >= items.size()) return false;
                    slideDir = next > sel ? 1 : -1;
                    sel = next;
                    render();
                    return true;
                }
            });
            stage.setOnTouchListener((v, e) -> {
                swipe.onTouchEvent(e);
                return true;
            });
        }
        LinearLayout.LayoutParams stageLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        stageLp.setMargins(dp(12), dp(4), dp(12), 0);
        root.addView(stage, stageLp);

        root.addView(filmstrip(), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(saveSheet(), sheetLp());
    }

    private LinearLayout.LayoutParams sheetLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(10);
        return lp;
    }

    /** The batch as small pictures: tap to select, x removes one, + picks more. */
    private View filmstrip() {
        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(this);
        row.setPadding(dp(12), dp(10), dp(12), 0);
        for (int i = 0; i < items.size(); i++) {
            final int index = i;
            Item it = items.get(i);
            FrameLayout cell = new FrameLayout(this);
            cell.setBackground(UiKit.rounded(this, 0xFF0D0D0D, PanelKit.LINE, 1f, 12));
            // the selection ring is a foreground, so a cut-out's checkerboard cannot cover it
            if (i == sel) cell.setForeground(UiKit.rounded(this, 0x00000000, Color.WHITE, dp(2), 12));
            ImageView iv = new ImageView(this);
            iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
            iv.setPadding(dp(5), dp(5), dp(5), dp(5));
            iv.setImageBitmap(it.shown());
            if (it.cut && it.cutBmp != null) iv.setBackground(new CheckerDrawable(dp(5), false));
            UiKit.clipRounded(this, cell, 12);
            cell.addView(iv, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            cell.setOnClickListener(v -> {
                if (busy != null || index == sel) return;
                slideDir = index > sel ? 1 : -1;
                sel = index;
                render();
            });
            TextView x = new TextView(this);
            x.setText("×");
            x.setTextSize(12);
            x.setGravity(Gravity.CENTER);
            x.setTextColor(Color.WHITE);
            x.setBackground(UiKit.rounded(this, 0xCC000000, 0x00000000, 0f, 999));
            x.setOnClickListener(v -> {
                if (busy != null) return;
                items.remove(index);
                if (items.isEmpty()) {
                    finish();
                    return;
                }
                if (sel >= index && sel > 0) sel--;
                render();
            });
            FrameLayout.LayoutParams xLp = new FrameLayout.LayoutParams(dp(20), dp(20), Gravity.TOP | Gravity.END);
            xLp.setMargins(0, dp(2), dp(2), 0);
            cell.addView(x, xLp);
            if (it.cut) {
                PanelKit.IconView badge = new PanelKit.IconView(this, KeyIcons.SCISSORS);
                badge.setIconScale(0.28f);
                badge.setBackground(UiKit.rounded(this, Color.WHITE, Color.WHITE, 0f, 5));
                badge.setActive(true);
                FrameLayout.LayoutParams bLp = new FrameLayout.LayoutParams(dp(16), dp(16), Gravity.BOTTOM | Gravity.START);
                bLp.setMargins(dp(3), 0, 0, dp(3));
                cell.addView(badge, bLp);
            }
            LinearLayout.LayoutParams cellLp = new LinearLayout.LayoutParams(dp(56), dp(56));
            cellLp.rightMargin = dp(6);
            row.addView(cell, cellLp);
        }
        TextView more = new TextView(this);   // pick more images into the batch
        more.setText("+");
        more.setTextSize(22);
        more.setGravity(Gravity.CENTER);
        more.setTextColor(PanelKit.DIM);
        GradientDrawable dashed = new GradientDrawable();
        dashed.setColor(0x00000000);
        dashed.setCornerRadius(dp(12));
        dashed.setStroke(dp(1), 0xFF333333, dp(4), dp(3));
        more.setBackground(dashed);
        more.setOnClickListener(v -> {
            if (busy == null) pick(MORE);
        });
        row.addView(more, new LinearLayout.LayoutParams(dp(56), dp(56)));
        scroll.addView(row);
        // keep the selected thumbnail in view after a swipe
        final int at = sel;
        scroll.post(() -> scroll.scrollTo(Math.max(0, at * dp(62) - dp(120)), 0));
        return scroll;
    }

    /** Where the images go (collection chips, or a name field for a new one) and the Save button. */
    private View saveSheet() {
        LinearLayout sheet = new LinearLayout(this);
        sheet.setOrientation(LinearLayout.VERTICAL);
        sheet.setBackgroundColor(0xFF0A0A0A);
        sheet.setPadding(dp(12), dp(10), dp(12), dp(14));
        View line = new View(this);
        line.setBackgroundColor(PanelKit.LINE);
        TextView cap = PanelKit.caption(this, "SAVE TO");
        cap.setPadding(0, 0, 0, dp(7));
        sheet.addView(cap);

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
                @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
                @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}

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
                render();
            });
            LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(34));
            cLp.leftMargin = dp(5);
            row.addView(cancel, cLp);
            sheet.addView(row);
            if (focusName) {
                focusName = false;
                name.requestFocus();
                name.post(() -> ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                        .showSoftInput(name, InputMethodManager.SHOW_IMPLICIT));
            }
        } else {
            HorizontalScrollView scroll = new HorizontalScrollView(this);
            scroll.setHorizontalScrollBarEnabled(false);
            LinearLayout chips = new LinearLayout(this);
            for (String c : StickerStore.collections(this)) {
                int count = StickerStore.list(this, c).size();
                TextView chip = PanelKit.chipSmall(this, c + "  " + count, c.equals(collection), v -> {
                    collection = c;
                    render();
                });
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(32));
                lp.rightMargin = dp(5);
                chips.addView(chip, lp);
            }
            TextView add = PanelKit.chipSmall(this, "+ New", false, v -> {
                adding = true;
                focusName = true;
                render();
            });
            add.setTextColor(PanelKit.DIM);
            chips.addView(add, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(32)));
            scroll.addView(chips);
            sheet.addView(scroll);
        }

        save.setText(saveLabel());
        save.setTextSize(14);
        save.setTextColor(Color.BLACK);
        save.setGravity(Gravity.CENTER);
        save.setTypeface(Fonts.cascadiaMono(this));
        save.setBackground(UiKit.pressable(this, Color.WHITE, 0xFFBBBBBB, Color.WHITE, 1f, 12));
        save.setOnClickListener(v -> save());
        LinearLayout.LayoutParams saveLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(44));
        saveLp.topMargin = dp(11);
        sheet.addView(save, saveLp);

        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.addView(line, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
        wrap.addView(sheet);
        return wrap;
    }

    private String saveLabel() {
        int n = items.size();
        String where = adding ? (newName.trim().isEmpty() ? "new collection" : newName.trim()) : collection;
        return "Save " + (n > 1 ? n + " images" : "image") + " to " + where;
    }

    // ----------------------------------------------------------- background removal

    /** Cut every image out, one after the other; or, if all are already cut, put them all back. */
    private void cutAll() {
        if (allCut()) {
            for (Item i : items) i.cut = false;
            render();
            return;
        }
        cutFrom(0);
    }

    private void cutFrom(int i) {
        if (i >= items.size()) {
            busy = null;
            render();
            return;
        }
        busy = "Removing background " + (i + 1) + "/" + items.size() + "...";
        render();
        cutItem(items.get(i), () -> cutFrom(i + 1));
    }

    /** Cut one image's subject out (on the device, no network), then call {@code done}. */
    private void cutItem(Item it, Runnable done) {
        if (it.cutBmp != null) {
            it.cut = true;
            done.run();
            return;
        }
        if (segmenter == null) {
            segmenter = SubjectSegmentation.getClient(
                    new SubjectSegmenterOptions.Builder().enableForegroundBitmap().build());
        }
        segmenter.process(InputImage.fromBitmap(it.original, 0))
                .addOnSuccessListener(result -> {
                    Bitmap fg = result.getForegroundBitmap();
                    if (fg == null) {
                        Toast.makeText(this, "No clear subject in one image", Toast.LENGTH_SHORT).show();
                    } else {
                        it.cutBmp = fg;
                        it.cut = true;
                    }
                    done.run();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Could not remove a background", Toast.LENGTH_SHORT).show();
                    done.run();
                });
    }

    // ------------------------------------------------------------------- saving

    private void save() {
        if (busy != null) return;
        String target = collection;
        if (adding) {
            if (newName.trim().isEmpty()) {
                Toast.makeText(this, "Name the collection", Toast.LENGTH_SHORT).show();
                return;
            }
            target = newName.trim();
        }
        List<Bitmap> bitmaps = new ArrayList<>();
        for (Item i : items) bitmaps.add(i.shown());
        String to = target;
        Context app = getApplicationContext();
        if (bitmaps.size() > 1) Toast.makeText(this, "Adding " + bitmaps.size() + " images...", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            int ok = 0;
            for (Bitmap b : bitmaps) if (StickerStore.save(app, b, to)) ok++;
            final int done = ok;
            runOnUiThread(() -> {
                if (done < bitmaps.size()) {
                    Toast.makeText(this, done + " of " + bitmaps.size() + " added", Toast.LENGTH_SHORT).show();
                }
                if (done > 0) setResult(RESULT_OK);
                finish();
            });
        }).start();
    }

    @Override
    protected void onDestroy() {
        if (segmenter != null) segmenter.close();
        super.onDestroy();
    }

    /** A dark checkerboard: shows that the background is transparent. */
    private static final class CheckerDrawable extends Drawable {
        private final Paint light = new Paint(), dark = new Paint();
        private final int cell;
        private final boolean rounded;

        CheckerDrawable(int cell, boolean rounded) {
            this.cell = Math.max(2, cell);
            this.rounded = rounded;
            light.setColor(0xFF1B1B1B);
            dark.setColor(0xFF0D0D0D);
        }

        @Override
        public void draw(Canvas c) {
            android.graphics.Rect b = getBounds();
            c.drawRect(b, dark);
            for (int y = 0, row = 0; y < b.height(); y += cell, row++) {
                for (int x = (row % 2) * cell; x < b.width(); x += cell * 2) {
                    c.drawRect(b.left + x, b.top + y, Math.min(b.right, b.left + x + cell),
                            Math.min(b.bottom, b.top + y + cell), light);
                }
            }
        }

        @Override public void setAlpha(int alpha) {}
        @Override public void setColorFilter(ColorFilter colorFilter) {}
        @Override public int getOpacity() { return PixelFormat.OPAQUE; }
    }
}

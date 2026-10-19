package com.plainphone.app;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.provider.CalendarContract;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;

import com.google.mlkit.nl.entityextraction.Entity;
import com.google.mlkit.nl.entityextraction.EntityAnnotation;
import com.google.mlkit.nl.entityextraction.EntityExtraction;
import com.google.mlkit.nl.entityextraction.EntityExtractor;
import com.google.mlkit.nl.entityextraction.EntityExtractorOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Last copies. Tap a text clip to paste it; tap an image to view it large (and send it from
 * there); tap the dot in a card's corner to pin it. Hold a clip to select: tap more to add, then
 * pin or delete the selection.
 */
final class ClipboardPanel extends FrameLayout {

    private static final int COLUMNS = 2;

    private final KeyboardRoot.Host host;
    private final LinearLayout main;
    private final GridLayout list;
    private final TextView title, clearBtn, allBtn;
    private final LinearLayout selectBar;
    private final TextView pinBtn, copyBtn;

    // the full-size image viewer drawn over the panel
    private final LinearLayout viewer;
    private final ZoomImageView viewerImage;
    private ClipboardHistory.Item viewing;

    private boolean selecting;
    private final Map<String, ClipboardHistory.Item> selected = new LinkedHashMap<>();
    private List<ClipboardHistory.Item> shown = new ArrayList<>();

    /** What a clip offers to do: a short label and the intent that does it. */
    private static final class Action {
        final String label;
        final Intent intent;

        Action(String label, Intent intent) {
            this.label = label;
            this.intent = intent;
        }
    }

    private final Map<String, Action> actions = new HashMap<>();   // text -> action (null value = none)
    private EntityExtractor extractor;
    private boolean modelReady;

    ClipboardPanel(Context ctx, KeyboardRoot.Host host) {
        super(ctx);
        this.host = host;

        main = new LinearLayout(ctx);
        main.setOrientation(LinearLayout.VERTICAL);
        addView(main, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout head = new LinearLayout(ctx);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setPadding(PanelKit.dp(ctx, 4), 0, 0, PanelKit.dp(ctx, 2));
        title = PanelKit.caption(ctx, "");
        head.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        allBtn = PanelKit.link(ctx, "All", v -> {
            for (ClipboardHistory.Item i : shown) selected.put(key(i), i);
            refresh();
        });
        head.addView(allBtn);
        clearBtn = PanelKit.link(ctx, "Clear", v -> { });
        head.addView(clearBtn);
        main.addView(head, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        ScrollView scroll = new ScrollView(ctx);
        scroll.setVerticalScrollBarEnabled(false);
        list = new GridLayout(ctx);
        list.setColumnCount(COLUMNS);
        scroll.addView(list, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        main.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        // shown while selecting: pin / unpin and delete the selection
        selectBar = new LinearLayout(ctx);
        pinBtn = PanelKit.chip(ctx, "Pin", false, v -> {
            if (selected.isEmpty()) return;
            ClipboardHistory.setPinned(ctx, selected.values(), !allPinned());
            stopSelecting();
        });
        copyBtn = PanelKit.chip(ctx, "Copy", false, v -> {
            if (selected.size() != 1) return;   // one clip at a time: it becomes the system clipboard
            copy(selected.values().iterator().next());
            stopSelecting();
        });
        TextView deleteBtn = PanelKit.chip(ctx, "Delete", true, v -> {
            if (selected.isEmpty()) return;
            ClipboardHistory.remove(ctx, selected.values());
            stopSelecting();
        });
        LinearLayout.LayoutParams a = new LinearLayout.LayoutParams(0, PanelKit.dp(ctx, 38), 1f);
        a.rightMargin = PanelKit.dp(ctx, 4);
        selectBar.addView(copyBtn, a);
        LinearLayout.LayoutParams a2 = new LinearLayout.LayoutParams(0, PanelKit.dp(ctx, 38), 1f);
        a2.rightMargin = PanelKit.dp(ctx, 4);
        selectBar.addView(pinBtn, a2);
        selectBar.addView(deleteBtn, new LinearLayout.LayoutParams(0, PanelKit.dp(ctx, 38), 1f));
        LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        barLp.topMargin = PanelKit.dp(ctx, 4);
        selectBar.setVisibility(GONE);
        main.addView(selectBar, barLp);

        viewer = new LinearLayout(ctx);
        viewer.setOrientation(LinearLayout.VERTICAL);
        viewer.setBackgroundColor(0xFF0A0A0A);
        viewer.setVisibility(GONE);
        viewerImage = new ZoomImageView(ctx);   // pinch to zoom, drag to pan, double-tap to toggle
        FrameLayout stage = new FrameLayout(ctx);
        stage.addView(viewerImage, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        PanelKit.IconView back = new PanelKit.IconView(ctx, KeyIcons.LEFT);
        back.setBackground(UiKit.rounded(ctx, 0xCC161616, 0xFF2B2B2B, 1f, 10));
        back.setOnClickListener(v -> closeViewer());
        FrameLayout.LayoutParams backLp = new FrameLayout.LayoutParams(PanelKit.dp(ctx, 30), PanelKit.dp(ctx, 26));
        backLp.gravity = Gravity.TOP | Gravity.START;
        backLp.topMargin = PanelKit.dp(ctx, 4);
        backLp.leftMargin = PanelKit.dp(ctx, 4);
        stage.addView(back, backLp);
        viewer.addView(stage, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        LinearLayout vActs = new LinearLayout(ctx);
        vActs.setGravity(Gravity.CENTER_HORIZONTAL);
        vActs.addView(viewerButton(ctx, "Delete", false, v -> {
            if (viewing != null) ClipboardHistory.remove(ctx, java.util.Collections.singletonList(viewing));
            closeViewer();
            refresh();
        }));
        vActs.addView(viewerButton(ctx, "Copy", false, v -> {
            if (viewing == null) return;
            copy(viewing);
            closeViewer();
            refresh();
        }));
        vActs.addView(viewerButton(ctx, "Send", true, v -> {
            if (viewing == null) return;
            File image = new File(viewing.image);
            closeViewer();
            host.sendSticker(image);
            host.closePanel();
        }));
        LinearLayout.LayoutParams vLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        vLp.topMargin = PanelKit.dp(ctx, 4);
        viewer.addView(vActs, vLp);
        addView(viewer, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private TextView viewerButton(Context ctx, String label, boolean on, View.OnClickListener l) {
        TextView t = PanelKit.chip(ctx, label, on, l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(PanelKit.dp(ctx, 88), PanelKit.dp(ctx, 34));
        lp.leftMargin = PanelKit.dp(ctx, 3);
        lp.rightMargin = PanelKit.dp(ctx, 3);
        t.setLayoutParams(lp);
        return t;
    }

    void release() {
        if (extractor != null) {
            extractor.close();
            extractor = null;
        }
        modelReady = false;
        closeViewer();
        selecting = false;
        selected.clear();
    }

    private void ensureExtractor() {
        if (extractor != null) return;
        extractor = EntityExtraction.getClient(
                new EntityExtractorOptions.Builder(EntityExtractorOptions.ENGLISH).build());
        EntityExtractor mine = extractor;
        mine.downloadModelIfNeeded()
                .addOnSuccessListener(v -> {
                    if (mine != extractor) return;
                    modelReady = true;
                    refresh();
                });
    }

    private static String key(ClipboardHistory.Item i) {
        return i.isImage() ? "i:" + i.image : "t:" + i.text;
    }

    private boolean allPinned() {
        for (ClipboardHistory.Item i : selected.values()) if (!i.pinned) return false;
        return !selected.isEmpty();
    }

    /** Make a clip the newest copy: it goes on the system clipboard and to the top of the list. */
    private void copy(ClipboardHistory.Item item) {
        if (item.isImage()) host.copyImage(new File(item.image));
        else host.copyText(item.text);
        ClipboardHistory.bump(getContext(), item);
    }

    private void stopSelecting() {
        selecting = false;
        selected.clear();
        refresh();
    }

    /** True while the image viewer or select mode is open: Back should close that, not the keyboard. */
    boolean canGoBack() {
        return viewing != null || selecting;
    }

    void goBack() {
        if (viewing != null) closeViewer();
        else if (selecting) stopSelecting();
    }

    private void closeViewer() {
        viewing = null;
        viewer.setVisibility(GONE);
        viewerImage.setImageBitmap(null);
    }

    private void openViewer(ClipboardHistory.Item item) {
        Bitmap bmp = BitmapFactory.decodeFile(item.image);
        if (bmp == null) return;
        viewing = item;
        viewerImage.setImageBitmap(bmp);
        viewer.setVisibility(VISIBLE);
    }

    void refresh() {
        ensureExtractor();
        Context ctx = getContext();
        List<ClipboardHistory.Item> items = ClipboardHistory.load(ctx);
        shown = items;
        if (selecting) {   // drop selections of clips that are gone
            java.util.Set<String> have = new java.util.HashSet<>();
            for (ClipboardHistory.Item i : items) have.add(key(i));
            selected.keySet().retainAll(have);
        }
        title.setText(selecting
                ? selected.size() + " SELECTED"
                : "LAST " + items.size() + " COPIES · HOLD TO SELECT");
        clearBtn.setText(selecting ? "Done" : "Clear");
        clearBtn.setOnClickListener(v -> {
            if (selecting) {
                stopSelecting();
            } else {
                ClipboardHistory.clearUnpinned(ctx);
                refresh();
            }
        });
        allBtn.setVisibility(selecting ? VISIBLE : GONE);
        selectBar.setVisibility(selecting ? VISIBLE : GONE);
        pinBtn.setText(allPinned() ? "Unpin" : "Pin");
        selectBar.setAlpha(selected.isEmpty() ? 0.4f : 1f);
        copyBtn.setAlpha(selected.size() == 1 ? 1f : 0.4f);
        list.removeAllViews();
        if (items.isEmpty()) {
            TextView empty = new TextView(ctx);
            empty.setText("Nothing copied yet.\nCopied text shows up here.");
            empty.setGravity(Gravity.CENTER);
            empty.setTextSize(12);
            empty.setTextColor(PanelKit.DIM);
            empty.setTypeface(Fonts.cascadiaMono(ctx));
            empty.setPadding(0, PanelKit.dp(ctx, 40), 0, 0);
            list.addView(empty, cell(ctx, COLUMNS));
            return;
        }
        for (ClipboardHistory.Item item : items) list.addView(row(ctx, item), cell(ctx, 1));
    }

    /** A cell {@code span} columns wide, sharing the row's width equally with its siblings. */
    private static GridLayout.LayoutParams cell(Context ctx, int span) {
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams(
                GridLayout.spec(GridLayout.UNDEFINED), GridLayout.spec(GridLayout.UNDEFINED, span, 1f));
        lp.width = 0;
        lp.setMargins(0, 0, PanelKit.dp(ctx, 4), PanelKit.dp(ctx, 4));
        return lp;
    }

    /** One clip as a card: text (or a small picture), a corner dot to pin it, or a check while
     * selecting. */
    private View row(Context ctx, ClipboardHistory.Item item) {
        boolean picked = selected.containsKey(key(item));
        LinearLayout card = new LinearLayout(ctx);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(PanelKit.dp(ctx, 10), PanelKit.dp(ctx, 9), PanelKit.dp(ctx, 10), PanelKit.dp(ctx, 9));
        card.setMinimumHeight(PanelKit.dp(ctx, 76));
        card.setBackground(selecting && picked
                ? UiKit.pressable(ctx, PanelKit.KEY, PanelKit.DOWN, Color.WHITE, 2f, 12)
                : UiKit.pressable(ctx, PanelKit.KEY, PanelKit.DOWN, PanelKit.LINE, 1f, 12));

        if (item.isImage()) {
            // a small picture, kept clear of the corner dot so a white image never hides it
            int h = PanelKit.dp(ctx, 52);
            ImageView iv = new ImageView(ctx);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setImageBitmap(thumb(item.image, h * 2));
            iv.setBackground(UiKit.rounded(ctx, 0xFF0A0A0A, 0xFF333333, 1f, 8));
            UiKit.clipRounded(ctx, iv, 8);
            card.addView(iv, new LinearLayout.LayoutParams(PanelKit.dp(ctx, 52), h));
        } else {
            TextView t = new TextView(ctx);
            t.setText(item.text);
            t.setTextSize(12);
            t.setTextColor(PanelKit.FG);
            t.setTypeface(Fonts.cascadiaMono(ctx));
            t.setGravity(Gravity.START | Gravity.TOP);
            t.setMaxLines(3);
            t.setPadding(0, 0, PanelKit.dp(ctx, 12), 0);
            t.setEllipsize(TextUtils.TruncateAt.END);
            card.addView(t, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            if (!selecting) attachAction(ctx, card, item.text);
        }

        card.setOnClickListener(v -> {
            if (selecting) {
                if (selected.remove(key(item)) == null) selected.put(key(item), item);
                refresh();
            } else if (item.isImage()) {
                openViewer(item);
            } else {
                host.insertText(item.text);
                host.closePanel();
            }
        });
        card.setOnLongClickListener(v -> {   // hold a clip to start selecting
            if (!selecting) {
                selecting = true;
                selected.clear();
            }
            selected.put(key(item), item);
            refresh();
            return true;
        });

        FrameLayout wrap = new FrameLayout(ctx);
        wrap.addView(card, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // corner: a check ring while selecting, otherwise the pin dot (tap to pin / unpin)
        View mark = new View(ctx);
        GradientDrawable dot = new GradientDrawable();
        dot.setShape(GradientDrawable.OVAL);
        boolean filled = selecting ? picked : item.pinned;
        dot.setColor(filled ? 0xFFFFFFFF : 0x00000000);
        dot.setStroke(PanelKit.dp(ctx, 1), filled ? 0xFFFFFFFF : 0xFF555555);
        mark.setBackground(dot);
        int size = PanelKit.dp(ctx, selecting ? 14 : 8);
        FrameLayout hit = new FrameLayout(ctx);   // a bigger touch target than the dot itself
        hit.addView(mark, new FrameLayout.LayoutParams(size, size, Gravity.CENTER));
        if (!selecting) {
            hit.setOnClickListener(v -> {
                ClipboardHistory.setPinned(ctx, java.util.Collections.singletonList(item), !item.pinned);
                refresh();
            });
        }
        FrameLayout.LayoutParams hitLp = new FrameLayout.LayoutParams(PanelKit.dp(ctx, 30), PanelKit.dp(ctx, 30));
        hitLp.gravity = Gravity.TOP | Gravity.END;
        wrap.addView(hit, hitLp);
        return wrap;
    }

    private static Bitmap thumb(String path, int size) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, o);
        int sample = 1;
        while (Math.max(o.outWidth, o.outHeight) / (sample * 2) >= size) sample *= 2;
        BitmapFactory.Options d = new BitmapFactory.Options();
        d.inSampleSize = sample;
        return BitmapFactory.decodeFile(path, d);
    }

    // --------------------------------------------------------------- actions

    /** Show an action chip on the row when the clip holds a URL, phone, email, address or date. */
    private void attachAction(Context ctx, LinearLayout row, String text) {
        if (actions.containsKey(text)) {
            addChip(ctx, row, actions.get(text));
            return;
        }
        if (!modelReady || extractor == null) return;
        EntityExtractor ex = extractor;
        ex.annotate(text).addOnSuccessListener(found -> {
            Action a = actionFor(found);
            actions.put(text, a);
            if (ex == extractor && row.isAttachedToWindow()) addChip(ctx, row, a);
        });
    }

    private void addChip(Context ctx, LinearLayout row, Action a) {
        if (a == null) return;
        TextView tag = new TextView(ctx);   // a small light tag under the text
        tag.setText(a.label);
        tag.setTextSize(10);
        tag.setTextColor(0xFF000000);
        tag.setTypeface(Fonts.cascadiaMono(ctx));
        tag.setPadding(PanelKit.dp(ctx, 7), PanelKit.dp(ctx, 3), PanelKit.dp(ctx, 7), PanelKit.dp(ctx, 3));
        tag.setBackground(UiKit.rounded(ctx, 0xFFCFCFCF, 0xFFCFCFCF, 0f, 6));
        tag.setOnClickListener(v -> {
            try {
                ctx.startActivity(a.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } catch (ActivityNotFoundException | SecurityException ignored) {
            }
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = PanelKit.dp(ctx, 6);
        row.addView(tag, lp);
    }

    private static Action actionFor(List<EntityAnnotation> found) {
        for (EntityAnnotation ann : found) {
            String span = ann.getAnnotatedText();
            for (Entity e : ann.getEntities()) {
                switch (e.getType()) {
                    case Entity.TYPE_URL: {
                        String u = span.contains("://") ? span : "http://" + span;
                        return new Action("Open link", new Intent(Intent.ACTION_VIEW, Uri.parse(u)));
                    }
                    case Entity.TYPE_PHONE:
                        return new Action("Call", new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(span))));
                    case Entity.TYPE_EMAIL:
                        return new Action("Email", new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + span)));
                    case Entity.TYPE_ADDRESS:
                        return new Action("Map", new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(span))));
                    case Entity.TYPE_DATE_TIME: {
                        long when = e.asDateTimeEntity().getTimestampMillis();
                        return new Action("Calendar", new Intent(Intent.ACTION_INSERT)
                                .setData(CalendarContract.Events.CONTENT_URI)
                                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, when));
                    }
                    default:
                }
            }
        }
        return null;
    }
}

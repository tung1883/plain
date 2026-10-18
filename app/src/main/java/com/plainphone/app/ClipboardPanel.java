package com.plainphone.app;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Last copies: tap one to paste it, tap its pin to keep it at the top. */
final class ClipboardPanel extends LinearLayout {

    private static final int COLUMNS = 2;

    private final KeyboardRoot.Host host;
    private final GridLayout list;
    private final TextView title;

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
        setOrientation(VERTICAL);

        LinearLayout head = new LinearLayout(ctx);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setPadding(PanelKit.dp(ctx, 4), 0, 0, PanelKit.dp(ctx, 2));
        title = PanelKit.caption(ctx, "");
        head.addView(title, new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView clearBtn = PanelKit.link(ctx, "Clear", v -> {
            ClipboardHistory.clearUnpinned(ctx);
            refresh();
        });
        head.addView(clearBtn);
        addView(head, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        ScrollView scroll = new ScrollView(ctx);
        scroll.setVerticalScrollBarEnabled(false);
        list = new GridLayout(ctx);
        list.setColumnCount(COLUMNS);
        scroll.addView(list, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        addView(scroll, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
    }

    void release() {
        if (extractor != null) {
            extractor.close();
            extractor = null;
        }
        modelReady = false;
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

    void refresh() {
        ensureExtractor();
        Context ctx = getContext();
        List<ClipboardHistory.Item> items = ClipboardHistory.load(ctx);
        title.setText("LAST " + items.size() + " COPIES · HOLD TO PIN");
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

    /** One clip as a card: top-left text (or a full-width image), a small pin dot in the corner
     * when pinned — the Samsung-keyboard-clipboard look, rather than a whole-card colour swap. */
    private View row(Context ctx, ClipboardHistory.Item item) {
        LinearLayout card = new LinearLayout(ctx);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(PanelKit.dp(ctx, 10), PanelKit.dp(ctx, 9), PanelKit.dp(ctx, 10), PanelKit.dp(ctx, 9));
        card.setMinimumHeight(PanelKit.dp(ctx, 76));
        card.setBackground(UiKit.pressable(ctx, PanelKit.KEY, PanelKit.DOWN, PanelKit.LINE, 1f, 12));

        if (item.isImage()) {
            card.setOnClickListener(v -> {
                host.sendSticker(new File(item.image));
                host.closePanel();
            });
            int h = PanelKit.dp(ctx, 72);
            ImageView iv = new ImageView(ctx);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setImageBitmap(thumb(item.image, h * 2));
            card.addView(iv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, h));
        } else {
            card.setOnClickListener(v -> {
                host.insertText(item.text);
                host.closePanel();
            });
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
            attachAction(ctx, card, item.text);
        }

        card.setOnLongClickListener(v -> {   // hold a copy to pin it to the top, hold again to unpin
            ClipboardHistory.togglePin(ctx, item);
            refresh();
            return true;
        });

        FrameLayout wrap = new FrameLayout(ctx);
        wrap.addView(card, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        View pin = new View(ctx);   // a small dot in the corner: filled when the clip is pinned
        android.graphics.drawable.GradientDrawable dot = new android.graphics.drawable.GradientDrawable();
        dot.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        dot.setColor(item.pinned ? 0xFFFFFFFF : 0x00000000);
        dot.setStroke(1, item.pinned ? 0xFFFFFFFF : 0xFF444444);
        pin.setBackground(dot);
        FrameLayout.LayoutParams pinLp = new FrameLayout.LayoutParams(PanelKit.dp(ctx, 8), PanelKit.dp(ctx, 8));
        pinLp.gravity = Gravity.TOP | Gravity.END;
        pinLp.topMargin = PanelKit.dp(ctx, 8);
        pinLp.rightMargin = PanelKit.dp(ctx, 8);
        wrap.addView(pin, pinLp);
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
        LayoutParams lp = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
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

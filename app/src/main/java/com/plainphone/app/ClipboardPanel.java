package com.plainphone.app;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.CalendarContract;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

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

    private final KeyboardRoot.Host host;
    private final LinearLayout list;
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
        head.setPadding(PanelKit.dp(ctx, 4), 0, PanelKit.dp(ctx, 4), PanelKit.dp(ctx, 4));
        title = PanelKit.caption(ctx, "");
        head.addView(title, new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        head.addView(PanelKit.chip(ctx, "Clear", false, v -> {
            ClipboardHistory.clearUnpinned(ctx);
            refresh();
        }));
        addView(head, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        ScrollView scroll = new ScrollView(ctx);
        scroll.setVerticalScrollBarEnabled(false);
        list = new LinearLayout(ctx);
        list.setOrientation(VERTICAL);
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
        title.setText("LAST " + items.size() + " COPIES - HOLD TO PIN");
        list.removeAllViews();
        if (items.isEmpty()) {
            TextView empty = new TextView(ctx);
            empty.setText("Nothing copied yet.\nCopied text shows up here.");
            empty.setGravity(Gravity.CENTER);
            empty.setTextSize(12);
            empty.setTextColor(PanelKit.DIM);
            empty.setTypeface(Fonts.cascadiaMono(ctx));
            empty.setPadding(0, PanelKit.dp(ctx, 40), 0, 0);
            list.addView(empty);
            return;
        }
        for (ClipboardHistory.Item item : items) list.addView(row(ctx, item));
    }

    private View row(Context ctx, ClipboardHistory.Item item) {
        LinearLayout row = new LinearLayout(ctx);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(PanelKit.dp(ctx, 10), PanelKit.dp(ctx, 8), PanelKit.dp(ctx, 10), PanelKit.dp(ctx, 8));
        row.setBackground(UiKit.pressable(ctx, PanelKit.KEY, PanelKit.DOWN,
                item.pinned ? 0xFFB5B5B5 : PanelKit.LINE, 1f, 8));
        row.setOnClickListener(v -> {
            host.insertText(item.text);
            host.closePanel();
        });

        TextView t = new TextView(ctx);
        t.setText(item.text);
        t.setTextSize(12);
        t.setTextColor(PanelKit.FG);
        t.setTypeface(Fonts.cascadiaMono(ctx));
        t.setMaxLines(2);
        t.setEllipsize(TextUtils.TruncateAt.END);
        row.addView(t, new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        attachAction(ctx, row, item.text);

        row.setOnLongClickListener(v -> {   // hold a copy to pin it to the top, hold again to unpin
            ClipboardHistory.togglePin(ctx, item.text);
            refresh();
            return true;
        });

        LayoutParams lp = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = PanelKit.dp(ctx, 4);
        row.setLayoutParams(lp);
        return row;
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
        TextView chip = PanelKit.chip(ctx, a.label, true, v -> {
            try {
                ctx.startActivity(a.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } catch (ActivityNotFoundException | SecurityException ignored) {
            }
        });
        chip.setTextSize(11);
        LayoutParams lp = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, PanelKit.dp(ctx, 28));
        lp.leftMargin = PanelKit.dp(ctx, 6);
        row.addView(chip, 1 + 0, lp);   // after the text, before the pin
    }

    private static Action actionFor(List<EntityAnnotation> found) {
        for (EntityAnnotation ann : found) {
            String span = ann.getAnnotatedText();
            for (Entity e : ann.getEntities()) {
                switch (e.getType()) {
                    case Entity.TYPE_URL: {
                        String u = span.contains("://") ? span : "http://" + span;
                        return new Action("Open", new Intent(Intent.ACTION_VIEW, Uri.parse(u)));
                    }
                    case Entity.TYPE_PHONE:
                        return new Action("Call", new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(span))));
                    case Entity.TYPE_EMAIL:
                        return new Action("Mail", new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + span)));
                    case Entity.TYPE_ADDRESS:
                        return new Action("Map", new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(span))));
                    case Entity.TYPE_DATE_TIME: {
                        long when = e.asDateTimeEntity().getTimestampMillis();
                        return new Action("Cal", new Intent(Intent.ACTION_INSERT)
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

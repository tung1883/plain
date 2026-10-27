package com.plainphone.app;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Search from the keyboard: files, text inside documents and contacts (Device), notes and to-dos
 * (Notes), and the web. The query is typed in the keyboard's compose bar; a tap on a result inserts
 * it into the app's field.
 */
final class SearchPanel extends LinearLayout {

    /** How many results the strip above the keys shows while typing. */
    static final int LIVE_ROWS = 2;

    private static final KeyboardSearch.Scope[] SCOPES = KeyboardSearch.Scope.values();
    private static final String[] NAMES = {"Device", "Notes", "Web"};

    private final KeyboardRoot.Host host;
    private final ComposeSession.Starter composer;
    private final KeyboardSearch engine = new KeyboardSearch();
    private final TextView queryBar;
    private final LinearLayout chips;
    private final LinearLayout list;
    private final ScrollView scroll;
    private final TextView off;
    private final LinearLayout strip;

    private KeyboardSearch.Scope scope = KeyboardSearch.Scope.DEVICE;
    private String query = "";
    private List<KbHit> hits = new ArrayList<>();
    private boolean indexing, secure, noLearning;
    private final Runnable rerun = this::run;

    SearchPanel(Context ctx, KeyboardRoot.Host host, ComposeSession.Starter composer) {
        super(ctx);
        this.host = host;
        this.composer = composer;
        setOrientation(VERTICAL);

        queryBar = new TextView(ctx);
        queryBar.setTextSize(13);
        queryBar.setTypeface(Fonts.cascadiaMono(ctx));
        queryBar.setSingleLine(true);
        queryBar.setEllipsize(android.text.TextUtils.TruncateAt.END);
        queryBar.setGravity(Gravity.CENTER_VERTICAL);
        queryBar.setMinHeight(PanelKit.dp(ctx, 36));
        queryBar.setPadding(PanelKit.dp(ctx, 12), 0, PanelKit.dp(ctx, 12), 0);
        queryBar.setBackground(UiKit.rounded(ctx, PanelKit.KEY, PanelKit.LINE, 1f, 9));
        queryBar.setOnClickListener(v -> composer.start(session, query));
        LayoutParams qLp = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        qLp.bottomMargin = PanelKit.dp(ctx, 4);
        addView(queryBar, qLp);

        chips = new LinearLayout(ctx);
        LayoutParams cLp = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cLp.bottomMargin = PanelKit.dp(ctx, 4);
        addView(chips, cLp);

        scroll = new ScrollView(ctx);
        scroll.setVerticalScrollBarEnabled(false);
        list = new LinearLayout(ctx);
        list.setOrientation(VERTICAL);
        scroll.addView(list, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        addView(scroll, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        off = new TextView(ctx);
        off.setText("Search is off in password fields");
        off.setTextSize(12);
        off.setGravity(Gravity.CENTER);
        off.setTextColor(PanelKit.DIM);
        off.setTypeface(Fonts.cascadiaMono(ctx));
        off.setVisibility(GONE);
        addView(off, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        strip = new LinearLayout(ctx);
        strip.setOrientation(VERTICAL);
        strip.setPadding(PanelKit.dp(ctx, 6), 0, PanelKit.dp(ctx, 6), PanelKit.dp(ctx, 2));
    }

    /** The kind of field the keyboard is in; search does nothing in a password field. */
    void setField(boolean secure, boolean noLearning) {
        this.secure = secure;
        this.noLearning = noLearning;
    }

    /** Called each time the panel is shown: a fresh, empty search on the scope used last. */
    void refresh() {
        Context ctx = getContext();
        query = "";
        hits = new ArrayList<>();
        scope = lastScope(ctx);
        boolean blocked = secure;
        queryBar.setVisibility(blocked ? GONE : VISIBLE);
        chips.setVisibility(blocked ? GONE : VISIBLE);
        scroll.setVisibility(blocked ? GONE : VISIBLE);
        off.setVisibility(blocked ? VISIBLE : GONE);
        if (blocked) return;
        drawQuery();
        drawChips();
        run();
    }

    void release() {
        engine.cancel();
        removeCallbacks(rerun);
    }

    boolean canGoBack() {
        return !query.isEmpty() && !secure;
    }

    void goBack() {
        query = "";
        drawQuery();
        run();
    }

    // --------------------------------------------------------------- scopes

    private static boolean enabled(Context ctx, KeyboardSearch.Scope s) {
        switch (s) {
            case DEVICE: return Config.isKbSearchDevice(ctx);
            case NOTES: return Config.isKbSearchNotes(ctx);
            default: return Config.isKbSearchWeb(ctx);
        }
    }

    private static KeyboardSearch.Scope lastScope(Context ctx) {
        String saved = Config.getKbSearchScope(ctx);
        KeyboardSearch.Scope first = null;
        for (KeyboardSearch.Scope s : SCOPES) {
            if (!enabled(ctx, s)) continue;
            if (first == null) first = s;
            if (s.name().equalsIgnoreCase(saved)) return s;
        }
        return first == null ? KeyboardSearch.Scope.DEVICE : first;
    }

    private void drawChips() {
        Context ctx = getContext();
        chips.removeAllViews();
        for (KeyboardSearch.Scope s : SCOPES) {
            if (!enabled(ctx, s)) continue;
            boolean on = s == scope;
            String label = NAMES[s.ordinal()];
            if (on && !query.isEmpty()) {
                int n = 0;
                for (KbHit h : hits) if (counted(h)) n++;
                if (n > 0) label += " " + n;
            }
            TextView chip = PanelKit.chipSmall(ctx, label, on, v -> {
                scope = s;
                Config.setKbSearchLastScope(ctx, s.name().toLowerCase());
                hits = new ArrayList<>();
                drawChips();
                run();
            });
            LayoutParams lp = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.rightMargin = PanelKit.dp(ctx, 4);
            chips.addView(chip, lp);
        }
    }

    private static boolean counted(KbHit h) {
        return h.kind != KbHit.Kind.PERMISSION && h.kind != KbHit.Kind.LOCKED && h.kind != KbHit.Kind.HINT;
    }

    // -------------------------------------------------------------- compose

    private final ComposeSession session = new ComposeSession() {
        @Override
        public String hint() {
            return "Search files, notes, web";
        }

        @Override
        public void onChange(String text) {
            if (text.equals(query) && !hits.isEmpty()) return;
            query = text;
            drawQuery();
            run();
        }

        @Override
        public void onDone(String text) {
            query = text;
            drawQuery();
            drawChips();
            render();
        }

        @Override
        public View strip() {
            return strip;
        }
    };

    private void drawQuery() {
        if (query.isEmpty()) {
            queryBar.setText("Search files, notes, web");
            queryBar.setTextColor(PanelKit.DIM);
        } else {
            queryBar.setText(query);
            queryBar.setTextColor(android.graphics.Color.WHITE);
        }
    }

    // -------------------------------------------------------------- results

    private void run() {
        removeCallbacks(rerun);
        if (secure) return;
        final String asked = query;
        engine.search(getContext(), scope, asked, (found, building) -> {
            if (!asked.equals(query)) return;
            hits = found;
            indexing = building;
            drawChips();
            render();
            if (building) postDelayed(rerun, 1500);
        });
    }

    private void render() {
        Context ctx = getContext();
        list.removeAllViews();
        strip.removeAllViews();
        TextMatch.Query q = TextMatch.prepare(query);
        String lastSection = null;
        boolean any = false;
        for (KbHit h : hits) {
            if (h.section != null && !h.section.equals(lastSection)) {
                TextView label = PanelKit.caption(ctx, h.section);
                label.setPadding(PanelKit.dp(ctx, 4), PanelKit.dp(ctx, 6), 0, PanelKit.dp(ctx, 2));
                list.addView(label);
                lastSection = h.section;
            }
            list.addView(row(ctx, h, q));
            any = true;
        }
        if (!any) list.addView(note(ctx, emptyText()));
        int shown = 0;
        for (KbHit h : hits) {
            if (shown >= LIVE_ROWS) break;
            if (!counted(h)) continue;
            strip.addView(row(ctx, h, q));
            shown++;
        }
        if (shown == 0 && !query.isEmpty()) strip.addView(note(ctx, indexing ? "Indexing files..." : "Nothing found"));
    }

    private String emptyText() {
        if (query.isEmpty()) {
            switch (scope) {
                case DEVICE: return "Type to search files, text inside documents and contacts";
                case NOTES: return "Type to search notes and to-dos";
                default: return "Type to search the web";
            }
        }
        return indexing ? "Indexing files..." : "Nothing found";
    }

    private TextView note(Context ctx, String text) {
        TextView t = new TextView(ctx);
        t.setText(text);
        t.setTextSize(12);
        t.setGravity(Gravity.CENTER);
        t.setTextColor(PanelKit.DIM);
        t.setTypeface(Fonts.cascadiaMono(ctx));
        t.setPadding(PanelKit.dp(ctx, 12), PanelKit.dp(ctx, 14), PanelKit.dp(ctx, 12), PanelKit.dp(ctx, 14));
        return t;
    }

    private static int iconFor(KbHit.Kind kind) {
        switch (kind) {
            case FILE: return KeyIcons.FILE;
            case CONTENT: return KeyIcons.LINES;
            case CONTACT: return KeyIcons.PEOPLE;
            case NOTE: return KeyIcons.NOTE;
            case TODO: return KeyIcons.CHECKLIST;
            case RECENT: return KeyIcons.CLOCK;
            case LOCKED: return KeyIcons.PIN;
            case PERMISSION: return KeyIcons.GEAR;
            default: return KeyIcons.GLOBE;
        }
    }

    private View row(Context ctx, KbHit h, TextMatch.Query q) {
        LinearLayout row = new LinearLayout(ctx);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(PanelKit.dp(ctx, 6), PanelKit.dp(ctx, 3), PanelKit.dp(ctx, 2), PanelKit.dp(ctx, 3));
        row.setMinimumHeight(PanelKit.dp(ctx, 44));
        row.setBackground(UiKit.pressable(ctx, 0x00000000, PanelKit.DOWN, 0x00000000, 0f, 9));
        row.setOnClickListener(v -> tap(h));

        PanelKit.IconView icon = new PanelKit.IconView(ctx, iconFor(h.kind));
        icon.setIconScale(0.4f);
        row.addView(icon, new LayoutParams(PanelKit.dp(ctx, 28), PanelKit.dp(ctx, 28)));

        LinearLayout text = new LinearLayout(ctx);
        text.setOrientation(VERTICAL);
        text.setPadding(PanelKit.dp(ctx, 6), 0, PanelKit.dp(ctx, 4), 0);
        TextView title = new TextView(ctx);
        title.setTextSize(13);
        title.setTextColor(PanelKit.FG);
        title.setTypeface(Fonts.cascadiaMono(ctx));
        title.setSingleLine(true);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        title.setText(q.empty ? h.title : Highlight.apply(h.title, q));
        text.addView(title);
        if (h.subtitle != null && !h.subtitle.isEmpty()) {
            TextView sub = new TextView(ctx);
            sub.setTextSize(10.5f);
            sub.setTextColor(PanelKit.DIM);
            sub.setTypeface(Fonts.cascadiaMono(ctx));
            sub.setSingleLine(true);
            sub.setEllipsize(android.text.TextUtils.TruncateAt.END);
            sub.setText(q.empty ? h.subtitle : Highlight.apply(h.subtitle, q));
            text.addView(sub);
        }
        row.addView(text, new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        if (h.file != null) {
            TextView slash = PanelKit.chipSmall(ctx, "/", false, v -> host.insertText(h.file.getAbsolutePath()));
            row.addView(slash, buttonLp(ctx));
            row.addView(button(ctx, KeyIcons.PAPERCLIP, v -> host.attachFile(h.file)), buttonLp(ctx));
        }
        if (h.file != null || h.open != null) {
            row.addView(button(ctx, KeyIcons.RIGHT, v -> openHit(h)), buttonLp(ctx));
        }
        return row;
    }

    private static LayoutParams buttonLp(Context ctx) {
        LayoutParams lp = new LayoutParams(PanelKit.dp(ctx, 30), PanelKit.dp(ctx, 30));
        lp.leftMargin = PanelKit.dp(ctx, 2);
        return lp;
    }

    private static View button(Context ctx, int kind, View.OnClickListener click) {
        PanelKit.IconView b = new PanelKit.IconView(ctx, kind);
        b.setBackground(UiKit.pressable(ctx, PanelKit.KEY, PanelKit.DOWN, PanelKit.LINE, 1f, 9));
        b.setOnClickListener(click);
        return b;
    }

    // -------------------------------------------------------------- actions

    private void tap(KbHit h) {
        if (h.kind == KbHit.Kind.PERMISSION) {
            host.requestAccess(h.access);
        } else if (h.insert != null) {
            host.insertText(h.insert);
        } else {
            openHit(h);
        }
    }

    private void openHit(KbHit h) {
        if (h.kind == KbHit.Kind.WEB && !secure && !noLearning && !query.trim().isEmpty()) {
            KeyboardSearch.saveRecent(getContext(), query);
        }
        host.openHit(h);
    }
}

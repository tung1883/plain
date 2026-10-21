package com.plainphone.app;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** The keyboard's own settings: dev row, auto-close. Applied immediately; height is the toolbar's drag button. */
final class KeyboardSettingsPanel extends LinearLayout {

    private final KeyboardRoot.Host host;
    private final LinearLayout list;
    private final ScrollView scroll;
    private final LanguagesPanel languages;

    KeyboardSettingsPanel(Context ctx, KeyboardRoot.Host host) {
        super(ctx);
        this.host = host;
        setOrientation(VERTICAL);

        scroll = new ScrollView(ctx);
        scroll.setVerticalScrollBarEnabled(false);
        list = new LinearLayout(ctx);
        list.setOrientation(VERTICAL);
        scroll.addView(list, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        addView(scroll, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        languages = new LanguagesPanel(ctx, host, this::goBack);
        languages.setVisibility(GONE);
        addView(languages, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
    }

    /** Show the Languages screen (from the language picker or the Languages row). */
    void openLanguages() {
        scroll.setVisibility(GONE);
        languages.setVisibility(VISIBLE);
        languages.refresh();
    }

    boolean canGoBack() {
        return languages.getVisibility() == VISIBLE;
    }

    void goBack() {
        refresh();
    }

    void refresh() {
        Context ctx = getContext();
        languages.setVisibility(GONE);
        scroll.setVisibility(VISIBLE);
        list.removeAllViews();

        addLink(ctx, "Languages", languageSummary(ctx), "Manage", this::openLanguages);

        boolean devRow = Config.isKbDevRow(ctx);
        addRow(ctx, "Dev row", "Esc Tab Ctrl Alt arrows", devRow ? 0 : 1,
                i -> Config.setKbDevRow(ctx, i == 0));

        boolean pair = Config.isKbAutoPair(ctx);
        addRow(ctx, "Auto-close", "( [ { \" ' type their pair", pair ? 0 : 1,
                i -> Config.setKbAutoPair(ctx, i == 0));

        addSwitch(ctx, "Number row", "On layouts with room for it. Vietnamese VNI always has one.",
                Config.isKbNumRow(ctx), on -> Config.setKbNumRow(ctx, on), true);
        boolean spell = Config.isKbSpell(ctx);
        addSwitch(ctx, "Suggestions", "Next words and phrases, fixes with accents restored (uber to über), auto-capital.",
                spell, on -> Config.setKbSpell(ctx, on), true);
        addSwitch(ctx, "Auto-correct", "Fixes a clear typo on space or punctuation. Backspace right after puts your word back.",
                Config.isKbAutocorrect(ctx), on -> Config.setKbAutocorrect(ctx, on), spell);
        addMix(ctx, spell);
        addLink(ctx, "Red underlines", "Marks unknown words in any app: pick Plainphone as the spell checker", "Set up", this::openSpellCheckerSettings);
    }

    /** Open the phone's spell-checker settings (or, failing that, its keyboard settings). */
    private void openSpellCheckerSettings() {
        Context ctx = getContext();
        for (String action : new String[]{"android.settings.SPELL_CHECKER_SETTINGS", android.provider.Settings.ACTION_INPUT_METHOD_SETTINGS}) {
            try {
                ctx.startActivity(new android.content.Intent(action).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK));
                return;
            } catch (RuntimeException ignored) {
                // this phone has no such screen: try the next
            }
        }
    }

    /** One on/off setting with a switch on the right; dimmed and inert when {@code enabled} is false. */
    private void addSwitch(Context ctx, String title, String hint, boolean on,
                           java.util.function.Consumer<Boolean> set, boolean enabled) {
        LinearLayout row = new LinearLayout(ctx);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(PanelKit.dp(ctx, 4), PanelKit.dp(ctx, 9), PanelKit.dp(ctx, 4), PanelKit.dp(ctx, 9));
        row.addView(textBlock(ctx, title, hint), new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        View sw = PanelKit.toggle(ctx, on, now -> {
            set.accept(now);
            host.settingsChanged();
            refresh();
        });
        row.addView(sw);
        if (!enabled) {
            row.setAlpha(0.4f);
            sw.setEnabled(false);
            sw.setClickable(false);
        }
        list.addView(row, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        divider(ctx);
    }

    /** "Also suggest from": the languages whose words are mixed into suggestions and spell check. */
    private void addMix(Context ctx, boolean enabled) {
        LinearLayout col = new LinearLayout(ctx);
        col.setOrientation(VERTICAL);
        col.setPadding(PanelKit.dp(ctx, 4), PanelKit.dp(ctx, 9), PanelKit.dp(ctx, 4), PanelKit.dp(ctx, 9));
        col.addView(textBlock(ctx, "Mixed typing", "Also suggest words from these languages, tagged in the strip."));
        LinearLayout chips = new LinearLayout(ctx);
        chips.setPadding(0, PanelKit.dp(ctx, 6), 0, 0);
        java.util.List<String> on = new java.util.ArrayList<>(java.util.Arrays.asList(Config.getKbMix(ctx).split(",")));
        for (InputLang l : InputLang.ALL) {
            if (l.id.equals("ko")) continue;
            boolean sel = on.contains(l.id);
            android.widget.TextView c = PanelKit.chipSmall(ctx, l.tag, sel, v -> {
                if (sel) on.remove(l.id);
                else on.add(l.id);
                on.remove("");
                Config.setKbMix(ctx, android.text.TextUtils.join(",", on));
                host.settingsChanged();
                refresh();
            });
            LayoutParams lp = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.rightMargin = PanelKit.dp(ctx, 4);
            chips.addView(c, lp);
        }
        col.addView(chips);
        if (!enabled) {
            col.setAlpha(0.4f);
            for (int i = 0; i < chips.getChildCount(); i++) chips.getChildAt(i).setEnabled(false);
        }
        list.addView(col, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        divider(ctx);
    }

    private LinearLayout textBlock(Context ctx, String title, String hint) {
        LinearLayout text = new LinearLayout(ctx);
        text.setOrientation(VERTICAL);
        TextView t = new TextView(ctx);
        t.setText(title);
        t.setTextSize(13);
        t.setTextColor(PanelKit.FG);
        t.setTypeface(Fonts.cascadiaMono(ctx));
        TextView h = new TextView(ctx);
        h.setText(hint);
        h.setTextSize(10.5f);
        h.setTextColor(PanelKit.DIM);
        h.setTypeface(Fonts.cascadiaMono(ctx));
        text.addView(t);
        text.addView(h);
        return text;
    }

    private void divider(Context ctx) {
        View line = new View(ctx);
        line.setBackgroundColor(0xFF181818);
        list.addView(line, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
    }

    private static String languageSummary(Context ctx) {
        StringBuilder sb = new StringBuilder();
        for (InputLang l : InputLang.parse(Config.getKbLangs(ctx))) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(l.tag);
        }
        return sb.toString();
    }

    /** A row that opens a sub-screen: label and hint on the left, a button on the right. */
    private void addLink(Context ctx, String title, String hint, String button, Runnable open) {
        LinearLayout row = new LinearLayout(ctx);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(PanelKit.dp(ctx, 4), PanelKit.dp(ctx, 9), PanelKit.dp(ctx, 4), PanelKit.dp(ctx, 9));
        LinearLayout text = new LinearLayout(ctx);
        text.setOrientation(VERTICAL);
        TextView t = new TextView(ctx);
        t.setText(title);
        t.setTextSize(13);
        t.setTextColor(PanelKit.FG);
        t.setTypeface(Fonts.cascadiaMono(ctx));
        TextView h = new TextView(ctx);
        h.setText(hint);
        h.setTextSize(10.5f);
        h.setTextColor(PanelKit.DIM);
        h.setTypeface(Fonts.cascadiaMono(ctx));
        text.addView(t);
        text.addView(h);
        row.addView(text, new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(PanelKit.chipSmall(ctx, button, false, v -> open.run()));
        list.addView(row, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        View line = new View(ctx);
        line.setBackgroundColor(0xFF181818);
        list.addView(line, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
    }

    /** One setting: label and hint on the left, an On/Off control on the right. */
    private void addRow(Context ctx, String title, String hint, int selected, java.util.function.IntConsumer set) {
        LinearLayout row = new LinearLayout(ctx);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(PanelKit.dp(ctx, 4), PanelKit.dp(ctx, 9), PanelKit.dp(ctx, 4), PanelKit.dp(ctx, 9));

        LinearLayout text = new LinearLayout(ctx);
        text.setOrientation(VERTICAL);
        TextView t = new TextView(ctx);
        t.setText(title);
        t.setTextSize(13);
        t.setTextColor(PanelKit.FG);
        t.setTypeface(Fonts.cascadiaMono(ctx));
        TextView h = new TextView(ctx);
        h.setText(hint);
        h.setTextSize(10.5f);
        h.setTextColor(PanelKit.DIM);
        h.setTypeface(Fonts.cascadiaMono(ctx));
        text.addView(t);
        text.addView(h);
        row.addView(text, new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        row.addView(PanelKit.segmented(ctx, new String[]{"On", "Off"}, selected, i -> {
            set.accept(i);
            host.settingsChanged();
            refresh();
        }));
        list.addView(row, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        View line = new View(ctx);
        line.setBackgroundColor(0xFF181818);
        list.addView(line, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
    }
}

package com.plainphone.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Connected dev services (GitHub / Vercel / Supabase): add one by pasting a
 * token, then tick the repos / projects a panel should watch. Reachable from
 * {@link DevSettingsActivity}.
 */
public class DevAccountsActivity extends Activity {

    private LinearLayout root;
    private Typeface font;
    private String selectedId;   // the account whose WATCHING list is expanded

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        font = Fonts.current(this);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);
        ScrollView scroller = new ScrollView(this);
        scroller.setBackgroundColor(Color.BLACK);
        scroller.addView(root, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        UiKit.screen(this, "Dev accounts", scroller);
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    private static final String[] KINDS = {DevAccount.GITHUB, DevAccount.VERCEL, DevAccount.SUPABASE};

    private void render() {
        font = Fonts.current(this);
        root.removeAllViews();

        root.addView(row("+ Add account", null, Color.GRAY, v -> pickKind(), null));

        List<DevAccount> accounts = DevAccount.all(this);
        if (accounts.isEmpty()) {
            root.addView(hint("No accounts yet. Paste a Personal Access Token to connect a service."));
        } else {
            for (String kind : KINDS) {
                List<DevAccount> ofKind = DevAccount.ofKind(this, kind);
                if (ofKind.isEmpty()) continue;
                root.addView(sectionLabel(ofKind.get(0).displayKind()));
                for (DevAccount a : ofKind) {
                    root.addView(row(a.label, null, Color.WHITE,
                            v -> { selectedId = a.id.equals(selectedId) ? null : a.id; render(); },
                            () -> confirmRemove(a)));
                    if (a.id.equals(selectedId)) renderWatching(a);
                }
            }
        }

        root.addView(sectionLabel("Section"));
        root.addView(row("Locked: " + (Lock.DEV.isLocked(this) ? "On" : "Off"), null, Color.GRAY,
                v -> Lock.DEV.toggleLock(this, this::render), null));
    }

    // --- WATCHING editor -------------------------------------------------

    private void renderWatching(DevAccount a) {
        root.addView(sectionLabel("Watching"));
        if (DevAccount.SUPABASE.equals(a.kind)) {
            root.addView(hint("Supabase watches its one project (" + a.base + ")."));
            return;
        }
        TextView loading = hint("Loading…");
        root.addView(loading);
        final String token = a.token(this);
        NetIo.POOL.execute(() -> {
            List<String[]> found;   // {key, label}
            String err = null;
            try {
                if (DevAccount.GITHUB.equals(a.kind)) {
                    found = Github.watchCandidates(this, a, token);
                } else if (DevAccount.VERCEL.equals(a.kind)) {
                    found = Vercel.watchCandidates(this, a, token);
                } else {
                    found = new ArrayList<>();
                }
            } catch (Exception e) {
                found = new ArrayList<>();
                err = friendly(e);
            }
            final List<String[]> candidates = found;
            final String error = err;
            NetIo.MAIN.post(() -> {
                if (isFinishing() || !a.id.equals(selectedId)) return;
                int at = root.indexOfChild(loading);
                if (at < 0) return;
                root.removeView(loading);
                if (error != null) { root.addView(hint(error), at); return; }
                if (candidates.isEmpty()) { root.addView(hint("Nothing to watch."), at); return; }
                for (int i = 0; i < candidates.size(); i++) {
                    String[] c = candidates.get(i);
                    root.addView(checkRow(a, c[0], c[1]), at + i);
                }
            });
        });
    }

    private View checkRow(DevAccount a, String key, String label) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(48, 22, 48, 22);
        r.setBackground(pressBg());

        TextView name = new TextView(this);
        name.setText(label);
        name.setTextColor(Color.WHITE);
        name.setTextSize(15);
        name.setTypeface(font);
        r.addView(name, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView mark = new TextView(this);
        mark.setTextSize(14);
        mark.setTypeface(font);
        boolean on = a.isWatched(key);
        mark.setText(on ? "[x]" : "[ ]");
        mark.setTextColor(on ? Color.WHITE : 0xFF484848);
        r.addView(mark);

        r.setOnClickListener(v -> {
            a.toggleWatch(key);
            a.save(this, null);
            boolean now = a.isWatched(key);
            mark.setText(now ? "[x]" : "[ ]");
            mark.setTextColor(now ? Color.WHITE : 0xFF484848);
        });
        return r;
    }

    // --- add flow ------------------------------------------------------

    private void pickKind() {
        final String[] labels = {"GitHub", "Vercel", "Supabase"};
        final String[] kinds = {DevAccount.GITHUB, DevAccount.VERCEL, DevAccount.SUPABASE};
        VaultUi.menu(this, "Add account", labels, new VaultUi.Choice[]{
                () -> promptToken(kinds[0]),
                () -> promptToken(kinds[1]),
                () -> promptToken(kinds[2]),
        });
    }

    private void promptToken(String kind) {
        String hint = DevAccount.GITHUB.equals(kind)
                ? "GitHub personal access token"
                : DevAccount.VERCEL.equals(kind) ? "Vercel token" : "Supabase access token";
        UiKit.textPrompt(this, hint, "", "Connect", token -> {
            String t = token == null ? "" : token.trim();
            if (t.isEmpty()) return;
            if (DevAccount.SUPABASE.equals(kind)) {
                UiKit.textPrompt(this, "Project ref", "", "Connect",
                        ref -> validateAndSave(kind, t, ref == null ? "" : ref.trim()));
            } else {
                validateAndSave(kind, t, "");
            }
        });
    }

    private void validateAndSave(String kind, String token, String ref) {
        Toast.makeText(this, "Checking…", Toast.LENGTH_SHORT).show();
        NetIo.POOL.execute(() -> {
            String label = null, base = "";
            String err = null;
            try {
                if (DevAccount.GITHUB.equals(kind)) {
                    JSONObject o = Http.getObject("https://api.github.com/user",
                            token, Github.HOSTS, Github.headers());
                    label = o.optString("login");
                } else if (DevAccount.VERCEL.equals(kind)) {
                    JSONObject o = Http.getObject("https://api.vercel.com/v2/user",
                            token, Vercel.HOSTS, null);
                    JSONObject u = o.optJSONObject("user");
                    label = u != null ? u.optString("username") : o.optString("username");
                } else {
                    label = Supabase.projectName(token, ref);
                    if (label == null) err = "No project “" + ref + "” for this token.";
                    base = ref;
                }
            } catch (Exception e) {
                err = friendly(e);
            }
            final String fLabel = label, fBase = base, fErr = err;
            NetIo.MAIN.post(() -> {
                if (isFinishing()) return;
                if (fErr != null || fLabel == null || fLabel.isEmpty()) {
                    Toast.makeText(this, fErr != null ? fErr : "Could not verify token",
                            Toast.LENGTH_LONG).show();
                    return;
                }
                DevAccount a = new DevAccount(DevAccount.newId(), kind, fLabel, fBase);
                a.save(this, token);
                selectedId = a.id;
                render();
            });
        });
    }

    private void confirmRemove(DevAccount a) {
        VaultUi.confirm(this, "Remove " + a.displayKind() + "?", "Its saved token is deleted too.",
                "Remove", () -> {
                    DevAccount.remove(this, a.id);
                    if (a.id.equals(selectedId)) selectedId = null;
                    render();
                }, "Cancel", null);
    }

    private static String friendly(Exception e) {
        if (e instanceof Http.HttpException) {
            int s = ((Http.HttpException) e).status;
            if (s == 401 || s == 403) return "Token rejected (" + s + ").";
            if (s == 429) return "Rate limited — try again shortly.";
            return "Service error " + s + ".";
        }
        return "Network error.";
    }

    // --- small views -------------------------------------------------

    private TextView hint(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextColor(Color.GRAY);
        t.setTextSize(13);
        t.setTypeface(font);
        t.setPadding(48, 24, 48, 24);
        return t;
    }

    private TextView sectionLabel(String text) {
        TextView t = new TextView(this);
        t.setText(text.toUpperCase());
        t.setTextColor(0xFF808080);
        t.setTextSize(13);
        t.setLetterSpacing(0.15f);
        t.setTypeface(font);
        t.setPadding(48, 34, 48, 12);
        return t;
    }

    private StateListDrawable pressBg() {
        StateListDrawable bg = new StateListDrawable();
        bg.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Color.DKGRAY));
        bg.addState(new int[]{}, new ColorDrawable(Color.BLACK));
        return bg;
    }

    private View row(String label, String value, int valueColor,
                     View.OnClickListener tap, Runnable longPress) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(48, 30, 48, 30);
        row.setBackground(pressBg());
        row.setOnClickListener(tap);
        if (longPress != null) {
            row.setOnLongClickListener(v -> { longPress.run(); return true; });
        }
        TextView l = new TextView(this);
        l.setText(label);
        l.setTextColor(Color.WHITE);
        l.setTextSize(18);
        l.setTypeface(font);
        row.addView(l);
        if (value != null) {
            TextView v = new TextView(this);
            v.setText(value);
            v.setTextColor(valueColor);
            v.setTextSize(13);
            v.setTypeface(font);
            v.setPadding(0, 6, 0, 0);
            row.addView(v);
        }
        return row;
    }
}

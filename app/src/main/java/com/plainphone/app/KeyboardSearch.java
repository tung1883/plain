package com.plainphone.app;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The keyboard's search engine. It reuses the home screen's matchers and maps what they find to
 * {@link KbHit}s that need no Activity. Queries run on one background thread, debounced, and a
 * result that has been overtaken by a newer query is dropped.
 */
final class KeyboardSearch {

    enum Scope { ALL, DEVICE, NOTES, WEB }

    interface Callback {
        void done(List<KbHit> hits, boolean indexing);
    }

    static final String DUCKDUCKGO = "https://duckduckgo.com/?q=";
    static final int MAX_RECENT = 8;

    private static final String TAG = "PlainKbSearch";
    private static final long DEBOUNCE_MS = 150;
    private static final int CONTENT = 10, NOTES = 8, TODOS = 8, SHOWN_RECENT = 3;
    private static final String RECENT_KEY = "kb_web_recent";

    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService pool = Executors.newSingleThreadExecutor();
    private int token;

    /** Run a search; {@code cb} is called on the main thread unless a newer search or {@link #cancel} came first. */
    void search(Context context, Scope scope, String query, Callback cb) {
        Context app = context.getApplicationContext();
        int mine = ++token;
        main.removeCallbacksAndMessages(null);
        String q = query.trim();
        if (q.isEmpty() && scope != Scope.WEB) {
            cb.done(new ArrayList<>(), false);
            return;
        }
        main.postDelayed(() -> pool.execute(() -> {
            long t0 = System.currentTimeMillis();
            boolean[] indexing = {false};
            List<KbHit> hits;
            try {
                hits = run(app, scope, q, indexing);
            } catch (RuntimeException e) {
                hits = new ArrayList<>();
            }
            List<KbHit> out = hits;
            android.util.Log.d(TAG, scope + " -> " + out.size() + " in " + (System.currentTimeMillis() - t0) + " ms");
            main.post(() -> {
                if (mine == token) cb.done(out, indexing[0]);
            });
        }), scope == Scope.WEB ? 0 : DEBOUNCE_MS);
    }

    void cancel() {
        token++;
        main.removeCallbacksAndMessages(null);
    }

    private static List<KbHit> run(Context ctx, Scope scope, String q, boolean[] indexing) {
        switch (scope) {
            case ALL: return all(ctx, q, indexing);
            case DEVICE: return device(ctx, q, indexing);
            case NOTES: return notes(ctx, q);
            default: return web(ctx, q);
        }
    }

    /** Everything the keyboard can search without leaving the field: notes and to-dos first (few, and the
     *  most likely to be wanted), then files, text inside documents and contacts. Web has its own scope. */
    private static List<KbHit> all(Context ctx, String q, boolean[] indexing) {
        List<KbHit> out = new ArrayList<>(plugins(ctx, q));
        if (Config.isKbSearchNotes(ctx)) out.addAll(notes(ctx, q));
        if (Config.isKbSearchDevice(ctx)) out.addAll(device(ctx, q, indexing));
        return out;
    }

    // ----------------------------------------------------------------- plugins

    /** The plugins that open from the keyboard, with the section lock (if any) that guards each. */
    private static final HomeMode[] PLUGINS = {HomeMode.NOTES, HomeMode.TODOS, HomeMode.RECORDER,
            HomeMode.VAULT, HomeMode.DEV, HomeMode.STATS};

    private static Lock lockOf(HomeMode mode) {
        switch (mode) {
            case NOTES: return Lock.NOTES;
            case TODOS: return Lock.TODOS;
            case RECORDER: return Lock.RECORDER;
            case DEV: return Lock.DEV;
            default: return null;        // the vault asks for its passphrase inside Plain
        }
    }

    /** Extra words a plugin also answers to, besides its label. */
    private static String aliases(HomeMode mode) {
        switch (mode) {
            case TODOS: return "todo todos tasks checklist";
            case RECORDER: return "recorder recording record audio voice";
            case VAULT: return "vault safe private secret";
            case DEV: return "dev developer shell terminal ssh";
            case STATS: return "stats statistics usage screen time";
            default: return "";
        }
    }

    private static List<KbHit> plugins(Context ctx, String raw) {
        List<KbHit> out = new ArrayList<>();
        List<HomeMode> shown = Config.getHomeModeOrder(ctx);          // hidden plugins stay hidden
        String q = TextMatch.fold(raw.trim());
        if (q.isEmpty()) return out;
        for (HomeMode mode : PLUGINS) {
            if (!shown.contains(mode)) continue;
            String hay = TextMatch.fold(mode.label + " " + aliases(mode));
            boolean hit = false;
            for (String word : hay.split(" ")) if (!word.isEmpty() && word.startsWith(q)) hit = true;
            if (!hit) continue;
            Lock lock = lockOf(mode);
            boolean closed = lock != null && lock.gateActive(ctx);
            Intent open = new Intent(ctx, MainActivity.class).putExtra(MainActivity.EXTRA_OPEN_MODE, mode.name());
            out.add(KbHit.plugin(mode.label,
                    closed ? (Config.isPinSet(ctx, lock.area) ? "Locked. Tap to unlock with PIN" : "Locked. Opens in Plain")
                            : mode == HomeMode.VAULT ? "Opens in Plain, asks for the passphrase" : "Plugin. Tap to open",
                    closed && Config.isPinSet(ctx, lock.area) ? lock : null, open));
        }
        return out;
    }

    // ------------------------------------------------------------------ device

    private static List<KbHit> device(Context ctx, String raw, boolean[] indexing) {
        List<KbHit> out = new ArrayList<>();
        ParsedQuery parsed = QueryParser.parse(raw);
        boolean files = FileIndex.canWalk(ctx) || DeviceSearch.canSearchFiles(ctx);
        boolean contacts = DeviceSearch.canSearchContacts(ctx);

        if (files) {
            for (SearchResult r : DeviceSearch.files(ctx, parsed)) {
                if (!(r.payload instanceof FileIndex.Entry)) continue;   // MediaStore rows have no path to insert
                FileIndex.Entry e = (FileIndex.Entry) r.payload;
                out.add(KbHit.file(e.file, fileSubtitle(DeviceSearch.describeLocation(e.file.getParentFile()),
                        e.directory ? -1 : e.file.length())));
            }
            if (out.isEmpty() && FileIndex.canWalk(ctx) && FileIndex.isScanning()) indexing[0] = true;
        }
        if (files && Config.isContentSearchEnabled(ctx)) {
            for (SearchResult r : ContentSearch.search(ctx, parsed, CONTENT)) {
                ContentHit h = (ContentHit) r.payload;
                String where = h.locator.isEmpty() ? h.folder : h.folder + " · " + h.locator;
                out.add(KbHit.content(h.file, where, cleanSnippet(h.snippet)));
            }
        }
        if (contacts) {
            for (SearchResult r : DeviceSearch.contacts(ctx, parsed.core)) {
                DeviceSearch.Contact c = (DeviceSearch.Contact) r.payload;
                Intent view = new Intent(Intent.ACTION_VIEW,
                        ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, c.id));
                List<String> numbers = DeviceSearch.phoneNumbers(ctx, c.id);
                if (numbers.isEmpty()) {
                    out.add(KbHit.contact(c.name, "Contact", c.name, view));
                } else {
                    for (String n : numbers) out.add(KbHit.contact(c.name, n, n, view));
                }
            }
        }
        if (!files) out.add(KbHit.permission("Allow file access", "files"));
        if (!contacts) out.add(KbHit.permission("Allow contacts", "contacts"));
        return out;
    }

    // ------------------------------------------------------------------- notes

    private static List<KbHit> notes(Context ctx, String raw) {
        List<KbHit> out = new ArrayList<>();
        Intent home = new Intent(ctx, MainActivity.class);
        TextMatch.Query q = TextMatch.prepare(raw);
        if (Lock.NOTES.gateActive(ctx)) {
            out.add(KbHit.locked("Notes are locked", Config.isPinSet(ctx, Lock.NOTES.area) ? Lock.NOTES : null, home));
        } else {
            List<Note> found = new ArrayList<>();
            List<Integer> scores = new ArrayList<>();
            for (Note n : Config.getNotes(ctx)) {   // never the vault's notes
                int score = TextMatch.score(n.text, q);
                if (score == TextMatch.NO_MATCH) continue;
                int at = 0;
                while (at < scores.size() && scores.get(at) <= score) at++;
                found.add(at, n);
                scores.add(at, score);
            }
            for (int i = 0; i < found.size() && i < NOTES; i++) {
                Note n = found.get(i);
                String preview = n.preview();
                out.add(KbHit.note(n.title(), preview == null ? "" : preview, n.text,
                        new Intent(ctx, NoteEditActivity.class).putExtra("noteId", n.id)));
            }
        }
        if (Lock.TODOS.gateActive(ctx)) {
            out.add(KbHit.locked("To-do is locked", Config.isPinSet(ctx, Lock.TODOS.area) ? Lock.TODOS : null, home));
        } else {
            boolean showDone = Config.isTodosShowCompleted(ctx);
            int added = 0;
            for (Todo t : Todos.load(ctx)) {
                if (added >= TODOS) break;
                if (t.done && !showDone) continue;
                if (TextMatch.score(t.description, q) == TextMatch.NO_MATCH) continue;
                out.add(KbHit.todo(t.displayText(), t.done ? "Done" : "To-do", home));
                added++;
            }
        }
        return out;
    }

    // --------------------------------------------------------------------- web

    private static List<KbHit> web(Context ctx, String q) {
        List<KbHit> out = new ArrayList<>();
        String browser = WebSearch.defaultBrowser(ctx);
        if (!q.isEmpty()) {
            out.add(KbHit.web(q, "DuckDuckGo", viewIntent(DUCKDUCKGO + Uri.encode(q), browser)));
            for (WebTarget t : Config.getWebTargets(ctx)) {
                if (!WebTarget.hasPlaceholder(t.url)) continue;
                out.add(KbHit.web(q, WebTarget.nameOrHost(t.name, t.url), viewIntent(t.urlFor(q), browser)));
            }
        }
        int shown = 0;
        for (String r : loadRecent(ctx)) {
            if (!q.isEmpty() && !TextMatch.fold(r).contains(TextMatch.fold(q))) continue;
            if (!q.isEmpty() && shown++ >= SHOWN_RECENT) break;
            out.add(KbHit.recent(r, viewIntent(DUCKDUCKGO + Uri.encode(r), browser)));
        }
        return out;
    }

    private static Intent viewIntent(String url, String browser) {
        Intent view = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        if (browser != null) view.setPackage(browser);
        return view;
    }

    static List<String> loadRecent(Context ctx) {
        String stored = PanelKit.prefs(ctx).getString(RECENT_KEY, "");
        List<String> out = new ArrayList<>();
        for (String s : stored.split("\n")) if (!s.isEmpty()) out.add(s);
        return out;
    }

    static void saveRecent(Context ctx, String query) {
        PanelKit.prefs(ctx).edit()
                .putString(RECENT_KEY, String.join("\n", pushRecent(loadRecent(ctx), query, MAX_RECENT)))
                .apply();
    }

    // -------------------------------------------------------- pure helpers (unit tested)

    /** {@code recent} with {@code query} moved to the front, no repeats, at most {@code cap} long. */
    static List<String> pushRecent(List<String> recent, String query, int cap) {
        String q = query.replace('\n', ' ').trim();
        if (q.isEmpty()) return new ArrayList<>(recent);
        List<String> out = new ArrayList<>();
        out.add(q);
        for (String r : recent) {
            if (!r.equalsIgnoreCase(q) && out.size() < cap) out.add(r);
        }
        return out;
    }

    /** A content snippet as plain text to insert: no leading or trailing ellipsis. */
    static String cleanSnippet(String snippet) {
        if (snippet == null) return "";
        String s = snippet.trim();
        while (s.startsWith("...") || s.startsWith("…")) s = s.substring(s.startsWith("...") ? 3 : 1).trim();
        while (s.endsWith("...") || s.endsWith("…")) s = s.substring(0, s.length() - (s.endsWith("...") ? 3 : 1)).trim();
        return s;
    }

    /** "Internal/Documents · 1.2 MB"; a negative size (a folder) leaves the size out. */
    static String fileSubtitle(String location, long bytes) {
        if (bytes < 0) return location;
        return location + " · " + size(bytes);
    }

    static String size(long bytes) {
        if (bytes < 1024) return bytes + " B";
        double v = bytes / 1024.0;
        String[] units = {"KB", "MB", "GB"};
        int u = 0;
        while (v >= 1024 && u < units.length - 1) {
            v /= 1024;
            u++;
        }
        return String.format(Locale.ROOT, v < 10 ? "%.1f %s" : "%.0f %s", v, units[u]);
    }
}

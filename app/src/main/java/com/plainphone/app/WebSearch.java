package com.plainphone.app;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * The last resort in search results: hand the query to the default browser.
 *
 * <p>A typed address opens directly; anything else becomes a search on the configured
 * engine. The browser is launched through exactly the same time-block, lock, and flag gates
 * as tapping its icon would be — a one-tap route to the web that skipped them would quietly
 * undo the restrictions the rest of the app exists to enforce.
 */
class WebSearch {

    private WebSearch() {}

    /**
     * Looks like an address rather than something to search for: no spaces, and either an
     * explicit scheme or a dotted name whose final segment is alphabetic, like a TLD. That
     * last requirement is what keeps "3.5" a search rather than an attempt to visit
     * https://3.5.
     *
     * <p>It can't be exact without a TLD list — "file.txt" is read as an address — but the
     * web row always sits last, below the actual file, so guessing wrong costs one
     * ignorable row rather than a wrong result.
     */
    private static final Pattern LOOKS_LIKE_URL = Pattern.compile(
            "^(https?://\\S+|[\\w-]+(\\.[\\w-]+)*\\.[a-z]{2,}(/\\S*)?)$",
            Pattern.CASE_INSENSITIVE);

    /** Query placeholder in a search engine's URL template. */
    private static final String QUERY_TOKEN = "%s";

    /**
     * Resolving the default browser and loading its label are both PackageManager calls, and
     * this runs on every keystroke — so the answer is cached. The default changes only when
     * the user changes it, which {@link #forget()} covers on return to the home screen.
     */
    private static String cachedBrowser;
    private static String cachedLabel;

    /** Drops the cached browser, so a default changed while away is picked up. */
    static void forget() {
        cachedBrowser = null;
        cachedLabel = null;
    }

    /**
     * The rows offered under the "Web" heading: the default browser search first, then one
     * per user-defined search. Empty when there's no browser to open them with.
     */
    static List<SearchResult> results(Activity host, TextMatch.Query query) {
        List<SearchResult> results = new ArrayList<>();
        if (query.empty) return results;

        if (cachedBrowser == null) {
            cachedBrowser = defaultBrowser(host);
            if (cachedBrowser == null) return results;
            cachedLabel = labelOf(host, cachedBrowser);
        }
        String browser = cachedBrowser;

        String typed = query.raw;
        boolean isUrl = LOOKS_LIKE_URL.matcher(typed).matches();
        Intent target = targetIntent(host, browser, typed, isUrl);

        // Just the words typed: the group heading already says this is a web search, and
        // the subtitle names where it goes, so a "Search the web for ..." prefix only
        // repeats them and pushes the actual query off the end of a narrow row.
        results.add(new SearchResult(SearchResult.Kind.WEB, typed, cachedLabel, 0,
                () -> open(host, browser, target)));

        // Ranked below the plain browser search, in the order they were added — a custom
        // search is a deliberate shortcut, not a better guess at what was meant.
        int rank = 1;
        for (WebTarget custom : Config.getWebTargets(host)) {
            if (!WebTarget.hasPlaceholder(custom.url)) continue;
            Intent view = new Intent(Intent.ACTION_VIEW, Uri.parse(custom.urlFor(typed)));
            view.setPackage(browser);
            results.add(new SearchResult(SearchResult.Kind.WEB, typed,
                    WebTarget.nameOrHost(custom.name, custom.url), rank++,
                    () -> open(host, browser, view)));
        }
        return results;
    }

    /**
     * What the browser should be asked to do.
     *
     * <p>For a search this is ACTION_WEB_SEARCH, which hands over the words and lets the
     * browser run them through <i>its own</i> configured search engine — Android exposes no
     * way to read another app's engine setting, so delegating is the only way to honour it.
     * Browsers that don't accept ACTION_WEB_SEARCH fall back to the URL template in Config,
     * which is why that setting still exists.
     */
    private static Intent targetIntent(Activity host, String browser, String typed, boolean isUrl) {
        if (!isUrl) {
            Intent search = new Intent(Intent.ACTION_WEB_SEARCH);
            search.putExtra(SearchManager.QUERY, typed);
            search.setPackage(browser);
            if (search.resolveActivity(host.getPackageManager()) != null) return search;
        }

        String url = isUrl ? asUrl(typed) : searchUrl(host, typed);
        Intent view = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        view.setPackage(browser);
        return view;
    }

    /**
     * Opens the address, routed through whichever gate currently applies to the browser.
     * This mirrors MainActivity.launchApp's precedence: a time block outranks a lock, which
     * outranks a flag.
     */
    private static void open(Activity host, String browser, Intent target) {
        TimeBlock blockingBlock = TimeBlockRules.getBlockingBlock(host, browser);
        if (blockingBlock != null) {
            Intent gate = new Intent(host, TimeBlockGateActivity.class);
            gate.putExtra("package", browser);
            gate.putExtra("blockId", blockingBlock.id);
            host.startActivity(gate);
            return;
        }

        String label = labelOf(host, browser);
        if (Config.getLockedPackages(host).contains(browser)) {
            host.startActivity(gateIntent(host, PinGateActivity.class, browser, label, target));
            return;
        }
        if (Config.getFlaggedPackages(host).contains(browser)) {
            host.startActivity(gateIntent(host, FlaggedGateActivity.class, browser, label, target));
            return;
        }

        try {
            host.startActivity(target);
        } catch (Exception e) {
            // The resolved browser refused it — drop the package restriction and let the
            // system offer whatever else can handle it, rather than losing the tap silently.
            Intent anyApp = new Intent(target);
            anyApp.setPackage(null);
            try {
                host.startActivity(anyApp);
            } catch (Exception ignored) {
                android.widget.Toast.makeText(host, "No app can open this",
                        android.widget.Toast.LENGTH_SHORT).show();
            }
        }
    }

    private static Intent gateIntent(Activity host, Class<?> gate, String browser,
                                     String label, Intent target) {
        Intent intent = new Intent(host, gate);
        intent.putExtra("package", browser);
        intent.putExtra("label", label);
        // Carried through the gate so the query survives the wait or the PIN prompt. Intent
        // is Parcelable, and the gates are not exported, so nothing outside Plain can inject
        // one of these.
        intent.putExtra(OPEN_INTENT, target);
        return intent;
    }

    /** Extra naming the Intent a gate should run once it's passed. */
    static final String OPEN_INTENT = "openIntent";

    private static String searchUrl(Activity host, String typed) {
        return Config.getSearchEngine(host).replace(QUERY_TOKEN, Uri.encode(typed));
    }

    private static String asUrl(String typed) {
        String lower = typed.toLowerCase(Locale.ROOT);
        return lower.startsWith("http://") || lower.startsWith("https://")
                ? typed
                : "https://" + typed;
    }

    /**
     * The package Android would itself hand a web address to. Resolving a concrete http URL
     * (rather than asking for a browser category) is what gets the user's actual default
     * instead of a chooser.
     */
    static String defaultBrowser(Activity host) {
        Intent probe = new Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com"));
        ResolveInfo resolved = host.getPackageManager()
                .resolveActivity(probe, PackageManager.MATCH_DEFAULT_ONLY);
        if (resolved == null || resolved.activityInfo == null) return null;

        String packageName = resolved.activityInfo.packageName;
        // With no default set, Android resolves to its chooser stub; opening that is fine,
        // but it isn't a package that can be gated or named, so treat it as "no browser".
        return "android".equals(packageName) ? null : packageName;
    }

    private static String labelOf(Activity host, String packageName) {
        try {
            PackageManager pm = host.getPackageManager();
            return pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString();
        } catch (Exception e) {
            return "Browser";
        }
    }
}

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

class WebSearch {

    private WebSearch() {}

    private static final Pattern LOOKS_LIKE_URL = Pattern.compile(
            "^(https?://\\S+|[\\w-]+(\\.[\\w-]+)*\\.[a-z]{2,}(/\\S*)?)$",
            Pattern.CASE_INSENSITIVE);

    private static final String QUERY_TOKEN = "%s";

    private static String cachedBrowser;
    private static String cachedLabel;

    static void forget() {
        cachedBrowser = null;
        cachedLabel = null;
    }

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

        results.add(new SearchResult(SearchResult.Kind.WEB, typed, cachedLabel, 0,
                () -> open(host, browser, target)));

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

        intent.putExtra(OPEN_INTENT, target);
        return intent;
    }

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

    static String defaultBrowser(Activity host) {
        Intent probe = new Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com"));
        ResolveInfo resolved = host.getPackageManager()
                .resolveActivity(probe, PackageManager.MATCH_DEFAULT_ONLY);
        if (resolved == null || resolved.activityInfo == null) return null;

        String packageName = resolved.activityInfo.packageName;

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


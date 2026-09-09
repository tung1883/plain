package com.plainphone.app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/** A minimal browser window: a URL bar and a {@link WebView}. */
final class WebPanel implements PanelContent {

    private static final String HOME = "https://duckduckgo.com/";

    private WebView web;
    private EditText urlBar;
    private String title = "Web";
    private String currentUrl;
    private Runnable onTitle;

    WebPanel() {}

    WebPanel(String startUrl) {
        if (startUrl != null && !startUrl.isEmpty()) this.currentUrl = startUrl;
    }

    @Override public void setTitleListener(Runnable r) { this.onTitle = r; }

    @Override public String kind() { return "web"; }

    @Override public String saveExtra() { return currentUrl != null ? currentUrl : ""; }

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override
    public android.view.View onCreate(Context ctx) {
        LinearLayout col = new LinearLayout(ctx);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setBackgroundColor(Color.BLACK);

        LinearLayout bar = new LinearLayout(ctx);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setBackgroundColor(0xFF0E0E0E);
        bar.setPadding(dp(ctx, 10), dp(ctx, 6), dp(ctx, 6), dp(ctx, 6));

        urlBar = new EditText(ctx);
        urlBar.setSingleLine(true);
        urlBar.setHint("search or url");
        urlBar.setHintTextColor(Color.GRAY);
        urlBar.setTextColor(Color.WHITE);
        urlBar.setTextSize(12.5f);
        urlBar.setTypeface(Fonts.cascadiaMono(ctx));
        urlBar.setBackground(null);
        urlBar.setImeOptions(EditorInfo.IME_ACTION_GO);
        urlBar.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_URI
                | android.text.InputType.TYPE_CLASS_TEXT);
        urlBar.setOnEditorActionListener((v, id, e) -> { go(urlBar.getText().toString()); return true; });
        bar.addView(urlBar, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        bar.addView(iconButton(ctx, "‹", () -> { if (web.canGoBack()) web.goBack(); }));
        bar.addView(iconButton(ctx, "⟳", () -> web.reload()));

        col.addView(bar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        web = new WebView(ctx);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setLoadWithOverviewMode(true);
        web.getSettings().setUseWideViewPort(true);
        web.getSettings().setSupportZoom(true);
        web.getSettings().setBuiltInZoomControls(true);
        web.getSettings().setDisplayZoomControls(false);
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                return false; // keep navigation inside the panel
            }
            @Override public void onPageFinished(WebView v, String url) {
                urlBar.setText(url);
                currentUrl = url;
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public void onReceivedTitle(WebView v, String t) {
                if (t != null && !t.isEmpty()) {
                    title = t.length() > 40 ? t.substring(0, 39) + "…" : t;
                    if (onTitle != null) onTitle.run();
                }
            }
        });
        col.addView(web, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        web.loadUrl(currentUrl != null ? currentUrl : HOME);
        return col;
    }

    private void go(String raw) {
        String s = raw.trim();
        if (s.isEmpty()) return;
        String url;
        if (s.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*")) {
            url = s;
        } else if (s.contains(".") && !s.contains(" ")) {
            url = "https://" + s;
        } else {
            url = "https://duckduckgo.com/?q=" + android.net.Uri.encode(s);
        }
        web.loadUrl(url);
        web.requestFocus();
    }

    @Override public String title() { return title; }

    @Override public void onLeave() { /* keep the page loaded across a workspace peek */ }

    @Override public void onClose() {
        if (web != null) {
            web.stopLoading();
            web.loadUrl("about:blank");
            web.destroy();
            web = null;
        }
    }

    private TextView iconButton(Context ctx, String glyph, Runnable action) {
        TextView t = new TextView(ctx);
        t.setText(glyph);
        t.setTextColor(0xFFC0C0C0);
        t.setTextSize(16);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(ctx, 12), dp(ctx, 2), dp(ctx, 12), dp(ctx, 4));
        t.setOnClickListener(v -> action.run());
        return t;
    }

    private static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }
}

package com.plainphone.app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import java.util.Map;

/**
 * Shared plumbing for the device-metric panels (Stats / Network / Storage): the
 * {@link DevConnection} channel, a poll timer that pauses when the panel is
 * hidden (exactly {@link ProcSurface}'s lifecycle), and a scrolling body the
 * subclass fills from each response.
 */
@SuppressLint("ViewConstructor")
abstract class MetricSurface extends LinearLayout {

    protected final Context ctx;
    protected final Typeface font;

    private DevConnection connection;
    private long channel = -1;
    private boolean opening;
    private boolean shown = true;

    private final Handler poll = new Handler();
    private boolean polling;
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            request();
            poll.postDelayed(this, pollMs());
        }
    };

    protected final LinearLayout body;
    private final View spinner;
    private boolean gotFirst;

    private final DevConnection.Sink sink = this::onChannelMessage;

    MetricSurface(Context ctx) {
        super(ctx);
        this.ctx = ctx;
        this.font = Fonts.cascadiaMono(ctx);
        setOrientation(VERTICAL);
        setBackgroundColor(Color.BLACK);

        FrameLayout wrap = new FrameLayout(ctx);
        ScrollView scroll = new ScrollView(ctx);
        scroll.setFillViewport(true);
        body = new LinearLayout(ctx);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(18), dp(14), dp(18), dp(18));
        scroll.addView(body, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        wrap.addView(scroll, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        spinner = UiKit.spinner(ctx);
        wrap.addView(spinner, new FrameLayout.LayoutParams(dp(24), dp(24), Gravity.CENTER));

        addView(wrap, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
    }

    // --- subclass hooks ------------------------------------------------

    /** The request frame to send each poll. */
    abstract Map<String, Object> requestFrame(long ch);

    /** The response {@code "t"} to react to. */
    abstract String responseType();

    /** Fill {@link #body} from one response frame (already the outer map). */
    abstract void render(Map<String, Object> frame);

    long pollMs() { return 2500; }

    // --- connection lifecycle (mirrors ProcSurface) ------------------

    void attach(DevConnection conn) {
        if (conn == null) {
            connection = null;
            channel = -1;
            opening = false;
            stopPoll();
            return;
        }
        if (connection != null && connection != conn) {
            connection = null;
            channel = -1;
            opening = false;
        }
        if (!opening && channel < 0) {
            connection = conn;
            opening = true;
            channel = conn.openChannel(sink);
        } else {
            connection = conn;
        }
        startPoll();
    }

    void detach() {
        stopPoll();
        if (channel >= 0 && connection != null) connection.closeChannel(channel);
        channel = -1;
        opening = false;
    }

    void setShown(boolean visible) {
        shown = visible;
        if (visible) startPoll();
        else stopPoll();
    }

    private void startPoll() {
        if (polling || !shown || channel < 0 || connection == null) return;
        polling = true;
        poll.removeCallbacks(tick);
        poll.post(tick);
    }

    private void stopPoll() {
        polling = false;
        poll.removeCallbacks(tick);
    }

    private void request() {
        if (channel >= 0 && connection != null) connection.send(requestFrame(channel));
    }

    @SuppressWarnings("unchecked")
    private void onChannelMessage(Map<String, Object> msg) {
        if (!responseType().equals(DevProtocol.type(msg))) return;
        gotFirst = true;
        spinner.setVisibility(View.GONE);
        body.removeAllViews();
        try {
            render(msg);
        } catch (RuntimeException e) {
            // a malformed frame shouldn't take the panel down
        }
    }

    // --- small view helpers for subclasses --------------------------

    protected LinearLayout card() {
        LinearLayout c = new LinearLayout(ctx);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackground(UiKit.rounded(ctx, 0xFF0B0B0B, 0xFF1C1C1C, 1f, UiKit.R_MD));
        c.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(10);
        c.setLayoutParams(lp);
        return c;
    }

    protected android.widget.TextView header(String text) {
        android.widget.TextView t = new android.widget.TextView(ctx);
        t.setText(text.toUpperCase());
        t.setTextColor(0xFF808080);
        t.setTextSize(12);
        t.setLetterSpacing(0.12f);
        t.setTypeface(font);
        t.setPadding(0, dp(14), 0, dp(8));
        return t;
    }

    protected android.widget.TextView line(String text, int color) {
        android.widget.TextView t = new android.widget.TextView(ctx);
        t.setText(text);
        t.setTextColor(color);
        t.setTextSize(13);
        t.setTypeface(font);
        t.setPadding(0, dp(3), 0, dp(3));
        return t;
    }

    protected View keyval(String k, String v, int valueColor) {
        LinearLayout r = new LinearLayout(ctx);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setPadding(0, dp(6), 0, dp(6));
        android.widget.TextView kt = new android.widget.TextView(ctx);
        kt.setText(k);
        kt.setTextColor(Color.WHITE);
        kt.setTextSize(14);
        kt.setTypeface(font);
        r.addView(kt, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        android.widget.TextView vt = new android.widget.TextView(ctx);
        vt.setText(v);
        vt.setTextColor(valueColor);
        vt.setTextSize(13);
        vt.setTypeface(font);
        vt.setGravity(Gravity.END);
        r.addView(vt);
        return r;
    }

    protected int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}

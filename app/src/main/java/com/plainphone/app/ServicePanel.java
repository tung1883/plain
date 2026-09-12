package com.plainphone.app;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/**
 * Base for the cloud-service panels (GitHub / Vercel / Supabase). Like
 * {@link VaultPanel} it implements {@link PanelContent} directly (not the
 * home-section-bound {@link PluginPanel}); it owns a {@link SectionListView}, a
 * {@link LockGate} for {@link Lock#DEV}, a slow poll while visible, a {@code ↻}
 * title button, and the shared {@link NetIo} plumbing with a generation guard.
 *
 * <p>Subclasses supply the account kind, a poll interval, and {@link #load} —
 * one blocking call on {@link NetIo#POOL} returning a list of
 * {@link ServiceData.Section}.
 */
abstract class ServicePanel implements PanelContent {

    protected Context ctx;
    private FrameLayout root;
    protected SectionListView list;
    private LockGate gate;
    private View spinner;
    private TextView error;
    private Runnable onTitleChanged;

    protected final String accountId;
    private final Handler poll = new Handler();
    private boolean shown, polling;
    private int generation;

    private List<ServiceData.Section> data;

    ServicePanel(String accountId) {
        this.accountId = (accountId == null || accountId.isEmpty()) ? null : accountId;
    }

    /** {@code "github"} / {@code "vercel"} / {@code "supabase"}. */
    abstract String accountKind();

    /** Display name, e.g. {@code "GitHub"}. */
    abstract String serviceName();

    @Override
    public String title() {
        DevAccount a = ctx == null ? null : account();
        return (a != null && a.label != null && !a.label.isEmpty())
                ? a.label + " · " + accountKind() : serviceName();
    }

    /** Poll interval while the panel is visible. */
    abstract long pollMs();

    /** Blocking — runs on {@link NetIo#POOL}. */
    abstract List<ServiceData.Section> load(DevAccount account, String token) throws Exception;

    // --- PanelContent ---------------------------------------------------

    @Override public final String saveExtra() { return accountId == null ? "" : accountId; }
    @Override public boolean needsConnection() { return false; }
    @Override public String hostId() { return null; }
    @Override public void setTitleListener(Runnable r) { this.onTitleChanged = r; }

    /** Optional view pinned above the row list (e.g. GitHub's contribution graph). */
    protected View header(Context ctx) { return null; }

    @Override
    public View onCreate(Context ctx) {
        this.ctx = ctx;
        root = new FrameLayout(ctx);

        list = new SectionListView(ctx);
        list.setProvider(this::fillRows);

        View header = header(ctx);
        if (header != null) {
            LinearLayout body = new LinearLayout(ctx);
            body.setOrientation(LinearLayout.VERTICAL);
            body.addView(header, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            body.addView(list, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
            root.addView(body, mm());
        } else {
            root.addView(list, mm());
        }

        error = new TextView(ctx);
        error.setTextColor(0xFFC88F87);
        error.setTextSize(12);
        error.setTypeface(Fonts.current(ctx));
        error.setPadding(dp(18), dp(8), dp(18), dp(8));
        error.setVisibility(View.GONE);
        FrameLayout.LayoutParams ep = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ep.gravity = Gravity.BOTTOM;
        root.addView(error, ep);

        spinner = UiKit.spinner(ctx);
        FrameLayout.LayoutParams sp = new FrameLayout.LayoutParams(dp(24), dp(24));
        sp.gravity = Gravity.CENTER;
        root.addView(spinner, sp);

        gate = new LockGate(ctx, "Tap to unlock Dev",
                () -> ctx.startActivity(Lock.DEV.pinGate(ctx)));
        gate.setVisibility(View.GONE);
        root.addView(gate, mm());

        refreshGate();
        return root;
    }

    @Override public void onShow() { shown = true; refreshGate(); startPoll(); }
    @Override public void onHide() { shown = false; stopPoll(); }
    @Override public void onLeave() { shown = false; stopPoll(); }
    @Override public void onClose() { stopPoll(); }
    @Override public void onFocus() { fetchNow(); }

    @Override
    public View[] titleButtons(Context c) {
        TextView r = new TextView(c);
        r.setText("↻");
        r.setTextColor(Color.WHITE);
        r.setTextSize(18);
        r.setTypeface(Fonts.current(c));
        r.setGravity(Gravity.CENTER);
        r.setPadding(dp(12), dp(2), dp(12), dp(4));
        r.setOnClickListener(v -> fetchNow());
        return new View[]{ r };
    }

    // --- refresh loop -------------------------------------------------

    private void refreshGate() {
        boolean locked = Lock.DEV.gateActive(ctx);
        gate.setVisibility(locked ? View.VISIBLE : View.GONE);
        if (locked) { stopPoll(); return; }
        if (Lock.DEV.isLocked(ctx)) Lock.DEV.keepUnlocked(ctx);
        fetchNow();
    }

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            fetchNow();
            poll.postDelayed(this, pollMs());
        }
    };

    private void startPoll() {
        if (polling || !shown || account() == null || Lock.DEV.gateActive(ctx)) return;
        polling = true;
        poll.removeCallbacks(tick);
        poll.postDelayed(tick, pollMs());
    }

    private void stopPoll() {
        polling = false;
        poll.removeCallbacks(tick);
    }

    protected final DevAccount account() {
        return accountId == null ? null : DevAccount.find(ctx, accountId);
    }

    private void fetchNow() {
        final DevAccount a = account();
        if (a == null) {
            spinner.setVisibility(View.GONE);
            data = null;
            showError("No account — open ▸ Settings");
            list.refresh();
            return;
        }
        final int g = ++generation;
        final String token = a.token(ctx);
        NetIo.POOL.execute(() -> {
            try {
                final List<ServiceData.Section> res = load(a, token);
                NetIo.MAIN.post(() -> {
                    if (g != generation) return;
                    data = res;
                    error.setVisibility(View.GONE);
                    spinner.setVisibility(View.GONE);
                    list.refresh();
                });
            } catch (Exception e) {
                final String msg = friendly(e);
                NetIo.MAIN.post(() -> {
                    if (g != generation) return;
                    spinner.setVisibility(View.GONE);
                    showError(msg);
                });
            }
        });
    }

    private void showError(String msg) {
        error.setText(msg);
        error.setVisibility(View.VISIBLE);
    }

    static String friendly(Exception e) {
        if (e instanceof Http.HttpException) {
            int s = ((Http.HttpException) e).status;
            if (s == 401 || s == 403) return "Token rejected (" + s + ").";
            if (s == 429) return "Rate limited — try again shortly.";
            return "Service error " + s + ".";
        }
        return "Network error.";
    }

    // --- row building ------------------------------------------------

    private void fillRows(List<Object> rows) {
        if (data == null) return;
        for (ServiceData.Section sec : data) {
            if (sec.rows.isEmpty()) continue;
            rows.add(new SearchResultsAdapter.Header(sec.header));
            for (ServiceData.Row r : sec.rows) {
                final String url = r.url;
                rows.add(new SearchResult(SearchResult.Kind.DEV, r.title, r.subtitle, -1,
                        () -> { if (url != null) openUrl(url); }));
            }
        }
    }

    protected final void openUrl(String url) {
        try {
            ctx.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Exception ignored) {}
    }

    private FrameLayout.LayoutParams mm() {
        return new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    private int dp(int v) {
        return Math.round(v * ctx.getResources().getDisplayMetrics().density);
    }
}

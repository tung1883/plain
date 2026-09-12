package com.plainphone.app;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/** A GitHub status window: contribution graph, open PRs, review requests, assigned
 *  issues, latest Actions run. */
final class GithubPanel extends ServicePanel {

    private TextView contribLabel;
    private ContributionGraphView contribGraph;
    private HorizontalScrollView contribScroll;
    private int contribBaseRightPad;

    GithubPanel(String accountId) { super(accountId); }

    @Override public String kind() { return "github"; }
    @Override String accountKind() { return DevAccount.GITHUB; }
    @Override String serviceName() { return "GitHub"; }
    @Override long pollMs() { return 90_000; }

    @Override
    List<ServiceData.Section> load(DevAccount account, String token) throws Exception {
        return Github.sections(ctx, account, token);
    }

    @Override
    protected View header(Context ctx) {
        Typeface font = Fonts.current(ctx);
        int inset = UiKit.dp(ctx, 18);
        int gap = UiKit.dp(ctx, 8);

        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);

        contribLabel = new TextView(ctx);
        contribLabel.setText("Contributions…");
        contribLabel.setTextColor(Color.GRAY);
        contribLabel.setTextSize(13);
        contribLabel.setTypeface(font);
        contribLabel.setPadding(inset, gap, inset, gap / 2);
        box.addView(contribLabel);

        contribGraph = new ContributionGraphView(ctx);
        contribScroll = new HorizontalScrollView(ctx);
        contribScroll.setHorizontalScrollBarEnabled(false);
        // Padding on the scroll view itself (not the scrolled content) so the
        // margin stays put as a viewport gutter no matter where it's scrolled to.
        contribBaseRightPad = inset;
        contribScroll.setPadding(inset, 0, inset, gap);
        contribScroll.setClipToPadding(true);
        contribScroll.addView(contribGraph);
        box.addView(contribScroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        loadContributions();
        return box;
    }

    @Override public void onFocus() { super.onFocus(); loadContributions(); }

    private void loadContributions() {
        DevAccount a = account();
        if (a == null || ctx == null) return;
        final String token = a.token(ctx);
        NetIo.POOL.execute(() -> {
            Github.Contributions data = null;
            try {
                data = Github.contributions(token);
            } catch (Exception ignored) {
            }
            final Github.Contributions fData = data;
            NetIo.MAIN.post(() -> {
                if (contribLabel == null || contribGraph == null) return;
                if (fData == null) {
                    contribLabel.setText("Contributions unavailable");
                    return;
                }
                contribLabel.setText(fData.total + " contributions in the last year");
                contribGraph.setData(fData.grid);
                scrollToEnd();
            });
        });
    }

    /** Scroll to the right edge (most recent week) so every column is whole
     *  except the leftmost visible one, which always shows exactly half —
     *  waits for the just-changed grid to actually finish laying out before
     *  computing the target, instead of scrolling against the stale width. */
    private void scrollToEnd() {
        if (contribScroll == null || contribGraph == null) return;
        contribScroll.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override public void onGlobalLayout() {
                contribScroll.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                int step = contribGraph.stepPx();
                int half = contribGraph.cellPx() / 2;
                int contentW = contribGraph.getWidth();
                int viewportW = contribScroll.getWidth()
                        - contribScroll.getPaddingLeft() - contribBaseRightPad;
                if (step <= 0 || contentW <= 0 || viewportW <= 0) return;
                // contentW mod step == cellPx (the grid ends flush after a cell, no
                // trailing gap), so this solves for the viewport width that leaves
                // exactly `half` px of the leftmost column showing at max scroll.
                int extraRightPad = ((viewportW - half) % step + step) % step;
                contribScroll.setPadding(contribScroll.getPaddingLeft(), contribScroll.getPaddingTop(),
                        contribBaseRightPad + extraRightPad, contribScroll.getPaddingBottom());
                // Padding change needs its own layout pass before the new scroll range applies.
                contribScroll.post(() -> {
                    int vw = contribScroll.getWidth()
                            - contribScroll.getPaddingLeft() - contribScroll.getPaddingRight();
                    contribScroll.scrollTo(Math.max(0, contentW - vw), 0);
                });
            }
        });
    }
}

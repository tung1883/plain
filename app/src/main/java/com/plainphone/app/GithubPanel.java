package com.plainphone.app;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
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

    GithubPanel(String accountId) { super(accountId); }

    @Override public String kind() { return "github"; }
    @Override String accountKind() { return DevAccount.GITHUB; }
    @Override String serviceName() { return "GitHub"; }
    @Override long pollMs() { return 90_000; }

    @Override
    List<ServiceData.Section> load(DevAccount account, String token) throws Exception {
        return Github.sections(ctx, account, token);
    }

    @Override protected boolean showSettingsRowInList() { return false; }

    @Override
    protected View header(Context ctx) {
        Typeface font = Fonts.current(ctx);
        int inset = UiKit.dp(ctx, 18);
        int gap = UiKit.dp(ctx, 8);

        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        box.addView(settingsRow(ctx));

        contribLabel = new TextView(ctx);
        contribLabel.setText("Contributions…");
        contribLabel.setTextColor(Color.GRAY);
        contribLabel.setTextSize(13);
        contribLabel.setTypeface(font);
        contribLabel.setPadding(inset, gap, inset, gap / 2);
        box.addView(contribLabel);

        contribGraph = new ContributionGraphView(ctx);
        LinearLayout pad = new LinearLayout(ctx);
        pad.setPadding(inset, 0, inset, gap);
        pad.addView(contribGraph);
        contribScroll = new HorizontalScrollView(ctx);
        contribScroll.setHorizontalScrollBarEnabled(false);
        contribScroll.addView(pad);
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
                // Show the most recent weeks (right edge) by default, not the oldest.
                if (contribScroll != null) contribScroll.post(() -> contribScroll.fullScroll(View.FOCUS_RIGHT));
            });
        });
    }
}

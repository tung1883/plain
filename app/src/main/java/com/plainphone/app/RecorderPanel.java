package com.plainphone.app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.List;

/**
 * A recorder window: the full home Recorder section, in a panel. A take runs inline above the
 * list ({@link RecordingSurface}), so recording never leaves the workspace.
 */
final class RecorderPanel extends PluginPanel {

    static final int REQ_MIC = 7702;

    private RecordingSurface take;
    private boolean beginAfterMic;

    @Override public String kind() { return "recorder"; }
    @Override public String title() { return "Recorder"; }
    @Override HomeMode section() { return HomeMode.RECORDER; }

    @Override void renderNormal(List<Object> rows) { RecorderSection.render(this, rows); }
    @Override void renderSelectionRows(List<Object> rows) { RecorderSection.renderSelection(this, rows); }
    @Override String selectionIdOf(Object payload) { return RecorderSection.selectionId(payload); }

    @Override
    void afterRoot() {
        // Take surface goes on top of the column (hidden until a take starts), above the bar and list.
        take = new RecordingSurface(ctx, this::onTakeDone);
        int pad = (int) (ctx.getResources().getDisplayMetrics().density * 16);
        take.setPadding(pad, pad, pad, pad);
        take.setVisibility(View.GONE);
        ((LinearLayout) list.getParent()).addView(take, 0, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
    }

    /** "+ New recording" or the live card: show the take here instead of opening a screen. */
    @Override
    public void startTake() {
        if (activity().checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            beginAfterMic = true;
            activity().requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_MIC);
            return;
        }
        showTake();
    }

    /** Routed from {@link WorkspaceActivity} when the mic permission prompt returns. */
    void onMicResult(boolean granted) {
        if (!beginAfterMic) return;
        beginAfterMic = false;
        if (granted) showTake();
        else Toast.makeText(ctx, "plainphone needs microphone access to record.", Toast.LENGTH_SHORT).show();
    }

    private void showTake() {
        take.setVisibility(View.VISIBLE);
        if (takeRunning()) take.attach();
        else take.begin();
    }

    private boolean takeRunning() {
        return RecorderService.isActive(ctx) && "recording".equals(RecorderService.activeDetail());
    }

    @Override public void onShow() {
        super.onShow();
        if (takeRunning()) showTake(); // a take started elsewhere (home, notification) stays visible
    }

    @Override public void onLeave() {
        super.onLeave();
        if (take != null) take.release();
    }

    @Override public void onClose() {
        super.onClose();
        if (take != null) take.release();
    }

    private void onTakeDone() {
        take.release();
        take.setVisibility(View.GONE);
        RecorderSection.suppressCard();
        refresh();
        // The service persists the take asynchronously; re-list once it lands (as stopActive does).
        new Handler(Looper.getMainLooper()).postDelayed(this::refresh, 400);
    }
}

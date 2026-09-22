package com.plainphone.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

/** Every folder pair configured for a host — see the approved mockup's pair-list screen. */
public class DevSyncActivity extends Activity {

    private String hostId;
    private SectionListView list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hostId = getIntent().getStringExtra(DevHostActivity.EXTRA_HOST_ID);
        DevHost host = DevHost.find(this, hostId);
        if (host == null) { finish(); return; }
        setTaskDescription(new android.app.ActivityManager.TaskDescription(host.label + " · sync"));

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setBackgroundColor(Color.BLACK);

        list = new SectionListView(this);
        list.setLongPress(row -> {
            if (!(row.payload instanceof String)) return false;
            confirmDelete((String) row.payload);
            return true;
        });
        col.addView(list, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        list.setProvider(this::fill);

        UiKit.screen(this, host.label + " · Sync", col);
    }

    @Override
    protected void onResume() {
        super.onResume();
        DevSyncJobs.addListener(this::refresh);
        if (list != null) list.refresh();
    }

    @Override
    protected void onPause() {
        super.onPause();
        DevSyncJobs.removeListener(this::refresh);
    }

    private void refresh() {
        runOnUiThread(() -> { if (list != null) list.refresh(); });
    }

    private void fill(List<Object> rows) {
        List<DevSyncPair> pairs = DevSyncPair.forHost(this, hostId);
        rows.add(new SearchResult(SearchResult.Kind.DEV, "+  New sync pair", null, -1,
                () -> startActivity(new Intent(this, DevSyncAddActivity.class)
                        .putExtra(DevHostActivity.EXTRA_HOST_ID, hostId))));
        for (DevSyncPair pair : pairs) {
            rows.add(new SearchResult(SearchResult.Kind.DEV, rowTitle(pair), rowSub(pair), -1,
                    () -> startActivity(new Intent(this, DevSyncRunActivity.class)
                            .putExtra(DevHostActivity.EXTRA_HOST_ID, hostId)
                            .putExtra(DevSyncRunActivity.EXTRA_PAIR_ID, pair.id)),
                    pair.id));
        }
        if (pairs.isEmpty()) {
            rows.add(new SearchResult(SearchResult.Kind.DEV, "No sync pairs yet",
                    "Add one above", -1, () -> {}));
        }
    }

    private String rowTitle(DevSyncPair pair) {
        String label = pair.label != null && !pair.label.isEmpty() ? pair.label : "Sync pair";
        String arrow = DevSyncPair.DIR_PUSH.equals(pair.direction) ? "→"
                : DevSyncPair.DIR_PULL.equals(pair.direction) ? "←" : "⇄";
        return label.contains(arrow) ? label : label + " " + arrow;
    }

    private String rowSub(DevSyncPair pair) {
        String dir = DevSyncPair.DIR_PUSH.equals(pair.direction) ? "Push"
                : DevSyncPair.DIR_PULL.equals(pair.direction) ? "Pull" : "Mirror";
        String detect = DevSyncPair.DETECT_CHECKSUM.equals(pair.detectMode) ? "Checksum"
                : DevSyncPair.DETECT_SIZE.equals(pair.detectMode) ? "Size" : "Modified time";
        String sched = pair.scheduleMinutes > 0 ? "every " + pair.scheduleMinutes + " min" : "manual";
        StringBuilder sb = new StringBuilder(dir).append(" · ").append(detect).append(" · ").append(sched);
        if (DevSyncJobs.pending(this, pair.id)) {
            sb.append("\nSyncing…");
        } else if (pair.lastConflicts > 0) {
            sb.append("\n").append(pair.lastConflicts).append(pair.lastConflicts == 1 ? " conflict" : " conflicts")
                    .append(" needs review");
        } else if (pair.lastRunAt > 0) {
            sb.append("\nSynced ").append(pair.lastFailed > 0 ? "with " + pair.lastFailed + " failure(s)" : "");
        } else {
            sb.append("\nNever run");
        }
        return sb.toString();
    }

    private void confirmDelete(String pairId) {
        DevSyncPair pair = DevSyncPair.find(this, pairId);
        if (pair == null) return;
        VaultUi.confirm(this, "Remove this sync pair?",
                "This only stops syncing — no files are deleted.", "Remove", () -> {
                    DevSyncPair.remove(this, pairId);
                    list.refresh();
                }, "Cancel", null);
    }
}

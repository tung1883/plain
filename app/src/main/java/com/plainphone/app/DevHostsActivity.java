package com.plainphone.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

/** The list of paired devices. Tap to connect and open the hub; long-press to remove. */
public class DevHostsActivity extends Activity {

    private LinearLayout root;
    private Typeface font;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        font = Fonts.current(this);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);
        ScrollView scroller = new ScrollView(this);
        scroller.setBackgroundColor(Color.BLACK);
        scroller.addView(root, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        UiKit.screen(this, "Dev", scroller);
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    private void render() {
        font = Fonts.current(this);
        root.removeAllViews();

        root.addView(row("+ Add device", "scan or paste a link", Color.GRAY,
                v -> startActivity(new Intent(this, DevPairActivity.class))));

        List<DevHost> hosts = DevHost.all(this);
        if (hosts.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No devices yet. Run “plaind pair” on a machine, then add it here.");
            empty.setTextColor(Color.GRAY);
            empty.setTextSize(14);
            empty.setTypeface(font);
            empty.setPadding(48, 40, 48, 40);
            root.addView(empty);
            return;
        }

        String liveId = DevService.connectedHostId();
        for (DevHost host : hosts) {
            boolean live = host.id.equals(liveId) && DevService.isConnected();
            root.addView(row(host.label, live ? "connected · " + host.address() : host.address(),
                    live ? Color.WHITE : Color.GRAY,
                    v -> {
                        DevService.connect(this, host.id);
                        startActivity(new Intent(this, DevHostActivity.class)
                                .putExtra(DevHostActivity.EXTRA_HOST_ID, host.id));
                    },
                    () -> confirmRemove(host)));
        }
    }

    private void confirmRemove(DevHost host) {
        VaultUi.confirm(this, "Remove " + host.label + "?", "Its saved key is deleted too.",
                "Remove", () -> {
                    if (host.id.equals(DevService.connectedHostId())) DevService.disconnect(this);
                    DevHost.remove(this, host.id);
                    render();
                }, "Cancel", null);
    }

    private View row(String label, String value, int valueColor, View.OnClickListener tap) {
        return row(label, value, valueColor, tap, null);
    }

    private View row(String label, String value, int valueColor,
                     View.OnClickListener tap, Runnable longPress) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(48, 30, 48, 30);
        StateListDrawable bg = new StateListDrawable();
        bg.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Color.DKGRAY));
        bg.addState(new int[]{}, new ColorDrawable(Color.BLACK));
        row.setBackground(bg);
        row.setOnClickListener(tap);
        if (longPress != null) {
            row.setOnLongClickListener(v -> {
                longPress.run();
                return true;
            });
        }

        TextView l = new TextView(this);
        l.setText(label);
        l.setTextColor(Color.WHITE);
        l.setTextSize(18);
        l.setTypeface(font);
        row.addView(l);

        TextView v = new TextView(this);
        v.setText(value);
        v.setTextColor(valueColor);
        v.setTextSize(13);
        v.setTypeface(font);
        v.setPadding(0, 6, 0, 0);
        row.addView(v);
        row.setGravity(Gravity.CENTER_VERTICAL);
        return row;
    }
}

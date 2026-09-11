package com.plainphone.app;

import android.app.Activity;
import android.app.ActivityManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

/**
 * Full-screen host for one cloud-service {@link PanelContent} (GitHub / Vercel
 * / Supabase), opened straight from the Dev home section instead of adding it
 * to a {@link WorkspaceActivity}. A "← Title" bar ({@link UiKit#header}) plus
 * the panel's own title buttons (its ↻) stand in for the floating {@link Panel}
 * chrome the content normally sits inside.
 */
public class CloudPanelActivity extends Activity {

    static final String EXTRA_KIND = "kind";
    static final String EXTRA_ACCOUNT_ID = "accountId";

    private PanelContent content;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String kindId = getIntent().getStringExtra(EXTRA_KIND);
        String accountId = getIntent().getStringExtra(EXTRA_ACCOUNT_ID);
        PanelKind kind = PanelKind.byId(kindId);
        if (kind == null) {
            finish();
            return;
        }

        content = kind.factory.create(this, null, accountId);
        View body = content.onCreate(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);

        LinearLayout header = UiKit.header(this, content.title());
        View[] buttons = content.titleButtons(this);
        if (buttons != null) {
            for (View b : buttons) {
                header.addView(b, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            }
        }
        root.addView(header, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        View hair = new View(this);
        hair.setBackgroundColor(0xFF1C1C1C);
        root.addView(hair, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));

        root.addView(body, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);
        setTaskDescription(new ActivityManager.TaskDescription(content.title()));
    }

    @Override
    protected void onResume() {
        super.onResume();
        content.onShow();
        content.onFocus();
    }

    @Override
    protected void onStop() {
        super.onStop();
        content.onLeave();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        content.onClose();
    }
}

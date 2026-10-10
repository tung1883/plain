package com.plainphone.app;

import android.app.Activity;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Nav home section: "Map" and "Compass" as two tabs under one HomeMode.NAV, same tab-strip
 * pattern as Chess's Board/Puzzles/Library. Radio (the third original nav-plugin piece) isn't
 * built yet.
 */
final class NavPanel {

    private final FrameLayout root;
    private final FrameLayout tabContainer;
    private final TextView[] tabButtons;
    private final MapPanel mapPanel;
    private final CompassPanel compassPanel;
    private int activeTab;
    private boolean sectionActive;

    NavPanel(Activity activity) {
        root = new FrameLayout(activity);
        root.setBackgroundColor(Color.BLACK);

        LinearLayout root2 = new LinearLayout(activity);
        root2.setOrientation(LinearLayout.VERTICAL);

        LinearLayout tabs = new LinearLayout(activity);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setPadding(UiKit.dp(activity, 18), UiKit.dp(activity, 14), UiKit.dp(activity, 18), 0);
        String[] labels = {"Map", "Compass"};
        tabButtons = new TextView[labels.length];
        for (int i = 0; i < labels.length; i++) {
            final int idx = i;
            TextView tab = new TextView(activity);
            tab.setText(labels[i]);
            tab.setTextSize(13);
            tab.setTypeface(Fonts.current(activity));
            tab.setIncludeFontPadding(false);
            tab.setGravity(Gravity.CENTER);
            tab.setPadding(0, UiKit.dp(activity, 12), 0, UiKit.dp(activity, 12));
            tab.setOnClickListener(v -> selectTab(idx));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            if (i > 0) lp.leftMargin = UiKit.dp(activity, 8);
            tabs.addView(tab, lp);
            tabButtons[i] = tab;
        }
        root2.addView(tabs);

        tabContainer = new FrameLayout(activity);
        tabContainer.setPadding(UiKit.dp(activity, 18), UiKit.dp(activity, 14), UiKit.dp(activity, 18), 0);
        mapPanel = new MapPanel(activity);
        compassPanel = new CompassPanel(activity);
        tabContainer.addView(mapPanel.view(), new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        tabContainer.addView(compassPanel.view(), new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root2.addView(tabContainer, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        root.addView(root2, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        activeTab = 0;
        restyleTabs();
        compassPanel.view().setVisibility(View.GONE);
    }

    View view() { return root; }

    MapPanel mapPanel() { return mapPanel; }

    private void selectTab(int index) {
        if (index == activeTab) return;
        activeTab = index;
        mapPanel.view().setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        compassPanel.view().setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        compassPanel.setActive(sectionActive && index == 1);
        restyleTabs();
    }

    private void restyleTabs() {
        for (int i = 0; i < tabButtons.length; i++) {
            boolean on = i == activeTab;
            tabButtons[i].setBackground(on
                    ? UiKit.rounded(tabButtons[i].getContext(), Color.WHITE, 0, 0f, UiKit.R_SM)
                    : UiKit.rounded(tabButtons[i].getContext(), Color.BLACK, 0xFF2C2C2C, 2f, UiKit.R_SM));
            tabButtons[i].setTextColor(on ? Color.BLACK : Color.WHITE);
        }
    }

    /** Whether the whole Nav section (either tab) is the thing currently on screen — drives
     *  whether the compass tab, if selected, is allowed to sample its sensor. */
    void setActive(boolean active) {
        sectionActive = active;
        compassPanel.setActive(active && activeTab == 1);
    }

    void onStart() { mapPanel.onStart(); }
    void onResume() { mapPanel.onResume(); }
    void onPause() { mapPanel.onPause(); }
    void onStop() { mapPanel.onStop(); }
    void onLowMemory() { mapPanel.onLowMemory(); }
    void onDestroy() { mapPanel.onDestroy(); }

    void onPermissionResult(int requestCode, boolean granted) {
        if (requestCode == MapPanel.REQUEST_LOCATION) mapPanel.onPermissionResult(granted);
    }
}

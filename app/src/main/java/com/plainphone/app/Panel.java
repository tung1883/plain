package com.plainphone.app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * A free-floating window in the {@link WorkspaceActivity}: a title bar you drag,
 * a close/minimise box, resize from any of the four corners, and a body supplied
 * by a {@link PanelContent}. Positioned by its {@link FrameLayout.LayoutParams}
 * margins inside {@link PanelHost}; tapping anywhere brings it to the front.
 */
@SuppressLint({"ViewConstructor", "ClickableViewAccessibility"})
final class Panel extends LinearLayout {

    interface Host {
        void onFocusPanel(Panel p);
        void onClosePanel(Panel p);
        void onMinimizePanel(Panel p);
        void onPanelMoved();
        int hostWidth();
        int hostHeight();
    }

    final PanelContent content;
    private final Host host;
    private final TextView titleView;
    private final float density;
    private final int cornerZone;

    private float downRawX, downRawY;
    private int downX, downY, downW, downH;
    /** 0 none, else a bitmask: 1 left, 2 top, 4 right, 8 bottom. */
    private int grab;

    Panel(Context ctx, PanelContent content, Host host) {
        super(ctx);
        this.content = content;
        this.host = host;
        this.density = ctx.getResources().getDisplayMetrics().density;
        this.cornerZone = dp(22);

        setOrientation(VERTICAL);
        int b = dp(2);
        setBackground(UiKit.rounded(ctx, Color.BLACK, 0xFF5A5A5A, b, UiKit.R_SM));
        setPadding(b, b, b, b); // keep the body off the border so the stroke shows
        UiKit.clipRounded(ctx, this, UiKit.R_SM);
        setElevation(dp(8));

        // Build the body first — the content is now live, so titleButtons()
        // (e.g. the shell's ⌨) can reach into it.
        View inner = content.onCreate(ctx);

        // --- title bar ------------------------------------------------
        LinearLayout bar = new LinearLayout(ctx);
        bar.setOrientation(HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        float r = UiKit.dp(ctx, UiKit.R_SM) - b;
        android.graphics.drawable.GradientDrawable barBg = new android.graphics.drawable.GradientDrawable();
        barBg.setColor(0xFF141414);
        barBg.setCornerRadii(new float[]{r, r, r, r, 0, 0, 0, 0});
        bar.setBackground(barBg);
        bar.setPadding(dp(14), dp(9), dp(6), dp(9));

        titleView = new TextView(ctx);
        titleView.setText(content.title());
        titleView.setTextColor(0xFFE6E6E6);
        titleView.setTextSize(12.5f);
        titleView.setTypeface(Fonts.cascadiaMono(ctx));
        titleView.setSingleLine(true);
        titleView.setEllipsize(android.text.TextUtils.TruncateAt.END);
        bar.addView(titleView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        View[] extra = content.titleButtons(ctx);
        if (extra != null) {
            for (View btn : extra) {
                if (btn == null) continue;
                btn.setMinimumWidth(dp(40));
                bar.addView(btn, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
            }
        }
        bar.addView(barButton(ctx, "–", () -> host.onMinimizePanel(this)));
        bar.addView(barButton(ctx, "×", () -> host.onClosePanel(this)));

        bar.setOnTouchListener(this::onBarTouch);
        addView(bar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        // --- body -----------------------------------------------------
        FrameLayout body = new FrameLayout(ctx);
        body.setBackgroundColor(Color.BLACK);
        // Clip the content's own square corners so the panel's rounded border
        // shows at the bottom two corners.
        UiKit.clipRounded(ctx, body, UiKit.R_SM - 1);
        body.addView(inner, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        addView(body, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
    }

    void refreshTitle() {
        post(() -> titleView.setText(content.title()));
    }

    boolean minimized() { return getVisibility() != VISIBLE; }

    /** Current [x, y, w, h] in host pixels. */
    int[] bounds() {
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) getLayoutParams();
        if (lp == null) return new int[]{0, 0, 0, 0};
        return new int[]{lp.leftMargin, lp.topMargin, lp.width, lp.height};
    }

    /** Place at (x,y) with size (w,h) inside the host, clamped to stay reachable. */
    void placeAt(int x, int y, int w, int h) {
        int minW = dp(150), minH = dp(110);
        w = Math.max(minW, Math.min(w, host.hostWidth()));
        h = Math.max(minH, Math.min(h, host.hostHeight()));
        x = Math.max(0, Math.min(x, host.hostWidth() - dp(72)));
        y = Math.max(0, Math.min(y, host.hostHeight() - dp(48)));
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) getLayoutParams();
        if (lp == null) lp = new FrameLayout.LayoutParams(w, h);
        lp.width = w; lp.height = h;
        lp.leftMargin = x; lp.topMargin = y;
        setLayoutParams(lp);
        host.onPanelMoved();
    }

    // --- touch: focus on any tap, resize from any corner -------------

    /** Resize from the two BOTTOM corners only — the top edge is the title bar
     *  (drag = move) and its right side holds the ⌨ / – / × buttons. */
    private int cornerAt(float x, float y) {
        if (y < getHeight() - cornerZone) return 0;
        if (x <= cornerZone) return 1 | 8;              // bottom-left
        if (x >= getWidth() - cornerZone) return 4 | 8; // bottom-right
        return 0;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent e) {
        if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
            host.onFocusPanel(this);                 // tapping anywhere raises it
            grab = cornerAt(e.getX(), e.getY());
            if (grab != 0) {
                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) getLayoutParams();
                downRawX = e.getRawX();
                downRawY = e.getRawY();
                downX = lp.leftMargin;
                downY = lp.topMargin;
                downW = lp.width;
                downH = lp.height;
                return true;                         // steal the gesture for a resize
            }
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (grab == 0) return false;
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_MOVE: {
                int dx = (int) (e.getRawX() - downRawX);
                int dy = (int) (e.getRawY() - downRawY);
                int x = downX, y = downY, w = downW, h = downH;
                if ((grab & 1) != 0) { x = downX + dx; w = downW - dx; }
                if ((grab & 4) != 0) { w = downW + dx; }
                if ((grab & 2) != 0) { y = downY + dy; h = downH - dy; }
                if ((grab & 8) != 0) { h = downH + dy; }
                placeAt(x, y, w, h);
                return true;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                grab = 0;
                return true;
        }
        return true;
    }

    private boolean onBarTouch(View v, MotionEvent e) {
        if (grab != 0) return false; // a corner resize owns this gesture
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) getLayoutParams();
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downRawX = e.getRawX();
                downRawY = e.getRawY();
                downX = lp.leftMargin;
                downY = lp.topMargin;
                return true;
            case MotionEvent.ACTION_MOVE:
                placeAt(downX + (int) (e.getRawX() - downRawX),
                        downY + (int) (e.getRawY() - downRawY),
                        lp.width, lp.height);
                return true;
        }
        return false;
    }

    private TextView barButton(Context ctx, String glyph, Runnable action) {
        TextView t = new TextView(ctx);
        t.setText(glyph);
        t.setTextColor(0xFFB0B0B0);
        t.setTextSize(16);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(12), dp(2), dp(12), dp(4));
        t.setOnClickListener(v -> action.run());
        return t;
    }

    private int dp(float v) { return Math.round(v * density); }
}

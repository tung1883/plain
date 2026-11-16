package com.plainphone.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.RectF;
import android.inputmethodservice.InputMethodService;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

/**
 * The keyboard's input view. Docked it is the {@link KeyboardRoot} at the bottom edge, as it always
 * was. Floating it is a transparent, screen-high layer with the keyboard drawn in it, which can be
 * dragged by a grip bar, resized by a corner, and collapsed to a round bubble; the service then tells
 * Android (through {@link #computeInsets}) that the keyboard takes no room and that only the
 * keyboard, or the bubble, takes touches, so the app underneath neither resizes nor loses its taps.
 * A floating keyboard turns see-through after a moment without touch.
 */
final class FloatingHost extends FrameLayout {

    enum Mode { DOCKED, FLOAT, BUBBLE }

    private static final int GRIP_DP = 22, BUBBLE_DP = 48, MAX_DESIGN_DP = 420, EDGE_DP = 8, MINUS_ZONE_DP = 44;
    private static final long GHOST_AFTER_MS = 2000;
    private static final float GHOST_ALPHA = 0.42f;

    private final KeyboardRoot root;
    private final LinearLayout column;
    private final FrameLayout box;
    private final Grip grip;
    private final Corner corner;
    private final Bubble bubble;
    private final DismissTargetView dismiss;
    private final View scrim;
    private Runnable dismissHandler;
    private final float dp;

    private Mode mode = Mode.DOCKED;
    private float scale;
    private float fx, fy;               // the keyboard's place, as 0..1 of the room it has to move in
    private boolean bubbleRight;
    private float bubbleFy;
    private boolean ghostOn;

    private final Runnable ghostRun = new Runnable() {
        @Override
        public void run() {
            if (mode != Mode.FLOAT || !ghostOn) return;
            if (root.isIdleForGhost()) box.animate().alpha(GHOST_ALPHA).setDuration(250).start();
            else postDelayed(this, 1000);   // a panel is open: look again in a moment
        }
    };

    FloatingHost(Context ctx, KeyboardRoot root) {
        super(ctx);
        this.root = root;
        dp = ctx.getResources().getDisplayMetrics().density;
        setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        column = new LinearLayout(ctx);
        column.setOrientation(LinearLayout.VERTICAL);
        grip = new Grip(ctx);
        column.addView(grip, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(GRIP_DP)));
        column.addView(root, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        box = new FrameLayout(ctx);
        box.setPivotX(0f);
        box.setPivotY(0f);
        box.addView(column, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        corner = new Corner(ctx);
        box.addView(corner, new FrameLayout.LayoutParams(px(28), px(28), Gravity.BOTTOM | Gravity.START));
        addView(box, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM));

        scrim = DismissTargetView.newScrim(ctx);
        addView(scrim, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ctx.getResources().getDisplayMetrics().heightPixels * 38 / 100, Gravity.BOTTOM));
        scrim.setVisibility(GONE);

        dismiss = new DismissTargetView(ctx);
        addView(dismiss, new FrameLayout.LayoutParams(px(DismissTargetView.SIZE_DP), px(DismissTargetView.SIZE_DP),
                Gravity.TOP | Gravity.START));
        dismiss.setVisibility(GONE);

        bubble = new Bubble(ctx);
        addView(bubble, new FrameLayout.LayoutParams(px(BUBBLE_DP), px(BUBBLE_DP), Gravity.TOP | Gravity.START));
        bubble.setVisibility(GONE);

        // keep the keyboard on screen when its own height changes (the height drag, a panel)
        box.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            if (mode == Mode.FLOAT && (b - t) != (ob - ot)) place();
        });

        root.setFloatHandler(this::toggleFloat);
        load();
        apply();
    }

    private int px(float v) {
        return Math.round(v * dp);
    }

    private void load() {
        Context ctx = getContext();
        scale = Config.getKbFloatScale(ctx);
        fx = Config.getKbFloatX(ctx);
        fy = Config.getKbFloatY(ctx);
        bubbleRight = Config.isKbBubbleRight(ctx);
        bubbleFy = Config.getKbBubbleY(ctx);
        ghostOn = Config.isKbGhost(ctx);
        mode = Config.isKbFloating(ctx) ? Mode.FLOAT : Mode.DOCKED;
    }

    // ------------------------------------------------------------------ the three states

    private void toggleFloat() {
        mode = mode == Mode.DOCKED ? Mode.FLOAT : Mode.DOCKED;
        Config.setKbFloating(getContext(), mode == Mode.FLOAT);
        apply();
    }

    /** True while the keyboard floats or is collapsed to its in-keyboard bubble (not docked). */
    boolean isFloatingOrBubble() {
        return mode != Mode.DOCKED;
    }

    /** A new text field was tapped: a keyboard that was collapsed to a bubble comes back floating. */
    void onNewField() {
        if (mode == Mode.BUBBLE) {
            mode = Mode.FLOAT;
            apply();
        }
    }

    /** A keyboard setting changed (the idle fade). */
    void refreshSettings() {
        ghostOn = Config.isKbGhost(getContext());
        wake();
    }

    private int designWidth() {
        int screenW = getWidth() > 0 ? getWidth() : getResources().getDisplayMetrics().widthPixels;
        return Math.min(screenW, px(MAX_DESIGN_DP));
    }

    private void apply() {
        boolean docked = mode == Mode.DOCKED;
        root.setFloating(!docked);

        syncHeight();

        grip.setVisibility(docked ? GONE : VISIBLE);
        corner.setVisibility(docked ? GONE : VISIBLE);
        box.setVisibility(mode == Mode.BUBBLE ? GONE : VISIBLE);
        bubble.setVisibility(mode == Mode.BUBBLE ? VISIBLE : GONE);

        FrameLayout.LayoutParams bp = (FrameLayout.LayoutParams) box.getLayoutParams();
        if (docked) {
            bp.width = ViewGroup.LayoutParams.MATCH_PARENT;
            bp.gravity = Gravity.BOTTOM;
            bp.leftMargin = bp.topMargin = 0;
            box.setScaleX(1f);
            box.setScaleY(1f);
            box.setAlpha(1f);
            box.setBackground(null);
            box.setClipToOutline(false);
        } else {
            bp.width = designWidth();
            bp.gravity = Gravity.TOP | Gravity.START;
            box.setScaleX(scale);
            box.setScaleY(scale);
            box.setBackground(UiKit.rounded(getContext(), 0xFF0A0A0A, 0xFF2C2C2C, 1f, 14));
            box.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View v, Outline outline) {
                    outline.setRoundRect(0, 0, v.getWidth(), v.getHeight(), 14 * dp);
                }
            });
            box.setClipToOutline(true);
        }
        box.setLayoutParams(bp);
        if (mode == Mode.FLOAT) place();
        if (mode == Mode.BUBBLE) placeBubble();
        wake();
        requestLayout();
    }

    /** The layer is only as high as the keyboard when docked, and as high as the screen when floating. */
    private void syncHeight() {
        int want = mode == Mode.DOCKED ? ViewGroup.LayoutParams.WRAP_CONTENT
                : getResources().getDisplayMetrics().heightPixels;
        ViewGroup.LayoutParams hp = getLayoutParams();
        if (hp != null && hp.height != want) {
            hp.height = want;
            setLayoutParams(hp);
        }
    }

    /** The window may give this view its own layout params when it adds it: put the height back. */
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        syncHeight();
    }

    // ------------------------------------------------------------------ placing

    private int topBound() {
        int id = getResources().getIdentifier("status_bar_height", "dimen", "android");
        return id > 0 ? getResources().getDimensionPixelSize(id) : px(24);
    }

    private float boxW() {
        return designWidth() * scale;
    }

    private float boxH() {
        return box.getMeasuredHeight() * scale;
    }

    /** Put the floating keyboard at its remembered place, kept inside the screen. */
    private void place() {
        int w = getWidth(), h = getHeight();
        if (w == 0 || h == 0 || box.getMeasuredHeight() == 0) return;
        int top = topBound();
        float left = FloatGeometry.posFromFraction(fx, w, boxW());
        float t = top + FloatGeometry.posFromFraction(fy, h - top - px(EDGE_DP), boxH());
        setBoxMargins(Math.round(left), Math.round(t));
    }

    private void setBoxMargins(int left, int top) {
        FrameLayout.LayoutParams bp = (FrameLayout.LayoutParams) box.getLayoutParams();
        if (bp.leftMargin == left && bp.topMargin == top) return;
        bp.leftMargin = left;
        bp.topMargin = top;
        box.setLayoutParams(bp);
    }

    private void placeBubble() {
        int w = getWidth(), h = getHeight();
        if (w == 0 || h == 0) return;
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) bubble.getLayoutParams();
        lp.leftMargin = Math.round(FloatGeometry.bubbleLeft(bubbleRight, w, px(BUBBLE_DP), px(EDGE_DP)));
        lp.topMargin = Math.round(topBound() + FloatGeometry.posFromFraction(bubbleFy, h - topBound(), px(BUBBLE_DP)));
        bubble.setLayoutParams(lp);
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        if (mode == Mode.FLOAT) {
            FrameLayout.LayoutParams bp = (FrameLayout.LayoutParams) box.getLayoutParams();
            if (bp.width != designWidth()) {
                bp.width = designWidth();
                box.setLayoutParams(bp);
            }
            place();
        } else if (mode == Mode.BUBBLE) {
            placeBubble();
        }
    }

    // ------------------------------------------------------------------ the window insets

    /**
     * Tell Android what the floating keyboard takes: no room (apps do not resize) and only its own
     * rectangle of touches. Docked, the default insets stand.
     */
    void computeInsets(InputMethodService.Insets outInsets, int windowHeight) {
        if (mode == Mode.DOCKED) return;
        outInsets.contentTopInsets = windowHeight;
        outInsets.visibleTopInsets = windowHeight;
        outInsets.touchableInsets = InputMethodService.Insets.TOUCHABLE_INSETS_REGION;
        View v = mode == Mode.BUBBLE ? bubble : box;
        float s = mode == Mode.BUBBLE ? 1f : scale;
        int[] loc = new int[2];
        v.getLocationInWindow(loc);
        outInsets.touchableRegion.set(loc[0], loc[1],
                loc[0] + Math.round(v.getWidth() * s), loc[1] + Math.round(v.getHeight() * s));
    }

    // ------------------------------------------------------------------ the idle fade

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        wake();
        return super.dispatchTouchEvent(ev);
    }

    /** The keyboard window was shown again (a bubble tap, a field tap): it comes back solid, not faded. */
    void onShown() {
        if (mode == Mode.BUBBLE && bubble.getVisibility() == INVISIBLE) {   // it was dropped on the X
            bubble.setVisibility(VISIBLE);
            placeBubble();
        }
        wake();
    }

    /** The keyboard window was hidden: make it solid for the next time, and touch nothing else. */
    void onHidden() {
        wake();
    }

    /** Make the keyboard solid and start the wait for it to fade again. */
    private void wake() {
        removeCallbacks(ghostRun);
        box.animate().cancel();
        if (box.getAlpha() != 1f) box.setAlpha(1f);
        if (mode == Mode.FLOAT && ghostOn) postDelayed(ghostRun, GHOST_AFTER_MS);
    }

    // ------------------------------------------------------------------ the grip bar

    /** The bar above a floating keyboard: a handle to drag it by, and a minus that collapses it to a bubble. */
    private final class Grip extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF pill = new RectF();
        private float downX, downY;
        private int left0, top0;
        private boolean moved, onMinus;

        Grip(Context ctx) {
            super(ctx);
        }

        @Override
        protected void onDraw(Canvas c) {
            c.drawColor(0xFF161616);
            float w = getWidth(), h = getHeight();
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0x44FFFFFF);
            pill.set((w - px(44)) / 2f, (h - px(4)) / 2f, (w + px(44)) / 2f, (h + px(4)) / 2f);
            c.drawRoundRect(pill, px(2), px(2), paint);
            KeyIcons.draw(c, KeyIcons.MINUS, w - px(MINUS_ZONE_DP) / 2f, h / 2f, px(6), 0xFFDDDDDD, false);
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN: {
                    downX = e.getRawX();
                    downY = e.getRawY();
                    FrameLayout.LayoutParams bp = (FrameLayout.LayoutParams) box.getLayoutParams();
                    left0 = bp.leftMargin;
                    top0 = bp.topMargin;
                    moved = false;
                    onMinus = e.getX() > getWidth() - px(MINUS_ZONE_DP);
                    getParent().requestDisallowInterceptTouchEvent(true);
                    return true;
                }
                case MotionEvent.ACTION_MOVE: {
                    float dx = e.getRawX() - downX, dy = e.getRawY() - downY;
                    if (!moved && Math.hypot(dx, dy) > px(6)) moved = true;
                    if (!moved || onMinus) return true;
                    int w = FloatingHost.this.getWidth(), h = FloatingHost.this.getHeight();
                    setBoxMargins(
                            Math.round(FloatGeometry.keepInside(left0 + dx, 0, w, boxW(), 0)),
                            Math.round(FloatGeometry.keepInside(top0 + dy, topBound(), h, boxH(), px(EDGE_DP))));
                    return true;
                }
                case MotionEvent.ACTION_UP: {
                    if (onMinus && !moved) {
                        mode = Mode.BUBBLE;
                        apply();
                        return true;
                    }
                    if (moved) saveBoxPlace();
                    return true;
                }
                default:
                    return true;
            }
        }
    }

    /** Remember where the keyboard now is, as a fraction of the room it has to move in. */
    private void saveBoxPlace() {
        FrameLayout.LayoutParams bp = (FrameLayout.LayoutParams) box.getLayoutParams();
        int top = topBound();
        fx = FloatGeometry.fractionFromPos(bp.leftMargin, getWidth(), boxW());
        fy = FloatGeometry.fractionFromPos(bp.topMargin - top, getHeight() - top - px(EDGE_DP), boxH());
        Config.setKbFloatPos(getContext(), fx, fy);
    }

    // ------------------------------------------------------------------ the resize corner

    /**
     * Drag the bottom-left corner: left makes it bigger, right smaller, and its bottom-right corner
     * stays put. It is drawn as a thick arc exactly on the keyboard's rounded border (14 dp radius,
     * inset half a stroke so it lies on the edge), a little way up the left side and along the bottom.
     */
    private final class Corner extends View {
        private static final float STROKE_DP = 2.6f, RADIUS_DP = 14f;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final android.graphics.Path arc = new android.graphics.Path();
        private float downX, scale0, right0, bottom0;

        Corner(Context ctx) {
            super(ctx);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(px(STROKE_DP));
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setColor(0x88FFFFFF);
        }

        @Override
        protected void onDraw(Canvas c) {
            float inset = px(STROKE_DP) / 2f, r = px(RADIUS_DP) - inset, cy = getHeight() - px(RADIUS_DP);
            float cx = px(RADIUS_DP);
            arc.reset();
            arc.moveTo(inset, cy - px(8));
            arc.lineTo(inset, cy);
            arc.arcTo(new RectF(cx - r, cy - r, cx + r, cy + r), 180f, -90f);
            arc.lineTo(cx + px(8), getHeight() - inset);
            c.drawPath(arc, paint);
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN: {
                    paint.setColor(Color.WHITE);
                    invalidate();
                    downX = e.getRawX();
                    scale0 = scale;
                    FrameLayout.LayoutParams bp = (FrameLayout.LayoutParams) box.getLayoutParams();
                    right0 = bp.leftMargin + boxW();
                    bottom0 = bp.topMargin + boxH();
                    getParent().requestDisallowInterceptTouchEvent(true);
                    return true;
                }
                case MotionEvent.ACTION_MOVE: {
                    float h0 = box.getMeasuredHeight();
                    scale = FloatGeometry.scaleFromDrag(scale0, e.getRawX() - downX, designWidth());
                    box.setScaleX(scale);
                    box.setScaleY(scale);
                    int w = FloatingHost.this.getWidth(), h = FloatingHost.this.getHeight();
                    setBoxMargins(
                            Math.round(FloatGeometry.keepInside(FloatGeometry.leftForRight(right0, designWidth(), scale),
                                    0, w, boxW(), 0)),
                            Math.round(FloatGeometry.keepInside(bottom0 - h0 * scale, topBound(), h, boxH(), px(EDGE_DP))));
                    FloatingHost.this.requestLayout();
                    return true;
                }
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    paint.setColor(0x88FFFFFF);
                    invalidate();
                    Config.setKbFloatScale(getContext(), scale);
                    saveBoxPlace();
                    return true;
                default:
                    return true;
            }
        }
    }

    // ------------------------------------------------------------------ the bubble

    /** What a minimized keyboard looks like: a round white button. Drag it, and it snaps to the nearer edge; tap it to bring the keyboard back. */
    private final class Bubble extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float downX, downY;
        private int left0, top0;
        private boolean moved;

        Bubble(Context ctx) {
            super(ctx);
            setElevation(px(6));
            setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View v, Outline outline) {
                    outline.setOval(0, 0, v.getWidth(), v.getHeight());
                }
            });
        }

        @Override
        protected void onDraw(Canvas c) {
            KbBubbleView.paintGlass(c, getWidth(), getHeight(), dp, paint);
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) getLayoutParams();
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downX = e.getRawX();
                    downY = e.getRawY();
                    left0 = lp.leftMargin;
                    top0 = lp.topMargin;
                    moved = false;
                    getParent().requestDisallowInterceptTouchEvent(true);
                    return true;
                case MotionEvent.ACTION_MOVE: {
                    float dx = e.getRawX() - downX, dy = e.getRawY() - downY;
                    if (!moved && Math.hypot(dx, dy) > px(6)) moved = true;
                    if (!moved) return true;
                    int w = FloatingHost.this.getWidth(), h = FloatingHost.this.getHeight();
                    lp.leftMargin = Math.round(FloatGeometry.keepInside(left0 + dx, 0, w, px(BUBBLE_DP), 0));
                    lp.topMargin = Math.round(FloatGeometry.keepInside(top0 + dy, topBound(), h, px(BUBBLE_DP), px(EDGE_DP)));
                    showDismissTarget(w, h);
                    float tx = dismiss.getX() + dismiss.getWidth() / 2f, ty = dismiss.getY() + dismiss.getHeight() / 2f;
                    boolean over = FloatGeometry.overTarget(lp.leftMargin + px(BUBBLE_DP) / 2f,
                            lp.topMargin + px(BUBBLE_DP) / 2f, tx, ty, px(DismissTargetView.REACH_DP));
                    dismiss.setHot(over);
                    setScaleX(over ? 0.78f : 1f);   // it shrinks into the button
                    setScaleY(over ? 0.78f : 1f);
                    if (over) {   // pulled onto the X
                        lp.leftMargin = Math.round(tx - px(BUBBLE_DP) / 2f);
                        lp.topMargin = Math.round(ty - px(BUBBLE_DP) / 2f);
                    }
                    setLayoutParams(lp);
                    return true;
                }
                case MotionEvent.ACTION_CANCEL:
                    hideDismissUi();
                    return true;
                case MotionEvent.ACTION_UP:
                    if (dismiss.isHot() && dismiss.getVisibility() == VISIBLE) {
                        // gone at once: Android takes a moment to hide the keyboard window, and the
                        // bubble must not show up at its old spot meanwhile
                        setVisibility(INVISIBLE);
                        hideDismissUi();
                        if (dismissHandler != null) dismissHandler.run();   // the keyboard goes away
                        return true;
                    }
                    hideDismissUi();
                    if (!moved) {
                        mode = Mode.FLOAT;
                        apply();
                        return true;
                    }
                    bubbleRight = FloatGeometry.bubbleOnRight(lp.leftMargin + px(BUBBLE_DP) / 2f, getWidthOfHost());
                    bubbleFy = FloatGeometry.fractionFromPos(lp.topMargin - topBound(),
                            getHeightOfHost() - topBound(), px(BUBBLE_DP));
                    Config.setKbBubble(getContext(), bubbleRight, bubbleFy);
                    placeBubble();
                    return true;
                default:
                    return true;
            }
        }
    }

    /** Show the round X near the bottom while the bubble is dragged. */
    private void showDismissTarget(int w, int h) {
        if (dismiss.getVisibility() == VISIBLE) return;
        FrameLayout.LayoutParams tp = (FrameLayout.LayoutParams) dismiss.getLayoutParams();
        tp.leftMargin = (w - px(DismissTargetView.SIZE_DP)) / 2;
        tp.topMargin = h - px(DismissTargetView.SIZE_DP) - px(DismissTargetView.BOTTOM_DP);
        dismiss.setLayoutParams(tp);
        scrim.setAlpha(0f);
        scrim.setVisibility(VISIBLE);
        scrim.animate().alpha(1f).setDuration(200).start();
        dismiss.setVisibility(VISIBLE);
        scrim.bringToFront();
        dismiss.bringToFront();
        bubble.bringToFront();
    }

    private void hideDismissUi() {
        dismiss.setHot(false);
        dismiss.setVisibility(GONE);
        scrim.setVisibility(GONE);
        bubble.setScaleX(1f);
        bubble.setScaleY(1f);
    }

    /** What happens when the bubble is dropped on the X (the service hides the keyboard). */
    void setDismissHandler(Runnable r) {
        dismissHandler = r;
    }

    private int getWidthOfHost() {
        return getWidth();
    }

    private int getHeightOfHost() {
        return getHeight();
    }
}

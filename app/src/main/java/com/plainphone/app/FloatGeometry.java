package com.plainphone.app;

/**
 * The arithmetic of the floating keyboard: where it may sit, how a drag resizes it, which edge the
 * bubble snaps to. No Android types, so it is tested on the JVM.
 */
final class FloatGeometry {

    static final float MIN_SCALE = 0.55f;
    static final float MAX_SCALE = 1f;

    private FloatGeometry() {}

    static float clamp(float v, float lo, float hi) {
        return hi < lo ? lo : Math.max(lo, Math.min(hi, v));
    }

    /**
     * The scale after dragging the bottom-left corner by {@code dragDx} pixels (left = bigger),
     * for a keyboard that is {@code designW} pixels wide at scale 1.
     */
    static float scaleFromDrag(float scale0, float dragDx, float designW) {
        return clamp(scale0 - dragDx / designW, MIN_SCALE, MAX_SCALE);
    }

    /** The left edge of a keyboard of the given scale whose right edge stays at {@code right}. */
    static float leftForRight(float right, float designW, float scale) {
        return right - designW * scale;
    }

    /** A position along one axis from its 0..1 fraction of the room (area minus box); the box stays inside. */
    static float posFromFraction(float fraction, float area, float box) {
        return clamp(fraction, 0f, 1f) * Math.max(0f, area - box);
    }

    /** The 0..1 fraction of the room that a position along one axis stands for. */
    static float fractionFromPos(float pos, float area, float box) {
        float room = area - box;
        return room <= 0f ? 0.5f : clamp(pos / room, 0f, 1f);
    }

    /** Keep a box of {@code box} pixels inside [lo, area - margin]. */
    static float keepInside(float pos, float lo, float area, float box, float margin) {
        return clamp(pos, lo, area - box - margin);
    }

    /** Which edge the bubble snaps to: the one its centre is nearer to. */
    static boolean bubbleOnRight(float centreX, float areaW) {
        return centreX > areaW / 2f;
    }

    /** The left edge of a bubble snapped to the right or left edge. */
    static float bubbleLeft(boolean right, float areaW, float size, float margin) {
        return right ? areaW - size - margin : margin;
    }
}

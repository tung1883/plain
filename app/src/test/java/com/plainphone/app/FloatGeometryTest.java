package com.plainphone.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class FloatGeometryTest {

    private static final float EPS = 0.0001f;

    @Test
    public void scaleGrowsWhenTheCornerIsDraggedLeft() {
        assertEquals(0.9f, FloatGeometry.scaleFromDrag(0.8f, -36f, 360f), EPS);
        assertEquals(0.7f, FloatGeometry.scaleFromDrag(0.8f, 36f, 360f), EPS);
    }

    @Test
    public void scaleStaysWithinItsLimits() {
        assertEquals(FloatGeometry.MAX_SCALE, FloatGeometry.scaleFromDrag(0.9f, -900f, 360f), EPS);
        assertEquals(FloatGeometry.MIN_SCALE, FloatGeometry.scaleFromDrag(0.6f, 900f, 360f), EPS);
    }

    @Test
    public void resizingKeepsTheRightEdgeFixed() {
        float designW = 400f, right = 380f;
        float scale = FloatGeometry.scaleFromDrag(0.8f, -40f, designW);
        float left = FloatGeometry.leftForRight(right, designW, scale);
        assertEquals(right, left + designW * scale, EPS);
    }

    @Test
    public void fractionAndPositionAreInverses() {
        float area = 1000f, box = 400f;
        for (float f : new float[]{0f, 0.25f, 0.5f, 1f}) {
            float pos = FloatGeometry.posFromFraction(f, area, box);
            assertEquals(f, FloatGeometry.fractionFromPos(pos, area, box), EPS);
        }
    }

    @Test
    public void aBoxThatFillsTheAreaSitsAtZeroAndIsCentred() {
        assertEquals(0f, FloatGeometry.posFromFraction(0.7f, 360f, 360f), EPS);
        assertEquals(0f, FloatGeometry.posFromFraction(0.7f, 300f, 360f), EPS);
        assertEquals(0.5f, FloatGeometry.fractionFromPos(0f, 360f, 360f), EPS);
    }

    @Test
    public void aFractionOutsideZeroToOneIsClamped() {
        assertEquals(600f, FloatGeometry.posFromFraction(3f, 1000f, 400f), EPS);
        assertEquals(0f, FloatGeometry.posFromFraction(-1f, 1000f, 400f), EPS);
    }

    @Test
    public void aDragCannotPushTheKeyboardOffTheScreen() {
        assertEquals(24f, FloatGeometry.keepInside(-50f, 24f, 800f, 300f, 8f), EPS);
        assertEquals(492f, FloatGeometry.keepInside(900f, 24f, 800f, 300f, 8f), EPS);
        assertEquals(200f, FloatGeometry.keepInside(200f, 24f, 800f, 300f, 8f), EPS);
    }

    @Test
    public void aBubbleIsDroppedOnTheTargetOnlyWithinReach() {
        assertTrue(FloatGeometry.overTarget(180f, 900f, 180f, 900f, 80f));
        assertTrue(FloatGeometry.overTarget(220f, 930f, 180f, 900f, 80f));
        assertFalse(FloatGeometry.overTarget(300f, 900f, 180f, 900f, 80f));
        assertFalse(FloatGeometry.overTarget(180f, 600f, 180f, 900f, 80f));
    }

    @Test
    public void theBubbleSnapsToTheNearerEdge() {
        assertTrue(FloatGeometry.bubbleOnRight(250f, 360f));
        assertFalse(FloatGeometry.bubbleOnRight(100f, 360f));
        assertEquals(304f, FloatGeometry.bubbleLeft(true, 360f, 48f, 8f), EPS);
        assertEquals(8f, FloatGeometry.bubbleLeft(false, 360f, 48f, 8f), EPS);
    }
}

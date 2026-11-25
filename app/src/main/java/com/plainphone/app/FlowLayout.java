package com.plainphone.app;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

/** Lays its children out left to right and wraps them onto new lines when they run out of width. */
class FlowLayout extends ViewGroup {

    private final int gapX, gapY;

    FlowLayout(Context c, int gapXpx, int gapYpx) {
        super(c);
        gapX = gapXpx;
        gapY = gapYpx;
    }

    @Override
    protected void onMeasure(int wSpec, int hSpec) {
        int maxW = MeasureSpec.getSize(wSpec) - getPaddingLeft() - getPaddingRight();
        int x = 0, y = 0, rowH = 0, usedW = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View v = getChildAt(i);
            if (v.getVisibility() == GONE) continue;
            measureChild(v, MeasureSpec.makeMeasureSpec(maxW, MeasureSpec.AT_MOST), MeasureSpec.UNSPECIFIED);
            int w = v.getMeasuredWidth(), h = v.getMeasuredHeight();
            if (x > 0 && x + w > maxW) {
                x = 0;
                y += rowH + gapY;
                rowH = 0;
            }
            x += w + gapX;
            rowH = Math.max(rowH, h);
            usedW = Math.max(usedW, x - gapX);
        }
        int height = y + rowH + getPaddingTop() + getPaddingBottom();
        setMeasuredDimension(resolveSize(usedW + getPaddingLeft() + getPaddingRight(), wSpec), resolveSize(height, hSpec));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int maxW = r - l - getPaddingLeft() - getPaddingRight();
        int x = 0, y = 0, rowH = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View v = getChildAt(i);
            if (v.getVisibility() == GONE) continue;
            int w = v.getMeasuredWidth(), h = v.getMeasuredHeight();
            if (x > 0 && x + w > maxW) {
                x = 0;
                y += rowH + gapY;
                rowH = 0;
            }
            v.layout(getPaddingLeft() + x, getPaddingTop() + y, getPaddingLeft() + x + w, getPaddingTop() + y + h);
            x += w + gapX;
            rowH = Math.max(rowH, h);
        }
    }
}

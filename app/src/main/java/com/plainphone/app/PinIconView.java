package com.plainphone.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

/** Map-pin-drop glyph, drawn rather than loaded from a font so it never falls back to a tofu
 *  box on a font that doesn't carry the codepoint. Reused for the locate button/search overlay's
 *  pill/row icons (as a {@link View}) and for the map's own from/to route pins (as a
 *  {@link #toBitmap}, registered into the style as a native {@code SymbolLayer} icon — a plain
 *  overlaid View for those visibly lags/jitters a frame behind the GL-rendered map during a
 *  pan, since it's a separate render pipeline from MapLibre's own). One shape, drawn in one
 *  place either way. */
final class PinIconView extends View {
    private final int color;

    PinIconView(Context context) {
        this(context, Color.BLACK);
    }

    PinIconView(Context context, int color) {
        super(context);
        this.color = color;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        draw(canvas, getWidth(), getHeight(), color);
    }

    /** Renders the same pin into a standalone bitmap, for registering as a MapLibre
     *  {@code SymbolLayer} icon via {@code Style.addImage}. */
    static Bitmap toBitmap(int sizePx, int color) {
        Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        draw(new Canvas(bitmap), sizePx, sizePx, color);
        return bitmap;
    }

    private static void draw(Canvas canvas, int w, int h, int color) {
        Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        fill.setColor(color);
        fill.setStyle(Paint.Style.FILL);
        Paint hole = new Paint(Paint.ANTI_ALIAS_FLAG);
        hole.setColor(Color.WHITE);
        hole.setStyle(Paint.Style.FILL);

        float s = Math.min(w, h) / 24f;   // 24x24 viewBox, scaled to view
        Path pin = new Path();
        pin.moveTo(12 * s, 2 * s);
        pin.cubicTo(8 * s, 2 * s, 5 * s, 5 * s, 5 * s, 9 * s);
        pin.cubicTo(5 * s, 14.2f * s, 12 * s, 22 * s, 12 * s, 22 * s);
        pin.cubicTo(12 * s, 22 * s, 19 * s, 14.2f * s, 19 * s, 9 * s);
        pin.cubicTo(19 * s, 5 * s, 16 * s, 2 * s, 12 * s, 2 * s);
        pin.close();
        canvas.drawPath(pin, fill);
        canvas.drawCircle(12 * s, 9 * s, 2.4f * s, hole);
    }
}

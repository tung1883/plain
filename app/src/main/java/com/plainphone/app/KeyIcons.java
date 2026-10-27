package com.plainphone.app;

import android.graphics.Canvas;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

/** Key icons drawn as paths, so no key depends on a font having the glyph. */
final class KeyIcons {

    static final int LEFT = 1, RIGHT = 2, UP = 3, DOWN = 4, SHIFT = 5, BACKSPACE = 6, ENTER = 7, KEYBOARD = 8,
            CLIPBOARD = 9, EMOJI = 10, TRANSLATE = 11, PIN = 12, SWAP = 13, PEN = 14, SCAN = 15, GEAR = 16,
            /** Drawn as Cascadia Mono text by {@link PanelKit.IconView}, not as a path. */
            KEYS = 17,
            RESIZE = 18, SLIDERS = 19, IMAGE = 20, CLOCK = 21, PEOPLE = 22, LEAF = 23, CUP = 24, BOX = 25,
            HASH = 26, FACE = 27, SCISSORS = 28, GLOBE = 29,
            SEARCH = 30, PAPERCLIP = 31, FILE = 32, LINES = 33, CHECKLIST = 34, NOTE = 35;

    private static final Paint PAINT = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Path PATH = new Path();
    private static final RectF RECT = new RectF();

    private KeyIcons() {}

    /** Icon for a key id, or 0 when the key is drawn as text. */
    static int forId(String id) {
        switch (id) {
            case "Left": return LEFT;
            case "Right": return RIGHT;
            case "Up": return UP;
            case "Down": return DOWN;
            case "Shift": return SHIFT;
            case "Bksp": return BACKSPACE;
            case "Enter": return ENTER;
            case "Ime": return KEYBOARD;
            default: return 0;
        }
    }

    /** Draws icon {@code kind} centred on (cx, cy), {@code s} = half its size; {@code fill} fills closed shapes. */
    static void draw(Canvas c, int kind, float cx, float cy, float s, int color, boolean fill) {
        Paint p = PAINT;
        p.setColor(color);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Math.max(1.5f, s * 0.2f));
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
        p.setPathEffect(null);
        Path path = PATH;
        path.reset();
        switch (kind) {
            case LEFT: arrow(c, cx, cy, s, 180f, p); return;
            case RIGHT: arrow(c, cx, cy, s, 0f, p); return;
            case UP: arrow(c, cx, cy, s, -90f, p); return;
            case DOWN: arrow(c, cx, cy, s, 90f, p); return;
            case SHIFT:
                path.moveTo(cx, cy - s);
                path.lineTo(cx + s * 0.85f, cy);
                path.lineTo(cx + s * 0.35f, cy);
                path.lineTo(cx + s * 0.35f, cy + s * 0.8f);
                path.lineTo(cx - s * 0.35f, cy + s * 0.8f);
                path.lineTo(cx - s * 0.35f, cy);
                path.lineTo(cx - s * 0.85f, cy);
                path.close();
                if (fill) p.setStyle(Paint.Style.FILL_AND_STROKE);
                c.drawPath(path, p);
                return;
            case BACKSPACE: {
                // A tag pointing left with softly rounded corners, a bold x inside.
                float w = s * 1.15f, h = s * 0.8f;
                path.moveTo(cx - w, cy);
                path.lineTo(cx - w * 0.38f, cy - h);
                path.lineTo(cx + w, cy - h);
                path.lineTo(cx + w, cy + h);
                path.lineTo(cx - w * 0.38f, cy + h);
                path.close();
                p.setPathEffect(new CornerPathEffect(s * 0.28f));
                c.drawPath(path, p);
                p.setPathEffect(null);
                float x = cx + w * 0.22f, d = s * 0.32f;
                c.drawLine(x - d, cy - d, x + d, cy + d, p);
                c.drawLine(x - d, cy + d, x + d, cy - d, p);
                return;
            }
            case ENTER:
                path.moveTo(cx + s * 0.8f, cy - s * 0.6f);
                path.lineTo(cx + s * 0.8f, cy);
                path.lineTo(cx - s * 0.75f, cy);
                c.drawPath(path, p);
                path.reset();
                path.moveTo(cx - s * 0.2f, cy - s * 0.5f);
                path.lineTo(cx - s * 0.75f, cy);
                path.lineTo(cx - s * 0.2f, cy + s * 0.5f);
                c.drawPath(path, p);
                return;
            case KEYBOARD:
                RECT.set(cx - s, cy - s * 0.65f, cx + s, cy + s * 0.65f);
                c.drawRoundRect(RECT, s * 0.25f, s * 0.25f, p);
                c.drawLine(cx - s * 0.5f, cy + s * 0.25f, cx + s * 0.5f, cy + s * 0.25f, p);
                p.setStyle(Paint.Style.FILL);
                float r = Math.max(1f, s * 0.1f);
                for (int i = -1; i <= 1; i++) c.drawCircle(cx + i * s * 0.5f, cy - s * 0.2f, r, p);
                return;
            case GLOBE:
                c.drawCircle(cx, cy, s * 0.95f, p);
                c.drawLine(cx - s * 0.95f, cy, cx + s * 0.95f, cy, p);
                RECT.set(cx - s * 0.42f, cy - s * 0.95f, cx + s * 0.42f, cy + s * 0.95f);
                c.drawOval(RECT, p);
                return;
            case CLIPBOARD:
                RECT.set(cx - s * 0.7f, cy - s * 0.8f, cx + s * 0.7f, cy + s);
                c.drawRoundRect(RECT, s * 0.2f, s * 0.2f, p);
                RECT.set(cx - s * 0.35f, cy - s, cx + s * 0.35f, cy - s * 0.55f);
                c.drawRoundRect(RECT, s * 0.12f, s * 0.12f, p);
                c.drawLine(cx - s * 0.38f, cy - s * 0.05f, cx + s * 0.38f, cy - s * 0.05f, p);
                c.drawLine(cx - s * 0.38f, cy + s * 0.4f, cx + s * 0.38f, cy + s * 0.4f, p);
                return;
            case EMOJI:
                c.drawCircle(cx, cy, s * 0.95f, p);
                p.setStyle(Paint.Style.FILL);
                c.drawCircle(cx - s * 0.33f, cy - s * 0.2f, Math.max(1f, s * 0.11f), p);
                c.drawCircle(cx + s * 0.33f, cy - s * 0.2f, Math.max(1f, s * 0.11f), p);
                p.setStyle(Paint.Style.STROKE);
                RECT.set(cx - s * 0.5f, cy - s * 0.3f, cx + s * 0.5f, cy + s * 0.55f);
                c.drawArc(RECT, 25f, 130f, false, p);
                return;
            case TRANSLATE:
                c.drawLine(cx - s * 0.95f, cy - s * 0.55f, cx - s * 0.05f, cy - s * 0.55f, p);
                c.drawLine(cx - s * 0.5f, cy - s * 0.8f, cx - s * 0.5f, cy - s * 0.55f, p);
                path.moveTo(cx - s * 0.8f, cy - s * 0.55f);
                path.quadTo(cx - s * 0.6f, cy + s * 0.1f, cx - s * 0.05f, cy + s * 0.35f);
                path.moveTo(cx - s * 0.2f, cy - s * 0.55f);
                path.quadTo(cx - s * 0.4f, cy + s * 0.1f, cx - s * 0.95f, cy + s * 0.35f);
                c.drawPath(path, p);
                path.reset();
                path.moveTo(cx + s * 0.05f, cy + s * 0.95f);
                path.lineTo(cx + s * 0.5f, cy - s * 0.05f);
                path.lineTo(cx + s * 0.95f, cy + s * 0.95f);
                c.drawPath(path, p);
                c.drawLine(cx + s * 0.2f, cy + s * 0.55f, cx + s * 0.8f, cy + s * 0.55f, p);
                return;
            case PIN:
                path.moveTo(cx - s * 0.35f, cy - s);
                path.lineTo(cx + s * 0.35f, cy - s);
                path.lineTo(cx + s * 0.25f, cy - s * 0.1f);
                path.lineTo(cx + s * 0.7f, cy + s * 0.3f);
                path.lineTo(cx - s * 0.7f, cy + s * 0.3f);
                path.lineTo(cx - s * 0.25f, cy - s * 0.1f);
                path.close();
                if (fill) p.setStyle(Paint.Style.FILL_AND_STROKE);
                c.drawPath(path, p);
                p.setStyle(Paint.Style.STROKE);
                c.drawLine(cx, cy + s * 0.3f, cx, cy + s, p);
                return;
            case PEN:
                c.save();
                c.rotate(45f, cx, cy);
                RECT.set(cx - s * 0.28f, cy - s * 0.95f, cx + s * 0.28f, cy + s * 0.35f);
                c.drawRoundRect(RECT, s * 0.1f, s * 0.1f, p);
                path.moveTo(cx - s * 0.28f, cy + s * 0.35f);
                path.lineTo(cx, cy + s * 0.95f);
                path.lineTo(cx + s * 0.28f, cy + s * 0.35f);
                c.drawPath(path, p);
                c.drawLine(cx - s * 0.28f, cy - s * 0.55f, cx + s * 0.28f, cy - s * 0.55f, p);
                c.restore();
                return;
            case GEAR: {
                // a toothed wheel: 8 trapezoid teeth around a root circle, a round hole in the middle
                float outer = s, root = s * 0.74f;
                double step = Math.PI / 4;
                for (int i = 0; i < 8; i++) {
                    double a = i * step;
                    double[][] v = {{root, a - 0.30}, {outer, a - 0.17}, {outer, a + 0.17}, {root, a + 0.30}};
                    for (int j = 0; j < 4; j++) {
                        float x = cx + (float) (v[j][0] * Math.cos(v[j][1]));
                        float y = cy + (float) (v[j][0] * Math.sin(v[j][1]));
                        if (i == 0 && j == 0) path.moveTo(x, y);
                        else path.lineTo(x, y);
                    }
                }
                path.close();
                p.setPathEffect(new CornerPathEffect(s * 0.14f));
                c.drawPath(path, p);
                p.setPathEffect(null);
                c.drawCircle(cx, cy, s * 0.34f, p);
                return;
            }
            case SCAN: {
                float a = s * 0.95f, l = s * 0.45f;
                for (int sx = -1; sx <= 1; sx += 2) {
                    for (int sy = -1; sy <= 1; sy += 2) {
                        path.reset();
                        path.moveTo(cx + sx * a, cy + sy * (a - l));
                        path.lineTo(cx + sx * a, cy + sy * a);
                        path.lineTo(cx + sx * (a - l), cy + sy * a);
                        c.drawPath(path, p);
                    }
                }
                c.drawLine(cx - s * 0.55f, cy, cx + s * 0.55f, cy, p);
                return;
            }
            case SWAP:
                c.drawLine(cx - s * 0.9f, cy - s * 0.4f, cx + s * 0.9f, cy - s * 0.4f, p);
                path.moveTo(cx + s * 0.4f, cy - s * 0.85f);
                path.lineTo(cx + s * 0.9f, cy - s * 0.4f);
                path.lineTo(cx + s * 0.4f, cy + s * 0.05f);
                c.drawPath(path, p);
                path.reset();
                c.drawLine(cx + s * 0.9f, cy + s * 0.4f, cx - s * 0.9f, cy + s * 0.4f, p);
                path.moveTo(cx - s * 0.4f, cy - s * 0.05f);
                path.lineTo(cx - s * 0.9f, cy + s * 0.4f);
                path.lineTo(cx - s * 0.4f, cy + s * 0.85f);
                c.drawPath(path, p);
                return;
            case RESIZE: {
                c.drawLine(cx, cy - s * 0.9f, cx, cy + s * 0.9f, p);
                path.moveTo(cx - s * 0.5f, cy - s * 0.4f);
                path.lineTo(cx, cy - s * 0.9f);
                path.lineTo(cx + s * 0.5f, cy - s * 0.4f);
                path.moveTo(cx - s * 0.5f, cy + s * 0.4f);
                path.lineTo(cx, cy + s * 0.9f);
                path.lineTo(cx + s * 0.5f, cy + s * 0.4f);
                c.drawPath(path, p);
                return;
            }
            case SLIDERS: {
                float dotR = s * 0.28f;
                c.drawLine(cx - s, cy - s * 0.5f, cx + s * 0.05f, cy - s * 0.5f, p);
                c.drawCircle(cx + s * 0.35f, cy - s * 0.5f, dotR, p);
                c.drawLine(cx + s * 0.65f, cy - s * 0.5f, cx + s, cy - s * 0.5f, p);
                c.drawLine(cx - s, cy + s * 0.5f, cx - s * 0.65f, cy + s * 0.5f, p);
                c.drawCircle(cx - s * 0.35f, cy + s * 0.5f, dotR, p);
                c.drawLine(cx - s * 0.05f, cy + s * 0.5f, cx + s, cy + s * 0.5f, p);
                return;
            }
            case IMAGE:
                RECT.set(cx - s, cy - s * 0.75f, cx + s, cy + s * 0.75f);
                c.drawRoundRect(RECT, s * 0.2f, s * 0.2f, p);
                c.drawCircle(cx - s * 0.4f, cy - s * 0.2f, s * 0.2f, p);
                path.moveTo(cx - s * 0.9f, cy + s * 0.6f);
                path.lineTo(cx - s * 0.2f, cy + s * 0.05f);
                path.lineTo(cx + s * 0.2f, cy + s * 0.4f);
                path.lineTo(cx + s * 0.5f, cy + s * 0.15f);
                path.lineTo(cx + s * 0.9f, cy + s * 0.55f);
                c.drawPath(path, p);
                return;
            case CLOCK:
                c.drawCircle(cx, cy, s * 0.95f, p);
                path.moveTo(cx, cy - s * 0.5f);
                path.lineTo(cx, cy);
                path.lineTo(cx + s * 0.35f, cy + s * 0.25f);
                c.drawPath(path, p);
                return;
            case PEOPLE:
                c.drawCircle(cx, cy - s * 0.4f, s * 0.35f, p);
                RECT.set(cx - s * 0.8f, cy + s * 0.15f, cx + s * 0.8f, cy + s * 1.6f);
                c.drawArc(RECT, 180f, 180f, false, p);
                return;
            case LEAF:
                path.moveTo(cx - s * 0.8f, cy + s * 0.8f);
                path.quadTo(cx - s * 0.8f, cy - s * 0.8f, cx + s * 0.8f, cy - s * 0.8f);
                path.quadTo(cx + s * 0.8f, cy + s * 0.8f, cx - s * 0.8f, cy + s * 0.8f);
                path.close();
                c.drawPath(path, p);
                c.drawLine(cx - s * 0.8f, cy + s * 0.8f, cx + s * 0.1f, cy - s * 0.1f, p);
                return;
            case CUP:
                path.moveTo(cx - s * 0.8f, cy - s * 0.4f);
                path.lineTo(cx - s * 0.8f, cy + s * 0.2f);
                path.quadTo(cx - s * 0.8f, cy + s * 0.85f, cx - s * 0.1f, cy + s * 0.85f);
                path.quadTo(cx + s * 0.4f, cy + s * 0.85f, cx + s * 0.4f, cy + s * 0.2f);
                path.lineTo(cx + s * 0.4f, cy - s * 0.4f);
                path.close();
                c.drawPath(path, p);
                RECT.set(cx + s * 0.2f, cy - s * 0.2f, cx + s * 0.95f, cy + s * 0.45f);
                c.drawArc(RECT, -90f, 180f, false, p);
                c.drawLine(cx - s * 0.5f, cy - s * 0.9f, cx - s * 0.5f, cy - s * 0.65f, p);
                c.drawLine(cx - s * 0.1f, cy - s * 0.9f, cx - s * 0.1f, cy - s * 0.65f, p);
                return;
            case BOX:
                path.moveTo(cx, cy - s);
                path.lineTo(cx + s * 0.85f, cy - s * 0.5f);
                path.lineTo(cx + s * 0.85f, cy + s * 0.5f);
                path.lineTo(cx, cy + s);
                path.lineTo(cx - s * 0.85f, cy + s * 0.5f);
                path.lineTo(cx - s * 0.85f, cy - s * 0.5f);
                path.close();
                path.moveTo(cx - s * 0.85f, cy - s * 0.5f);
                path.lineTo(cx, cy);
                path.lineTo(cx + s * 0.85f, cy - s * 0.5f);
                path.moveTo(cx, cy);
                path.lineTo(cx, cy + s);
                c.drawPath(path, p);
                return;
            case HASH:
                c.drawLine(cx - s * 0.25f, cy - s * 0.9f, cx - s * 0.55f, cy + s * 0.9f, p);
                c.drawLine(cx + s * 0.55f, cy - s * 0.9f, cx + s * 0.25f, cy + s * 0.9f, p);
                c.drawLine(cx - s * 0.9f, cy - s * 0.35f, cx + s * 0.9f, cy - s * 0.35f, p);
                c.drawLine(cx - s * 0.9f, cy + s * 0.35f, cx + s * 0.9f, cy + s * 0.35f, p);
                return;
            case SCISSORS:
                c.drawCircle(cx - s * 0.55f, cy - s * 0.6f, s * 0.3f, p);
                c.drawCircle(cx - s * 0.55f, cy + s * 0.6f, s * 0.3f, p);
                c.drawLine(cx - s * 0.3f, cy - s * 0.4f, cx + s * 0.95f, cy + s * 0.7f, p);
                c.drawLine(cx - s * 0.3f, cy + s * 0.4f, cx + s * 0.95f, cy - s * 0.7f, p);
                return;
            case SEARCH:
                c.drawCircle(cx - s * 0.08f, cy - s * 0.08f, s * 0.54f, p);
                c.drawLine(cx + s * 0.34f, cy + s * 0.34f, cx + s * 0.88f, cy + s * 0.88f, p);
                return;
            case PAPERCLIP: {
                // 24x24 mockup path, scaled around the centre
                float k = s / 12f, ox = cx - 12 * k, oy = cy - 12 * k;
                path.moveTo(ox + 15.5f * k, oy + 7 * k);
                path.lineTo(ox + 15.5f * k, oy + 17 * k);
                RECT.set(ox + 8.5f * k, oy + 13.5f * k, ox + 15.5f * k, oy + 20.5f * k);
                path.arcTo(RECT, 0f, 180f, false);
                path.lineTo(ox + 8.5f * k, oy + 6 * k);
                RECT.set(ox + 8.5f * k, oy + 3.5f * k, ox + 13.5f * k, oy + 8.5f * k);
                path.arcTo(RECT, 180f, 180f, false);
                path.lineTo(ox + 13.5f * k, oy + 16 * k);
                RECT.set(ox + 11.5f * k, oy + 15f * k, ox + 13.5f * k, oy + 17f * k);
                path.arcTo(RECT, 0f, 180f, false);
                path.lineTo(ox + 11.5f * k, oy + 8.5f * k);
                c.drawPath(path, p);
                return;
            }
            case FILE:
                path.moveTo(cx - s * 0.58f, cy - s * 0.75f);
                path.lineTo(cx + s * 0.1f, cy - s * 0.75f);
                path.lineTo(cx + s * 0.58f, cy - s * 0.25f);
                path.lineTo(cx + s * 0.58f, cy + s * 0.75f);
                path.lineTo(cx - s * 0.58f, cy + s * 0.75f);
                path.close();
                path.moveTo(cx + s * 0.1f, cy - s * 0.75f);
                path.lineTo(cx + s * 0.1f, cy - s * 0.25f);
                path.lineTo(cx + s * 0.58f, cy - s * 0.25f);
                c.drawPath(path, p);
                return;
            case LINES:
                c.drawLine(cx - s * 0.58f, cy - s * 0.5f, cx + s * 0.58f, cy - s * 0.5f, p);
                c.drawLine(cx - s * 0.58f, cy - s * 0.04f, cx + s * 0.58f, cy - s * 0.04f, p);
                c.drawLine(cx - s * 0.58f, cy + s * 0.42f, cx + s * 0.16f, cy + s * 0.42f, p);
                return;
            case CHECKLIST:
                RECT.set(cx - s * 0.67f, cy - s * 0.58f, cx + s * 0.67f, cy + s * 0.58f);
                c.drawRoundRect(RECT, s * 0.1f, s * 0.1f, p);
                c.drawLine(cx - s * 0.33f, cy - s * 0.17f, cx + s * 0.33f, cy - s * 0.17f, p);
                c.drawLine(cx - s * 0.33f, cy + s * 0.17f, cx + s * 0.08f, cy + s * 0.17f, p);
                return;
            case NOTE:
                RECT.set(cx - s * 0.5f, cy - s * 0.75f, cx + s * 0.5f, cy + s * 0.75f);
                c.drawRoundRect(RECT, s * 0.1f, s * 0.1f, p);
                for (int i = -1; i <= 1; i++) {
                    c.drawLine(cx - s * 0.25f, cy + i * s * 0.33f, cx + s * 0.25f, cy + i * s * 0.33f, p);
                }
                return;
            case FACE:
                p.setStyle(Paint.Style.FILL);
                c.drawCircle(cx - s * 0.45f, cy - s * 0.35f, Math.max(1.2f, s * 0.12f), p);
                c.drawCircle(cx + s * 0.45f, cy - s * 0.35f, Math.max(1.2f, s * 0.12f), p);
                p.setStyle(Paint.Style.STROKE);
                RECT.set(cx - s * 0.8f, cy - s * 0.5f, cx + s * 0.8f, cy + s * 0.7f);
                c.drawArc(RECT, 25f, 130f, false, p);
                return;
            default:
        }
    }

    private static void arrow(Canvas c, float cx, float cy, float s, float deg, Paint p) {
        c.save();
        c.rotate(deg, cx, cy);
        c.drawLine(cx - s * 0.7f, cy, cx + s * 0.7f, cy, p);
        Path path = PATH;
        path.reset();
        path.moveTo(cx + s * 0.15f, cy - s * 0.55f);
        path.lineTo(cx + s * 0.7f, cy);
        path.lineTo(cx + s * 0.15f, cy + s * 0.55f);
        c.drawPath(path, p);
        c.restore();
    }
}

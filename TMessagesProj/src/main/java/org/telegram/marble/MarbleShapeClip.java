/*
 * Marblegram — clips circular avatars to the chosen MaterialShapes form.
 */

package org.telegram.marble;

import android.graphics.Canvas;
import android.graphics.Path;

import org.telegram.messenger.ImageReceiver;

public final class MarbleShapeClip {

    private static final Path path = new Path();
    private static float[] cachedShape;
    private static float cachedCx = Float.NaN, cachedCy, cachedR;

    /** Only images that are drawn as circles (avatars) get a shape; rectangles (expanded profile photo) are untouched. */
    public static boolean shouldClip(ImageReceiver receiver, float w, float h, int roundRadius) {
        if (w <= 0 || h <= 0 || Math.abs(w - h) > 1f) {
            return false;
        }
        if (roundRadius < Math.min(w, h) / 2f - 1f) {
            return false;
        }
        return MarbleTheme.avatarShapeRadii() != null;
    }

    public static void clip(Canvas canvas, float cx, float cy, float radius) {
        float[] shape = MarbleTheme.avatarShapeRadii();
        if (shape == null) {
            return;
        }
        if (shape != cachedShape || cx != cachedCx || cy != cachedCy || radius != cachedR) {
            MaterialShapes.morph(path, shape, shape, 0, cx, cy, radius, -90);
            cachedShape = shape;
            cachedCx = cx;
            cachedCy = cy;
            cachedR = radius;
        }
        canvas.clipPath(path);
    }

    private MarbleShapeClip() {}
}

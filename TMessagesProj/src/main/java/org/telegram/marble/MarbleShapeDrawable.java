/*
 * Marblegram — a Drawable that fills one of the M3 Expressive MaterialShapes (tonal container),
 * optionally slowly rotating (used behind empty-state illustrations, icons, avatars placeholders).
 */

package org.telegram.marble;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;

public class MarbleShapeDrawable extends Drawable {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final float[] shape;
    private final float rotationPeriodMs;

    /** @param rotationPeriodMs full turn duration, 0 = static */
    public MarbleShapeDrawable(float[] shape, int color, float rotationPeriodMs) {
        this.shape = shape;
        this.rotationPeriodMs = rotationPeriodMs;
        paint.setColor(color);
    }

    public void setColor(int color) {
        paint.setColor(color);
        invalidateSelf();
    }

    @Override
    public void draw(Canvas canvas) {
        final Rect b = getBounds();
        final float rotation = rotationPeriodMs > 0 ? (SystemClock.elapsedRealtime() % (long) rotationPeriodMs) / rotationPeriodMs * 360f : 0;
        path.rewind();
        MaterialShapes.morph(path, shape, shape, 0, b.exactCenterX(), b.exactCenterY(), Math.min(b.width(), b.height()) / 2f, rotation);
        canvas.drawPath(path, paint);
        if (rotationPeriodMs > 0) {
            invalidateSelf();
        }
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        paint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}

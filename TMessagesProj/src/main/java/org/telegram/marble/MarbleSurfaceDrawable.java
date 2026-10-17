/*
 * Marblegram — M3 container surface (dialogs: 28dp extraLarge, bottom sheets: 28dp top corners)
 * with a soft elevation shadow. Drawn white so callers can tint it with a MULTIPLY color filter,
 * exactly like the nine-patch backgrounds it replaces.
 */

package org.telegram.marble;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

import org.telegram.messenger.AndroidUtilities;

public class MarbleSurfaceDrawable extends Drawable {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect padding;
    private final float radius;
    private final boolean topOnly;
    private final RectF rect = new RectF();
    private final Path path = new Path();

    public MarbleSurfaceDrawable(float radiusDp, boolean topOnly) {
        this.radius = AndroidUtilities.dp(radiusDp);
        this.topOnly = topOnly;
        int p = AndroidUtilities.dp(8);
        padding = topOnly ? new Rect(0, p, 0, 0) : new Rect(p, p, p, p);
        paint.setColor(0xFFFFFFFF);
        paint.setShadowLayer(AndroidUtilities.dp(6), 0, AndroidUtilities.dp(1), 0x22000000);
    }

    public static Drawable dialog() {
        return new MarbleSurfaceDrawable(28, false);
    }

    /** M3 Expressive menus: 16dp (large) container corners. */
    public static Drawable menu() {
        return new MarbleSurfaceDrawable(16, false);
    }

    public static Drawable sheet() {
        return new MarbleSurfaceDrawable(28, true);
    }

    @Override
    public boolean getPadding(@NonNull Rect out) {
        out.set(padding);
        return true;
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Rect b = getBounds();
        rect.set(b.left + padding.left, b.top + padding.top, b.right - padding.right, b.bottom - padding.bottom);
        if (topOnly) {
            path.rewind();
            path.addRoundRect(rect, new float[]{radius, radius, radius, radius, 0, 0, 0, 0}, Path.Direction.CW);
            canvas.drawPath(path, paint);
        } else {
            canvas.drawRoundRect(rect, radius, radius, paint);
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

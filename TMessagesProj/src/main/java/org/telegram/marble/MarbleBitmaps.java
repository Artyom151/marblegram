/*
 * Marblegram — vector-safe replacement for BitmapFactory.decodeResource.
 * Many stock icons were replaced by VectorDrawables (Material Symbols); BitmapFactory returns null
 * for those, which crashed screens that decode icons into Bitmaps. Here we rasterize the drawable instead.
 */

package org.telegram.marble;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

public final class MarbleBitmaps {

    public static Bitmap decodeResource(Resources res, int id) {
        return decodeResource(res, id, null);
    }

    public static Bitmap decodeResource(Resources res, int id, BitmapFactory.Options options) {
        Bitmap bitmap = null;
        try {
            bitmap = BitmapFactory.decodeResource(res, id, options);
        } catch (Throwable ignore) {}
        if (bitmap != null) {
            return bitmap;
        }
        try {
            Drawable drawable = res.getDrawable(id, null);
            int w = Math.max(1, drawable.getIntrinsicWidth()), h = Math.max(1, drawable.getIntrinsicHeight());
            Bitmap.Config config = options != null && options.inPreferredConfig != null ? options.inPreferredConfig : Bitmap.Config.ARGB_8888;
            bitmap = Bitmap.createBitmap(w, h, config);
            drawable.setBounds(0, 0, w, h);
            drawable.draw(new Canvas(bitmap));
            return bitmap;
        } catch (Throwable e) {
            return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        }
    }

    private MarbleBitmaps() {}
}

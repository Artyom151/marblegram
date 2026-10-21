/*
 * Marblegram — M3 Expressive LoadingIndicator / ContainedLoadingIndicator for the View system.
 * Morphs through MaterialShapes.INDETERMINATE with a spring-like overshoot while rotating.
 */

package org.telegram.marble;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.SystemClock;
import android.view.View;

public class LoadingIndicator {

    /** Compose: LoadingIndicatorDefaults — 650ms per morph, indicator 38dp in a 48dp container. */
    private static final long MORPH_MS = 650;
    private static final float FULL_ROTATION_MS = 4666f;
    public static final float ACTIVE_FRACTION = 38f / 48f;

    private final Paint shapePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint containerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final long start = SystemClock.elapsedRealtime();
    private boolean contained;

    public LoadingIndicator() {
        shapePaint.setStyle(Paint.Style.FILL);
        containerPaint.setStyle(Paint.Style.FILL);
    }

    public void setColor(int color) {
        shapePaint.setColor(color);
    }

    /** ContainedLoadingIndicator: shape on a circular container. Pass 0 to disable. */
    public void setContainerColor(int color) {
        contained = color != 0;
        containerPaint.setColor(color);
    }

    public void setAlpha(int alpha) {
        shapePaint.setAlpha(alpha);
        containerPaint.setAlpha(alpha);
    }

    /** Spring approximation (damping 0.6, stiffness ~200) normalized to [0,1] with overshoot. */
    private static float spring(float x) {
        if (x >= 1f) return 1f;
        double zeta = 0.6, w = 9.0;
        double wd = w * Math.sqrt(1 - zeta * zeta);
        double env = Math.exp(-zeta * w * x);
        return (float) (1 - env * (Math.cos(wd * x) + zeta * w / wd * Math.sin(wd * x)));
    }

    /**
     * @param size outer size in px (container diameter); the active indicator is drawn at {@link #ACTIVE_FRACTION}.
     */
    public void draw(Canvas canvas, float cx, float cy, float size, View invalidate) {
        long t = SystemClock.elapsedRealtime() - start;
        int n = MaterialShapes.INDETERMINATE.length;
        long step = t / MORPH_MS;
        float frac = (t % MORPH_MS) / (float) MORPH_MS;
        float[] from = MaterialShapes.INDETERMINATE[(int) (step % n)];
        float[] to = MaterialShapes.INDETERMINATE[(int) ((step + 1) % n)];
        float p = spring(Math.min(1f, frac * 1.6f));

        // constant rotation + extra 90° kick per morph, like Compose implementation
        float rotation = (t % (long) FULL_ROTATION_MS) / FULL_ROTATION_MS * 360f + (step % 4) * 90f + p * 90f;

        if (contained) {
            canvas.drawCircle(cx, cy, size / 2f, containerPaint);
        }
        float radius = size / 2f * ACTIVE_FRACTION * (contained ? 0.82f : 1f);
        // scale pulse: shapes "breathe" slightly mid-morph
        radius *= 1f - 0.06f * (float) Math.sin(Math.PI * Math.min(1f, frac * 1.6f));
        MaterialShapes.morph(path, from, to, Math.max(0f, Math.min(1.15f, p)), cx, cy, radius, rotation);
        canvas.drawPath(path, shapePaint);
        if (invalidate != null) {
            invalidate.invalidate();
        }
    }
}

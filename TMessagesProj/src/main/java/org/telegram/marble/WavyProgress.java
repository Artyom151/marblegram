/*
 * Marblegram — M3 Expressive CircularWavyProgressIndicator (determinate) for Canvas.
 * Active arc is a sine wave around the circle, followed by a gap and a flat track.
 */

package org.telegram.marble;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.SystemClock;

import androidx.core.graphics.ColorUtils;

import org.telegram.messenger.AndroidUtilities;

public final class WavyProgress {

    private static final Path path = new Path();

    /**
     * @param paint stroke paint (its color is the indicator color, width is respected)
     */
    public static void drawCircular(Canvas canvas, Paint paint, float cx, float cy, float diameter, float progress) {
        progress = Math.max(0f, Math.min(1f, progress));
        final int color = paint.getColor();
        final float stroke = paint.getStrokeWidth();
        final float radius = diameter / 2f - stroke;
        final float amplitude = Math.min(AndroidUtilities.dpf2(1.6f), radius * 0.1f);
        final int waves = Math.max(6, Math.round((float) (2 * Math.PI * radius) / AndroidUtilities.dpf2(10)));
        final float phase = (SystemClock.elapsedRealtime() % 1500) / 1500f * (float) (2 * Math.PI);
        final float gapDeg = (float) Math.toDegrees(stroke * 2.5f / radius);

        final float sweep = 360f * progress;
        // track
        if (progress < 1f) {
            paint.setColor(ColorUtils.setAlphaComponent(color, (int) (Color_alpha(color) * 0.24f)));
            float start = -90f + sweep + gapDeg, end = 270f - gapDeg;
            if (end > start) {
                AndroidUtilities.rectTmp.set(cx - radius, cy - radius, cx + radius, cy + radius);
                canvas.drawArc(AndroidUtilities.rectTmp, start, end - start, false, paint);
            }
            paint.setColor(color);
        }
        if (sweep <= 0.5f) {
            return;
        }
        // active wavy arc; amplitude ramps in/out at ends so caps stay on the circle
        path.rewind();
        final int steps = Math.max(8, (int) (sweep / 2f));
        for (int i = 0; i <= steps; i++) {
            float deg = -90f + sweep * i / steps;
            double a = Math.toRadians(deg);
            float ramp = Math.min(1f, Math.min(i, steps - i) / 6f);
            float r = radius + amplitude * ramp * (float) Math.sin(waves * (a + Math.PI / 2) - phase);
            float x = cx + r * (float) Math.cos(a), y = cy + r * (float) Math.sin(a);
            if (i == 0) path.moveTo(x, y); else path.lineTo(x, y);
        }
        canvas.drawPath(path, paint);
    }

    /** Is the M3E wavy style active (Marblegram on + expressive shapes). */
    public static boolean enabled() {
        return MarbleTheme.wavyProgress();
    }

    private static final Path arcPath = new Path();

    /**
     * Drop-in replacement for {@link Canvas#drawArc} with useCenter=false: draws the arc as an
     * M3E wave (amplitude ramps to 0 at both ends so stroke caps stay on the circle).
     */
    public static void drawArc(Canvas canvas, android.graphics.RectF oval, float startDeg, float sweepDeg, Paint paint) {
        final float radius = Math.min(oval.width(), oval.height()) / 2f;
        if (!enabled() || radius < AndroidUtilities.dp(8) || Math.abs(sweepDeg) >= 359.5f) {
            canvas.drawArc(oval, startDeg, sweepDeg, false, paint);
            return;
        }
        final float cx = oval.centerX(), cy = oval.centerY();
        final float amplitude = Math.min(AndroidUtilities.dpf2(1.4f), radius * 0.09f);
        final int waves = Math.max(6, Math.round((float) (2 * Math.PI * radius) / AndroidUtilities.dpf2(9)));
        final float phase = (SystemClock.elapsedRealtime() % 1500) / 1500f * (float) (2 * Math.PI);
        arcPath.rewind();
        final int steps = Math.max(8, (int) (Math.abs(sweepDeg) / 2f));
        for (int i = 0; i <= steps; i++) {
            double a = Math.toRadians(startDeg + sweepDeg * i / steps);
            float ramp = Math.min(1f, Math.min(i, steps - i) / 6f);
            float r = radius + amplitude * ramp * (float) Math.sin(waves * a - phase);
            float x = cx + r * (float) Math.cos(a), y = cy + r * (float) Math.sin(a);
            if (i == 0) arcPath.moveTo(x, y); else arcPath.lineTo(x, y);
        }
        canvas.drawPath(arcPath, paint);
    }

    /**
     * M3E LinearWavyProgressIndicator: wavy active segment, gap, flat track and a stop dot at the end.
     * Falls back to a flat M3 linear indicator when the view is too thin for a wave.
     */
    public static void drawLinear(Canvas canvas, float left, float top, float right, float bottom, float progress, int activeColor, int trackColor, Paint paint) {
        progress = Math.max(0f, Math.min(1f, progress));
        final float h = bottom - top, cy = (top + bottom) / 2f;
        final boolean wavy = h >= AndroidUtilities.dp(8);
        final float stroke = wavy ? Math.min(AndroidUtilities.dpf2(4), h / 3f) : h;
        final float amplitude = wavy ? (h - stroke) / 2f * 0.8f : 0;
        final float width = right - left;
        final float end = left + width * progress;
        final float gap = stroke * 1.5f;
        final Paint.Style oldStyle = paint.getStyle();
        final Paint.Cap oldCap = paint.getStrokeCap();
        final float oldWidth = paint.getStrokeWidth();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(stroke);

        // track + stop indicator
        if (progress < 1f && trackColor != 0) {
            paint.setColor(trackColor);
            float ts = Math.min(right - stroke / 2f, end + gap + stroke / 2f);
            if (right - stroke / 2f > ts) {
                canvas.drawLine(ts, cy, right - stroke / 2f, cy, paint);
            }
            paint.setColor(activeColor);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(right - stroke / 2f, cy, stroke / 2f * 0.8f, paint);
            paint.setStyle(Paint.Style.STROKE);
        }
        if (end - left > stroke / 2f) {
            paint.setColor(activeColor);
            if (amplitude > 0) {
                arcPath.rewind();
                final float wavelength = AndroidUtilities.dp(20);
                final float phase = (SystemClock.elapsedRealtime() % 1200) / 1200f * wavelength;
                final float x0 = left + stroke / 2f, x1 = end - stroke / 2f;
                for (float x = x0; ; x += AndroidUtilities.dpf2(1.5f)) {
                    float xx = Math.min(x, x1);
                    float ramp = Math.min(1f, Math.min(xx - x0, x1 - xx) / AndroidUtilities.dp(6));
                    float y = cy + amplitude * ramp * (float) Math.sin(2 * Math.PI * (xx - x0 + phase) / wavelength);
                    if (x == x0) arcPath.moveTo(xx, y); else arcPath.lineTo(xx, y);
                    if (xx >= x1) break;
                }
                canvas.drawPath(arcPath, paint);
            } else {
                canvas.drawLine(left + stroke / 2f, cy, Math.max(left + stroke / 2f, end - stroke / 2f), cy, paint);
            }
        }
        paint.setStyle(oldStyle);
        paint.setStrokeCap(oldCap);
        paint.setStrokeWidth(oldWidth);
    }

    private static int Color_alpha(int color) {
        return color >>> 24;
    }

    private WavyProgress() {}
}

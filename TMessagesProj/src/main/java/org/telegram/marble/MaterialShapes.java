/*
 * Marblegram — subset of M3 Expressive MaterialShapes expressed as polar radius functions,
 * so any two shapes can be morphed by interpolating radii at the same angles.
 */

package org.telegram.marble;

import android.graphics.Path;

public final class MaterialShapes {

    public static final int SAMPLES = 144;

    public static final float[] CIRCLE = build(a -> 1f);
    public static final float[] SOFT_BURST = build(a -> star(a, 10, .16f, 2.2f));
    public static final float[] COOKIE_9 = build(a -> cookie(a, 9, .07f));
    public static final float[] PENTAGON = roundedPolygon(5, 0.42f, (float) -Math.PI / 2);
    public static final float[] PILL = build(a -> superEllipse(a + (float) Math.PI / 4, 1f, .62f, 3.2f));
    public static final float[] SUNNY = build(a -> star(a, 8, .1f, 1.4f));
    public static final float[] COOKIE_4 = build(a -> cookie(a + (float) Math.PI / 4, 4, .13f));
    public static final float[] OVAL = build(a -> superEllipse(a - (float) Math.PI / 4, 1f, .7f, 2f));
    public static final float[] COOKIE_12 = build(a -> cookie(a, 12, .06f));
    public static final float[] FLOWER = build(a -> star(a, 8, .22f, 1.1f));

    /** Same order as Compose LoadingIndicatorDefaults.IndeterminateIndicatorPolygons. */
    public static final float[][] INDETERMINATE = {SOFT_BURST, COOKIE_9, PENTAGON, PILL, SUNNY, COOKIE_4, OVAL};

    private interface Fn { float r(float angle); }

    private static float[] build(Fn fn) {
        float[] r = new float[SAMPLES];
        float max = 0;
        for (int i = 0; i < SAMPLES; i++) {
            r[i] = fn.r((float) (2 * Math.PI * i / SAMPLES));
            max = Math.max(max, r[i]);
        }
        for (int i = 0; i < SAMPLES; i++) r[i] /= max;
        return r;
    }

    private static float cookie(float a, int n, float depth) {
        return 1f - depth * (0.5f - 0.5f * (float) Math.cos(n * a));
    }

    /** Rounded star: sharpness < 1 is wide/soft lobes, > 1 narrower peaks. */
    private static float star(float a, int n, float depth, float sharpness) {
        float c = 0.5f + 0.5f * (float) Math.cos(n * a);
        return 1f - depth + depth * (float) Math.pow(c, sharpness);
    }

    /**
     * True rounded regular polygon (like graphics-shapes RoundedPolygon with CornerRounding):
     * Minkowski sum of a smaller polygon and a circle of radius {@code corner} (fraction of circumradius).
     * Sampled as a convex outline, then resampled to polar radii.
     */
    private static float[] roundedPolygon(int n, float corner, float rotation) {
        final int fine = 1440;
        final double inner = 1.0 - corner; // circumradius of the core polygon
        double[] px = new double[fine], py = new double[fine];
        for (int i = 0; i < fine; i++) {
            double nx = Math.cos(2 * Math.PI * i / fine), ny = Math.sin(2 * Math.PI * i / fine);
            // support point of the core polygon in direction (nx, ny): its best vertex
            double best = -1e9, bx = 0, by = 0;
            for (int k = 0; k < n; k++) {
                double va = rotation + 2 * Math.PI * k / n;
                double vx = inner * Math.cos(va), vy = inner * Math.sin(va);
                double d = vx * nx + vy * ny;
                if (d > best) { best = d; bx = vx; by = vy; }
            }
            px[i] = bx + corner * nx;
            py[i] = by + corner * ny;
        }
        float[] r = new float[SAMPLES];
        for (int s = 0; s < SAMPLES; s++) {
            double a = 2 * Math.PI * s / SAMPLES;
            double dx = Math.cos(a), dy = Math.sin(a);
            // intersect ray from origin with the convex outline
            double hit = 0;
            for (int i = 0; i < fine; i++) {
                int j = (i + 1) % fine;
                double ex = px[j] - px[i], ey = py[j] - py[i];
                double den = dx * ey - dy * ex;
                if (Math.abs(den) < 1e-12) continue;
                double t = (px[i] * ey - py[i] * ex) / den;
                double u = (px[i] * dy - py[i] * dx) / den;
                if (t > 0 && u >= 0 && u <= 1) { hit = t; break; }
            }
            r[s] = (float) hit;
        }
        float max = 0;
        for (float v : r) max = Math.max(max, v);
        for (int s = 0; s < SAMPLES; s++) r[s] /= max;
        return r;
    }

    private static float superEllipse(float a, float w, float h, float n) {
        double c = Math.abs(Math.cos(a)) / w, s = Math.abs(Math.sin(a)) / h;
        return (float) Math.pow(Math.pow(c, n) + Math.pow(s, n), -1.0 / n);
    }

    /** Writes a closed path for lerp(from, to, t), centered at cx,cy, scaled to radius, rotated by degrees. */
    public static void morph(Path path, float[] from, float[] to, float t, float cx, float cy, float radius, float rotationDeg) {
        path.rewind();
        double rot = Math.toRadians(rotationDeg);
        float px0 = 0, py0 = 0, mx = 0, my = 0;
        float[] xs = new float[SAMPLES], ys = new float[SAMPLES];
        for (int i = 0; i < SAMPLES; i++) {
            float r = (from[i] + (to[i] - from[i]) * t) * radius;
            double a = 2 * Math.PI * i / SAMPLES + rot;
            xs[i] = cx + r * (float) Math.cos(a);
            ys[i] = cy + r * (float) Math.sin(a);
        }
        // smooth closed curve through midpoints using quadratic beziers
        mx = (xs[SAMPLES - 1] + xs[0]) / 2f;
        my = (ys[SAMPLES - 1] + ys[0]) / 2f;
        path.moveTo(mx, my);
        for (int i = 0; i < SAMPLES; i++) {
            int n = (i + 1) % SAMPLES;
            px0 = (xs[i] + xs[n]) / 2f;
            py0 = (ys[i] + ys[n]) / 2f;
            path.quadTo(xs[i], ys[i], px0, py0);
        }
        path.close();
    }

    private MaterialShapes() {}
}

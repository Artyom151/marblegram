/*
 * Marblegram — M3 type scale (incl. Expressive "Emphasized" variants) for the View system.
 * Bundles Roboto Flex (the variable typeface of the M3 Expressive type scale, OFL) and instances it per
 * weight / optical size via font-variation axes. Falls back to the system sans-serif below API 26.
 */

package org.telegram.marble;

import android.graphics.Typeface;
import android.os.Build;
import android.util.TypedValue;
import android.widget.TextView;

public final class MarbleTypography {

    /** size sp, line height sp, weight, emphasized weight — values from M3 tokens. */
    public enum Style {
        DISPLAY_LARGE(57, 64, 400, 500), DISPLAY_MEDIUM(45, 52, 400, 500), DISPLAY_SMALL(36, 44, 400, 500),
        HEADLINE_LARGE(32, 40, 400, 500), HEADLINE_MEDIUM(28, 36, 400, 500), HEADLINE_SMALL(24, 32, 400, 500),
        TITLE_LARGE(22, 28, 400, 500), TITLE_MEDIUM(16, 24, 500, 600), TITLE_SMALL(14, 20, 500, 600),
        BODY_LARGE(16, 24, 400, 500), BODY_MEDIUM(14, 20, 400, 500), BODY_SMALL(12, 16, 400, 500),
        LABEL_LARGE(14, 20, 500, 600), LABEL_MEDIUM(12, 16, 500, 600), LABEL_SMALL(11, 16, 500, 600);

        public final int size, lineHeight, weight, emphasizedWeight;

        Style(int size, int lineHeight, int weight, int emphasizedWeight) {
            this.size = size;
            this.lineHeight = lineHeight;
            this.weight = weight;
            this.emphasizedWeight = emphasizedWeight;
        }
    }

    private static Typeface family;
    private static final Typeface[] cache = new Typeface[10];

    private static Typeface family() {
        if (family == null) {
            Typeface found = null;
            for (String name : new String[]{"google-sans-flex", "google-sans-text", "google-sans"}) {
                Typeface t = Typeface.create(name, Typeface.NORMAL);
                if (t != null && !t.equals(Typeface.DEFAULT)) {
                    found = t;
                    break;
                }
            }
            family = found != null ? found : Typeface.create("sans-serif", Typeface.NORMAL);
        }
        return family;
    }

    private static final String FLEX_ASSET = "fonts/marble_roboto_flex.ttf";
    private static final java.util.HashMap<String, Typeface> flexCache = new java.util.HashMap<>();
    private static boolean flexBroken;

    /** Roboto Flex instance: wght 100..1000, opsz 8..144 (optical size = text size), wdth 25..151. */
    public static Typeface flex(int weight, float opticalSize, float width) {
        return flex(weight, opticalSize, width, 0);
    }

    /** @param slant degrees, -10..0 (Roboto Flex 'slnt' axis; negative = italic-like oblique) */
    public static Typeface flex(int weight, float opticalSize, float width, float slant) {
        if (Build.VERSION.SDK_INT < 26 || flexBroken) {
            return null;
        }
        final String key = weight + "/" + Math.round(opticalSize) + "/" + Math.round(width) + "/" + Math.round(slant);
        Typeface t = flexCache.get(key);
        if (t == null) {
            try {
                t = new Typeface.Builder(org.telegram.messenger.ApplicationLoader.applicationContext.getAssets(), FLEX_ASSET)
                    .setFontVariationSettings("'wght' " + weight + ", 'opsz' " + Math.max(8, Math.min(144, Math.round(opticalSize))) + ", 'wdth' " + width + ", 'slnt' " + slant)
                    .build();
            } catch (Throwable e) {
                flexBroken = true;
                return null;
            }
            flexCache.put(key, t);
        }
        return t;
    }

    public static Typeface weight(int weight) {
        Typeface t = flex(weight, 14, 100);
        if (t != null) {
            return t;
        }
        int idx = Math.max(1, Math.min(9, weight / 100));
        if (cache[idx] == null) {
            if (Build.VERSION.SDK_INT >= 28) {
                cache[idx] = Typeface.create(family(), idx * 100, false);
            } else {
                cache[idx] = Typeface.create(family(), idx >= 6 ? Typeface.BOLD : Typeface.NORMAL);
            }
        }
        return cache[idx];
    }

    public static Typeface typeface(Style style, boolean emphasized) {
        final int w = emphasized ? style.emphasizedWeight : style.weight;
        // display/headline styles get a slightly condensed width when emphasized (expressive look)
        Typeface t = flex(w, style.size, emphasized && style.size >= 24 ? 92 : 100);
        return t != null ? t : weight(w);
    }

    /**
     * Maps Telegram's bundled Roboto family files to Roboto Flex instances so manually-drawn text
     * (Paint/StaticLayout in cells, editors, stories, calls, counters) follows the M3 type family too.
     * Mono (code), Merriweather (editor text styles) and gram.ttf (icon glyphs) are intentionally kept.
     */
    public static Typeface mapTelegramFont(String assetPath) {
        if (assetPath == null || !MarbleTheme.expressiveTypography()) return null;
        switch (assetPath) {
            case "fonts/rmedium.ttf": return flex(600, 14, 100);
            case "fonts/rextrabold.ttf": return flex(800, 20, 100);
            case "fonts/rcondensedbold.ttf": return flex(700, 20, 75);
            case "fonts/ritalic.ttf": return flex(400, 14, 100, -10);
            case "fonts/rmediumitalic.ttf": return flex(600, 14, 100, -10);
            case "fonts/num.otf": return flex(700, 28, 100);
            default: return null;
        }
    }

    /** Replacement for AndroidUtilities.bold(): M3 titles are Medium, Expressive emphasized is SemiBold. */
    public static Typeface emphasized() {
        return weight(600);
    }

    public static Typeface label() {
        return weight(500);
    }

    public static void apply(TextView view, Style style, boolean emphasized) {
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, style.size);
        view.setTypeface(typeface(style, emphasized));
        if (Build.VERSION.SDK_INT >= 28) {
            view.setLineHeight((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, style.lineHeight, view.getResources().getDisplayMetrics()));
        }
    }

    /**
     * Makes Roboto Flex (regular, opsz 14) the default typeface for every TextView/Paint that doesn't set one,
     * so the whole UI follows the M3 type family. Only when Expressive typography is on.
     */
    public static void installGlobal() {
        try {
            if (!MarbleTheme.expressiveTypography()) return;
            final Typeface regular = flex(400, 14, 100);
            final Typeface bold = flex(600, 14, 100);
            if (regular == null) return;
            setStatic("DEFAULT", regular);
            setStatic("SANS_SERIF", regular);
            if (bold != null) setStatic("DEFAULT_BOLD", bold);
            try {
                java.lang.reflect.Method m = Typeface.class.getDeclaredMethod("setDefault", Typeface.class);
                m.setAccessible(true);
                m.invoke(null, regular);
            } catch (Throwable ignore) {}
            try {
                java.lang.reflect.Field f = Typeface.class.getDeclaredField("sSystemFontMap");
                f.setAccessible(true);
                @SuppressWarnings("unchecked")
                java.util.Map<String, Typeface> map = new java.util.HashMap<>((java.util.Map<String, Typeface>) f.get(null));
                map.put("sans-serif", regular);
                f.set(null, map);
            } catch (Throwable ignore) {}
        } catch (Throwable e) {
            org.telegram.messenger.FileLog.e(e);
        }
    }

    private static void setStatic(String name, Typeface value) {
        try {
            java.lang.reflect.Field f = Typeface.class.getDeclaredField(name);
            f.setAccessible(true);
            f.set(null, value);
        } catch (Throwable ignore) {}
    }

    private MarbleTypography() {}
}

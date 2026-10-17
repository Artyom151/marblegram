/*
 * Marblegram — Material 3 Expressive redesign layer.
 * Maps a Material You (Android 12+) tonal palette onto Telegram theme keys.
 */

package org.telegram.marble;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.util.SparseIntArray;

import androidx.core.graphics.ColorUtils;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.ui.ActionBar.Theme;

import static org.telegram.ui.ActionBar.Theme.*;

public class MarbleTheme {

    public static final String PREFS = "marblegram";
    /** Seed used before Android 12 or when dynamic color is disabled (M3 baseline purple-blue). */
    public static final int DEFAULT_SEED = 0xFF4F6BD8;

    // Tones used: 0,10,50,100,200,300,400,500,600,700,800,900,950,1000 (system naming)
    private static final int[] TONES = {0, 10, 50, 100, 200, 300, 400, 500, 600, 700, 800, 900, 1000};

    private static SharedPreferences prefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static boolean isEnabled() {
        try {
            return prefs().getBoolean("enabled", true);
        } catch (Exception e) {
            return true;
        }
    }

    public static boolean useDynamicColor() {
        try {
            return Build.VERSION.SDK_INT >= 31 && prefs().getBoolean("dynamic", true);
        } catch (Exception e) {
            return Build.VERSION.SDK_INT >= 31;
        }
    }

    public static void setEnabled(boolean enabled) {
        prefs().edit().putBoolean("enabled", enabled).apply();
        invalidate();
        Theme.refreshThemeColors();
    }

    public static void setDynamic(boolean dynamic) {
        prefs().edit().putBoolean("dynamic", dynamic).apply();
        invalidate();
        Theme.refreshThemeColors();
    }

    /** Seed presets offered when dynamic color is off (or below Android 12). */
    public static final int[] SEEDS = {DEFAULT_SEED, 0xFF386A20, 0xFF7E57C2, 0xFFB3261E, 0xFFE8710A, 0xFF006A6A, 0xFF9C4146, 0xFF5D5F5F};

    public static int getSeedIndex() {
        int i = prefs().getInt("seedIndex", 0);
        return i >= 0 && i < SEEDS.length ? i : 0;
    }

    public static void setSeedIndex(int index) {
        prefs().edit().putInt("seedIndex", index).apply();
        invalidate();
        Theme.refreshThemeColors();
    }

    public static int getSeed() {
        return SEEDS[getSeedIndex()];
    }

    // --- Typography / shapes / components -----------------------------------------------------

    public static boolean expressiveTypography() {
        try {
            return isEnabled() && prefs().getBoolean("typography", true);
        } catch (Exception e) {
            return false;
        }
    }

    public static void setExpressiveTypography(boolean value) {
        prefs().edit().putBoolean("typography", value).apply();
        org.telegram.messenger.AndroidUtilities.mediumTypeface = null;
    }

    public static boolean expressiveShapes() {
        try {
            return isEnabled() && prefs().getBoolean("shapes", true);
        } catch (Exception e) {
            return false;
        }
    }

    public static void setExpressiveShapes(boolean value) {
        prefs().edit().putBoolean("shapes", value).apply();
    }

    public static final int AVATAR_CIRCLE = 0, AVATAR_COOKIE = 1, AVATAR_SUNNY = 2, AVATAR_PENTAGON = 3, AVATAR_SOFT_BURST = 4, AVATAR_FLOWER = 5;

    public static int avatarShape() {
        try {
            return isEnabled() ? prefs().getInt("avatarShape", AVATAR_COOKIE) : AVATAR_CIRCLE;
        } catch (Exception e) {
            return AVATAR_CIRCLE;
        }
    }

    public static void setAvatarShape(int shape) {
        prefs().edit().putInt("avatarShape", shape).apply();
    }

    public static float[] avatarShapeRadii() {
        switch (avatarShape()) {
            case AVATAR_COOKIE: return MaterialShapes.COOKIE_12;
            case AVATAR_SUNNY: return MaterialShapes.SUNNY;
            case AVATAR_PENTAGON: return MaterialShapes.PENTAGON;
            case AVATAR_SOFT_BURST: return MaterialShapes.SOFT_BURST;
            case AVATAR_FLOWER: return MaterialShapes.FLOWER;
            default: return null;
        }
    }

    // --- Component toggles ---------------------------------------------------------------------

    private static boolean flag(String key, boolean def) {
        try {
            return isEnabled() && prefs().getBoolean(key, def);
        } catch (Exception e) {
            return false;
        }
    }

    public static void setFlag(String key, boolean value) {
        prefs().edit().putBoolean(key, value).apply();
    }

    public static boolean rawFlag(String key, boolean def) {
        try {
            return prefs().getBoolean(key, def);
        } catch (Exception e) {
            return def;
        }
    }

    public static final String FAB_MENU = "fabMenu", WAVY = "wavy", SEGMENTED = "segmented", FOLDER_CHIPS = "folderChips";

    public static boolean fabMenu() { return flag(FAB_MENU, true); }
    public static boolean wavyProgress() { return expressiveShapes() && flag(WAVY, true); }
    public static boolean segmentedLists() { return expressiveShapes() && flag(SEGMENTED, true); }
    public static boolean folderChips() { return expressiveShapes() && flag(FOLDER_CHIPS, true); }

    /** Message bubble corner radius in dp (M3E default 20). */
    public static int bubbleRadius() {
        try {
            return prefs().getInt("bubbleRadius", 20);
        } catch (Exception e) {
            return 20;
        }
    }

    public static void setBubbleRadius(int dp) {
        prefs().edit().putInt("bubbleRadius", dp).apply();
    }

    // --- Round video messages ---------------------------------------------------------------------

    /** true = start recording with the front camera, false = back camera. */
    public static boolean roundFrontCamera() {
        try {
            return prefs().getBoolean("roundFront", true);
        } catch (Exception e) {
            return true;
        }
    }

    public static void setRoundFrontCamera(boolean front) {
        prefs().edit().putBoolean("roundFront", front).apply();
    }

    /** Video messages are always circles (the corner-roundness option was removed at the user's request). */
    public static int roundVideoCorner() {
        return 100;
    }

    public static int roundVideoRadius(int size) {
        return size / 2;
    }

    public static boolean containedLoading() {
        try {
            return prefs().getBoolean("containedLoading", true);
        } catch (Exception e) {
            return true;
        }
    }

    public static void setContainedLoading(boolean value) {
        prefs().edit().putBoolean("containedLoading", value).apply();
    }

    /** Tonal palette: index by system tone (0..1000). */
    public static final class Palette {
        final int[] a1 = new int[TONES.length], a2 = new int[TONES.length], a3 = new int[TONES.length],
                n1 = new int[TONES.length], n2 = new int[TONES.length];

        static int idx(int tone) {
            for (int i = 0; i < TONES.length; i++) if (TONES[i] == tone) return i;
            throw new IllegalArgumentException("tone " + tone);
        }

        public int a1(int t) { return a1[idx(t)]; }
        public int a2(int t) { return a2[idx(t)]; }
        public int a3(int t) { return a3[idx(t)]; }
        public int n1(int t) { return n1[idx(t)]; }
        public int n2(int t) { return n2[idx(t)]; }
    }

    private static Palette cachedPalette;
    private static int cachedKey;

    public static Palette palette() {
        final boolean dyn = useDynamicColor();
        final int key = dyn ? 1 : getSeed();
        if (cachedPalette != null && cachedKey == key) {
            return cachedPalette;
        }
        Palette p = new Palette();
        if (dyn) {
            fillSystem(p);
        } else {
            fillFromSeed(p, getSeed());
        }
        cachedPalette = p;
        cachedKey = key;
        return p;
    }

    private static void fillSystem(Palette p) {
        Context ctx = ApplicationLoader.applicationContext;
        int[][] ids = {
            {android.R.color.system_accent1_0, android.R.color.system_accent1_10, android.R.color.system_accent1_50, android.R.color.system_accent1_100, android.R.color.system_accent1_200, android.R.color.system_accent1_300, android.R.color.system_accent1_400, android.R.color.system_accent1_500, android.R.color.system_accent1_600, android.R.color.system_accent1_700, android.R.color.system_accent1_800, android.R.color.system_accent1_900, android.R.color.system_accent1_1000},
            {android.R.color.system_accent2_0, android.R.color.system_accent2_10, android.R.color.system_accent2_50, android.R.color.system_accent2_100, android.R.color.system_accent2_200, android.R.color.system_accent2_300, android.R.color.system_accent2_400, android.R.color.system_accent2_500, android.R.color.system_accent2_600, android.R.color.system_accent2_700, android.R.color.system_accent2_800, android.R.color.system_accent2_900, android.R.color.system_accent2_1000},
            {android.R.color.system_accent3_0, android.R.color.system_accent3_10, android.R.color.system_accent3_50, android.R.color.system_accent3_100, android.R.color.system_accent3_200, android.R.color.system_accent3_300, android.R.color.system_accent3_400, android.R.color.system_accent3_500, android.R.color.system_accent3_600, android.R.color.system_accent3_700, android.R.color.system_accent3_800, android.R.color.system_accent3_900, android.R.color.system_accent3_1000},
            {android.R.color.system_neutral1_0, android.R.color.system_neutral1_10, android.R.color.system_neutral1_50, android.R.color.system_neutral1_100, android.R.color.system_neutral1_200, android.R.color.system_neutral1_300, android.R.color.system_neutral1_400, android.R.color.system_neutral1_500, android.R.color.system_neutral1_600, android.R.color.system_neutral1_700, android.R.color.system_neutral1_800, android.R.color.system_neutral1_900, android.R.color.system_neutral1_1000},
            {android.R.color.system_neutral2_0, android.R.color.system_neutral2_10, android.R.color.system_neutral2_50, android.R.color.system_neutral2_100, android.R.color.system_neutral2_200, android.R.color.system_neutral2_300, android.R.color.system_neutral2_400, android.R.color.system_neutral2_500, android.R.color.system_neutral2_600, android.R.color.system_neutral2_700, android.R.color.system_neutral2_800, android.R.color.system_neutral2_900, android.R.color.system_neutral2_1000},
        };
        int[][] out = {p.a1, p.a2, p.a3, p.n1, p.n2};
        for (int s = 0; s < ids.length; s++) {
            for (int i = 0; i < TONES.length; i++) {
                out[s][i] = ctx.getColor(ids[s][i]);
            }
        }
    }

    /** Approximate HCT tonal palettes using CIE-LAB lightness (good enough without the material-color-utilities dependency). */
    private static void fillFromSeed(Palette p, int seed) {
        double[] lab = new double[3];
        ColorUtils.colorToLAB(seed, lab);
        double hue = Math.atan2(lab[2], lab[1]);
        double chroma = Math.hypot(lab[1], lab[2]);
        double a1c = Math.max(chroma, 48), a2c = 16, a3c = 24, n1c = 4, n2c = 8;
        double a3h = hue + Math.toRadians(60);
        for (int i = 0; i < TONES.length; i++) {
            double L = 100 - TONES[i] / 10.0; // system tone 0 => L100, 1000 => L0
            p.a1[i] = lab(L, a1c * chromaScale(L), hue);
            p.a2[i] = lab(L, a2c * chromaScale(L), hue);
            p.a3[i] = lab(L, a3c * chromaScale(L), a3h);
            p.n1[i] = lab(L, n1c, hue);
            p.n2[i] = lab(L, n2c, hue);
        }
    }

    private static double chromaScale(double L) {
        // taper chroma at extremes so near-white / near-black stay clean
        return Math.max(0, Math.min(1, Math.min(L, 100 - L) / 35.0));
    }

    private static int lab(double L, double c, double h) {
        if (L >= 100) return Color.WHITE;
        if (L <= 0) return Color.BLACK;
        return ColorUtils.LABToColor(L, c * Math.cos(h), c * Math.sin(h));
    }

    // --- M3 color roles ---------------------------------------------------------------------

    public static final class Roles {
        public int primary, onPrimary, primaryContainer, onPrimaryContainer;
        public int secondary, secondaryContainer, onSecondaryContainer;
        public int tertiary, tertiaryContainer, onTertiaryContainer;
        public int surface, onSurface, onSurfaceVariant, outline, outlineVariant;
        public int surfaceContainerLowest, surfaceContainerLow, surfaceContainer, surfaceContainerHigh, surfaceContainerHighest;
        public int inverseSurface, inverseOnSurface, error;
    }

    private static final Roles[] cachedRoles = new Roles[2];

    /**
     * Android 12+: wallpaper colors may change while the app is in background.
     * Call from LaunchActivity.onResume / onConfigurationChanged.
     */
    public static void checkDynamicColorsChanged() {
        try {
            if (!isEnabled() || !useDynamicColor() || cachedPalette == null) {
                return;
            }
            int now = ApplicationLoader.applicationContext.getColor(android.R.color.system_accent1_600);
            int neutral = ApplicationLoader.applicationContext.getColor(android.R.color.system_neutral1_50);
            if (now != cachedPalette.a1(600) || neutral != cachedPalette.n1(50)) {
                invalidate();
                Theme.refreshThemeColors();
            }
        } catch (Exception ignore) {}
    }

    /** Drops cached palette/roles, e.g. after a wallpaper (Material You) or settings change. */
    public static void invalidate() {
        cachedPalette = null;
        cachedRoles[0] = cachedRoles[1] = null;
    }

    public static Roles roles(boolean dark) {
        Roles cached = cachedRoles[dark ? 1 : 0];
        if (cached != null && cachedPalette != null) {
            return cached;
        }
        Roles r = buildRoles(dark);
        cachedRoles[dark ? 1 : 0] = r;
        return r;
    }

    private static Roles buildRoles(boolean dark) {
        Palette p = palette();
        Roles r = new Roles();
        if (!dark) {
            r.primary = p.a1(600); r.onPrimary = p.a1(0);
            r.primaryContainer = p.a1(100); r.onPrimaryContainer = p.a1(900);
            r.secondary = p.a2(600); r.secondaryContainer = p.a2(100); r.onSecondaryContainer = p.a2(900);
            r.tertiary = p.a3(600); r.tertiaryContainer = p.a3(100); r.onTertiaryContainer = p.a3(900);
            r.surface = p.n1(10); r.onSurface = p.n1(900); r.onSurfaceVariant = p.n2(700);
            r.outline = p.n2(500); r.outlineVariant = p.n2(200);
            r.surfaceContainerLowest = p.n1(0);
            r.surfaceContainerLow = blend(p.n1(10), p.n1(50), .5f);
            r.surfaceContainer = p.n1(50);
            r.surfaceContainerHigh = blend(p.n1(50), p.n1(100), .5f);
            r.surfaceContainerHighest = p.n1(100);
            r.inverseSurface = p.n1(800); r.inverseOnSurface = p.n1(50);
            r.error = 0xFFBA1A1A;
        } else {
            r.primary = p.a1(200); r.onPrimary = p.a1(800);
            r.primaryContainer = p.a1(700); r.onPrimaryContainer = p.a1(100);
            r.secondary = p.a2(200); r.secondaryContainer = p.a2(700); r.onSecondaryContainer = p.a2(100);
            r.tertiary = p.a3(200); r.tertiaryContainer = p.a3(700); r.onTertiaryContainer = p.a3(100);
            r.surface = blend(p.n1(900), p.n1(1000), .35f); r.onSurface = p.n1(100); r.onSurfaceVariant = p.n2(200);
            r.outline = p.n2(400); r.outlineVariant = p.n2(700);
            r.surfaceContainerLowest = blend(p.n1(900), p.n1(1000), .7f);
            r.surfaceContainerLow = p.n1(900);
            r.surfaceContainer = blend(p.n1(900), p.n1(800), .35f);
            r.surfaceContainerHigh = blend(p.n1(900), p.n1(800), .7f);
            r.surfaceContainerHighest = p.n1(800);
            r.inverseSurface = p.n1(100); r.inverseOnSurface = p.n1(800);
            r.error = 0xFFFFB4AB;
        }
        return r;
    }

    /**
     * Private call background (Material You): kind 0 = ringing/connecting, 1 = active;
     * variant 0 = normal, 1 = dark, 2 = light. Green (connected) and red (error) stay semantic.
     */
    public static int[] callGradient(int kind, int variant) {
        Palette p = palette();
        int t = variant == 1 ? 700 : variant == 2 ? 400 : 500;
        int t2 = variant == 1 ? 800 : variant == 2 ? 500 : 600;
        if (kind == 0) {
            return new int[]{p.a3(t), p.a1(t2), p.a1(t), p.a2(t)};
        }
        return new int[]{p.a1(t), p.a1(t2), p.a3(t2), p.a2(t)};
    }

    /**
     * Second pass: every remaining Telegram-blue accent (keys not mapped explicitly above) is moved
     * to the Material palette, so no stray stock-blue survives anywhere in the UI.
     * Peer/avatar/name colors are skipped — they must stay distinguishable.
     */
    private static void retintRemainingAccents(SparseIntArray c, SparseIntArray before, Roles r, boolean dark) {
        final float[] hsv = new float[3];
        for (int key = 0; key < Theme.colorsCount; key++) {
            final int idx = c.indexOfKey(key);
            if (idx >= 0 && (before.indexOfKey(key) < 0 || before.get(key) != c.valueAt(idx))) {
                continue; // set explicitly by Marblegram
            }
            final String name = org.telegram.ui.ActionBar.ThemeColors.getStringName(key);
            if (name == null || name.startsWith("avatar_") || name.contains("Name") || name.startsWith("color_") || name.contains("peer") || name.startsWith("chat_wallpaper")
                || name.startsWith("premium") || name.startsWith("stars") || name.startsWith("statistic") || name.startsWith("calls_") || name.startsWith("voipgroup_") || name.contains("Gradient")) {
                continue;
            }
            final int color = idx >= 0 ? c.valueAt(idx) : Theme.getDefaultColor(key);
            final int alpha = Color.alpha(color);
            if (alpha == 0) continue;
            Color.colorToHSV(color, hsv);
            if (hsv[0] < 190 || hsv[0] > 225 || hsv[1] < 0.35f || hsv[2] < 0.35f) {
                continue; // not a Telegram-blue accent
            }
            final double lum = ColorUtils.calculateLuminance(color | 0xFF000000);
            final int target = dark
                ? (lum > 0.35 ? r.primary : r.primaryContainer)
                : (lum > 0.6 ? r.primaryContainer : r.primary);
            c.put(key, ColorUtils.setAlphaComponent(target, alpha));
        }
    }

    /** Accent for always-dark media editor UI (primary of the dark scheme). */
    public static int editorAccent(int fallback) {
        try {
            return isEnabled() && ApplicationLoader.applicationContext != null ? roles(true).primary : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    /** Re-tones an arbitrary color to an M3 tonal container keeping its hue (category icon background). */
    public static int tonal(int color, boolean dark) {
        return tone(color, dark ? 30 : 88, dark ? 36 : 40);
    }

    /** Matching on-container tone for the glyph drawn over {@link #tonal(int, boolean)}. */
    public static int onTonal(int color, boolean dark) {
        return tone(color, dark ? 88 : 28, dark ? 30 : 52);
    }

    private static int tone(int color, double L, double maxChroma) {
        double[] lab = new double[3];
        ColorUtils.colorToLAB(color, lab);
        double hue = Math.atan2(lab[2], lab[1]);
        double chroma = Math.min(Math.hypot(lab[1], lab[2]), maxChroma);
        return lab(L, chroma, hue);
    }

    private static int blend(int a, int b, float t) {
        return ColorUtils.blendARGB(a, b, t);
    }

    private static int alpha(int c, float a) {
        return ColorUtils.setAlphaComponent(c, (int) (255 * a));
    }

    /** Called from {@link Theme#refreshThemeColors(boolean, boolean)} after accent colors are calculated. */
    public static void apply(SparseIntArray c, boolean dark) {
        if (ApplicationLoader.applicationContext == null || !isEnabled()) {
            return;
        }
        invalidate();
        final SparseIntArray before = c.clone();
        Roles r = roles(dark);
        final int ripple = alpha(r.onSurface, .12f);

        // Top app bar (M3: surface, no colored bar)
        c.put(key_actionBarDefault, r.surface);
        c.put(key_actionBarDefaultTitle, r.onSurface);
        c.put(key_actionBarDefaultIcon, r.onSurfaceVariant);
        c.put(key_actionBarDefaultSubtitle, r.onSurfaceVariant);
        c.put(key_actionBarDefaultSelector, ripple);
        c.put(key_actionBarDefaultSearch, r.onSurface);
        c.put(key_actionBarDefaultSearchPlaceholder, r.onSurfaceVariant);
        c.put(key_actionBarActionModeDefault, r.surfaceContainer);
        c.put(key_actionBarActionModeDefaultIcon, r.onSurfaceVariant);
        c.put(key_actionBarTabActiveText, r.onPrimary);
        c.put(key_actionBarTabUnactiveText, r.onSurfaceVariant);
        c.put(key_actionBarTabLine, r.primary);
        c.put(key_actionBarTabSelector, ripple);
        c.put(key_actionBarDefaultSubmenuBackground, r.surfaceContainer);
        c.put(key_actionBarDefaultSubmenuItem, r.onSurface);
        c.put(key_actionBarDefaultSubmenuItemIcon, r.onSurfaceVariant);
        c.put(key_avatar_backgroundActionBarBlue, r.surface);

        // Surfaces & text
        c.put(key_windowBackgroundWhite, r.surface);
        c.put(key_windowBackgroundGray, r.surfaceContainer);
        c.put(key_windowBackgroundWhiteBlackText, r.onSurface);
        c.put(key_windowBackgroundWhiteGrayText, r.onSurfaceVariant);
        c.put(key_windowBackgroundWhiteGrayText2, r.onSurfaceVariant);
        c.put(key_windowBackgroundWhiteBlueText4, r.primary);
        c.put(key_windowBackgroundWhiteBlueHeader, r.primary);
        c.put(key_windowBackgroundWhiteBlueButton, r.primary);
        c.put(key_windowBackgroundWhiteValueText, r.primary);
        c.put(key_divider, r.outlineVariant);
        c.put(key_dialogBackground, r.surfaceContainerHigh);
        c.put(key_dialogTextBlack, r.onSurface);
        c.put(key_dialogButton, r.primary);

        // Chat list
        c.put(key_chats_actionBackground, r.primaryContainer);
        c.put(key_chats_actionPressedBackground, blend(r.primaryContainer, r.onPrimaryContainer, .12f));
        c.put(key_chats_actionIcon, r.onPrimaryContainer);
        c.put(key_chats_name, r.onSurface);
        c.put(key_chats_secretName, r.tertiary);
        c.put(key_chats_message, r.onSurfaceVariant);
        c.put(key_chats_message_threeLines, r.onSurfaceVariant);
        c.put(key_chats_nameMessage, r.onSurface);
        c.put(key_chats_nameMessage_threeLines, r.onSurface);
        c.put(key_chats_actionMessage, r.primary);
        c.put(key_chats_sentCheck, r.primary);
        c.put(key_chats_sentReadCheck, r.primary);
        c.put(key_chats_mentionIcon, r.onPrimary);
        c.put(key_chats_unreadCounterMuted, r.outline);
        c.put(key_chats_tabletSelectedOverlay, alpha(r.primary, .08f));
        c.put(key_listSelector, alpha(r.onSurface, .1f));
        c.put(key_windowBackgroundWhiteGrayIcon, r.onSurfaceVariant);
        c.put(key_windowBackgroundWhiteBlueText, r.primary);
        c.put(key_windowBackgroundWhiteGrayText3, r.onSurfaceVariant);
        c.put(key_windowBackgroundWhiteHintText, r.onSurfaceVariant);
        c.put(key_windowBackgroundWhiteInputField, r.outline);
        c.put(key_windowBackgroundWhiteInputFieldActivated, r.primary);
        c.put(key_dialogTextBlue, r.primary);
        c.put(key_dialogButtonSelector, alpha(r.primary, .1f));
        c.put(key_dialogTextGray, r.onSurfaceVariant);
        c.put(key_dialogIcon, r.onSurfaceVariant);
        c.put(key_dialogTextLink, r.primary);
        c.put(key_dialogLineProgress, r.primary);
        c.put(key_dialogLineProgressBackground, r.surfaceContainerHighest);
        c.put(key_dialogInputField, r.outline);
        c.put(key_dialogInputFieldActivated, r.primary);
        c.put(key_fastScrollActive, r.primary);
        c.put(key_chat_emojiPanelBackground, r.surfaceContainer);
        c.put(key_chat_messagePanelText, r.onSurface);
        c.put(key_chat_messagePanelHint, r.onSurfaceVariant);
        c.put(key_chat_messagePanelIcons, r.onSurfaceVariant);
        c.put(key_chat_serviceBackground, alpha(r.inverseSurface, .55f));
        c.put(key_chat_goDownButton, r.surfaceContainerHigh);
        c.put(key_chat_goDownButtonCounterBackground, r.primary);
        c.put(key_chat_recordedVoiceBackground, r.primary);
        c.put(key_chat_recordVoiceCancel, r.primary);
        c.put(key_chat_attachCheckBoxBackground, r.primary);
        c.put(key_undo_background, r.inverseSurface);
        c.put(key_sheet_scrollUp, alpha(r.onSurfaceVariant, .4f));
        c.put(key_sheet_other, r.onSurfaceVariant);
        c.put(key_chat_gifSaveHintBackground, alpha(r.inverseSurface, .92f));
        c.put(key_chat_gifSaveHintText, r.inverseOnSurface);
        c.put(key_chat_stickersHintPanel, r.surfaceContainerHigh);
        c.put(key_dialogTextHint, r.onSurfaceVariant);
        c.put(key_groupcreate_hintText, r.onSurfaceVariant);
        c.put(key_undo_infoColor, r.inverseOnSurface);
        c.put(key_undo_cancelColor, r.primary);
        c.put(key_profile_actionBackground, r.primaryContainer);
        c.put(key_profile_actionIcon, r.onPrimaryContainer);
        c.put(key_profile_tabSelectedLine, r.primary);
        c.put(key_profile_tabSelectedText, r.primary);
        c.put(key_profile_tabText, r.onSurfaceVariant);
        c.put(key_actionBarTabSelector, alpha(r.primary, .1f));
        c.put(key_radioBackground, r.onSurfaceVariant);
        c.put(key_checkboxCheck, r.onPrimary);
        c.put(key_checkboxSquareCheck, r.onPrimary);
        c.put(key_checkboxSquareUnchecked, r.onSurfaceVariant);
        c.put(key_switch2TrackChecked, r.primary);
        c.put(key_switchTrackBlueChecked, r.primary);
        c.put(key_switchTrackBlue, r.surfaceContainerHighest);
        c.put(key_switchTrackBlueThumbChecked, r.onPrimary);
        c.put(key_switchTrackBlueThumb, r.outline);
        c.put(key_dialogCheckboxSquareBackground, r.primary);
        c.put(key_dialogRadioBackgroundChecked, r.primary);
        c.put(key_dialogRoundCheckBox, r.primary);
        c.put(key_dialogTopBackground, r.primary);
        c.put(key_dialogButton, r.primary);
        c.put(key_dialogSearchBackground, r.surfaceContainerHighest);
        c.put(key_dialogSearchHint, r.onSurfaceVariant);
        c.put(key_dialogSearchText, r.onSurface);
        c.put(key_dialogSearchIcon, r.onSurfaceVariant);
        c.put(key_chats_menuTopBackgroundCats, r.primaryContainer);
        c.put(key_chats_menuTopBackground, r.primaryContainer);
        c.put(key_chats_menuItemCheck, r.primary);
        c.put(key_avatar_backgroundSaved, r.primary);
        c.put(key_avatar_backgroundArchived, r.tertiary);
        c.put(key_avatar_backgroundArchivedHidden, r.tertiaryContainer);
        c.put(key_chats_archivePinBackground, r.secondary);
        c.put(key_chats_archivePullDownBackground, r.surfaceContainerHigh);
        c.put(key_chats_archivePullDownBackgroundActive, r.tertiary);
        c.put(key_chat_inLoader, r.primary);
        c.put(key_chat_inFileProgress, r.primary);
        c.put(key_chat_outFileProgress, r.primary);
        c.put(key_chat_inForwardedNameText, r.primary);
        c.put(key_chat_outForwardedNameText, r.onPrimaryContainer);
        c.put(key_chat_inReplyNameText, r.primary);
        c.put(key_chat_outReplyNameText, r.onPrimaryContainer);
        c.put(key_chat_inViews, r.onSurfaceVariant);
        c.put(key_chat_outViews, alpha(r.onPrimaryContainer, .7f));
        c.put(key_chat_messageLinkOut, r.primary);
        c.put(key_chat_inAudioSeekbarFill, r.primary);
        c.put(key_chat_outAudioSeekbarFill, r.primary);
        c.put(key_chat_inVoiceSeekbarFill, r.primary);
        c.put(key_chat_outVoiceSeekbarFill, r.primary);
        c.put(key_chat_selectedBackground, alpha(r.primary, .12f));
        c.put(key_chats_date, r.onSurfaceVariant);
        c.put(key_chats_unreadCounter, r.primary);
        c.put(key_chats_unreadCounterText, r.onPrimary);
        c.put(key_chats_unreadCounterMuted, r.outline);
        c.put(key_chats_pinnedOverlay, alpha(r.secondaryContainer, dark ? .35f : .55f));
        c.put(key_chats_archiveBackground, r.tertiary);
        c.put(key_chats_menuBackground, r.surfaceContainerLow);
        c.put(key_chats_menuItemText, r.onSurface);
        c.put(key_chats_menuItemIcon, r.onSurfaceVariant);
        c.put(key_chats_menuName, r.onSurface);
        c.put(key_chats_menuPhone, r.onSurfaceVariant);
        c.put(key_chats_tabUnreadActiveBackground, r.primary);

        // Controls
        c.put(key_progressCircle, r.primary);
        c.put(key_featuredStickers_addButton, r.primary);
        c.put(key_featuredStickers_addButtonPressed, blend(r.primary, r.onPrimary, .12f));
        c.put(key_featuredStickers_buttonText, r.onPrimary);
        c.put(key_switchTrackChecked, r.primary);
        c.put(key_switchTrack, r.surfaceContainerHighest);
        c.put(key_checkboxSquareBackground, r.primary);
        c.put(key_checkbox, r.primary);
        c.put(key_radioBackgroundChecked, r.primary);

        // Attach menu, emoji panel, player, location, pickers
        c.put(key_chat_attachActiveTab, r.primary);
        c.put(key_chat_attachUnactiveTab, r.onSurfaceVariant);
        c.put(key_chat_attachIcon, r.onPrimaryContainer);
        c.put(key_chat_attachGalleryBackground, r.primaryContainer);
        c.put(key_chat_attachAudioBackground, r.tertiaryContainer);
        c.put(key_chat_attachContactBackground, r.secondaryContainer);
        c.put(key_chat_attachLocationBackground, r.tertiaryContainer);
        c.put(key_chat_attachPollBackground, r.secondaryContainer);
        c.put(key_chat_attachCheckBoxCheck, r.onPrimary);
        c.put(key_chat_emojiSearchBackground, r.surfaceContainerHighest);
        c.put(key_chat_emojiSearchIcon, r.onSurfaceVariant);
        c.put(key_chat_emojiPanelIcon, r.onSurfaceVariant);
        c.put(key_chat_emojiBottomPanelIcon, r.onSurfaceVariant);
        c.put(key_chat_emojiPanelIconSelected, r.onSecondaryContainer);
        c.put(key_chat_emojiPanelStickerPackSelector, r.secondaryContainer);
        c.put(key_chat_emojiPanelStickerPackSelectorLine, r.primary);
        c.put(key_chat_emojiPanelBackspace, r.onSurfaceVariant);
        c.put(key_chat_emojiPanelEmptyText, r.onSurfaceVariant);
        c.put(key_chat_emojiPanelTrendingTitle, r.onSurface);
        c.put(key_chat_emojiPanelStickerSetName, r.onSurfaceVariant);
        c.put(key_chat_emojiPanelStickerSetNameHighlight, r.primary);
        c.put(key_chat_emojiPanelNewTrending, r.primary);
        c.put(key_chat_recordedVoicePlayPause, r.onPrimary);
        c.put(key_chat_recordedVoiceDot, r.error);
        c.put(key_chat_recordTime, r.onSurfaceVariant);
        c.put(key_chat_TextSelectionCursor, r.primary);
        c.put(key_dialogFloatingButton, r.primaryContainer);
        c.put(key_dialogFloatingButtonPressed, blend(r.primaryContainer, r.onPrimaryContainer, .12f));
        c.put(key_returnToCallBackground, r.tertiary);
        c.put(key_returnToCallMutedBackground, r.secondary);
        c.put(key_returnToCallText, r.onPrimary);
        c.put(key_featuredStickers_addButton2, r.primary);
        c.put(key_featuredStickers_addedIcon, r.primary);
        c.put(key_featuredStickers_buttonProgress, r.onPrimary);
        c.put(key_featuredStickers_unread, r.primary);
        c.put(key_picker_enabledButton, r.primary);
        c.put(key_picker_badge, r.primary);
        c.put(key_picker_badgeText, r.onPrimary);
        c.put(key_location_sendLocationBackground, r.primaryContainer);
        c.put(key_location_sendLocationIcon, r.onPrimaryContainer);
        c.put(key_location_sendLocationText, r.primary);
        c.put(key_location_sendLiveLocationBackground, r.tertiaryContainer);
        c.put(key_location_sendLiveLocationIcon, r.onTertiaryContainer);
        c.put(key_location_sendLiveLocationText, r.tertiary);
        c.put(key_location_liveLocationProgress, r.primary);
        c.put(key_location_actionIcon, r.onSurfaceVariant);
        c.put(key_location_actionActiveIcon, r.primary);
        c.put(key_location_actionBackground, r.surfaceContainerHigh);
        c.put(key_player_actionBarTitle, r.onSurface);
        c.put(key_player_actionBarSubtitle, r.onSurfaceVariant);
        c.put(key_player_actionBarItems, r.onSurfaceVariant);
        c.put(key_player_background, r.surfaceContainerLow);
        c.put(key_player_time, r.onSurfaceVariant);
        c.put(key_player_progressBackground, r.surfaceContainerHighest);
        c.put(key_player_progress, r.primary);
        c.put(key_player_button, r.onSurface);
        c.put(key_player_buttonActive, r.primary);
        c.put(key_passport_authorizeBackground, r.primary);
        c.put(key_passport_authorizeText, r.onPrimary);
        c.put(key_profile_verifiedBackground, r.primary);
        c.put(key_profile_verifiedCheck, r.onPrimary);
        c.put(key_profile_actionPressedBackground, blend(r.primaryContainer, r.onPrimaryContainer, .12f));

        c.put(key_chat_editMediaButton, roles(true).primary);

        // Premium screens: expressive primary → tertiary gradient (Stars keep their gold)
        c.put(key_premiumGradient0, r.primary);
        c.put(key_premiumGradient1, blend(r.primary, r.tertiary, .25f));
        c.put(key_premiumGradient2, blend(r.primary, r.tertiary, .5f));
        c.put(key_premiumGradient3, blend(r.primary, r.tertiary, .75f));
        c.put(key_premiumGradient4, r.tertiary);
        c.put(key_premiumGradientBackground1, r.primaryContainer);
        c.put(key_premiumGradientBackground2, blend(r.primaryContainer, r.tertiaryContainer, .33f));
        c.put(key_premiumGradientBackground3, blend(r.primaryContainer, r.tertiaryContainer, .66f));
        c.put(key_premiumGradientBackground4, r.tertiaryContainer);
        c.put(key_premiumGradientBottomSheet1, r.primary);
        c.put(key_premiumGradientBottomSheet2, blend(r.primary, r.tertiary, .5f));
        c.put(key_premiumGradientBottomSheet3, r.tertiary);
        c.put(key_dialogGiftsBackground, r.surfaceContainer);
        c.put(key_dialogGiftsTabText, r.onSurfaceVariant);
        c.put(key_chat_unreadMessagesStartBackground, r.surfaceContainerHigh);
        c.put(key_chat_unreadMessagesStartText, r.primary);
        c.put(key_chat_unreadMessagesStartArrowIcon, r.primary);

        // Voice/video chats are always dark: use the dark roles
        Roles d = dark ? r : roles(true);
        c.put(key_voipgroup_actionBar, d.surface);
        c.put(key_voipgroup_actionBarUnscrolled, d.surface);
        c.put(key_voipgroup_listViewBackground, d.surfaceContainer);
        c.put(key_voipgroup_listViewBackgroundUnscrolled, d.surfaceContainerLow);
        c.put(key_voipgroup_dialogBackground, d.surfaceContainerHigh);
        c.put(key_voipgroup_inviteMembersBackground, d.surfaceContainer);
        c.put(key_voipgroup_actionBarItems, d.onSurface);
        c.put(key_voipgroup_nameText, d.onSurface);
        c.put(key_voipgroup_lastSeenText, d.onSurfaceVariant);
        c.put(key_voipgroup_lastSeenTextUnscrolled, d.onSurfaceVariant);
        c.put(key_voipgroup_listeningText, d.primary);
        c.put(key_voipgroup_speakingText, d.tertiary);
        c.put(key_voipgroup_mutedIcon, d.onSurfaceVariant);
        c.put(key_voipgroup_mutedIconUnscrolled, d.onSurfaceVariant);
        c.put(key_voipgroup_searchBackground, d.surfaceContainerHighest);
        c.put(key_voipgroup_searchText, d.onSurface);
        c.put(key_voipgroup_searchPlaceholder, d.onSurfaceVariant);
        c.put(key_voipgroup_soundButton, d.secondaryContainer);
        c.put(key_voipgroup_soundButton2, d.secondaryContainer);
        c.put(key_voipgroup_soundButtonActive, d.primary);
        c.put(key_voipgroup_soundButtonActive2, d.primary);
        c.put(key_voipgroup_soundButtonActiveScrolled, d.primary);
        c.put(key_voipgroup_soundButtonActive2Scrolled, d.primary);
        c.put(key_voipgroup_disabledButton, d.surfaceContainerHighest);
        c.put(key_voipgroup_connectingProgress, d.primary);
        c.put(key_voipgroup_checkMenu, d.primary);
        c.put(key_voipgroup_overlayBlue1, d.primary);
        c.put(key_voipgroup_overlayBlue2, d.primaryContainer);
        c.put(key_voipgroup_overlayGreen1, d.tertiary);
        c.put(key_voipgroup_overlayGreen2, d.tertiaryContainer);
        c.put(key_voipgroup_topPanelBlue1, d.primary);
        c.put(key_voipgroup_topPanelBlue2, d.primaryContainer);
        c.put(key_voipgroup_topPanelGreen1, d.tertiary);
        c.put(key_voipgroup_topPanelGreen2, d.tertiaryContainer);
        c.put(key_voipgroup_topPanelGray, d.surfaceContainerHighest);
        c.put(key_voipgroup_windowBackgroundWhiteInputField, d.outline);
        c.put(key_voipgroup_windowBackgroundWhiteInputFieldActivated, d.primary);

        // Navigation bar / floating toolbars (glass tabs)
        c.put(key_glass_tabSelected, r.onSecondaryContainer);
        c.put(key_glass_tabSelectedText, r.onSurface);
        c.put(key_glass_tabUnselected, r.onSurfaceVariant);
        c.put(key_glass_defaultIcon, r.onSurfaceVariant);
        c.put(key_glass_defaultText, r.onSurface);
        c.put(key_glass_targetMainTabs, r.surfaceContainer);

        // Chat
        c.put(key_chat_messagePanelBackground, r.surfaceContainer);
        c.put(key_chat_messagePanelSend, r.primary);
        c.put(key_chat_outBubble, r.primaryContainer);
        c.put(key_chat_outBubbleSelected, blend(r.primaryContainer, r.onPrimaryContainer, .1f));
        c.delete(key_chat_outBubbleGradient1);
        c.delete(key_chat_outBubbleGradient2);
        c.delete(key_chat_outBubbleGradient3);
        c.put(key_chat_messageTextOut, r.onPrimaryContainer);
        c.put(key_chat_outTimeText, alpha(r.onPrimaryContainer, .7f));
        c.put(key_chat_outSentCheck, r.primary);
        c.put(key_chat_outSentCheckRead, r.primary);
        c.put(key_chat_outReplyLine, r.primary);
        c.put(key_chat_outMenu, alpha(r.onPrimaryContainer, .6f));
        c.put(key_chat_outLoader, r.primary);
        c.put(key_chat_outLoaderSelected, r.primary);
        c.put(key_chat_outMediaIcon, r.primaryContainer);
        c.put(key_chat_inBubble, r.surfaceContainerHigh);
        c.put(key_chat_inBubbleSelected, r.surfaceContainerHighest);
        c.put(key_chat_messageTextIn, r.onSurface);
        c.put(key_chat_inTimeText, r.onSurfaceVariant);
        c.put(key_chat_inReplyLine, r.primary);
        c.put(key_chat_messageLinkIn, r.primary);
        c.put(key_chat_inMenu, r.onSurfaceVariant);

        // Messages (M3E containers): media/file/audio/reply/preview inside bubbles
        final int onOut = r.onPrimaryContainer, outVar = alpha(r.onPrimaryContainer, .72f);
        c.put(key_chat_inBubbleShadow, 0);
        c.put(key_chat_outBubbleShadow, 0);
        c.put(key_chat_inMediaIcon, r.surfaceContainerHigh);
        c.put(key_chat_inMediaIconSelected, r.surfaceContainerHighest);
        c.put(key_chat_inLoaderSelected, r.primary);
        c.put(key_chat_inFileBackground, r.primaryContainer);
        c.put(key_chat_inFileBackgroundSelected, r.primaryContainer);
        c.put(key_chat_outFileBackground, r.primary);
        c.put(key_chat_outFileBackgroundSelected, r.primary);
        c.put(key_chat_inFileNameText, r.onSurface);
        c.put(key_chat_outFileNameText, onOut);
        c.put(key_chat_inFileInfoText, r.onSurfaceVariant);
        c.put(key_chat_outFileInfoText, outVar);
        c.put(key_chat_inAudioTitleText, r.onSurface);
        c.put(key_chat_outAudioTitleText, onOut);
        c.put(key_chat_inAudioPerformerText, r.onSurfaceVariant);
        c.put(key_chat_outAudioPerformerText, outVar);
        c.put(key_chat_inAudioPerformerSelectedText, r.onSurfaceVariant);
        c.put(key_chat_outAudioPerformerSelectedText, outVar);
        c.put(key_chat_inAudioDurationText, r.onSurfaceVariant);
        c.put(key_chat_outAudioDurationText, outVar);
        c.put(key_chat_inAudioDurationSelectedText, r.onSurfaceVariant);
        c.put(key_chat_outAudioDurationSelectedText, outVar);
        c.put(key_chat_inAudioProgress, r.surfaceContainerHigh);
        c.put(key_chat_outAudioProgress, r.primaryContainer);
        c.put(key_chat_inAudioSelectedProgress, r.surfaceContainerHighest);
        c.put(key_chat_outAudioSelectedProgress, r.primaryContainer);
        c.put(key_chat_inAudioSeekbar, alpha(r.primary, .24f));
        c.put(key_chat_outAudioSeekbar, alpha(r.primary, .24f));
        c.put(key_chat_inAudioCacheSeekbar, alpha(r.primary, .36f));
        c.put(key_chat_outAudioCacheSeekbar, alpha(r.primary, .36f));
        c.put(key_chat_inAudioSeekbarSelected, alpha(r.primary, .3f));
        c.put(key_chat_outAudioSeekbarSelected, alpha(r.primary, .3f));
        c.put(key_chat_inVoiceSeekbar, alpha(r.primary, .3f));
        c.put(key_chat_outVoiceSeekbar, alpha(r.primary, .3f));
        c.put(key_chat_inVoiceSeekbarSelected, alpha(r.primary, .4f));
        c.put(key_chat_outVoiceSeekbarSelected, alpha(r.primary, .4f));
        c.put(key_chat_inReplyLine, r.primary);
        c.put(key_chat_outReplyLine, r.primary);
        c.put(key_chat_outReplyLine2, r.tertiary);
        c.put(key_chat_inReplyMessageText, r.onSurfaceVariant);
        c.put(key_chat_outReplyMessageText, outVar);
        c.put(key_chat_inReplyMediaMessageText, r.onSurfaceVariant);
        c.put(key_chat_outReplyMediaMessageText, outVar);
        c.put(key_chat_inReplyMediaMessageSelectedText, r.onSurfaceVariant);
        c.put(key_chat_outReplyMediaMessageSelectedText, outVar);
        c.put(key_chat_inPreviewLine, r.primary);
        c.put(key_chat_outPreviewLine, r.primary);
        c.put(key_chat_inSiteNameText, r.primary);
        c.put(key_chat_outSiteNameText, r.primary);
        c.put(key_chat_inContactNameText, r.primary);
        c.put(key_chat_outContactNameText, r.primary);
        c.put(key_chat_inInstant, r.primary);
        c.put(key_chat_outInstant, r.primary);
        c.put(key_chat_inInstantSelected, r.primary);
        c.put(key_chat_outInstantSelected, r.primary);
        c.put(key_chat_inPreviewInstantText, r.primary);
        c.put(key_chat_outPreviewInstantText, r.primary);
        c.put(key_chat_inTimeSelectedText, r.onSurfaceVariant);
        c.put(key_chat_outTimeSelectedText, outVar);
        c.put(key_chat_inVenueInfoText, r.onSurfaceVariant);
        c.put(key_chat_outVenueInfoText, outVar);
        c.put(key_chat_inPollCorrectAnswer, r.tertiary);
        c.put(key_chat_outPollCorrectAnswer, r.tertiary);
        c.put(key_chat_outPollWrongAnswer, r.error);

        // Stories: unread ring = primary → tertiary (expressive gradient), close friends stay green
        c.put(key_stories_circle1, r.primary);
        c.put(key_stories_circle2, r.tertiary);
        c.put(key_stories_circle_dialog1, r.primary);
        c.put(key_stories_circle_dialog2, r.tertiary);

        // Pinned message bar & reply/edit panel
        c.put(key_chat_topPanelBackground, r.surfaceContainer);
        c.put(key_chat_topPanelLine, r.primary);
        c.put(key_chat_topPanelTitle, r.primary);
        c.put(key_chat_topPanelMessage, r.onSurfaceVariant);
        c.put(key_chat_topPanelClose, r.onSurfaceVariant);
        c.put(key_chat_replyPanelIcons, r.primary);
        c.put(key_chat_replyPanelClose, r.onSurfaceVariant);
        c.put(key_chat_replyPanelName, r.primary);
        c.put(key_chat_replyPanelLine, r.outlineVariant);

        // Mini audio player (top panel while music plays)
        c.put(key_inappPlayerBackground, r.surfaceContainer);
        c.put(key_inappPlayerPlayPause, r.primary);
        c.put(key_inappPlayerTitle, r.onSurface);
        c.put(key_inappPlayerPerformer, r.onSurfaceVariant);
        c.put(key_inappPlayerClose, r.onSurfaceVariant);

        retintRemainingAccents(c, before, r, dark);
    }

    /**
     * Maps a Telegram corner radius (dp) onto the M3 Expressive shape scale:
     * extraSmall 4 → small 8 → medium 12 → large 16 → largeIncreased 20 → extraLarge 28 → extraLargeIncreased 32.
     * Radii ≤ 2dp (hairlines) and ≥ 32dp (pills/circles) are kept. Only when expressive shapes are on.
     */
    public static float m3CornerDp(float dp) {
        if (dp <= 2f || dp >= 32f || !expressiveShapes()) return dp;
        if (dp <= 5f) return 8f;
        if (dp <= 9f) return 12f;
        if (dp <= 13f) return 16f;
        if (dp <= 17f) return 20f;
        if (dp <= 24f) return 28f;
        return 32f;
    }

    public static int m3CornerPx(int px) {
        if (px <= 0) return px;
        final float d = org.telegram.messenger.AndroidUtilities.density;
        return Math.round(m3CornerDp(px / d) * d);
    }

    public static float[] m3CornersPx(float[] radii) {
        if (radii == null || !expressiveShapes()) return radii;
        final float[] out = new float[radii.length];
        for (int k = 0; k < radii.length; k++) out[k] = m3CornerPx(Math.round(radii[k]));
        return out;
    }

    /** Float px variant for Canvas.drawRoundRect radii (see {@link #m3CornerDp}). */
    public static float m3CornerPxF(float px) {
        if (px <= 0 || !expressiveShapes()) return px;
        final float d = org.telegram.messenger.AndroidUtilities.density;
        return m3CornerDp(px / d) * d;
    }
}

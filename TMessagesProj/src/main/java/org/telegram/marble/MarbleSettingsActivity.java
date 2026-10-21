/*
 * Marblegram — settings screen for the Material 3 Expressive redesign.
 */

package org.telegram.marble;

import android.content.Context;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;

import java.util.ArrayList;

import static org.telegram.messenger.LocaleController.getString;

public class MarbleSettingsActivity extends BaseFragment {

    private static final int ID_ENABLED = 1;
    private static final int ID_DYNAMIC = 2;
    private static final int ID_TYPOGRAPHY = 3;
    private static final int ID_SHAPES = 4;
    private static final int ID_CONTAINED = 5;
    private static final int ID_ROUND_FRONT = 6;
    private static final int ID_ROUND_BACK = 7;
    private static final int ID_FAB_MENU = 8;
    private static final int ID_WAVY = 9;
    private static final int ID_SEGMENTED = 10;
    private static final int ID_FOLDER_CHIPS = 11;

    private static final int ID_PAGE_COLORS = 101;
    private static final int ID_PAGE_SHAPES = 102;
    private static final int ID_PAGE_TYPOGRAPHY = 103;
    private static final int ID_PAGE_COMPONENTS = 104;
    private static final int ID_PAGE_ROUND = 105;
    private static final int ID_PAGE_APP_ICON = 106;
    private static final int ID_PAGE_PLUGINS = 107;
    private static final int ID_PAGE_FEATURES = 108;

    public static final int PAGE_MAIN = 0, PAGE_COLORS = 1, PAGE_SHAPES = 2, PAGE_TYPOGRAPHY = 3, PAGE_COMPONENTS = 4, PAGE_ROUND = 5;

    private final int page;
    private UniversalRecyclerView listView;
    private MarbleLargeTopBar topBar;

    public MarbleSettingsActivity() {
        this(PAGE_MAIN);
    }

    public MarbleSettingsActivity(int page) {
        this.page = page;
    }

    private int pageTitle() {
        switch (page) {
            case PAGE_COLORS: return R.string.MarbleColors;
            case PAGE_SHAPES: return R.string.MarbleShapesPage;
            case PAGE_TYPOGRAPHY: return R.string.MarbleTypographyPage;
            case PAGE_COMPONENTS: return R.string.MarbleComponents;
            case PAGE_ROUND: return R.string.MarbleRoundVideos;
            default: return R.string.MarbleSettings;
        }
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(getString(pageTitle()));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        FrameLayout contentView = new FrameLayout(context);
        topBar = new MarbleLargeTopBar(context, getString(pageTitle()), null, resourceProvider);
        listView = new UniversalRecyclerView(this, this::fillItems, this::onItemClick, null);
        listView.setSections();
        listView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray, resourceProvider));
        contentView.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.FILL));
        actionBar.setAdaptiveBackground(listView);
        topBar.attach(actionBar, listView);
        return fragmentView = contentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (listView != null) {
            listView.adapter.update(false);
        }
    }

    private void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        if (topBar != null) items.add(UItem.asCustomShadow(topBar));
        switch (page) {
            case PAGE_COLORS: fillColors(items); break;
            case PAGE_SHAPES: fillShapes(items); break;
            case PAGE_TYPOGRAPHY: fillTypography(items); break;
            case PAGE_COMPONENTS: fillComponents(items); break;
            case PAGE_ROUND: fillRound(items); break;
            default: fillMain(items); break;
        }
    }

    private void fillMain(ArrayList<UItem> items) {
        final boolean enabled = MarbleTheme.isEnabled();
        items.add(UItem.asCustom(new MarblePreviewView(getContext()), 172));
        items.add(UItem.asCheck(ID_ENABLED, getString(R.string.MarbleEnable)).setChecked(enabled));
        items.add(UItem.asShadow(getString(R.string.MarbleEnableInfo)));
        if (!enabled) {
            return;
        }
        items.add(UItem.asButton(ID_PAGE_COLORS, R.drawable.msg_theme, getString(R.string.MarbleColors)));
        items.add(UItem.asButton(ID_PAGE_SHAPES, R.drawable.msg_customize, getString(R.string.MarbleShapesPage)));
        items.add(UItem.asButton(ID_PAGE_TYPOGRAPHY, R.drawable.msg_text_outlined, getString(R.string.MarbleTypographyPage)));
        items.add(UItem.asButton(ID_PAGE_COMPONENTS, R.drawable.msg_settings, getString(R.string.MarbleComponents)));
        items.add(UItem.asShadow(null));
        items.add(UItem.asButton(ID_PAGE_ROUND, R.drawable.msg_video, getString(R.string.MarbleRoundVideos)));
        items.add(UItem.asButton(ID_PAGE_APP_ICON, R.drawable.msg_photos, getString(R.string.MarbleAppIcon)));
        items.add(UItem.asButton(ID_PAGE_FEATURES, R.drawable.msg_secret, getString(R.string.MarbleFeatures)));
        items.add(UItem.asButton(ID_PAGE_PLUGINS, R.drawable.msg_customize, getString(R.string.MarblePluginsSettings)));
        items.add(UItem.asShadow(getString(R.string.MarbleAppIconInfo)));
    }

    private void fillColors(ArrayList<UItem> items) {
        if (Build.VERSION.SDK_INT >= 31) {
            items.add(UItem.asCheck(ID_DYNAMIC, getString(R.string.MarbleDynamicColor)).setChecked(MarbleTheme.useDynamicColor()));
        }
        if (!MarbleTheme.useDynamicColor()) {
            items.add(UItem.asHeader(getString(R.string.MarbleSeedHeader)));
            items.add(UItem.asSlideView(new String[]{
                getString(R.string.MarbleSeedBlue), getString(R.string.MarbleSeedGreen), getString(R.string.MarbleSeedViolet), getString(R.string.MarbleSeedRed),
                getString(R.string.MarbleSeedOrange), getString(R.string.MarbleSeedTeal), getString(R.string.MarbleSeedRose), getString(R.string.MarbleSeedGray)
            }, MarbleTheme.getSeedIndex(), index -> {
                MarbleTheme.setSeedIndex(index);
                refreshAll();
            }));
        }
        items.add(UItem.asShadow(getString(Build.VERSION.SDK_INT >= 31 ? R.string.MarbleDynamicColorInfo : R.string.MarbleSeedInfo)));
    }

    private void fillShapes(ArrayList<UItem> items) {
        items.add(UItem.asCheck(ID_SHAPES, getString(R.string.MarbleShapes)).setChecked(MarbleTheme.expressiveShapes()));
        items.add(UItem.asShadow(getString(R.string.MarbleComponentsInfo)));
        items.add(UItem.asHeader(getString(R.string.MarbleAvatarShape)));
        items.add(UItem.asSlideView(new String[]{
            getString(R.string.MarbleShapeCircle), getString(R.string.MarbleShapeCookie), getString(R.string.MarbleShapeSunny),
            getString(R.string.MarbleShapePentagon), getString(R.string.MarbleShapeBurst), getString(R.string.MarbleShapeFlower)
        }, MarbleTheme.avatarShape(), index -> {
            MarbleTheme.setAvatarShape(index);
            refreshAll();
        }));
        items.add(UItem.asShadow(getString(R.string.MarbleAvatarShapeInfo)));
    }

    private void fillTypography(ArrayList<UItem> items) {
        items.add(UItem.asCheck(ID_TYPOGRAPHY, getString(R.string.MarbleTypography)).setChecked(MarbleTheme.expressiveTypography()));
        items.add(UItem.asShadow(getString(R.string.MarbleTypographyInfo)));
    }

    private void fillComponents(ArrayList<UItem> items) {
        items.add(UItem.asHeader(getString(R.string.MarbleProgressHeader)));
        items.add(UItem.asCheck(ID_CONTAINED, getString(R.string.MarbleContainedLoading)).setChecked(MarbleTheme.containedLoading()));
        items.add(UItem.asCheck(ID_WAVY, getString(R.string.MarbleWavy)).setChecked(MarbleTheme.rawFlag(MarbleTheme.WAVY, true)));
        items.add(UItem.asShadow(getString(R.string.MarbleWavyInfo)));
        items.add(UItem.asHeader(getString(R.string.MarbleNavigationHeader)));
        items.add(UItem.asCheck(ID_FAB_MENU, getString(R.string.MarbleFabMenu)).setChecked(MarbleTheme.rawFlag(MarbleTheme.FAB_MENU, true)));
        items.add(UItem.asCheck(ID_FOLDER_CHIPS, getString(R.string.MarbleFolderChips)).setChecked(MarbleTheme.rawFlag(MarbleTheme.FOLDER_CHIPS, true)));
        items.add(UItem.asCheck(ID_SEGMENTED, getString(R.string.MarbleSegmented)).setChecked(MarbleTheme.rawFlag(MarbleTheme.SEGMENTED, true)));
        items.add(UItem.asShadow(null));
        items.add(UItem.asHeader(getString(R.string.MarbleBubbleRadius)));
        items.add(UItem.asIntSlideView(
            1, 4, MarbleTheme.bubbleRadius(), 28,
            val -> val + " dp",
            val -> MarbleTheme.setBubbleRadius(val)
        ));
        items.add(UItem.asShadow(getString(R.string.MarbleBubbleRadiusInfo)));
    }

    private void fillRound(ArrayList<UItem> items) {
        items.add(UItem.asHeader(getString(R.string.MarbleRoundCamera)));
        items.add(UItem.asRadio(ID_ROUND_FRONT, getString(R.string.MarbleRoundCameraFront)).setChecked(MarbleTheme.roundFrontCamera()));
        items.add(UItem.asRadio(ID_ROUND_BACK, getString(R.string.MarbleRoundCameraBack)).setChecked(!MarbleTheme.roundFrontCamera()));
        items.add(UItem.asShadow(null));
    }

    private void onItemClick(UItem item, View view, int position, float x, float y) {
        boolean value;
        switch (item.id) {
            case ID_PAGE_COLORS: presentFragment(new MarbleSettingsActivity(PAGE_COLORS)); return;
            case ID_PAGE_SHAPES: presentFragment(new MarbleSettingsActivity(PAGE_SHAPES)); return;
            case ID_PAGE_TYPOGRAPHY: presentFragment(new MarbleSettingsActivity(PAGE_TYPOGRAPHY)); return;
            case ID_PAGE_COMPONENTS: presentFragment(new MarbleSettingsActivity(PAGE_COMPONENTS)); return;
            case ID_PAGE_ROUND: presentFragment(new MarbleSettingsActivity(PAGE_ROUND)); return;
            case ID_PAGE_FEATURES: presentFragment(new org.telegram.marble.features.MarbleFeaturesSettingsActivity()); return;
            case ID_PAGE_PLUGINS: presentFragment(new org.telegram.marble.plugins.PluginsActivity()); return;
            case ID_PAGE_APP_ICON: presentFragment(new org.telegram.ui.ThemeActivity(org.telegram.ui.ThemeActivity.THEME_TYPE_BASIC)); return;
            case ID_ROUND_FRONT:
            case ID_ROUND_BACK:
                MarbleTheme.setRoundFrontCamera(item.id == ID_ROUND_FRONT);
                listView.adapter.update(true);
                return;
            case ID_ENABLED:
                MarbleTheme.setEnabled(value = !MarbleTheme.isEnabled());
                org.telegram.messenger.AndroidUtilities.mediumTypeface = null;
                break;
            case ID_DYNAMIC:
                MarbleTheme.setDynamic(value = !MarbleTheme.useDynamicColor());
                break;
            case ID_TYPOGRAPHY:
                MarbleTheme.setExpressiveTypography(value = !MarbleTheme.expressiveTypography());
                BulletinFactory.of(this).createSimpleBulletin(R.raw.info, getString(R.string.MarbleRestartHint)).show();
                break;
            case ID_SHAPES:
                MarbleTheme.setExpressiveShapes(value = !MarbleTheme.expressiveShapes());
                break;
            case ID_FAB_MENU:
                MarbleTheme.setFlag(MarbleTheme.FAB_MENU, value = !MarbleTheme.rawFlag(MarbleTheme.FAB_MENU, true));
                break;
            case ID_WAVY:
                MarbleTheme.setFlag(MarbleTheme.WAVY, value = !MarbleTheme.rawFlag(MarbleTheme.WAVY, true));
                break;
            case ID_SEGMENTED:
                MarbleTheme.setFlag(MarbleTheme.SEGMENTED, value = !MarbleTheme.rawFlag(MarbleTheme.SEGMENTED, true));
                break;
            case ID_FOLDER_CHIPS:
                MarbleTheme.setFlag(MarbleTheme.FOLDER_CHIPS, value = !MarbleTheme.rawFlag(MarbleTheme.FOLDER_CHIPS, true));
                break;
            case ID_CONTAINED:
                MarbleTheme.setContainedLoading(value = !MarbleTheme.containedLoading());
                break;
            default:
                return;
        }
        if (view instanceof TextCheckCell) {
            ((TextCheckCell) view).setChecked(value);
        }
        refreshAll();
        listView.adapter.update(true);
    }

    private void refreshAll() {
        Theme.refreshThemeColors();
        if (parentLayout != null) {
            parentLayout.rebuildAllFragmentViews(false, false);
        }
        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme, false, true);
    }

    /** Header preview: contained loading indicator + avatar shape + FAB on the current palette. */
    private static class MarblePreviewView extends View {
        private final LoadingIndicator indicator = new LoadingIndicator();
        private final android.graphics.Paint paint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        private final android.graphics.Path path = new android.graphics.Path();
        private final android.text.TextPaint textPaint = new android.text.TextPaint(android.graphics.Paint.ANTI_ALIAS_FLAG);

        MarblePreviewView(Context context) {
            super(context);
        }

        @Override
        protected void onDraw(android.graphics.Canvas canvas) {
            final float d = org.telegram.messenger.AndroidUtilities.density;
            MarbleTheme.Roles r = MarbleTheme.roles(Theme.isCurrentThemeDark());
            float w = getWidth(), h = getHeight();
            canvas.drawColor(r.surfaceContainer);

            // avatar shape
            float[] shape = MarbleTheme.avatarShapeRadii();
            paint.setColor(r.tertiaryContainer);
            MaterialShapes.morph(path, shape != null ? shape : MaterialShapes.CIRCLE, shape != null ? shape : MaterialShapes.CIRCLE, 0, w * 0.22f, h * 0.45f, 36 * d, 0);
            canvas.drawPath(path, paint);

            // loading indicator
            indicator.setContainerColor(MarbleTheme.containedLoading() ? r.primaryContainer : 0);
            indicator.setColor(MarbleTheme.containedLoading() ? r.onPrimaryContainer : r.primary);
            indicator.draw(canvas, w * 0.5f, h * 0.45f, 64 * d, this);

            // FAB (M3E: 16dp corners, primaryContainer)
            paint.setColor(r.primaryContainer);
            float fs = 56 * d, fx = w * 0.78f, fy = h * 0.45f;
            float corner = MarbleTheme.expressiveShapes() ? 16 * d : fs / 2;
            canvas.drawRoundRect(fx - fs / 2, fy - fs / 2, fx + fs / 2, fy + fs / 2, corner, corner, paint);
            paint.setColor(r.onPrimaryContainer);
            canvas.drawCircle(fx, fy, 6 * d, paint);

            textPaint.setColor(r.onSurfaceVariant);
            textPaint.setTextSize(12 * d);
            textPaint.setTypeface(MarbleTypography.label());
            String t = "Marblegram · Material 3 Expressive";
            canvas.drawText(t, (w - textPaint.measureText(t)) / 2, h - 16 * d, textPaint);
        }
    }
}

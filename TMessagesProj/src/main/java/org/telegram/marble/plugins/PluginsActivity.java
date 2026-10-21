/*
 * Marblegram Plugin SDK — Settings → Marblegram → Plugins.
 */

package org.telegram.marble.plugins;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.marble.MarbleTheme;
import org.telegram.marble.MarbleTypography;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RLottieDrawable;
import org.telegram.ui.Components.RLottieImageView;
import org.telegram.ui.Components.Switch;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;

import java.io.File;
import java.util.ArrayList;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.LocaleController.getString;

public class PluginsActivity extends BaseFragment {

    private static final int REQUEST_INSTALL = 7301;
    private static final int MENU_INSTALL = 1;

    private static final int ID_ENABLED = 1;
    private static final int ID_RELOAD = 2;
    private static final int ID_DELETE = 3;
    private static final int ID_ACTION_BASE = 1000;

    /** null = list of plugins; otherwise the detail page of this plugin. */
    private final String pluginId;
    private UniversalRecyclerView listView;
    private org.telegram.marble.MarbleLargeTopBar topBar;

    public PluginsActivity() {
        this(null);
    }

    public PluginsActivity(String pluginId) {
        this.pluginId = pluginId;
    }

    @Override
    public View createView(Context context) {
        PluginManager.getInstance().init();
        Plugin plugin = pluginId == null ? null : PluginManager.getInstance().find(pluginId);

        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(plugin != null ? plugin.manifest.name : getString(R.string.MarblePlugins));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == MENU_INSTALL) {
                    pickPluginFile();
                }
            }
        });
        if (pluginId == null) {
            ActionBarMenu menu = actionBar.createMenu();
            menu.addItem(MENU_INSTALL, R.drawable.msg_add);
        }

        FrameLayout contentView = new FrameLayout(context);
        topBar = new org.telegram.marble.MarbleLargeTopBar(context, plugin != null ? plugin.manifest.name : getString(R.string.MarblePlugins), null, resourceProvider);
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
        if (pluginId == null) {
            fillList(items);
        } else {
            fillDetail(items);
        }
    }

    private void fillList(ArrayList<UItem> items) {
        ArrayList<Plugin> plugins = PluginManager.getInstance().getPlugins();
        if (plugins.isEmpty()) {
            items.add(UItem.asShadow(getString(R.string.MarblePluginsEmpty)));
        } else {
            items.add(UItem.asHeader(getString(R.string.MarblePluginsInstalled)));
            for (Plugin p : plugins) {
                items.add(UItem.asCustom(new PluginCell(getContext(), p, true), 76));
            }
            items.add(UItem.asShadow(null));
        }
        items.add(UItem.asButton(ID_ACTION_BASE - 1, R.drawable.msg_add, getString(R.string.MarblePluginsInstall)));
        items.add(UItem.asShadow(getString(R.string.MarblePluginsInfo)));
    }

    private void fillDetail(ArrayList<UItem> items) {
        Plugin p = PluginManager.getInstance().find(pluginId);
        if (p == null) {
            return;
        }
        items.add(UItem.asCustom(new PluginCell(getContext(), p, false), 76));
        if (!TextUtils.isEmpty(p.manifest.description)) {
            items.add(UItem.asShadow(p.manifest.description));
        } else {
            items.add(UItem.asShadow(null));
        }
        items.add(UItem.asCheck(ID_ENABLED, getString(R.string.MarblePluginEnabled)).setChecked(p.enabled));
        // Marblegram: list the permissions the plugin declared in plugin.json.
        items.add(UItem.asShadow(getString(R.string.MarblePluginPermissions) + ": " + (p.manifest.permissions.isEmpty()
            ? getString(R.string.MarblePluginNoPermissions) : TextUtils.join(", ", p.manifest.permissions))));
        if (p.error != null) {
            items.add(UItem.asShadow(getString(R.string.MarblePluginError) + ": " + p.error));
        } else {
            items.add(UItem.asShadow(null));
        }
        if (p.api != null && !p.api.actions.isEmpty() && p.isRunning()) {
            items.add(UItem.asHeader(getString(R.string.MarblePluginActions)));
            for (int i = 0; i < p.api.actions.size(); i++) {
                items.add(UItem.asButton(ID_ACTION_BASE + i, R.drawable.msg_instant, p.api.actions.get(i).title));
            }
            items.add(UItem.asShadow(null));
        }
        items.add(UItem.asButton(ID_RELOAD, R.drawable.msg_retry, getString(R.string.MarblePluginReload)));
        items.add(UItem.asButton(ID_DELETE, R.drawable.msg_delete, getString(R.string.MarblePluginDelete)).red());
        items.add(UItem.asShadow(String.format("%s · v%s · %s · SDK %d",
            p.manifest.id, p.manifest.version, p.manifest.language, p.manifest.minSdk)));
    }

    private void onItemClick(UItem item, View view, int position, float x, float y) {
        if (pluginId == null) {
            if (view instanceof PluginCell) {
                presentFragment(new PluginsActivity(((PluginCell) view).plugin.manifest.id));
            } else if (item.id == ID_ACTION_BASE - 1) {
                pickPluginFile();
            }
            return;
        }
        Plugin p = PluginManager.getInstance().find(pluginId);
        if (p == null) {
            return;
        }
        if (item.id == ID_ENABLED) {
            PluginManager.getInstance().setEnabled(p, !p.enabled);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(p.enabled);
            }
            listView.adapter.update(true);
        } else if (item.id == ID_RELOAD) {
            PluginManager.getInstance().reload(p);
            listView.adapter.update(true);
            BulletinFactory.of(this).createSimpleBulletin(R.raw.info, getString(R.string.MarblePluginReloaded)).show();
        } else if (item.id == ID_DELETE) {
            new AlertDialog.Builder(getContext(), resourceProvider)
                .setTitle(getString(R.string.MarblePluginDelete))
                .setMessage(p.manifest.name)
                .setPositiveButton(getString(R.string.Delete), (d, w) -> {
                    PluginManager.getInstance().uninstall(p);
                    finishFragment();
                })
                .setNegativeButton(getString(R.string.Cancel), null)
                .show();
        } else if (item.id >= ID_ACTION_BASE && p.api != null) {
            int idx = item.id - ID_ACTION_BASE;
            if (idx < p.api.actions.size()) {
                PluginManager.getInstance().callAction(p, p.api.actions.get(idx).function);
                listView.adapter.update(true);
            }
        }
    }

    private void pickPluginFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, REQUEST_INSTALL);
    }

    @Override
    public void onActivityResultFragment(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_INSTALL || resultCode != Activity.RESULT_OK || data == null || data.getData() == null) {
            return;
        }
        final android.net.Uri uri = data.getData();
        if (getParentActivity() == null) return;
        // SDK 2 plugins get full access to the account: always confirm installs from files
        showDialog(new org.telegram.ui.ActionBar.AlertDialog.Builder(getParentActivity())
            .setTitle(getString(R.string.MarblePluginFullAccessTitle))
            .setMessage(getString(R.string.MarblePluginFullAccessText))
            .setPositiveButton(getString(R.string.MarblePluginInstall), (d, w) -> installConfirmed(uri))
            .setNegativeButton(getString(R.string.Cancel), null)
            .create());
    }

    private void installConfirmed(android.net.Uri uri) {
        try {
            Plugin p = PluginManager.getInstance().install(uri);
            listView.adapter.update(true);
            BulletinFactory.of(this).createSimpleBulletin(R.raw.contact_check, String.format(getString(R.string.MarblePluginInstalled), p.manifest.name)).show();
        } catch (Exception e) {
            BulletinFactory.of(this).createErrorBulletin(getString(R.string.MarblePluginInstallError) + ": " + e.getMessage()).show();
        }
    }

    /** Plugin row: animated (Lottie) or static avatar in an M3E shape, name, author/version, switch. */
    static class PluginCell extends LinearLayout {

        final Plugin plugin;

        PluginCell(Context context, Plugin plugin, boolean withSwitch) {
            super(context);
            this.plugin = plugin;
            setOrientation(HORIZONTAL);
            setGravity(Gravity.CENTER_VERTICAL);
            setPadding(dp(16), 0, dp(16), 0);
            final MarbleTheme.Roles r = MarbleTheme.roles(Theme.isCurrentThemeDark());

            FrameLayout avatar = new FrameLayout(context);
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.OVAL);
            bg.setColor(r.tertiaryContainer);
            avatar.setBackground(bg);
            avatar.setClipToOutline(true);
            File anim = plugin.file(plugin.manifest.animation);
            File icon = plugin.file(plugin.manifest.icon);
            if (anim != null && anim.exists()) {
                RLottieImageView lottie = new RLottieImageView(context);
                RLottieDrawable drawable = new RLottieDrawable(anim, null, dp(48), dp(48), null, false, null, 0, false);
                drawable.setAutoRepeat(1);
                lottie.setAnimation(drawable);
                lottie.playAnimation();
                avatar.addView(lottie, LayoutHelper.createFrame(48, 48, Gravity.CENTER));
            } else if (icon != null && icon.exists()) {
                BackupImageView image = new BackupImageView(context);
                image.setRoundRadius(dp(24));
                image.setImage(ImageLocation.getForPath(icon.getAbsolutePath()), "48_48", null, null, null);
                avatar.addView(image, LayoutHelper.createFrame(48, 48, Gravity.CENTER));
            } else {
                TextView letter = new TextView(context);
                letter.setText(plugin.manifest.name.isEmpty() ? "?" : plugin.manifest.name.substring(0, 1).toUpperCase());
                letter.setTextColor(r.onTertiaryContainer);
                letter.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
                letter.setTypeface(MarbleTypography.emphasized());
                letter.setGravity(Gravity.CENTER);
                avatar.addView(letter, LayoutHelper.createFrame(48, 48, Gravity.CENTER));
            }
            addView(avatar, LayoutHelper.createLinear(48, 48));

            LinearLayout texts = new LinearLayout(context);
            texts.setOrientation(VERTICAL);
            TextView name = new TextView(context);
            name.setText(plugin.manifest.name);
            name.setSingleLine();
            name.setEllipsize(TextUtils.TruncateAt.END);
            name.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            name.setTypeface(MarbleTypography.label());
            texts.addView(name);
            TextView info = new TextView(context);
            String lang = PluginManifest.LANG_PYTHON.equals(plugin.manifest.language) ? "Python" : "Lua";
            String sub = (TextUtils.isEmpty(plugin.manifest.author) ? "" : plugin.manifest.author + " · ") + "v" + plugin.manifest.version + " · " + lang;
            if (plugin.error != null) {
                sub = getString(R.string.MarblePluginError) + ": " + plugin.error;
            }
            info.setText(sub);
            info.setSingleLine();
            info.setEllipsize(TextUtils.TruncateAt.END);
            info.setTextColor(plugin.error != null ? r.error : Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
            info.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            texts.addView(info);
            addView(texts, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, 16, 0, 8, 0));

            if (withSwitch) {
                Switch toggle = new Switch(context);
                toggle.setColors(Theme.key_switchTrack, Theme.key_switchTrackChecked, Theme.key_windowBackgroundWhite, Theme.key_windowBackgroundWhite);
                toggle.setChecked(plugin.enabled, false);
                toggle.setOnClickListener(v -> {
                    PluginManager.getInstance().setEnabled(plugin, !plugin.enabled);
                    toggle.setChecked(plugin.enabled, true);
                    info.setText(plugin.error != null ? getString(R.string.MarblePluginError) + ": " + plugin.error : info.getText());
                });
                addView(toggle, LayoutHelper.createLinear(44, 40));
            }
        }
    }
}

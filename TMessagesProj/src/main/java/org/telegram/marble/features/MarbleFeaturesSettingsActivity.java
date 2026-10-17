/*
 * Marblegram — Settings → Marblegram → Privacy & tools:
 * ghost mode, deleted/edited message history, translation, AI replies.
 */

package org.telegram.marble.features;

import android.content.Context;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import org.telegram.marble.MarbleLargeTopBar;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;

import java.util.ArrayList;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.LocaleController.getString;

public class MarbleFeaturesSettingsActivity extends BaseFragment {

    private static final int ID_GHOST_READ = 1;
    private static final int ID_GHOST_TYPING = 2;
    private static final int ID_GHOST_OFFLINE = 3;
    private static final int ID_SAVE_DELETED = 4;
    private static final int ID_SAVE_EDITED = 5;
    private static final int ID_DELETED_LIST = 6;
    private static final int ID_AI_KEY = 7;
    private static final int ID_AI_STYLE = 8;

    private UniversalRecyclerView listView;
    private MarbleLargeTopBar topBar;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(getString(R.string.MarbleFeatures));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) finishFragment();
            }
        });
        FrameLayout contentView = new FrameLayout(context);
        topBar = new MarbleLargeTopBar(context, getString(R.string.MarbleFeatures), null, resourceProvider);
        listView = new UniversalRecyclerView(this, this::fillItems, this::onItemClick, null);
        listView.setSections();
        listView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray, resourceProvider));
        contentView.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.FILL));
        actionBar.setAdaptiveBackground(listView);
        topBar.attach(actionBar, listView);
        return fragmentView = contentView;
    }

    private void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        if (topBar != null) items.add(UItem.asCustomShadow(topBar));
        items.add(UItem.asHeader(getString(R.string.MarbleGhostMode)));
        items.add(UItem.asCheck(ID_GHOST_READ, getString(R.string.MarbleGhostNoRead)).setChecked(MarbleGhost.get(MarbleGhost.NO_READ)));
        items.add(UItem.asCheck(ID_GHOST_TYPING, getString(R.string.MarbleGhostNoTyping)).setChecked(MarbleGhost.get(MarbleGhost.NO_TYPING)));
        items.add(UItem.asCheck(ID_GHOST_OFFLINE, getString(R.string.MarbleGhostOffline)).setChecked(MarbleGhost.get(MarbleGhost.OFFLINE)));
        items.add(UItem.asShadow(getString(R.string.MarbleGhostInfo)));

        items.add(UItem.asHeader(getString(R.string.MarbleHistoryHeader)));
        items.add(UItem.asCheck(ID_SAVE_DELETED, getString(R.string.MarbleSaveDeleted)).setChecked(MarbleHistory.saveDeleted()));
        items.add(UItem.asCheck(ID_SAVE_EDITED, getString(R.string.MarbleSaveEdited)).setChecked(MarbleHistory.saveEdited()));
        items.add(UItem.asButton(ID_DELETED_LIST, R.drawable.msg_delete, getString(R.string.MarbleDeletedMessages)));
        items.add(UItem.asShadow(getString(R.string.MarbleHistoryInfo)));

        items.add(UItem.asHeader(getString(R.string.MarbleTranslateHeader)));
        items.add(UItem.asShadow(getString(R.string.MarbleTranslateInfo)));

        items.add(UItem.asHeader(getString(R.string.MarbleAiReply)));
        items.add(UItem.asButton(ID_AI_KEY, R.drawable.msg_permissions, getString(R.string.MarbleAiKey),
            MarbleAi.isConfigured() ? "••••" + last4(MarbleAi.apiKey()) : getString(R.string.MarbleAiKeyNotSet)));
        items.add(UItem.asButton(ID_AI_STYLE, R.drawable.msg_customize, getString(R.string.MarbleAiStyle),
            TextUtils.isEmpty(MarbleAi.style()) ? "" : MarbleAi.style()));
        items.add(UItem.asShadow(getString(R.string.MarbleAiInfo)));
    }

    private static String last4(String key) {
        return key.length() <= 4 ? key : key.substring(key.length() - 4);
    }

    private void onItemClick(UItem item, View view, int position, float x, float y) {
        boolean value;
        switch (item.id) {
            case ID_GHOST_READ: MarbleGhost.set(MarbleGhost.NO_READ, value = !MarbleGhost.get(MarbleGhost.NO_READ)); break;
            case ID_GHOST_TYPING: MarbleGhost.set(MarbleGhost.NO_TYPING, value = !MarbleGhost.get(MarbleGhost.NO_TYPING)); break;
            case ID_GHOST_OFFLINE: MarbleGhost.set(MarbleGhost.OFFLINE, value = !MarbleGhost.get(MarbleGhost.OFFLINE)); break;
            case ID_SAVE_DELETED: MarbleHistory.setOption(MarbleHistory.PREF_SAVE_DELETED, value = !MarbleHistory.saveDeleted()); break;
            case ID_SAVE_EDITED: MarbleHistory.setOption(MarbleHistory.PREF_SAVE_EDITED, value = !MarbleHistory.saveEdited()); break;
            case ID_DELETED_LIST: presentFragment(new MarbleDeletedActivity()); return;
            case ID_AI_KEY:
                editText(getString(R.string.MarbleAiKey), MarbleAi.apiKey(), true, v -> {
                    MarbleAi.setApiKey(v);
                    listView.adapter.update(true);
                });
                return;
            case ID_AI_STYLE:
                editText(getString(R.string.MarbleAiStyle), MarbleAi.style(), false, v -> {
                    MarbleAi.setStyle(v);
                    listView.adapter.update(true);
                });
                return;
            default:
                return;
        }
        if (view instanceof TextCheckCell) {
            ((TextCheckCell) view).setChecked(value);
        }
    }

    private void editText(String title, String current, boolean secret, org.telegram.messenger.Utilities.Callback<String> done) {
        Context context = getContext();
        EditTextBoldCursor field = new EditTextBoldCursor(context);
        field.setText(current);
        field.setTextSize(16);
        field.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        field.setLineColors(Theme.getColor(Theme.key_dialogInputField), Theme.getColor(Theme.key_dialogInputFieldActivated), Theme.getColor(Theme.key_text_RedBold));
        field.setInputType(secret ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        field.setPadding(0, dp(8), 0, dp(8));
        FrameLayout container = new FrameLayout(context);
        container.addView(field, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP, 24, 8, 24, 8));
        showDialog(new AlertDialog.Builder(context, resourceProvider)
            .setTitle(title)
            .setView(container)
            .setPositiveButton(getString(R.string.Save), (d, w) -> done.run(field.getText().toString()))
            .setNegativeButton(getString(R.string.Cancel), null)
            .create());
    }
}

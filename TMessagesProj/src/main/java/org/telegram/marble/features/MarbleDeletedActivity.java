/*
 * Marblegram — list of locally saved deleted messages.
 */

package org.telegram.marble.features;

import android.content.Context;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.marble.MarbleTypography;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;

import java.util.ArrayList;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.LocaleController.getString;

public class MarbleDeletedActivity extends BaseFragment {

    private static final int MENU_CLEAR = 1;

    private final long dialogId;
    private UniversalRecyclerView listView;
    private org.telegram.marble.MarbleLargeTopBar topBar;
    private ArrayList<MarbleHistory.Entry> entries;

    public MarbleDeletedActivity() {
        this(0);
    }

    /** dialogId == 0 → all chats. */
    public MarbleDeletedActivity(long dialogId) {
        this.dialogId = dialogId;
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(getString(R.string.MarbleDeletedMessages));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == MENU_CLEAR) {
                    new AlertDialog.Builder(getContext(), resourceProvider)
                        .setTitle(getString(R.string.MarbleClearHistory))
                        .setMessage(getString(R.string.MarbleClearHistoryInfo))
                        .setPositiveButton(getString(R.string.Delete), (d, w) -> MarbleHistory.getInstance().clearAll(() -> load()))
                        .setNegativeButton(getString(R.string.Cancel), null)
                        .show();
                }
            }
        });
        actionBar.createMenu().addItem(MENU_CLEAR, R.drawable.msg_delete);

        FrameLayout contentView = new FrameLayout(context);
        topBar = new org.telegram.marble.MarbleLargeTopBar(context, getString(R.string.MarbleDeletedMessages), null, resourceProvider);
        listView = new UniversalRecyclerView(this, this::fillItems, null, null);
        listView.setSections();
        listView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray, resourceProvider));
        contentView.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.FILL));
        actionBar.setAdaptiveBackground(listView);
        topBar.attach(actionBar, listView);
        load();
        return fragmentView = contentView;
    }

    private void load() {
        MarbleHistory.getInstance().runInBackground(() -> {
            ArrayList<MarbleHistory.Entry> list = MarbleHistory.getInstance().deleted(currentAccount, dialogId, 500);
            AndroidUtilities.runOnUIThread(() -> {
                entries = list;
                if (listView != null) listView.adapter.update(true);
            });
        });
    }

    private String title(long did) {
        MessagesController mc = getMessagesController();
        if (did > 0) {
            TLRPC.User u = mc.getUser(did);
            return u == null ? String.valueOf(did) : (u.first_name + (u.last_name != null ? " " + u.last_name : "")).trim();
        }
        TLRPC.Chat c = mc.getChat(-did);
        return c == null ? String.valueOf(did) : c.title;
    }

    private void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        if (topBar != null) items.add(UItem.asCustomShadow(topBar));
        if (entries == null) {
            return;
        }
        if (entries.isEmpty()) {
            items.add(UItem.asShadow(getString(R.string.MarbleDeletedEmpty)));
            return;
        }
        for (MarbleHistory.Entry e : entries) {
            items.add(UItem.asCustom(new EntryCell(getContext(), e), LayoutHelper.WRAP_CONTENT));
        }
        items.add(UItem.asShadow(getString(R.string.MarbleDeletedInfo)));
    }

    private class EntryCell extends LinearLayout {
        EntryCell(Context context, MarbleHistory.Entry e) {
            super(context);
            setOrientation(VERTICAL);
            setPadding(dp(20), dp(12), dp(20), dp(12));
            TextView head = new TextView(context);
            String who = e.fromId != 0 && e.fromId != e.dialogId ? title(e.fromId) + " · " : "";
            head.setText(title(e.dialogId) + " · " + who + LocaleController.formatDateTime(e.date, true));
            head.setSingleLine();
            head.setEllipsize(TextUtils.TruncateAt.END);
            head.setTypeface(MarbleTypography.label());
            head.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            head.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader));
            addView(head);
            TextView body = new TextView(context);
            body.setText(e.text);
            body.setTextIsSelectable(true);
            body.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            body.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            addView(body, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 4, 0, 0));
            TextView foot = new TextView(context);
            foot.setText(getString(R.string.MarbleDeletedAt) + " " + LocaleController.formatDateTime((int) e.deletedAt, true));
            foot.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            foot.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
            addView(foot, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 4, 0, 0));
        }
    }
}

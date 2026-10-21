/*
 * Marblegram — M3 Expressive MediumFlexibleTopAppBar for list screens.
 * A large headline sits at the top of the list; as it scrolls away it shrinks/fades
 * and the compact ActionBar title fades in (collapsed state).
 */

package org.telegram.marble;

import android.content.Context;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.SimpleTextView;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

public class MarbleLargeTopBar extends LinearLayout {

    private final TextView title;
    private TextView subtitle;

    public MarbleLargeTopBar(Context context, CharSequence titleText, CharSequence subtitleText, Theme.ResourcesProvider rp) {
        super(context);
        setOrientation(VERTICAL);
        setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(12), AndroidUtilities.dp(20), AndroidUtilities.dp(20));

        title = new TextView(context);
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 32); // headlineLarge (emphasized)
        title.setTypeface(MarbleTypography.weight(600));
        title.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, rp));
        title.setText(titleText);
        title.setMaxLines(2);
        title.setEllipsize(TextUtils.TruncateAt.END);
        title.setPivotX(0);
        addView(title, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        if (!TextUtils.isEmpty(subtitleText)) {
            subtitle = new TextView(context);
            subtitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15); // bodyLarge
            subtitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText, rp));
            subtitle.setText(subtitleText);
            addView(subtitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 6, 0, 0));
        }
    }

    /** Hooks the list scroll so the headline collapses into the ActionBar title. */
    public void attach(ActionBar actionBar, RecyclerView listView) {
        final SimpleTextView abTitle = actionBar.getTitleTextView();
        if (abTitle != null) {
            abTitle.setAlpha(0f);
        }
        listView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                update(actionBar);
            }
        });
        addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> update(actionBar));
    }

    private void update(ActionBar actionBar) {
        float collapse;
        if (getParent() == null || !isAttachedToWindow()) {
            collapse = 1f;
        } else {
            final int h = Math.max(1, getHeight());
            collapse = Math.max(0f, Math.min(1f, -getTop() / (float) (h * 0.6f)));
        }
        final float s = 1f - 0.18f * collapse;
        title.setScaleX(s);
        title.setScaleY(s);
        title.setAlpha(1f - collapse);
        if (subtitle != null) {
            subtitle.setAlpha(Math.max(0f, 1f - collapse * 1.8f));
        }
        final SimpleTextView abTitle = actionBar.getTitleTextView();
        if (abTitle != null) {
            abTitle.setAlpha(Math.max(0f, (collapse - 0.55f) / 0.45f));
        }
    }
}

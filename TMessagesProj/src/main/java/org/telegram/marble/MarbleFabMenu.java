/*
 * Marblegram — M3 Expressive FloatingActionButtonMenu for the View system.
 * The FAB morphs into a close button and a column of tonal pill items springs out above it.
 */

package org.telegram.marble;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;

import java.util.ArrayList;

import static org.telegram.messenger.AndroidUtilities.dp;

public class MarbleFabMenu extends FrameLayout {

    public static class Item {
        final int icon;
        final CharSequence text;
        final Runnable action;

        public Item(int icon, CharSequence text, Runnable action) {
            this.icon = icon;
            this.text = text;
            this.action = action;
        }
    }

    private final Paint scrimPaint = new Paint();
    private final LinearLayout column;
    private final ImageView closeButton;
    private final ArrayList<View> rows = new ArrayList<>();
    private float scrim;
    private boolean dismissing;

    /** Shows the menu anchored to {@code fab} (its bottom-right corner stays in place). */
    public static void show(View fab, ArrayList<Item> items) {
        if (!(fab.getContext() instanceof Activity)) {
            return;
        }
        ViewGroup root = ((Activity) fab.getContext()).findViewById(android.R.id.content);
        if (root == null) {
            return;
        }
        MarbleFabMenu menu = new MarbleFabMenu(fab.getContext(), fab, root, items);
        root.addView(menu, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        menu.open();
    }

    private MarbleFabMenu(Context context, View fab, ViewGroup root, ArrayList<Item> items) {
        super(context);
        setWillNotDraw(false);
        final MarbleTheme.Roles r = MarbleTheme.roles(Theme.isCurrentThemeDark());

        int[] fabLoc = new int[2], rootLoc = new int[2];
        fab.getLocationInWindow(fabLoc);
        root.getLocationInWindow(rootLoc);
        final int fabRight = root.getWidth() - (fabLoc[0] - rootLoc[0] + fab.getWidth());
        final int fabBottom = root.getHeight() - (fabLoc[1] - rootLoc[1] + fab.getHeight());
        final int size = Math.max(fab.getWidth(), fab.getHeight());

        // close button in place of the FAB (M3E: FAB morphs into a round primary toggle)
        closeButton = new ImageView(context);
        closeButton.setScaleType(ImageView.ScaleType.CENTER);
        closeButton.setImageResource(R.drawable.ic_close_white);
        closeButton.setColorFilter(r.onPrimary);
        GradientDrawable closeBg = new GradientDrawable();
        closeBg.setShape(GradientDrawable.OVAL);
        closeBg.setColor(r.primary);
        closeButton.setBackground(closeBg);
        closeButton.setElevation(dp(3));
        closeButton.setOnClickListener(v -> dismiss());
        LayoutParams clp = new LayoutParams(size, size, Gravity.BOTTOM | Gravity.RIGHT);
        clp.rightMargin = fabRight;
        clp.bottomMargin = fabBottom;
        addView(closeButton, clp);

        column = new LinearLayout(context);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(Gravity.RIGHT);
        for (int i = 0; i < items.size(); i++) {
            final Item item = items.get(i);
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(16), 0, dp(20), 0);
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dp(28));
            bg.setColor(r.primaryContainer);
            row.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(Theme.multAlpha(r.onPrimaryContainer, .12f)), bg, null));
            row.setElevation(dp(2));

            ImageView icon = new ImageView(context);
            icon.setImageResource(item.icon);
            icon.setColorFilter(r.onPrimaryContainer);
            row.addView(icon, new LinearLayout.LayoutParams(dp(24), dp(24)));

            TextView text = new TextView(context);
            text.setText(item.text);
            text.setTextColor(r.onPrimaryContainer);
            text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            text.setTypeface(MarbleTypography.label());
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            tlp.leftMargin = dp(12);
            row.addView(text, tlp);

            row.setOnClickListener(v -> {
                dismiss();
                AndroidUtilities.runOnUIThread(item.action, 120);
            });
            LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(56));
            rlp.topMargin = i == 0 ? 0 : dp(4);
            column.addView(row, rlp);
            rows.add(row);
        }
        LayoutParams lp = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.RIGHT);
        lp.rightMargin = fabRight;
        lp.bottomMargin = fabBottom + size + dp(8);
        addView(column, lp);

        setOnClickListener(v -> dismiss());
        setClickable(true);
    }

    private void open() {
        ValueAnimator a = ValueAnimator.ofFloat(0, 1);
        a.addUpdateListener(an -> {
            scrim = (float) an.getAnimatedValue();
            invalidate();
        });
        a.setDuration(220);
        a.start();

        closeButton.setRotation(-90);
        new SpringAnimation(closeButton, SpringAnimation.ROTATION, 0).setSpring(new SpringForce(0).setStiffness(400).setDampingRatio(0.6f)).start();

        for (int i = 0; i < rows.size(); i++) {
            View row = rows.get(rows.size() - 1 - i); // bottom item first
            row.setAlpha(0f);
            row.setScaleX(0.6f);
            row.setScaleY(0.6f);
            row.setPivotX(0);
            row.setTranslationY(dp(24));
            final int delay = i * 35;
            postDelayed(() -> {
                row.setPivotX(row.getWidth());
                row.animate().alpha(1f).setDuration(160).start();
                spring(row, SpringAnimation.SCALE_X, 1f);
                spring(row, SpringAnimation.SCALE_Y, 1f);
                spring(row, SpringAnimation.TRANSLATION_Y, 0f);
            }, delay);
        }
    }

    private static void spring(View v, androidx.dynamicanimation.animation.DynamicAnimation.ViewProperty p, float to) {
        new SpringAnimation(v, p, to).setSpring(new SpringForce(to).setStiffness(500).setDampingRatio(0.62f)).start();
    }

    public void dismiss() {
        if (dismissing) {
            return;
        }
        dismissing = true;
        for (View row : rows) {
            row.animate().alpha(0f).translationY(dp(16)).scaleX(0.8f).scaleY(0.8f).setDuration(150).start();
        }
        closeButton.animate().rotation(90).alpha(0f).setDuration(150).start();
        ValueAnimator a = ValueAnimator.ofFloat(scrim, 0);
        a.addUpdateListener(an -> {
            scrim = (float) an.getAnimatedValue();
            invalidate();
        });
        a.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                AndroidUtilities.removeFromParent(MarbleFabMenu.this);
            }
        });
        a.setDuration(180);
        a.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        scrimPaint.setColor(Theme.multAlpha(0x66000000, scrim));
        canvas.drawRect(0, 0, getWidth(), getHeight(), scrimPaint);
    }
}

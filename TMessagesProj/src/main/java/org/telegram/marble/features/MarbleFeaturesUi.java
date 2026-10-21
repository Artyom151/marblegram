/*
 * Marblegram — dialogs for translation, AI replies and message history.
 */

package org.telegram.marble.features;

import android.content.Context;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.marble.MarbleTypography;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RadialProgressView;

import java.util.Locale;

import static org.telegram.messenger.LocaleController.getString;

public final class MarbleFeaturesUi {

    /** Container with a loading indicator that is swapped for text once the result arrives. */
    static class ResultView extends FrameLayout {
        final RadialProgressView progress;
        final TextView text;

        ResultView(Context context) {
            super(context);
            progress = new RadialProgressView(context);
            progress.setSize(AndroidUtilities.dp(36));
            addView(progress, LayoutHelper.createFrame(48, 48, Gravity.CENTER, 0, 12, 0, 12));
            text = new TextView(context);
            text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            text.setLineSpacing(AndroidUtilities.dp(2), 1f);
            text.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
            text.setTextIsSelectable(true);
            text.setVisibility(GONE);
            addView(text, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP, 24, 4, 24, 8));
        }

        void show(CharSequence value, boolean isError) {
            progress.setVisibility(GONE);
            text.setVisibility(VISIBLE);
            text.setText(value);
            text.setTextColor(isError ? Theme.getColor(Theme.key_text_RedRegular) : Theme.getColor(Theme.key_dialogTextBlack));
        }
    }

    public static void showTranslation(BaseFragment fragment, String original) {
        if (fragment == null || fragment.getParentActivity() == null || TextUtils.isEmpty(original)) {
            return;
        }
        Context context = fragment.getParentActivity();
        ResultView view = new ResultView(context);
        String[] result = new String[1];
        AlertDialog dialog = new AlertDialog.Builder(context, fragment.getResourceProvider())
            .setTitle(getString(R.string.MarbleTranslateTitle))
            .setView(view)
            .setPositiveButton(getString(R.string.Copy), (d, w) -> {
                if (result[0] != null) {
                    AndroidUtilities.addToClipboard(result[0]);
                    BulletinFactory.of(fragment).createCopyBulletin(getString(R.string.TextCopied)).show();
                }
            })
            .setNegativeButton(getString(R.string.Close), null)
            .create();
        fragment.showDialog(dialog);
        MarbleTranslator.translate(original, MarbleTranslator.defaultTargetLanguage(), (r, error) -> {
            if (r != null) {
                result[0] = r.text;
                view.show(r.text, false);
                if (r.sourceLang != null) {
                    dialog.setTitle(getString(R.string.MarbleTranslateTitle) + " · " + r.sourceLang.toUpperCase(Locale.ROOT) + " → " + MarbleTranslator.defaultTargetLanguage().toUpperCase(Locale.ROOT));
                }
            } else {
                view.show(getString(R.string.MarbleTranslateError) + (error != null ? "\n" + error : ""), true);
            }
        });
    }

    /** Shows every stored version of a message, oldest first. */
    public static void showEditHistory(BaseFragment fragment, int account, long dialogId, int messageId) {
        if (fragment == null || fragment.getParentActivity() == null) {
            return;
        }
        Context context = fragment.getParentActivity();
        ResultView view = new ResultView(context);
        AlertDialog dialog = new AlertDialog.Builder(context, fragment.getResourceProvider())
            .setTitle(getString(R.string.MarbleEditHistory))
            .setView(view)
            .setPositiveButton(getString(R.string.Close), null)
            .create();
        fragment.showDialog(dialog);
        MarbleHistory.getInstance().runInBackground(() -> {
            java.util.ArrayList<MarbleHistory.Entry> versions = MarbleHistory.getInstance().versions(account, dialogId, messageId);
            android.text.SpannableStringBuilder sb = new android.text.SpannableStringBuilder();
            for (int i = 0; i < versions.size(); i++) {
                MarbleHistory.Entry e = versions.get(i);
                int start = sb.length();
                String header = (i == 0 ? getString(R.string.MarbleOriginal) : getString(R.string.MarbleVersion) + " " + i)
                    + " · " + org.telegram.messenger.LocaleController.formatDateTime(e.editDate != 0 && i > 0 ? e.editDate : e.date, true);
                sb.append(header).append('\n');
                sb.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), start, sb.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                sb.setSpan(new android.text.style.ForegroundColorSpan(Theme.getColor(Theme.key_dialogTextBlue)), start, sb.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                sb.append(e.text);
                if (i < versions.size() - 1) sb.append("\n\n");
            }
            final CharSequence result = versions.size() < 2 ? getString(R.string.MarbleNoEditHistory) : sb;
            AndroidUtilities.runOnUIThread(() -> view.show(result, false));
        });
    }

    /** Drafts a reply with Claude; "Insert" puts it into the input field via {@code insert}. */
    public static void showAiReply(BaseFragment fragment, int account, long dialogId, java.util.List<org.telegram.messenger.MessageObject> messages,
                                   org.telegram.messenger.MessageObject target, org.telegram.messenger.Utilities.Callback<String> insert) {
        if (fragment == null || fragment.getParentActivity() == null) {
            return;
        }
        Context context = fragment.getParentActivity();
        if (!MarbleAi.isConfigured()) {
            fragment.showDialog(new AlertDialog.Builder(context, fragment.getResourceProvider())
                .setTitle(getString(R.string.MarbleAiReply))
                .setMessage(getString(R.string.MarbleAiNoKey))
                .setPositiveButton(getString(R.string.MarbleAiOpenSettings), (d, w) -> fragment.presentFragment(new MarbleFeaturesSettingsActivity()))
                .setNegativeButton(getString(R.string.Cancel), null)
                .create());
            return;
        }
        ResultView view = new ResultView(context);
        String[] result = new String[1];
        AlertDialog dialog = new AlertDialog.Builder(context, fragment.getResourceProvider())
            .setTitle(getString(R.string.MarbleAiReply))
            .setView(view)
            .setPositiveButton(getString(R.string.MarbleAiInsert), (d, w) -> {
                if (result[0] != null) insert.run(result[0]);
            })
            .setNeutralButton(getString(R.string.MarbleAiAnother), (d, w) -> showAiReply(fragment, account, dialogId, messages, target, insert))
            .setNegativeButton(getString(R.string.Close), null)
            .create();
        fragment.showDialog(dialog);
        MarbleAi.draftReply(account, dialogId, messages, target, (reply, error) -> {
            if (reply != null) {
                result[0] = reply;
                view.show(reply, false);
                return;
            }
            String msg;
            switch (error == null ? "" : error) {
                case "bad_key": msg = getString(R.string.MarbleAiBadKey); break;
                case "rate_limit": msg = getString(R.string.MarbleAiRateLimit); break;
                case "network": msg = getString(R.string.MarbleAiNetwork); break;
                case "refusal": msg = getString(R.string.MarbleAiRefusal); break;
                default: msg = getString(R.string.MarbleAiError) + (error != null ? "\n" + error : "");
            }
            view.show(msg, true);
        });
    }

    private MarbleFeaturesUi() {}
}

/*
 * Marblegram — AI reply drafts powered by the Claude API (official Anthropic Java SDK).
 * The user supplies their own API key (Settings → Marblegram → AI replies); the key never leaves the device
 * except in requests to api.anthropic.com.
 */

package org.telegram.marble.features;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.errors.AnthropicIoException;
import com.anthropic.errors.AnthropicServiceException;
import com.anthropic.errors.PermissionDeniedException;
import com.anthropic.errors.RateLimitException;
import com.anthropic.errors.UnauthorizedException;
import com.anthropic.models.beta.messages.BetaContentBlock;
import com.anthropic.models.beta.messages.BetaFallbacksParam;
import com.anthropic.models.beta.messages.BetaMessage;
import com.anthropic.models.beta.messages.BetaOutputConfig;
import com.anthropic.models.beta.messages.BetaStopReason;
import com.anthropic.models.beta.messages.MessageCreateParams;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MarbleAi {

    public static final String MODEL = "claude-opus-5-5";
    private static final String PREF_KEY = "aiApiKey";
    private static final String PREF_STYLE = "aiStyle";
    private static final int CONTEXT_MESSAGES = 30;

    public interface Callback {
        void onResult(String reply, String error);
    }

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static AnthropicClient client;
    private static String clientKey;

    private static SharedPreferences prefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences("marblegram", Context.MODE_PRIVATE);
    }

    public static String apiKey() {
        return prefs().getString(PREF_KEY, "");
    }

    public static void setApiKey(String key) {
        prefs().edit().putString(PREF_KEY, key == null ? "" : key.trim()).apply();
    }

    public static boolean isConfigured() {
        return !TextUtils.isEmpty(apiKey());
    }

    /** Optional free-form instruction, e.g. "отвечай коротко и с юмором". */
    public static String style() {
        return prefs().getString(PREF_STYLE, "");
    }

    public static void setStyle(String style) {
        prefs().edit().putString(PREF_STYLE, style == null ? "" : style.trim()).apply();
    }

    private static synchronized AnthropicClient client() {
        String key = apiKey();
        if (client == null || !key.equals(clientKey)) {
            client = AnthropicOkHttpClient.builder().apiKey(key).build();
            clientKey = key;
        }
        return client;
    }

    private static String name(int account, long id) {
        MessagesController mc = MessagesController.getInstance(account);
        if (id > 0) {
            TLRPC.User u = mc.getUser(id);
            if (u == null) return "User";
            return (u.first_name + (u.last_name != null ? " " + u.last_name : "")).trim();
        }
        TLRPC.Chat c = mc.getChat(-id);
        return c == null ? "Chat" : c.title;
    }

    /**
     * Drafts a reply to the conversation. {@code messages} are in Telegram order (newest first);
     * {@code target}, if not null, is the message the user wants to answer.
     */
    public static void draftReply(int account, long dialogId, List<MessageObject> messages, MessageObject target, Callback callback) {
        if (!isConfigured()) {
            callback.onResult(null, "no_key");
            return;
        }
        final long selfId = UserConfig.getInstance(account).getClientUserId();
        final String selfName = name(account, selfId);
        final String chatName = name(account, dialogId);
        // oldest → newest transcript of the latest text messages
        final ArrayList<String> lines = new ArrayList<>();
        int taken = 0;
        for (int i = 0; i < messages.size() && taken < CONTEXT_MESSAGES; i++) {
            MessageObject m = messages.get(i);
            if (m == null || m.messageOwner == null || TextUtils.isEmpty(m.messageOwner.message)) continue;
            String who = m.isOut() ? selfName + " (me)" : name(account, m.getFromChatId());
            lines.add(0, who + ": " + m.messageOwner.message);
            taken++;
        }
        final String targetText = target != null && target.messageOwner != null ? target.messageOwner.message : null;
        final String targetFrom = target != null ? name(account, target.getFromChatId()) : null;

        executor.execute(() -> {
            String reply = null;
            String error = null;
            try {
                StringBuilder user = new StringBuilder();
                user.append("Chat: ").append(chatName).append("\n\nRecent messages (oldest first):\n");
                for (String l : lines) user.append(l).append('\n');
                if (!TextUtils.isEmpty(targetText)) {
                    user.append("\nWrite my reply to this message from ").append(targetFrom).append(":\n").append(targetText).append('\n');
                } else {
                    user.append("\nWrite my next message in this chat.\n");
                }

                String system = "You draft Telegram messages on behalf of " + selfName + ". "
                    + "Write exactly one reply the way they would type it in this chat: same language as the conversation, "
                    + "same tone and level of formality, natural length for a chat message. "
                    + "Output only the message text, with no quotes, names or explanations.";
                String extra = style();
                if (!TextUtils.isEmpty(extra)) {
                    system += " Additional preferences from the user: " + extra;
                }

                MessageCreateParams params = MessageCreateParams.builder()
                    .model(MODEL)
                    .maxTokens(4096L)
                    .system(system)
                    .outputConfig(BetaOutputConfig.builder().effort(BetaOutputConfig.Effort.LOW).build())
                    // re-run on a fallback model if the request is declined by a safety classifier
                    .addBeta("server-side-fallback-2026-07-01")
                    .fallbacks(BetaFallbacksParam.ofDefault())
                    .addUserMessage(user.toString())
                    .build();

                BetaMessage response = client().beta().messages().create(params);
                if (response.stopReason().isPresent() && response.stopReason().get().equals(BetaStopReason.REFUSAL)) {
                    error = "refusal";
                } else {
                    StringBuilder out = new StringBuilder();
                    for (BetaContentBlock block : response.content()) {
                        block.text().ifPresent(t -> out.append(t.text()));
                    }
                    reply = out.toString().trim();
                    if (reply.isEmpty()) {
                        error = "empty";
                    }
                }
            } catch (UnauthorizedException | PermissionDeniedException e) {
                error = "bad_key";
            } catch (RateLimitException e) {
                error = "rate_limit";
            } catch (AnthropicServiceException e) {
                error = "HTTP " + e.statusCode() + ": " + e.getMessage();
            } catch (AnthropicIoException e) {
                error = "network";
            } catch (Exception e) {
                error = String.valueOf(e.getMessage());
            }
            final String r = reply, err = error;
            AndroidUtilities.runOnUIThread(() -> callback.onResult(r, err));
        });
    }

    private MarbleAi() {}
}

/*
 * Marblegram — ghost mode. Filters outgoing MTProto requests that reveal activity:
 *   no read receipts  — messages/channels/encrypted read history, read contents, story views
 *   no typing         — setTyping / setEncryptedTyping
 *   stay offline      — account.updateStatus is always sent with offline = true
 * Local state still updates (chats are marked read on this device), only the server is not told.
 */

package org.telegram.marble.features;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_account;
import org.telegram.tgnet.tl.TL_stories;

public final class MarbleGhost {

    public static final String NO_READ = "ghostNoRead", NO_TYPING = "ghostNoTyping", OFFLINE = "ghostOffline";

    private static SharedPreferences prefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences("marblegram", Context.MODE_PRIVATE);
    }

    private static volatile boolean noRead, noTyping, offline, loaded;

    public static void reload() {
        try {
            SharedPreferences p = prefs();
            noRead = p.getBoolean(NO_READ, false);
            noTyping = p.getBoolean(NO_TYPING, false);
            offline = p.getBoolean(OFFLINE, false);
            loaded = true;
        } catch (Exception ignore) {}
    }

    public static boolean get(String key) {
        if (!loaded) reload();
        switch (key) {
            case NO_READ: return noRead;
            case NO_TYPING: return noTyping;
            case OFFLINE: return offline;
        }
        return false;
    }

    public static void set(String key, boolean value) {
        prefs().edit().putBoolean(key, value).apply();
        reload();
    }

    public static boolean isActive() {
        if (!loaded) reload();
        return noRead || noTyping || offline;
    }

    /**
     * Called from ConnectionsManager before a request is queued.
     * @return true if the request must not be sent at all.
     */
    public static boolean shouldDrop(TLObject request) {
        if (!loaded) reload();
        if (!noRead && !noTyping && !offline) {
            return false;
        }
        if (noRead && (request instanceof TLRPC.TL_messages_readHistory
                || request instanceof TLRPC.TL_channels_readHistory
                || request instanceof TLRPC.TL_messages_readEncryptedHistory
                || request instanceof TLRPC.TL_messages_readMessageContents
                || request instanceof TLRPC.TL_channels_readMessageContents
                || request instanceof TLRPC.TL_messages_readDiscussion
                || request instanceof TL_stories.TL_stories_readStories
                || request instanceof TL_stories.TL_stories_incrementStoryViews)) {
            android.util.Log.i("MarbleGhost", "dropped " + request.getClass().getSimpleName());
            return true;
        }
        if (noTyping && (request instanceof TLRPC.TL_messages_setTyping
                || request instanceof TLRPC.TL_messages_setEncryptedTyping)) {
            android.util.Log.i("MarbleGhost", "dropped " + request.getClass().getSimpleName());
            return true;
        }
        if (offline && request instanceof TL_account.updateStatus) {
            ((TL_account.updateStatus) request).offline = true;
            android.util.Log.i("MarbleGhost", "forced offline status");
        }
        return false;
    }

    private MarbleGhost() {}
}

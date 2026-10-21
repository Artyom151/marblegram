/*
 * Marblegram — local history of deleted and edited messages.
 *
 * Every text message this device sees (loaded in a chat, received, edited) is copied into a private
 * SQLite database (marble_history.db). Edits add a new version; deletions mark the row as deleted
 * instead of losing it. Nothing is uploaded anywhere.
 */

package org.telegram.marble.features;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.UserConfig;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MarbleHistory implements NotificationCenter.NotificationCenterDelegate {

    public static class Entry {
        public long dialogId;
        public int messageId;
        public int version;
        public long fromId;
        public int date;
        public int editDate;
        public String text;
        public boolean deleted;
        public long deletedAt;
        boolean channel;
    }

    public static final String PREF_SAVE_DELETED = "saveDeleted";
    public static final String PREF_SAVE_EDITED = "saveEdited";

    private static volatile MarbleHistory instance;

    public static MarbleHistory getInstance() {
        if (instance == null) {
            synchronized (MarbleHistory.class) {
                if (instance == null) instance = new MarbleHistory();
            }
        }
        return instance;
    }

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private SQLiteOpenHelper helper;
    private boolean registered;

    private static SharedPreferences prefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences("marblegram", Context.MODE_PRIVATE);
    }

    public static boolean saveDeleted() {
        return prefs().getBoolean(PREF_SAVE_DELETED, true);
    }

    public static boolean saveEdited() {
        return prefs().getBoolean(PREF_SAVE_EDITED, true);
    }

    public static void setOption(String key, boolean value) {
        prefs().edit().putBoolean(key, value).apply();
    }

    private SQLiteDatabase db() {
        if (helper == null) {
            helper = new SQLiteOpenHelper(ApplicationLoader.applicationContext, "marble_history.db", null, 1) {
                @Override
                public void onCreate(SQLiteDatabase db) {
                    db.execSQL("CREATE TABLE messages (account INTEGER, dialog_id INTEGER, msg_id INTEGER, version INTEGER, from_id INTEGER, date INTEGER, edit_date INTEGER, text TEXT, channel INTEGER DEFAULT 0, deleted INTEGER DEFAULT 0, deleted_at INTEGER DEFAULT 0, PRIMARY KEY(account, dialog_id, msg_id, version))");
                    db.execSQL("CREATE INDEX messages_by_id ON messages(account, msg_id)");
                    db.execSQL("CREATE INDEX messages_deleted ON messages(account, deleted, deleted_at)");
                }

                @Override
                public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}
            };
        }
        return helper.getWritableDatabase();
    }

    /** Called once at startup (LaunchActivity). */
    public void init() {
        if (registered) return;
        registered = true;
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            NotificationCenter nc = NotificationCenter.getInstance(a);
            nc.addObserver(this, NotificationCenter.messagesDidLoad);
            nc.addObserver(this, NotificationCenter.didReceiveNewMessages);
            nc.addObserver(this, NotificationCenter.replaceMessagesObjects);
            nc.addObserver(this, NotificationCenter.messagesDeleted);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        android.util.Log.i("MarbleHistory", "event " + id + " account " + account + " args " + args.length);
        if (!saveDeleted() && !saveEdited()) {
            return;
        }
        if (id == NotificationCenter.messagesDidLoad) {
            record(account, (ArrayList<MessageObject>) args[2]);
        } else if (id == NotificationCenter.didReceiveNewMessages) {
            if (args.length > 2 && Boolean.TRUE.equals(args[2])) return; // scheduled
            record(account, (ArrayList<MessageObject>) args[1]);
        } else if (id == NotificationCenter.replaceMessagesObjects) {
            if (saveEdited()) record(account, (ArrayList<MessageObject>) args[1]);
        } else if (id == NotificationCenter.messagesDeleted) {
            if (args.length > 2 && Boolean.TRUE.equals(args[2])) return; // scheduled
            if (!saveDeleted()) return;
            ArrayList<Integer> ids = (ArrayList<Integer>) args[0];
            long channelId = (Long) args[1];
            markDeleted(account, channelId, new ArrayList<>(ids));
        }
    }

    private void record(int account, ArrayList<MessageObject> objects) {
        if (objects == null || objects.isEmpty()) return;
        final ArrayList<Entry> entries = new ArrayList<>();
        for (MessageObject m : objects) {
            if (m == null || m.messageOwner == null || m.getId() <= 0 || m.scheduled) continue;
            String text = m.messageOwner.message;
            if (TextUtils.isEmpty(text)) continue;
            Entry e = new Entry();
            e.dialogId = m.getDialogId();
            e.messageId = m.getId();
            e.fromId = m.getFromChatId();
            e.date = m.messageOwner.date;
            e.editDate = m.messageOwner.edit_date;
            e.text = text;
            e.channel = m.messageOwner.peer_id != null && m.messageOwner.peer_id.channel_id != 0;
            entries.add(e);
        }
        if (entries.isEmpty()) return;
        android.util.Log.i("MarbleHistory", "recording " + entries.size() + " messages");
        io.execute(() -> {
            SQLiteDatabase db = db();
            db.beginTransaction();
            try {
                for (Entry e : entries) {
                    // latest stored version for this message
                    String lastText = null;
                    int lastVersion = -1;
                    try (Cursor c = db.rawQuery("SELECT version, text FROM messages WHERE account = ? AND dialog_id = ? AND msg_id = ? ORDER BY version DESC LIMIT 1",
                        new String[]{String.valueOf(account), String.valueOf(e.dialogId), String.valueOf(e.messageId)})) {
                        if (c.moveToFirst()) {
                            lastVersion = c.getInt(0);
                            lastText = c.getString(1);
                        }
                    }
                    if (e.text.equals(lastText)) continue;
                    if (lastVersion >= 0 && !saveEdited()) continue;
                    ContentValues v = new ContentValues();
                    v.put("account", account);
                    v.put("dialog_id", e.dialogId);
                    v.put("msg_id", e.messageId);
                    v.put("version", lastVersion + 1);
                    v.put("from_id", e.fromId);
                    v.put("date", e.date);
                    v.put("edit_date", e.editDate);
                    v.put("text", e.text);
                    v.put("channel", e.channel ? 1 : 0);
                    db.insertWithOnConflict("messages", null, v, SQLiteDatabase.CONFLICT_IGNORE);
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        });
    }

    private void markDeleted(int account, long channelId, ArrayList<Integer> ids) {
        if (ids.isEmpty()) return;
        android.util.Log.i("MarbleHistory", "deleted " + ids + " channel " + channelId);
        final long now = System.currentTimeMillis() / 1000;
        io.execute(() -> {
            SQLiteDatabase db = db();
            db.beginTransaction();
            try {
                for (Integer id : ids) {
                    ContentValues v = new ContentValues();
                    v.put("deleted", 1);
                    v.put("deleted_at", now);
                    if (channelId != 0) {
                        db.update("messages", v, "account = ? AND dialog_id = ? AND msg_id = ? AND deleted = 0",
                            new String[]{String.valueOf(account), String.valueOf(-channelId), String.valueOf(id)});
                    } else {
                        // private chats and basic groups share one id space per account; channels are excluded
                        db.update("messages", v, "account = ? AND msg_id = ? AND deleted = 0 AND channel = 0",
                            new String[]{String.valueOf(account), String.valueOf(id)});
                    }
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        });
    }

    private static Entry read(Cursor c) {
        Entry e = new Entry();
        e.dialogId = c.getLong(0);
        e.messageId = c.getInt(1);
        e.version = c.getInt(2);
        e.fromId = c.getLong(3);
        e.date = c.getInt(4);
        e.editDate = c.getInt(5);
        e.text = c.getString(6);
        e.deleted = c.getInt(7) != 0;
        e.deletedAt = c.getLong(8);
        return e;
    }

    private static final String COLUMNS = "dialog_id, msg_id, version, from_id, date, edit_date, text, deleted, deleted_at";

    /** All stored versions of one message, oldest first. Runs synchronously — call off the UI thread. */
    public ArrayList<Entry> versions(int account, long dialogId, int messageId) {
        ArrayList<Entry> out = new ArrayList<>();
        try (Cursor c = db().rawQuery("SELECT " + COLUMNS + " FROM messages WHERE account = ? AND dialog_id = ? AND msg_id = ? ORDER BY version",
            new String[]{String.valueOf(account), String.valueOf(dialogId), String.valueOf(messageId)})) {
            while (c.moveToNext()) out.add(read(c));
        }
        return out;
    }

    /** Latest version of every deleted message (optionally in one dialog), newest deletions first. */
    public ArrayList<Entry> deleted(int account, long dialogId, int limit) {
        ArrayList<Entry> out = new ArrayList<>();
        String where = "m.account = ? AND m.deleted = 1" + (dialogId != 0 ? " AND m.dialog_id = " + dialogId : "");
        try (Cursor c = db().rawQuery("SELECT " + COLUMNS.replaceAll("(\\w+)", "m.$1") + " FROM messages m WHERE " + where
                + " AND m.version = (SELECT MAX(version) FROM messages x WHERE x.account = m.account AND x.dialog_id = m.dialog_id AND x.msg_id = m.msg_id)"
                + " ORDER BY m.deleted_at DESC LIMIT " + limit,
            new String[]{String.valueOf(account)})) {
            while (c.moveToNext()) out.add(read(c));
        }
        return out;
    }

    public void clearAll(Runnable done) {
        io.execute(() -> {
            db().delete("messages", null, null);
            if (done != null) org.telegram.messenger.AndroidUtilities.runOnUIThread(done);
        });
    }

    public void runInBackground(Runnable r) {
        io.execute(r);
    }

    private MarbleHistory() {}
}

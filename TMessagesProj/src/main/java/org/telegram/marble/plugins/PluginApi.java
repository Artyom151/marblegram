/*
 * Marblegram Plugin SDK — the `tg` object every plugin gets.
 * Everything here is safe to call from Lua and Python. For anything not covered, plugins can reach
 * the full Telegram codebase through the Java bridge (luajava.bindClass / jclass) — see docs/plugins.
 */

package org.telegram.marble.plugins;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.UserConfig;
import org.telegram.marble.features.MarbleHistory;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.LaunchActivity;

import java.util.ArrayList;
import java.util.HashMap;

public class PluginApi {

    /** SDK level; bump when the API grows. Marblegram: 3 added permissions, current_chat, send_text, history_*. */
    public static final int SDK_VERSION = 3;

    public static class Action {
        public final String title;
        public final String function;

        Action(String title, String function) {
            this.title = title;
            this.function = function;
        }
    }

    private final Plugin plugin;
    final ArrayList<Action> actions = new ArrayList<>();

    PluginApi(Plugin plugin) {
        this.plugin = plugin;
    }

    /** Marblegram: throws unless plugin.json declared the permission. Used by the SDK 3 methods below. */
    private void requirePermission(String permission) {
        if (!plugin.manifest.hasPermission(permission)) {
            throw new SecurityException("plugin '" + plugin.manifest.id + "' needs permission \"" + permission + "\" in plugin.json");
        }
    }

    // --- info ----------------------------------------------------------------------------------

    public int sdk_version() { return SDK_VERSION; }
    public String plugin_id() { return plugin.manifest.id; }
    public String plugin_dir() { return plugin.dir.getAbsolutePath(); }
    public Context context() { return ApplicationLoader.applicationContext; }
    public int account() { return UserConfig.selectedAccount; }
    public long self_id() { return UserConfig.getInstance(UserConfig.selectedAccount).getClientUserId(); }

    public String user_name(long userId) {
        TLRPC.User user = MessagesController.getInstance(UserConfig.selectedAccount).getUser(userId);
        return user == null ? null : (user.first_name + (user.last_name != null ? " " + user.last_name : "")).trim();
    }

    public String chat_title(long dialogId) {
        if (dialogId > 0) {
            return user_name(dialogId);
        }
        TLRPC.Chat chat = MessagesController.getInstance(UserConfig.selectedAccount).getChat(-dialogId);
        return chat == null ? null : chat.title;
    }

    // --- output --------------------------------------------------------------------------------

    public void log(Object message) {
        Log.i("MarblePlugin", "[" + plugin.manifest.id + "] " + message);
    }

    /** Shows a Material snackbar (Telegram bulletin) on the current screen. */
    public void toast(Object text) {
        AndroidUtilities.runOnUIThread(() -> {
            BaseFragment fragment = LaunchActivity.getLastFragment();
            if (fragment != null) {
                BulletinFactory.of(fragment).createSimpleBulletin(org.telegram.messenger.R.raw.info, String.valueOf(text)).show();
            }
        });
    }

    // --- messaging -----------------------------------------------------------------------------

    /** Sends a text message to a dialog (user id > 0, chat/channel id < 0). Plugin hooks are not re-run for it. */
    public void send_message(long dialogId, String text) {
        AndroidUtilities.runOnUIThread(() -> {
            PluginManager.getInstance().suppressHooks = true;
            try {
                SendMessagesHelper.getInstance(UserConfig.selectedAccount).sendMessage(SendMessagesHelper.SendMessageParams.of(text, dialogId));
            } finally {
                PluginManager.getInstance().suppressHooks = false;
            }
        });
    }

    // --- Marblegram SDK 3: chat, confirmed sending, local history ---------------------------------
    // Permissions are declared in plugin.json: "permissions": ["chat.read", "messages.send", "history.read"].

    /**
     * Marblegram (permission chat.read): the chat on top of the screen stack, or null if it is not a chat.
     * Returns a map with "dialog_id" (Long) and "title" (String).
     */
    public HashMap<String, Object> current_chat() {
        requirePermission(PluginManifest.PERM_CHAT_READ);
        BaseFragment f = fragment();
        if (!(f instanceof ChatActivity)) {
            return null;
        }
        long dialogId = ((ChatActivity) f).getDialogId();
        HashMap<String, Object> out = new HashMap<>();
        out.put("dialog_id", dialogId);
        out.put("title", chat_title(dialogId));
        return out;
    }

    /**
     * Marblegram (permission messages.send): shows a confirmation dialog with the chat and text;
     * the message is sent only if the user taps Send. Ghost mode filters still apply to the request.
     */
    public void send_text(long dialogId, String text) {
        requirePermission(PluginManifest.PERM_MESSAGES_SEND);
        AndroidUtilities.runOnUIThread(() -> {
            BaseFragment f = fragment();
            if (f == null || f.getParentActivity() == null) return;
            String title = chat_title(dialogId);
            f.showDialog(new org.telegram.ui.ActionBar.AlertDialog.Builder(f.getParentActivity())
                .setTitle(plugin.manifest.name)
                .setMessage((title == null ? String.valueOf(dialogId) : title) + "\n\n" + text)
                .setPositiveButton(org.telegram.messenger.LocaleController.getString(org.telegram.messenger.R.string.Send),
                    (d, w) -> send_message(dialogId, text))
                .setNegativeButton(org.telegram.messenger.LocaleController.getString(org.telegram.messenger.R.string.Cancel), null)
                .create());
        });
    }

    /**
     * Marblegram (permission history.read): latest version of deleted messages from the local history
     * (dialog_id 0 = all chats). Each item is a map: dialog_id, message_id, from_id, date, text, deleted_at.
     * Reads the device database only. Call from run_in_background.
     */
    public java.util.List<HashMap<String, Object>> history_deleted(long dialogId, int limit) {
        requirePermission(PluginManifest.PERM_HISTORY_READ);
        ArrayList<HashMap<String, Object>> out = new ArrayList<>();
        for (MarbleHistory.Entry e : MarbleHistory.getInstance().deleted(account(), dialogId, Math.max(1, Math.min(limit, 500)))) {
            out.add(entry_map(e));
        }
        return out;
    }

    /**
     * Marblegram (permission history.read): every stored version of one message, oldest first, from the
     * local history. Maps as in history_deleted, plus "version" and "edit_date". Call from run_in_background.
     */
    public java.util.List<HashMap<String, Object>> history_versions(long dialogId, int messageId) {
        requirePermission(PluginManifest.PERM_HISTORY_READ);
        ArrayList<HashMap<String, Object>> out = new ArrayList<>();
        for (MarbleHistory.Entry e : MarbleHistory.getInstance().versions(account(), dialogId, messageId)) {
            HashMap<String, Object> m = entry_map(e);
            m.put("version", e.version);
            m.put("edit_date", e.editDate);
            out.add(m);
        }
        return out;
    }

    private static HashMap<String, Object> entry_map(MarbleHistory.Entry e) {
        HashMap<String, Object> m = new HashMap<>();
        m.put("dialog_id", e.dialogId);
        m.put("message_id", e.messageId);
        m.put("from_id", e.fromId);
        m.put("date", e.date);
        m.put("text", e.text);
        m.put("deleted_at", e.deletedAt);
        return m;
    }

    // --- UI ------------------------------------------------------------------------------------

    /** Adds a button to the plugin's page in Settings → Marblegram → Plugins; calls the named function on tap. */
    public void add_action(String title, String functionName) {
        actions.add(new Action(title, functionName));
    }

    public void run_on_ui(Runnable runnable) {
        AndroidUtilities.runOnUIThread(runnable);
    }

    /** Runs a task on a background thread (for network or heavy work). */
    public void run_in_background(Runnable runnable) {
        new Thread(() -> {
            try {
                runnable.run();
            } catch (Throwable e) {
                plugin.error = "background: " + e.getMessage();
                Log.e("MarblePlugin", plugin.manifest.id + " background task", e);
            }
        }, "plugin-" + plugin.manifest.id).start();
    }

    /**
     * Shows a Material text input dialog; calls the named plugin function with the entered text.
     * @param secret hide the text (for API keys)
     */
    public void input(String title, String hint, String current, boolean secret, String functionName) {
        AndroidUtilities.runOnUIThread(() -> {
            BaseFragment fragment = LaunchActivity.getLastFragment();
            if (fragment == null || fragment.getParentActivity() == null) return;
            Context ctx = fragment.getParentActivity();
            org.telegram.ui.Components.EditTextBoldCursor field = new org.telegram.ui.Components.EditTextBoldCursor(ctx);
            field.setText(current == null ? "" : current);
            field.setHint(hint);
            field.setTextSize(16);
            field.setTextColor(org.telegram.ui.ActionBar.Theme.getColor(org.telegram.ui.ActionBar.Theme.key_dialogTextBlack));
            field.setHintTextColor(org.telegram.ui.ActionBar.Theme.getColor(org.telegram.ui.ActionBar.Theme.key_dialogTextHint));
            field.setLineColors(org.telegram.ui.ActionBar.Theme.getColor(org.telegram.ui.ActionBar.Theme.key_dialogInputField),
                org.telegram.ui.ActionBar.Theme.getColor(org.telegram.ui.ActionBar.Theme.key_dialogInputFieldActivated),
                org.telegram.ui.ActionBar.Theme.getColor(org.telegram.ui.ActionBar.Theme.key_text_RedBold));
            field.setInputType(secret ? android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD : android.text.InputType.TYPE_CLASS_TEXT);
            android.widget.FrameLayout box = new android.widget.FrameLayout(ctx);
            box.addView(field, org.telegram.ui.Components.LayoutHelper.createFrame(-1, -2, android.view.Gravity.TOP, 24, 8, 24, 8));
            fragment.showDialog(new org.telegram.ui.ActionBar.AlertDialog.Builder(ctx)
                .setTitle(title)
                .setView(box)
                .setPositiveButton(org.telegram.messenger.LocaleController.getString(org.telegram.messenger.R.string.OK),
                    (d, w) -> PluginManager.getInstance().callFunction(plugin, functionName, field.getText().toString()))
                .setNegativeButton(org.telegram.messenger.LocaleController.getString(org.telegram.messenger.R.string.Cancel), null)
                .create());
        });
    }

    // --- network (call from run_in_background, never on the UI thread) --------------------------

    public String http_get(String url) throws Exception {
        return http("GET", url, null, null);
    }

    public String http_post(String url, String body, String contentType) throws Exception {
        return http("POST", url, body, contentType);
    }

    private static String http(String method, String url, String body, String contentType) throws Exception {
        java.net.HttpURLConnection c = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
        c.setRequestMethod(method);
        c.setConnectTimeout(15000);
        c.setReadTimeout(60000);
        c.setRequestProperty("User-Agent", "Marblegram-Plugin/" + SDK_VERSION);
        if (body != null) {
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type", contentType == null ? "application/json; charset=utf-8" : contentType);
            try (java.io.OutputStream os = c.getOutputStream()) {
                os.write(body.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
        }
        int code = c.getResponseCode();
        java.io.InputStream in = code >= 400 ? c.getErrorStream() : c.getInputStream();
        StringBuilder sb = new StringBuilder();
        if (in != null) {
            try (java.io.BufferedReader r = new java.io.BufferedReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) sb.append(line).append('\n');
            }
        }
        c.disconnect();
        if (code >= 400) {
            throw new java.io.IOException("HTTP " + code + ": " + sb);
        }
        return sb.toString();
    }

    // --- storage -------------------------------------------------------------------------------

    private SharedPreferences prefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences("marble_plugin_" + plugin.manifest.id, Context.MODE_PRIVATE);
    }

    public String get_setting(String key, String def) { return prefs().getString(key, def); }
    public void set_setting(String key, String value) { prefs().edit().putString(key, value).apply(); }

    // =============================================================================================
    // SDK 2 — full access to Telegram
    // =============================================================================================

    // --- reflection: any class, field, method (private too) --------------------------------------

    /** Class by full or short name: "MessagesController", "ChatActivity", "TL_messages_getHistory", "android.view.View". */
    public Class<?> cls(String name) throws Exception { return PluginReflect.cls(name); }

    /** Reads a field. target: instance, Class, or class name (static). Private fields included. */
    public Object get(Object target, String field) throws Exception { return PluginReflect.get(target, field); }

    /** Writes a field (numbers/strings are coerced to the field type). */
    public void set(Object target, String field, Object value) throws Exception { PluginReflect.set(target, field, value); }

    /** Calls any method by name; overload picked by argument count/types. target: instance, Class or class name. */
    public Object call(Object target, String method, Object... args) throws Exception { return PluginReflect.call(target, method, args); }

    /** Creates an object with any constructor: tg.new_object("ChatActivity", bundle). */
    public Object new_object(String className, Object... args) throws Exception { return PluginReflect.make(className, args); }

    // --- Telegram controllers (current account) --------------------------------------------------

    public MessagesController messages() { return MessagesController.getInstance(account()); }
    public org.telegram.messenger.MessagesStorage storage() { return org.telegram.messenger.MessagesStorage.getInstance(account()); }
    public org.telegram.tgnet.ConnectionsManager connections() { return org.telegram.tgnet.ConnectionsManager.getInstance(account()); }
    public UserConfig user_config() { return UserConfig.getInstance(account()); }
    public org.telegram.messenger.NotificationCenter notifications() { return org.telegram.messenger.NotificationCenter.getInstance(account()); }
    public SendMessagesHelper sender() { return SendMessagesHelper.getInstance(account()); }
    public org.telegram.messenger.MediaDataController media_data() { return org.telegram.messenger.MediaDataController.getInstance(account()); }
    public org.telegram.messenger.ContactsController contacts() { return org.telegram.messenger.ContactsController.getInstance(account()); }
    public org.telegram.messenger.FileLoader files() { return org.telegram.messenger.FileLoader.getInstance(account()); }

    public TLRPC.User user(long id) { return messages().getUser(id); }
    public TLRPC.Chat chat(long id) { return messages().getChat(Math.abs(id)); }
    public TLRPC.InputPeer input_peer(long dialogId) { return messages().getInputPeer(dialogId); }

    // --- MTProto: any API method ----------------------------------------------------------------

    /** New TL object by name: tg.tl("TL_messages_getHistory"), tg.tl("inputPeerSelf"). */
    public Object tl(String name) throws Exception {
        return PluginReflect.make(name.startsWith("TL_") || name.contains(".") ? name : "TL_" + name, null);
    }

    /**
     * Sends any TL request on behalf of the current account. callbackFn(response, error_text) gets the
     * parsed response object (null on error). Returns the request id.
     */
    public int send_request(org.telegram.tgnet.TLObject request, String callbackFn) {
        return connections().sendRequest(request, (response, error) -> {
            if (callbackFn != null) {
                PluginManager.getInstance().callFunction(plugin, callbackFn, response, error == null ? null : error.text);
            }
        });
    }

    public void cancel_request(int requestId) { connections().cancelRequest(requestId, true); }

    // --- messages ---------------------------------------------------------------------------------

    public void edit_message(long dialogId, int messageId, String text) {
        TLRPC.TL_messages_editMessage req = new TLRPC.TL_messages_editMessage();
        req.peer = input_peer(dialogId);
        req.id = messageId;
        req.message = text;
        req.flags |= 2048;
        connections().sendRequest(req, (response, error) -> {
            if (response instanceof TLRPC.Updates) messages().processUpdates((TLRPC.Updates) response, false);
        });
    }

    /** Deletes the user's messages (revoke = for everyone). ids: Python list / Lua table of ints. */
    public void delete_messages(long dialogId, java.util.List<?> ids, boolean revoke) {
        ArrayList<Integer> list = new ArrayList<>();
        for (Object o : ids) list.add(((Number) o).intValue());
        AndroidUtilities.runOnUIThread(() -> messages().deleteMessages(list, null, null, dialogId, 0, revoke, 0));
    }

    public void forward_messages(long fromDialogId, java.util.List<?> ids, long toDialogId) {
        TLRPC.TL_messages_forwardMessages req = new TLRPC.TL_messages_forwardMessages();
        req.from_peer = input_peer(fromDialogId);
        req.to_peer = input_peer(toDialogId);
        for (Object o : ids) {
            req.id.add(((Number) o).intValue());
            req.random_id.add(org.telegram.messenger.Utilities.random.nextLong());
        }
        connections().sendRequest(req, (response, error) -> {
            if (response instanceof TLRPC.Updates) messages().processUpdates((TLRPC.Updates) response, false);
        });
    }

    // --- events: any NotificationCenter event ----------------------------------------------------

    private final java.util.HashMap<String, org.telegram.messenger.NotificationCenter.NotificationCenterDelegate> subscriptions = new java.util.HashMap<>();

    private static int eventId(String event) throws Exception {
        return org.telegram.messenger.NotificationCenter.class.getField(event).getInt(null);
    }

    /**
     * Subscribes to any NotificationCenter event by field name ("messagesDeleted", "updateInterfaces",
     * "dialogsNeedReload", …). fn(account, args_list) is called on the UI thread.
     */
    public void subscribe(String event, String fn) throws Exception {
        final int id = eventId(event);
        final String key = event + "\n" + fn;
        if (subscriptions.containsKey(key)) return;
        org.telegram.messenger.NotificationCenter.NotificationCenterDelegate d = (nid, acc, args) ->
            PluginManager.getInstance().callFunction(plugin, fn, acc, java.util.Arrays.asList(args));
        subscriptions.put(key, d);
        AndroidUtilities.runOnUIThread(() -> observe(d, id, true));
    }

    public void unsubscribe(String event, String fn) throws Exception {
        final int id = eventId(event);
        final org.telegram.messenger.NotificationCenter.NotificationCenterDelegate d = subscriptions.remove(event + "\n" + fn);
        if (d != null) AndroidUtilities.runOnUIThread(() -> observe(d, id, false));
    }

    private static void observe(org.telegram.messenger.NotificationCenter.NotificationCenterDelegate d, int id, boolean add) {
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            if (add) org.telegram.messenger.NotificationCenter.getInstance(a).addObserver(d, id);
            else org.telegram.messenger.NotificationCenter.getInstance(a).removeObserver(d, id);
        }
        if (add) org.telegram.messenger.NotificationCenter.getGlobalInstance().addObserver(d, id);
        else org.telegram.messenger.NotificationCenter.getGlobalInstance().removeObserver(d, id);
    }

    /** Posts any event on the current account, e.g. tg.post_event("updateInterfaces", [0]). */
    public void post_event(String event, java.util.List<?> args) throws Exception {
        final int id = eventId(event);
        final Object[] a = args == null ? new Object[0] : args.toArray();
        AndroidUtilities.runOnUIThread(() -> notifications().postNotificationName(id, a));
    }

    /** Called by PluginManager when the plugin stops: drops all event subscriptions. */
    void release() {
        for (java.util.Map.Entry<String, org.telegram.messenger.NotificationCenter.NotificationCenterDelegate> e : subscriptions.entrySet()) {
            try {
                final int id = eventId(e.getKey().substring(0, e.getKey().indexOf('\n')));
                final org.telegram.messenger.NotificationCenter.NotificationCenterDelegate d = e.getValue();
                AndroidUtilities.runOnUIThread(() -> observe(d, id, false));
            } catch (Exception ignore) {}
        }
        subscriptions.clear();
    }

    // --- UI: any screen, any view -------------------------------------------------------------

    /** The fragment on top (ChatActivity, DialogsActivity, ProfileActivity, …). */
    public BaseFragment fragment() { return LaunchActivity.getLastFragment(); }

    public android.view.View root_view() {
        BaseFragment f = fragment();
        return f == null ? null : f.getFragmentView();
    }

    /** All views under root whose class (or a superclass) simple/full name matches, e.g. "ChatMessageCell". */
    public java.util.List<android.view.View> find_views(android.view.View root, String className) {
        ArrayList<android.view.View> out = new ArrayList<>();
        collect(root, className, out);
        return out;
    }

    private static void collect(android.view.View v, String name, ArrayList<android.view.View> out) {
        if (v == null) return;
        for (Class<?> c = v.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            if (c.getSimpleName().equals(name) || c.getName().equals(name)) {
                out.add(v);
                break;
            }
        }
        if (v instanceof android.view.ViewGroup) {
            android.view.ViewGroup g = (android.view.ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) collect(g.getChildAt(i), name, out);
        }
    }

    public void open_chat(long dialogId) {
        AndroidUtilities.runOnUIThread(() -> {
            android.os.Bundle args = new android.os.Bundle();
            if (dialogId > 0) args.putLong("user_id", dialogId); else args.putLong("chat_id", -dialogId);
            BaseFragment f = fragment();
            if (f != null) f.presentFragment(new org.telegram.ui.ChatActivity(args));
        });
    }

    public void present(BaseFragment f) {
        AndroidUtilities.runOnUIThread(() -> {
            BaseFragment top = fragment();
            if (top != null) top.presentFragment(f);
        });
    }

    public void alert(String title, String text) {
        AndroidUtilities.runOnUIThread(() -> {
            BaseFragment f = fragment();
            if (f == null || f.getParentActivity() == null) return;
            f.showDialog(new org.telegram.ui.ActionBar.AlertDialog.Builder(f.getParentActivity())
                .setTitle(title).setMessage(text)
                .setPositiveButton(org.telegram.messenger.LocaleController.getString(org.telegram.messenger.R.string.OK), null).create());
        });
    }

    /** Overrides any theme color by key name without "key_" ("chat_inBubble", "windowBackgroundWhite"), ARGB. */
    public void set_theme_color(String key, long argb) throws Exception {
        final int k = org.telegram.ui.ActionBar.Theme.class.getField("key_" + key).getInt(null);
        AndroidUtilities.runOnUIThread(() -> {
            org.telegram.ui.ActionBar.Theme.setColor(k, (int) argb, false);
            org.telegram.messenger.NotificationCenter.getGlobalInstance().postNotificationName(org.telegram.messenger.NotificationCenter.didSetNewTheme, false, false);
        });
    }

    public int theme_color(String key) throws Exception {
        return org.telegram.ui.ActionBar.Theme.getColor(org.telegram.ui.ActionBar.Theme.class.getField("key_" + key).getInt(null));
    }

    /** Persists SharedConfig after changing its fields with tg.set("SharedConfig", field, value). */
    public void save_config() {
        org.telegram.messenger.SharedConfig.saveConfig();
    }
}

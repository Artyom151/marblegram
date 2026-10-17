/*
 * Marblegram Plugin SDK — loads, installs and dispatches events to plugins.
 *
 * Plugins live in files/marble_plugins/<id>/ (plugin.json + scripts + assets).
 * Hooks (all optional top-level functions in the plugin script):
 *   on_load()                                   — after the script ran
 *   on_unload()                                 — before the plugin is disabled/removed
 *   on_send_message(dialog_id, text) -> text    — return a new text, false/"" to cancel sending, nil to keep it
 *   on_message(dialog_id, text, out)            — every new incoming/outgoing message
 *   on_chat_open(dialog_id)                     — a chat is opened
 *   on_app_start()                              — app process started (after all plugins loaded)
 */

package org.telegram.marble.plugins;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.UserConfig;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class PluginManager implements NotificationCenter.NotificationCenterDelegate {

    private static volatile PluginManager instance;

    public static PluginManager getInstance() {
        if (instance == null) {
            synchronized (PluginManager.class) {
                if (instance == null) {
                    instance = new PluginManager();
                }
            }
        }
        return instance;
    }

    private final ArrayList<Plugin> plugins = new ArrayList<>();
    private boolean initialized;
    volatile boolean suppressHooks;

    public File root() {
        File dir = new File(ApplicationLoader.applicationContext.getFilesDir(), "marble_plugins");
        dir.mkdirs();
        return dir;
    }

    private SharedPreferences prefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences("marble_plugins", Context.MODE_PRIVATE);
    }

    public ArrayList<Plugin> getPlugins() {
        return plugins;
    }

    public Plugin find(String id) {
        for (Plugin p : plugins) {
            if (p.manifest.id.equals(id)) return p;
        }
        return null;
    }

    /** Called once from LaunchActivity.onCreate. */
    public void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        installBundledPlugins();
        File[] dirs = root().listFiles();
        if (dirs != null) {
            for (File dir : dirs) {
                Plugin plugin = readPlugin(dir);
                if (plugin != null) {
                    plugins.add(plugin);
                    if (plugin.enabled) {
                        start(plugin);
                    }
                }
            }
        }
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            NotificationCenter.getInstance(a).addObserver(this, NotificationCenter.didReceiveNewMessages);
        }
        dispatch("on_app_start");
    }

    /** Built-in plugins shipped in assets/marble_plugins/<id>/; installed + enabled once, never re-added after removal. */
    private void installBundledPlugins() {
        try {
            android.content.res.AssetManager assets = ApplicationLoader.applicationContext.getAssets();
            String[] ids = assets.list("marble_plugins");
            if (ids == null) return;
            for (String id : ids) {
                if (prefs().getBoolean("bundled_" + id, false)) continue;
                File target = new File(root(), id);
                if (!target.exists()) {
                    target.mkdirs();
                    String[] files = assets.list("marble_plugins/" + id);
                    if (files == null) continue;
                    for (String f : files) {
                        try (InputStream in = assets.open("marble_plugins/" + id + "/" + f);
                             FileOutputStream out = new FileOutputStream(new File(target, f))) {
                            byte[] buf = new byte[8192];
                            int n;
                            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                        }
                    }
                    prefs().edit().putBoolean("enabled_" + id, true).apply();
                }
                prefs().edit().putBoolean("bundled_" + id, true).apply();
            }
        } catch (Exception e) {
            Log.e("MarblePlugin", "bundled plugins", e);
        }
    }

    private Plugin readPlugin(File dir) {
        try {
            File json = new File(dir, "plugin.json");
            if (!json.exists()) {
                return null;
            }
            PluginManifest manifest = PluginManifest.parse(readText(new java.io.FileInputStream(json)));
            Plugin plugin = new Plugin(manifest, dir);
            plugin.enabled = prefs().getBoolean("enabled_" + manifest.id, false);
            return plugin;
        } catch (Exception e) {
            Log.e("MarblePlugin", "bad plugin in " + dir, e);
            return null;
        }
    }

    private static String readText(InputStream in) throws Exception {
        try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line).append('\n');
            return sb.toString();
        }
    }

    private PluginEngine createEngine(Plugin plugin) {
        if (PluginManifest.LANG_PYTHON.equals(plugin.manifest.language)) {
            return new PythonPluginEngine();
        }
        return new LuaPluginEngine();
    }

    private void start(Plugin plugin) {
        plugin.error = null;
        if (plugin.manifest.minSdk > PluginApi.SDK_VERSION) {
            plugin.error = "needs SDK " + plugin.manifest.minSdk + ", this client has " + PluginApi.SDK_VERSION;
            return;
        }
        try {
            plugin.api = new PluginApi(plugin);
            PluginEngine engine = createEngine(plugin);
            engine.load(plugin, plugin.api);
            plugin.engine = engine;
            safeCall(plugin, "on_load");
        } catch (Throwable e) {
            plugin.engine = null;
            plugin.error = String.valueOf(e.getMessage() != null ? e.getMessage() : e);
            Log.e("MarblePlugin", "failed to start " + plugin.manifest.id, e);
        }
    }

    private void stop(Plugin plugin) {
        if (plugin.engine != null) {
            safeCall(plugin, "on_unload");
            if (plugin.api != null) plugin.api.release();
            plugin.engine.unload();
            plugin.engine = null;
        }
    }

    public void setEnabled(Plugin plugin, boolean enabled) {
        plugin.enabled = enabled;
        prefs().edit().putBoolean("enabled_" + plugin.manifest.id, enabled).apply();
        if (enabled) {
            start(plugin);
        } else {
            stop(plugin);
        }
    }

    public void reload(Plugin plugin) {
        stop(plugin);
        if (plugin.enabled) {
            start(plugin);
        }
    }

    public void uninstall(Plugin plugin) {
        stop(plugin);
        plugins.remove(plugin);
        prefs().edit().remove("enabled_" + plugin.manifest.id).apply();
        deleteRecursive(plugin.dir);
    }

    /** Installs (or updates) a plugin from a .mgplugin zip; returns the plugin. */
    public Plugin install(Uri uri) throws Exception {
        File tmp = new File(root(), ".installing");
        deleteRecursive(tmp);
        tmp.mkdirs();
        try (InputStream raw = ApplicationLoader.applicationContext.getContentResolver().openInputStream(uri);
             ZipInputStream zip = new ZipInputStream(raw)) {
            ZipEntry entry;
            byte[] buffer = new byte[16384];
            while ((entry = zip.getNextEntry()) != null) {
                File out = new File(tmp, entry.getName());
                if (!out.getCanonicalPath().startsWith(tmp.getCanonicalPath() + File.separator)) {
                    throw new SecurityException("zip entry outside of plugin folder: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    out.mkdirs();
                    continue;
                }
                out.getParentFile().mkdirs();
                try (FileOutputStream fos = new FileOutputStream(out)) {
                    int n;
                    while ((n = zip.read(buffer)) > 0) fos.write(buffer, 0, n);
                }
            }
        }
        // allow a single top-level folder inside the zip
        File manifestDir = tmp;
        if (!new File(tmp, "plugin.json").exists()) {
            File[] children = tmp.listFiles();
            if (children != null && children.length == 1 && children[0].isDirectory()) {
                manifestDir = children[0];
            }
        }
        Plugin parsed = readPlugin(manifestDir);
        if (parsed == null) {
            deleteRecursive(tmp);
            throw new IllegalArgumentException("plugin.json not found or invalid");
        }
        Plugin old = find(parsed.manifest.id);
        boolean wasEnabled = old != null && old.enabled;
        if (old != null) {
            stop(old);
            plugins.remove(old);
            deleteRecursive(old.dir);
        }
        File target = new File(root(), parsed.manifest.id);
        deleteRecursive(target);
        if (!manifestDir.renameTo(target)) {
            throw new IllegalStateException("cannot move plugin into place");
        }
        deleteRecursive(tmp);
        Plugin plugin = readPlugin(target);
        plugins.add(plugin);
        if (wasEnabled) {
            setEnabled(plugin, true);
        }
        return plugin;
    }

    private static void deleteRecursive(File f) {
        if (f == null || !f.exists()) return;
        File[] children = f.listFiles();
        if (children != null) for (File c : children) deleteRecursive(c);
        f.delete();
    }

    // --- dispatch ------------------------------------------------------------------------------

    private Object safeCall(Plugin plugin, String fn, Object... args) {
        if (plugin.engine == null || !plugin.engine.hasFunction(fn)) {
            return null;
        }
        try {
            return plugin.engine.call(fn, args);
        } catch (Throwable e) {
            plugin.error = fn + ": " + (e.getMessage() != null ? e.getMessage() : e);
            Log.e("MarblePlugin", plugin.manifest.id + "." + fn, e);
            return null;
        }
    }

    public void dispatch(String fn, Object... args) {
        for (int i = 0; i < plugins.size(); i++) {
            safeCall(plugins.get(i), fn, args);
        }
    }

    public void callAction(Plugin plugin, String function) {
        safeCall(plugin, function);
    }

    public void callFunction(Plugin plugin, String function, Object... args) {
        safeCall(plugin, function, args);
    }

    /**
     * Hook from SendMessagesHelper.sendMessage. Returns the (possibly changed) text,
     * or null if a plugin cancelled sending.
     */
    public String onSendMessage(long dialogId, String text) {
        if (suppressHooks || plugins.isEmpty() || text == null) {
            return text;
        }
        String current = text;
        for (int i = 0; i < plugins.size(); i++) {
            Object r = safeCall(plugins.get(i), "on_send_message", dialogId, current);
            if (r == null) continue;
            if (Boolean.FALSE.equals(r) || "".equals(r)) {
                return null;
            }
            current = String.valueOf(r);
        }
        return current;
    }

    /** Hook from BaseFragment.onResume: on_fragment_resume(fragment, class_name) — plugins can edit any screen. */
    public void onFragmentResume(Object fragment) {
        if (!plugins.isEmpty()) {
            dispatch("on_fragment_resume", fragment, fragment.getClass().getSimpleName());
        }
    }

    /**
     * Hook from ConnectionsManager.sendRequestInternal (any thread): on_request(request, class_name).
     * Plugins may mutate the request in place; returning False drops it. Returns false to drop.
     */
    public boolean onRequest(Object request) {
        if (plugins.isEmpty() || request == null) {
            return true;
        }
        for (int i = 0; i < plugins.size(); i++) {
            if (Boolean.FALSE.equals(safeCall(plugins.get(i), "on_request", request, request.getClass().getSimpleName()))) {
                return false;
            }
        }
        return true;
    }

    /** Hook from MessagesController.processUpdates: on_update(updates, class_name) for every server update batch. */
    public void onUpdate(Object updates) {
        if (!plugins.isEmpty() && updates != null) {
            dispatch("on_update", updates, updates.getClass().getSimpleName());
        }
    }

    public void onChatOpen(long dialogId) {
        if (!plugins.isEmpty()) {
            dispatch("on_chat_open", dialogId);
        }
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id != NotificationCenter.didReceiveNewMessages || plugins.isEmpty()) {
            return;
        }
        if (args.length > 2 && args[2] instanceof Boolean && (Boolean) args[2]) {
            return; // scheduled
        }
        long dialogId = (Long) args[0];
        @SuppressWarnings("unchecked")
        ArrayList<MessageObject> messages = (ArrayList<MessageObject>) args[1];
        for (MessageObject m : messages) {
            String text = m.messageText != null ? m.messageText.toString() : "";
            dispatch("on_message", dialogId, text, m.isOut());
        }
    }
}

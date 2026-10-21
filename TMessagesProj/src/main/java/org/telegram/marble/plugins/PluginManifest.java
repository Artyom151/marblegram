/*
 * Marblegram Plugin SDK — plugin.json manifest.
 */

package org.telegram.marble.plugins;

import org.json.JSONObject;

public class PluginManifest {

    public static final String LANG_LUA = "lua";
    public static final String LANG_PYTHON = "python";

    /** Unique id: [a-z0-9_.-], also the install folder name. */
    public String id;
    public String name;
    public String author;
    public String version = "1.0";
    public String description = "";
    /** Path (inside the plugin) of a square PNG/JPG avatar, optional. */
    public String icon;
    /** Path of a Lottie JSON animation shown instead of the avatar, optional. */
    public String animation;
    /** "lua" or "python". */
    public String language = LANG_LUA;
    /** Entry file, defaults to main.lua / main.py. */
    public String main;
    /** Minimal SDK level the plugin needs. */
    public int minSdk = 1;

    public static PluginManifest parse(String json) throws Exception {
        JSONObject o = new JSONObject(json);
        PluginManifest m = new PluginManifest();
        m.id = o.getString("id");
        if (!m.id.matches("[A-Za-z0-9_.\\-]{1,64}")) {
            throw new IllegalArgumentException("bad plugin id: " + m.id);
        }
        m.name = o.optString("name", m.id);
        m.author = o.optString("author", "");
        m.version = o.optString("version", "1.0");
        m.description = o.optString("description", "");
        m.icon = o.optString("icon", null);
        m.animation = o.optString("animation", null);
        m.language = o.optString("language", LANG_LUA).toLowerCase();
        m.main = o.optString("main", LANG_PYTHON.equals(m.language) ? "main.py" : "main.lua");
        m.minSdk = o.optInt("min_sdk", 1);
        return m;
    }
}

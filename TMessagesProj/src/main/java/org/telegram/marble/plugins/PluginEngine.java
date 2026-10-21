/*
 * Marblegram Plugin SDK — a scripting runtime (Lua, Python).
 */

package org.telegram.marble.plugins;

public interface PluginEngine {

    /** Executes the plugin's entry file with the {@code tg} API object in scope. */
    void load(Plugin plugin, PluginApi api) throws Exception;

    /** True if the script defines a top-level function with this name. */
    boolean hasFunction(String name);

    /** Calls a top-level function; returns its result converted to a Java value (String/Boolean/Number/null). */
    Object call(String name, Object... args) throws Exception;

    void unload();
}

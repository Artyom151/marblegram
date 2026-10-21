/*
 * Marblegram Plugin SDK — an installed plugin.
 */

package org.telegram.marble.plugins;

import java.io.File;

public class Plugin {

    public final PluginManifest manifest;
    public final File dir;
    public boolean enabled;
    public PluginEngine engine;
    public PluginApi api;
    /** Last load/runtime error, shown in the plugins screen. */
    public String error;

    Plugin(PluginManifest manifest, File dir) {
        this.manifest = manifest;
        this.dir = dir;
    }

    public File file(String relative) {
        return relative == null ? null : new File(dir, relative);
    }

    public boolean isRunning() {
        return engine != null;
    }
}

/*
 * Marblegram Plugin SDK — Lua 5.2 runtime (LuaJ, pure Java).
 * Plugins get `tg` (PluginApi) and the full `luajava` bridge to the Telegram/Android Java classes.
 */

package org.telegram.marble.plugins;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.luaj.vm2.lib.jse.CoerceLuaToJava;
import org.luaj.vm2.lib.jse.JsePlatform;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class LuaPluginEngine implements PluginEngine {

    private Globals globals;

    @Override
    public void load(Plugin plugin, PluginApi api) throws Exception {
        globals = JsePlatform.standardGlobals();
        globals.set("tg", CoerceJavaToLua.coerce(api));
        // let `require "foo"` find foo.lua next to main.lua
        globals.get("package").set("path", LuaValue.valueOf(plugin.dir.getAbsolutePath() + "/?.lua"));
        File main = plugin.file(plugin.manifest.main);
        try (InputStream in = new FileInputStream(main)) {
            globals.load(in, "@" + main.getName(), "t", globals).call();
        }
    }

    @Override
    public boolean hasFunction(String name) {
        return globals != null && globals.get(name).isfunction();
    }

    @Override
    public Object call(String name, Object... args) {
        LuaValue fn = globals.get(name);
        if (!fn.isfunction()) {
            return null;
        }
        LuaValue[] luaArgs = new LuaValue[args.length];
        for (int i = 0; i < args.length; i++) {
            luaArgs[i] = CoerceJavaToLua.coerce(args[i]);
        }
        Varargs result = fn.invoke(LuaValue.varargsOf(luaArgs));
        LuaValue v = result.arg1();
        if (v.isnil()) return null;
        if (v.isboolean()) return v.toboolean();
        if (v.isnumber()) return v.todouble();
        if (v.isstring()) return v.tojstring();
        return CoerceLuaToJava.coerce(v, Object.class);
    }

    @Override
    public void unload() {
        globals = null;
    }
}

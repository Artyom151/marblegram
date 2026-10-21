/*
 * Marblegram Plugin SDK — Python 3 runtime (Chaquopy).
 * Accessed through reflection so the core SDK compiles without Chaquopy; the app module bundles it.
 * Plugins get `tg` (PluginApi) and the Chaquopy `java` bridge (from java import jclass).
 */

package org.telegram.marble.plugins;

import org.telegram.messenger.ApplicationLoader;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class PythonPluginEngine implements PluginEngine {

    private Object python;      // com.chaquo.python.Python
    private Object globals;     // PyObject dict

    public static boolean isAvailable() {
        try {
            Class.forName("com.chaquo.python.Python");
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    private static Object invoke(Object target, String method, Object... args) throws Exception {
        for (Method m : target.getClass().getMethods()) {
            if (m.getName().equals(method) && (m.isVarArgs() || m.getParameterTypes().length == args.length)) {
                if (m.isVarArgs()) {
                    Class<?> varType = m.getParameterTypes()[m.getParameterTypes().length - 1].getComponentType();
                    int fixed = m.getParameterTypes().length - 1;
                    Object[] real = new Object[fixed + 1];
                    System.arraycopy(args, 0, real, 0, fixed);
                    Object var = java.lang.reflect.Array.newInstance(varType, args.length - fixed);
                    for (int i = fixed; i < args.length; i++) java.lang.reflect.Array.set(var, i - fixed, args[i]);
                    real[fixed] = var;
                    return m.invoke(target, real);
                }
                return m.invoke(target, args);
            }
        }
        throw new NoSuchMethodException(method);
    }

    private static void ensureStarted() throws Exception {
        Class<?> py = Class.forName("com.chaquo.python.Python");
        if (!(Boolean) py.getMethod("isStarted").invoke(null)) {
            Class<?> platform = Class.forName("com.chaquo.python.android.AndroidPlatform");
            Object p = platform.getConstructor(android.content.Context.class).newInstance(ApplicationLoader.applicationContext);
            py.getMethod("start", Class.forName("com.chaquo.python.Python$Platform")).invoke(null, p);
        }
    }

    @Override
    public void load(Plugin plugin, PluginApi api) throws Exception {
        if (!isAvailable()) {
            throw new IllegalStateException("Python runtime is not bundled in this build");
        }
        ensureStarted();
        python = Class.forName("com.chaquo.python.Python").getMethod("getInstance").invoke(null);
        Object builtins = invoke(python, "getBuiltins");
        globals = invoke(builtins, "callAttr", "dict");
        invoke(globals, "callAttr", "__setitem__", "tg", api);
        invoke(globals, "callAttr", "__setitem__", "__name__", "__marble_plugin__");
        invoke(globals, "callAttr", "__setitem__", "__file__", plugin.file(plugin.manifest.main).getAbsolutePath());
        Object sys = invoke(python, "getModule", "sys");
        Object path = invoke(sys, "get", "path");
        invoke(path, "callAttr", "insert", 0, plugin.dir.getAbsolutePath());
        File main = plugin.file(plugin.manifest.main);
        String code = new String(Files.readAllBytes(main.toPath()), StandardCharsets.UTF_8);
        Object compiled = invoke(builtins, "callAttr", "compile", code, main.getAbsolutePath(), "exec");
        invoke(builtins, "callAttr", "exec", compiled, globals);
    }

    private Object function(String name) throws Exception {
        Object fn = invoke(globals, "callAttr", "get", name);
        if (fn == null) return null;
        Object callable = invoke(invoke(python, "getBuiltins"), "callAttr", "callable", fn);
        return (Boolean) invoke(callable, "toBoolean") ? fn : null;
    }

    @Override
    public boolean hasFunction(String name) {
        try {
            return globals != null && function(name) != null;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Object call(String name, Object... args) throws Exception {
        Object fn = function(name);
        if (fn == null) {
            return null;
        }
        Object result = invoke(fn, "call", args);
        if (result == null) return null;
        Object type = invoke(invoke(invoke(python, "getBuiltins"), "callAttr", "type", result), "callAttr", "__getattribute__", "__name__");
        String t = String.valueOf(type);
        switch (t) {
            case "NoneType": return null;
            case "bool": return invoke(result, "toBoolean");
            case "int": return invoke(result, "toLong");
            case "float": return invoke(result, "toDouble");
            case "str": return result.toString();
            default: return invoke(result, "toJava", Object.class);
        }
    }

    @Override
    public void unload() {
        globals = null;
    }
}

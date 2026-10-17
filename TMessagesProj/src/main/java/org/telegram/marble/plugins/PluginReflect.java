/*
 * Marblegram Plugin SDK — reflection bridge used by tg.cls/get/set/call/new.
 * Reaches private fields/methods of any Telegram or Android class and coerces script values
 * (Python int → Long, Lua number → Integer/Double, …) to the Java parameter types.
 */

package org.telegram.marble.plugins;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;

final class PluginReflect {

    /** Packages searched for short class names, in order. */
    private static final String[] PACKAGES = {
        "", "org.telegram.messenger.", "org.telegram.ui.", "org.telegram.ui.ActionBar.", "org.telegram.ui.Components.",
        "org.telegram.ui.Cells.", "org.telegram.ui.Stories.", "org.telegram.tgnet.", "org.telegram.tgnet.TLRPC$",
        "org.telegram.tgnet.tl.", "org.telegram.marble.", "org.telegram.marble.features.",
        "android.view.", "android.widget.", "android.graphics.", "java.util."
    };
    private static final HashMap<String, Class<?>> cache = new HashMap<>();

    private PluginReflect() {}

    static Class<?> cls(String name) throws ClassNotFoundException {
        synchronized (cache) {
            Class<?> c = cache.get(name);
            if (c != null) return c;
            for (String p : PACKAGES) {
                try {
                    c = Class.forName(p + name);
                    cache.put(name, c);
                    return c;
                } catch (ClassNotFoundException ignore) {}
            }
        }
        throw new ClassNotFoundException(name);
    }

    static Field field(Class<?> c, String name) throws NoSuchFieldException {
        for (Class<?> k = c; k != null; k = k.getSuperclass()) {
            try {
                Field f = k.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignore) {}
        }
        throw new NoSuchFieldException(c.getName() + "." + name);
    }

    /** target may be an instance, a Class (static) or a class name string (static). */
    static Object get(Object target, String name) throws Exception {
        Class<?> c = classOf(target);
        Field f = field(c, name);
        return f.get(Modifier.isStatic(f.getModifiers()) ? null : target);
    }

    static void set(Object target, String name, Object value) throws Exception {
        Class<?> c = classOf(target);
        Field f = field(c, name);
        f.set(Modifier.isStatic(f.getModifiers()) ? null : target, coerce(value, f.getType()));
    }

    static Object call(Object target, String name, Object[] args) throws Exception {
        if (args == null) args = new Object[0];
        Class<?> c = classOf(target);
        for (Class<?> k = c; k != null; k = k.getSuperclass()) {
            for (Method m : k.getDeclaredMethods()) {
                if (!m.getName().equals(name) || m.getParameterTypes().length != args.length) continue;
                Object[] real = coerceAll(args, m.getParameterTypes());
                if (real == null) continue;
                m.setAccessible(true);
                return m.invoke(Modifier.isStatic(m.getModifiers()) ? null : target, real);
            }
        }
        throw new NoSuchMethodException(c.getName() + "." + name + "/" + args.length);
    }

    static Object make(String className, Object[] args) throws Exception {
        if (args == null) args = new Object[0];
        Class<?> c = cls(className);
        for (Constructor<?> k : c.getDeclaredConstructors()) {
            if (k.getParameterTypes().length != args.length) continue;
            Object[] real = coerceAll(args, k.getParameterTypes());
            if (real == null) continue;
            k.setAccessible(true);
            return k.newInstance(real);
        }
        throw new NoSuchMethodException(className + ".<init>/" + args.length);
    }

    private static Class<?> classOf(Object target) throws ClassNotFoundException {
        if (target instanceof Class) return (Class<?>) target;
        if (target instanceof String) return cls((String) target);
        if (target == null) throw new NullPointerException("target is null");
        return target.getClass();
    }

    private static Object[] coerceAll(Object[] args, Class<?>[] types) {
        Object[] out = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            Object v = coerce(args[i], types[i]);
            if (v == NO) return null;
            out[i] = v;
        }
        return out;
    }

    private static final Object NO = new Object();

    /** Converts a script value to {@code type}; returns NO when impossible. */
    static Object coerce(Object v, Class<?> type) {
        if (v == null) return type.isPrimitive() ? NO : null;
        if (type.isInstance(v)) return v;
        if (v instanceof Number) {
            Number n = (Number) v;
            if (type == int.class || type == Integer.class) return n.intValue();
            if (type == long.class || type == Long.class) return n.longValue();
            if (type == float.class || type == Float.class) return n.floatValue();
            if (type == double.class || type == Double.class) return n.doubleValue();
            if (type == short.class || type == Short.class) return n.shortValue();
            if (type == byte.class || type == Byte.class) return n.byteValue();
            if (type == boolean.class || type == Boolean.class) return n.intValue() != 0;
        }
        if (v instanceof Boolean && (type == boolean.class)) return v;
        if (type == String.class || type == CharSequence.class) return String.valueOf(v);
        if (type == char.class && v instanceof String && ((String) v).length() == 1) return ((String) v).charAt(0);
        if (type.isArray() && v instanceof java.util.List) {
            java.util.List<?> list = (java.util.List<?>) v;
            Object arr = Array.newInstance(type.getComponentType(), list.size());
            for (int i = 0; i < list.size(); i++) {
                Object e = coerce(list.get(i), type.getComponentType());
                if (e == NO) return NO;
                Array.set(arr, i, e);
            }
            return arr;
        }
        if (type == java.util.ArrayList.class && v instanceof java.util.List) return new java.util.ArrayList<>((java.util.List<?>) v);
        return NO;
    }
}

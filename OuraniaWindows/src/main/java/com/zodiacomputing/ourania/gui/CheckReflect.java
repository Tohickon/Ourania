package com.zodiacomputing.ourania.gui;

import java.lang.reflect.Field;

/**
 * Field access for the check suites, by name - including a dotted path such as
 * {@code "natalRing.time"}.
 *
 * <p><b>Nine suites carried their own copy of the same four lines</b>, each looking a field up
 * on one class by one name. When SkymapPanel's ring data moved into {@link WheelRing} (J13,
 * 2026-09-19), about 180 lookups by name - {@code set(sky, "baseChartTime", ...)} - pointed at
 * fields that no longer existed, and every copy would have needed teaching the same new trick.
 * They all call this now.
 *
 * <p>A name is looked for on the object's class and then up its superclasses, and each segment
 * of a path is followed through the object the previous one held.
 */
public final class CheckReflect {

    private CheckReflect() { }

    public static Object get(Object target, String path) throws Exception {
        Object holder = holder(target, path);
        return field(holder.getClass(), last(path)).get(holder);
    }

    public static void set(Object target, String path, Object value) throws Exception {
        Object holder = holder(target, path);
        field(holder.getClass(), last(path)).set(holder, value);
    }

    /** The field itself, made accessible, found on cls or the nearest superclass declaring it. */
    public static Field field(Class<?> cls, String name) throws NoSuchFieldException {
        for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException notHere) {
                // Keep climbing.
            }
        }
        throw new NoSuchFieldException(name + " on " + cls.getName() + " or its superclasses");
    }

    private static Object holder(Object target, String path) throws Exception {
        String[] segments = path.split("\\.");
        Object h = target;
        for (int i = 0; i < segments.length - 1; i++) {
            h = field(h.getClass(), segments[i]).get(h);
        }
        return h;
    }

    private static String last(String path) {
        int dot = path.lastIndexOf('.');
        return dot < 0 ? path : path.substring(dot + 1);
    }
}

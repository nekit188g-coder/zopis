package com.nameprotect;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.class_10799;
import net.minecraft.class_1297;
import net.minecraft.class_1304;
import net.minecraft.class_1309;
import net.minecraft.class_1799;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_634;
import net.minecraft.class_640;

/**
 * ВСІ виклики Minecraft API, яких не було в оригінальному моді, зібрані тут.
 * Якщо при збірці якесь ім'я не знайдеться — правити треба лише цей файл (позначено VERIFY).
 */
public final class Compat {
    private Compat() {}

    // ---------------------------------------------------------------- текст
    /** VERIFY: class_310.field_1772 = font, class_327.method_1727 = width(String) */
    public static int textWidth(String s) {
        return class_310.method_1551().field_1772.method_1727(s);
    }

    /** VERIFY: class_332.method_51433 = drawString(Font, String, x, y, color, shadow) */
    public static void text(class_332 g, String s, int x, int y, int color) {
        g.method_51433(class_310.method_1551().field_1772, s, x, y, color, false);
    }

    // ---------------------------------------------------------------- FPS
    private static int frames;
    private static long fpsWindowStart = System.nanoTime();
    private static int fps;

    /** Викликається раз на кадр з HUD-рендера. */
    public static void frame() {
        frames++;
        long now = System.nanoTime();
        if (now - fpsWindowStart >= 500_000_000L) {
            fps = Math.round(frames * 1.0e9f / (now - fpsWindowStart));
            frames = 0;
            fpsWindowStart = now;
        }
    }

    public static int fps() {
        return fps;
    }

    // ---------------------------------------------------------------- пінг
    /** VERIFY: class_310.method_1562 = getConnection, class_634.method_2874 = getPlayerInfo(UUID), class_640.method_2959 = getLatency */
    public static int ping() {
        try {
            class_310 mc = class_310.method_1551();
            if (mc.field_1724 == null) return -1;
            class_634 conn = mc.method_1562();
            if (conn == null) return -1;
            class_640 info = conn.method_2874(mc.field_1724.method_5667());
            return info == null ? -1 : info.method_2959();
        } catch (Throwable t) {
            return -1;
        }
    }

    // ---------------------------------------------------------------- здоров'я / броня
    /** VERIFY: method_6032 = getHealth, method_6063 = getMaxHealth, method_6067 = getAbsorptionAmount */
    public static float health(class_1309 e) {
        return e.method_6032();
    }

    public static float maxHealth(class_1309 e) {
        return Math.max(1f, e.method_6063());
    }

    public static float absorption(class_1309 e) {
        return e.method_6067();
    }

    /** slot = "MAINHAND" / "HEAD" / "CHEST" / "LEGS" / "FEET". VERIFY: class_1309.method_6118 = getItemBySlot */
    public static class_1799 stackIn(class_1309 e, String slot) {
        for (class_1304 s : class_1304.values()) {
            if (s.name().equals(slot)) return e.method_6118(s);
        }
        return null;
    }

    /** VERIFY: class_1799.method_7960 = isEmpty */
    public static boolean isEmpty(class_1799 s) {
        return s == null || s.method_7960();
    }

    /** VERIFY: class_332.method_51427 = renderItem(ItemStack, x, y) */
    public static void item(class_332 g, class_1799 stack, int x, int y, float scale) {
        var m = g.method_51448();
        m.pushMatrix();
        m.translate((float) x, (float) y);
        m.scale(scale);
        g.method_51427(stack, 0, 0);
        m.popMatrix();
    }

    // ---------------------------------------------------------------- голова гравця
    private static final Map<Class<?>, List<Method>> ACCESSORS = new HashMap<>();
    private static final Map<Class<?>, Method> SKIN_GETTER = new HashMap<>();
    private static final Method NONE;

    static {
        Method none = null;
        try {
            none = Object.class.getMethod("toString");
        } catch (Exception ignored) {
        }
        NONE = none;
    }

    /** Шукає геттер скіна (PlayerSkin = class_8685, getSkin = method_52814) через reflection — без жорстких залежностей. */
    private static Object skinOf(class_1297 e) {
        try {
            Class<?> k = e.getClass();
            Method m = SKIN_GETTER.get(k);
            if (m == null) {
                m = NONE;
                for (Method c : k.getMethods()) {
                    if (c.getParameterCount() == 0 && !Modifier.isStatic(c.getModifiers())
                            && (c.getReturnType().getName().equals("net.minecraft.class_8685") || c.getName().equals("method_52814"))) {
                        m = c;
                        break;
                    }
                }
                SKIN_GETTER.put(k, m);
            }
            return m == NONE ? null : m.invoke(e);
        } catch (Throwable t) {
            return null;
        }
    }

    private static List<Method> accessors(Class<?> k) {
        return ACCESSORS.computeIfAbsent(k, kk -> {
            List<Method> out = new ArrayList<>();
            try {
                if (kk.isRecord()) {
                    for (RecordComponent rc : kk.getRecordComponents()) {
                        Method a = rc.getAccessor();
                        a.setAccessible(true);
                        out.add(a);
                    }
                } else {
                    for (Method c : kk.getMethods()) {
                        if (c.getParameterCount() == 0 && !Modifier.isStatic(c.getModifiers())
                                && class_2960.class.isAssignableFrom(c.getReturnType())) {
                            out.add(c);
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
            return out;
        });
    }

    private static boolean isTexturePath(class_2960 id) {
        String s = id.toString();
        return s.contains("textures/") || s.contains("skins/");
    }

    private static class_2960 findTexture(Object o, int depth) {
        if (o == null || depth > 3) return null;
        if (o instanceof class_2960 id) return isTexturePath(id) ? id : null;
        try {
            for (Method m : accessors(o.getClass())) {
                Class<?> rt = m.getReturnType();
                if (rt.isPrimitive() || rt == String.class || rt.isEnum() || rt == Boolean.class) continue;
                class_2960 r = findTexture(m.invoke(o), depth + 1);
                if (r != null) return r;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    public static class_2960 skinTexture(class_1297 e) {
        return e == null ? null : findTexture(skinOf(e), 0);
    }

    public static void head(class_332 g, class_1297 e, int x, int y, int size) {
        class_2960 tex = skinTexture(e);
        if (tex == null) {
            Ui.rrect(g, x, y, size, size, 3, 0xFF2A2A3A);
            String s = "?";
            if (e != null) {
                String n = e.method_5477().getString();
                if (!n.isEmpty()) s = n.substring(0, 1).toUpperCase();
            }
            Ui.textC(g, s, x + size / 2, y + (size - Ui.fontH()) / 2 + 1, Ui.MID);
            return;
        }
        var m = g.method_51448();
        m.pushMatrix();
        m.translate((float) x, (float) y);
        m.scale(size / 8f);
        g.method_25290(class_10799.field_56883, tex, 0, 0, 8f, 8f, 8, 8, 64, 64);
        g.method_25290(class_10799.field_56883, tex, 0, 0, 40f, 8f, 8, 8, 64, 64);
        m.popMatrix();
    }

    // ---------------------------------------------------------------- динамічна текстура (обкладинка)
    private static final class_2960 COVER_ID = class_2960.method_60655("nameprotect", "media_cover");
    private static boolean coverWarned;

    /**
     * PNG -> NativeImage (class_1011) -> DynamicTexture (class_1043) -> TextureManager.register.
     * Усе через reflection, щоб збірка не залежала від точних імен. Повертає null, якщо не вдалося.
     */
    public static class_2960 uploadCover(byte[] png) {
        try {
            Class<?> ni = Class.forName("net.minecraft.class_1011");
            Object img = null;
            for (Method m : ni.getMethods()) {
                if (Modifier.isStatic(m.getModifiers()) && m.getParameterCount() == 1
                        && m.getParameterTypes()[0] == InputStream.class && m.getReturnType() == ni) {
                    img = m.invoke(null, new ByteArrayInputStream(png));
                    break;
                }
            }
            if (img == null) throw new IllegalStateException("NativeImage.read(InputStream) not found");

            Class<?> dt = Class.forName("net.minecraft.class_1043");
            Object tex = null;
            for (Constructor<?> c : dt.getConstructors()) {
                Class<?>[] p = c.getParameterTypes();
                if (p.length == 1 && p[0] == ni) {
                    tex = c.newInstance(img);
                } else if (p.length == 2 && p[0] == Supplier.class && p[1] == ni) {
                    Supplier<String> label = () -> "nameprotect_cover";
                    tex = c.newInstance(label, img);
                }
                if (tex != null) break;
            }
            if (tex == null) throw new IllegalStateException("DynamicTexture constructor not found");

            class_310 mc = class_310.method_1551();
            Object tm = null;
            for (Method m : mc.getClass().getMethods()) {
                if (m.getParameterCount() == 0 && (m.getReturnType().getName().equals("net.minecraft.class_1060")
                        || m.getName().equals("method_1531"))) {
                    tm = m.invoke(mc);
                    break;
                }
            }
            if (tm == null) throw new IllegalStateException("TextureManager not found");
            for (Method m : tm.getClass().getMethods()) {
                Class<?>[] p = m.getParameterTypes();
                if (p.length == 2 && p[0] == class_2960.class && p[1].isInstance(tex) && m.getReturnType() == void.class) {
                    m.invoke(tm, COVER_ID, tex);
                    return COVER_ID;
                }
            }
            throw new IllegalStateException("TextureManager.register not found");
        } catch (Throwable t) {
            if (!coverWarned) {
                coverWarned = true;
                System.err.println("[nameprotect] cover texture failed: " + t);
            }
            return null;
        }
    }
}

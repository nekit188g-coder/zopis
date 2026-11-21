package com.nameprotect;

import net.minecraft.class_10799;
import net.minecraft.class_1297;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_332;

/** TargetESP: картинка поверх цілі (моб/гравець), на яку дивишся або яку вдарив. */
public final class TargetEsp {
    private static final String[] NAMES = {"radar", "bruh", "circle", "what", "triad", "fsociety", "photo", "sardelki"};
    private static final boolean[] SPINNABLE = {true, true, true, true, true, true, false, false};
    private static final float[] SIZE = {0.7f, 1.0f, 1.4f};
    private static final class_2960[] TEX = new class_2960[NAMES.length];
    private static final long HOLD_MS = 2000;

    private static class_1297 lastTarget;

    private static boolean hasSmooth;
    private static double sx, sy, sz;
    private static long lastNs;

    private static boolean visible;
    private static float ndcX, ndcY, ndcH;

    static {
        for (int i = 0; i < NAMES.length; i++) {
            TEX[i] = class_2960.method_60655("nameprotect", "textures/target/" + NAMES[i] + ".png");
        }
    }

    private TargetEsp() {}

    /** Щокадру після збору стану світу (Proj.begin і Targets.update вже викликано). */
    public static void extract() {
        visible = false;
        if (!Config.targetEsp) return;
        class_310 mc = class_310.method_1551();
        if (mc.field_1724 == null || mc.field_1687 == null) return;

        class_1297 t = Targets.get();
        if (t == null) {
            hasSmooth = false;
            return;
        }
        // невидимих (ефект Invisibility тощо) не показуємо
        if (Config.espIgnoreInvisible && t.method_5767()) {
            hasSmooth = false;
            return;
        }
        if (Targets.age() > HOLD_MS) return;
        if (t != lastTarget) {
            lastTarget = t;
            hasSmooth = false;
        }

        long now = System.nanoTime();
        double dt = lastNs == 0 ? 1.0 : (now - lastNs) / 1.0e9;
        lastNs = now;
        if (!hasSmooth) {
            sx = t.method_23317(); sy = t.method_23318(); sz = t.method_23321();
            hasSmooth = true;
        } else {
            double k = 1.0 - Math.exp(-dt * 25.0);
            sx += (t.method_23317() - sx) * k;
            sy += (t.method_23318() - sy) * k;
            sz += (t.method_23321() - sz) * k;
        }

        double h = t.method_17682();
        float[] c = new float[2], top = new float[2], bot = new float[2];
        if (!Proj.project(sx, sy + h * 0.5, sz, c)) return;
        if (!Proj.project(sx, sy + h, sz, top)) return;
        if (!Proj.project(sx, sy, sz, bot)) return;

        ndcX = c[0];
        ndcY = c[1];
        ndcH = Math.abs(top[1] - bot[1]) * 0.5f;   // частка висоти екрана
        visible = true;
    }

    public static void render(class_332 g) {
        if (!visible || !Config.targetEsp) return;
        int w = g.method_51421(), h = g.method_51443();
        float size = ndcH * h * 1.15f * SIZE[Math.max(0, Math.min(2, Config.size))];
        size = Math.max(16f, Math.min(h * 0.8f, size));
        float cx = (ndcX * 0.5f + 0.5f) * w;
        float cy = (0.5f - ndcY * 0.5f) * h;
        if (cx < -size || cx > w + size || cy < -size || cy > h + size) return;

        int m = Math.max(0, Math.min(NAMES.length - 1, Config.mode));
        g.method_51448().pushMatrix();
        g.method_51448().translate(cx, cy);
        if (Config.spin && SPINNABLE[m]) {
            float ang = (float) ((System.nanoTime() / 1.0e9) % 3.0 / 3.0 * (Math.PI * 2.0));
            g.method_51448().rotate(ang);
        }
        g.method_51448().scale(size / 256f);
        g.method_25290(class_10799.field_56883, TEX[m], -128, -128, 0, 0, 256, 256, 256, 256);
        g.method_51448().popMatrix();
    }
}

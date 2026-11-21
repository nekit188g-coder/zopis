package com.nameprotect;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.class_1297;
import net.minecraft.class_332;

/** KillEffect: червоні лазери зверху + кола на землі там, де помер вбитий тобою. 5 секунд. */
public final class KillEffects {
    private static final long LIFE_MS = 5000;
    private static final long WATCH_MS = 6000;

    private static final class Fx {
        final double x, y, z;
        final long born;

        Fx(double x, double y, double z, long born) {
            this.x = x; this.y = y; this.z = z; this.born = born;
        }
    }

    private static final class Watch {
        final class_1297 e;
        long time;

        Watch(class_1297 e, long time) {
            this.e = e; this.time = time;
        }
    }

    private static final List<Fx> FX = new ArrayList<>();
    private static final List<Watch> WATCHED = new ArrayList<>();
    /** {x1, y1, x2, y2 (NDC), kind (0 = лазер, 1 = коло), alpha} */
    private static final List<float[]> SEGS = new ArrayList<>();

    private KillEffects() {}

    /** Викликається, коли ти вдарив сутність. */
    public static void watch(class_1297 e) {
        long now = System.currentTimeMillis();
        for (Watch w : WATCHED) {
            if (w.e == e) {
                w.time = now;
                return;
            }
        }
        if (WATCHED.size() > 16) WATCHED.remove(0);
        WATCHED.add(new Watch(e, now));
    }

    /** Кожен тік: чи не помер хтось із тих, кого ми били. */
    public static void tick() {
        if (WATCHED.isEmpty()) return;
        long now = System.currentTimeMillis();
        Iterator<Watch> it = WATCHED.iterator();
        while (it.hasNext()) {
            Watch w = it.next();
            if (now - w.time > WATCH_MS) {
                it.remove();
            } else if (!w.e.method_5805()) {
                it.remove();
                if (Config.killEffect) FX.add(new Fx(w.e.method_23317(), w.e.method_23318(), w.e.method_23321(), now));
                if (Config.killSound) HitSounds.playKill();
            }
        }
    }

    /** Щокадру: рахуємо відрізки лазерів і кіл у екранних координатах. */
    public static void extract() {
        SEGS.clear();
        long now = System.currentTimeMillis();
        FX.removeIf(f -> now - f.born > LIFE_MS);
        if (!Config.killEffect || FX.isEmpty()) return;

        float[] seg = new float[4];
        for (Fx f : FX) {
            long age = now - f.born;
            float t = age / 1000f;
            float fade = age > 4000 ? (5000 - age) / 1000f : 1f;

            // 7 лазерів: один у центрі + 6 навколо
            for (int i = 0; i < 7; i++) {
                double ang = Math.toRadians(i * 60.0);
                double r = i == 0 ? 0.0 : 1.7;
                double bx = f.x + Math.cos(ang) * r;
                double bz = f.z + Math.sin(ang) * r;
                float p = Math.max(0f, Math.min(1f, (t - i * 0.07f) / 0.30f));
                if (t - i * 0.07f <= 0f) continue;
                double ground = f.y;
                double bottom, top;
                if (p < 1f) {
                    double head = ground + 18.0 * (1f - p);
                    bottom = head;
                    top = Math.min(ground + 18.0, head + 8.0);
                } else {
                    bottom = ground;
                    top = ground + 18.0;
                }
                float flick = 0.8f + 0.2f * (float) Math.sin(t * 22f + i);
                if (Proj.segment(bx, bottom, bz, bx, top, bz, seg)) {
                    SEGS.add(new float[]{seg[0], seg[1], seg[2], seg[3], 0f, fade * flick});
                }
            }

            ring(f, 1.7 + 0.1 * Math.sin(t * 5f), fade, 40);
            ring(f, 0.9, fade * 0.8f, 28);
            float k = (t * 1.1f) % 1f;
            ring(f, 0.4 + k * 3.2, fade * (1f - k), 48);
        }
    }

    private static void ring(Fx f, double r, float alpha, int n) {
        if (alpha <= 0.02f) return;
        float[] seg = new float[4];
        double y = f.y + 0.05;
        for (int k = 0; k < n; k++) {
            double a0 = Math.PI * 2.0 * k / n;
            double a1 = Math.PI * 2.0 * (k + 1) / n;
            if (Proj.segment(f.x + Math.cos(a0) * r, y, f.z + Math.sin(a0) * r,
                    f.x + Math.cos(a1) * r, y, f.z + Math.sin(a1) * r, seg)) {
                SEGS.add(new float[]{seg[0], seg[1], seg[2], seg[3], 1f, alpha});
            }
        }
    }

    private static int argb(float a, int r, int gr, int b) {
        int ai = Math.max(0, Math.min(255, (int) (a * 255f)));
        return (ai << 24) | (r << 16) | (gr << 8) | b;
    }

    public static void render(class_332 g) {
        if (SEGS.isEmpty() || !Config.killEffect) return;
        int w = g.method_51421(), h = g.method_51443();
        for (float[] s : SEGS) {
            float x1 = (s[0] * 0.5f + 0.5f) * w, y1 = (0.5f - s[1] * 0.5f) * h;
            float x2 = (s[2] * 0.5f + 0.5f) * w, y2 = (0.5f - s[3] * 0.5f) * h;
            float a = Math.max(0f, Math.min(1f, s[5]));
            if (s[4] < 0.5f) {
                line(g, x1, y1, x2, y2, 5, argb(a * 0.30f, 255, 0, 0), w, h);
                line(g, x1, y1, x2, y2, 2, argb(a, 255, 50, 50), w, h);
            } else {
                line(g, x1, y1, x2, y2, 2, argb(a * 0.35f, 255, 0, 0), w, h);
                line(g, x1, y1, x2, y2, 1, argb(a, 255, 70, 70), w, h);
            }
        }
    }

    /** Лінія з квадратиків, обрізана по краях екрана (Ліанга-Барскі). */
    private static void line(class_332 g, float x1, float y1, float x2, float y2, int th, int color, int w, int h) {
        float dx = x2 - x1, dy = y2 - y1;
        float t0 = 0f, t1 = 1f;
        float[] p = {-dx, dx, -dy, dy};
        float[] q = {x1 + 8f, w + 8f - x1, y1 + 8f, h + 8f - y1};
        for (int i = 0; i < 4; i++) {
            if (p[i] == 0f) {
                if (q[i] < 0f) return;
            } else {
                float r = q[i] / p[i];
                if (p[i] < 0f) {
                    if (r > t1) return;
                    if (r > t0) t0 = r;
                } else {
                    if (r < t0) return;
                    if (r < t1) t1 = r;
                }
            }
        }
        float ax = x1 + dx * t0, ay = y1 + dy * t0, bx = x1 + dx * t1, by = y1 + dy * t1;
        float len = (float) Math.sqrt((bx - ax) * (bx - ax) + (by - ay) * (by - ay));
        int n = (int) Math.max(1f, Math.min(400f, len / th));
        int half = th / 2;
        for (int i = 0; i < n; i++) {
            float t = i / (float) n;
            int x = (int) (ax + (bx - ax) * t);
            int y = (int) (ay + (by - ay) * t);
            g.method_25294(x - half, y - half, x - half + th, y - half + th, color);
        }
    }
}

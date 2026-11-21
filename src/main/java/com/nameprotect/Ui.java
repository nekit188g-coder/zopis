package com.nameprotect;

import net.minecraft.class_310;
import net.minecraft.class_332;

/** Малі помічники для малювання: закруглені прямокутники, текст, іконки. */
public final class Ui {
    public static final int BG = 0xF2111118;
    public static final int BORDER = 0xFF1F1F2C;
    public static final int ROW = 0xFF15151E;
    public static final int ROW_HOVER = 0xFF1A1A26;
    public static final int TXT = 0xFFEDEDF5;
    public static final int DIM = 0xFF4A4A5E;
    public static final int MID = 0xFF8A8AA0;
    public static final int ACCENT = 0xFF6C4DF0;
    public static final int OFF_TRACK = 0xFF2A2A38;

    private Ui() {}

    public static void rrect(class_332 g, int x, int y, int w, int h, int r, int c) {
        r = Math.min(r, Math.min(w, h) / 2);
        if (r <= 0) {
            g.method_25294(x, y, x + w, y + h, c);
            return;
        }
        g.method_25294(x, y + r, x + w, y + h - r, c);
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = (int) Math.round(r - Math.sqrt(r * r - dy * dy));
            g.method_25294(x + inset, y + i, x + w - inset, y + i + 1, c);
            g.method_25294(x + inset, y + h - 1 - i, x + w - inset, y + h - i, c);
        }
    }

    public static int lerp(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int aa = (a >>> 24) & 255, ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
        int ba = (b >>> 24) & 255, br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
        return ((int) (aa + (ba - aa) * t) << 24) | ((int) (ar + (br - ar) * t) << 16)
                | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
    }

    /** Множить альфу кольору на a (0..1). */
    public static int alpha(int c, float a) {
        int al = (int) (((c >>> 24) & 255) * Math.max(0f, Math.min(1f, a)));
        return (al << 24) | (c & 0xFFFFFF);
    }

    public static int tw(String s) {
        return Compat.textWidth(s);
    }

    public static void text(class_332 g, String s, int x, int y, int c) {
        Compat.text(g, s, x, y, c);
    }

    public static void textC(class_332 g, String s, int cx, int y, int c) {
        Compat.text(g, s, cx - Compat.textWidth(s) / 2, y, c);
    }

    public static void line(class_332 g, int x1, int y1, int x2, int y2, int c) {
        int dx = Math.abs(x2 - x1), dy = -Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1, sy = y1 < y2 ? 1 : -1;
        int err = dx + dy;
        for (int guard = 0; guard < 512; guard++) {
            g.method_25294(x1, y1, x1 + 1, y1 + 1, c);
            if (x1 == x2 && y1 == y2) break;
            int e2 = 2 * err;
            if (e2 >= dy) { err += dy; x1 += sx; }
            if (e2 <= dx) { err += dx; y1 += sy; }
        }
    }

    public static void ring(class_332 g, int cx, int cy, float r, int c) {
        int n = Math.max(16, (int) (r * 10));
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2 * i / n;
            int px = (int) Math.round(cx + Math.cos(a) * r);
            int py = (int) Math.round(cy + Math.sin(a) * r);
            g.method_25294(px, py, px + 1, py + 1, c);
        }
    }

    /** Логотип-«пульс» 13x9. */
    public static void pulse(class_332 g, int x, int y, int c) {
        int[][] p = {{0, 5}, {3, 5}, {5, 1}, {7, 8}, {9, 4}, {10, 5}, {13, 5}};
        for (int i = 0; i + 1 < p.length; i++) {
            line(g, x + p[i][0], y + p[i][1] - 1, x + p[i + 1][0], y + p[i + 1][1] - 1, c);
        }
    }

    public static void dots(class_332 g, int cx, int cy, int c) {
        for (int i = -1; i <= 1; i++) {
            g.method_25294(cx + i * 4 - 1, cy - 1, cx + i * 4 + 1, cy + 1, c);
        }
    }

    public static void search(class_332 g, int cx, int cy, int c) {
        ring(g, cx - 1, cy - 1, 3.5f, c);
        line(g, cx + 2, cy + 2, cx + 5, cy + 5, c);
    }

    public static void gear(class_332 g, int cx, int cy, int c) {
        ring(g, cx, cy, 3.2f, c);
        ring(g, cx, cy, 1.2f, c);
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * 2 * i / 8;
            int px = (int) Math.round(cx + Math.cos(a) * 5);
            int py = (int) Math.round(cy + Math.sin(a) * 5);
            g.method_25294(px, py, px + 1, py + 1, c);
        }
    }

    public static void toggle(class_332 g, int x, int y, float t) {
        rrect(g, x, y, 20, 10, 5, lerp(OFF_TRACK, ACCENT, t));
        int kx = x + 1 + Math.round(10 * Math.max(0f, Math.min(1f, t)));
        rrect(g, kx, y + 1, 8, 8, 4, lerp(0xFF6B6B7B, 0xFFFFFFFF, t));
    }

    public static int fontH() {
        return 9;
    }

    public static class_310 mc() {
        return class_310.method_1551();
    }
}

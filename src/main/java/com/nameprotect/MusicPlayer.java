package com.nameprotect;

import net.minecraft.class_10799;
import net.minecraft.class_2960;
import net.minecraft.class_332;

/** MusicPlayer: обкладинка, назва, артист, еквалайзер; кнопки ⏮ ⏯ ⏭ працюють, коли відкрите меню. Перетягується. */
public final class MusicPlayer {
    private static final int W = 154, H = 44, COVER = 32;

    private static int lx, ly;
    private static boolean shown;
    private static float fade;
    private static class_2960 cover;
    private static String coverFor = "";

    private MusicPlayer() {}

    private static boolean menuOpen() {
        return Ui.mc().field_1755 instanceof MenuScreen;
    }

    public static boolean contains(int mx, int my) {
        return shown && mx >= lx && mx < lx + W && my >= ly && my < ly + H;
    }

    public static int x() { return lx; }

    public static int y() { return ly; }

    public static void setPos(int x, int y, int screenW, int screenH) {
        Config.mpX = Math.max(0, Math.min(screenW - W, x));
        Config.mpY = Math.max(0, Math.min(screenH - H, y));
    }

    private static int bx(int i) {
        return lx + W - 8 - (3 - i) * 17;
    }

    private static int by() {
        return ly + H - 17;
    }

    /** Клік по кнопках керування (тільки при відкритому меню). */
    public static boolean click(int mx, int my) {
        if (!shown || !menuOpen()) return false;
        for (int i = 0; i < 3; i++) {
            if (mx >= bx(i) && mx < bx(i) + 15 && my >= by() && my < by() + 13) {
                if (i == 0) Media.prev();
                else if (i == 1) Media.playPause();
                else Media.next();
                return true;
            }
        }
        return false;
    }

    public static void render(class_332 g) {
        boolean menu = menuOpen();
        boolean want = Config.musicPlayer && (Media.has || menu);
        fade += ((want ? 1f : 0f) - fade) * 0.18f;
        if (fade < 0.02f) {
            shown = false;
            return;
        }
        shown = true;
        float a = fade;

        // нова обкладинка від фонового потоку -> текстура (тільки з render-потоку)
        byte[] png = Media.pendingPng;
        if (png != null) {
            Media.pendingPng = null;
            class_2960 t = Compat.uploadCover(png);
            if (t != null) {
                cover = t;
                coverFor = Media.pendingKey;
            }
        }

        int sw = g.method_51421(), sh = g.method_51443();
        int x = Config.mpX >= 0 ? Config.mpX : sw - W - 8;
        int y = Config.mpY >= 0 ? Config.mpY : sh - H - 8;
        x = Math.max(0, Math.min(sw - W, x));
        y = Math.max(0, Math.min(sh - H, y));
        lx = x; ly = y;

        Ui.rrect(g, x, y, W, H, 6, Ui.alpha(0xE0121219, a));

        // обкладинка або заглушка з нотою
        int cx = x + 6, cy = y + 6;
        if (cover != null && coverFor.equals(Media.key()) && Media.has) {
            var m = g.method_51448();
            m.pushMatrix();
            m.translate((float) cx, (float) cy);
            m.scale(COVER / 128f);
            g.method_25290(class_10799.field_56883, cover, 0, 0, 0f, 0f, 128, 128, 128, 128);
            m.popMatrix();
        } else {
            Ui.rrect(g, cx, cy, COVER, COVER, 4, Ui.alpha(0xFF23233A, a));
            int nx = cx + 13, ny = cy + 21;
            Ui.ring(g, nx, ny, 3f, Ui.alpha(Ui.ACCENT, a));
            g.method_25294(nx + 3, cy + 8, nx + 5, ny, Ui.alpha(Ui.ACCENT, a));
            g.method_25294(nx + 3, cy + 8, nx + 10, cy + 10, Ui.alpha(Ui.ACCENT, a));
        }

        // текст
        int tx = x + 46;
        String title = Media.has ? Media.title : "Nothing playing";
        String artist = Media.has ? Media.artist : "Open Spotify / YouTube";
        Ui.text(g, fit(title, W - 46 - 20), tx, y + 8, Ui.alpha(Ui.TXT, a));
        Ui.text(g, fit(artist, W - 46 - 8), tx, y + 19, Ui.alpha(Ui.MID, a));

        // еквалайзер (грає) — праворуч зверху
        if (Media.has) {
            double t = System.nanoTime() / 1.0e9;
            for (int i = 0; i < 3; i++) {
                int bh = Media.playing ? 3 + (int) Math.round(3 * (0.5 + 0.5 * Math.sin(t * (5 + i * 2) + i))) : 2;
                int ex = x + W - 14 + i * 3;
                g.method_25294(ex, y + 14 - bh, ex + 2, y + 14, Ui.alpha(Ui.ACCENT, a));
            }
        }

        // нижній рядок: джерело, або кнопки керування при відкритому меню
        if (menu) {
            for (int i = 0; i < 3; i++) {
                int px = bx(i), py = by();
                boolean hov = false;
                Ui.rrect(g, px, py, 15, 13, 3, 0xFF1E1E2A);
                int c = Ui.TXT;
                if (i == 1) {
                    if (Media.playing) {
                        g.method_25294(px + 5, py + 3, px + 7, py + 10, c);
                        g.method_25294(px + 8, py + 3, px + 10, py + 10, c);
                    } else {
                        tri(g, px + 6, py + 3, true, c);
                    }
                } else if (i == 0) {
                    g.method_25294(px + 4, py + 3, px + 5, py + 10, c);
                    tri(g, px + 6, py + 3, false, c);
                } else {
                    tri(g, px + 4, py + 3, true, c);
                    g.method_25294(px + 10, py + 3, px + 11, py + 10, c);
                }
            }
        } else if (Media.has) {
            Ui.text(g, Media.source, tx, y + 31, Ui.alpha(Ui.DIM, a));
        }
    }

    private static final int[] TRI = {7, 5, 4, 2, 1};

    /** Трикутник 5x7: right=true — вістря вправо. */
    private static void tri(class_332 g, int x, int y, boolean right, int c) {
        for (int i = 0; i < 5; i++) {
            int h = TRI[i];
            int col = right ? x + i : x + 4 - i;
            int top = y + (7 - h) / 2;
            g.method_25294(col, top, col + 1, top + h, c);
        }
    }

    private static String fit(String s, int maxW) {
        if (Ui.tw(s) <= maxW) return s;
        while (s.length() > 1 && Ui.tw(s + "...") > maxW) s = s.substring(0, s.length() - 1);
        return s + "...";
    }
}

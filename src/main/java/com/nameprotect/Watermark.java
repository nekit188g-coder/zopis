package com.nameprotect;

import net.minecraft.class_1297;
import net.minecraft.class_332;

/** Watermark: «пілюля» — лого, голова, нік, пінг, FPS. Перетягується мишкою, коли відкрите меню. */
public final class Watermark {
    private static int lx, ly, lw, lh;

    private Watermark() {}

    public static boolean contains(int mx, int my) {
        return lw > 0 && mx >= lx && mx < lx + lw && my >= ly && my < ly + lh;
    }

    public static int x() { return lx; }

    public static int y() { return ly; }

    public static void setPos(int x, int y, int screenW, int screenH) {
        Config.wmX = Math.max(0, Math.min(screenW - lw, x));
        Config.wmY = Math.max(0, Math.min(screenH - lh, y));
    }

    public static void render(class_332 g) {
        Compat.frame();
        if (!Config.watermark) {
            lw = 0;
            return;
        }

        String name = NameProtectClient.realName;
        if (name == null || name.isEmpty()) name = "Player";
        int ping = Compat.ping();
        String pingS = ping >= 0 ? String.valueOf(ping) : "-";
        String fpsS = String.valueOf(Compat.fps());

        int h = 20;
        int pad = 10;
        int w = pad + 13 + 7 + 12 + 5 + Ui.tw(name) + 10
                + Ui.tw(pingS) + 3 + Ui.tw("ms") + 10
                + Ui.tw(fpsS) + 3 + Ui.tw("FPS") + pad;
        int sw = g.method_51421(), sh = g.method_51443();
        int x = Config.wmX >= 0 ? Config.wmX : (sw - w) / 2;
        int y = Config.wmY >= 0 ? Config.wmY : 8;
        x = Math.max(0, Math.min(sw - w, x));
        y = Math.max(0, Math.min(sh - h, y));
        lx = x; ly = y; lw = w; lh = h;

        Ui.rrect(g, x, y, w, h, 10, 0xD0121219);
        int cx = x + pad;
        int ty = y + (h - Ui.fontH()) / 2 + 1;
        Ui.pulse(g, cx, y + (h - 9) / 2, Ui.ACCENT);
        cx += 13 + 7;
        Compat.head(g, localPlayer(), cx, y + 4, 12);
        cx += 12 + 5;
        Ui.text(g, name, cx, ty, Ui.TXT);
        cx += Ui.tw(name) + 10;
        Ui.text(g, pingS, cx, ty, Ui.TXT);
        cx += Ui.tw(pingS) + 3;
        Ui.text(g, "ms", cx, ty, Ui.MID);
        cx += Ui.tw("ms") + 10;
        Ui.text(g, fpsS, cx, ty, Ui.TXT);
        cx += Ui.tw(fpsS) + 3;
        Ui.text(g, "FPS", cx, ty, Ui.MID);
    }

    private static class_1297 localPlayer() {
        return Ui.mc().field_1724;
    }
}

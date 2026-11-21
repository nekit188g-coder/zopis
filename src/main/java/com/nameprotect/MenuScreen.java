package com.nameprotect;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import net.minecraft.class_11908;
import net.minecraft.class_11909;
import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_437;
import org.lwjgl.glfw.GLFW;

/** ClickGUI на Right Shift: вкладки Visuals / HUD / Unilites, сітка модулів зі свічами, «...» відкриває налаштування. */
public class MenuScreen extends class_437 {
    private static final int PW = 400, PH = 250, ROW_H = 26, GAP = 4;
    private static final String[] TAB_NAMES = {"Visuals", "HUD", "Unilites"};
    private static final String[] MODES = {"Radar", "Bruh", "Circle", "What", "Triad", "Fsociety", "Photo", "Sardelki"};

    // ------------------------------------------------------------- модель
    private static final class Opt {
        final String label;
        final String[] values;
        final IntSupplier get;
        final IntConsumer set;

        Opt(String label, String[] values, IntSupplier get, IntConsumer set) {
            this.label = label; this.values = values; this.get = get; this.set = set;
        }

        boolean isSwitch() {
            return values.length == 2 && values[0].equals("Off");
        }
    }

    private static final class Module {
        final String name;
        final BooleanSupplier on;
        final Runnable toggle;
        final Opt[] opts;
        float anim = -1f;

        Module(String name, BooleanSupplier on, Runnable toggle, Opt... opts) {
            this.name = name; this.on = on; this.toggle = toggle; this.opts = opts;
        }
    }

    private static Opt choice(String l, String[] v, IntSupplier g, IntConsumer s) {
        return new Opt(l, v, g, s);
    }

    private static Opt sw(String l, BooleanSupplier g, java.util.function.Consumer<Boolean> s) {
        return new Opt(l, new String[]{"Off", "On"}, () -> g.getAsBoolean() ? 1 : 0, v -> s.accept(v == 1));
    }

    private static int lastHitSound = 1;

    private static final Module[][] TABS = {
            {
                    new Module("TargetESP", () -> Config.targetEsp, () -> Config.targetEsp = !Config.targetEsp,
                            choice("Mode", MODES, () -> Config.mode, v -> Config.mode = v),
                            choice("Size", new String[]{"S", "M", "L"}, () -> Config.size, v -> Config.size = v),
                            sw("Spin", () -> Config.spin, v -> Config.spin = v),
                            sw("Ignore invisible", () -> Config.espIgnoreInvisible, v -> Config.espIgnoreInvisible = v)),
                    new Module("KillEffect", () -> Config.killEffect, () -> Config.killEffect = !Config.killEffect),
            },
            {
                    new Module("Watermark", () -> Config.watermark, () -> Config.watermark = !Config.watermark),
                    new Module("TargetHUD", () -> Config.targetHud, () -> Config.targetHud = !Config.targetHud,
                            sw("Armor", () -> Config.hudArmor, v -> Config.hudArmor = v)),
                    new Module("MusicPlayer", () -> Config.musicPlayer, () -> Config.musicPlayer = !Config.musicPlayer,
                            sw("Cover (online)", () -> Config.mpCover, v -> Config.mpCover = v)),
            },
            {
                    new Module("HitSounds", () -> Config.hitSound != 0, () -> {
                        if (Config.hitSound != 0) {
                            lastHitSound = Config.hitSound;
                            Config.hitSound = 0;
                        } else {
                            Config.hitSound = lastHitSound;
                            HitSounds.play(Config.hitSound, 1.0f);
                        }
                    },
                            choice("Sound", new String[]{"Off", "Undertale", "Bubble"}, () -> Config.hitSound, v -> {
                                Config.hitSound = v;
                                HitSounds.play(v, 1.0f);
                            }),
                            choice("Volume", new String[]{"25%", "50%", "100%"}, () -> Config.volume, v -> {
                                Config.volume = v;
                                HitSounds.play(Config.hitSound, 1.0f);
                            })),
                    new Module("KillSound", () -> Config.killSound, () -> Config.killSound = !Config.killSound),
                    new Module("NameProtect", () -> Config.nameProtect, () -> Config.nameProtect = !Config.nameProtect),
            },
    };

    // ------------------------------------------------------------- стан
    private record Hit(int x, int y, int w, int h, Runnable action) {}

    private final List<Hit> hits = new ArrayList<>();
    private int mx, my;
    private int tab = 0;
    private Module open = null;
    private int px, py;                 // лівий верхній кут панелі
    private int drag = 0;               // 0 нічого, 1 Watermark, 2 MusicPlayer
    private int grabX, grabY;

    public MenuScreen() {
        super(class_2561.method_43470("LoLHelper"));
    }

    @Override
    public boolean method_25421() {
        return false;
    }

    private boolean in(int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    // ------------------------------------------------------------- малювання
    @Override
    public void method_25394(class_332 g, int mouseX, int mouseY, float delta) {
        mx = mouseX;
        my = mouseY;
        hits.clear();

        int x0 = (this.field_22789 - PW) / 2;
        int y0 = (this.field_22790 - PH) / 2 - 12;
        px = x0;
        py = y0;

        if (drag != 0) {
            boolean down = GLFW.glfwGetMouseButton(GLFW.glfwGetCurrentContext(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
            if (down) {
                if (drag == 1) Watermark.setPos(mx - grabX, my - grabY, this.field_22789, this.field_22790);
                else MusicPlayer.setPos(mx - grabX, my - grabY, this.field_22789, this.field_22790);
            } else {
                drag = 0;
                Config.save();
            }
        }

        // панель з тонкою рамкою
        Ui.rrect(g, x0 - 1, y0 - 1, PW + 2, PH + 2, 7, Ui.BORDER);
        Ui.rrect(g, x0, y0, PW, PH, 6, Ui.BG);

        drawHeader(g, x0, y0);
        if (open == null) drawGrid(g, x0, y0);
        else drawSettings(g, x0, y0);
        drawDock(g, x0 + PW / 2, y0 + PH + 8);
    }

    private void drawHeader(class_332 g, int x0, int y0) {
        Ui.pulse(g, x0 + 14, y0 + 16, Ui.ACCENT);

        int tx = x0 + 40;
        for (int i = 0; i < TAB_NAMES.length; i++) {
            final int idx = i;
            int w = Ui.tw(TAB_NAMES[i]);
            boolean sel = tab == i;
            boolean hov = in(tx - 4, y0 + 10, w + 8, 22);
            Ui.text(g, TAB_NAMES[i], tx, y0 + 16, sel ? Ui.TXT : (hov ? Ui.MID : Ui.DIM));
            hits.add(new Hit(tx - 4, y0 + 10, w + 8, 22, () -> {
                tab = idx;
                open = null;
            }));
            tx += w + 18;
        }

        Ui.search(g, x0 + PW - 92, y0 + 20, Ui.DIM);
        Ui.gear(g, x0 + PW - 66, y0 + 21, Ui.DIM);

        // аватар
        int ax = x0 + PW - 12 - 26, ay = y0 + 8;
        Ui.rrect(g, ax - 1, ay - 1, 28, 28, 14, 0xFF2A2A38);
        Compat.head(g, Ui.mc().field_1724, ax + 1, ay + 1, 24);
        Ui.rrect(g, ax + 19, ay + 19, 7, 7, 3, Ui.BG);
        Ui.rrect(g, ax + 20, ay + 20, 5, 5, 2, 0xFF3DDC84);
    }

    private void drawGrid(class_332 g, int x0, int y0) {
        Module[] mods = TABS[tab];
        int cw = (PW - 24 - 8) / 2;
        for (int i = 0; i < mods.length; i++) {
            int x = x0 + 12 + (i % 2) * (cw + 8);
            int y = y0 + 52 + (i / 2) * (ROW_H + GAP);
            moduleRow(g, mods[i], x, y, cw);
        }
    }

    private void moduleRow(class_332 g, Module m, int x, int y, int w) {
        boolean hover = in(x, y, w, ROW_H);
        Ui.rrect(g, x, y, w, ROW_H, 4, hover ? Ui.ROW_HOVER : Ui.ROW);

        boolean on = m.on.getAsBoolean();
        if (m.anim < 0f) m.anim = on ? 1f : 0f;
        m.anim += ((on ? 1f : 0f) - m.anim) * 0.3f;

        Ui.text(g, m.name, x + 10, y + (ROW_H - Ui.fontH()) / 2 + 1, Ui.lerp(Ui.DIM, Ui.TXT, m.anim));

        boolean hasOpts = m.opts.length > 0;
        boolean dotsHover = hasOpts && in(x + w - 62, y, 20, ROW_H);
        Ui.dots(g, x + w - 52, y + ROW_H / 2, hasOpts ? (dotsHover ? Ui.TXT : Ui.MID) : Ui.DIM);
        Ui.toggle(g, x + w - 30, y + (ROW_H - 10) / 2, m.anim);

        if (hasOpts) hits.add(new Hit(x + w - 62, y, 20, ROW_H, () -> open = m));
        hits.add(new Hit(x, y, w, ROW_H, m.toggle));
    }

    private void drawSettings(class_332 g, int x0, int y0) {
        int x = x0 + 12, w = PW - 24;
        boolean backHov = in(x, y0 + 48, 120, 18);
        Ui.text(g, "<  " + open.name, x + 2, y0 + 53, backHov ? Ui.TXT : Ui.MID);
        hits.add(new Hit(x, y0 + 48, 120, 18, () -> open = null));

        int y = y0 + 74;
        for (Opt o : open.opts) {
            Ui.rrect(g, x, y, w, 24, 4, Ui.ROW);
            Ui.text(g, o.label, x + 10, y + 8, Ui.TXT);
            int cur = o.get.getAsInt();
            int xr = x + w - 10;
            if (o.isSwitch()) {
                Ui.toggle(g, xr - 20, y + 7, cur == 1 ? 1f : 0f);
                final int next = cur == 1 ? 0 : 1;
                hits.add(new Hit(x, y, w, 24, () -> o.set.accept(next)));
            } else if (o.values.length <= 4) {
                for (int i = o.values.length - 1; i >= 0; i--) {
                    final int idx = i;
                    int cwid = Ui.tw(o.values[i]) + 14;
                    int cx = xr - cwid;
                    chip(g, cx, y + 5, cwid, o.values[i], cur == i);
                    hits.add(new Hit(cx, y + 5, cwid, 14, () -> o.set.accept(idx)));
                    xr = cx - 4;
                }
            } else {
                String v = o.values[Math.max(0, Math.min(o.values.length - 1, cur))];
                int cwid = 90;
                int cx = xr - cwid;
                chip(g, cx, y + 5, cwid, v, true);
                final int next = (cur + 1) % o.values.length;
                hits.add(new Hit(x, y, w, 24, () -> o.set.accept(next)));
            }
            y += 28;
        }
    }

    private void chip(class_332 g, int x, int y, int w, String label, boolean selected) {
        boolean hov = in(x, y, w, 14);
        Ui.rrect(g, x, y, w, 14, 4, selected ? Ui.ACCENT : (hov ? 0xFF262634 : 0xFF1E1E2A));
        Ui.textC(g, label, x + w / 2, y + 3, selected ? 0xFFFFFFFF : Ui.MID);
    }

    /** Нижній док (як на скріні): перша іконка активна, решта — декор. */
    private void drawDock(class_332 g, int cx, int y) {
        int w = 5 * 22 + 10, h = 24;
        int x = cx - w / 2;
        Ui.rrect(g, x, y, w, h, 6, 0xE0121219);
        for (int i = 0; i < 5; i++) {
            int ix = x + 5 + i * 22, iy = y + 4;
            if (i == 0) Ui.rrect(g, ix, iy, 16, 16, 4, Ui.ACCENT);
            int c = i == 0 ? 0xFFFFFFFF : Ui.DIM;
            int mx0 = ix + 8, my0 = iy + 8;
            switch (i) {
                case 0 -> {
                    g.method_25294(ix + 3, iy + 5, ix + 13, iy + 6, c);
                    g.method_25294(ix + 3, iy + 10, ix + 13, iy + 11, c);
                    g.method_25294(ix + 5, iy + 4, ix + 7, iy + 7, c);
                    g.method_25294(ix + 9, iy + 9, ix + 11, iy + 12, c);
                }
                case 1 -> {
                    g.method_25294(ix + 3, iy + 3, ix + 13, iy + 4, c);
                    g.method_25294(ix + 3, iy + 12, ix + 13, iy + 13, c);
                    g.method_25294(ix + 3, iy + 3, ix + 4, iy + 13, c);
                    g.method_25294(ix + 12, iy + 3, ix + 13, iy + 13, c);
                    g.method_25294(ix + 7, iy + 4, ix + 8, iy + 12, c);
                }
                case 2 -> {
                    Ui.ring(g, mx0, my0 - 2, 2.5f, c);
                    Ui.ring(g, mx0, my0 + 6, 4.5f, c);
                }
                case 3 -> {
                    g.method_25294(ix + 3, iy + 3, ix + 13, iy + 4, c);
                    g.method_25294(ix + 3, iy + 7, ix + 13, iy + 8, c);
                    g.method_25294(ix + 3, iy + 12, ix + 13, iy + 13, c);
                    g.method_25294(ix + 3, iy + 3, ix + 4, iy + 13, c);
                    g.method_25294(ix + 12, iy + 3, ix + 13, iy + 13, c);
                }
                default -> {
                    Ui.ring(g, mx0, my0, 5f, c);
                    Ui.ring(g, mx0, my0, 2f, c);
                }
            }
        }
    }

    // ------------------------------------------------------------- ввід
    @Override
    public boolean method_25402(class_11909 event, boolean doubleClick) {
        for (Hit h : hits) {
            if (in(h.x(), h.y(), h.w(), h.h())) {
                h.action().run();
                Config.save();
                return true;
            }
        }
        // клік по панелі, але не по елементу — нічого не чіпаємо
        if (in(px, py, PW, PH)) return true;
        // кнопки плеєра, потім перетягування віджетів
        if (MusicPlayer.click(mx, my)) return true;
        if (Watermark.contains(mx, my)) {
            drag = 1; grabX = mx - Watermark.x(); grabY = my - Watermark.y();
            return true;
        }
        if (MusicPlayer.contains(mx, my)) {
            drag = 2; grabX = mx - MusicPlayer.x(); grabY = my - MusicPlayer.y();
            return true;
        }
        return super.method_25402(event, doubleClick);
    }

    @Override
    public boolean method_25404(class_11908 event) {
        if (event.comp_4795() == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            this.method_25419();
            return true;
        }
        return super.method_25404(event);
    }
}

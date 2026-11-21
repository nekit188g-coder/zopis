package com.nameprotect;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.class_10799;
import net.minecraft.class_2960;
import net.minecraft.class_332;

/** Тексти меню — картинки, відрендерені шрифтом arialmt.ttf. */
public final class Labels {
    public static final int H = 43;
    private static final Map<String, Integer> W = new HashMap<>();
    private static final Map<String, class_2960> TEX = new HashMap<>();

    static {
        W.put("wm", 189);
        W.put("targetesp", 189);
        W.put("mode", 107);
        W.put("m1", 154);
        W.put("m2", 132);
        W.put("m3", 147);
        W.put("m4", 130);
        W.put("m5", 139);
        W.put("m6", 199);
        W.put("m7", 216);
        W.put("m8", 192);
        W.put("hitsounds", 179);
        W.put("h0", 51);
        W.put("h1", 219);
        W.put("h2", 172);
        W.put("watermark", 186);
        W.put("on", 54);
        W.put("off", 78);
        W.put("killeffect", 155);
        W.put("killsound", 169);
        W.put("nameprotect", 232);
        W.put("spin", 83);
        W.put("size", 92);
        W.put("s", 30);
        W.put("m", 30);
        W.put("l", 30);
        W.put("volume", 147);
        W.put("v25", 86);
        W.put("v50", 86);
        W.put("v100", 108);
    }

    private Labels() {}

    public static int width(String id) {
        Integer w = W.get(id);
        return w == null ? 0 : w;
    }

    public static void draw(class_332 g, String id, int x, int y, float scale) {
        Integer w = W.get(id);
        if (w == null) return;
        class_2960 tex = TEX.computeIfAbsent(id, k -> class_2960.method_60655("nameprotect", "textures/gui/label/" + k + ".png"));
        g.method_51448().pushMatrix();
        g.method_51448().translate((float) x, (float) y);
        g.method_51448().scale(scale);
        g.method_25290(class_10799.field_56883, tex, 0, 0, 0, 0, w, H, w, H);
        g.method_51448().popMatrix();
    }
}

package com.nameprotect;

import net.minecraft.class_1109;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_3414;

public final class HitSounds {
    private static final class_3414 UNDERTALE = class_3414.method_47908(class_2960.method_60655("nameprotect", "hit_undertale"));
    private static final class_3414 BUBBLE = class_3414.method_47908(class_2960.method_60655("nameprotect", "hit_bubble"));
    private static final float[] VOL = {0.25f, 0.5f, 1.0f};

    private HitSounds() {}

    public static void play() {
        play(Config.hitSound, 1.0f);
    }

    /** Звук вбивства: той самий хітсаунд, але нижчим тоном. */
    public static void playKill() {
        play(Config.hitSound, 0.7f);
    }

    public static void play(int mode, float pitch) {
        if (mode <= 0) return;
        class_3414 ev = mode == 1 ? UNDERTALE : BUBBLE;
        float vol = VOL[Math.max(0, Math.min(2, Config.volume))];
        class_310.method_1551().method_1483().method_4873(class_1109.method_4757(ev, pitch, vol));
    }
}

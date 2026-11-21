package com.nameprotect;

import net.minecraft.class_1297;
import net.minecraft.class_1309;
import net.minecraft.class_239;
import net.minecraft.class_310;
import net.minecraft.class_3966;

/** Поточна ціль (на яку дивишся або яку вдарив). Спільна для TargetESP і TargetHUD. */
public final class Targets {
    /** Скільки мс ціль залишається актуальною після останнього погляду/удару. */
    public static final long HOLD_MS = 3000;

    private static class_1297 target;
    private static long time;

    private Targets() {}

    public static void set(class_1297 e) {
        target = e;
        time = System.currentTimeMillis();
    }

    public static class_1297 get() {
        return target;
    }

    public static long age() {
        return System.currentTimeMillis() - time;
    }

    /** Щокадру. */
    public static void update() {
        if (!Config.targetEsp && !Config.targetHud) {
            target = null;
            return;
        }
        class_310 mc = class_310.method_1551();
        if (mc.field_1724 == null || mc.field_1687 == null) {
            target = null;
            return;
        }
        class_239 hr = mc.field_1765;
        if (hr instanceof class_3966 ehr && ehr.method_17782() instanceof class_1309 le
                && le != mc.field_1724 && le.method_5805()) {
            set(le);
        }
        class_1297 t = target;
        if (t != null && (!t.method_5805() || t.method_31481() || age() > HOLD_MS)) {
            target = null;
        }
    }
}

package com.nameprotect.mixin;

import com.nameprotect.Config;
import com.nameprotect.NameProtectClient;
import net.minecraft.class_5223;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** NameProtect: увесь текст (чат, таб, неймтеги, GUI) проходить тут — міняємо нік візуально. */
@Mixin(class_5223.class)
public class StringDecomposerMixin {
    private static final String FAKE_NAME = "PULSE - DODGE 10%";

    @ModifyVariable(
            method = "iterateFormatted(Ljava/lang/String;ILnet/minecraft/network/chat/Style;Lnet/minecraft/network/chat/Style;Lnet/minecraft/util/FormattedCharSink;)Z",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0,
            require = 0
    )
    private static String nameprotect$replaceName(String text) {
        String real = NameProtectClient.realName;
        if (!Config.nameProtect || text == null || real == null || real.isEmpty() || !text.contains(real)) return text;
        return text.replace(real, FAKE_NAME);
    }
}

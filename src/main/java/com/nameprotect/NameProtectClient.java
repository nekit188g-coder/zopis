package com.nameprotect;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.class_1269;
import net.minecraft.class_1309;
import net.minecraft.class_2960;
import net.minecraft.class_304;
import net.minecraft.class_3675;
import org.lwjgl.glfw.GLFW;

public class NameProtectClient implements ClientModInitializer {
    /** Справжній нік (для NameProtect-підміни). */
    public static volatile String realName = "";

    private static class_304 menuKey;

    @Override
    public void onInitializeClient() {
        Config.load();
        Media.start();

        menuKey = KeyBindingHelper.registerKeyBinding(new class_304(
                "key.nameprotect.menu", class_3675.class_307.field_1668, GLFW.GLFW_KEY_RIGHT_SHIFT, class_304.class_11900.field_62556));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.field_1724 != null) {
                realName = client.field_1724.method_5477().getString();
            }
            KillEffects.tick();
            while (menuKey.method_1436()) {
                if (client.field_1755 == null) {
                    client.method_1507(new MenuScreen());
                }
            }
        });

        WorldRenderEvents.END_EXTRACTION.register(ctx -> {
            Proj.begin(ctx);
            Targets.update();
            TargetEsp.extract();
            KillEffects.extract();
        });

        HudElementRegistry.addLast(class_2960.method_60655("nameprotect", "overlay"), (g, tick) -> {
            KillEffects.render(g);
            TargetEsp.render(g);
            TargetHud.render(g);
            MusicPlayer.render(g);
            Watermark.render(g);
        });

        AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (level.method_8608() && entity instanceof class_1309) {
                Targets.set(entity);
                KillEffects.watch(entity);
                HitSounds.play();
            }
            return class_1269.field_5811;
        });
    }
}

package com.tps.anchoroptimiser;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;

public class TpsAnchorOptimiser implements ClientModInitializer {
    public static final String MOD_ID = "tps_anchor_optimiser";

    private static KeyMapping openMenu;

    @Override
    public void onInitializeClient() {
        Rules.load();

        openMenu = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.tps_anchor_optimiser.open",
                InputConstants.Type.KEYSYM,
                InputConstants.KEY_K,
                "key.categories.misc"));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (openMenu.consumeClick()) {
                if (mc.screen == null) {
                    mc.setScreen(new RulesScreen(null));
                }
            }
        });
    }
}

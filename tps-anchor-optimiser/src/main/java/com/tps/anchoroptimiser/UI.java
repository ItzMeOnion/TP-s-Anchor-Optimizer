package com.tps.anchoroptimiser;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;

/** Small helpers shared by the menu screens. */
final class UI {
    /** The 16 vanilla chat colours, in the usual order. */
    static final int[] COLORS = {
        0x000000, 0x0000AA, 0x00AA00, 0x00AAAA,
        0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
        0x555555, 0x5555FF, 0x55FF55, 0x55FFFF,
        0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF
    };

    private UI() {}

    /** Draws the REAL item (ignoring any disguise) so the menu is never confusing. */
    static void icon(GuiGraphics g, Item item, int x, int y) {
        boolean old = Rules.bypass;
        Rules.bypass = true;
        try {
            g.renderItem(item.getDefaultInstance(), x, y);
        } finally {
            Rules.bypass = old;
        }
    }

    static String label(String id) {
        return Rules.item(id)
                .map(i -> Component.translatable(i.getDescriptionId()).getString())
                .orElse("(none)");
    }
}

package dev.skylasliver.expbook.util;

import net.minecraft.client.gui.GuiGraphics;

/** Shared drawing helpers for the book screen. */
public final class RenderUtil {

    private RenderUtil() {
    }

    /** Draws the three-row player inventory well plus the hotbar row. */
    public static void drawPlayerInventory(GuiGraphics graphics, int left, int top, int slotTop) {
        graphics.fill(left + 7, top + slotTop - 1, left + 169, top + slotTop + 55, 0xFF0E0A14);
        graphics.fill(left + 7, top + slotTop + 57, left + 169, top + slotTop + 75, 0xFF0E0A14);
    }
}

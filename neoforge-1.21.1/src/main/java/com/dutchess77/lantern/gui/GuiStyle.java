package com.dutchess77.lantern.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Shared look for the mod's block GUIs: title treatment and raised buttons. */
final class GuiStyle {

    private GuiStyle() {
    }

    /** Purple title with shadow - same treatment on every block GUI. Not bold:
     *  bold width collides with the bench's north socket. */
    static void drawTitle(GuiGraphics guiGraphics, Font font, Component title) {
        guiGraphics.drawString(font, title, 8, 6, 0x7B4FC8, true);
    }

    /** Raised button face: dark outline, light top-left, shadowed bottom-right. */
    static void paintRaised(GuiGraphics guiGraphics, int x, int y, int w, int h, boolean hovered) {
        guiGraphics.fill(x, y, x + w, y + h, 0xFF2A2A2A);
        guiGraphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, hovered ? 0xFFB4BEC8 : 0xFF9DA5AD);
        guiGraphics.fill(x + 1, y + 1, x + w - 1, y + 2, 0xFFD8DEE4);
        guiGraphics.fill(x + 1, y + 1, x + 2, y + h - 1, 0xFFD8DEE4);
        guiGraphics.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, 0xFF5A6068);
        guiGraphics.fill(x + w - 2, y + 2, x + w - 1, y + h - 1, 0xFF5A6068);
    }
}

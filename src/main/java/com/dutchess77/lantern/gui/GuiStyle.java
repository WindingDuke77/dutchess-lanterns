package com.dutchess77.lantern.gui;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Shared look for the mod's block GUIs: title treatment and raised buttons. */
@SideOnly(Side.CLIENT)
final class GuiStyle {

    private GuiStyle() {
    }

    /** Bold purple title with shadow - same treatment on every block GUI. */
    static void drawTitle(FontRenderer fr, String title) {
        fr.drawString(TextFormatting.BOLD + title, 8, 6, 0x7B4FC8, true);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /** Raised button face: dark outline, light top-left, shadowed bottom-right. */
    static void paintRaised(int x, int y, int w, int h, boolean hovered) {
        Gui.drawRect(x, y, x + w, y + h, 0xFF2A2A2A);
        Gui.drawRect(x + 1, y + 1, x + w - 1, y + h - 1, hovered ? 0xFFB4BEC8 : 0xFF9DA5AD);
        Gui.drawRect(x + 1, y + 1, x + w - 1, y + 2, 0xFFD8DEE4);
        Gui.drawRect(x + 1, y + 1, x + 2, y + h - 1, 0xFFD8DEE4);
        Gui.drawRect(x + 1, y + h - 2, x + w - 1, y + h - 1, 0xFF5A6068);
        Gui.drawRect(x + w - 2, y + 2, x + w - 1, y + h - 1, 0xFF5A6068);
    }
}

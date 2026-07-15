package com.dutchess77.lantern.gui;

import com.dutchess77.lantern.Lantern;
import com.dutchess77.lantern.block.DarknessWardTileEntity;
import com.dutchess77.lantern.network.WardAdjustMessage;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Size / offset editor for the Darkness Ward. Button ids encode
 * (field * 2 + direction); values render straight from the client tile
 * entity, which the server keeps synced after every adjustment.
 */
@SideOnly(Side.CLIENT)
public class DarknessWardGui extends GuiContainer {

    private static final ResourceLocation TEXTURE =
        new ResourceLocation(Lantern.MODID, "textures/gui/darkness_ward.png");

    private static final int[] ROW_Y = { 25, 39, 53 };
    private static final String[] AXES = { "X", "Y", "Z" };

    private final DarknessWardTileEntity ward;

    public DarknessWardGui(InventoryPlayer playerInventory, DarknessWardTileEntity ward) {
        super(new DarknessWardContainer(playerInventory, ward));
        this.ward = ward;
        this.xSize = 176;
        this.ySize = 166;
    }

    @Override
    public void initGui() {
        super.initGui();
        buttonList.clear();
        for (int axis = 0; axis < 3; axis++) {
            int y = guiTop + ROW_Y[axis];
            addButton(new SmallButton(axis * 2, guiLeft + 20, y, "-"));           // radius -
            addButton(new SmallButton(axis * 2 + 1, guiLeft + 56, y, "+"));       // radius +
            addButton(new SmallButton((axis + 3) * 2, guiLeft + 84, y, "-"));     // offset -
            addButton(new SmallButton((axis + 3) * 2 + 1, guiLeft + 120, y, "+")); // offset +
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        int field = button.id / 2;
        int delta = (button.id % 2 == 0 ? -1 : 1) * (GuiScreen.isShiftKeyDown() ? 5 : 1);
        Lantern.NETWORK.sendToServer(new WardAdjustMessage(ward.getPos(), field, delta));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRenderer.drawString(I18n.format("gui.lantern.ward"), 8, 6, 0x404040);
        drawCentered(I18n.format("gui.lantern.ward_size"), 44, 15, 0x404040);
        drawCentered(I18n.format("gui.lantern.ward_offset"), 108, 15, 0x404040);
        for (int axis = 0; axis < 3; axis++) {
            int y = ROW_Y[axis] + 2;
            fontRenderer.drawString(AXES[axis], 10, y, 0x404040);
            drawCentered(Integer.toString(ward.getRadius(axis) * 2 + 1), 44, y, 0x404040);
            int offset = ward.getOffset(axis);
            drawCentered((offset > 0 ? "+" : "") + offset, 108, y, 0x404040);
        }
        fontRenderer.drawString(TextFormatting.DARK_PURPLE
            + I18n.format("gui.lantern.ward_limit", ward.cap() * 2 + 1), 8, 70, 0x404040);
    }

    private void drawCentered(String text, int centerX, int y, int color) {
        fontRenderer.drawString(text, centerX - fontRenderer.getStringWidth(text) / 2, y, color);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        mc.getTextureManager().bindTexture(TEXTURE);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
    }

    /** Flat 12x12 +/- button drawn in the vanilla slot palette. */
    private static class SmallButton extends GuiButton {

        SmallButton(int id, int x, int y, String label) {
            super(id, x, y, 12, 12, label);
        }

        @Override
        public void drawButton(net.minecraft.client.Minecraft mc, int mouseX, int mouseY, float partialTicks) {
            if (!visible) {
                return;
            }
            hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
            drawRect(x, y, x + width, y + height, 0xFF373737);
            drawRect(x + 1, y + 1, x + width, y + height, 0xFFFFFFFF);
            drawRect(x + 1, y + 1, x + width - 1, y + height - 1, hovered ? 0xFFA8B0B8 : 0xFF8B8B8B);
            mc.fontRenderer.drawString(displayString,
                x + (width - mc.fontRenderer.getStringWidth(displayString)) / 2 + 1,
                y + (height - 8) / 2 + 1, 0xFF303030);
        }
    }
}

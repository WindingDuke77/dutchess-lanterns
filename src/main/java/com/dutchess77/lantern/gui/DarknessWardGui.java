package com.dutchess77.lantern.gui;

import com.dutchess77.lantern.Lantern;
import com.dutchess77.lantern.block.DarknessWardTileEntity;
import com.dutchess77.lantern.client.WardAreaRenderer;
import com.dutchess77.lantern.network.WardAdjustMessage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Size / offset editor for the Darkness Ward, styled after EnderIO's range
 * widget: values with stacked +/- spinners, and a toggle that highlights the
 * warded area in the world. Values render straight from the client tile
 * entity, which the server keeps synced after every adjustment.
 */
@SideOnly(Side.CLIENT)
public class DarknessWardGui extends GuiContainer {

    private static final ResourceLocation TEXTURE =
        new ResourceLocation(Lantern.MODID, "textures/gui/darkness_ward.png");

    private static final int[] ROW_Y = { 24, 38, 52 };
    private static final String[] AXES = { "X", "Y", "Z" };
    private static final int ID_SHOW_AREA = 100;

    private final DarknessWardTileEntity ward;
    private GuiButton showAreaButton;

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
            int y = ROW_Y[axis];
            addSpinner(axis, guiLeft + 52, guiTop + y);           // radius
            addSpinner(axis + 3, guiLeft + 108, guiTop + y);      // offset
        }
        showAreaButton = addButton(new ShowAreaButton(ID_SHOW_AREA, guiLeft + 126, guiTop + 36));
    }

    /** EnderIO-style spinner: tiny + on top of tiny -, one field. */
    private void addSpinner(int field, int x, int y) {
        addButton(new MiniButton(field * 2 + 1, x, y, "+"));
        addButton(new MiniButton(field * 2, x, y + 7, "-"));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == ID_SHOW_AREA) {
            WardAreaRenderer.toggle(ward.getWorld(), ward.getPos());
            return;
        }
        int field = button.id / 2;
        int delta = (button.id % 2 == 0 ? -1 : 1) * (GuiScreen.isShiftKeyDown() ? 5 : 1);
        Lantern.NETWORK.sendToServer(new WardAdjustMessage(ward.getPos(), field, delta));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        renderHoveredToolTip(mouseX, mouseY);
        if (showAreaButton != null && showAreaButton.isMouseOver()) {
            drawHoveringText(I18n.format(WardAreaRenderer.isShown(ward.getWorld(), ward.getPos())
                ? "gui.lantern.ward_hide" : "gui.lantern.ward_show"), mouseX, mouseY);
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRenderer.drawString(I18n.format("gui.lantern.ward"), 8, 6, 0x404040);
        drawCentered(I18n.format("gui.lantern.ward_size"), 40, 14, 0x404040);
        drawCentered(I18n.format("gui.lantern.ward_offset"), 96, 14, 0x404040);
        for (int axis = 0; axis < 3; axis++) {
            int y = ROW_Y[axis] + 3;
            fontRenderer.drawString(AXES[axis], 10, y, 0x404040);
            drawCentered(Integer.toString(ward.getRadius(axis) * 2 + 1), 36, y, 0x404040);
            int offset = ward.getOffset(axis);
            drawCentered((offset > 0 ? "+" : "") + offset, 92, y, 0x404040);
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
        // wired-charger-style ghost hint: Range upgrades go in the sockets
        ItemStack ghost = new ItemStack(com.dutchess77.lantern.ModItems.RANGE_UPGRADE);
        for (int i = 0; i < DarknessWardTileEntity.SOCKET_COUNT; i++) {
            if (ward.getSockets().getStackInSlot(i).isEmpty()) {
                LanternBenchGui.drawGhost(ghost, guiLeft + 150, guiTop + 9 + i * 18);
            }
        }
    }

    /** Flat 12x7 spinner half drawn in the vanilla slot palette. */
    private static class MiniButton extends GuiButton {

        MiniButton(int id, int x, int y, String label) {
            super(id, x, y, 12, 7, label);
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
            if (!visible) {
                return;
            }
            hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
            drawRect(x, y, x + width, y + height, 0xFF373737);
            drawRect(x + 1, y + 1, x + width, y + height, 0xFFFFFFFF);
            drawRect(x + 1, y + 1, x + width - 1, y + height - 1, hovered ? 0xFFA8B0B8 : 0xFF8B8B8B);
            int cx = x + width / 2;
            int cy = y + height / 2;
            drawRect(cx - 2, cy, cx + 2, cy + 1, 0xFF303030);           // '-'
            if ("+".equals(displayString)) {
                drawRect(cx - 1, cy - 2, cx, cy + 3, 0xFF303030);       // vertical stroke of '+'
            }
        }
    }

    /** Area-highlight toggle: purple box when showing, red slash when hidden. */
    private class ShowAreaButton extends GuiButton {

        ShowAreaButton(int id, int x, int y) {
            super(id, x, y, 16, 16, "");
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
            if (!visible) {
                return;
            }
            hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
            drawRect(x, y, x + width, y + height, 0xFF373737);
            drawRect(x + 1, y + 1, x + width, y + height, 0xFFFFFFFF);
            drawRect(x + 1, y + 1, x + width - 1, y + height - 1, hovered ? 0xFFA8B0B8 : 0xFF8B8B8B);
            boolean shown = WardAreaRenderer.isShown(ward.getWorld(), ward.getPos());
            int box = shown ? 0xFF9040D8 : 0xFF55585C;
            drawRect(x + 4, y + 4, x + 12, y + 5, box);                  // box outline
            drawRect(x + 4, y + 11, x + 12, y + 12, box);
            drawRect(x + 4, y + 5, x + 5, y + 11, box);
            drawRect(x + 11, y + 5, x + 12, y + 11, box);
            if (!shown) {
                for (int i = 0; i < 10; i++) {                           // red slash = hidden
                    drawRect(x + 3 + i, y + 12 - i, x + 4 + i, y + 13 - i, 0xFFC03030);
                }
            }
        }
    }
}

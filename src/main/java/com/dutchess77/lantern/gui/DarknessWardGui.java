package com.dutchess77.lantern.gui;

import java.io.IOException;

import org.lwjgl.input.Mouse;

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
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Size / offset editor for the Darkness Ward, styled after EnderIO's range
 * widget: values with stacked +/- spinners (scroll over a value works too),
 * and a toggle that highlights the warded area in the world. Values render
 * straight from the client tile entity, which the server keeps synced after
 * every adjustment.
 */
@SideOnly(Side.CLIENT)
public class DarknessWardGui extends GuiContainer {

    private static final ResourceLocation TEXTURE =
        new ResourceLocation(Lantern.MODID, "textures/gui/darkness_ward.png");

    private static final int[] ROW_Y = { 28, 46, 64 };
    private static final String[] AXES = { "X", "Y", "Z" };
    private static final int SIZE_VALUE_X = 40;
    private static final int OFFSET_VALUE_X = 100;
    private static final int SIZE_SPINNER_X = 54;
    private static final int OFFSET_SPINNER_X = 114;
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
            int y = guiTop + ROW_Y[axis];
            addSpinner(axis, guiLeft + SIZE_SPINNER_X, y);
            addSpinner(axis + 3, guiLeft + OFFSET_SPINNER_X, y);
        }
        showAreaButton = addButton(new ShowAreaButton(ID_SHOW_AREA, guiLeft + 134, guiTop + 46));
    }

    /** EnderIO-style spinner: small + above small -, one field. */
    private void addSpinner(int field, int x, int y) {
        addButton(new MiniButton(field * 2 + 1, x, y, true));
        addButton(new MiniButton(field * 2, x, y + 9, false));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == ID_SHOW_AREA) {
            WardAreaRenderer.toggle(ward.getWorld(), ward.getPos());
            return;
        }
        adjust(button.id / 2, button.id % 2 == 0 ? -1 : 1);
    }

    private void adjust(int field, int direction) {
        int delta = direction * (GuiScreen.isShiftKeyDown() ? 5 : 1);
        Lantern.NETWORK.sendToServer(new WardAdjustMessage(ward.getPos(), field, delta));
    }

    /** Scrolling over a value or its spinner nudges it (shift = x5). */
    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) {
            return;
        }
        int mx = Mouse.getEventX() * width / mc.displayWidth - guiLeft;
        int my = height - Mouse.getEventY() * height / mc.displayHeight - 1 - guiTop;
        for (int axis = 0; axis < 3; axis++) {
            if (my < ROW_Y[axis] - 1 || my >= ROW_Y[axis] + 18) {
                continue;
            }
            if (mx >= 24 && mx < 68) {
                adjust(axis, wheel > 0 ? 1 : -1);
            } else if (mx >= 84 && mx < 128) {
                adjust(axis + 3, wheel > 0 ? 1 : -1);
            }
        }
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
        for (GuiButton button : buttonList) {
            // size spinners: explain what caps them
            if (button.id < 6 && button.isMouseOver()) {
                drawHoveringText(I18n.format("gui.lantern.ward_limit", ward.cap() * 2 + 1), mouseX, mouseY);
                break;
            }
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        GuiStyle.drawTitle(fontRenderer, I18n.format("gui.lantern.ward"));
        drawCentered(I18n.format("gui.lantern.ward_size"), SIZE_VALUE_X + 10, 18, 0x404040);
        drawCentered(I18n.format("gui.lantern.ward_offset"), OFFSET_VALUE_X + 10, 18, 0x404040);
        for (int axis = 0; axis < 3; axis++) {
            int y = ROW_Y[axis] + 5;
            fontRenderer.drawString(AXES[axis], 10, y, 0x404040);
            drawCentered(Integer.toString(ward.getRadius(axis) * 2 + 1), SIZE_VALUE_X, y, 0x404040);
            int offset = ward.getOffset(axis);
            drawCentered((offset > 0 ? "+" : "") + offset, OFFSET_VALUE_X, y, 0x404040);
        }
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

    /** Raised 12x8 spinner half with a +/- glyph. */
    private static class MiniButton extends GuiButton {

        private final boolean plus;

        MiniButton(int id, int x, int y, boolean plus) {
            super(id, x, y, 12, 8, "");
            this.plus = plus;
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
            if (!visible) {
                return;
            }
            hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
            GuiStyle.paintRaised(x, y, width, height, hovered);
            drawRect(x + 4, y + 4, x + 9, y + 5, 0xFF1E1E1E);      // '-'
            if (plus) {
                drawRect(x + 6, y + 2, x + 7, y + 7, 0xFF1E1E1E);  // vertical stroke of '+'
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
            GuiStyle.paintRaised(x, y, width, height, hovered);
            boolean shown = WardAreaRenderer.isShown(ward.getWorld(), ward.getPos());
            int box = shown ? 0xFF9040D8 : 0xFF55585C;
            drawRect(x + 4, y + 4, x + 12, y + 5, box);
            drawRect(x + 4, y + 11, x + 12, y + 12, box);
            drawRect(x + 4, y + 5, x + 5, y + 11, box);
            drawRect(x + 11, y + 5, x + 12, y + 11, box);
            if (!shown) {
                for (int i = 0; i < 10; i++) {
                    drawRect(x + 3 + i, y + 12 - i, x + 4 + i, y + 13 - i, 0xFFC03030);
                }
            }
        }
    }
}

package com.dutchess77.lantern.gui;

import java.util.ArrayList;
import java.util.List;

import com.dutchess77.lantern.Lantern;
import com.dutchess77.lantern.ModItems;
import com.dutchess77.lantern.block.DarknessWardTileEntity;
import com.dutchess77.lantern.client.WardAreaRenderer;
import com.dutchess77.lantern.item.UpgradeType;
import com.dutchess77.lantern.network.WardAdjustMessage;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Size / offset editor for the Darkness Ward, styled after EnderIO's range
 * widget: values with stacked +/- spinners (scroll over a value works too),
 * and a toggle that highlights the warded area in the world. Values render
 * straight from the client block entity, which the server keeps synced after
 * every adjustment.
 */
public class DarknessWardGui extends AbstractContainerScreen<DarknessWardContainer> {

    private static final ResourceLocation TEXTURE =
        ResourceLocation.fromNamespaceAndPath(Lantern.MODID, "textures/gui/darkness_ward.png");

    private static final int[] ROW_Y = { 28, 46, 64 };
    private static final String[] AXES = { "X", "Y", "Z" };
    private static final int SIZE_VALUE_X = 40;
    private static final int OFFSET_VALUE_X = 100;
    private static final int SIZE_SPINNER_X = 54;
    private static final int OFFSET_SPINNER_X = 114;

    private final DarknessWardTileEntity ward;
    private ShowAreaButton showAreaButton;
    private final List<MiniButton> sizeSpinners = new ArrayList<>();

    public DarknessWardGui(DarknessWardContainer menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.ward = menu.getWard();
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        sizeSpinners.clear();
        for (int axis = 0; axis < 3; axis++) {
            int y = topPos + ROW_Y[axis];
            addSpinner(axis, leftPos + SIZE_SPINNER_X, y, true);
            addSpinner(axis + 3, leftPos + OFFSET_SPINNER_X, y, false);
        }
        showAreaButton = addRenderableWidget(new ShowAreaButton(leftPos + 134, topPos + 46));
    }

    /** EnderIO-style spinner: small + above small -, one field. */
    private void addSpinner(int field, int x, int y, boolean size) {
        MiniButton up = addRenderableWidget(new MiniButton(x, y, true, () -> adjust(field, 1)));
        MiniButton down = addRenderableWidget(new MiniButton(x, y + 9, false, () -> adjust(field, -1)));
        if (size) {
            sizeSpinners.add(up);
            sizeSpinners.add(down);
        }
    }

    private void adjust(int field, int direction) {
        int delta = direction * (hasShiftDown() ? 5 : 1);
        PacketDistributor.sendToServer(new WardAdjustMessage(ward.getBlockPos(), field, delta));
    }

    /** Scrolling over a value or its spinner nudges it (shift = x5). */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
            return true;
        }
        if (scrollY == 0.0D) {
            return false;
        }
        double mx = mouseX - leftPos;
        double my = mouseY - topPos;
        boolean handled = false;
        for (int axis = 0; axis < 3; axis++) {
            if (my < ROW_Y[axis] - 1 || my >= ROW_Y[axis] + 18) {
                continue;
            }
            if (mx >= 24 && mx < 68) {
                adjust(axis, scrollY > 0 ? 1 : -1);
                handled = true;
            } else if (mx >= 84 && mx < 128) {
                adjust(axis + 3, scrollY > 0 ? 1 : -1);
                handled = true;
            }
        }
        return handled;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // renderBackground (and renderBg with it) already runs via renderWithTooltip in 1.21
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
        if (showAreaButton != null && showAreaButton.isHovered()) {
            guiGraphics.renderTooltip(font, Component.translatable(
                WardAreaRenderer.isShown(ward.getLevel(), ward.getBlockPos())
                    ? "gui.lantern.ward_hide" : "gui.lantern.ward_show"), mouseX, mouseY);
            return;
        }
        // size spinners: explain what caps them
        for (MiniButton button : sizeSpinners) {
            if (button.isHovered()) {
                guiGraphics.renderTooltip(font,
                    Component.translatable("gui.lantern.ward_limit", ward.cap() * 2 + 1), mouseX, mouseY);
                break;
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        GuiStyle.drawTitle(guiGraphics, font, title);
        drawCentered(guiGraphics, Component.translatable("gui.lantern.ward_size").getString(),
            SIZE_VALUE_X + 10, 18, 0x404040);
        drawCentered(guiGraphics, Component.translatable("gui.lantern.ward_offset").getString(),
            OFFSET_VALUE_X + 10, 18, 0x404040);
        for (int axis = 0; axis < 3; axis++) {
            int y = ROW_Y[axis] + 5;
            guiGraphics.drawString(font, AXES[axis], 10, y, 0x404040, false);
            drawCentered(guiGraphics, Integer.toString(ward.getRadius(axis) * 2 + 1), SIZE_VALUE_X, y, 0x404040);
            int offset = ward.getOffset(axis);
            drawCentered(guiGraphics, (offset > 0 ? "+" : "") + offset, OFFSET_VALUE_X, y, 0x404040);
        }
    }

    private void drawCentered(GuiGraphics guiGraphics, String text, int centerX, int y, int color) {
        guiGraphics.drawString(font, text, centerX - font.width(text) / 2, y, color, false);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        // wired-charger-style ghost hint: Range upgrades go in the sockets
        ItemStack ghost = new ItemStack(ModItems.upgradeFor(UpgradeType.RANGE, 1));
        for (int i = 0; i < DarknessWardTileEntity.SOCKET_COUNT; i++) {
            if (ward.getSockets().getStackInSlot(i).isEmpty()) {
                LanternBenchGui.drawGhost(guiGraphics, ghost, leftPos + 150, topPos + 9 + i * 18);
            }
        }
    }

    /** Raised 12x8 spinner half with a +/- glyph. */
    private static class MiniButton extends AbstractButton {

        private final boolean plus;
        private final Runnable action;

        MiniButton(int x, int y, boolean plus, Runnable action) {
            super(x, y, 12, 8, Component.empty());
            this.plus = plus;
            this.action = action;
        }

        @Override
        public void onPress() {
            action.run();
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            GuiStyle.paintRaised(guiGraphics, getX(), getY(), width, height, isHovered());
            guiGraphics.fill(getX() + 4, getY() + 4, getX() + 9, getY() + 5, 0xFF1E1E1E);      // '-'
            if (plus) {
                guiGraphics.fill(getX() + 6, getY() + 2, getX() + 7, getY() + 7, 0xFF1E1E1E);  // vertical stroke of '+'
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
            defaultButtonNarrationText(narrationElementOutput);
        }
    }

    /** Area-highlight toggle: purple box when showing, red slash when hidden. */
    private class ShowAreaButton extends AbstractButton {

        ShowAreaButton(int x, int y) {
            super(x, y, 16, 16, Component.empty());
        }

        @Override
        public void onPress() {
            WardAreaRenderer.toggle(ward.getLevel(), ward.getBlockPos());
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            int x = getX();
            int y = getY();
            GuiStyle.paintRaised(guiGraphics, x, y, width, height, isHovered());
            boolean shown = WardAreaRenderer.isShown(ward.getLevel(), ward.getBlockPos());
            int box = shown ? 0xFF9040D8 : 0xFF55585C;
            guiGraphics.fill(x + 4, y + 4, x + 12, y + 5, box);
            guiGraphics.fill(x + 4, y + 11, x + 12, y + 12, box);
            guiGraphics.fill(x + 4, y + 5, x + 5, y + 11, box);
            guiGraphics.fill(x + 11, y + 5, x + 12, y + 11, box);
            if (!shown) {
                for (int i = 0; i < 10; i++) {
                    guiGraphics.fill(x + 3 + i, y + 12 - i, x + 4 + i, y + 13 - i, 0xFFC03030);
                }
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
            defaultButtonNarrationText(narrationElementOutput);
        }
    }
}

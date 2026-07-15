package com.dutchess77.lantern.gui;

import com.dutchess77.lantern.Lantern;
import com.dutchess77.lantern.ModItems;
import com.dutchess77.lantern.block.LanternBenchTileEntity;
import com.dutchess77.lantern.item.LanternItem;
import com.dutchess77.lantern.item.LanternUpgrades;
import com.dutchess77.lantern.item.UpgradeType;

import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Bench screen. The 1.12.2 Baubles side panel is gone (no Baubles on 1.21),
 * so the panel is the plain 176-wide layout.
 */
public class LanternBenchGui extends AbstractContainerScreen<LanternBenchContainer> {

    private static final ResourceLocation TEXTURE =
        ResourceLocation.fromNamespaceAndPath(Lantern.MODID, "textures/gui/lantern_bench.png");

    /** Same layout as the container: center, then N/W/E/S sockets. */
    private static final int[][] SLOT_POSITIONS = { {80, 35}, {80, 13}, {58, 35}, {102, 35}, {80, 57} };

    /** Width of the main panel. */
    private static final int PANEL_WIDTH = 176;

    private final LanternBenchTileEntity bench;

    /** Wired-charger-style ghost hints: what belongs in each empty slot. */
    private final ItemStack[] centerGhosts = {
        new ItemStack(ModItems.LANTERN.get()), new ItemStack(ModItems.ENERGY_LANTERN.get()),
        new ItemStack(ModItems.TORCH_LANTERN.get()), new ItemStack(ModItems.GLOW_WAND.get()),
        new ItemStack(ModItems.ENERGY_GLOW_WAND.get())
    };
    private final ItemStack[] socketGhosts = {
        new ItemStack(ModItems.upgradeFor(UpgradeType.RANGE, 1)),
        new ItemStack(ModItems.upgradeFor(UpgradeType.EFFICIENCY, 1)),
        new ItemStack(ModItems.upgradeFor(UpgradeType.CAPACITY, 1))
    };

    public LanternBenchGui(LanternBenchContainer menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.bench = menu.getBench();
        this.imageWidth = PANEL_WIDTH;
        this.imageHeight = 166;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // renderBackground (and renderBg with it) already runs via renderWithTooltip in 1.21
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        GuiStyle.drawTitle(guiGraphics, font, title);
        ItemStack lantern = bench.getInventory().getStackInSlot(LanternBenchTileEntity.SLOT_LANTERN);
        Component status = lantern.getItem() instanceof LanternItem
            ? Component.translatable("gui.lantern.sockets",
                LanternUpgrades.list(lantern).size() + occupiedSockets(), LanternUpgrades.socketCount(lantern))
            : Component.translatable("gui.lantern.insert");
        guiGraphics.drawString(font, status, 8, 72, 0x404040, false);
    }

    private int occupiedSockets() {
        int occupied = 0;
        for (int i = LanternBenchTileEntity.SOCKET_FIRST;
             i < LanternBenchTileEntity.SOCKET_FIRST + LanternBenchTileEntity.SOCKET_COUNT; i++) {
            if (!bench.getInventory().getStackInSlot(i).isEmpty()) {
                occupied++;
            }
        }
        return occupied;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, PANEL_WIDTH, imageHeight);
        int cycle = (int) (Util.getMillis() / 1200L);
        if (bench.getInventory().getStackInSlot(LanternBenchTileEntity.SLOT_LANTERN).isEmpty()) {
            drawGhost(guiGraphics, centerGhosts[cycle % centerGhosts.length],
                leftPos + SLOT_POSITIONS[0][0], topPos + SLOT_POSITIONS[0][1]);
        }
        for (int i = 0; i < LanternBenchTileEntity.SOCKET_COUNT; i++) {
            if (bench.getInventory().getStackInSlot(LanternBenchTileEntity.SOCKET_FIRST + i).isEmpty()) {
                drawGhost(guiGraphics, socketGhosts[(cycle + i) % socketGhosts.length],
                    leftPos + SLOT_POSITIONS[1 + i][0], topPos + SLOT_POSITIONS[1 + i][1]);
            }
        }
    }

    /** Renders a faded item as a slot hint, washed toward the slot gray. */
    static void drawGhost(GuiGraphics guiGraphics, ItemStack stack, int x, int y) {
        guiGraphics.renderItem(stack, x, y);
        // z=300 sits above the item (~200) but below tooltips (400)
        guiGraphics.fill(x, y, x + 16, y + 16, 300, 0xB08B8B8B);
    }
}

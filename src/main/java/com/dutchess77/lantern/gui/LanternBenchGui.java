package com.dutchess77.lantern.gui;

import com.dutchess77.lantern.Lantern;
import com.dutchess77.lantern.block.LanternBenchTileEntity;
import com.dutchess77.lantern.item.LanternItem;
import com.dutchess77.lantern.item.LanternUpgrades;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class LanternBenchGui extends GuiContainer {

    private static final ResourceLocation TEXTURE =
        new ResourceLocation(Lantern.MODID, "textures/gui/lantern_bench.png");

    private static final boolean BAUBLES = Loader.isModLoaded("baubles");

    private final LanternBenchTileEntity bench;

    public LanternBenchGui(InventoryPlayer playerInventory, LanternBenchTileEntity bench) {
        super(new LanternBenchContainer(playerInventory, bench));
        this.bench = bench;
        this.xSize = 176;
        this.ySize = 166;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRenderer.drawString(I18n.format("gui.lantern.bench"), 8, 6, 0x404040);
        if (BAUBLES) {
            String baubles = TextFormatting.DARK_PURPLE + I18n.format("gui.lantern.baubles");
            fontRenderer.drawString(baubles, xSize - 8 - fontRenderer.getStringWidth(baubles), 6, 0x404040);
        }
        ItemStack lantern = bench.getInventory().getStackInSlot(LanternBenchTileEntity.SLOT_LANTERN);
        String status = lantern.getItem() instanceof LanternItem
            ? I18n.format("gui.lantern.sockets",
                LanternUpgrades.list(lantern).size() + occupiedSockets(), LanternUpgrades.socketCount(lantern))
            : I18n.format("gui.lantern.insert");
        fontRenderer.drawString(status, 8, 72, 0x404040);
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
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        mc.getTextureManager().bindTexture(TEXTURE);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
    }
}

package com.dutchess77.lantern.gui;

import java.io.IOException;

import com.dutchess77.lantern.Lantern;
import com.dutchess77.lantern.block.LanternBenchTileEntity;
import com.dutchess77.lantern.item.LanternItem;
import com.dutchess77.lantern.item.LanternUpgrades;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class LanternBenchGui extends GuiContainer {

    private static final ResourceLocation TEXTURE =
        new ResourceLocation(Lantern.MODID, "textures/gui/lantern_bench.png");

    private final LanternBenchTileEntity bench;

    public LanternBenchGui(InventoryPlayer playerInventory, LanternBenchTileEntity bench) {
        super(new LanternBenchContainer(playerInventory, bench));
        this.bench = bench;
        this.xSize = 176;
        this.ySize = 166;
    }

    @Override
    public void initGui() {
        super.initGui();
        buttonList.add(new GuiButton(0, guiLeft + 120, guiTop + 30, 48, 20,
            I18n.format("gui.lantern.clear")));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0) {
            mc.playerController.sendEnchantPacket(inventorySlots.windowId, 0);
        }
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
        ItemStack lantern = bench.getInventory().getStackInSlot(LanternBenchTileEntity.SLOT_LANTERN);
        String sockets = lantern.getItem() instanceof LanternItem
            ? I18n.format("gui.lantern.sockets",
                LanternUpgrades.list(lantern).size(), LanternUpgrades.socketCount(lantern))
            : I18n.format("gui.lantern.insert");
        fontRenderer.drawString(sockets, 8, 60, 0x404040);
        fontRenderer.drawString(I18n.format("container.inventory"), 8, ySize - 94, 0x404040);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        mc.getTextureManager().bindTexture(TEXTURE);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
    }
}

package com.dutchess77.lantern.gui;

import com.dutchess77.lantern.Lantern;
import com.dutchess77.lantern.block.LanternBenchTileEntity;
import com.dutchess77.lantern.item.LanternItem;
import com.dutchess77.lantern.item.LanternUpgrades;

import com.dutchess77.lantern.ModItems;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
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

    /** Wired-charger-style ghost hints: what belongs in each empty slot. */
    private static final ItemStack[] CENTER_GHOSTS = {
        new ItemStack(ModItems.LANTERN), new ItemStack(ModItems.ENERGY_LANTERN),
        new ItemStack(ModItems.TORCH_LANTERN), new ItemStack(ModItems.GLOW_WAND),
        new ItemStack(ModItems.ENERGY_GLOW_WAND)
    };
    private static final ItemStack[] SOCKET_GHOSTS = {
        new ItemStack(ModItems.RANGE_UPGRADE), new ItemStack(ModItems.EFFICIENCY_UPGRADE),
        new ItemStack(ModItems.CAPACITY_UPGRADE)
    };
    /** Same layout as the container: center, then N/W/E/S sockets. */
    private static final int[][] SLOT_POSITIONS = { {80, 35}, {80, 13}, {58, 35}, {102, 35}, {80, 57} };

    /** Slot-type silhouettes inside Baubles' own GUI texture (amulet..charm). */
    private static final ResourceLocation BAUBLES_TEXTURE =
        new ResourceLocation("baubles", "textures/gui/expanded_inventory.png");
    private static final int[][] BAUBLE_ICON_UV =
        { {77, 8}, {77, 26}, {77, 44}, {77, 62}, {97, 8}, {97, 26}, {97, 44} };

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
            fontRenderer.drawString(baubles, xSize - 8 - fontRenderer.getStringWidth(baubles), 72, 0x404040);
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
        int cycle = (int) (Minecraft.getSystemTime() / 1200L);
        if (bench.getInventory().getStackInSlot(LanternBenchTileEntity.SLOT_LANTERN).isEmpty()) {
            drawGhost(CENTER_GHOSTS[cycle % CENTER_GHOSTS.length],
                guiLeft + SLOT_POSITIONS[0][0], guiTop + SLOT_POSITIONS[0][1]);
        }
        for (int i = 0; i < LanternBenchTileEntity.SOCKET_COUNT; i++) {
            if (bench.getInventory().getStackInSlot(LanternBenchTileEntity.SOCKET_FIRST + i).isEmpty()) {
                drawGhost(SOCKET_GHOSTS[(cycle + i) % SOCKET_GHOSTS.length],
                    guiLeft + SLOT_POSITIONS[1 + i][0], guiTop + SLOT_POSITIONS[1 + i][1]);
            }
        }
        drawBaublesPanel();
    }

    /** Side column of the player's worn baubles, attached to the right edge. */
    private void drawBaublesPanel() {
        int count = ((LanternBenchContainer) inventorySlots).getBaubleCount();
        if (count <= 0) {
            return;
        }
        int x0 = guiLeft + 174;
        int x1 = guiLeft + 205;
        int y0 = guiTop + 8;
        int y1 = guiTop + 19 + count * 18;
        drawRect(x0, y0, x1, y1, 0xFFC6C6C6);
        drawRect(x0, y0, x1 - 1, y0 + 2, 0xFFFFFFFF);     // light top
        drawRect(x1 - 2, y0 + 1, x1, y1, 0xFF555555);     // dark right
        drawRect(x0 + 1, y1 - 2, x1 - 1, y1, 0xFF555555); // dark bottom
        for (int i = 0; i < count; i++) {
            drawSlotInset(guiLeft + LanternBenchContainer.BAUBLE_X - 1,
                guiTop + LanternBenchContainer.BAUBLE_Y - 1 + i * 18);
        }
        // slot-type silhouettes for empty slots, straight from Baubles' texture
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F); // drawRect leaves its fill color set
        mc.getTextureManager().bindTexture(BAUBLES_TEXTURE);
        int firstBauble = 1 + LanternBenchTileEntity.SOCKET_COUNT + 36;
        for (int i = 0; i < count && i < BAUBLE_ICON_UV.length; i++) {
            if (!inventorySlots.getSlot(firstBauble + i).getHasStack()) {
                drawTexturedModalRect(guiLeft + LanternBenchContainer.BAUBLE_X,
                    guiTop + LanternBenchContainer.BAUBLE_Y + i * 18,
                    BAUBLE_ICON_UV[i][0], BAUBLE_ICON_UV[i][1], 16, 16);
            }
        }
    }

    private void drawSlotInset(int x, int y) {
        drawRect(x, y, x + 17, y + 1, 0xFF373737);
        drawRect(x, y + 1, x + 1, y + 17, 0xFF373737);
        drawRect(x + 1, y + 17, x + 18, y + 18, 0xFFFFFFFF);
        drawRect(x + 17, y + 1, x + 18, y + 17, 0xFFFFFFFF);
        drawRect(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);
    }

    /** Renders a faded item as a slot hint, washed toward the slot gray. */
    static void drawGhost(ItemStack stack, int x, int y) {
        RenderHelper.enableGUIStandardItemLighting();
        Minecraft.getMinecraft().getRenderItem().renderItemIntoGUI(stack, x, y);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableDepth();
        drawRect(x, y, x + 16, y + 16, 0xB08B8B8B);
        GlStateManager.enableDepth();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }
}

package com.dutchess77.lantern.item;

import com.dutchess77.lantern.LanternConfig;

import net.minecraft.client.resources.I18n;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Lantern of Paranoia, but tidy: places plain torches instead of buried
 * glowstone, snapped to the same global grid with the same gap-fill pass.
 * Needs no EnderIO and never sweeps torches up.
 */
public class TorchLanternItem extends LanternItem {

    public TorchLanternItem() {
        super("torch_lantern");
    }

    @Override
    protected Item fuelItem() {
        return Item.getItemFromBlock(Blocks.TORCH);
    }

    @Override
    protected String chargeChatKey() {
        return "chat.lantern.torches";
    }

    @Override
    public int getRGBDurabilityForDisplay(ItemStack stack) {
        return 0xFFA030;
    }

    @Override
    @SideOnly(Side.CLIENT)
    protected String describeFuel(ItemStack stack) {
        return TextFormatting.GOLD
            + I18n.format("tooltip.lantern.torches", getCharge(stack), LanternConfig.bufferCapacity);
    }

    @Override
    @SideOnly(Side.CLIENT)
    protected String describeCost() {
        return I18n.format("tooltip.lantern.cost_torch");
    }

    @Override
    protected String howtoFillKey() {
        return "tooltip.lantern.howto2_torch";
    }
}

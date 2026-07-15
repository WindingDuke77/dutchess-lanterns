package com.dutchess77.lantern.item;

import com.dutchess77.lantern.LanternConfig;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Lantern of Paranoia, but tidy: places plain torches instead of buried
 * glowstone, snapped to the same global grid with the same gap-fill pass.
 * Needs no EnderIO and never sweeps torches up.
 */
public class TorchLanternItem extends LanternItem {

    public TorchLanternItem() {
        super();
    }

    @Override
    protected Item fuelItem() {
        return Items.TORCH;
    }

    @Override
    protected String chargeChatKey() {
        return "chat.lantern.torches";
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xFFA030;
    }

    @Override
    protected MutableComponent describeFuel(ItemStack stack) {
        return Component.translatable("tooltip.lantern.torches", getCharge(stack), LanternConfig.bufferCapacity)
            .withStyle(ChatFormatting.GOLD);
    }

    @Override
    protected MutableComponent describeCost() {
        return Component.translatable("tooltip.lantern.cost_torch");
    }

    @Override
    protected String howtoFillKey() {
        return "tooltip.lantern.howto2_torch";
    }
}

package com.dutchess77.lantern.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Creative-only lantern: works exactly like the normal Lantern (hidden camo
 * lights) but costs nothing. Its lights never drop glowstone. No recipe.
 */
public class CreativeLanternItem extends LanternItem {

    public CreativeLanternItem() {
        super();
    }

    @Override
    protected void fill(Player player, ItemStack stack) {
        player.displayClientMessage(Component.translatable("chat.lantern.creative"), true);
    }

    @Override
    public boolean consumePlacementCost(Player player, ItemStack stack) {
        return true;
    }

    @Override
    public boolean lightsRefundGlowstone() {
        return false; // free lights owe nobody a refund
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return false;
    }

    @Override
    protected MutableComponent describeFuel(ItemStack stack) {
        return Component.translatable("tooltip.lantern.unlimited").withStyle(ChatFormatting.LIGHT_PURPLE);
    }

    @Override
    protected MutableComponent describeCost() {
        return Component.translatable("tooltip.lantern.cost_creative");
    }

    @Override
    protected String howtoFillKey() {
        return "tooltip.lantern.howto2_creative";
    }
}

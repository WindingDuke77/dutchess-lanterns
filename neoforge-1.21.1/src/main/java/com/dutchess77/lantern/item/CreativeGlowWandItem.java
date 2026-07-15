package com.dutchess77.lantern.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Creative-only Glow Wand: swaps cost nothing. Its lights are marked FE-paid
 * so reverting or mining them refunds/drops no glowstone. No recipe.
 */
public class CreativeGlowWandItem extends GlowWandItem {

    public CreativeGlowWandItem() {
        super();
    }

    @Override
    public boolean consumePlacementCost(Player player, ItemStack stack) {
        return true;
    }

    @Override
    protected boolean paysWithEnergy() {
        return true; // free lights owe nobody a refund
    }

    @Override
    protected void fill(Player player, ItemStack stack) {
        // nothing to fill
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
        return "tooltip.lantern.wand_fill_creative";
    }
}

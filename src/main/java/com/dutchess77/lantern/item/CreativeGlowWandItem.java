package com.dutchess77.lantern.item;

import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Creative-only Glow Wand: swaps cost nothing. Its lights are marked FE-paid
 * so reverting or mining them refunds/drops no glowstone. No recipe.
 */
public class CreativeGlowWandItem extends GlowWandItem {

    public CreativeGlowWandItem() {
        super("creative_glow_wand");
    }

    @Override
    public boolean consumePlacementCost(EntityPlayer player, ItemStack stack) {
        return true;
    }

    @Override
    protected boolean paysWithEnergy() {
        return true; // free lights owe nobody a refund
    }

    @Override
    protected void fill(EntityPlayer player, ItemStack stack) {
        // nothing to fill
    }

    @Override
    public boolean showDurabilityBar(ItemStack stack) {
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    protected String describeFuel(ItemStack stack) {
        return TextFormatting.LIGHT_PURPLE + I18n.format("tooltip.lantern.unlimited");
    }

    @Override
    @SideOnly(Side.CLIENT)
    protected String describeCost() {
        return I18n.format("tooltip.lantern.cost_creative");
    }

    @Override
    protected String howtoFillKey() {
        return "tooltip.lantern.wand_fill_creative";
    }
}

package com.dutchess77.lantern.item;

import com.dutchess77.lantern.LanternConfig;

import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Creative-only lantern: works exactly like the normal Lantern (hidden camo
 * lights) but costs nothing. Its lights never drop glowstone. No recipe.
 */
public class CreativeLanternItem extends LanternItem {

    public CreativeLanternItem() {
        this("creative_lantern");
    }

    protected CreativeLanternItem(String name) {
        super(name);
    }

    @Override
    protected void fill(EntityPlayer player, ItemStack stack) {
        player.sendStatusMessage(new TextComponentTranslation("chat.lantern.creative"), true);
    }

    @Override
    public boolean consumePlacementCost(EntityPlayer player, ItemStack stack) {
        return true;
    }

    @Override
    public boolean lightsRefundGlowstone() {
        return false; // free lights owe nobody a refund
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
        return "tooltip.lantern.howto2_creative";
    }
}

package com.dutchess77.lantern.item;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentTranslation;

/**
 * Dev tool: free like the Creative Lantern, but places plain visible
 * Glowstone blocks so placements are easy to eyeball. No recipe.
 */
public class DevLanternItem extends CreativeLanternItem {

    public DevLanternItem() {
        super("dev_lantern");
    }

    @Override
    protected void fill(EntityPlayer player, ItemStack stack) {
        player.sendStatusMessage(new TextComponentTranslation("chat.lantern.dev"), true);
    }

    @Override
    protected String howtoFillKey() {
        return "tooltip.lantern.howto2_dev";
    }
}

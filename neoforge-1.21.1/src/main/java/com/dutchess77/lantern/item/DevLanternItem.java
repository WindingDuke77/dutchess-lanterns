package com.dutchess77.lantern.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Dev tool: free like the Creative Lantern, but places plain visible
 * Glowstone blocks so placements are easy to eyeball. No recipe.
 */
public class DevLanternItem extends CreativeLanternItem {

    public DevLanternItem() {
        super();
    }

    @Override
    protected void fill(Player player, ItemStack stack) {
        player.displayClientMessage(Component.translatable("chat.lantern.dev"), true);
    }

    @Override
    protected String howtoFillKey() {
        return "tooltip.lantern.howto2_dev";
    }
}

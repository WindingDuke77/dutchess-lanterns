package com.dutchess77.lantern.item;

import com.dutchess77.lantern.LanternConfig;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/**
 * Glow Wand variant powered by Forge Energy instead of Glowstone, mirroring
 * the Energy Lantern. Charge it in any FE item charger.
 */
public class EnergyGlowWandItem extends GlowWandItem {

    public EnergyGlowWandItem() {
        super();
    }

    @Override
    protected boolean paysWithEnergy() {
        return true;
    }

    @Override
    protected void fill(Player player, ItemStack stack) {
        // no inventory fuel and no status chatter - the durability bar shows the charge
    }

    @Override
    public boolean consumePlacementCost(Player player, ItemStack stack) {
        if (player.getAbilities().instabuild && LanternConfig.freeInCreative) {
            return true;
        }
        if (player.getRandom().nextFloat() < LanternUpgrades.freeChance(stack)) {
            return true; // Efficiency upgrade proc
        }
        int cost = LanternConfig.energyPerLight;
        int stored = EnergyLanternItem.getEnergy(stack);
        if (stored < cost) {
            return false;
        }
        EnergyLanternItem.setEnergy(stack, stored - cost);
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int capacity = Math.max(1, EnergyLanternItem.energyCapacityOf(stack));
        return Math.round(13.0F * Math.min(EnergyLanternItem.getEnergy(stack), capacity) / capacity);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x3AC6D8;
    }

    @Override
    protected MutableComponent describeFuel(ItemStack stack) {
        return Component.translatable("tooltip.lantern.energy",
            EnergyLanternItem.formatFE(EnergyLanternItem.getEnergy(stack)),
            EnergyLanternItem.formatFE(EnergyLanternItem.energyCapacityOf(stack)))
            .withStyle(ChatFormatting.RED);
    }

    @Override
    protected MutableComponent describeCost() {
        return Component.translatable("tooltip.lantern.cost_wand_energy",
            EnergyLanternItem.formatFE(LanternConfig.energyPerLight));
    }

    @Override
    protected String howtoFillKey() {
        return "tooltip.lantern.howto2_energy";
    }

    @Override
    protected void refundReclaimed(Player player, ItemStack lantern, int count) {
        // energy can't hold glowstone - hand the blocks back as items
        while (count > 0) {
            int give = Math.min(64, count);
            ItemHandlerHelper.giveItemToPlayer(player, new ItemStack(Blocks.GLOWSTONE, give));
            count -= give;
        }
    }
}

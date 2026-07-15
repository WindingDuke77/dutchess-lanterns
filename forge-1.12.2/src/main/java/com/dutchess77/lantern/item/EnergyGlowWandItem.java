package com.dutchess77.lantern.item;

import javax.annotation.Nullable;

import com.dutchess77.lantern.LanternConfig;

import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Glow Wand variant powered by Forge Energy instead of Glowstone, mirroring
 * the Energy Lantern. Charge it in any FE item charger.
 */
public class EnergyGlowWandItem extends GlowWandItem {

    public EnergyGlowWandItem() {
        super("energy_glow_wand");
    }

    @Override
    protected boolean paysWithEnergy() {
        return true;
    }

    @Override
    protected void fill(EntityPlayer player, ItemStack stack) {
        // no inventory fuel and no status chatter - the durability bar shows the charge
    }

    @Override
    public boolean consumePlacementCost(EntityPlayer player, ItemStack stack) {
        if (player.capabilities.isCreativeMode && LanternConfig.freeInCreative) {
            return true;
        }
        if (player.getRNG().nextFloat() < LanternUpgrades.freeChance(stack)) {
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
    public double getDurabilityForDisplay(ItemStack stack) {
        int capacity = Math.max(1, EnergyLanternItem.energyCapacityOf(stack));
        return 1.0D - Math.min(EnergyLanternItem.getEnergy(stack), capacity) / (double) capacity;
    }

    @Override
    public int getRGBDurabilityForDisplay(ItemStack stack) {
        return 0x3AC6D8;
    }

    @Override
    @SideOnly(Side.CLIENT)
    protected String describeFuel(ItemStack stack) {
        return TextFormatting.RED + I18n.format("tooltip.lantern.energy",
            EnergyLanternItem.formatFE(EnergyLanternItem.getEnergy(stack)),
            EnergyLanternItem.formatFE(EnergyLanternItem.energyCapacityOf(stack)));
    }

    @Override
    @SideOnly(Side.CLIENT)
    protected String describeCost() {
        return I18n.format("tooltip.lantern.cost_wand_energy",
            EnergyLanternItem.formatFE(LanternConfig.energyPerLight));
    }

    @Override
    protected String howtoFillKey() {
        return "tooltip.lantern.howto2_energy";
    }

    @Override
    protected void refundReclaimed(EntityPlayer player, ItemStack lantern, int count) {
        // energy can't hold glowstone - hand the blocks back as items
        while (count > 0) {
            int give = Math.min(64, count);
            net.minecraftforge.items.ItemHandlerHelper.giveItemToPlayer(player,
                new ItemStack(net.minecraft.init.Blocks.GLOWSTONE, give));
            count -= give;
        }
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable NBTTagCompound nbt) {
        return EnergyLanternItem.newEnergyProvider(stack);
    }
}

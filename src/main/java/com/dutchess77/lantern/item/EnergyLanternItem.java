package com.dutchess77.lantern.item;

import javax.annotation.Nullable;

import com.dutchess77.lantern.LanternConfig;

import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Lantern variant powered by Forge Energy instead of Glowstone. Charge it in
 * any FE item charger (EnderIO capacitor banks, etc.).
 */
public class EnergyLanternItem extends LanternItem {

    private static final String TAG_ENERGY = "Energy";

    public EnergyLanternItem() {
        super("energy_lantern");
    }

    @Override
    protected void fill(EntityPlayer player, ItemStack stack) {
        // no inventory fuel; just report the charge level
        player.sendStatusMessage(new TextComponentTranslation("chat.lantern.energy",
            formatFE(getEnergy(stack)), formatFE(LanternConfig.energyCapacity)), true);
    }

    /** 1234 -> "1.23k", 200000 -> "200k", 1500000 -> "1.5M"; at most two decimals. */
    public static String formatFE(int fe) {
        if (fe >= 1_000_000) {
            return trimDecimals(fe / 1_000_000.0D) + "M";
        }
        if (fe >= 1_000) {
            return trimDecimals(fe / 1_000.0D) + "k";
        }
        return Integer.toString(fe);
    }

    private static String trimDecimals(double value) {
        String s = String.format(java.util.Locale.ROOT, "%.2f", value);
        while (s.endsWith("0")) {
            s = s.substring(0, s.length() - 1);
        }
        if (s.endsWith(".")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    @Override
    public boolean consumePlacementCost(EntityPlayer player, ItemStack stack) {
        if (player.capabilities.isCreativeMode) {
            return true;
        }
        int cost = LanternConfig.energyPerLight;
        int stored = getEnergy(stack);
        if (stored < cost) {
            return false;
        }
        setEnergy(stack, stored - cost);
        return true;
    }

    @Override
    public double getDurabilityForDisplay(ItemStack stack) {
        int capacity = Math.max(1, LanternConfig.energyCapacity);
        return 1.0D - Math.min(getEnergy(stack), capacity) / (double) capacity;
    }

    @Override
    public int getRGBDurabilityForDisplay(ItemStack stack) {
        return 0x3AC6D8;
    }

    @Override
    @SideOnly(Side.CLIENT)
    protected String describeFuel(ItemStack stack) {
        return TextFormatting.RED
            + I18n.format("tooltip.lantern.energy", formatFE(getEnergy(stack)), formatFE(LanternConfig.energyCapacity));
    }

    @Override
    @SideOnly(Side.CLIENT)
    protected String describeCost() {
        return I18n.format("tooltip.lantern.cost_energy", formatFE(LanternConfig.energyPerLight));
    }

    @Override
    protected String howtoFillKey() {
        return "tooltip.lantern.howto2_energy";
    }

    /** A stack with no Energy tag (/give, creative menu, JEI) counts as full; crafted stacks carry Energy:0. */
    public static int getEnergy(ItemStack stack) {
        if (stack.hasTagCompound() && stack.getTagCompound().hasKey(TAG_ENERGY)) {
            return stack.getTagCompound().getInteger(TAG_ENERGY);
        }
        return LanternConfig.energyCapacity;
    }

    public static void setEnergy(ItemStack stack, int energy) {
        getOrCreateTag(stack).setInteger(TAG_ENERGY,
            Math.max(0, Math.min(LanternConfig.energyCapacity, energy)));
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable NBTTagCompound nbt) {
        return new ICapabilityProvider() {
            @Override
            public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
                return capability == CapabilityEnergy.ENERGY;
            }

            @Override
            @Nullable
            public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
                return capability == CapabilityEnergy.ENERGY
                    ? CapabilityEnergy.ENERGY.cast(new StackEnergy(stack)) : null;
            }
        };
    }

    private static final class StackEnergy implements IEnergyStorage {

        private final ItemStack stack;

        StackEnergy(ItemStack stack) {
            this.stack = stack;
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int stored = getEnergy(stack);
            int accepted = Math.max(0, Math.min(maxReceive, LanternConfig.energyCapacity - stored));
            if (accepted > 0 && !simulate) {
                setEnergy(stack, stored + accepted);
            }
            return accepted;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int stored = getEnergy(stack);
            int removed = Math.max(0, Math.min(maxExtract, stored));
            if (removed > 0 && !simulate) {
                setEnergy(stack, stored - removed);
            }
            return removed;
        }

        @Override
        public int getEnergyStored() {
            return getEnergy(stack);
        }

        @Override
        public int getMaxEnergyStored() {
            return LanternConfig.energyCapacity;
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    }
}

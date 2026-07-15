package com.dutchess77.lantern.item;

import com.dutchess77.lantern.LanternConfig;
import com.dutchess77.lantern.LanternDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/**
 * Lantern variant powered by Forge Energy instead of Glowstone. Charge it in
 * any FE item charger (capacitor banks, etc.).
 */
public class EnergyLanternItem extends LanternItem {

    public EnergyLanternItem() {
        super();
    }

    /** FE capacity including Capacity upgrades. */
    public static int energyCapacityOf(ItemStack stack) {
        return LanternUpgrades.effectiveCapacity(stack, LanternConfig.energyCapacity);
    }

    @Override
    protected void fill(Player player, ItemStack stack) {
        // no inventory fuel and no status chatter - the durability bar shows the charge
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
    public boolean lightsRefundGlowstone() {
        return false; // FE-paid lights refund no glowstone
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
        int stored = getEnergy(stack);
        if (stored < cost) {
            return false;
        }
        setEnergy(stack, stored - cost);
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int capacity = Math.max(1, energyCapacityOf(stack));
        return Math.round(13.0F * Math.min(getEnergy(stack), capacity) / capacity);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x3AC6D8;
    }

    @Override
    protected MutableComponent describeFuel(ItemStack stack) {
        return Component.translatable("tooltip.lantern.energy",
            formatFE(getEnergy(stack)), formatFE(energyCapacityOf(stack)))
            .withStyle(ChatFormatting.RED);
    }

    @Override
    protected MutableComponent describeCost() {
        return Component.translatable("tooltip.lantern.cost_energy", formatFE(LanternConfig.energyPerLight));
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

    /** A stack with no Energy component (/give, creative menu, JEI) counts as full; crafted stacks carry energy=0. */
    public static int getEnergy(ItemStack stack) {
        Integer energy = stack.get(LanternDataComponents.ENERGY.get());
        return energy != null ? energy : energyCapacityOf(stack);
    }

    public static void setEnergy(ItemStack stack, int energy) {
        stack.set(LanternDataComponents.ENERGY.get(),
            Math.max(0, Math.min(energyCapacityOf(stack), energy)));
    }

    /**
     * FE capability backed by the stack's Energy component - shared with the
     * Energy Glow Wand. Registered in {@link ModCapabilities}. A hand-rolled
     * storage (not ComponentEnergyStorage) so the "no component = full"
     * fallback of {@link #getEnergy} is preserved.
     */
    public static IEnergyStorage newEnergyStorage(ItemStack stack) {
        return new StackEnergy(stack);
    }

    private static final class StackEnergy implements IEnergyStorage {

        private final ItemStack stack;

        StackEnergy(ItemStack stack) {
            this.stack = stack;
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int stored = getEnergy(stack);
            int accepted = Math.max(0, Math.min(maxReceive, energyCapacityOf(stack) - stored));
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
            return energyCapacityOf(stack);
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

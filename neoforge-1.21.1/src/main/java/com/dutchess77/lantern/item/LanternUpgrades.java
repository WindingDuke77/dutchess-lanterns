package com.dutchess77.lantern.item;

import java.util.ArrayList;
import java.util.List;

import com.dutchess77.lantern.LanternDataComponents;
import com.dutchess77.lantern.LanternDataComponents.InstalledUpgrade;
import com.dutchess77.lantern.ModItems;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

/**
 * Upgrades socketed into a lantern, stored on the item as a list data
 * component of {type, tier} (the 1.12.2 NBT list). Same-type upgrades stack
 * additively.
 *
 * <p>The 1.12.2 nested {@code LanternUpgrades.Installed} class is now the
 * shared {@link LanternDataComponents.InstalledUpgrade} record: read entries
 * with the record accessors {@code type()} / {@code tier()} instead of the old
 * {@code .type} / {@code .tier} fields.
 */
public final class LanternUpgrades {

    private LanternUpgrades() {
    }

    public static int socketCount(ItemStack lantern) {
        return lantern.getItem() instanceof EnergyLanternItem
            || lantern.getItem() instanceof EnergyGlowWandItem ? 4 : 3;
    }

    public static List<InstalledUpgrade> list(ItemStack lantern) {
        return lantern.getOrDefault(LanternDataComponents.UPGRADES.get(), List.of());
    }

    /** Installs one upgrade if a socket is free. */
    public static boolean install(ItemStack lantern, UpgradeType type, int tier) {
        List<InstalledUpgrade> current = list(lantern);
        if (current.size() >= socketCount(lantern)) {
            return false;
        }
        // the component list is immutable - copy, add, set
        List<InstalledUpgrade> next = new ArrayList<>(current);
        next.add(new InstalledUpgrade(type, Math.min(UpgradeItem.MAX_TIER, Math.max(1, tier))));
        lantern.set(LanternDataComponents.UPGRADES.get(), List.copyOf(next));
        return true;
    }

    /** Pops all upgrades off the lantern, returning them as items. */
    public static NonNullList<ItemStack> removeAll(ItemStack lantern) {
        NonNullList<ItemStack> items = NonNullList.create();
        for (InstalledUpgrade installed : list(lantern)) {
            UpgradeItem item = ModItems.upgradeFor(installed.type(), installed.tier());
            if (item != null) {
                items.add(new ItemStack(item));
            }
        }
        lantern.remove(LanternDataComponents.UPGRADES.get());
        return items;
    }

    private static int tierSum(ItemStack lantern, UpgradeType type) {
        int sum = 0;
        for (InstalledUpgrade installed : list(lantern)) {
            if (installed.type() == type) {
                sum += installed.tier();
            }
        }
        return sum;
    }

    /** Base radius + 4 per Range tier, capped so the scan stays affordable. */
    public static int effectiveRadius(ItemStack lantern, int base) {
        return Math.min(32, base + 4 * tierSum(lantern, UpgradeType.RANGE));
    }

    public static final int MAX_REACH = 20;

    /** Glow Wand targeting reach: vanilla-ish 5 blocks + 2 per Range tier, capped. */
    public static int effectiveReach(ItemStack wand) {
        return Math.min(MAX_REACH, 5 + 2 * tierSum(wand, UpgradeType.RANGE));
    }

    /** Chance a placement costs nothing: 15% per Efficiency tier, capped at 80%. */
    public static float freeChance(ItemStack lantern) {
        return Math.min(0.8F, 0.15F * tierSum(lantern, UpgradeType.EFFICIENCY));
    }

    /** Capacity multiplier x4: +75% per Capacity tier (tier IV alone = 4x). */
    public static int effectiveCapacity(ItemStack lantern, int base) {
        long scaled = (long) base * (4 + 3 * tierSum(lantern, UpgradeType.CAPACITY)) / 4;
        return (int) Math.min(Integer.MAX_VALUE, scaled);
    }
}

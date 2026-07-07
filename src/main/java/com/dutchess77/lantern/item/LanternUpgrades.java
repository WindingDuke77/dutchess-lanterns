package com.dutchess77.lantern.item;

import java.util.ArrayList;
import java.util.List;

import com.dutchess77.lantern.ModItems;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.NonNullList;
import net.minecraftforge.common.util.Constants;

/**
 * Upgrades socketed into a lantern, stored on the item as an NBT list of
 * {Type, Tier}. Same-type upgrades stack additively.
 */
public final class LanternUpgrades {

    private static final String TAG_UPGRADES = "Upgrades";
    private static final String TAG_TYPE = "Type";
    private static final String TAG_TIER = "Tier";

    public static final class Installed {
        public final UpgradeType type;
        public final int tier;

        Installed(UpgradeType type, int tier) {
            this.type = type;
            this.tier = tier;
        }
    }

    private LanternUpgrades() {
    }

    public static int socketCount(ItemStack lantern) {
        return lantern.getItem() instanceof EnergyLanternItem ? 4 : 3;
    }

    public static List<Installed> list(ItemStack lantern) {
        List<Installed> result = new ArrayList<>();
        if (lantern.hasTagCompound() && lantern.getTagCompound().hasKey(TAG_UPGRADES)) {
            NBTTagList tags = lantern.getTagCompound().getTagList(TAG_UPGRADES, Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < tags.tagCount(); i++) {
                NBTTagCompound tag = tags.getCompoundTagAt(i);
                UpgradeType type = UpgradeType.byKey(tag.getString(TAG_TYPE));
                if (type != null) {
                    result.add(new Installed(type, Math.min(UpgradeItem.MAX_TIER, Math.max(1, tag.getInteger(TAG_TIER)))));
                }
            }
        }
        return result;
    }

    /** Installs one upgrade if a socket is free. */
    public static boolean install(ItemStack lantern, UpgradeType type, int tier) {
        if (list(lantern).size() >= socketCount(lantern)) {
            return false;
        }
        if (!lantern.hasTagCompound()) {
            lantern.setTagCompound(new NBTTagCompound());
        }
        NBTTagList tags = lantern.getTagCompound().getTagList(TAG_UPGRADES, Constants.NBT.TAG_COMPOUND);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString(TAG_TYPE, type.key);
        tag.setInteger(TAG_TIER, tier);
        tags.appendTag(tag);
        lantern.getTagCompound().setTag(TAG_UPGRADES, tags);
        return true;
    }

    /** Pops all upgrades off the lantern, returning them as items. */
    public static NonNullList<ItemStack> removeAll(ItemStack lantern) {
        NonNullList<ItemStack> items = NonNullList.create();
        for (Installed installed : list(lantern)) {
            UpgradeItem item = ModItems.upgradeFor(installed.type);
            if (item != null) {
                items.add(new ItemStack(item, 1, installed.tier - 1));
            }
        }
        if (lantern.hasTagCompound()) {
            lantern.getTagCompound().removeTag(TAG_UPGRADES);
        }
        return items;
    }

    private static int tierSum(ItemStack lantern, UpgradeType type) {
        int sum = 0;
        for (Installed installed : list(lantern)) {
            if (installed.type == type) {
                sum += installed.tier;
            }
        }
        return sum;
    }

    /** Base radius + 4 per Range tier, capped so the scan stays affordable. */
    public static int effectiveRadius(ItemStack lantern, int base) {
        return Math.min(32, base + 4 * tierSum(lantern, UpgradeType.RANGE));
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

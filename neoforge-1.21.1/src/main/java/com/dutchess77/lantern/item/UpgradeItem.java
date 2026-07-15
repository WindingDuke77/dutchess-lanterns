package com.dutchess77.lantern.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * One item per upgrade type and tier. 1.12.2 used metadata 0-3 for tiers I-IV;
 * 1.21 has no metadata, so each tier is its own registered item
 * (e.g. range_upgrade_t1..t4) and the default descriptionId already matches
 * the "item.lantern.&lt;type&gt;_t&lt;tier&gt;" lang keys.
 */
public class UpgradeItem extends Item {

    public static final int MAX_TIER = 4;

    public final UpgradeType type;
    public final int tier;

    public UpgradeItem(UpgradeType type, int tier) {
        super(new Item.Properties());
        this.type = type;
        this.tier = Math.min(MAX_TIER, Math.max(1, tier));
    }

    /** Tier 1..4. */
    public static int tierOf(ItemStack stack) {
        return stack.getItem() instanceof UpgradeItem upgrade ? upgrade.tier : 1;
    }
}

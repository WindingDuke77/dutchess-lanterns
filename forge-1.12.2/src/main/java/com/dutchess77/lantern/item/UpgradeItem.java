package com.dutchess77.lantern.item;

import com.dutchess77.lantern.Lantern;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;

/** One item per upgrade type; metadata 0-3 = tier I-IV. */
public class UpgradeItem extends Item {

    public static final int MAX_TIER = 4;

    public final UpgradeType type;

    public UpgradeItem(UpgradeType type) {
        this.type = type;
        setRegistryName(Lantern.MODID, type.key);
        setTranslationKey(Lantern.MODID + "." + type.key);
        setCreativeTab(CreativeTabs.TOOLS);
        setHasSubtypes(true);
        setMaxDamage(0);
    }

    /** Tier 1..4. */
    public static int tierOf(ItemStack stack) {
        return Math.min(MAX_TIER, Math.max(1, stack.getMetadata() + 1));
    }

    @Override
    public String getTranslationKey(ItemStack stack) {
        return super.getTranslationKey(stack) + "_t" + tierOf(stack);
    }

    @Override
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
        if (isInCreativeTab(tab)) {
            for (int meta = 0; meta < MAX_TIER; meta++) {
                items.add(new ItemStack(this, 1, meta));
            }
        }
    }
}

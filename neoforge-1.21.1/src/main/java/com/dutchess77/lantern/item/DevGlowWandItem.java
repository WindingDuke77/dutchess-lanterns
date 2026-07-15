package com.dutchess77.lantern.item;

/**
 * Dev tool: free like the Creative Glow Wand, but swaps the target for plain
 * visible Glowstone instead of a hidden camo light. No recipe.
 */
public class DevGlowWandItem extends CreativeGlowWandItem {

    public DevGlowWandItem() {
        super();
    }

    @Override
    protected boolean placesVisibleGlowstone() {
        return true;
    }

    @Override
    protected String howtoFillKey() {
        return "tooltip.lantern.wand_fill_dev";
    }
}

package com.dutchess77.lantern.compat;

import java.lang.reflect.Method;

import com.dutchess77.lantern.item.LanternItem;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

/** Reads the player's Baubles slots via reflection; no compile dependency. */
public final class BaublesCompat {

    private static Method getBaublesHandler;
    private static boolean lookedUp;

    private BaublesCompat() {
    }

    /** First active lantern worn in a bauble slot, or EMPTY. */
    public static ItemStack findActiveLantern(EntityPlayer player) {
        try {
            if (!lookedUp) {
                lookedUp = true;
                try {
                    getBaublesHandler = Class.forName("baubles.api.BaublesApi")
                        .getMethod("getBaublesHandler", EntityPlayer.class);
                } catch (Throwable absent) {
                    // Baubles not installed - stay null
                }
            }
            if (getBaublesHandler == null) {
                return ItemStack.EMPTY;
            }
            Object handler = getBaublesHandler.invoke(null, player);
            if (handler instanceof IItemHandler) {
                IItemHandler slots = (IItemHandler) handler;
                for (int i = 0; i < slots.getSlots(); i++) {
                    ItemStack stack = slots.getStackInSlot(i);
                    if (!stack.isEmpty() && stack.getItem() instanceof LanternItem
                        && LanternItem.isActive(stack)) {
                        return stack;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return ItemStack.EMPTY;
    }
}

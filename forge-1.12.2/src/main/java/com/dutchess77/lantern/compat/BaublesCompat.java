package com.dutchess77.lantern.compat;

import java.lang.reflect.Method;

import javax.annotation.Nullable;

import com.dutchess77.lantern.item.LanternItem;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

/** Reads the player's Baubles slots via reflection; no compile dependency. */
public final class BaublesCompat {

    private static Method getBaublesHandler;
    private static Method isItemValidForSlot;
    private static boolean lookedUp;

    private BaublesCompat() {
    }

    private static void lookup() {
        if (lookedUp) {
            return;
        }
        lookedUp = true;
        try {
            getBaublesHandler = Class.forName("baubles.api.BaublesApi")
                .getMethod("getBaublesHandler", EntityPlayer.class);
            isItemValidForSlot = Class.forName("baubles.api.cap.IBaublesItemHandler")
                .getMethod("isItemValidForSlot", int.class, ItemStack.class, EntityLivingBase.class);
        } catch (Throwable absent) {
            // Baubles not installed - stay null
            getBaublesHandler = null;
            isItemValidForSlot = null;
        }
    }

    /** The player's Baubles inventory, or null without Baubles. */
    @Nullable
    public static IItemHandler handler(EntityPlayer player) {
        lookup();
        if (getBaublesHandler == null) {
            return null;
        }
        try {
            Object handler = getBaublesHandler.invoke(null, player);
            return handler instanceof IItemHandler ? (IItemHandler) handler : null;
        } catch (Throwable t) {
            return null;
        }
    }

    /** Baubles' own per-slot rule (bauble type must match the slot). */
    public static boolean isValidForSlot(IItemHandler handler, int slot, ItemStack stack, EntityPlayer player) {
        if (isItemValidForSlot == null) {
            return false;
        }
        try {
            return (Boolean) isItemValidForSlot.invoke(handler, slot, stack, player);
        } catch (Throwable t) {
            return false;
        }
    }

    /** First active lantern worn in a bauble slot, or EMPTY. */
    public static ItemStack findActiveLantern(EntityPlayer player) {
        IItemHandler slots = handler(player);
        if (slots != null) {
            for (int i = 0; i < slots.getSlots(); i++) {
                ItemStack stack = slots.getStackInSlot(i);
                if (!stack.isEmpty() && stack.getItem() instanceof LanternItem
                    && LanternItem.isActive(stack)) {
                    return stack;
                }
            }
        }
        return ItemStack.EMPTY;
    }
}

package com.dutchess77.lantern.compat;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.dutchess77.lantern.item.LanternItem;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Reads the player's Curios slots via reflection; no compile dependency.
 * 1.21 replacement for the 1.12.2 BaublesCompat (Curios needs no item
 * interface - lanterns are assigned to slots by datapack tag instead).
 */
public final class CuriosCompat {

    private static Method getCuriosInventory;
    private static Method getEquippedCurios;
    private static boolean lookedUp;

    private CuriosCompat() {
    }

    private static void lookup() {
        if (lookedUp) {
            return;
        }
        lookedUp = true;
        try {
            // CuriosApi.getCuriosInventory(LivingEntity) -> Optional<ICuriosItemHandler>
            getCuriosInventory = Class.forName("top.theillusivec4.curios.api.CuriosApi")
                .getMethod("getCuriosInventory", LivingEntity.class);
            // ICuriosItemHandler.getEquippedCurios() -> IItemHandlerModifiable
            getEquippedCurios = Class.forName("top.theillusivec4.curios.api.type.capability.ICuriosItemHandler")
                .getMethod("getEquippedCurios");
        } catch (Throwable absent) {
            // Curios not installed - stay null
            getCuriosInventory = null;
            getEquippedCurios = null;
        }
    }

    /** Every lantern-family item worn in a Curios slot, or an empty list without Curios. */
    public static List<ItemStack> equippedLanterns(Player player) {
        lookup();
        if (getCuriosInventory == null || getEquippedCurios == null) {
            return List.of();
        }
        try {
            Object inventory = getCuriosInventory.invoke(null, player);
            if (inventory instanceof Optional<?> optional) {
                if (optional.isEmpty()) {
                    return List.of();
                }
                inventory = optional.get();
            }
            if (inventory == null) {
                return List.of();
            }
            Object handlerObj = getEquippedCurios.invoke(inventory);
            if (!(handlerObj instanceof IItemHandler handler)) {
                return List.of();
            }
            List<ItemStack> lanterns = new ArrayList<>();
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack stack = handler.getStackInSlot(i);
                if (!stack.isEmpty() && stack.getItem() instanceof LanternItem) {
                    lanterns.add(stack);
                }
            }
            return lanterns;
        } catch (Throwable t) {
            return List.of();
        }
    }
}

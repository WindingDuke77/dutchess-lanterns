package com.dutchess77.lantern.gui;

import com.dutchess77.lantern.block.LanternBenchTileEntity;
import com.dutchess77.lantern.item.LanternItem;
import com.dutchess77.lantern.item.LanternUpgrades;
import com.dutchess77.lantern.item.UpgradeItem;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.SlotItemHandler;

public class LanternBenchContainer extends Container {

    private final LanternBenchTileEntity bench;

    public LanternBenchContainer(InventoryPlayer playerInventory, LanternBenchTileEntity bench) {
        this.bench = bench;

        addSlotToContainer(new SlotItemHandler(bench.getInventory(), LanternBenchTileEntity.SLOT_LANTERN, 44, 35) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return stack.getItem() instanceof LanternItem;
            }
        });
        addSlotToContainer(new SlotItemHandler(bench.getInventory(), LanternBenchTileEntity.SLOT_INPUT, 98, 35) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return stack.getItem() instanceof UpgradeItem;
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlotToContainer(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlotToContainer(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return bench.getWorld().getTileEntity(bench.getPos()) == bench
            && player.getDistanceSq(bench.getPos().add(0.5, 0.5, 0.5)) <= 64.0D;
    }

    /** Button 0 = pop all upgrades off the lantern back to the player. */
    @Override
    public boolean enchantItem(EntityPlayer player, int id) {
        if (id != 0 || player.world.isRemote) {
            return false;
        }
        ItemStack lantern = bench.getInventory().getStackInSlot(LanternBenchTileEntity.SLOT_LANTERN);
        if (lantern.isEmpty() || !(lantern.getItem() instanceof LanternItem)) {
            return false;
        }
        for (ItemStack upgrade : LanternUpgrades.removeAll(lantern)) {
            ItemHandlerHelper.giveItemToPlayer(player, upgrade);
        }
        bench.getInventory().setStackInSlot(LanternBenchTileEntity.SLOT_LANTERN, lantern);
        return true;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        ItemStack moved = ItemStack.EMPTY;
        Slot slot = inventorySlots.get(index);
        if (slot == null || !slot.getHasStack()) {
            return moved;
        }
        ItemStack current = slot.getStack();
        moved = current.copy();
        if (index < 2) {
            // bench -> player
            if (!mergeItemStack(current, 2, inventorySlots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (current.getItem() instanceof LanternItem) {
            if (!mergeItemStack(current, 0, 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (current.getItem() instanceof UpgradeItem) {
            if (!mergeItemStack(current, 1, 2, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }
        if (current.isEmpty()) {
            slot.putStack(ItemStack.EMPTY);
        } else {
            slot.onSlotChanged();
        }
        return moved;
    }
}

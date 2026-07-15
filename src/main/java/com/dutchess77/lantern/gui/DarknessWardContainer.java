package com.dutchess77.lantern.gui;

import com.dutchess77.lantern.block.DarknessWardTileEntity;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

public class DarknessWardContainer extends Container {

    private final DarknessWardTileEntity ward;

    public DarknessWardContainer(InventoryPlayer playerInventory, DarknessWardTileEntity ward) {
        this.ward = ward;

        // socket column down the right edge
        for (int i = 0; i < DarknessWardTileEntity.SOCKET_COUNT; i++) {
            addSlotToContainer(new SlotItemHandler(ward.getSockets(), i, 150, 9 + i * 18) {
                @Override
                public boolean isItemValid(ItemStack stack) {
                    return DarknessWardTileEntity.isRangeUpgrade(stack);
                }

                @Override
                public int getSlotStackLimit() {
                    return 1;
                }
            });
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlotToContainer(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlotToContainer(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    public DarknessWardTileEntity getWard() {
        return ward;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return ward.getWorld().getTileEntity(ward.getPos()) == ward
            && player.getDistanceSq(ward.getPos().add(0.5, 0.5, 0.5)) <= 64.0D;
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
        int sockets = DarknessWardTileEntity.SOCKET_COUNT;
        if (index < sockets) {
            if (!mergeItemStack(current, sockets, inventorySlots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (DarknessWardTileEntity.isRangeUpgrade(current)) {
            if (!mergeItemStack(current, 0, sockets, false)) {
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

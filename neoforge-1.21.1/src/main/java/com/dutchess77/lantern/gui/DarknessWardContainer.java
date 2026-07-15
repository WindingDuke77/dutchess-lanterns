package com.dutchess77.lantern.gui;

import com.dutchess77.lantern.ModMenus;
import com.dutchess77.lantern.block.DarknessWardTileEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

public class DarknessWardContainer extends AbstractContainerMenu {

    private final DarknessWardTileEntity ward;

    /** Client factory: resolves the block entity from the pos the server wrote. */
    public DarknessWardContainer(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory,
            (DarknessWardTileEntity) playerInventory.player.level().getBlockEntity(buf.readBlockPos()));
    }

    public DarknessWardContainer(int containerId, Inventory playerInventory, DarknessWardTileEntity ward) {
        super(ModMenus.DARKNESS_WARD.get(), containerId);
        this.ward = ward;

        // socket column down the right edge
        for (int i = 0; i < DarknessWardTileEntity.SOCKET_COUNT; i++) {
            addSlot(new SlotItemHandler(ward.getSockets(), i, 150, 9 + i * 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return DarknessWardTileEntity.isRangeUpgrade(stack);
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }

                @Override
                public int getMaxStackSize(ItemStack stack) {
                    return 1;
                }
            });
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    public DarknessWardTileEntity getWard() {
        return ward;
    }

    @Override
    public boolean stillValid(Player player) {
        BlockPos pos = ward.getBlockPos();
        return ward.getLevel() != null
            && ward.getLevel().getBlockEntity(pos) == ward
            && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0D;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack moved = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return moved;
        }
        ItemStack current = slot.getItem();
        moved = current.copy();
        int sockets = DarknessWardTileEntity.SOCKET_COUNT;
        if (index < sockets) {
            if (!moveItemStackTo(current, sockets, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (DarknessWardTileEntity.isRangeUpgrade(current)) {
            if (!moveItemStackTo(current, 0, sockets, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }
        if (current.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return moved;
    }
}

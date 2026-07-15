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
import net.minecraftforge.items.SlotItemHandler;

public class LanternBenchContainer extends Container {

    /** N, W, E, S around the central lantern slot at (80, 35). */
    private static final int[][] SOCKET_POSITIONS = { {80, 13}, {58, 35}, {102, 35}, {80, 57} };

    /** Baubles side panel: 7 slots in a column just outside the main panel. */
    public static final int BAUBLE_X = 183;
    public static final int BAUBLE_Y = 14;

    private final LanternBenchTileEntity bench;
    private int baubleCount;

    public LanternBenchContainer(InventoryPlayer playerInventory, LanternBenchTileEntity bench) {
        this.bench = bench;

        addSlotToContainer(new SlotItemHandler(bench.getInventory(), LanternBenchTileEntity.SLOT_LANTERN, 80, 35) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                // no swapping a lantern in while sockets are occupied - take the old one out first
                return stack.getItem() instanceof LanternItem && bench.socketsEmpty();
            }

            @Override
            public ItemStack onTake(EntityPlayer player, ItemStack stack) {
                if (bench.getWorld() != null && !bench.getWorld().isRemote) {
                    bench.packInto(stack);
                }
                return super.onTake(player, stack);
            }
        });

        for (int i = 0; i < LanternBenchTileEntity.SOCKET_COUNT; i++) {
            final int socketIndex = i;
            addSlotToContainer(new SlotItemHandler(bench.getInventory(),
                LanternBenchTileEntity.SOCKET_FIRST + i, SOCKET_POSITIONS[i][0], SOCKET_POSITIONS[i][1]) {
                @Override
                public boolean isItemValid(ItemStack stack) {
                    ItemStack lantern = bench.getInventory().getStackInSlot(LanternBenchTileEntity.SLOT_LANTERN);
                    return stack.getItem() instanceof UpgradeItem
                        && lantern.getItem() instanceof LanternItem
                        && socketIndex < LanternUpgrades.socketCount(lantern);
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

        // worn baubles alongside, so a worn lantern goes straight into the bench
        final EntityPlayer player = playerInventory.player;
        net.minecraftforge.items.IItemHandler baubles =
            com.dutchess77.lantern.compat.BaublesCompat.handler(player);
        if (baubles != null) {
            baubleCount = Math.min(7, baubles.getSlots());
            for (int i = 0; i < baubleCount; i++) {
                final int index = i;
                addSlotToContainer(new SlotItemHandler(baubles, i, BAUBLE_X, BAUBLE_Y + i * 18) {
                    @Override
                    public boolean isItemValid(ItemStack stack) {
                        return com.dutchess77.lantern.compat.BaublesCompat.isValidForSlot(
                            getItemHandler(), index, stack, player);
                    }

                    @Override
                    public int getSlotStackLimit() {
                        return 1;
                    }

                    /** Equip/unequip callbacks, matching Baubles' own SlotBauble. */
                    @Override
                    public void putStack(ItemStack stack) {
                        ItemStack previous = getStack();
                        if (!ItemStack.areItemStacksEqual(previous, stack)) {
                            if (!previous.isEmpty() && previous.getItem() instanceof baubles.api.IBauble) {
                                ((baubles.api.IBauble) previous.getItem()).onUnequipped(previous, player);
                            }
                            if (!stack.isEmpty() && stack.getItem() instanceof baubles.api.IBauble) {
                                ((baubles.api.IBauble) stack.getItem()).onEquipped(stack, player);
                            }
                        }
                        super.putStack(stack);
                    }

                    @Override
                    public ItemStack onTake(EntityPlayer taker, ItemStack stack) {
                        if (!stack.isEmpty() && stack.getItem() instanceof baubles.api.IBauble) {
                            ((baubles.api.IBauble) stack.getItem()).onUnequipped(stack, taker);
                        }
                        return super.onTake(taker, stack);
                    }

                    @Override
                    public boolean canTakeStack(EntityPlayer taker) {
                        ItemStack stack = getStack();
                        return stack.isEmpty() || !(stack.getItem() instanceof baubles.api.IBauble)
                            || ((baubles.api.IBauble) stack.getItem()).canUnequip(stack, taker);
                    }
                });
            }
        }
    }

    public int getBaubleCount() {
        return baubleCount;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return bench.getWorld().getTileEntity(bench.getPos()) == bench
            && player.getDistanceSq(bench.getPos().add(0.5, 0.5, 0.5)) <= 64.0D;
    }

    /**
     * Pack/unpack rewrites slots and the taken stack's NBT outside the click
     * the client predicted, leaving stale sockets and cursor until the GUI is
     * reopened - push a full resync after every click instead.
     */
    @Override
    public ItemStack slotClick(int slotId, int dragType, net.minecraft.inventory.ClickType clickType,
                               EntityPlayer player) {
        ItemStack result = super.slotClick(slotId, dragType, clickType, player);
        if (player instanceof net.minecraft.entity.player.EntityPlayerMP) {
            net.minecraft.entity.player.EntityPlayerMP mp = (net.minecraft.entity.player.EntityPlayerMP) player;
            mp.sendAllContents(this, getInventory());
            mp.updateHeldItem();
        }
        return result;
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
        int benchSlots = 1 + LanternBenchTileEntity.SOCKET_COUNT;
        int playerEnd = benchSlots + 36; // bauble slots live past the player inventory
        if (index < benchSlots) {
            if (index == 0 && !bench.getWorld().isRemote) {
                // shift-clicking the lantern out: pack sockets into it first
                bench.packInto(current);
                moved = current.copy();
            }
            if (!mergeItemStack(current, benchSlots, playerEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else if (index >= playerEnd) {
            // worn bauble out to the player inventory
            if (!mergeItemStack(current, benchSlots, playerEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else if (current.getItem() instanceof LanternItem) {
            if (!mergeItemStack(current, 0, 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (current.getItem() instanceof UpgradeItem) {
            if (!mergeItemStack(current, 1, benchSlots, false)) {
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
        if (current.getCount() != moved.getCount()) {
            slot.onTake(player, current); // fires bauble unequip callbacks on shift-click out
        }
        return moved;
    }
}

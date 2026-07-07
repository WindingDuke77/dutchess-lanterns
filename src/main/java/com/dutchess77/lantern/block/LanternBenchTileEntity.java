package com.dutchess77.lantern.block;

import com.dutchess77.lantern.item.LanternItem;
import com.dutchess77.lantern.item.LanternUpgrades;
import com.dutchess77.lantern.item.UpgradeItem;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.NonNullList;
import net.minecraftforge.items.ItemStackHandler;

/**
 * Slot 0: lantern. Slots 1-4: sockets. While a lantern is docked its
 * upgrades physically sit in the socket slots; taking the lantern out packs
 * the sockets back into its NBT. No item-handler capability is exposed, so
 * hoppers cannot bypass the pack/unpack.
 */
public class LanternBenchTileEntity extends TileEntity {

    public static final int SLOT_LANTERN = 0;
    public static final int SOCKET_FIRST = 1;
    public static final int SOCKET_COUNT = 4;

    private boolean transferring;

    private final ItemStackHandler inventory = new ItemStackHandler(1 + SOCKET_COUNT) {
        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            markDirty();
            if (!transferring && slot == SLOT_LANTERN && world != null && !world.isRemote) {
                unpackLantern();
            }
        }
    };

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public boolean socketsEmpty() {
        for (int i = SOCKET_FIRST; i < SOCKET_FIRST + SOCKET_COUNT; i++) {
            if (!inventory.getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** Freshly docked lantern: its installed upgrades pop out into the sockets. */
    private void unpackLantern() {
        ItemStack lantern = inventory.getStackInSlot(SLOT_LANTERN);
        if (lantern.isEmpty() || !(lantern.getItem() instanceof LanternItem)) {
            return;
        }
        transferring = true;
        try {
            NonNullList<ItemStack> upgrades = LanternUpgrades.removeAll(lantern);
            int slot = SOCKET_FIRST;
            for (ItemStack upgrade : upgrades) {
                while (slot < SOCKET_FIRST + SOCKET_COUNT && !inventory.getStackInSlot(slot).isEmpty()) {
                    slot++;
                }
                if (slot < SOCKET_FIRST + SOCKET_COUNT) {
                    inventory.setStackInSlot(slot++, upgrade);
                } else if (upgrade.getItem() instanceof UpgradeItem) {
                    // no free socket - keep it on the item
                    LanternUpgrades.install(lantern,
                        ((UpgradeItem) upgrade.getItem()).type, UpgradeItem.tierOf(upgrade));
                }
            }
            inventory.setStackInSlot(SLOT_LANTERN, lantern);
        } finally {
            transferring = false;
        }
    }

    /** Socket contents install onto the given lantern; installed sockets clear. */
    public void packInto(ItemStack lantern) {
        if (lantern.isEmpty() || !(lantern.getItem() instanceof LanternItem)) {
            return;
        }
        transferring = true;
        try {
            for (int i = SOCKET_FIRST; i < SOCKET_FIRST + SOCKET_COUNT; i++) {
                ItemStack socket = inventory.getStackInSlot(i);
                if (!socket.isEmpty() && socket.getItem() instanceof UpgradeItem
                    && LanternUpgrades.install(lantern,
                        ((UpgradeItem) socket.getItem()).type, UpgradeItem.tierOf(socket))) {
                    inventory.setStackInSlot(i, ItemStack.EMPTY);
                }
            }
        } finally {
            transferring = false;
        }
        markDirty();
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        inventory.deserializeNBT(compound.getCompoundTag("Inventory"));
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setTag("Inventory", inventory.serializeNBT());
        return compound;
    }
}

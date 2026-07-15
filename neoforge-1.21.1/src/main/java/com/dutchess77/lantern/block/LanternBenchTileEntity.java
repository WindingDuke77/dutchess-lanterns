package com.dutchess77.lantern.block;

import com.dutchess77.lantern.ModBlocks;
import com.dutchess77.lantern.item.LanternItem;
import com.dutchess77.lantern.item.LanternUpgrades;
import com.dutchess77.lantern.item.UpgradeItem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Slot 0: lantern. Slots 1-4: sockets. While a lantern is docked its
 * upgrades physically sit in the socket slots; taking the lantern out packs
 * the sockets back into its data. No item-handler capability is exposed, so
 * hoppers cannot bypass the pack/unpack.
 */
public class LanternBenchTileEntity extends BlockEntity {

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
            setChanged();
            if (!transferring && slot == SLOT_LANTERN && level != null && !level.isClientSide) {
                unpackLantern();
            }
        }
    };

    public LanternBenchTileEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.LANTERN_BENCH_BE.get(), pos, state);
    }

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
            int slot = SOCKET_FIRST;
            for (ItemStack upgrade : LanternUpgrades.removeAll(lantern)) {
                while (slot < SOCKET_FIRST + SOCKET_COUNT && !inventory.getStackInSlot(slot).isEmpty()) {
                    slot++;
                }
                if (slot < SOCKET_FIRST + SOCKET_COUNT) {
                    inventory.setStackInSlot(slot++, upgrade);
                } else if (upgrade.getItem() instanceof UpgradeItem upgradeItem) {
                    // no free socket - keep it on the item
                    LanternUpgrades.install(lantern, upgradeItem.type, UpgradeItem.tierOf(upgrade));
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
                if (!socket.isEmpty() && socket.getItem() instanceof UpgradeItem upgradeItem
                    && LanternUpgrades.install(lantern, upgradeItem.type, UpgradeItem.tierOf(socket))) {
                    inventory.setStackInSlot(i, ItemStack.EMPTY);
                }
            }
        } finally {
            transferring = false;
        }
        setChanged();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        CompoundTag inventoryTag = tag.getCompound("Inventory");
        // deserializeNBT resizes the handler to the saved Size - benches saved by
        // older versions had fewer slots and would crash the 5-slot container
        inventoryTag.putInt("Size", 1 + SOCKET_COUNT);
        inventory.deserializeNBT(registries, inventoryTag);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", inventory.serializeNBT(registries));
    }
}

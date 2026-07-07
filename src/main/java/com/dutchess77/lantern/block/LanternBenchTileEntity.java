package com.dutchess77.lantern.block;

import javax.annotation.Nullable;

import com.dutchess77.lantern.item.LanternItem;
import com.dutchess77.lantern.item.LanternUpgrades;
import com.dutchess77.lantern.item.UpgradeItem;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;

/**
 * Slot 0: lantern. Slot 1: upgrade input - anything placed there installs
 * onto the lantern immediately (server side) while sockets are free.
 */
public class LanternBenchTileEntity extends TileEntity {

    public static final int SLOT_LANTERN = 0;
    public static final int SLOT_INPUT = 1;

    private final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            markDirty();
            if (world != null && !world.isRemote) {
                tryInstall();
            }
        }
    };

    public ItemStackHandler getInventory() {
        return inventory;
    }

    /** Moves upgrades from the input slot into the lantern's free sockets. */
    public void tryInstall() {
        ItemStack lantern = inventory.getStackInSlot(SLOT_LANTERN);
        ItemStack input = inventory.getStackInSlot(SLOT_INPUT);
        if (lantern.isEmpty() || input.isEmpty()
            || !(lantern.getItem() instanceof LanternItem)
            || !(input.getItem() instanceof UpgradeItem)) {
            return;
        }
        UpgradeItem upgrade = (UpgradeItem) input.getItem();
        boolean changed = false;
        while (!input.isEmpty()
            && LanternUpgrades.install(lantern, upgrade.type, UpgradeItem.tierOf(input))) {
            input.shrink(1);
            changed = true;
        }
        if (changed) {
            inventory.setStackInSlot(SLOT_LANTERN, lantern);
            inventory.setStackInSlot(SLOT_INPUT, input);
        }
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

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
    }

    @Override
    @Nullable
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(inventory);
        }
        return super.getCapability(capability, facing);
    }
}

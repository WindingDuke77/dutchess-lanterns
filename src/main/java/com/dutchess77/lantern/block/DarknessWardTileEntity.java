package com.dutchess77.lantern.block;

import com.dutchess77.lantern.LanternConfig;
import com.dutchess77.lantern.item.UpgradeItem;
import com.dutchess77.lantern.item.UpgradeType;
import com.dutchess77.lantern.logic.WardRegistry;

import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.items.ItemStackHandler;

/**
 * Holds the warded box (per-axis half-extents plus an offset from the block)
 * and four sockets for Range upgrades that raise the size cap. Registers
 * itself in the WardRegistry while loaded.
 */
public class DarknessWardTileEntity extends TileEntity {

    public static final int SOCKET_COUNT = 4;
    private static final int HARD_CAP = 64;

    /** Adjust fields for the GUI packet: 0-2 radius XYZ, 3-5 offset XYZ. */
    public static final int FIELD_RADIUS_X = 0;
    public static final int FIELD_OFFSET_X = 3;

    /** Freshly placed wards start small (9x9x9) - grow them in the GUI. */
    private static final int DEFAULT_RADIUS = 4;

    private int radiusX = DEFAULT_RADIUS;
    private int radiusY = DEFAULT_RADIUS;
    private int radiusZ = DEFAULT_RADIUS;
    private int offsetX;
    private int offsetY;
    private int offsetZ;

    private final ItemStackHandler sockets = new ItemStackHandler(SOCKET_COUNT) {
        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return isRangeUpgrade(stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            markDirty();
            clampToCap();
            sync();
        }
    };

    public static boolean isRangeUpgrade(ItemStack stack) {
        return stack.getItem() instanceof UpgradeItem
            && ((UpgradeItem) stack.getItem()).type == UpgradeType.RANGE;
    }

    public ItemStackHandler getSockets() {
        return sockets;
    }

    /** Size cap per axis: config base + 4 per socketed Range tier. */
    public int cap() {
        int tiers = 0;
        for (int i = 0; i < SOCKET_COUNT; i++) {
            ItemStack stack = sockets.getStackInSlot(i);
            if (isRangeUpgrade(stack)) {
                tiers += UpgradeItem.tierOf(stack);
            }
        }
        return Math.min(HARD_CAP, LanternConfig.wardRadius + 4 * tiers);
    }

    public int getRadius(int axis) {
        switch (axis) {
            case 0: return radiusX;
            case 1: return radiusY;
            default: return radiusZ;
        }
    }

    public int getOffset(int axis) {
        switch (axis) {
            case 0: return offsetX;
            case 1: return offsetY;
            default: return offsetZ;
        }
    }

    /** GUI packet entry point (server side): bump one field, clamped. */
    public void adjust(int field, int delta) {
        delta = Math.max(-16, Math.min(16, delta));
        switch (field) {
            case 0: radiusX += delta; break;
            case 1: radiusY += delta; break;
            case 2: radiusZ += delta; break;
            case 3: offsetX += delta; break;
            case 4: offsetY += delta; break;
            case 5: offsetZ += delta; break;
            default: return;
        }
        clampToCap();
        markDirty();
        sync();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private void clampToCap() {
        int cap = cap();
        radiusX = clamp(radiusX, 0, cap);
        radiusY = clamp(radiusY, 0, cap);
        radiusZ = clamp(radiusZ, 0, cap);
        offsetX = clamp(offsetX, -cap, cap);
        offsetY = clamp(offsetY, -cap, cap);
        offsetZ = clamp(offsetZ, -cap, cap);
    }

    public boolean contains(BlockPos target) {
        return Math.abs(target.getX() - (pos.getX() + offsetX)) <= radiusX
            && Math.abs(target.getY() - (pos.getY() + offsetY)) <= radiusY
            && Math.abs(target.getZ() - (pos.getZ() + offsetZ)) <= radiusZ;
    }

    private void sync() {
        if (world != null && !world.isRemote) {
            IBlockState state = world.getBlockState(pos);
            world.notifyBlockUpdate(pos, state, state, 3);
        }
    }

    @Override
    public void onLoad() {
        if (world != null && !world.isRemote) {
            WardRegistry.add(world, this);
        }
    }

    @Override
    public void invalidate() {
        super.invalidate();
        if (world != null && !world.isRemote) {
            WardRegistry.remove(world, pos);
        }
    }

    @Override
    public void onChunkUnload() {
        super.onChunkUnload();
        if (world != null && !world.isRemote) {
            WardRegistry.remove(world, pos);
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        if (compound.hasKey("Rx")) {
            radiusX = compound.getInteger("Rx");
            radiusY = compound.getInteger("Ry");
            radiusZ = compound.getInteger("Rz");
            offsetX = compound.getInteger("Ox");
            offsetY = compound.getInteger("Oy");
            offsetZ = compound.getInteger("Oz");
        }
        if (compound.hasKey("Sockets")) {
            NBTTagCompound socketsTag = compound.getCompoundTag("Sockets");
            socketsTag.setInteger("Size", SOCKET_COUNT);
            sockets.deserializeNBT(socketsTag);
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setInteger("Rx", radiusX);
        compound.setInteger("Ry", radiusY);
        compound.setInteger("Rz", radiusZ);
        compound.setInteger("Ox", offsetX);
        compound.setInteger("Oy", offsetY);
        compound.setInteger("Oz", offsetZ);
        compound.setTag("Sockets", sockets.serializeNBT());
        return compound;
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(super.getUpdateTag());
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, 0, getUpdateTag());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        readFromNBT(pkt.getNbtCompound());
    }
}

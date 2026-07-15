package com.dutchess77.lantern.block;

import com.dutchess77.lantern.LanternConfig;
import com.dutchess77.lantern.ModBlocks;
import com.dutchess77.lantern.item.UpgradeItem;
import com.dutchess77.lantern.item.UpgradeType;
import com.dutchess77.lantern.logic.WardRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Holds the warded box (per-axis half-extents plus an offset from the block)
 * and four sockets for Range upgrades that raise the size cap. Registers
 * itself in the WardRegistry while loaded.
 */
public class DarknessWardTileEntity extends BlockEntity {

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
            setChanged();
            clampToCap();
            sync();
        }
    };

    public DarknessWardTileEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.DARKNESS_WARD_BE.get(), pos, state);
    }

    public static boolean isRangeUpgrade(ItemStack stack) {
        return stack.getItem() instanceof UpgradeItem upgrade && upgrade.type == UpgradeType.RANGE;
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
        setChanged();
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
        BlockPos pos = getBlockPos();
        return Math.abs(target.getX() - (pos.getX() + offsetX)) <= radiusX
            && Math.abs(target.getY() - (pos.getY() + offsetY)) <= radiusY
            && Math.abs(target.getZ() - (pos.getZ() + offsetZ)) <= radiusZ;
    }

    private void sync() {
        if (level != null && !level.isClientSide) {
            BlockState state = level.getBlockState(worldPosition);
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            WardRegistry.add(level, this);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && !level.isClientSide) {
            WardRegistry.remove(level, worldPosition);
        }
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        if (level != null && !level.isClientSide) {
            WardRegistry.remove(level, worldPosition);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Rx")) {
            radiusX = tag.getInt("Rx");
            radiusY = tag.getInt("Ry");
            radiusZ = tag.getInt("Rz");
            offsetX = tag.getInt("Ox");
            offsetY = tag.getInt("Oy");
            offsetZ = tag.getInt("Oz");
        }
        if (tag.contains("Sockets")) {
            CompoundTag socketsTag = tag.getCompound("Sockets");
            socketsTag.putInt("Size", SOCKET_COUNT);
            sockets.deserializeNBT(registries, socketsTag);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Rx", radiusX);
        tag.putInt("Ry", radiusY);
        tag.putInt("Rz", radiusZ);
        tag.putInt("Ox", offsetX);
        tag.putInt("Oy", offsetY);
        tag.putInt("Oz", offsetZ);
        tag.put("Sockets", sockets.serializeNBT(registries));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            loadAdditional(tag, registries);
        }
    }
}

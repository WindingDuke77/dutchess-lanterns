package com.dutchess77.lantern.block;

import com.dutchess77.lantern.ModBlocks;
import com.dutchess77.lantern.client.HiddenLightBakedModel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Remembers which block this light replaced, and keeps the client in sync.
 * The mimic is volatile because map mods may read it off-thread.
 */
public class HiddenLightTileEntity extends BlockEntity {

    private static final String TAG_MIMIC = "Mimic";
    private static final String TAG_FROM_ENERGY = "FromEnergy";

    private volatile BlockState mimic;
    private boolean fromEnergy;

    public HiddenLightTileEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.HIDDEN_LIGHT_BE.get(), pos, state);
    }

    public BlockState getMimic() {
        return mimic;
    }

    /** True when an Energy Lantern paid FE for this light - it drops no glowstone. */
    public boolean isFromEnergy() {
        return fromEnergy;
    }

    public void setMimic(BlockState state, boolean fromEnergy) {
        this.mimic = state;
        this.fromEnergy = fromEnergy;
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState blockState = level.getBlockState(worldPosition);
            level.sendBlockUpdated(worldPosition, blockState, blockState, 3);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        mimic = null;
        fromEnergy = tag.getBoolean(TAG_FROM_ENERGY);
        if (tag.contains(TAG_MIMIC)) {
            BlockState state = NbtUtils.readBlockState(
                registries.lookupOrThrow(Registries.BLOCK), tag.getCompound(TAG_MIMIC));
            if (!state.isAir()) {
                mimic = state;
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean(TAG_FROM_ENERGY, fromEnergy);
        BlockState mimic = this.mimic;
        if (mimic != null) {
            tag.put(TAG_MIMIC, NbtUtils.writeBlockState(mimic));
        }
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
        requestModelDataUpdate();
        if (level != null && level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        requestModelDataUpdate();
    }

    @Override
    public ModelData getModelData() {
        BlockState mimic = this.mimic;
        return mimic == null ? ModelData.EMPTY
            : ModelData.builder().with(HiddenLightBakedModel.MIMIC, mimic).build();
    }
}

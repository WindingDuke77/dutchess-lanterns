package com.dutchess77.lantern.block;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

/**
 * Remembers which block this light replaced, and keeps the client in sync.
 * Extends the bundled FramedTileEntity skeleton so Xaero's maps color this
 * block as its disguise (they call getCamoState reflectively, off-thread -
 * hence the volatile mimic).
 */
public class HiddenLightTileEntity extends xfacthd.framedblocks.common.tileentity.FramedTileEntity {

    private static final String TAG_ID = "MimicId";
    private static final String TAG_META = "MimicMeta";
    private static final String TAG_FROM_ENERGY = "FromEnergy";

    private volatile IBlockState mimic;
    private boolean fromEnergy;

    @Override
    @Nullable
    public IBlockState getCamoState() {
        return mimic;
    }

    @Nullable
    public IBlockState getMimic() {
        return mimic;
    }

    /** True when an Energy Lantern paid FE for this light - it drops no glowstone. */
    public boolean isFromEnergy() {
        return fromEnergy;
    }

    public void setMimic(@Nullable IBlockState state, boolean fromEnergy) {
        this.mimic = state;
        this.fromEnergy = fromEnergy;
        markDirty();
        if (world != null && !world.isRemote) {
            IBlockState blockState = world.getBlockState(pos);
            world.notifyBlockUpdate(pos, blockState, blockState, 3);
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        mimic = null;
        fromEnergy = compound.getBoolean(TAG_FROM_ENERGY);
        if (compound.hasKey(TAG_ID)) {
            Block block = Block.REGISTRY.getObject(new ResourceLocation(compound.getString(TAG_ID)));
            if (block != Blocks.AIR) {
                try {
                    mimic = block.getStateFromMeta(compound.getInteger(TAG_META));
                } catch (Throwable t) {
                    mimic = block.getDefaultState();
                }
            }
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setBoolean(TAG_FROM_ENERGY, fromEnergy);
        if (mimic != null && mimic.getBlock().getRegistryName() != null) {
            compound.setString(TAG_ID, mimic.getBlock().getRegistryName().toString());
            compound.setInteger(TAG_META, mimic.getBlock().getMetaFromState(mimic));
        }
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
        if (world != null) {
            world.markBlockRangeForRenderUpdate(pos, pos);
        }
    }
}

package com.dutchess77.lantern.block;

import com.dutchess77.lantern.logic.WardRegistry;

import net.minecraft.tileentity.TileEntity;

/** No data of its own - exists to track the ward's position in the WardRegistry. */
public class DarknessWardTileEntity extends TileEntity {

    @Override
    public void onLoad() {
        if (world != null && !world.isRemote) {
            WardRegistry.add(world, pos);
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
}

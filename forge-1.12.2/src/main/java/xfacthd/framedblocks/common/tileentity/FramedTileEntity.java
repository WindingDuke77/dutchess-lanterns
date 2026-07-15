package xfacthd.framedblocks.common.tileentity;

import javax.annotation.Nullable;

import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;

/**
 * Bundled skeleton matching the class + method Xaero's Minimap/World Map look
 * up by name for camouflage-block support. FramedBlocks does not exist on
 * 1.12, so shipping this name lets HiddenLightTileEntity extend it and have
 * both maps color our hidden lights as the block they disguise as.
 */
public class FramedTileEntity extends TileEntity {

    @Nullable
    public IBlockState getCamoState() {
        return null;
    }
}

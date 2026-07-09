package team.chisel.ctm.api;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * Bundled CTM API stub (Connected Textures Mod for 1.12.2). Blocks
 * implementing this tell CTM/Chisel which state to treat them as when
 * computing texture connections - so chiseled floors connect straight
 * across a hidden light. The real CTM class wins the classloading race
 * when the mod is present.
 */
public interface IFacade {

    @Nonnull
    @Deprecated
    IBlockState getFacade(@Nonnull IBlockAccess world, @Nonnull BlockPos pos, @Nullable EnumFacing side);

    @Nonnull
    default IBlockState getFacade(@Nonnull IBlockAccess world, @Nonnull BlockPos pos, @Nullable EnumFacing side,
                                  @Nonnull BlockPos connection) {
        return getFacade(world, pos, side);
    }
}

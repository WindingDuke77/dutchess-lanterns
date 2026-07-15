package com.dutchess77.lantern.block;

import javax.annotation.Nonnull;

import com.dutchess77.lantern.Lantern;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;

/**
 * The mod's own invisible light: a full solid block at light 15 whose model
 * renders whatever block it replaced (stored in its tile entity). Drops the
 * Glowstone block it cost.
 */
public class HiddenLightBlock extends Block implements team.chisel.ctm.api.IFacade {

    public static final IUnlistedProperty<IBlockState> MIMIC = new IUnlistedProperty<IBlockState>() {
        @Override
        public String getName() {
            return "mimic";
        }

        @Override
        public boolean isValid(IBlockState value) {
            return true;
        }

        @Override
        public Class<IBlockState> getType() {
            return IBlockState.class;
        }

        @Override
        public String valueToString(IBlockState value) {
            return value.toString();
        }
    };

    public HiddenLightBlock() {
        super(Material.ROCK);
        setRegistryName(Lantern.MODID, "hidden_light");
        setTranslationKey(Lantern.MODID + ".hidden_light");
        setHardness(0.3F);
        setSoundType(SoundType.STONE);
        setLightLevel(1.0F); // registration-time max; runtime value from getLightValue below
    }

    /** Emission is configurable so disguised floors can be softened (default 15 = glowstone). */
    @Override
    public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
        return com.dutchess77.lantern.LanternConfig.lightEmission;
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new HiddenLightTileEntity();
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new ExtendedBlockState(this, new IProperty[0], new IUnlistedProperty[] { MIMIC });
    }

    @Override
    public IBlockState getExtendedState(IBlockState state, IBlockAccess world, BlockPos pos) {
        TileEntity te = world.getTileEntity(pos);
        if (state instanceof IExtendedBlockState && te instanceof HiddenLightTileEntity) {
            IBlockState mimic = ((HiddenLightTileEntity) te).getMimic();
            if (mimic != null) {
                // resolve the mimic's actual + extended state at THIS position so
                // CTM/connected-texture models compute their connections
                try {
                    IBlockState actual = mimic.getActualState(world, pos);
                    mimic = actual.getBlock().getExtendedState(actual, world, pos);
                } catch (Throwable t) {
                    // some mods' state code chokes on foreign positions - plain mimic then
                }
                // CTM models need their own context-carrying state to connect textures
                mimic = com.dutchess77.lantern.compat.CTMCompat.wrapForCTM(mimic, world, pos);
                return ((IExtendedBlockState) state).withProperty(MIMIC, mimic);
            }
        }
        return state;
    }

    /** CTM facade: neighboring connected-texture blocks connect across us as if we were the mimic. */
    @Nonnull
    @Override
    public IBlockState getFacade(@Nonnull IBlockAccess world, @Nonnull BlockPos pos, EnumFacing side) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof HiddenLightTileEntity) {
            IBlockState mimic = ((HiddenLightTileEntity) te).getMimic();
            if (mimic != null) {
                return mimic;
            }
        }
        return getDefaultState();
    }

    /** Drops the glowstone block it cost - unless an Energy Lantern paid FE for it. */
    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state,
                         int fortune) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof HiddenLightTileEntity && ((HiddenLightTileEntity) te).isFromEnergy()) {
            return;
        }
        drops.add(new ItemStack(Blocks.GLOWSTONE));
    }

    /** Delay removal on harvest so getDrops can still read the tile entity. */
    @Override
    public boolean removedByPlayer(IBlockState state, World world, BlockPos pos, EntityPlayer player,
                                   boolean willHarvest) {
        if (willHarvest) {
            return true;
        }
        return super.removedByPlayer(state, world, pos, player, willHarvest);
    }

    @Override
    public void harvestBlock(World world, EntityPlayer player, BlockPos pos, IBlockState state,
                             TileEntity te, ItemStack stack) {
        super.harvestBlock(world, player, pos, state, te, stack);
        world.setBlockToAir(pos);
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos,
                                  EntityPlayer player) {
        return new ItemStack(this); // shows as "Hidden Light (Lantern)" in Waila/pick-block
    }

    /** Minimaps (Xaero's etc.) color the map from this - report the disguise, not stone gray. */
    @Override
    public MapColor getMapColor(IBlockState state, IBlockAccess world, BlockPos pos) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof HiddenLightTileEntity) {
            IBlockState mimic = ((HiddenLightTileEntity) te).getMimic();
            if (mimic != null) {
                try {
                    return mimic.getMapColor(world, pos);
                } catch (Throwable t) {
                    // fall through to stone
                }
            }
        }
        return MapColor.STONE;
    }

    /** Walking on the light sounds like the block it mimics. */
    @Override
    public SoundType getSoundType(IBlockState state, World world, BlockPos pos, Entity entity) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof HiddenLightTileEntity) {
            IBlockState mimic = ((HiddenLightTileEntity) te).getMimic();
            if (mimic != null && !(mimic.getBlock() instanceof HiddenLightBlock)) {
                try {
                    return mimic.getBlock().getSoundType(mimic, world, pos, entity);
                } catch (Throwable t) {
                    // fall through to stone
                }
            }
        }
        return SoundType.STONE;
    }

    @Override
    public boolean canRenderInLayer(IBlockState state, BlockRenderLayer layer) {
        // every layer: grass adds a CUTOUT_MIPPED overlay, CTM glow textures add
        // TRANSLUCENT ones - the baked model filters per-mimic anyway
        return true;
    }
}

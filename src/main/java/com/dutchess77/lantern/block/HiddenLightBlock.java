package com.dutchess77.lantern.block;

import java.util.Random;

import com.dutchess77.lantern.Lantern;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
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
public class HiddenLightBlock extends Block {

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
        setLightLevel(1.0F);
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
                return ((IExtendedBlockState) state).withProperty(MIMIC, mimic);
            }
        }
        return state;
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return Item.getItemFromBlock(Blocks.GLOWSTONE);
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos,
                                  EntityPlayer player) {
        return new ItemStack(Blocks.GLOWSTONE);
    }

    @Override
    public boolean canRenderInLayer(IBlockState state, BlockRenderLayer layer) {
        // mimics are always opaque full cubes, but e.g. grass adds a CUTOUT_MIPPED overlay
        return layer == BlockRenderLayer.SOLID
            || layer == BlockRenderLayer.CUTOUT
            || layer == BlockRenderLayer.CUTOUT_MIPPED;
    }
}

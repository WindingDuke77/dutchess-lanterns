package com.dutchess77.lantern.block;

import com.dutchess77.lantern.Lantern;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * Keeps an area dark: no Lantern auto-places lights (or sweeps torches)
 * within wardRadius blocks of it - mob farms stay spawnable. The Glow Wand
 * deliberately ignores it, so manual lights still work inside.
 */
public class DarknessWardBlock extends Block {

    public DarknessWardBlock() {
        super(Material.ROCK);
        setRegistryName(Lantern.MODID, "darkness_ward");
        setTranslationKey(Lantern.MODID + ".darkness_ward");
        setCreativeTab(CreativeTabs.TOOLS);
        setHardness(2.0F);
        setResistance(10.0F);
        setSoundType(SoundType.STONE);
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new DarknessWardTileEntity();
    }
}

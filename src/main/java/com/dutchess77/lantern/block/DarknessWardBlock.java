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

    @Override
    public boolean onBlockActivated(World world, net.minecraft.util.math.BlockPos pos, IBlockState state,
                                    net.minecraft.entity.player.EntityPlayer player,
                                    net.minecraft.util.EnumHand hand, net.minecraft.util.EnumFacing facing,
                                    float hitX, float hitY, float hitZ) {
        if (!world.isRemote) {
            player.openGui(Lantern.instance, Lantern.GUI_WARD, world, pos.getX(), pos.getY(), pos.getZ());
        }
        return true;
    }

    @Override
    public void breakBlock(World world, net.minecraft.util.math.BlockPos pos, IBlockState state) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof DarknessWardTileEntity) {
            net.minecraftforge.items.ItemStackHandler sockets = ((DarknessWardTileEntity) te).getSockets();
            for (int i = 0; i < sockets.getSlots(); i++) {
                net.minecraft.inventory.InventoryHelper.spawnItemStack(world,
                    pos.getX(), pos.getY(), pos.getZ(), sockets.getStackInSlot(i));
            }
        }
        super.breakBlock(world, pos, state);
    }
}

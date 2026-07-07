package com.dutchess77.lantern.block;

import com.dutchess77.lantern.Lantern;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Workstation for socketing upgrades into lanterns. */
public class LanternBenchBlock extends Block {

    public LanternBenchBlock() {
        super(Material.WOOD);
        setRegistryName(Lantern.MODID, "lantern_bench");
        setTranslationKey(Lantern.MODID + ".lantern_bench");
        setCreativeTab(CreativeTabs.TOOLS);
        setHardness(2.0F);
        setSoundType(SoundType.WOOD);
        setLightLevel(0.4F); // soft workstation glow
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new LanternBenchTileEntity();
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                    EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!world.isRemote) {
            player.openGui(Lantern.instance, Lantern.GUI_BENCH, world, pos.getX(), pos.getY(), pos.getZ());
        }
        return true;
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof LanternBenchTileEntity) {
            LanternBenchTileEntity bench = (LanternBenchTileEntity) te;
            // socketed upgrades ride along inside the lantern
            bench.packInto(bench.getInventory().getStackInSlot(LanternBenchTileEntity.SLOT_LANTERN));
            for (int i = 0; i < bench.getInventory().getSlots(); i++) {
                net.minecraft.inventory.InventoryHelper.spawnItemStack(world,
                    pos.getX(), pos.getY(), pos.getZ(), bench.getInventory().getStackInSlot(i));
            }
        }
        super.breakBlock(world, pos, state);
    }
}

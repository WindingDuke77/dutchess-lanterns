package com.dutchess77.lantern.gui;

import com.dutchess77.lantern.Lantern;
import com.dutchess77.lantern.block.DarknessWardTileEntity;
import com.dutchess77.lantern.block.LanternBenchTileEntity;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

public class LanternGuiHandler implements IGuiHandler {

    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        TileEntity te = world.getTileEntity(new BlockPos(x, y, z));
        if (id == Lantern.GUI_BENCH && te instanceof LanternBenchTileEntity) {
            return new LanternBenchContainer(player.inventory, (LanternBenchTileEntity) te);
        }
        if (id == Lantern.GUI_WARD && te instanceof DarknessWardTileEntity) {
            return new DarknessWardContainer(player.inventory, (DarknessWardTileEntity) te);
        }
        return null;
    }

    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        TileEntity te = world.getTileEntity(new BlockPos(x, y, z));
        if (id == Lantern.GUI_BENCH && te instanceof LanternBenchTileEntity) {
            return new LanternBenchGui(player.inventory, (LanternBenchTileEntity) te);
        }
        if (id == Lantern.GUI_WARD && te instanceof DarknessWardTileEntity) {
            return new DarknessWardGui(player.inventory, (DarknessWardTileEntity) te);
        }
        return null;
    }
}

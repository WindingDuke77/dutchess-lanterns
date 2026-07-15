package com.dutchess77.lantern.logic;

import java.util.HashSet;
import java.util.Set;

import com.dutchess77.lantern.LanternConfig;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.ItemHandlerHelper;

/** Removes whitelisted torches in the area and returns them to the player. */
public final class TorchSweeper {

    private static String[] cachedTorchConfig;
    private static Set<Block> torchBlocks;

    private TorchSweeper() {
    }

    public static void sweep(World world, EntityPlayer player, BlockPos center, int r, int vr) {
        for (BlockPos pos : BlockPos.getAllInBoxMutable(center.add(-r, -vr, -r), center.add(r, vr, r))) {
            if (!world.isBlockLoaded(pos)) {
                continue;
            }
            IBlockState state = world.getBlockState(pos);
            if (isTorch(state.getBlock()) && !WardRegistry.isWarded(world, pos)) {
                ItemStack drop = new ItemStack(Item.getItemFromBlock(state.getBlock()));
                world.setBlockToAir(pos.toImmutable());
                if (!drop.isEmpty()) {
                    ItemHandlerHelper.giveItemToPlayer(player, drop);
                }
            }
        }
    }

    private static boolean isTorch(Block block) {
        if (cachedTorchConfig != LanternConfig.torchWhitelist) {
            Set<Block> resolved = new HashSet<>();
            for (String name : LanternConfig.torchWhitelist) {
                Block b = Block.REGISTRY.getObject(new ResourceLocation(name));
                if (b != Blocks.AIR) {
                    resolved.add(b);
                }
            }
            torchBlocks = resolved;
            cachedTorchConfig = LanternConfig.torchWhitelist;
        }
        return torchBlocks.contains(block);
    }
}

package com.dutchess77.lantern.logic;

import java.util.HashSet;
import java.util.Set;

import com.dutchess77.lantern.LanternConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/** Removes whitelisted torches in the area and returns them to the player. */
public final class TorchSweeper {

    private static String[] cachedTorchConfig;
    private static Set<Block> torchBlocks;

    private TorchSweeper() {
    }

    public static void sweep(Level level, Player player, BlockPos center, int r, int vr) {
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -vr, -r), center.offset(r, vr, r))) {
            if (!level.hasChunkAt(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (isTorch(state.getBlock()) && !WardRegistry.isWarded(level, pos)) {
                ItemStack drop = new ItemStack(state.getBlock());
                level.setBlock(pos.immutable(), Blocks.AIR.defaultBlockState(), 3);
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
                ResourceLocation id = ResourceLocation.tryParse(name);
                if (id == null) {
                    continue;
                }
                Block b = BuiltInRegistries.BLOCK.get(id);
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

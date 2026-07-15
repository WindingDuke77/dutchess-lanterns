package com.dutchess77.lantern.logic;

import java.util.HashSet;
import java.util.Set;

import com.dutchess77.lantern.LanternConfig;
import com.dutchess77.lantern.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;

/**
 * All world-reading rules in one place: what counts as open space, valid
 * ground, an existing lamp, and how surfaces are located in a column.
 * Pure functions - no placement, no side effects.
 */
public final class SurfaceScanner {

    /** Exact column first is implicit; these are the +-1 fallbacks (N, E, S, W, then diagonals). */
    public static final int[][] NEIGHBOR_OFFSETS = {
        {0, -1}, {1, 0}, {0, 1}, {-1, 0}, {1, -1}, {1, 1}, {-1, 1}, {-1, -1}
    };

    private static String[] cachedBlacklistConfig;
    private static Set<String> blacklist;

    private SurfaceScanner() {
    }

    public static boolean isLamp(Block block) {
        return block == ModBlocks.HIDDEN_LIGHT.get()
            || block == Blocks.GLOWSTONE;
    }

    /** Air, replaceable plants/snow, transparent blocks, and (configurably) water. */
    public static boolean isOpen(BlockState state, Level level, BlockPos pos) {
        if (state.isAir()) {
            return true;
        }
        if (state.liquid()) {
            // water counts as open so ocean/lake floors get lit; lava never does
            return LanternConfig.lightUnderwater && state.getFluidState().is(FluidTags.WATER);
        }
        return state.canBeReplaced() || !state.isSolidRender(level, pos);
    }

    /**
     * Any full OPAQUE block may host a light, bar light sources, leaves,
     * containers, unbreakables, ores, and the blacklist. Transparent blocks
     * (leaves, glass, ...) are never replaced - they only count as open space.
     */
    public static boolean isValidGround(BlockState state, Level level, BlockPos pos) {
        Block block = state.getBlock();
        if (state.getLightEmission(level, pos) > 0) { // never waste a block replacing an existing light
            return false;
        }
        if (state.is(BlockTags.LEAVES) || !state.isSolidRender(level, pos)) {
            return false;
        }
        if (state.hasBlockEntity()) {
            return false;
        }
        float hardness = state.getDestroySpeed(level, pos);
        if (hardness < 0.0F || hardness >= 10.0F) { // bedrock, obsidian, reinforced/functional blocks
            return false;
        }
        if (!state.isCollisionShapeFullBlock(level, pos)) {
            return false;
        }
        if (isOre(state) || isBlacklisted(block)) {
            return false;
        }
        return true;
    }

    /** Loaded, not already a lamp, and replaceable. */
    public static boolean canHost(Level level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        return !isLamp(state.getBlock()) && isValidGround(state, level, pos);
    }

    /**
     * Block in the column a mob could stand on (solid up-face, open above)
     * NEAREST the reference level - regardless of whether the Lantern may
     * replace it. Nearest, not topmost, so floors above the player don't
     * shadow the level the player is actually on.
     */
    public static BlockPos findStandableSurface(Level level, int x, int z, int centerY, int vr) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockPos best = null;
        int bestDistance = Integer.MAX_VALUE;
        boolean openAbove = false;
        for (int y = centerY + vr + 1; y >= centerY - vr; y--) {
            pos.set(x, y, z);
            if (!level.hasChunkAt(pos)) {
                return null;
            }
            BlockState state = level.getBlockState(pos);
            if (isOpen(state, level, pos)) {
                openAbove = true;
                continue;
            }
            if (openAbove && y <= centerY + vr && state.isFaceSturdy(level, pos, Direction.UP)) {
                int distance = Math.abs(y + 1 - centerY);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = new BlockPos(x, y, z);
                }
            }
            openAbove = false;
        }
        return best;
    }

    /**
     * Replaceable floor block in the column NEAREST the player's level -
     * valid ground (or an existing lamp, so lit columns skip cleanly) with
     * open or transparent space directly above.
     */
    public static BlockPos findFloor(Level level, int x, int z, int centerY, int vr) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockPos best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (int y = centerY + vr; y >= centerY - vr; y--) {
            pos.set(x, y, z);
            if (!level.hasChunkAt(pos)) {
                return null;
            }
            BlockState state = level.getBlockState(pos);
            if (!isLamp(state.getBlock()) && !isValidGround(state, level, pos)) {
                continue;
            }
            BlockPos up = pos.above();
            BlockState upState = level.getBlockState(up);
            if (isOpen(upState, level, up)) {
                int distance = Math.abs(y + 1 - centerY);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = new BlockPos(x, y, z);
                }
            }
        }
        return best;
    }

    /** Never convert ores into lighting: reject anything tagged as an ore. */
    public static boolean isOre(BlockState state) {
        return state.is(Tags.Blocks.ORES);
    }

    public static boolean isBlacklisted(Block block) {
        ResourceLocation name = BuiltInRegistries.BLOCK.getKey(block);
        if (cachedBlacklistConfig != LanternConfig.groundBlacklist) {
            blacklist = new HashSet<>();
            for (String entry : LanternConfig.groundBlacklist) {
                blacklist.add(entry);
            }
            cachedBlacklistConfig = LanternConfig.groundBlacklist;
        }
        return blacklist.contains(name.toString());
    }
}

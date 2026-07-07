package com.dutchess77.lantern.logic;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import com.dutchess77.lantern.LanternConfig;
import com.dutchess77.lantern.ModBlocks;
import com.dutchess77.lantern.compat.EnderIOPaintHelper;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;

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

    private static final Map<IBlockState, Boolean> ORE_CACHE = new IdentityHashMap<>();

    private static String[] cachedBlacklistConfig;
    private static Set<String> blacklist;

    private SurfaceScanner() {
    }

    public static boolean isLamp(Block block) {
        return block == ModBlocks.HIDDEN_LIGHT
            || EnderIOPaintHelper.isPaintedGlowstone(block) // legacy lights from pre-1.11 versions
            || block == Blocks.GLOWSTONE;
    }

    /** Air, replaceable plants/snow, transparent blocks, and (configurably) water. */
    public static boolean isOpen(IBlockState state, World world, BlockPos pos) {
        if (state.getBlock().isAir(state, world, pos)) {
            return true;
        }
        if (state.getMaterial().isLiquid()) {
            // water counts as open so ocean/lake floors get lit; lava never does
            return LanternConfig.lightUnderwater && state.getMaterial() == Material.WATER;
        }
        return state.getBlock().isReplaceable(world, pos) || !state.isOpaqueCube();
    }

    /**
     * Any full OPAQUE block may host a light, bar light sources, leaves,
     * containers, unbreakables, ores, and the blacklist. Transparent blocks
     * (leaves, glass, ...) are never replaced - they only count as open space.
     */
    public static boolean isValidGround(IBlockState state, World world, BlockPos pos) {
        Block block = state.getBlock();
        if (state.getLightValue(world, pos) > 0) { // never waste a block replacing an existing light
            return false;
        }
        if (state.getMaterial() == Material.LEAVES || !state.isOpaqueCube()) {
            return false;
        }
        if (block.hasTileEntity(state)) {
            return false;
        }
        if (state.getBlockHardness(world, pos) < 0.0F) { // bedrock and friends
            return false;
        }
        if (!state.isFullCube()) {
            return false;
        }
        if (isOre(state) || isBlacklisted(block)) {
            return false;
        }
        return true;
    }

    /** Loaded, not already a lamp, and replaceable. */
    public static boolean canHost(World world, BlockPos pos) {
        if (!world.isBlockLoaded(pos)) {
            return false;
        }
        IBlockState state = world.getBlockState(pos);
        return !isLamp(state.getBlock()) && isValidGround(state, world, pos);
    }

    /**
     * Block in the column a mob could stand on (solid up-face, open above)
     * NEAREST the reference level - regardless of whether the Lantern may
     * replace it. Nearest, not topmost, so floors above the player don't
     * shadow the level the player is actually on.
     */
    public static BlockPos findStandableSurface(World world, int x, int z, int centerY, int vr) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockPos best = null;
        int bestDistance = Integer.MAX_VALUE;
        boolean openAbove = false;
        for (int y = centerY + vr + 1; y >= centerY - vr; y--) {
            pos.setPos(x, y, z);
            if (!world.isBlockLoaded(pos)) {
                return null;
            }
            IBlockState state = world.getBlockState(pos);
            if (isOpen(state, world, pos)) {
                openAbove = true;
                continue;
            }
            if (openAbove && y <= centerY + vr && state.isSideSolid(world, pos, EnumFacing.UP)) {
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
    public static BlockPos findFloor(World world, int x, int z, int centerY, int vr) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockPos best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (int y = centerY + vr; y >= centerY - vr; y--) {
            pos.setPos(x, y, z);
            if (!world.isBlockLoaded(pos)) {
                return null;
            }
            IBlockState state = world.getBlockState(pos);
            if (!isLamp(state.getBlock()) && !isValidGround(state, world, pos)) {
                continue;
            }
            BlockPos up = pos.up();
            IBlockState upState = world.getBlockState(up);
            if (isOpen(upState, world, up)) {
                int distance = Math.abs(y + 1 - centerY);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = new BlockPos(x, y, z);
                }
            }
        }
        return best;
    }

    /** Never convert ores into lighting: reject anything ore-dictionaried as ore*. */
    public static boolean isOre(IBlockState state) {
        Boolean cached = ORE_CACHE.get(state);
        if (cached != null) {
            return cached;
        }
        boolean ore = false;
        Item item = Item.getItemFromBlock(state.getBlock());
        if (item != null && item != net.minecraft.init.Items.AIR) {
            int meta;
            try {
                meta = state.getBlock().getMetaFromState(state);
            } catch (Throwable t) {
                meta = 0;
            }
            ItemStack stack = new ItemStack(item, 1, meta);
            if (!stack.isEmpty()) {
                for (int id : OreDictionary.getOreIDs(stack)) {
                    if (OreDictionary.getOreName(id).startsWith("ore")) {
                        ore = true;
                        break;
                    }
                }
            }
        }
        ORE_CACHE.put(state, ore);
        return ore;
    }

    public static boolean isBlacklisted(Block block) {
        ResourceLocation name = block.getRegistryName();
        if (name == null) {
            return true;
        }
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

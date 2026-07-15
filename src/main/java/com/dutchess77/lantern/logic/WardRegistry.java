package com.dutchess77.lantern.logic;

import java.util.HashMap;
import java.util.Map;

import com.dutchess77.lantern.block.DarknessWardTileEntity;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Server-side index of loaded Darkness Ward tile entities, kept current as
 * they load/unload. Placement code asks it whether a spot lies inside any
 * ward's configured box before spending fuel there.
 */
public final class WardRegistry {

    private static final Map<Integer, Map<BlockPos, DarknessWardTileEntity>> WARDS = new HashMap<>();

    private WardRegistry() {
    }

    public static void add(World world, DarknessWardTileEntity ward) {
        WARDS.computeIfAbsent(world.provider.getDimension(), k -> new HashMap<>())
            .put(ward.getPos().toImmutable(), ward);
    }

    public static void remove(World world, BlockPos pos) {
        Map<BlockPos, DarknessWardTileEntity> wards = WARDS.get(world.provider.getDimension());
        if (wards != null) {
            wards.remove(pos);
        }
    }

    /** True when pos lies within any loaded ward's box. */
    public static boolean isWarded(World world, BlockPos pos) {
        return isWarded(world, pos, 0);
    }

    /**
     * Placement keep-out: the box plus the configured buffer, so lights set
     * just outside a ward cannot bleed light back across its border.
     */
    public static boolean isPlacementBlocked(World world, BlockPos pos) {
        return isWarded(world, pos, com.dutchess77.lantern.LanternConfig.wardBuffer);
    }

    private static boolean isWarded(World world, BlockPos pos, int margin) {
        Map<BlockPos, DarknessWardTileEntity> wards = WARDS.get(world.provider.getDimension());
        if (wards == null || wards.isEmpty()) {
            return false;
        }
        for (DarknessWardTileEntity ward : wards.values()) {
            if (!ward.isInvalid() && ward.contains(pos, margin)) {
                return true;
            }
        }
        return false;
    }
}

package com.dutchess77.lantern.logic;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.dutchess77.lantern.LanternConfig;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Server-side index of loaded Darkness Ward positions, kept current by the
 * ward tile entities as they load/unload. Placement code asks it whether a
 * spot lies inside any ward's cube before spending fuel there.
 */
public final class WardRegistry {

    private static final Map<Integer, Set<BlockPos>> WARDS = new HashMap<>();

    private WardRegistry() {
    }

    public static void add(World world, BlockPos pos) {
        WARDS.computeIfAbsent(world.provider.getDimension(), k -> new HashSet<>()).add(pos.toImmutable());
    }

    public static void remove(World world, BlockPos pos) {
        Set<BlockPos> wards = WARDS.get(world.provider.getDimension());
        if (wards != null) {
            wards.remove(pos);
        }
    }

    /** True when pos lies within the (cubic) radius of any loaded ward. */
    public static boolean isWarded(World world, BlockPos pos) {
        Set<BlockPos> wards = WARDS.get(world.provider.getDimension());
        if (wards == null || wards.isEmpty()) {
            return false;
        }
        int r = LanternConfig.wardRadius;
        for (BlockPos ward : wards) {
            if (Math.abs(ward.getX() - pos.getX()) <= r
                && Math.abs(ward.getY() - pos.getY()) <= r
                && Math.abs(ward.getZ() - pos.getZ()) <= r) {
                return true;
            }
        }
        return false;
    }
}

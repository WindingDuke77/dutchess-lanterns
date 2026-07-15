package com.dutchess77.lantern.logic;

import java.util.HashMap;
import java.util.Map;

import com.dutchess77.lantern.block.DarknessWardTileEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Server-side index of loaded Darkness Ward block entities, kept current as
 * they load/unload. Placement code asks it whether a spot lies inside any
 * ward's configured box before spending fuel there.
 */
public final class WardRegistry {

    private static final Map<ResourceKey<Level>, Map<BlockPos, DarknessWardTileEntity>> WARDS = new HashMap<>();

    private WardRegistry() {
    }

    public static void add(Level level, DarknessWardTileEntity ward) {
        WARDS.computeIfAbsent(level.dimension(), k -> new HashMap<>())
            .put(ward.getBlockPos().immutable(), ward);
    }

    public static void remove(Level level, BlockPos pos) {
        Map<BlockPos, DarknessWardTileEntity> wards = WARDS.get(level.dimension());
        if (wards != null) {
            wards.remove(pos);
        }
    }

    /** True when pos lies within any loaded ward's box. */
    public static boolean isWarded(Level level, BlockPos pos) {
        Map<BlockPos, DarknessWardTileEntity> wards = WARDS.get(level.dimension());
        if (wards == null || wards.isEmpty()) {
            return false;
        }
        for (DarknessWardTileEntity ward : wards.values()) {
            if (!ward.isRemoved() && ward.contains(pos)) {
                return true;
            }
        }
        return false;
    }
}

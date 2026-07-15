package com.dutchess77.lantern.logic;

import java.util.Map;
import java.util.WeakHashMap;

import com.dutchess77.lantern.LanternConfig;
import com.dutchess77.lantern.ModBlocks;
import com.dutchess77.lantern.block.HiddenLightTileEntity;
import com.dutchess77.lantern.fx.SparkleManager;
import com.dutchess77.lantern.item.LanternItem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Placement engine: global-grid pass plus the anchored gap-fill pass.
 * Modes: hidden (camo block), visible (plain glowstone), torch (standing torches).
 */
public class LightPlacer {

    /** How long a spot that could not actually be lit is left alone (ticks). */
    private static final long UNFIXABLE_COOLDOWN = 600;

    private final Map<Player, Long> lastFuelWarn = new WeakHashMap<>();

    /**
     * Spots the gap-filler placed for (or gave up on) that stayed dark anyway -
     * e.g. under slabs where light cannot reach. Without this, every pass
     * would burn another block trying to fix the same unfixable spot.
     */
    private final Map<Long, Long> unfixableSpots = new java.util.HashMap<>();

    private static long spotKey(Level level, BlockPos pos) {
        return pos.asLong() ^ ((long) level.dimension().location().hashCode() << 58);
    }

    /** Runs a full area pass: grid first, then gap-fill (if enabled and fuel remains). */
    public void processArea(Level level, Player player, ItemStack lantern, BlockPos center,
                            int r, int vr, boolean torchMode, boolean visible) {
        if (placeGrid(level, player, lantern, center, r, vr, torchMode, visible) && LanternConfig.fillGaps) {
            fillGapsPass(level, player, lantern, center, r, vr, torchMode, visible);
        }
    }

    /** Returns false when it ran out of fuel (skip the gap-fill pass). */
    private boolean placeGrid(Level level, Player player, ItemStack lantern, BlockPos center,
                              int r, int vr, boolean torchMode, boolean visible) {
        int spacing = Math.max(2, LanternConfig.gridSpacing);
        int minX = center.getX() - r;
        int minZ = center.getZ() - r;
        // first world-grid coordinate >= min (floorMod keeps the grid global in -X/-Z)
        int startX = minX + Math.floorMod(-minX, spacing);
        int startZ = minZ + Math.floorMod(-minZ, spacing);

        for (int gx = startX; gx <= center.getX() + r; gx += spacing) {
            for (int gz = startZ; gz <= center.getZ() + r; gz += spacing) {
                PlaceResult result = torchMode
                    ? tryPlaceTorch(level, player, lantern, gx, gz, center.getY(), vr, false)
                    : tryPlaceAtGridPoint(level, player, lantern, gx, gz, center.getY(), vr, visible);
                if (result == PlaceResult.NO_FUEL) {
                    warnNoFuel(level, player);
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Sweeps every column for spots a mob could stand on that the grid left
     * dark and fixes each one with a light anchored to that exact spot.
     * Freshly placed lights raise the light level of the surrounding columns,
     * so the pass self-limits.
     */
    private void fillGapsPass(Level level, Player player, ItemStack lantern, BlockPos center,
                              int r, int vr, boolean torchMode, boolean visible) {
        long now = level.getGameTime();
        cleanupUnfixable(now);
        for (int x = center.getX() - r; x <= center.getX() + r; x++) {
            for (int z = center.getZ() - r; z <= center.getZ() + r; z++) {
                BlockPos standable = SurfaceScanner.findStandableSurface(level, x, z, center.getY(), vr);
                if (standable == null
                    || level.getBrightness(LightLayer.BLOCK, standable.above()) > LanternConfig.lightThreshold) {
                    continue;
                }
                long key = spotKey(level, standable);
                Long retryAt = unfixableSpots.get(key);
                if (retryAt != null && now < retryAt) {
                    continue;
                }
                PlaceResult result = torchMode
                    ? tryPlaceTorch(level, player, lantern, x, z, center.getY(), vr, true)
                    : rescueDark(level, player, lantern, standable, visible);
                if (result == PlaceResult.NO_FUEL) {
                    warnNoFuel(level, player);
                    return;
                }
                // still dark after placing (or nothing placeable)? leave it alone for a while
                if (result == PlaceResult.SKIP
                    || level.getBrightness(LightLayer.BLOCK, standable.above()) <= LanternConfig.lightThreshold) {
                    unfixableSpots.put(key, now + UNFIXABLE_COOLDOWN);
                }
            }
        }
    }

    private void cleanupUnfixable(long now) {
        if (unfixableSpots.size() > 8192) {
            unfixableSpots.values().removeIf(retryAt -> now >= retryAt);
        }
    }

    /** Grid pass: floors only - walls are handled by the anchored gap-fill rescue. */
    private PlaceResult tryPlaceAtGridPoint(Level level, Player player, ItemStack lantern,
                                            int gx, int gz, int centerY, int vr, boolean visible) {
        BlockPos floor = SurfaceScanner.findFloor(level, gx, gz, centerY, vr);
        if (floor == null) {
            // exact grid column obstructed: shift by one, first valid column wins
            for (int[] offset : SurfaceScanner.NEIGHBOR_OFFSETS) {
                floor = SurfaceScanner.findFloor(level, gx + offset[0], gz + offset[1], centerY, vr);
                if (floor != null) {
                    break;
                }
            }
        }
        if (floor == null) {
            return PlaceResult.SKIP;
        }
        if (SurfaceScanner.isLamp(level.getBlockState(floor).getBlock())) {
            return PlaceResult.SKIP;
        }
        // block light only: daylight must not mask ground that goes dark at night
        if (level.getBrightness(LightLayer.BLOCK, floor.above()) > LanternConfig.lightThreshold) {
            return PlaceResult.SKIP;
        }
        return placeAt(level, player, lantern, floor, floor.above(), visible);
    }

    /**
     * Lights a verified-dark standable spot with a placement anchored to it,
     * so the new light actually reaches the spot: floor under it first, then
     * the walls beside it, then a neighboring column's floor within 2 blocks
     * of its height. Light checks are skipped - the spot is known dark.
     */
    private PlaceResult rescueDark(Level level, Player player, ItemStack lantern,
                                   BlockPos surface, boolean visible) {
        BlockPos air = surface.above();
        if (SurfaceScanner.canHost(level, surface)) {
            return placeAt(level, player, lantern, surface, air, visible);
        }
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            BlockPos wall = air.relative(facing);
            if (SurfaceScanner.canHost(level, wall)) {
                return placeAt(level, player, lantern, wall, air, visible);
            }
        }
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            BlockPos wall = surface.relative(facing);
            if (SurfaceScanner.canHost(level, wall)) {
                return placeAt(level, player, lantern, wall, air, visible);
            }
        }
        for (int[] offset : SurfaceScanner.NEIGHBOR_OFFSETS) {
            BlockPos neighborFloor = SurfaceScanner.findStandableSurface(level,
                surface.getX() + offset[0], surface.getZ() + offset[1], surface.getY(), 2);
            if (neighborFloor != null && SurfaceScanner.canHost(level, neighborFloor)) {
                return placeAt(level, player, lantern, neighborFloor, air, visible);
            }
        }
        return PlaceResult.SKIP;
    }

    /** Torch mode: stand a vanilla torch on the surface instead of replacing it. */
    private PlaceResult tryPlaceTorch(Level level, Player player, ItemStack lantern,
                                      int x, int z, int centerY, int vr, boolean ignoreLight) {
        BlockPos spot = null;
        for (int i = -1; i < SurfaceScanner.NEIGHBOR_OFFSETS.length && spot == null; i++) {
            int cx = i < 0 ? x : x + SurfaceScanner.NEIGHBOR_OFFSETS[i][0];
            int cz = i < 0 ? z : z + SurfaceScanner.NEIGHBOR_OFFSETS[i][1];
            BlockPos ground = SurfaceScanner.findStandableSurface(level, cx, cz, centerY, vr);
            if (ground == null) {
                continue;
            }
            BlockPos above = ground.above();
            BlockState aboveState = level.getBlockState(above);
            boolean replaceable = aboveState.isAir()
                || (aboveState.canBeReplaced() && !aboveState.liquid());
            if (replaceable && Blocks.TORCH.defaultBlockState().canSurvive(level, above)) {
                spot = above;
            }
        }
        if (spot == null || WardRegistry.isWarded(level, spot)) {
            return PlaceResult.SKIP;
        }
        if (!ignoreLight
            && level.getBrightness(LightLayer.BLOCK, spot) > LanternConfig.lightThreshold) {
            return PlaceResult.SKIP;
        }
        if (!((LanternItem) lantern.getItem()).consumePlacementCost(player, lantern)) {
            return PlaceResult.NO_FUEL;
        }
        level.setBlock(spot, Blocks.TORCH.defaultBlockState(), 3);
        SparkleManager.add(level, spot);
        return PlaceResult.PLACED;
    }

    /** Consumes fuel and swaps the target block for a light; sparkles at the spot it serves. */
    private PlaceResult placeAt(Level level, Player player, ItemStack lantern,
                                BlockPos target, BlockPos sparkleAt, boolean visible) {
        if (WardRegistry.isWarded(level, target)) {
            return PlaceResult.SKIP; // Darkness Ward: this area stays dark
        }
        if (!((LanternItem) lantern.getItem()).consumePlacementCost(player, lantern)) {
            return PlaceResult.NO_FUEL;
        }
        BlockState original = level.getBlockState(target);
        if (visible) {
            level.setBlock(target, Blocks.GLOWSTONE.defaultBlockState(), 3);
        } else {
            level.setBlock(target, ModBlocks.HIDDEN_LIGHT.get().defaultBlockState(), 3);
            BlockEntity be = level.getBlockEntity(target);
            if (be instanceof HiddenLightTileEntity hidden) {
                hidden.setMimic(original,
                    !((LanternItem) lantern.getItem()).lightsRefundGlowstone());
            }
        }
        SparkleManager.add(level, sparkleAt);
        return PlaceResult.PLACED;
    }

    private void warnNoFuel(Level level, Player player) {
        Long last = lastFuelWarn.get(player);
        long now = level.getGameTime();
        if (last == null || now - last >= 100) {
            lastFuelWarn.put(player, now);
            player.displayClientMessage(Component.translatable("chat.lantern.no_fuel"), true);
        }
    }
}

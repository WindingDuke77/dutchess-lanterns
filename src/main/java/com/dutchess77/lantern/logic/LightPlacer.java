package com.dutchess77.lantern.logic;

import java.util.Map;
import java.util.WeakHashMap;

import com.dutchess77.lantern.LanternConfig;
import com.dutchess77.lantern.ModBlocks;
import com.dutchess77.lantern.block.HiddenLightTileEntity;
import com.dutchess77.lantern.fx.SparkleManager;
import com.dutchess77.lantern.item.EnergyLanternItem;
import com.dutchess77.lantern.item.LanternItem;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;

/**
 * Placement engine: global-grid pass plus the anchored gap-fill pass.
 * Modes: hidden (camo block), visible (plain glowstone), torch (standing torches).
 */
public class LightPlacer {

    /** How long a spot that could not actually be lit is left alone (ticks). */
    private static final long UNFIXABLE_COOLDOWN = 600;

    private final Map<EntityPlayer, Long> lastFuelWarn = new WeakHashMap<>();

    /**
     * Spots the gap-filler placed for (or gave up on) that stayed dark anyway -
     * e.g. under slabs where light cannot reach. Without this, every pass
     * would burn another block trying to fix the same unfixable spot.
     */
    private final Map<Long, Long> unfixableSpots = new java.util.HashMap<>();

    private static long spotKey(World world, BlockPos pos) {
        return pos.toLong() ^ ((long) world.provider.getDimension() << 58);
    }

    /** Runs a full area pass: grid first, then gap-fill (if enabled and fuel remains). */
    public void processArea(World world, EntityPlayer player, ItemStack lantern, BlockPos center,
                            int r, int vr, boolean torchMode, boolean visible) {
        if (placeGrid(world, player, lantern, center, r, vr, torchMode, visible) && LanternConfig.fillGaps) {
            fillGapsPass(world, player, lantern, center, r, vr, torchMode, visible);
        }
    }

    /** Returns false when it ran out of fuel (skip the gap-fill pass). */
    private boolean placeGrid(World world, EntityPlayer player, ItemStack lantern, BlockPos center,
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
                    ? tryPlaceTorch(world, player, lantern, gx, gz, center.getY(), vr, false)
                    : tryPlaceAtGridPoint(world, player, lantern, gx, gz, center.getY(), vr, visible);
                if (result == PlaceResult.NO_FUEL) {
                    warnNoFuel(world, player);
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
    private void fillGapsPass(World world, EntityPlayer player, ItemStack lantern, BlockPos center,
                              int r, int vr, boolean torchMode, boolean visible) {
        long now = world.getTotalWorldTime();
        cleanupUnfixable(now);
        for (int x = center.getX() - r; x <= center.getX() + r; x++) {
            for (int z = center.getZ() - r; z <= center.getZ() + r; z++) {
                BlockPos standable = SurfaceScanner.findStandableSurface(world, x, z, center.getY(), vr);
                if (standable == null
                    || world.getLightFor(EnumSkyBlock.BLOCK, standable.up()) > LanternConfig.lightThreshold) {
                    continue;
                }
                long key = spotKey(world, standable);
                Long retryAt = unfixableSpots.get(key);
                if (retryAt != null && now < retryAt) {
                    continue;
                }
                PlaceResult result = torchMode
                    ? tryPlaceTorch(world, player, lantern, x, z, center.getY(), vr, true)
                    : rescueDark(world, player, lantern, standable, visible);
                if (result == PlaceResult.NO_FUEL) {
                    warnNoFuel(world, player);
                    return;
                }
                // still dark after placing (or nothing placeable)? leave it alone for a while
                if (result == PlaceResult.SKIP
                    || world.getLightFor(EnumSkyBlock.BLOCK, standable.up()) <= LanternConfig.lightThreshold) {
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
    private PlaceResult tryPlaceAtGridPoint(World world, EntityPlayer player, ItemStack lantern,
                                            int gx, int gz, int centerY, int vr, boolean visible) {
        BlockPos floor = SurfaceScanner.findFloor(world, gx, gz, centerY, vr);
        if (floor == null) {
            // exact grid column obstructed: shift by one, first valid column wins
            for (int[] offset : SurfaceScanner.NEIGHBOR_OFFSETS) {
                floor = SurfaceScanner.findFloor(world, gx + offset[0], gz + offset[1], centerY, vr);
                if (floor != null) {
                    break;
                }
            }
        }
        if (floor == null) {
            return PlaceResult.SKIP;
        }
        if (SurfaceScanner.isLamp(world.getBlockState(floor).getBlock())) {
            return PlaceResult.SKIP;
        }
        // block light only: daylight must not mask ground that goes dark at night
        if (world.getLightFor(EnumSkyBlock.BLOCK, floor.up()) > LanternConfig.lightThreshold) {
            return PlaceResult.SKIP;
        }
        return placeAt(world, player, lantern, floor, floor.up(), visible);
    }

    /**
     * Lights a verified-dark standable spot with a placement anchored to it,
     * so the new light actually reaches the spot: floor under it first, then
     * the walls beside it, then a neighboring column's floor within 2 blocks
     * of its height. Light checks are skipped - the spot is known dark.
     */
    private PlaceResult rescueDark(World world, EntityPlayer player, ItemStack lantern,
                                   BlockPos surface, boolean visible) {
        BlockPos air = surface.up();
        if (SurfaceScanner.canHost(world, surface)) {
            return placeAt(world, player, lantern, surface, air, visible);
        }
        for (EnumFacing facing : EnumFacing.HORIZONTALS) {
            BlockPos wall = air.offset(facing);
            if (SurfaceScanner.canHost(world, wall)) {
                return placeAt(world, player, lantern, wall, air, visible);
            }
        }
        for (EnumFacing facing : EnumFacing.HORIZONTALS) {
            BlockPos wall = surface.offset(facing);
            if (SurfaceScanner.canHost(world, wall)) {
                return placeAt(world, player, lantern, wall, air, visible);
            }
        }
        for (int[] offset : SurfaceScanner.NEIGHBOR_OFFSETS) {
            BlockPos neighborFloor = SurfaceScanner.findStandableSurface(world,
                surface.getX() + offset[0], surface.getZ() + offset[1], surface.getY(), 2);
            if (neighborFloor != null && SurfaceScanner.canHost(world, neighborFloor)) {
                return placeAt(world, player, lantern, neighborFloor, air, visible);
            }
        }
        return PlaceResult.SKIP;
    }

    /** Torch mode: stand a vanilla torch on the surface instead of replacing it. */
    private PlaceResult tryPlaceTorch(World world, EntityPlayer player, ItemStack lantern,
                                      int x, int z, int centerY, int vr, boolean ignoreLight) {
        BlockPos spot = null;
        for (int i = -1; i < SurfaceScanner.NEIGHBOR_OFFSETS.length && spot == null; i++) {
            int cx = i < 0 ? x : x + SurfaceScanner.NEIGHBOR_OFFSETS[i][0];
            int cz = i < 0 ? z : z + SurfaceScanner.NEIGHBOR_OFFSETS[i][1];
            BlockPos ground = SurfaceScanner.findStandableSurface(world, cx, cz, centerY, vr);
            if (ground == null) {
                continue;
            }
            BlockPos above = ground.up();
            IBlockState aboveState = world.getBlockState(above);
            boolean replaceable = aboveState.getBlock().isAir(aboveState, world, above)
                || (aboveState.getBlock().isReplaceable(world, above) && !aboveState.getMaterial().isLiquid());
            if (replaceable && Blocks.TORCH.canPlaceBlockAt(world, above)) {
                spot = above;
            }
        }
        if (spot == null || WardRegistry.isPlacementBlocked(world, spot)) {
            return PlaceResult.SKIP;
        }
        if (!ignoreLight
            && world.getLightFor(EnumSkyBlock.BLOCK, spot) > LanternConfig.lightThreshold) {
            return PlaceResult.SKIP;
        }
        if (!((LanternItem) lantern.getItem()).consumePlacementCost(player, lantern)) {
            return PlaceResult.NO_FUEL;
        }
        world.setBlockState(spot, Blocks.TORCH.getDefaultState(), 3);
        SparkleManager.add(world, spot);
        return PlaceResult.PLACED;
    }

    /** Consumes fuel and swaps the target block for a light; sparkles at the spot it serves. */
    private PlaceResult placeAt(World world, EntityPlayer player, ItemStack lantern,
                                BlockPos target, BlockPos sparkleAt, boolean visible) {
        if (WardRegistry.isPlacementBlocked(world, target)) {
            return PlaceResult.SKIP; // Darkness Ward: this area (plus buffer) stays dark
        }
        if (!((LanternItem) lantern.getItem()).consumePlacementCost(player, lantern)) {
            return PlaceResult.NO_FUEL;
        }
        IBlockState original = world.getBlockState(target);
        if (visible) {
            world.setBlockState(target, Blocks.GLOWSTONE.getDefaultState(), 3);
        } else {
            world.setBlockState(target, ModBlocks.HIDDEN_LIGHT.getDefaultState(), 3);
            TileEntity te = world.getTileEntity(target);
            if (te instanceof HiddenLightTileEntity) {
                ((HiddenLightTileEntity) te).setMimic(original,
                    !((LanternItem) lantern.getItem()).lightsRefundGlowstone());
            }
        }
        SparkleManager.add(world, sparkleAt);
        return PlaceResult.PLACED;
    }

    private void warnNoFuel(World world, EntityPlayer player) {
        Long last = lastFuelWarn.get(player);
        long now = world.getTotalWorldTime();
        if (last == null || now - last >= 100) {
            lastFuelWarn.put(player, now);
            player.sendStatusMessage(new TextComponentTranslation("chat.lantern.no_fuel"), true);
        }
    }
}

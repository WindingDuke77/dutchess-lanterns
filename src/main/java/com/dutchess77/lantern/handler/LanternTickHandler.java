package com.dutchess77.lantern.handler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

import com.dutchess77.lantern.LanternConfig;
import com.dutchess77.lantern.compat.EnderIOPaintHelper;
import com.dutchess77.lantern.item.CreativeLanternItem;
import com.dutchess77.lantern.item.LanternItem;
import com.dutchess77.lantern.item.TorchLanternItem;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.oredict.OreDictionary;

public class LanternTickHandler {

    /** Exact grid column first is implicit; these are the +-1 fallbacks (N, E, S, W, then diagonals). */
    private static final int[][] NEIGHBOR_OFFSETS = {
        {0, -1}, {1, 0}, {0, 1}, {-1, 0}, {1, -1}, {1, 1}, {-1, 1}, {-1, -1}
    };

    private static final Map<IBlockState, Boolean> ORE_CACHE = new IdentityHashMap<>();

    private static String[] cachedTorchConfig;
    private static Set<Block> torchBlocks;

    private final Set<UUID> warnedNoEnderIO = new HashSet<>();
    private final Map<EntityPlayer, Long> lastFuelWarn = new WeakHashMap<>();
    private final List<Sparkle> sparkles = new ArrayList<>();

    private enum PlaceResult { PLACED, SKIP, NO_FUEL }

    private static final class Sparkle {
        final WorldServer world;
        final BlockPos pos;
        final long expiry;

        Sparkle(WorldServer world, BlockPos pos, long expiry) {
            this.world = world;
            this.pos = pos;
            this.expiry = expiry;
        }
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.world.isRemote || sparkles.isEmpty()
            || event.world.getTotalWorldTime() % 4 != 0) {
            return;
        }
        Iterator<Sparkle> it = sparkles.iterator();
        while (it.hasNext()) {
            Sparkle sparkle = it.next();
            if (sparkle.world != event.world) {
                continue;
            }
            if (event.world.getTotalWorldTime() >= sparkle.expiry) {
                it.remove();
                continue;
            }
            sparkle.world.spawnParticle(EnumParticleTypes.END_ROD,
                sparkle.pos.getX() + 0.5D, sparkle.pos.getY() + 0.4D, sparkle.pos.getZ() + 0.5D,
                2, 0.25D, 0.15D, 0.25D, 0.005D);
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        EntityPlayer player = event.player;
        World world = player.world;
        if (world.isRemote || player.ticksExisted % LanternConfig.tickInterval != 0) {
            return;
        }
        ItemStack lantern = findActiveLantern(player);
        if (lantern.isEmpty()) {
            return;
        }

        BlockPos center = new BlockPos(player);
        int r = LanternConfig.horizontalRadius;
        int vr = LanternConfig.verticalRange;

        if (lantern.getItem() instanceof TorchLanternItem) {
            // torch mode: no sweep, no EnderIO needed
            if (placeGrid(world, player, lantern, center, r, vr, true, false)
                && LanternConfig.fillGaps) {
                fillGapsPass(world, player, lantern, center, r, vr, true, false);
            }
            return;
        }

        // creative lantern places plain visible glowstone, no EnderIO needed
        boolean visible = lantern.getItem() instanceof CreativeLanternItem;
        if (!visible && !EnderIOPaintHelper.isAvailable()) {
            if (warnedNoEnderIO.add(player.getUniqueID())) {
                player.sendMessage(new TextComponentTranslation("chat.lantern.no_enderio"));
            }
            return;
        }

        sweepTorches(world, player, center, r, vr);
        if (placeGrid(world, player, lantern, center, r, vr, false, visible) && LanternConfig.fillGaps) {
            fillGapsPass(world, player, lantern, center, r, vr, false, visible);
        }
    }

    private static ItemStack findActiveLantern(EntityPlayer player) {
        if (isActiveLantern(player.getHeldItemMainhand())) {
            return player.getHeldItemMainhand();
        }
        if (isActiveLantern(player.getHeldItemOffhand())) {
            return player.getHeldItemOffhand();
        }
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = player.inventory.mainInventory.get(slot);
            if (isActiveLantern(stack)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static boolean isActiveLantern(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof LanternItem && LanternItem.isActive(stack);
    }

    // ---------------------------------------------------------------- torches

    private void sweepTorches(World world, EntityPlayer player, BlockPos center, int r, int vr) {
        for (BlockPos pos : BlockPos.getAllInBoxMutable(center.add(-r, -vr, -r), center.add(r, vr, r))) {
            if (!world.isBlockLoaded(pos)) {
                continue;
            }
            IBlockState state = world.getBlockState(pos);
            if (isTorch(state.getBlock())) {
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

    // ------------------------------------------------------------ grid lights

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
     * dark and fixes each one with a light anchored to that exact spot: the
     * floor under it, a wall directly beside it, or a neighboring floor at
     * similar height. Freshly placed lights raise the light level of the
     * surrounding columns, so the pass self-limits.
     */
    private void fillGapsPass(World world, EntityPlayer player, ItemStack lantern, BlockPos center,
                              int r, int vr, boolean torchMode, boolean visible) {
        for (int x = center.getX() - r; x <= center.getX() + r; x++) {
            for (int z = center.getZ() - r; z <= center.getZ() + r; z++) {
                BlockPos standable = findStandableSurface(world, x, z, center.getY(), vr);
                if (standable == null
                    || world.getLightFor(EnumSkyBlock.BLOCK, standable.up()) > LanternConfig.lightThreshold) {
                    continue;
                }
                PlaceResult result = torchMode
                    ? tryPlaceTorch(world, player, lantern, x, z, center.getY(), vr, true)
                    : rescueDark(world, player, lantern, standable, visible);
                if (result == PlaceResult.NO_FUEL) {
                    warnNoFuel(world, player);
                    return;
                }
            }
        }
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
        if (canHost(world, surface)) {
            return placeAt(world, player, lantern, surface, air, visible);
        }
        for (EnumFacing facing : EnumFacing.HORIZONTALS) {
            BlockPos wall = air.offset(facing);
            if (canHost(world, wall)) {
                return placeAt(world, player, lantern, wall, air, visible);
            }
        }
        for (EnumFacing facing : EnumFacing.HORIZONTALS) {
            BlockPos wall = surface.offset(facing);
            if (canHost(world, wall)) {
                return placeAt(world, player, lantern, wall, air, visible);
            }
        }
        for (int[] offset : NEIGHBOR_OFFSETS) {
            BlockPos neighborFloor = findStandableSurface(world,
                surface.getX() + offset[0], surface.getZ() + offset[1], surface.getY(), 2);
            if (neighborFloor != null && canHost(world, neighborFloor)) {
                return placeAt(world, player, lantern, neighborFloor, air, visible);
            }
        }
        return PlaceResult.SKIP;
    }

    private static boolean canHost(World world, BlockPos pos) {
        if (!world.isBlockLoaded(pos)) {
            return false;
        }
        IBlockState state = world.getBlockState(pos);
        return !isLamp(state.getBlock()) && isValidGround(state, world, pos);
    }

    /** Torch mode: stand a vanilla torch on the surface instead of replacing it. */
    private PlaceResult tryPlaceTorch(World world, EntityPlayer player, ItemStack lantern,
                                      int x, int z, int centerY, int vr, boolean ignoreLight) {
        BlockPos spot = null;
        for (int i = -1; i < NEIGHBOR_OFFSETS.length && spot == null; i++) {
            int cx = i < 0 ? x : x + NEIGHBOR_OFFSETS[i][0];
            int cz = i < 0 ? z : z + NEIGHBOR_OFFSETS[i][1];
            BlockPos ground = findStandableSurface(world, cx, cz, centerY, vr);
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
        if (spot == null) {
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
        if (world instanceof WorldServer && LanternConfig.sparkleSeconds > 0) {
            sparkles.add(new Sparkle((WorldServer) world, spot,
                world.getTotalWorldTime() + LanternConfig.sparkleSeconds * 20L));
        }
        return PlaceResult.PLACED;
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

    /** Grid pass: floors only - walls are handled by the anchored gap-fill rescue. */
    private PlaceResult tryPlaceAtGridPoint(World world, EntityPlayer player, ItemStack lantern,
                                            int gx, int gz, int centerY, int vr, boolean visible) {
        BlockPos floor = findFloor(world, gx, gz, centerY, vr);
        if (floor == null) {
            // exact grid column obstructed: shift by one, first valid column wins
            for (int[] offset : NEIGHBOR_OFFSETS) {
                floor = findFloor(world, gx + offset[0], gz + offset[1], centerY, vr);
                if (floor != null) {
                    break;
                }
            }
        }
        if (floor == null) {
            return PlaceResult.SKIP;
        }
        if (isLamp(world.getBlockState(floor).getBlock())) {
            return PlaceResult.SKIP;
        }
        // block light only: daylight must not mask ground that goes dark at night
        if (world.getLightFor(EnumSkyBlock.BLOCK, floor.up()) > LanternConfig.lightThreshold) {
            return PlaceResult.SKIP;
        }
        return placeAt(world, player, lantern, floor, floor.up(), visible);
    }

    /** Consumes fuel and swaps the target block for a light; sparkles at the spot it serves. */
    private PlaceResult placeAt(World world, EntityPlayer player, ItemStack lantern,
                                BlockPos target, BlockPos sparkleAt, boolean visible) {
        if (!((LanternItem) lantern.getItem()).consumePlacementCost(player, lantern)) {
            return PlaceResult.NO_FUEL;
        }
        IBlockState original = world.getBlockState(target);
        if (visible) {
            world.setBlockState(target, Blocks.GLOWSTONE.getDefaultState(), 3);
        } else {
            world.setBlockState(target, EnderIOPaintHelper.paintedGlowstoneSolid().getDefaultState(), 3);
            TileEntity te = world.getTileEntity(target);
            EnderIOPaintHelper.paint(te, original);
            if (te != null) {
                te.markDirty();
            }
            IBlockState placed = world.getBlockState(target);
            world.notifyBlockUpdate(target, placed, placed, 3);
        }
        if (world instanceof WorldServer && LanternConfig.sparkleSeconds > 0) {
            sparkles.add(new Sparkle((WorldServer) world, sparkleAt,
                world.getTotalWorldTime() + LanternConfig.sparkleSeconds * 20L));
        }
        return PlaceResult.PLACED;
    }

    /**
     * Replaceable floor block in the column NEAREST the player's level -
     * valid ground (or an existing lamp, so lit columns skip cleanly) with
     * open or transparent space directly above. Nearest, not topmost, so a
     * floor or overhang above the player doesn't hijack the placement.
     */
    private BlockPos findFloor(World world, int x, int z, int centerY, int vr) {
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

    public static boolean isLamp(Block block) {
        return EnderIOPaintHelper.isPaintedGlowstone(block) || block == Blocks.GLOWSTONE;
    }

    private static boolean isOpen(IBlockState state, World world, BlockPos pos) {
        if (state.getBlock().isAir(state, world, pos)) {
            return true;
        }
        if (state.getMaterial().isLiquid()) {
            return false;
        }
        // replaceable plants/snow, or any transparent block (glass, leaves, ...)
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
        String nameString = name.toString();
        for (String entry : LanternConfig.groundBlacklist) {
            if (nameString.equals(entry)) {
                return true;
            }
        }
        return false;
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

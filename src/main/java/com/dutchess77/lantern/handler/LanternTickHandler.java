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
import com.dutchess77.lantern.item.LanternItem;

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

    /** Faces a light may shine out of: floor placements first, then wall placements. */
    private static final EnumFacing[] EXPOSED_FACES = {
        EnumFacing.UP, EnumFacing.NORTH, EnumFacing.EAST, EnumFacing.SOUTH, EnumFacing.WEST
    };

    private static final Set<Material> GROUND_MATERIALS = new HashSet<>();
    static {
        GROUND_MATERIALS.add(Material.GROUND);
        GROUND_MATERIALS.add(Material.GRASS);
        GROUND_MATERIALS.add(Material.ROCK);
        GROUND_MATERIALS.add(Material.SAND);
        GROUND_MATERIALS.add(Material.CLAY);
        GROUND_MATERIALS.add(Material.CRAFTED_SNOW);
    }

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
        if (!EnderIOPaintHelper.isAvailable()) {
            if (warnedNoEnderIO.add(player.getUniqueID())) {
                player.sendMessage(new TextComponentTranslation("chat.lantern.no_enderio"));
            }
            return;
        }

        BlockPos center = new BlockPos(player);
        int r = LanternConfig.horizontalRadius;
        int vr = LanternConfig.verticalRange;

        sweepTorches(world, player, center, r, vr);
        placeLights(world, player, lantern, center, r, vr);
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

    private void placeLights(World world, EntityPlayer player, ItemStack lantern, BlockPos center, int r, int vr) {
        int spacing = Math.max(2, LanternItem.getSpacing(lantern));
        int minX = center.getX() - r;
        int minZ = center.getZ() - r;
        // first world-grid coordinate >= min (floorMod keeps the grid global in -X/-Z)
        int startX = minX + Math.floorMod(-minX, spacing);
        int startZ = minZ + Math.floorMod(-minZ, spacing);

        for (int gx = startX; gx <= center.getX() + r; gx += spacing) {
            for (int gz = startZ; gz <= center.getZ() + r; gz += spacing) {
                PlaceResult result = tryPlaceAtGridPoint(world, player, lantern, gx, gz, center.getY(), vr);
                if (result == PlaceResult.NO_FUEL) {
                    warnNoFuel(world, player);
                    return;
                }
            }
        }
    }

    /** A solid block the light can be embedded in, plus the open face it shines from. */
    private static final class Spot {
        final BlockPos target;
        final BlockPos open;

        Spot(BlockPos target, BlockPos open) {
            this.target = target;
            this.open = open;
        }
    }

    private PlaceResult tryPlaceAtGridPoint(World world, EntityPlayer player, ItemStack lantern,
                                            int gx, int gz, int centerY, int vr) {
        Spot spot = findSpot(world, gx, gz, centerY, vr);
        if (spot == null) {
            // exact grid column obstructed: shift by one, first valid column wins
            for (int[] offset : NEIGHBOR_OFFSETS) {
                spot = findSpot(world, gx + offset[0], gz + offset[1], centerY, vr);
                if (spot != null) {
                    break;
                }
            }
        }
        if (spot == null) {
            return PlaceResult.SKIP;
        }
        return placeIfDark(world, player, lantern, spot);
    }

    private PlaceResult placeIfDark(World world, EntityPlayer player, ItemStack lantern, Spot spot) {
        IBlockState original = world.getBlockState(spot.target);
        if (EnderIOPaintHelper.isPaintedGlowstone(original.getBlock())) {
            return PlaceResult.SKIP;
        }
        // block light only: daylight must not mask ground that goes dark at night
        if (world.getLightFor(EnumSkyBlock.BLOCK, spot.open) > LanternConfig.lightThreshold) {
            return PlaceResult.SKIP;
        }
        if (!((LanternItem) lantern.getItem()).consumePlacementCost(player, lantern)) {
            return PlaceResult.NO_FUEL;
        }
        world.setBlockState(spot.target, EnderIOPaintHelper.paintedGlowstoneSolid().getDefaultState(), 3);
        TileEntity te = world.getTileEntity(spot.target);
        EnderIOPaintHelper.paint(te, original);
        if (te != null) {
            te.markDirty();
        }
        IBlockState placed = world.getBlockState(spot.target);
        world.notifyBlockUpdate(spot.target, placed, placed, 3);
        if (world instanceof WorldServer && LanternConfig.sparkleSeconds > 0) {
            sparkles.add(new Sparkle((WorldServer) world, spot.open,
                world.getTotalWorldTime() + LanternConfig.sparkleSeconds * 20L));
        }
        return PlaceResult.PLACED;
    }

    /**
     * Topmost solid block in the column (within the vertical window) with an
     * exposed face - open or transparent above (floors, incl. under glass) or
     * beside it (walls). Only valid ground or an already-placed light counts.
     */
    private Spot findSpot(World world, int x, int z, int centerY, int vr) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = centerY + vr; y >= centerY - vr; y--) {
            pos.setPos(x, y, z);
            if (!world.isBlockLoaded(pos)) {
                return null;
            }
            IBlockState state = world.getBlockState(pos);
            boolean candidate = EnderIOPaintHelper.isPaintedGlowstone(state.getBlock())
                || isValidGround(state, world, pos);
            if (!candidate) {
                continue;
            }
            BlockPos open = findOpenFace(world, pos);
            if (open != null) {
                return new Spot(new BlockPos(x, y, z), open);
            }
        }
        return null;
    }

    private static BlockPos findOpenFace(World world, BlockPos pos) {
        for (EnumFacing face : EXPOSED_FACES) {
            BlockPos neighbor = pos.offset(face);
            if (!world.isBlockLoaded(neighbor)) {
                continue;
            }
            IBlockState state = world.getBlockState(neighbor);
            if (isOpen(state, world, neighbor)) {
                return neighbor;
            }
        }
        return null;
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

    private static boolean isValidGround(IBlockState state, World world, BlockPos pos) {
        Block block = state.getBlock();
        if (!GROUND_MATERIALS.contains(state.getMaterial())) {
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
    private static boolean isOre(IBlockState state) {
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

    private static boolean isBlacklisted(Block block) {
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

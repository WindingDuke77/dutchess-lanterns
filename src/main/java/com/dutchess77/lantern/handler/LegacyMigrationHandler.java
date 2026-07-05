package com.dutchess77.lantern.handler;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import com.dutchess77.lantern.Lantern;
import com.dutchess77.lantern.LanternConfig;
import com.dutchess77.lantern.ModBlocks;
import com.dutchess77.lantern.block.HiddenLightTileEntity;
import com.dutchess77.lantern.compat.EnderIOPaintHelper;

import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Converts legacy EnderIO painted-glowstone lights (placed by pre-1.11
 * versions of this mod) into lantern:hidden_light, for free, as chunks load
 * in existing worlds. Only the solid painted glowstone variant is touched,
 * and only its tile-entity map is scanned - no full-chunk block sweep.
 */
public class LegacyMigrationHandler {

    /** Chunks queued at load time; converted on a later world tick when mutation is safe. */
    private final Map<World, Deque<ChunkPos>> pending = new WeakHashMap<>();
    private final Map<World, Set<ChunkPos>> queued = new WeakHashMap<>();

    private static final int CHUNKS_PER_TICK = 2;

    private int migratedTotal;

    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        World world = event.getWorld();
        if (world.isRemote || !LanternConfig.migrateLegacyLights || !EnderIOPaintHelper.isAvailable()) {
            return;
        }
        ChunkPos pos = event.getChunk().getPos();
        if (queued.computeIfAbsent(world, w -> new HashSet<>()).add(pos)) {
            pending.computeIfAbsent(world, w -> new ArrayDeque<>()).add(pos);
        }
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.world.isRemote) {
            return;
        }
        Deque<ChunkPos> queue = pending.get(event.world);
        if (queue == null || queue.isEmpty()) {
            return;
        }
        Set<ChunkPos> seen = queued.get(event.world);
        for (int i = 0; i < CHUNKS_PER_TICK && !queue.isEmpty(); i++) {
            ChunkPos pos = queue.poll();
            if (seen != null) {
                seen.remove(pos);
            }
            Chunk chunk = event.world.getChunkProvider().getLoadedChunk(pos.x, pos.z);
            if (chunk != null) {
                migrateChunk(event.world, chunk);
            }
        }
    }

    private void migrateChunk(World world, Chunk chunk) {
        // copy: converting mutates the chunk's tile entity map
        List<Map.Entry<BlockPos, TileEntity>> entries = new ArrayList<>(chunk.getTileEntityMap().entrySet());
        int count = 0;
        for (Map.Entry<BlockPos, TileEntity> entry : entries) {
            BlockPos pos = entry.getKey();
            if (!EnderIOPaintHelper.isPaintedGlowstone(world.getBlockState(pos).getBlock())) {
                continue;
            }
            IBlockState paint = EnderIOPaintHelper.getPaint(entry.getValue());
            world.setBlockState(pos, ModBlocks.HIDDEN_LIGHT.getDefaultState(), 3);
            TileEntity te = world.getTileEntity(pos);
            if (te instanceof HiddenLightTileEntity) {
                ((HiddenLightTileEntity) te).setMimic(paint, false);
            }
            count++;
        }
        if (count > 0) {
            migratedTotal += count;
            Lantern.LOGGER.info("Migrated {} legacy EnderIO light(s) in chunk {},{} ({} total this session)",
                count, chunk.x, chunk.z, migratedTotal);
        }
    }
}

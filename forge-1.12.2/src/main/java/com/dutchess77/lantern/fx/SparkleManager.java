package com.dutchess77.lantern.fx;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.dutchess77.lantern.LanternConfig;

import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Temporary end-rod sparkles above freshly placed lights. Server side only. */
public class SparkleManager {

    private static final List<Sparkle> SPARKLES = new ArrayList<>();

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

    public static void add(World world, BlockPos pos) {
        if (world instanceof WorldServer && LanternConfig.sparkleSeconds > 0) {
            SPARKLES.add(new Sparkle((WorldServer) world, pos,
                world.getTotalWorldTime() + LanternConfig.sparkleSeconds * 20L));
        }
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.world.isRemote || SPARKLES.isEmpty()
            || event.world.getTotalWorldTime() % 4 != 0) {
            return;
        }
        Iterator<Sparkle> it = SPARKLES.iterator();
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
}

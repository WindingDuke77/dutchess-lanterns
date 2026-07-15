package com.dutchess77.lantern.fx;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.dutchess77.lantern.Lantern;
import com.dutchess77.lantern.LanternConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Temporary end-rod sparkles above freshly placed lights. Server side only. */
@EventBusSubscriber(modid = Lantern.MODID)
public class SparkleManager {

    private static final List<Sparkle> SPARKLES = new ArrayList<>();

    private static final class Sparkle {
        final ServerLevel level;
        final BlockPos pos;
        final long expiry;

        Sparkle(ServerLevel level, BlockPos pos, long expiry) {
            this.level = level;
            this.pos = pos;
            this.expiry = expiry;
        }
    }

    public static void add(Level level, BlockPos pos) {
        if (level instanceof ServerLevel serverLevel && LanternConfig.sparkleSeconds > 0) {
            SPARKLES.add(new Sparkle(serverLevel, pos,
                level.getGameTime() + LanternConfig.sparkleSeconds * 20L));
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel().isClientSide || SPARKLES.isEmpty()
            || event.getLevel().getGameTime() % 4 != 0) {
            return;
        }
        Iterator<Sparkle> it = SPARKLES.iterator();
        while (it.hasNext()) {
            Sparkle sparkle = it.next();
            if (sparkle.level != event.getLevel()) {
                continue;
            }
            if (event.getLevel().getGameTime() >= sparkle.expiry) {
                it.remove();
                continue;
            }
            sparkle.level.sendParticles(ParticleTypes.END_ROD,
                sparkle.pos.getX() + 0.5D, sparkle.pos.getY() + 0.4D, sparkle.pos.getZ() + 0.5D,
                2, 0.25D, 0.15D, 0.25D, 0.005D);
        }
    }
}

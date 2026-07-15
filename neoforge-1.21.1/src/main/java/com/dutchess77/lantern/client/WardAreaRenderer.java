package com.dutchess77.lantern.client;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.dutchess77.lantern.Lantern;
import com.dutchess77.lantern.block.DarknessWardTileEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

/**
 * Draws a purple outline box over each ward whose "show area" toggle is on
 * (EnderIO range-display style). Pure client state - nothing is synced.
 */
@EventBusSubscriber(modid = Lantern.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class WardAreaRenderer {

    private static final Set<GlobalPos> SHOWN = new HashSet<>();

    private static final float RED = 0.56F;
    private static final float GREEN = 0.25F;
    private static final float BLUE = 0.85F;
    private static final float ALPHA = 0.9F;

    private WardAreaRenderer() {
    }

    public static boolean isShown(Level level, BlockPos pos) {
        return SHOWN.contains(GlobalPos.of(level.dimension(), pos.immutable()));
    }

    public static void toggle(Level level, BlockPos pos) {
        GlobalPos key = GlobalPos.of(level.dimension(), pos.immutable());
        if (!SHOWN.remove(key)) {
            SHOWN.add(key);
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            SHOWN.clear();
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || SHOWN.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }

        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(-cam.x, -cam.y, -cam.z);

        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());

        // entries are always from the current level: SHOWN clears on level unload
        Iterator<GlobalPos> it = SHOWN.iterator();
        while (it.hasNext()) {
            GlobalPos key = it.next();
            if (!key.dimension().equals(level.dimension())) {
                continue;
            }
            BlockPos pos = key.pos();
            if (!(level.getBlockEntity(pos) instanceof DarknessWardTileEntity ward)) {
                it.remove(); // ward gone (broken or unloaded) - stop drawing it
                continue;
            }
            double minX = pos.getX() + ward.getOffset(0) - ward.getRadius(0);
            double minY = pos.getY() + ward.getOffset(1) - ward.getRadius(1);
            double minZ = pos.getZ() + ward.getOffset(2) - ward.getRadius(2);
            double maxX = pos.getX() + ward.getOffset(0) + ward.getRadius(0) + 1;
            double maxY = pos.getY() + ward.getOffset(1) + ward.getRadius(1) + 1;
            double maxZ = pos.getZ() + ward.getOffset(2) + ward.getRadius(2) + 1;
            LevelRenderer.renderLineBox(poseStack, lines, minX, minY, minZ, maxX, maxY, maxZ,
                RED, GREEN, BLUE, ALPHA);
        }

        buffers.endBatch(RenderType.lines());
        poseStack.popPose();
    }
}

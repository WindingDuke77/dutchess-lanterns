package com.dutchess77.lantern.network;

import com.dutchess77.lantern.Lantern;
import com.dutchess77.lantern.block.DarknessWardTileEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = Lantern.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class LanternNetwork {

    private LanternNetwork() {
    }

    @SubscribeEvent
    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(WardAdjustMessage.TYPE, WardAdjustMessage.STREAM_CODEC,
            LanternNetwork::handleWardAdjust);
    }

    /** Server side: same validation as the 1.12.2 handler - reach check, then apply. */
    private static void handleWardAdjust(WardAdjustMessage message, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            BlockPos pos = message.pos();
            if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64.0D) {
                return;
            }
            if (player.level().getBlockEntity(pos) instanceof DarknessWardTileEntity ward) {
                ward.adjust(message.field(), message.delta());
            }
        });
    }
}

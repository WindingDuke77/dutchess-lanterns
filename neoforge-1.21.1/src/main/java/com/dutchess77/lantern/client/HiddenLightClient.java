package com.dutchess77.lantern.client;

import com.dutchess77.lantern.Lantern;
import com.dutchess77.lantern.ModBlocks;
import com.dutchess77.lantern.block.HiddenLightBlock;
import com.dutchess77.lantern.block.HiddenLightTileEntity;

import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/**
 * Client wiring for the hidden light: wraps its baked model in the
 * mimic-rendering model and delegates tint lookups to the mimicked block
 * (replaces the 1.12.2 ModBlocks.ClientBake).
 */
@EventBusSubscriber(modid = Lantern.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class HiddenLightClient {

    private HiddenLightClient() {
    }

    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        ModelResourceLocation location = new ModelResourceLocation(
            ResourceLocation.fromNamespaceAndPath(Lantern.MODID, "hidden_light"), "");
        BakedModel base = event.getModels().get(location);
        if (base != null) {
            event.getModels().put(location, new HiddenLightBakedModel(base));
        }
    }

    /** Delegate tint lookups (grass green, foliage, water...) to the mimicked block. */
    @SubscribeEvent
    public static void onBlockColors(RegisterColorHandlersEvent.Block event) {
        final BlockColors colors = event.getBlockColors();
        event.register((state, level, pos, tintIndex) -> {
            if (level != null && pos != null) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof HiddenLightTileEntity hidden) {
                    BlockState mimic = hidden.getMimic();
                    if (mimic != null && !(mimic.getBlock() instanceof HiddenLightBlock)) {
                        try {
                            return colors.getColor(mimic, level, pos, tintIndex);
                        } catch (Throwable t) {
                            return -1;
                        }
                    }
                }
            }
            return -1;
        }, ModBlocks.HIDDEN_LIGHT.get());
    }
}

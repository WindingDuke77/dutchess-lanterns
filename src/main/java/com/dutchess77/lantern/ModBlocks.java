package com.dutchess77.lantern;

import com.dutchess77.lantern.block.HiddenLightBlock;
import com.dutchess77.lantern.block.HiddenLightTileEntity;
import com.dutchess77.lantern.client.HiddenLightBakedModel;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = Lantern.MODID)
public class ModBlocks {

    public static final HiddenLightBlock HIDDEN_LIGHT = new HiddenLightBlock();

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        event.getRegistry().register(HIDDEN_LIGHT);
        GameRegistry.registerTileEntity(HiddenLightTileEntity.class, Lantern.MODID + ":hidden_light");
    }

    @Mod.EventBusSubscriber(value = Side.CLIENT, modid = Lantern.MODID)
    public static class ClientBake {
        @SubscribeEvent
        public static void onModelBake(ModelBakeEvent event) {
            ModelResourceLocation location =
                new ModelResourceLocation(HIDDEN_LIGHT.getRegistryName(), "normal");
            IBakedModel base = event.getModelRegistry().getObject(location);
            if (base != null) {
                event.getModelRegistry().putObject(location, new HiddenLightBakedModel(base));
            }
        }
    }
}

package com.dutchess77.lantern;

import com.dutchess77.lantern.item.LanternItem;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = Lantern.MODID)
public class ModItems {

    public static final LanternItem LANTERN = new LanternItem();

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().register(LANTERN);
    }

    @Mod.EventBusSubscriber(value = Side.CLIENT, modid = Lantern.MODID)
    public static class ClientModels {
        @SubscribeEvent
        public static void registerModels(ModelRegistryEvent event) {
            ModelLoader.setCustomModelResourceLocation(LANTERN, 0,
                new ModelResourceLocation(LANTERN.getRegistryName(), "inventory"));
        }
    }
}

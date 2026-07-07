package com.dutchess77.lantern;

import com.dutchess77.lantern.item.CreativeLanternItem;
import com.dutchess77.lantern.item.EnergyLanternItem;
import com.dutchess77.lantern.item.LanternItem;
import com.dutchess77.lantern.item.TorchLanternItem;

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
    public static final EnergyLanternItem ENERGY_LANTERN = new EnergyLanternItem();
    public static final TorchLanternItem TORCH_LANTERN = new TorchLanternItem();
    public static final CreativeLanternItem CREATIVE_LANTERN = new CreativeLanternItem();
    public static final Item GLOW_CAPACITOR = new Item()
        .setRegistryName(Lantern.MODID, "glow_capacitor")
        .setTranslationKey(Lantern.MODID + ".glow_capacitor")
        .setCreativeTab(net.minecraft.creativetab.CreativeTabs.MATERIALS);

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().register(LANTERN);
        event.getRegistry().register(ENERGY_LANTERN);
        event.getRegistry().register(TORCH_LANTERN);
        event.getRegistry().register(CREATIVE_LANTERN);
        event.getRegistry().register(GLOW_CAPACITOR);
    }

    @Mod.EventBusSubscriber(value = Side.CLIENT, modid = Lantern.MODID)
    public static class ClientModels {
        @SubscribeEvent
        public static void registerModels(ModelRegistryEvent event) {
            ModelLoader.setCustomModelResourceLocation(LANTERN, 0,
                new ModelResourceLocation(LANTERN.getRegistryName(), "inventory"));
            ModelLoader.setCustomModelResourceLocation(ENERGY_LANTERN, 0,
                new ModelResourceLocation(ENERGY_LANTERN.getRegistryName(), "inventory"));
            ModelLoader.setCustomModelResourceLocation(TORCH_LANTERN, 0,
                new ModelResourceLocation(TORCH_LANTERN.getRegistryName(), "inventory"));
            ModelLoader.setCustomModelResourceLocation(CREATIVE_LANTERN, 0,
                new ModelResourceLocation(CREATIVE_LANTERN.getRegistryName(), "inventory"));
            ModelLoader.setCustomModelResourceLocation(GLOW_CAPACITOR, 0,
                new ModelResourceLocation(GLOW_CAPACITOR.getRegistryName(), "inventory"));
        }
    }
}

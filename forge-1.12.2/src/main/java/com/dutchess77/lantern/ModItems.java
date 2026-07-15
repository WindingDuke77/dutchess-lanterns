package com.dutchess77.lantern;

import com.dutchess77.lantern.item.CreativeGlowWandItem;
import com.dutchess77.lantern.item.CreativeLanternItem;
import com.dutchess77.lantern.item.DevGlowWandItem;
import com.dutchess77.lantern.item.DevLanternItem;
import com.dutchess77.lantern.item.EnergyGlowWandItem;
import com.dutchess77.lantern.item.EnergyLanternItem;
import com.dutchess77.lantern.item.GlowWandItem;
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
    public static final GlowWandItem GLOW_WAND = new GlowWandItem();
    public static final EnergyGlowWandItem ENERGY_GLOW_WAND = new EnergyGlowWandItem();
    public static final CreativeGlowWandItem CREATIVE_GLOW_WAND = new CreativeGlowWandItem();
    public static final DevLanternItem DEV_LANTERN = new DevLanternItem();
    public static final DevGlowWandItem DEV_GLOW_WAND = new DevGlowWandItem();
    public static final Item GLOW_CAPACITOR = new Item()
        .setRegistryName(Lantern.MODID, "glow_capacitor")
        .setTranslationKey(Lantern.MODID + ".glow_capacitor")
        .setCreativeTab(net.minecraft.creativetab.CreativeTabs.MATERIALS);
    public static final com.dutchess77.lantern.item.UpgradeItem RANGE_UPGRADE =
        new com.dutchess77.lantern.item.UpgradeItem(com.dutchess77.lantern.item.UpgradeType.RANGE);
    public static final com.dutchess77.lantern.item.UpgradeItem EFFICIENCY_UPGRADE =
        new com.dutchess77.lantern.item.UpgradeItem(com.dutchess77.lantern.item.UpgradeType.EFFICIENCY);
    public static final com.dutchess77.lantern.item.UpgradeItem CAPACITY_UPGRADE =
        new com.dutchess77.lantern.item.UpgradeItem(com.dutchess77.lantern.item.UpgradeType.CAPACITY);

    public static com.dutchess77.lantern.item.UpgradeItem upgradeFor(com.dutchess77.lantern.item.UpgradeType type) {
        switch (type) {
            case RANGE: return RANGE_UPGRADE;
            case EFFICIENCY: return EFFICIENCY_UPGRADE;
            case CAPACITY: return CAPACITY_UPGRADE;
            default: return null;
        }
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().register(LANTERN);
        event.getRegistry().register(ENERGY_LANTERN);
        event.getRegistry().register(TORCH_LANTERN);
        event.getRegistry().register(CREATIVE_LANTERN);
        event.getRegistry().register(GLOW_WAND);
        event.getRegistry().register(ENERGY_GLOW_WAND);
        event.getRegistry().register(CREATIVE_GLOW_WAND);
        event.getRegistry().register(DEV_LANTERN);
        event.getRegistry().register(DEV_GLOW_WAND);
        event.getRegistry().register(GLOW_CAPACITOR);
        event.getRegistry().register(RANGE_UPGRADE);
        event.getRegistry().register(EFFICIENCY_UPGRADE);
        event.getRegistry().register(CAPACITY_UPGRADE);
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
            ModelLoader.setCustomModelResourceLocation(GLOW_WAND, 0,
                new ModelResourceLocation(GLOW_WAND.getRegistryName(), "inventory"));
            ModelLoader.setCustomModelResourceLocation(ENERGY_GLOW_WAND, 0,
                new ModelResourceLocation(ENERGY_GLOW_WAND.getRegistryName(), "inventory"));
            ModelLoader.setCustomModelResourceLocation(CREATIVE_GLOW_WAND, 0,
                new ModelResourceLocation(CREATIVE_GLOW_WAND.getRegistryName(), "inventory"));
            ModelLoader.setCustomModelResourceLocation(DEV_LANTERN, 0,
                new ModelResourceLocation(DEV_LANTERN.getRegistryName(), "inventory"));
            ModelLoader.setCustomModelResourceLocation(DEV_GLOW_WAND, 0,
                new ModelResourceLocation(DEV_GLOW_WAND.getRegistryName(), "inventory"));
            ModelLoader.setCustomModelResourceLocation(GLOW_CAPACITOR, 0,
                new ModelResourceLocation(GLOW_CAPACITOR.getRegistryName(), "inventory"));
            for (Item upgrade : new Item[] { RANGE_UPGRADE, EFFICIENCY_UPGRADE, CAPACITY_UPGRADE }) {
                for (int meta = 0; meta < com.dutchess77.lantern.item.UpgradeItem.MAX_TIER; meta++) {
                    // one model per tier: _t1.._t4 with tier-colored frame and pips
                    ModelLoader.setCustomModelResourceLocation(upgrade, meta,
                        new ModelResourceLocation(upgrade.getRegistryName() + "_t" + (meta + 1), "inventory"));
                }
            }
        }
    }
}

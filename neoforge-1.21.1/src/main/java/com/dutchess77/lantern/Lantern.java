package com.dutchess77.lantern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(Lantern.MODID)
public class Lantern {

    public static final String MODID = "lantern";
    public static final String NAME = "Dutchess Lanterns";
    public static final String VERSION = "4.0.0";

    public static final Logger LOGGER = LogManager.getLogger(NAME);

    public Lantern(IEventBus modBus, ModContainer container) {
        ModBlocks.BLOCKS.register(modBus);
        ModBlocks.BLOCK_ENTITIES.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModItems.TABS.register(modBus);
        ModMenus.MENUS.register(modBus);
        LanternDataComponents.COMPONENTS.register(modBus);

        container.registerConfig(ModConfig.Type.COMMON, LanternConfig.SPEC);
        modBus.addListener(LanternConfig::onLoad);

        LOGGER.info("{} {} loading", NAME, VERSION);
    }
}

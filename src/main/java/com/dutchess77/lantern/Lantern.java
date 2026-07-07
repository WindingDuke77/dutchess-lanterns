package com.dutchess77.lantern;

import org.apache.logging.log4j.Logger;

import com.dutchess77.lantern.command.LanternCommand;
import com.dutchess77.lantern.compat.EnderIOPaintHelper;
import com.dutchess77.lantern.compat.XaeroMapCompat;
import com.dutchess77.lantern.fx.SparkleManager;
import com.dutchess77.lantern.handler.LanternTickHandler;
import com.dutchess77.lantern.handler.LegacyMigrationHandler;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLLoadCompleteEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

@Mod(
    modid = Lantern.MODID,
    name = Lantern.NAME,
    version = Lantern.VERSION,
    acceptedMinecraftVersions = "[1.12.2]",
    dependencies = "required-after:forge@[14.23.5.2800,);after:enderio;after:baubles"
)
public class Lantern {

    public static final String MODID = "lantern";
    public static final String NAME = "Dutchess Lanterns";
    public static final String VERSION = "2.2.0";

    public static Logger LOGGER;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER = event.getModLog();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        EnderIOPaintHelper.init();
        MinecraftForge.EVENT_BUS.register(new LanternTickHandler());
        MinecraftForge.EVENT_BUS.register(new SparkleManager());
        MinecraftForge.EVENT_BUS.register(new LegacyMigrationHandler());
    }

    @Mod.EventHandler
    public void loadComplete(FMLLoadCompleteEvent event) {
        XaeroMapCompat.init();
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new LanternCommand());
    }
}

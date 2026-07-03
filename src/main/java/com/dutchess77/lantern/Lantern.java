package com.dutchess77.lantern;

import org.apache.logging.log4j.Logger;

import com.dutchess77.lantern.command.LanternCommand;
import com.dutchess77.lantern.compat.EnderIOPaintHelper;
import com.dutchess77.lantern.handler.LanternTickHandler;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

@Mod(
    modid = Lantern.MODID,
    name = Lantern.NAME,
    version = Lantern.VERSION,
    acceptedMinecraftVersions = "[1.12.2]",
    dependencies = "required-after:forge@[14.23.5.2800,);required-after:enderio"
)
public class Lantern {

    public static final String MODID = "lantern";
    public static final String NAME = "Lantern";
    public static final String VERSION = "1.8.0";

    public static Logger LOGGER;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER = event.getModLog();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        EnderIOPaintHelper.init();
        MinecraftForge.EVENT_BUS.register(new LanternTickHandler());
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new LanternCommand());
    }
}

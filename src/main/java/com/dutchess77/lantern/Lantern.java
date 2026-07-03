package com.dutchess77.lantern;

import org.apache.logging.log4j.Logger;

import com.dutchess77.lantern.compat.EnderIOPaintHelper;
import com.dutchess77.lantern.handler.LanternTickHandler;
import com.dutchess77.lantern.network.PacketAdjustSpacing;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

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
    public static final String VERSION = "1.4.0";

    public static Logger LOGGER;
    public static SimpleNetworkWrapper NETWORK;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER = event.getModLog();
        NETWORK = NetworkRegistry.INSTANCE.newSimpleChannel(MODID);
        NETWORK.registerMessage(PacketAdjustSpacing.Handler.class, PacketAdjustSpacing.class, 0, Side.SERVER);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        EnderIOPaintHelper.init();
        MinecraftForge.EVENT_BUS.register(new LanternTickHandler());
    }
}

package com.dutchess77.lantern;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Config(modid = Lantern.MODID)
public class LanternConfig {

    @Config.Comment("Distance in blocks between points of the global lighting grid (world-aligned)")
    @Config.RangeInt(min = 2, max = 16)
    public static int gridSpacing = 6;

    @Config.Comment("How many Glowstone blocks the Lantern's internal buffer can hold")
    @Config.RangeInt(min = 1, max = 4096)
    public static int bufferCapacity = 512;

    @Config.Comment("Only place lights where block light is at or below this level (7 = mob spawn threshold)")
    @Config.RangeInt(min = 0, max = 14)
    public static int lightThreshold = 7;

    @Config.Comment("Horizontal radius of the area processed around the player (8 = 16x16)")
    @Config.RangeInt(min = 1, max = 16)
    public static int horizontalRadius = 8;

    @Config.Comment("Vertical range scanned above/below the player for torches and ground")
    @Config.RangeInt(min = 1, max = 16)
    public static int verticalRange = 8;

    @Config.Comment("How often (in ticks) an active Lantern processes the area")
    @Config.RangeInt(min = 1, max = 100)
    public static int tickInterval = 5;

    @Config.Comment("How long (in seconds) placed lights sparkle")
    @Config.RangeInt(min = 0, max = 60)
    public static int sparkleSeconds = 6;

    @Config.Comment("Forge Energy capacity of the Energy Lantern")
    @Config.RangeInt(min = 1000, max = 100000000)
    public static int energyCapacity = 200000;

    @Config.Comment("Forge Energy cost per placed light for the Energy Lantern")
    @Config.RangeInt(min = 0, max = 1000000)
    public static int energyPerLight = 2000;

    @Config.Comment("Registry names of torch blocks the Lantern sweeps up")
    public static String[] torchWhitelist = {"minecraft:torch"};

    @Config.Comment("Registry names of blocks the Lantern must never replace")
    public static String[] groundBlacklist = {};

    @Mod.EventBusSubscriber(modid = Lantern.MODID)
    public static class SyncHandler {
        @SubscribeEvent
        public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
            if (Lantern.MODID.equals(event.getModID())) {
                ConfigManager.sync(Lantern.MODID, Config.Type.INSTANCE);
            }
        }
    }
}

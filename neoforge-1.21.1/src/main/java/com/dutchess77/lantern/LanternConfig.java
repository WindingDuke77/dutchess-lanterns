package com.dutchess77.lantern;

import java.util.List;

import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Config is defined as a TOML ModConfigSpec but baked into plain static fields on
 * load/reload, so gameplay code reads {@code LanternConfig.gridSpacing} exactly like
 * the 1.12.2 version did.
 */
public final class LanternConfig {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.IntValue GRID_SPACING = BUILDER
        .comment("Distance in blocks between points of the global lighting grid (world-aligned)")
        .defineInRange("gridSpacing", 6, 2, 16);
    private static final ModConfigSpec.IntValue BUFFER_CAPACITY = BUILDER
        .comment("How many Glowstone blocks the Lantern's internal buffer can hold")
        .defineInRange("bufferCapacity", 512, 1, 4096);
    private static final ModConfigSpec.IntValue LIGHT_THRESHOLD = BUILDER
        .comment("Only place lights where block light is at or below this level (7 = mob spawn threshold)")
        .defineInRange("lightThreshold", 7, 0, 14);
    private static final ModConfigSpec.IntValue LIGHT_EMISSION = BUILDER
        .comment("How much light hidden lights emit (15 = glowstone).",
            "Lower values look more natural on disguised floors but need a tighter grid to stay spawn-proof.",
            "A value of N keeps light >= 8 up to N-8 blocks away, so pair it with gridSpacing <= 2*(N-8).")
        .defineInRange("lightEmission", 15, 9, 15);
    private static final ModConfigSpec.IntValue HORIZONTAL_RADIUS = BUILDER
        .comment("Horizontal radius of the area processed around the player (16 = 32x32)")
        .defineInRange("horizontalRadius", 16, 1, 32);
    private static final ModConfigSpec.IntValue VERTICAL_RANGE = BUILDER
        .comment("Vertical range scanned above/below the player for torches and ground")
        .defineInRange("verticalRange", 8, 1, 16);
    private static final ModConfigSpec.IntValue TICK_INTERVAL = BUILDER
        .comment("How often (in ticks) an active Lantern processes the area")
        .defineInRange("tickInterval", 5, 1, 100);
    private static final ModConfigSpec.BooleanValue FILL_GAPS = BUILDER
        .comment("After the grid pass, also light dark spots the grid missed (walls, unplaceable ground)")
        .define("fillGaps", true);
    private static final ModConfigSpec.BooleanValue FREE_IN_CREATIVE = BUILDER
        .comment("Creative-mode players place lights for free (the Creative Lantern is always free)")
        .define("freeInCreative", false);
    private static final ModConfigSpec.BooleanValue LIGHT_UNDERWATER = BUILDER
        .comment("Also light the ground under water (ocean/lake floors)")
        .define("lightUnderwater", true);
    private static final ModConfigSpec.BooleanValue REFILL_FROM_CONTAINERS = BUILDER
        .comment("When the buffer and inventory run dry, pull fuel out of carried containers (backpacks etc.)")
        .define("refillFromContainers", true);
    private static final ModConfigSpec.ConfigValue<List<? extends String>> DIMENSION_BLACKLIST = BUILDER
        .comment("Dimension ids where the Lantern never acts (e.g. \"minecraft:the_nether\")")
        .defineListAllowEmpty("dimensionBlacklist", List.of(), () -> "", o -> o instanceof String);
    private static final ModConfigSpec.IntValue SPARKLE_SECONDS = BUILDER
        .comment("How long (in seconds) placed lights sparkle")
        .defineInRange("sparkleSeconds", 6, 0, 60);
    private static final ModConfigSpec.IntValue ENERGY_CAPACITY = BUILDER
        .comment("Forge Energy capacity of the Energy Lantern")
        .defineInRange("energyCapacity", 1000000, 1000, 100000000);
    private static final ModConfigSpec.IntValue ENERGY_PER_LIGHT = BUILDER
        .comment("Forge Energy cost per placed light for the Energy Lantern")
        .defineInRange("energyPerLight", 2000, 0, 1000000);
    private static final ModConfigSpec.IntValue WARD_RADIUS = BUILDER
        .comment("Base per-axis size cap (radius) of a Darkness Ward's area; Range upgrades add +4 per tier.",
            "New wards start at radius 4 (9x9x9). The Glow Wand ignores wards - manual placements always work.")
        .defineInRange("wardRadius", 16, 1, 64);
    private static final ModConfigSpec.ConfigValue<List<? extends String>> TORCH_WHITELIST = BUILDER
        .comment("Registry names of torch blocks the Lantern sweeps up")
        .defineListAllowEmpty("torchWhitelist", List.of("minecraft:torch"), () -> "", o -> o instanceof String);
    private static final ModConfigSpec.ConfigValue<List<? extends String>> GROUND_BLACKLIST = BUILDER
        .comment("Registry names of blocks the Lantern must never replace")
        .defineListAllowEmpty("groundBlacklist", List.of("minecraft:obsidian"), () -> "", o -> o instanceof String);

    public static final ModConfigSpec SPEC = BUILDER.build();

    // baked values - same names and types the 1.12.2 code used
    public static int gridSpacing = 6;
    public static int bufferCapacity = 512;
    public static int lightThreshold = 7;
    public static int lightEmission = 15;
    public static int horizontalRadius = 16;
    public static int verticalRange = 8;
    public static int tickInterval = 5;
    public static boolean fillGaps = true;
    public static boolean freeInCreative = false;
    public static boolean lightUnderwater = true;
    public static boolean refillFromContainers = true;
    public static String[] dimensionBlacklist = {};
    public static int sparkleSeconds = 6;
    public static int energyCapacity = 1000000;
    public static int energyPerLight = 2000;
    public static int wardRadius = 16;
    public static String[] torchWhitelist = {"minecraft:torch"};
    public static String[] groundBlacklist = {"minecraft:obsidian"};

    private LanternConfig() {
    }

    public static void onLoad(ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC) {
            return;
        }
        gridSpacing = GRID_SPACING.get();
        bufferCapacity = BUFFER_CAPACITY.get();
        lightThreshold = LIGHT_THRESHOLD.get();
        lightEmission = LIGHT_EMISSION.get();
        horizontalRadius = HORIZONTAL_RADIUS.get();
        verticalRange = VERTICAL_RANGE.get();
        tickInterval = TICK_INTERVAL.get();
        fillGaps = FILL_GAPS.get();
        freeInCreative = FREE_IN_CREATIVE.get();
        lightUnderwater = LIGHT_UNDERWATER.get();
        refillFromContainers = REFILL_FROM_CONTAINERS.get();
        dimensionBlacklist = DIMENSION_BLACKLIST.get().toArray(new String[0]);
        sparkleSeconds = SPARKLE_SECONDS.get();
        energyCapacity = ENERGY_CAPACITY.get();
        energyPerLight = ENERGY_PER_LIGHT.get();
        wardRadius = WARD_RADIUS.get();
        torchWhitelist = TORCH_WHITELIST.get().toArray(new String[0]);
        groundBlacklist = GROUND_BLACKLIST.get().toArray(new String[0]);
    }
}

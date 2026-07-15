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
import com.dutchess77.lantern.item.UpgradeItem;
import com.dutchess77.lantern.item.UpgradeType;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Lantern.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Lantern.MODID);

    public static final DeferredItem<LanternItem> LANTERN = ITEMS.register("lantern", LanternItem::new);
    public static final DeferredItem<EnergyLanternItem> ENERGY_LANTERN =
        ITEMS.register("energy_lantern", EnergyLanternItem::new);
    public static final DeferredItem<TorchLanternItem> TORCH_LANTERN =
        ITEMS.register("torch_lantern", TorchLanternItem::new);
    public static final DeferredItem<CreativeLanternItem> CREATIVE_LANTERN =
        ITEMS.register("creative_lantern", CreativeLanternItem::new);
    public static final DeferredItem<GlowWandItem> GLOW_WAND = ITEMS.register("glow_wand", GlowWandItem::new);
    public static final DeferredItem<EnergyGlowWandItem> ENERGY_GLOW_WAND =
        ITEMS.register("energy_glow_wand", EnergyGlowWandItem::new);
    public static final DeferredItem<CreativeGlowWandItem> CREATIVE_GLOW_WAND =
        ITEMS.register("creative_glow_wand", CreativeGlowWandItem::new);
    public static final DeferredItem<DevLanternItem> DEV_LANTERN = ITEMS.register("dev_lantern", DevLanternItem::new);
    public static final DeferredItem<DevGlowWandItem> DEV_GLOW_WAND =
        ITEMS.register("dev_glow_wand", DevGlowWandItem::new);
    public static final DeferredItem<Item> GLOW_CAPACITOR =
        ITEMS.register("glow_capacitor", () -> new Item(new Item.Properties()));

    // 1.12.2 used one item per type with meta 0-3 for tiers; 1.21 has no metadata,
    // so each tier is its own item (t1..t4) matching the existing per-tier models.
    @SuppressWarnings("unchecked")
    private static final DeferredItem<UpgradeItem>[][] UPGRADES = new DeferredItem[UpgradeType.values().length][4];

    static {
        for (UpgradeType type : UpgradeType.values()) {
            for (int tier = 1; tier <= 4; tier++) {
                final UpgradeType t = type;
                final int ti = tier;
                UPGRADES[type.ordinal()][tier - 1] =
                    ITEMS.register(type.key + "_t" + tier, () -> new UpgradeItem(t, ti));
            }
        }
    }

    public static UpgradeItem upgradeFor(UpgradeType type, int tier) {
        return UPGRADES[type.ordinal()][Math.max(1, Math.min(4, tier)) - 1].get();
    }

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("lantern",
        () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.lantern"))
            .icon(() -> LANTERN.get().getDefaultInstance())
            .displayItems((params, output) -> {
                output.accept(LANTERN.get());
                output.accept(ENERGY_LANTERN.get());
                output.accept(TORCH_LANTERN.get());
                output.accept(CREATIVE_LANTERN.get());
                output.accept(GLOW_WAND.get());
                output.accept(ENERGY_GLOW_WAND.get());
                output.accept(CREATIVE_GLOW_WAND.get());
                output.accept(DEV_LANTERN.get());
                output.accept(DEV_GLOW_WAND.get());
                output.accept(GLOW_CAPACITOR.get());
                for (UpgradeType type : UpgradeType.values()) {
                    for (int tier = 1; tier <= 4; tier++) {
                        output.accept(upgradeFor(type, tier));
                    }
                }
                output.accept(ModBlocks.LANTERN_BENCH_ITEM.get());
                output.accept(ModBlocks.DARKNESS_WARD_ITEM.get());
                output.accept(ModBlocks.HIDDEN_LIGHT_ITEM.get());
            })
            .build());

    private ModItems() {
    }
}

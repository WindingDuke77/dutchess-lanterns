package com.dutchess77.lantern;

import com.dutchess77.lantern.gui.DarknessWardContainer;
import com.dutchess77.lantern.gui.LanternBenchContainer;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {

    public static final DeferredRegister<MenuType<?>> MENUS =
        DeferredRegister.create(Registries.MENU, Lantern.MODID);

    // both menus carry only a BlockPos as extra data; the client-side ctor reads it
    public static final DeferredHolder<MenuType<?>, MenuType<LanternBenchContainer>> LANTERN_BENCH =
        MENUS.register("lantern_bench", () -> IMenuTypeExtension.create(LanternBenchContainer::new));
    public static final DeferredHolder<MenuType<?>, MenuType<DarknessWardContainer>> DARKNESS_WARD =
        MENUS.register("darkness_ward", () -> IMenuTypeExtension.create(DarknessWardContainer::new));

    private ModMenus() {
    }
}

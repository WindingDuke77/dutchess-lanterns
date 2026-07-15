package com.dutchess77.lantern.client;

import com.dutchess77.lantern.Lantern;
import com.dutchess77.lantern.ModMenus;
import com.dutchess77.lantern.gui.DarknessWardGui;
import com.dutchess77.lantern.gui.LanternBenchGui;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** Client-side menu-screen bindings (replaces the 1.12.2 IGuiHandler). */
@EventBusSubscriber(modid = Lantern.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class LanternScreens {

    private LanternScreens() {
    }

    @SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.LANTERN_BENCH.get(), LanternBenchGui::new);
        event.register(ModMenus.DARKNESS_WARD.get(), DarknessWardGui::new);
    }
}

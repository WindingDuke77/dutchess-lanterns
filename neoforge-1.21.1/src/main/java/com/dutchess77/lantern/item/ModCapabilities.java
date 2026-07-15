package com.dutchess77.lantern.item;

import com.dutchess77.lantern.Lantern;
import com.dutchess77.lantern.ModItems;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * 1.21 replacement for the 1.12.2 {@code initCapabilities} overrides: the FE
 * item capability of the Energy Lantern and Energy Glow Wand is registered
 * here, backed by the ENERGY data component.
 */
@EventBusSubscriber(modid = Lantern.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class ModCapabilities {

    private ModCapabilities() {
    }

    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.registerItem(Capabilities.EnergyStorage.ITEM,
            (stack, ctx) -> EnergyLanternItem.newEnergyStorage(stack),
            ModItems.ENERGY_LANTERN.get(), ModItems.ENERGY_GLOW_WAND.get());
    }
}

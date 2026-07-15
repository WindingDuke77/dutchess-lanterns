package com.dutchess77.lantern.handler;

import com.dutchess77.lantern.Lantern;
import com.dutchess77.lantern.LanternConfig;
import com.dutchess77.lantern.compat.CuriosCompat;
import com.dutchess77.lantern.item.DevLanternItem;
import com.dutchess77.lantern.item.LanternItem;
import com.dutchess77.lantern.item.LanternUpgrades;
import com.dutchess77.lantern.item.TorchLanternItem;
import com.dutchess77.lantern.logic.LightPlacer;
import com.dutchess77.lantern.logic.TorchSweeper;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Orchestration only: gates (side, interval, dimension), finds the active
 * lantern anywhere in the player's own inventory (never other blocks'
 * inventories), and hands off to TorchSweeper + LightPlacer.
 */
@EventBusSubscriber(modid = Lantern.MODID)
public class LanternTickHandler {

    private static final LightPlacer placer = new LightPlacer();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        Level level = player.level();
        if (level.isClientSide || player.tickCount % LanternConfig.tickInterval != 0) {
            return;
        }
        String dimension = level.dimension().location().toString();
        for (String blocked : LanternConfig.dimensionBlacklist) {
            if (blocked.equals(dimension)) {
                return;
            }
        }

        ItemStack lantern = findActiveLantern(player);
        if (lantern.isEmpty()) {
            return;
        }

        BlockPos center = player.blockPosition();
        int r = LanternUpgrades.effectiveRadius(lantern, LanternConfig.horizontalRadius);
        int vr = LanternConfig.verticalRange;

        if (lantern.getItem() instanceof TorchLanternItem) {
            // torch mode never sweeps torches - it places them
            placer.processArea(level, player, lantern, center, r, vr, true, false);
            return;
        }

        boolean visible = lantern.getItem() instanceof DevLanternItem;
        TorchSweeper.sweep(level, player, center, r, vr);
        placer.processArea(level, player, lantern, center, r, vr, false, visible);
    }

    /**
     * Hands, then the player's whole inventory, then Curios slots.
     * Deliberately never chests, item conduits, or storage systems -
     * only the player entity's own inventory is consulted.
     */
    private static ItemStack findActiveLantern(Player player) {
        if (isActiveLantern(player.getMainHandItem())) {
            return player.getMainHandItem();
        }
        if (isActiveLantern(player.getOffhandItem())) {
            return player.getOffhandItem();
        }
        for (ItemStack stack : player.getInventory().items) {
            if (isActiveLantern(stack)) {
                return stack;
            }
        }
        for (ItemStack stack : CuriosCompat.equippedLanterns(player)) {
            if (isActiveLantern(stack)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static boolean isActiveLantern(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof LanternItem && LanternItem.isActive(stack);
    }
}

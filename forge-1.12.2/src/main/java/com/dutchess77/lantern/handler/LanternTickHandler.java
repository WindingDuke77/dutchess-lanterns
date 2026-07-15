package com.dutchess77.lantern.handler;

import com.dutchess77.lantern.LanternConfig;
import com.dutchess77.lantern.compat.BaublesCompat;
import com.dutchess77.lantern.item.DevLanternItem;
import com.dutchess77.lantern.item.LanternItem;
import com.dutchess77.lantern.item.TorchLanternItem;
import com.dutchess77.lantern.logic.LightPlacer;
import com.dutchess77.lantern.logic.TorchSweeper;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Orchestration only: gates (side, interval, dimension), finds the active
 * lantern anywhere in the player's own inventory (never other blocks'
 * inventories), and hands off to TorchSweeper + LightPlacer.
 */
public class LanternTickHandler {

    private final LightPlacer placer = new LightPlacer();

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        EntityPlayer player = event.player;
        World world = player.world;
        if (world.isRemote || player.ticksExisted % LanternConfig.tickInterval != 0) {
            return;
        }
        int dimension = world.provider.getDimension();
        for (int blocked : LanternConfig.dimensionBlacklist) {
            if (blocked == dimension) {
                return;
            }
        }

        ItemStack lantern = findActiveLantern(player);
        if (lantern.isEmpty()) {
            return;
        }

        BlockPos center = new BlockPos(player);
        int r = com.dutchess77.lantern.item.LanternUpgrades.effectiveRadius(lantern, LanternConfig.horizontalRadius);
        int vr = LanternConfig.verticalRange;

        if (lantern.getItem() instanceof TorchLanternItem) {
            // torch mode never sweeps torches - it places them
            placer.processArea(world, player, lantern, center, r, vr, true, false);
            return;
        }

        boolean visible = lantern.getItem() instanceof DevLanternItem;
        TorchSweeper.sweep(world, player, center, r, vr);
        placer.processArea(world, player, lantern, center, r, vr, false, visible);
    }

    /**
     * Hands, then the player's whole inventory, then Baubles slots.
     * Deliberately never chests, item conduits, or storage systems -
     * only the player entity's own inventory is consulted.
     */
    private static ItemStack findActiveLantern(EntityPlayer player) {
        if (isActiveLantern(player.getHeldItemMainhand())) {
            return player.getHeldItemMainhand();
        }
        if (isActiveLantern(player.getHeldItemOffhand())) {
            return player.getHeldItemOffhand();
        }
        for (ItemStack stack : player.inventory.mainInventory) {
            if (isActiveLantern(stack)) {
                return stack;
            }
        }
        return BaublesCompat.findActiveLantern(player);
    }

    private static boolean isActiveLantern(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof LanternItem && LanternItem.isActive(stack);
    }
}

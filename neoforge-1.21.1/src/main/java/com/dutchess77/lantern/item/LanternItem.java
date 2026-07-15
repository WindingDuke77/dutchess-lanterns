package com.dutchess77.lantern.item;

import java.util.List;

import com.dutchess77.lantern.LanternConfig;
import com.dutchess77.lantern.LanternDataComponents;
import com.dutchess77.lantern.LanternDataComponents.InstalledUpgrade;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

public class LanternItem extends Item {

    public LanternItem() {
        super(new Item.Properties().stacksTo(1));
    }

    /** Refund reclaimed lights as Glowstone: buffer first when this lantern burns Glowstone, else items. */
    protected void refundReclaimed(Player player, ItemStack lantern, int count) {
        if (fuelItem() == Blocks.GLOWSTONE.asItem()) {
            int capacity = LanternConfig.bufferCapacity;
            int charge = Math.min(getCharge(lantern), capacity);
            int toBuffer = Math.min(count, capacity - charge);
            setCharge(lantern, charge + toBuffer);
            count -= toBuffer;
        }
        while (count > 0) {
            int give = Math.min(64, count);
            ItemHandlerHelper.giveItemToPlayer(player, new ItemStack(Blocks.GLOWSTONE, give));
            count -= give;
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            boolean nowActive = !isActive(stack);
            setActive(stack, nowActive);
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable(
                    nowActive ? "chat.lantern.on" : "chat.lantern.off"), true);
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 0.7F, nowActive ? 1.6F : 0.6F);
            }
            return InteractionResultHolder.success(stack);
        }
        if (!level.isClientSide) {
            fill(player, stack);
        }
        return InteractionResultHolder.success(stack);
    }

    /** The inventory item this lantern burns (Glowstone blocks by default). */
    protected Item fuelItem() {
        return Items.GLOWSTONE;
    }

    /** Whether this lantern's placed lights drop/refund Glowstone (paid ones do, FE/free ones don't). */
    public boolean lightsRefundGlowstone() {
        return true;
    }

    protected String chargeChatKey() {
        return "chat.lantern.charge";
    }

    /** Buffer size including Capacity upgrades. */
    public int capacityOf(ItemStack stack) {
        return LanternUpgrades.effectiveCapacity(stack, LanternConfig.bufferCapacity);
    }

    /** Plain right-click loads fuel items from the inventory into the buffer. */
    protected void fill(Player player, ItemStack stack) {
        int capacity = capacityOf(stack);
        int charge = Math.min(getCharge(stack), capacity);
        int moved = 0;
        Item fuel = fuelItem();
        for (ItemStack slot : player.getInventory().items) {
            if (charge >= capacity) {
                break;
            }
            if (!slot.isEmpty() && slot.is(fuel)) {
                int take = Math.min(slot.getCount(), capacity - charge);
                slot.shrink(take);
                charge += take;
                moved += take;
            }
        }
        // top up from carried containers (backpacks etc.)
        if (LanternConfig.refillFromContainers && charge < capacity) {
            for (ItemStack held : player.getInventory().items) {
                if (charge >= capacity) {
                    break;
                }
                IItemHandler container = containerOf(held);
                if (container == null) {
                    continue;
                }
                for (int i = 0; i < container.getSlots() && charge < capacity; i++) {
                    ItemStack inside = container.getStackInSlot(i);
                    if (!inside.isEmpty() && inside.is(fuel)) {
                        ItemStack taken = container.extractItem(i, capacity - charge, false);
                        charge += taken.getCount();
                        moved += taken.getCount();
                    }
                }
            }
        }
        setCharge(stack, charge);
        player.displayClientMessage(Component.translatable(chargeChatKey(),
            charge, capacity), true);
        if (moved > 0) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 0.6F, 1.0F);
        }
    }

    /** Pays for one placed light: buffer first, then raw fuel items from the inventory. */
    public boolean consumePlacementCost(Player player, ItemStack stack) {
        if (player.getAbilities().instabuild && LanternConfig.freeInCreative) {
            return true;
        }
        if (player.getRandom().nextFloat() < LanternUpgrades.freeChance(stack)) {
            return true; // Efficiency upgrade proc
        }
        int charge = getCharge(stack);
        if (charge > 0) {
            setCharge(stack, charge - 1);
            return true;
        }
        Item fuel = fuelItem();
        for (ItemStack slot : player.getInventory().items) {
            if (!slot.isEmpty() && slot.is(fuel)) {
                slot.shrink(1);
                return true;
            }
        }
        return pullOneFromContainers(player, fuel);
    }

    /** Last resort: pull a single fuel item out of a carried container (backpack etc.). */
    private static boolean pullOneFromContainers(Player player, Item fuel) {
        if (!LanternConfig.refillFromContainers) {
            return false;
        }
        for (ItemStack held : player.getInventory().items) {
            IItemHandler container = containerOf(held);
            if (container == null) {
                continue;
            }
            for (int i = 0; i < container.getSlots(); i++) {
                ItemStack inside = container.getStackInSlot(i);
                if (!inside.isEmpty() && inside.is(fuel)
                    && !container.extractItem(i, 1, false).isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static IItemHandler containerOf(ItemStack stack) {
        if (stack.isEmpty() || stack.getItem() instanceof LanternItem) {
            return null;
        }
        return stack.getCapability(Capabilities.ItemHandler.ITEM);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int capacity = Math.max(1, capacityOf(stack));
        return Math.round(13.0F * Math.min(getCharge(stack), capacity) / capacity);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xFFD860;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return isActive(stack);
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        // fuel data churns every placement pass; only re-equip on a real change
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        boolean active = isActive(stack);
        tooltip.add(Component.translatable(active ? "tooltip.lantern.active" : "tooltip.lantern.inactive")
            .withStyle(active ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));
        tooltip.add(describeFuel(stack));
        tooltip.add(describeCost().withStyle(ChatFormatting.GRAY));
        List<InstalledUpgrade> upgrades = LanternUpgrades.list(stack);
        tooltip.add(Component.translatable("tooltip.lantern.sockets",
            upgrades.size(), LanternUpgrades.socketCount(stack)).withStyle(ChatFormatting.LIGHT_PURPLE));
        for (InstalledUpgrade upgrade : upgrades) {
            tooltip.add(Component.literal("  ").append(Component.translatable(
                "item.lantern." + upgrade.type().key + "_t" + upgrade.tier()))
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        // raw effective stats
        int radius = LanternUpgrades.effectiveRadius(stack, LanternConfig.horizontalRadius);
        MutableComponent radiusLine =
            Component.translatable("tooltip.lantern.stat_range", radius, radius * 2 + 1, radius * 2 + 1);
        if (radius >= 32) {
            radiusLine.append(Component.translatable("tooltip.lantern.stat_max"));
        }
        tooltip.add(radiusLine.withStyle(ChatFormatting.AQUA));
        float freeChance = LanternUpgrades.freeChance(stack);
        if (freeChance > 0.0F) {
            MutableComponent efficiencyLine =
                Component.translatable("tooltip.lantern.stat_efficiency", Math.round(freeChance * 100.0F));
            if (freeChance >= 0.8F) {
                efficiencyLine.append(Component.translatable("tooltip.lantern.stat_max"));
            }
            tooltip.add(efficiencyLine.withStyle(ChatFormatting.AQUA));
        }
        tooltip.add(Component.translatable("tooltip.lantern.stat_interval",
            LanternConfig.tickInterval, LanternConfig.gridSpacing, LanternConfig.gridSpacing)
            .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.lantern.howto1")
            .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable(howtoFillKey())
            .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        if (ModList.get().isLoaded("curios")) {
            tooltip.add(Component.translatable("tooltip.lantern.curios")
                .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
        }
    }

    protected MutableComponent describeFuel(ItemStack stack) {
        return Component.translatable("tooltip.lantern.charge", getCharge(stack), capacityOf(stack))
            .withStyle(ChatFormatting.YELLOW);
    }

    protected MutableComponent describeCost() {
        return Component.translatable("tooltip.lantern.cost");
    }

    protected String howtoFillKey() {
        return "tooltip.lantern.howto2";
    }

    public static boolean isActive(ItemStack stack) {
        return stack.getOrDefault(LanternDataComponents.ACTIVE.get(), Boolean.FALSE);
    }

    public static void setActive(ItemStack stack, boolean active) {
        stack.set(LanternDataComponents.ACTIVE.get(), active);
    }

    /** A stack with no Charge component (/give, creative menu, JEI) counts as full; crafted stacks carry charge=0. */
    public static int getCharge(ItemStack stack) {
        Integer charge = stack.get(LanternDataComponents.CHARGE.get());
        if (charge != null) {
            return charge;
        }
        return stack.getItem() instanceof LanternItem lantern
            ? lantern.capacityOf(stack)
            : LanternConfig.bufferCapacity;
    }

    public static void setCharge(ItemStack stack, int charge) {
        stack.set(LanternDataComponents.CHARGE.get(), Math.max(0, charge));
    }
}

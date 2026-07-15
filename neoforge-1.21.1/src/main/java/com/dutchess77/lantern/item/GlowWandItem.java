package com.dutchess77.lantern.item;

import java.util.List;

import com.dutchess77.lantern.LanternDataComponents.InstalledUpgrade;
import com.dutchess77.lantern.ModBlocks;
import com.dutchess77.lantern.block.HiddenLightTileEntity;
import com.dutchess77.lantern.fx.SparkleManager;
import com.dutchess77.lantern.logic.SurfaceScanner;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Manual, single-block sibling of the Lantern: right-click a block to swap it
 * for a hidden light disguised as that block; sneak + right-click a hidden
 * light to swap it back. Same fuel, sockets and bench as the Lantern, but it
 * never auto-places (it is never Active, so the tick handler ignores it).
 */
public class GlowWandItem extends LanternItem {

    public GlowWandItem() {
        super();
    }

    /** Marks swapped blocks as FE-paid, so reverting them refunds no glowstone. */
    protected boolean paysWithEnergy() {
        return false;
    }

    /** Dev variant: drop the disguise and place bare Glowstone. */
    protected boolean placesVisibleGlowstone() {
        return false;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        ItemStack stack = context.getItemInHand();
        return player.isShiftKeyDown()
            ? revert(player, stack, context.getLevel(), context.getClickedPos())
            : swap(player, stack, context.getLevel(), context.getClickedPos());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(stack);
        }
        // Range upgrades extend targeting past vanilla reach; only then can a
        // block be hit here that useOn did not already handle
        HitResult hit = player.pick(LanternUpgrades.effectiveReach(stack), 0.0F, false);
        if (hit.getType() == HitResult.Type.BLOCK) {
            swap(player, stack, level, ((BlockHitResult) hit).getBlockPos());
            return InteractionResultHolder.success(stack);
        }
        if (!level.isClientSide) {
            fill(player, stack);
        }
        return InteractionResultHolder.success(stack);
    }

    private InteractionResult swap(Player player, ItemStack stack, Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() == ModBlocks.HIDDEN_LIGHT.get()) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("chat.lantern.wand_already"), true);
            }
            return InteractionResult.SUCCESS;
        }
        // same rules as auto-placement: no lights, ores, containers, unbreakables
        if (!SurfaceScanner.canHost(level, pos)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("chat.lantern.wand_invalid"), true);
            }
            return InteractionResult.SUCCESS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!consumePlacementCost(player, stack)) {
            player.displayClientMessage(Component.translatable("chat.lantern.no_fuel"), true);
            return InteractionResult.SUCCESS;
        }
        if (placesVisibleGlowstone()) {
            level.setBlock(pos, Blocks.GLOWSTONE.defaultBlockState(), 3);
        } else {
            level.setBlock(pos, ModBlocks.HIDDEN_LIGHT.get().defaultBlockState(), 3);
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof HiddenLightTileEntity hidden) {
                hidden.setMimic(state, paysWithEnergy());
            }
        }
        SparkleManager.add(level, pos);
        level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
            SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 0.6F, 1.4F);
        return InteractionResult.SUCCESS;
    }

    private InteractionResult revert(Player player, ItemStack stack, Level level, BlockPos pos) {
        if (level.getBlockState(pos).getBlock() != ModBlocks.HIDDEN_LIGHT.get()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            BlockState mimic = null;
            boolean fromEnergy = false;
            if (be instanceof HiddenLightTileEntity hidden) {
                mimic = hidden.getMimic();
                fromEnergy = hidden.isFromEnergy();
            }
            level.setBlock(pos, mimic != null ? mimic : Blocks.STONE.defaultBlockState(), 3);
            if (!fromEnergy) {
                refundReclaimed(player, stack, 1);
            }
            player.displayClientMessage(Component.translatable("chat.lantern.wand_reverted"), true);
            level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 0.6F, 0.7F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected String howtoFillKey() {
        return "tooltip.lantern.wand_fill";
    }

    @Override
    protected MutableComponent describeCost() {
        return Component.translatable("tooltip.lantern.cost_wand");
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
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
        int reach = LanternUpgrades.effectiveReach(stack);
        MutableComponent reachLine = Component.translatable("tooltip.lantern.stat_reach", reach);
        if (reach >= LanternUpgrades.MAX_REACH) {
            reachLine.append(Component.translatable("tooltip.lantern.stat_max"));
        }
        tooltip.add(reachLine.withStyle(ChatFormatting.AQUA));
        float freeChance = LanternUpgrades.freeChance(stack);
        if (freeChance > 0.0F) {
            MutableComponent efficiencyLine =
                Component.translatable("tooltip.lantern.stat_efficiency", Math.round(freeChance * 100.0F));
            if (freeChance >= 0.8F) {
                efficiencyLine.append(Component.translatable("tooltip.lantern.stat_max"));
            }
            tooltip.add(efficiencyLine.withStyle(ChatFormatting.AQUA));
        }
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.lantern.wand_howto1")
            .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable("tooltip.lantern.wand_howto2")
            .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable(howtoFillKey())
            .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}

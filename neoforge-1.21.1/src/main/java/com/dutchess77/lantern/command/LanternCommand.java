package com.dutchess77.lantern.command;

import java.util.ArrayList;
import java.util.List;

import com.dutchess77.lantern.Lantern;
import com.dutchess77.lantern.LanternConfig;
import com.dutchess77.lantern.ModBlocks;
import com.dutchess77.lantern.block.HiddenLightTileEntity;
import com.dutchess77.lantern.logic.SurfaceScanner;
import com.mojang.brigadier.arguments.IntegerArgumentType;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Debug commands: /lantern help | status | why [x y z] | scan [radius] | undo [radius] */
@EventBusSubscriber(modid = Lantern.MODID)
public final class LanternCommand {

    private LanternCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("lantern")
            .then(Commands.literal("help")
                .executes(ctx -> help(ctx.getSource())))
            .then(Commands.literal("status")
                .requires(src -> src.hasPermission(2))
                .executes(ctx -> status(ctx.getSource())))
            .then(Commands.literal("why")
                .requires(src -> src.hasPermission(2))
                .executes(ctx -> why(ctx.getSource(),
                    BlockPos.containing(ctx.getSource().getPosition()).below()))
                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                    .executes(ctx -> why(ctx.getSource(), BlockPosArgument.getLoadedBlockPos(ctx, "pos")))))
            .then(Commands.literal("scan")
                .requires(src -> src.hasPermission(2))
                .executes(ctx -> scan(ctx.getSource(), LanternConfig.horizontalRadius))
                .then(Commands.argument("radius", IntegerArgumentType.integer(1, 64))
                    .executes(ctx -> scan(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "radius")))))
            .then(Commands.literal("undo")
                .requires(src -> src.hasPermission(2))
                .executes(ctx -> undo(ctx.getSource(), LanternConfig.horizontalRadius))
                .then(Commands.argument("radius", IntegerArgumentType.integer(1, 64))
                    .executes(ctx -> undo(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "radius"))))));
    }

    private static void say(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal(message), false);
    }

    private static int help(CommandSourceStack source) {
        say(source, "=== Dutchess Lanterns " + Lantern.VERSION + " ===");
        say(source, "Sneak + Right-Click (air): toggle on/off (glints while on)");
        say(source, "Right-Click: load fuel from inventory + carried containers");
        say(source, "Glow Wand: Right-Click swaps a block for a glowing copy, Sneak swaps it back");
        say(source, "Works held or on the hotbar");
        say(source, "While on: sweeps torches back to you, buries invisible painted glowstone");
        say(source, "on a global 6-block grid, gap-fills walls/awkward spots, lights underwater");
        say(source, "Variants: Energy (FE, nether star), Torch (places torches), Creative (free, visible)");
        say(source, "Debug: /lantern status | why | scan | undo -- config in config/lantern-common.toml");
        return 1;
    }

    private static int status(CommandSourceStack source) {
        say(source, "Lantern " + Lantern.VERSION + " debug status");
        say(source, " light block: " + ModBlocks.HIDDEN_LIGHT.getId());
        say(source, " gridSpacing=" + LanternConfig.gridSpacing
            + " radius=" + LanternConfig.horizontalRadius
            + " vertical=" + LanternConfig.verticalRange
            + " interval=" + LanternConfig.tickInterval + "t");
        say(source, " lightThreshold=" + LanternConfig.lightThreshold
            + " fillGaps=" + LanternConfig.fillGaps
            + " sparkle=" + LanternConfig.sparkleSeconds + "s");
        say(source, " buffer=" + LanternConfig.bufferCapacity
            + " energy=" + LanternConfig.energyCapacity + "FE @" + LanternConfig.energyPerLight + "FE/light");
        if (source.getEntity() instanceof Player player) {
            ItemStack held = player.getMainHandItem();
            if (held.getItem() instanceof com.dutchess77.lantern.item.EnergyLanternItem) {
                say(source, " held Energy Lantern (server truth): "
                    + com.dutchess77.lantern.item.EnergyLanternItem.getEnergy(held) + "/"
                    + com.dutchess77.lantern.item.EnergyLanternItem.energyCapacityOf(held) + " FE");
            } else if (held.getItem() instanceof com.dutchess77.lantern.item.LanternItem lanternItem) {
                say(source, " held lantern (server truth): "
                    + com.dutchess77.lantern.item.LanternItem.getCharge(held) + "/"
                    + lanternItem.capacityOf(held) + " glowstone");
            }
        }
        return 1;
    }

    private static int why(CommandSourceStack source, BlockPos pos) {
        ServerLevel world = source.getLevel();
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        say(source, "Block " + BuiltInRegistries.BLOCK.getKey(block)
            + " @ " + pos.getX() + " " + pos.getY() + " " + pos.getZ());
        say(source, " opaque=" + state.canOcclude()
            + " fullCube=" + state.isCollisionShapeFullBlock(world, pos)
            + " lightValue=" + state.getLightEmission(world, pos)
            + " blockEntity=" + state.hasBlockEntity()
            + " hardness=" + state.getDestroySpeed(world, pos));
        say(source, " ore=" + SurfaceScanner.isOre(state)
            + " blacklisted=" + SurfaceScanner.isBlacklisted(block)
            + " existingLamp=" + SurfaceScanner.isLamp(block));
        say(source, " -> replaceable by lantern: "
            + (SurfaceScanner.isValidGround(state, world, pos) ? "YES" : "NO"));
        BlockPos above = pos.above();
        say(source, " light above: block=" + world.getBrightness(LightLayer.BLOCK, above)
            + " sky=" + world.getBrightness(LightLayer.SKY, above)
            + " -> " + (world.getBrightness(LightLayer.BLOCK, above) <= LanternConfig.lightThreshold
                ? "DARK (lantern would act)" : "lit (lantern would skip)"));
        BlockPos standable = SurfaceScanner.findStandableSurface(world,
            pos.getX(), pos.getZ(), pos.getY(), LanternConfig.verticalRange);
        say(source, " standable surface in this column: "
            + (standable == null ? "none" : standable.getX() + " " + standable.getY() + " " + standable.getZ()));
        return 1;
    }

    private static int scan(CommandSourceStack source, int radius) {
        ServerLevel world = source.getLevel();
        BlockPos center = BlockPos.containing(source.getPosition());
        int vr = LanternConfig.verticalRange;
        List<BlockPos> dark = new ArrayList<>();
        for (int x = center.getX() - radius; x <= center.getX() + radius; x++) {
            for (int z = center.getZ() - radius; z <= center.getZ() + radius; z++) {
                BlockPos standable = SurfaceScanner.findStandableSurface(world, x, z, center.getY(), vr);
                if (standable != null
                    && world.getBrightness(LightLayer.BLOCK, standable.above()) <= LanternConfig.lightThreshold) {
                    dark.add(standable.above());
                }
            }
        }
        say(source, "Dark standable spots within " + radius + " blocks: " + dark.size());
        for (int i = 0; i < Math.min(10, dark.size()); i++) {
            BlockPos p = dark.get(i);
            say(source, "  " + p.getX() + " " + p.getY() + " " + p.getZ()
                + " (block light " + world.getBrightness(LightLayer.BLOCK, p) + ")");
        }
        if (dark.size() > 10) {
            say(source, "  ... and " + (dark.size() - 10) + " more");
        }
        return dark.size();
    }

    private static int undo(CommandSourceStack source, int radius) {
        ServerLevel world = source.getLevel();
        BlockPos center = BlockPos.containing(source.getPosition());
        int vertical = Math.min(radius, 16);
        int count = 0;
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-radius, -vertical, -radius), center.offset(radius, vertical, radius))) {
            if (!world.isLoaded(pos)) {
                continue;
            }
            Block block = world.getBlockState(pos).getBlock();
            if (block == ModBlocks.HIDDEN_LIGHT.get()) {
                BlockState mimic = world.getBlockEntity(pos) instanceof HiddenLightTileEntity light
                    ? light.getMimic() : null;
                world.setBlock(pos.immutable(),
                    mimic != null ? mimic : Blocks.STONE.defaultBlockState(), 3);
                count++;
            }
        }
        say(source, "Reverted " + count + " hidden lights within " + radius
            + " blocks back to the block they mimic (stone when unknown).");
        return count;
    }
}

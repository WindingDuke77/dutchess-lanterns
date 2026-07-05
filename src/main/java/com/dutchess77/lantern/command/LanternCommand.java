package com.dutchess77.lantern.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.dutchess77.lantern.Lantern;
import com.dutchess77.lantern.LanternConfig;
import com.dutchess77.lantern.compat.EnderIOPaintHelper;
import com.dutchess77.lantern.handler.LanternTickHandler;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;

/** Debug commands: /lantern status | why [x y z] | scan [radius] | undo [radius] */
public class LanternCommand extends CommandBase {

    @Override
    public String getName() {
        return "lantern";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/lantern help | status | why [x y z] | scan [radius] | undo [radius]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender,
                                          String[] args, BlockPos targetPos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "help", "status", "why", "scan", "undo");
        }
        return new ArrayList<>();
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            throw new WrongUsageException(getUsage(sender));
        }
        World world = sender.getEntityWorld();
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "help":
                help(sender);
                break;
            case "status":
                status(sender);
                break;
            case "why":
                why(sender, world, args.length >= 4
                    ? parseBlockPos(sender, args, 1, false)
                    : sender.getPosition().down());
                break;
            case "scan":
                scan(sender, world, args.length >= 2 ? parseInt(args[1], 1, 64) : LanternConfig.horizontalRadius);
                break;
            case "undo":
                undo(sender, world, args.length >= 2 ? parseInt(args[1], 1, 64) : LanternConfig.horizontalRadius);
                break;
            default:
                throw new WrongUsageException(getUsage(sender));
        }
    }

    private static void say(ICommandSender sender, String message) {
        sender.sendMessage(new TextComponentString(message));
    }

    private static void help(ICommandSender sender) {
        say(sender, "=== Lantern " + Lantern.VERSION + " ===");
        say(sender, "Sneak + Right-Click (air): toggle on/off (glints while on)");
        say(sender, "Right-Click: load fuel from inventory + carried containers");
        say(sender, "Sneak + Right-Click ON A BLOCK: reclaim hidden lights nearby (refunds glowstone)");
        say(sender, "Works held, on the hotbar, or worn in the Baubles charm slot");
        say(sender, "While on: sweeps torches back to you, buries invisible painted glowstone");
        say(sender, "on a global 6-block grid, gap-fills walls/awkward spots, lights underwater");
        say(sender, "Variants: Energy (FE, nether star), Torch (places torches), Creative (free, visible)");
        say(sender, "Debug: /lantern status | why | scan | undo -- config in config/lantern.cfg");
    }

    private static void status(ICommandSender sender) {
        say(sender, "Lantern " + Lantern.VERSION + " debug status");
        say(sender, " EnderIO painted glowstone: "
            + (EnderIOPaintHelper.isAvailable()
                ? "available (" + EnderIOPaintHelper.paintedGlowstoneSolid().getRegistryName() + ")"
                : "MISSING"));
        say(sender, " gridSpacing=" + LanternConfig.gridSpacing
            + " radius=" + LanternConfig.horizontalRadius
            + " vertical=" + LanternConfig.verticalRange
            + " interval=" + LanternConfig.tickInterval + "t");
        say(sender, " lightThreshold=" + LanternConfig.lightThreshold
            + " fillGaps=" + LanternConfig.fillGaps
            + " sparkle=" + LanternConfig.sparkleSeconds + "s");
        say(sender, " buffer=" + LanternConfig.bufferCapacity
            + " energy=" + LanternConfig.energyCapacity + "FE @" + LanternConfig.energyPerLight + "FE/light");
    }

    private static void why(ICommandSender sender, World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        say(sender, "Block " + block.getRegistryName() + " @ " + pos.getX() + " " + pos.getY() + " " + pos.getZ());
        say(sender, " opaque=" + state.isOpaqueCube()
            + " fullCube=" + state.isFullCube()
            + " lightValue=" + state.getLightValue(world, pos)
            + " tileEntity=" + block.hasTileEntity(state)
            + " hardness=" + state.getBlockHardness(world, pos));
        say(sender, " ore=" + LanternTickHandler.isOre(state)
            + " blacklisted=" + LanternTickHandler.isBlacklisted(block)
            + " existingLamp=" + LanternTickHandler.isLamp(block));
        say(sender, " -> replaceable by lantern: "
            + (LanternTickHandler.isValidGround(state, world, pos) ? "YES" : "NO"));
        BlockPos above = pos.up();
        say(sender, " light above: block=" + world.getLightFor(EnumSkyBlock.BLOCK, above)
            + " sky=" + world.getLightFor(EnumSkyBlock.SKY, above)
            + " -> " + (world.getLightFor(EnumSkyBlock.BLOCK, above) <= LanternConfig.lightThreshold
                ? "DARK (lantern would act)" : "lit (lantern would skip)"));
        BlockPos standable = LanternTickHandler.findStandableSurface(world,
            pos.getX(), pos.getZ(), pos.getY(), LanternConfig.verticalRange);
        say(sender, " standable surface in this column: "
            + (standable == null ? "none" : standable.getX() + " " + standable.getY() + " " + standable.getZ()));
    }

    private static void scan(ICommandSender sender, World world, int radius) {
        BlockPos center = sender.getPosition();
        int vr = LanternConfig.verticalRange;
        List<BlockPos> dark = new ArrayList<>();
        for (int x = center.getX() - radius; x <= center.getX() + radius; x++) {
            for (int z = center.getZ() - radius; z <= center.getZ() + radius; z++) {
                BlockPos standable = LanternTickHandler.findStandableSurface(world, x, z, center.getY(), vr);
                if (standable != null
                    && world.getLightFor(EnumSkyBlock.BLOCK, standable.up()) <= LanternConfig.lightThreshold) {
                    dark.add(standable.up());
                }
            }
        }
        say(sender, "Dark standable spots within " + radius + " blocks: " + dark.size());
        for (int i = 0; i < Math.min(10, dark.size()); i++) {
            BlockPos p = dark.get(i);
            say(sender, "  " + p.getX() + " " + p.getY() + " " + p.getZ()
                + " (block light " + world.getLightFor(EnumSkyBlock.BLOCK, p) + ")");
        }
        if (dark.size() > 10) {
            say(sender, "  ... and " + (dark.size() - 10) + " more");
        }
    }

    private static void undo(ICommandSender sender, World world, int radius) {
        if (!EnderIOPaintHelper.isAvailable()) {
            say(sender, "EnderIO painted glowstone not available");
            return;
        }
        BlockPos center = sender.getPosition();
        int vertical = Math.min(radius, 16);
        int count = 0;
        for (BlockPos pos : BlockPos.getAllInBoxMutable(
                center.add(-radius, -vertical, -radius), center.add(radius, vertical, radius))) {
            if (!world.isBlockLoaded(pos)) {
                continue;
            }
            if (EnderIOPaintHelper.isPaintedGlowstone(world.getBlockState(pos).getBlock())) {
                TileEntity te = world.getTileEntity(pos);
                IBlockState paint = EnderIOPaintHelper.getPaint(te);
                world.setBlockState(pos.toImmutable(),
                    paint != null ? paint : Blocks.STONE.getDefaultState(), 3);
                count++;
            }
        }
        say(sender, "Reverted " + count + " hidden lights within " + radius
            + " blocks back to their painted block (stone when unpainted).");
    }
}

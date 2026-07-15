package com.dutchess77.lantern.item;

import java.util.List;

import javax.annotation.Nullable;

import com.dutchess77.lantern.LanternConfig;
import com.dutchess77.lantern.ModBlocks;
import com.dutchess77.lantern.block.HiddenLightTileEntity;
import com.dutchess77.lantern.fx.SparkleManager;
import com.dutchess77.lantern.logic.SurfaceScanner;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Manual, single-block sibling of the Lantern: right-click a block to swap it
 * for a hidden light disguised as that block; sneak + right-click a hidden
 * light to swap it back. Same fuel, sockets and bench as the Lantern, but it
 * never auto-places (it is never Active, so the tick handler ignores it).
 */
public class GlowWandItem extends LanternItem {

    public GlowWandItem() {
        this("glow_wand");
    }

    protected GlowWandItem(String name) {
        super(name);
    }

    /** Marks swapped blocks as FE-paid, so reverting them refunds no glowstone. */
    protected boolean paysWithEnergy() {
        return false;
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand,
                                      EnumFacing facing, float hitX, float hitY, float hitZ) {
        ItemStack stack = player.getHeldItem(hand);
        return player.isSneaking()
            ? revert(player, stack, world, pos)
            : swap(player, stack, world, pos);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (player.isSneaking()) {
            return new ActionResult<>(EnumActionResult.PASS, stack);
        }
        // Range upgrades extend targeting past vanilla reach; only then can a
        // block be hit here that onItemUse did not already handle
        RayTraceResult hit = rayTraceExtended(world, player, LanternUpgrades.effectiveReach(stack));
        if (hit != null && hit.typeOfHit == RayTraceResult.Type.BLOCK) {
            swap(player, stack, world, hit.getBlockPos());
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }
        if (!world.isRemote) {
            fill(player, stack);
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    private EnumActionResult swap(EntityPlayer player, ItemStack stack, World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        if (state.getBlock() == ModBlocks.HIDDEN_LIGHT) {
            if (!world.isRemote) {
                player.sendStatusMessage(new TextComponentTranslation("chat.lantern.wand_already"), true);
            }
            return EnumActionResult.SUCCESS;
        }
        // same rules as auto-placement: no lights, ores, containers, unbreakables
        if (!SurfaceScanner.canHost(world, pos)) {
            if (!world.isRemote) {
                player.sendStatusMessage(new TextComponentTranslation("chat.lantern.wand_invalid"), true);
            }
            return EnumActionResult.SUCCESS;
        }
        if (world.isRemote) {
            return EnumActionResult.SUCCESS;
        }
        if (!consumePlacementCost(player, stack)) {
            player.sendStatusMessage(new TextComponentTranslation("chat.lantern.no_fuel"), true);
            return EnumActionResult.SUCCESS;
        }
        world.setBlockState(pos, ModBlocks.HIDDEN_LIGHT.getDefaultState(), 3);
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof HiddenLightTileEntity) {
            ((HiddenLightTileEntity) te).setMimic(state, paysWithEnergy());
        }
        SparkleManager.add(world, pos);
        world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
            SoundEvents.BLOCK_NOTE_PLING, SoundCategory.PLAYERS, 0.6F, 1.4F);
        return EnumActionResult.SUCCESS;
    }

    private EnumActionResult revert(EntityPlayer player, ItemStack stack, World world, BlockPos pos) {
        if (world.getBlockState(pos).getBlock() != ModBlocks.HIDDEN_LIGHT) {
            return EnumActionResult.PASS;
        }
        if (!world.isRemote) {
            TileEntity te = world.getTileEntity(pos);
            IBlockState mimic = null;
            boolean fromEnergy = false;
            if (te instanceof HiddenLightTileEntity) {
                mimic = ((HiddenLightTileEntity) te).getMimic();
                fromEnergy = ((HiddenLightTileEntity) te).isFromEnergy();
            }
            world.setBlockState(pos, mimic != null ? mimic : Blocks.STONE.getDefaultState(), 3);
            if (!fromEnergy) {
                refundReclaimed(player, stack, 1);
            }
            player.sendStatusMessage(new TextComponentTranslation("chat.lantern.wand_reverted"), true);
            world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                SoundEvents.BLOCK_NOTE_PLING, SoundCategory.PLAYERS, 0.6F, 0.7F);
        }
        return EnumActionResult.SUCCESS;
    }

    @Nullable
    private static RayTraceResult rayTraceExtended(World world, EntityPlayer player, double reach) {
        Vec3d start = new Vec3d(player.posX, player.posY + player.getEyeHeight(), player.posZ);
        Vec3d look = player.getLook(1.0F);
        Vec3d end = start.add(new Vec3d(look.x * reach, look.y * reach, look.z * reach));
        return world.rayTraceBlocks(start, end, false, true, false);
    }

    @Override
    protected String howtoFillKey() {
        return "tooltip.lantern.wand_fill";
    }

    @Override
    @SideOnly(Side.CLIENT)
    protected String describeCost() {
        return I18n.format("tooltip.lantern.cost_wand");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        tooltip.add(describeFuel(stack));
        tooltip.add(TextFormatting.GRAY + describeCost());
        List<LanternUpgrades.Installed> upgrades = LanternUpgrades.list(stack);
        tooltip.add(TextFormatting.LIGHT_PURPLE + I18n.format("tooltip.lantern.sockets",
            upgrades.size(), LanternUpgrades.socketCount(stack)));
        for (LanternUpgrades.Installed upgrade : upgrades) {
            tooltip.add(TextFormatting.LIGHT_PURPLE + "  " + I18n.format(
                "item.lantern." + upgrade.type.key + "_t" + upgrade.tier + ".name"));
        }
        int reach = LanternUpgrades.effectiveReach(stack);
        String reachLine = I18n.format("tooltip.lantern.stat_reach", reach);
        if (reach >= LanternUpgrades.MAX_REACH) {
            reachLine += I18n.format("tooltip.lantern.stat_max");
        }
        tooltip.add(TextFormatting.AQUA + reachLine);
        float freeChance = LanternUpgrades.freeChance(stack);
        if (freeChance > 0.0F) {
            String efficiencyLine = I18n.format("tooltip.lantern.stat_efficiency", Math.round(freeChance * 100.0F));
            if (freeChance >= 0.8F) {
                efficiencyLine += I18n.format("tooltip.lantern.stat_max");
            }
            tooltip.add(TextFormatting.AQUA + efficiencyLine);
        }
        tooltip.add("");
        tooltip.add(TextFormatting.DARK_GRAY.toString() + TextFormatting.ITALIC + I18n.format("tooltip.lantern.wand_howto1"));
        tooltip.add(TextFormatting.DARK_GRAY.toString() + TextFormatting.ITALIC + I18n.format("tooltip.lantern.wand_howto2"));
        tooltip.add(TextFormatting.DARK_GRAY.toString() + TextFormatting.ITALIC + I18n.format(howtoFillKey()));
    }
}

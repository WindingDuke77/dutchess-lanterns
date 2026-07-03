package com.dutchess77.lantern.item;

import java.util.List;

import javax.annotation.Nullable;

import com.dutchess77.lantern.Lantern;
import com.dutchess77.lantern.LanternConfig;

import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class LanternItem extends Item {

    private static final String TAG_ACTIVE = "Active";
    private static final String TAG_CHARGE = "Charge";
    private static final String TAG_SPACING = "Spacing";

    public static final int MIN_SPACING = 5;
    public static final int MAX_SPACING = 7;

    public LanternItem() {
        setRegistryName(Lantern.MODID, "lantern");
        setTranslationKey(Lantern.MODID + ".lantern");
        setCreativeTab(CreativeTabs.TOOLS);
        setMaxStackSize(1);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (player.isSneaking()) {
            boolean nowActive = !isActive(stack);
            setActive(stack, nowActive);
            if (!world.isRemote) {
                player.sendStatusMessage(new TextComponentTranslation(
                    nowActive ? "chat.lantern.on" : "chat.lantern.off"), true);
                world.playSound(null, player.posX, player.posY, player.posZ,
                    SoundEvents.BLOCK_LEVER_CLICK, SoundCategory.PLAYERS, 0.4F, nowActive ? 0.8F : 0.6F);
            }
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }
        if (!world.isRemote) {
            int moved = fillFromInventory(player, stack);
            player.sendStatusMessage(new TextComponentTranslation("chat.lantern.charge",
                getCharge(stack), LanternConfig.bufferCapacity), true);
            if (moved > 0) {
                world.playSound(null, player.posX, player.posY, player.posZ,
                    SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.PLAYERS, 0.6F, 1.0F);
            }
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    private static int fillFromInventory(EntityPlayer player, ItemStack stack) {
        int capacity = LanternConfig.bufferCapacity;
        int charge = Math.min(getCharge(stack), capacity);
        int moved = 0;
        Item glowstone = Item.getItemFromBlock(Blocks.GLOWSTONE);
        for (ItemStack slot : player.inventory.mainInventory) {
            if (charge >= capacity) {
                break;
            }
            if (!slot.isEmpty() && slot.getItem() == glowstone && !slot.hasTagCompound()) {
                int take = Math.min(slot.getCount(), capacity - charge);
                slot.shrink(take);
                charge += take;
                moved += take;
            }
        }
        setCharge(stack, charge);
        return moved;
    }

    @Override
    public boolean showDurabilityBar(ItemStack stack) {
        return true;
    }

    @Override
    public double getDurabilityForDisplay(ItemStack stack) {
        int capacity = Math.max(1, LanternConfig.bufferCapacity);
        return 1.0D - Math.min(getCharge(stack), capacity) / (double) capacity;
    }

    @Override
    public int getRGBDurabilityForDisplay(ItemStack stack) {
        return 0xFFD860;
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return isActive(stack);
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        // charge NBT churns every placement pass; only re-equip on a real change
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        tooltip.add(I18n.format(isActive(stack) ? "tooltip.lantern.active" : "tooltip.lantern.inactive"));
        tooltip.add(I18n.format("tooltip.lantern.charge", getCharge(stack), LanternConfig.bufferCapacity));
        tooltip.add(I18n.format("tooltip.lantern.spacing", getSpacing(stack), getSpacing(stack)));
        tooltip.add(I18n.format("tooltip.lantern.howto"));
    }

    public static boolean isActive(ItemStack stack) {
        return stack.hasTagCompound() && stack.getTagCompound().getBoolean(TAG_ACTIVE);
    }

    public static void setActive(ItemStack stack, boolean active) {
        getOrCreateTag(stack).setBoolean(TAG_ACTIVE, active);
    }

    public static int getCharge(ItemStack stack) {
        return stack.hasTagCompound() ? stack.getTagCompound().getInteger(TAG_CHARGE) : 0;
    }

    public static void setCharge(ItemStack stack, int charge) {
        getOrCreateTag(stack).setInteger(TAG_CHARGE, Math.max(0, charge));
    }

    /** Item-level grid spacing; falls back to the config default when never scrolled. */
    public static int getSpacing(ItemStack stack) {
        if (stack.hasTagCompound() && stack.getTagCompound().hasKey(TAG_SPACING)) {
            return clampSpacing(stack.getTagCompound().getInteger(TAG_SPACING));
        }
        return LanternConfig.gridSpacing;
    }

    /** Shifts spacing by dir (+1/-1) within [MIN_SPACING, MAX_SPACING]; returns the new value. */
    public static int adjustSpacing(ItemStack stack, int dir) {
        int next = clampSpacing(clampSpacing(getSpacing(stack)) + Integer.signum(dir));
        getOrCreateTag(stack).setInteger(TAG_SPACING, next);
        return next;
    }

    private static int clampSpacing(int spacing) {
        return Math.min(MAX_SPACING, Math.max(MIN_SPACING, spacing));
    }

    private static NBTTagCompound getOrCreateTag(ItemStack stack) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        return stack.getTagCompound();
    }
}

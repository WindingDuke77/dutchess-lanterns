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
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class LanternItem extends Item {

    private static final String TAG_ACTIVE = "Active";
    private static final String TAG_CHARGE = "Charge";

    public LanternItem() {
        this("lantern");
    }

    protected LanternItem(String name) {
        setRegistryName(Lantern.MODID, name);
        setTranslationKey(Lantern.MODID + "." + name);
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
                    SoundEvents.BLOCK_NOTE_PLING, SoundCategory.PLAYERS, 0.7F, nowActive ? 1.6F : 0.6F);
            }
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }
        if (!world.isRemote) {
            fill(player, stack);
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    /** The inventory item this lantern burns (Glowstone blocks by default). */
    protected Item fuelItem() {
        return Item.getItemFromBlock(Blocks.GLOWSTONE);
    }

    protected String chargeChatKey() {
        return "chat.lantern.charge";
    }

    /** Plain right-click loads fuel items from the inventory into the buffer. */
    protected void fill(EntityPlayer player, ItemStack stack) {
        int capacity = LanternConfig.bufferCapacity;
        int charge = Math.min(getCharge(stack), capacity);
        int moved = 0;
        Item fuel = fuelItem();
        for (ItemStack slot : player.inventory.mainInventory) {
            if (charge >= capacity) {
                break;
            }
            if (!slot.isEmpty() && slot.getItem() == fuel && !slot.hasTagCompound()) {
                int take = Math.min(slot.getCount(), capacity - charge);
                slot.shrink(take);
                charge += take;
                moved += take;
            }
        }
        setCharge(stack, charge);
        player.sendStatusMessage(new TextComponentTranslation(chargeChatKey(),
            charge, capacity), true);
        if (moved > 0) {
            player.world.playSound(null, player.posX, player.posY, player.posZ,
                SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.PLAYERS, 0.6F, 1.0F);
        }
    }

    /** Pays for one placed light: buffer first, then raw fuel items from the inventory. */
    public boolean consumePlacementCost(EntityPlayer player, ItemStack stack) {
        if (player.capabilities.isCreativeMode) {
            return true;
        }
        int charge = getCharge(stack);
        if (charge > 0) {
            setCharge(stack, charge - 1);
            return true;
        }
        Item fuel = fuelItem();
        for (ItemStack slot : player.inventory.mainInventory) {
            if (!slot.isEmpty() && slot.getItem() == fuel && !slot.hasTagCompound()) {
                slot.shrink(1);
                return true;
            }
        }
        return false;
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
        // fuel NBT churns every placement pass; only re-equip on a real change
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        boolean active = isActive(stack);
        tooltip.add((active ? TextFormatting.GREEN : TextFormatting.DARK_GRAY)
            + I18n.format(active ? "tooltip.lantern.active" : "tooltip.lantern.inactive"));
        tooltip.add(describeFuel(stack));
        tooltip.add(TextFormatting.GRAY + describeCost());
        tooltip.add("");
        tooltip.add(TextFormatting.DARK_GRAY.toString() + TextFormatting.ITALIC + I18n.format("tooltip.lantern.howto1"));
        tooltip.add(TextFormatting.DARK_GRAY.toString() + TextFormatting.ITALIC + I18n.format(howtoFillKey()));
    }

    @SideOnly(Side.CLIENT)
    protected String describeFuel(ItemStack stack) {
        return TextFormatting.YELLOW
            + I18n.format("tooltip.lantern.charge", getCharge(stack), LanternConfig.bufferCapacity);
    }

    @SideOnly(Side.CLIENT)
    protected String describeCost() {
        return I18n.format("tooltip.lantern.cost");
    }

    protected String howtoFillKey() {
        return "tooltip.lantern.howto2";
    }

    public static boolean isActive(ItemStack stack) {
        return stack.hasTagCompound() && stack.getTagCompound().getBoolean(TAG_ACTIVE);
    }

    public static void setActive(ItemStack stack, boolean active) {
        getOrCreateTag(stack).setBoolean(TAG_ACTIVE, active);
    }

    /** A stack with no Charge tag (/give, creative menu, JEI) counts as full; crafted stacks carry Charge:0. */
    public static int getCharge(ItemStack stack) {
        if (stack.hasTagCompound() && stack.getTagCompound().hasKey(TAG_CHARGE)) {
            return stack.getTagCompound().getInteger(TAG_CHARGE);
        }
        return LanternConfig.bufferCapacity;
    }

    public static void setCharge(ItemStack stack, int charge) {
        getOrCreateTag(stack).setInteger(TAG_CHARGE, Math.max(0, charge));
    }

    protected static NBTTagCompound getOrCreateTag(ItemStack stack) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        return stack.getTagCompound();
    }
}

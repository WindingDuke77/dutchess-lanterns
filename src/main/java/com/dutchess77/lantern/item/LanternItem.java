package com.dutchess77.lantern.item;

import java.util.List;

import javax.annotation.Nullable;

import com.dutchess77.lantern.Lantern;
import com.dutchess77.lantern.LanternConfig;
import com.dutchess77.lantern.ModBlocks;
import com.dutchess77.lantern.block.HiddenLightTileEntity;
import com.dutchess77.lantern.compat.EnderIOPaintHelper;

import net.minecraft.block.state.IBlockState;
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
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

public class LanternItem extends Item implements baubles.api.IBauble {

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

    /** Sneak + use ON A BLOCK reclaims placed hidden lights around it (air-click still toggles). */
    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand,
                                      EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!player.isSneaking()) {
            return EnumActionResult.PASS;
        }
        if (!world.isRemote) {
            int reclaimed = reclaimLights(player, player.getHeldItem(hand), world, pos);
            player.sendStatusMessage(new TextComponentTranslation("chat.lantern.reclaimed", reclaimed), true);
            if (reclaimed > 0) {
                world.playSound(null, player.posX, player.posY, player.posZ,
                    SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.5F, 0.8F);
            }
        }
        return EnumActionResult.SUCCESS;
    }

    private int reclaimLights(EntityPlayer player, ItemStack lantern, World world, BlockPos center) {
        int rh = LanternConfig.horizontalRadius;
        int rv = LanternConfig.verticalRange;
        int count = 0;
        int refundable = 0; // FE-paid lights refund no glowstone
        for (BlockPos pos : BlockPos.getAllInBoxMutable(center.add(-rh, -rv, -rh), center.add(rh, rv, rh))) {
            if (!world.isBlockLoaded(pos)) {
                continue;
            }
            net.minecraft.block.Block block = world.getBlockState(pos).getBlock();
            if (block == ModBlocks.HIDDEN_LIGHT) {
                net.minecraft.tileentity.TileEntity te = world.getTileEntity(pos);
                IBlockState mimic = null;
                boolean fromEnergy = false;
                if (te instanceof HiddenLightTileEntity) {
                    mimic = ((HiddenLightTileEntity) te).getMimic();
                    fromEnergy = ((HiddenLightTileEntity) te).isFromEnergy();
                }
                world.setBlockState(pos.toImmutable(),
                    mimic != null ? mimic : Blocks.STONE.getDefaultState(), 3);
                count++;
                if (!fromEnergy) {
                    refundable++;
                }
            } else if (EnderIOPaintHelper.isPaintedGlowstone(block)) {
                // legacy lights placed by pre-1.11 versions
                IBlockState paint = EnderIOPaintHelper.getPaint(world.getTileEntity(pos));
                world.setBlockState(pos.toImmutable(),
                    paint != null ? paint : Blocks.STONE.getDefaultState(), 3);
                count++;
                refundable++;
            }
        }
        if (refundable > 0 && refundOnReclaim()) {
            refundReclaimed(player, lantern, refundable);
        }
        return count;
    }

    protected boolean refundOnReclaim() {
        return true;
    }

    /** Refund reclaimed lights as Glowstone: buffer first when this lantern burns Glowstone, else items. */
    protected void refundReclaimed(EntityPlayer player, ItemStack lantern, int count) {
        if (fuelItem() == Item.getItemFromBlock(Blocks.GLOWSTONE)) {
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

    /** Buffer size including Capacity upgrades. */
    public int capacityOf(ItemStack stack) {
        return LanternUpgrades.effectiveCapacity(stack, LanternConfig.bufferCapacity);
    }

    /** Plain right-click loads fuel items from the inventory into the buffer. */
    protected void fill(EntityPlayer player, ItemStack stack) {
        int capacity = capacityOf(stack);
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
        // top up from carried containers (backpacks etc.)
        if (LanternConfig.refillFromContainers && charge < capacity) {
            for (ItemStack held : player.inventory.mainInventory) {
                if (charge >= capacity) {
                    break;
                }
                IItemHandler container = containerOf(held);
                if (container == null) {
                    continue;
                }
                for (int i = 0; i < container.getSlots() && charge < capacity; i++) {
                    ItemStack inside = container.getStackInSlot(i);
                    if (!inside.isEmpty() && inside.getItem() == fuel && !inside.hasTagCompound()) {
                        ItemStack taken = container.extractItem(i, capacity - charge, false);
                        charge += taken.getCount();
                        moved += taken.getCount();
                    }
                }
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
        if (player.capabilities.isCreativeMode && LanternConfig.freeInCreative) {
            return true;
        }
        if (player.getRNG().nextFloat() < LanternUpgrades.freeChance(stack)) {
            return true; // Efficiency upgrade proc
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
        return pullOneFromContainers(player, fuel);
    }

    /** Last resort: pull a single fuel item out of a carried container (backpack etc.). */
    private static boolean pullOneFromContainers(EntityPlayer player, Item fuel) {
        if (!LanternConfig.refillFromContainers) {
            return false;
        }
        for (ItemStack held : player.inventory.mainInventory) {
            IItemHandler container = containerOf(held);
            if (container == null) {
                continue;
            }
            for (int i = 0; i < container.getSlots(); i++) {
                ItemStack inside = container.getStackInSlot(i);
                if (!inside.isEmpty() && inside.getItem() == fuel && !inside.hasTagCompound()
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
        return stack.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null)
            ? stack.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null)
            : null;
    }

    @Override
    public boolean showDurabilityBar(ItemStack stack) {
        return true;
    }

    @Override
    public double getDurabilityForDisplay(ItemStack stack) {
        int capacity = Math.max(1, capacityOf(stack));
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
        java.util.List<LanternUpgrades.Installed> upgrades = LanternUpgrades.list(stack);
        tooltip.add(TextFormatting.LIGHT_PURPLE + I18n.format("tooltip.lantern.sockets",
            upgrades.size(), LanternUpgrades.socketCount(stack)));
        for (LanternUpgrades.Installed upgrade : upgrades) {
            tooltip.add(TextFormatting.LIGHT_PURPLE + "  " + I18n.format(
                "item.lantern." + upgrade.type.key + "_t" + upgrade.tier + ".name"));
        }
        // raw effective stats
        int radius = LanternUpgrades.effectiveRadius(stack, LanternConfig.horizontalRadius);
        String radiusLine = I18n.format("tooltip.lantern.stat_range", radius, radius * 2 + 1, radius * 2 + 1);
        if (radius >= 32) {
            radiusLine += I18n.format("tooltip.lantern.stat_max");
        }
        tooltip.add(TextFormatting.AQUA + radiusLine);
        float freeChance = LanternUpgrades.freeChance(stack);
        if (freeChance > 0.0F) {
            String efficiencyLine = I18n.format("tooltip.lantern.stat_efficiency", Math.round(freeChance * 100.0F));
            if (freeChance >= 0.8F) {
                efficiencyLine += I18n.format("tooltip.lantern.stat_max");
            }
            tooltip.add(TextFormatting.AQUA + efficiencyLine);
        }
        tooltip.add(TextFormatting.AQUA + I18n.format("tooltip.lantern.stat_interval",
            LanternConfig.tickInterval, LanternConfig.gridSpacing, LanternConfig.gridSpacing));
        tooltip.add("");
        tooltip.add(TextFormatting.DARK_GRAY.toString() + TextFormatting.ITALIC + I18n.format("tooltip.lantern.howto1"));
        tooltip.add(TextFormatting.DARK_GRAY.toString() + TextFormatting.ITALIC + I18n.format(howtoFillKey()));
        if (net.minecraftforge.fml.common.Loader.isModLoaded("baubles")) {
            tooltip.add(TextFormatting.DARK_PURPLE.toString() + TextFormatting.ITALIC
                + I18n.format("tooltip.lantern.baubles"));
        }
    }

    @SideOnly(Side.CLIENT)
    protected String describeFuel(ItemStack stack) {
        return TextFormatting.YELLOW
            + I18n.format("tooltip.lantern.charge", getCharge(stack), capacityOf(stack));
    }

    @SideOnly(Side.CLIENT)
    protected String describeCost() {
        return I18n.format("tooltip.lantern.cost");
    }

    protected String howtoFillKey() {
        return "tooltip.lantern.howto2";
    }

    @Override
    public baubles.api.BaubleType getBaubleType(ItemStack stack) {
        return baubles.api.BaubleType.CHARM;
    }

    @Override
    public boolean willAutoSync(net.minecraft.item.ItemStack stack, net.minecraft.entity.EntityLivingBase player) {
        return true; // charge NBT changes while worn must reach the client
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
        return stack.getItem() instanceof LanternItem
            ? ((LanternItem) stack.getItem()).capacityOf(stack)
            : LanternConfig.bufferCapacity;
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

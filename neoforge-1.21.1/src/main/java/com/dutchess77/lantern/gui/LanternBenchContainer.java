package com.dutchess77.lantern.gui;

import com.dutchess77.lantern.ModMenus;
import com.dutchess77.lantern.block.LanternBenchTileEntity;
import com.dutchess77.lantern.item.LanternItem;
import com.dutchess77.lantern.item.LanternUpgrades;
import com.dutchess77.lantern.item.UpgradeItem;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * Bench menu: central lantern slot, four upgrade sockets, player inventory.
 * The 1.12.2 Baubles side panel is gone - Baubles does not exist on 1.21.
 */
public class LanternBenchContainer extends AbstractContainerMenu {

    /** N, W, E, S around the central lantern slot at (80, 35). */
    private static final int[][] SOCKET_POSITIONS = { {80, 13}, {58, 35}, {102, 35}, {80, 57} };

    private final LanternBenchTileEntity bench;

    /** Client factory: resolves the block entity from the pos the server wrote. */
    public LanternBenchContainer(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory,
            (LanternBenchTileEntity) playerInventory.player.level().getBlockEntity(buf.readBlockPos()));
    }

    public LanternBenchContainer(int containerId, Inventory playerInventory, LanternBenchTileEntity bench) {
        super(ModMenus.LANTERN_BENCH.get(), containerId);
        this.bench = bench;

        addSlot(new SlotItemHandler(bench.getInventory(), LanternBenchTileEntity.SLOT_LANTERN, 80, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                // no swapping a lantern in while sockets are occupied - take the old one out first
                return stack.getItem() instanceof LanternItem && bench.socketsEmpty();
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                if (bench.getLevel() != null && !bench.getLevel().isClientSide) {
                    bench.packInto(stack);
                }
                super.onTake(player, stack);
            }
        });

        for (int i = 0; i < LanternBenchTileEntity.SOCKET_COUNT; i++) {
            final int socketIndex = i;
            addSlot(new SlotItemHandler(bench.getInventory(),
                LanternBenchTileEntity.SOCKET_FIRST + i, SOCKET_POSITIONS[i][0], SOCKET_POSITIONS[i][1]) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    ItemStack lantern = bench.getInventory().getStackInSlot(LanternBenchTileEntity.SLOT_LANTERN);
                    return stack.getItem() instanceof UpgradeItem
                        && lantern.getItem() instanceof LanternItem
                        && socketIndex < LanternUpgrades.socketCount(lantern);
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }

                @Override
                public int getMaxStackSize(ItemStack stack) {
                    return 1;
                }
            });
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    public LanternBenchTileEntity getBench() {
        return bench;
    }

    @Override
    public boolean stillValid(Player player) {
        BlockPos pos = bench.getBlockPos();
        return bench.getLevel() != null
            && bench.getLevel().getBlockEntity(pos) == bench
            && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0D;
    }

    /**
     * Pack/unpack rewrites slots and the taken stack's data outside the click
     * the client predicted, leaving stale sockets and cursor until the GUI is
     * reopened - push a full resync after every click instead.
     */
    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        super.clicked(slotId, button, clickType, player);
        if (player instanceof ServerPlayer) {
            sendAllDataToRemote();
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack moved = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return moved;
        }
        ItemStack current = slot.getItem();
        moved = current.copy();
        int benchSlots = 1 + LanternBenchTileEntity.SOCKET_COUNT;
        int playerEnd = benchSlots + 36;
        if (index < benchSlots) {
            if (index == 0 && bench.getLevel() != null && !bench.getLevel().isClientSide) {
                // shift-clicking the lantern out: pack sockets into it first
                bench.packInto(current);
                moved = current.copy();
            }
            if (!moveItemStackTo(current, benchSlots, playerEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else if (current.getItem() instanceof LanternItem) {
            if (!moveItemStackTo(current, 0, 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (current.getItem() instanceof UpgradeItem) {
            if (!moveItemStackTo(current, 1, benchSlots, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }
        if (current.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (current.getCount() != moved.getCount()) {
            slot.onTake(player, current);
        }
        return moved;
    }
}

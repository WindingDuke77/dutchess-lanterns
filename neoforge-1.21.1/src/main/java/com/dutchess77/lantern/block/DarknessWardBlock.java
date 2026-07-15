package com.dutchess77.lantern.block;

import com.dutchess77.lantern.gui.DarknessWardContainer;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Keeps an area dark: no Lantern auto-places lights (or sweeps torches)
 * within wardRadius blocks of it - mob farms stay spawnable. The Glow Wand
 * deliberately ignores it, so manual lights still work inside.
 */
public class DarknessWardBlock extends BaseEntityBlock {

    private static final MapCodec<DarknessWardBlock> CODEC = simpleCodec(props -> new DarknessWardBlock());

    public DarknessWardBlock() {
        super(BlockBehaviour.Properties.of()
            .mapColor(MapColor.STONE)
            .strength(2.0F, 6.0F) // 1.12.2 setResistance(10.0F) == 6.0F explosion resistance here
            .sound(SoundType.STONE));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DarknessWardTileEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hitResult) {
        if (!level.isClientSide && player instanceof ServerPlayer sp
            && level.getBlockEntity(pos) instanceof DarknessWardTileEntity ward) {
            sp.openMenu(new SimpleMenuProvider(
                (id, inv, p) -> new DarknessWardContainer(id, inv, ward), getName()), pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState,
                            boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof DarknessWardTileEntity ward) {
                ItemStackHandler sockets = ward.getSockets();
                for (int i = 0; i < sockets.getSlots(); i++) {
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(),
                        sockets.getStackInSlot(i));
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}

package com.dutchess77.lantern.block;

import com.dutchess77.lantern.gui.LanternBenchContainer;
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

/** Workstation for socketing upgrades into lanterns. */
public class LanternBenchBlock extends BaseEntityBlock {

    private static final MapCodec<LanternBenchBlock> CODEC = simpleCodec(props -> new LanternBenchBlock());

    public LanternBenchBlock() {
        super(BlockBehaviour.Properties.of()
            .mapColor(MapColor.WOOD)
            .strength(2.0F)
            .sound(SoundType.WOOD)
            .lightLevel(state -> 6)); // soft workstation glow (1.12.2's 0.4F * 15)
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LanternBenchTileEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hitResult) {
        if (!level.isClientSide && player instanceof ServerPlayer sp
            && level.getBlockEntity(pos) instanceof LanternBenchTileEntity bench) {
            sp.openMenu(new SimpleMenuProvider(
                (id, inv, p) -> new LanternBenchContainer(id, inv, bench), getName()), pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState,
                            boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof LanternBenchTileEntity bench) {
                // socketed upgrades ride along inside the lantern
                bench.packInto(bench.getInventory().getStackInSlot(LanternBenchTileEntity.SLOT_LANTERN));
                for (int i = 0; i < bench.getInventory().getSlots(); i++) {
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(),
                        bench.getInventory().getStackInSlot(i));
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}

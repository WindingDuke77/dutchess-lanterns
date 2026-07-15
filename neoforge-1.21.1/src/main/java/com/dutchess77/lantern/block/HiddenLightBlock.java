package com.dutchess77.lantern.block;

import java.util.Collections;
import java.util.List;

import com.dutchess77.lantern.LanternConfig;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.HitResult;

/**
 * The mod's own invisible light: a full solid block at light 15 whose model
 * renders whatever block it replaced (stored in its block entity). Drops the
 * Glowstone block it cost.
 */
public class HiddenLightBlock extends BaseEntityBlock {

    private static final MapCodec<HiddenLightBlock> CODEC = simpleCodec(props -> new HiddenLightBlock());

    public HiddenLightBlock() {
        super(BlockBehaviour.Properties.of()
            .mapColor(MapColor.STONE)
            .strength(0.3F)
            .sound(SoundType.STONE)
            // registration-time value; runtime value from getLightEmission below
            .lightLevel(state -> LanternConfig.lightEmission)
            .noLootTable());
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HiddenLightTileEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /** Emission is configurable so disguised floors can be softened (default 15 = glowstone). */
    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        return LanternConfig.lightEmission;
    }

    /** Facade appearance: other mods (CTM-style connections, covers...) see the mimic. */
    @Override
    public BlockState getAppearance(BlockState state, BlockAndTintGetter level, BlockPos pos, Direction side,
                                    BlockState queryState, BlockPos queryPos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof HiddenLightTileEntity hidden) {
            BlockState mimic = hidden.getMimic();
            if (mimic != null && !(mimic.getBlock() instanceof HiddenLightBlock)) {
                return mimic;
            }
        }
        return state;
    }

    /** Drops the glowstone block it cost - unless an Energy Lantern paid FE for it. */
    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        BlockEntity be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (be instanceof HiddenLightTileEntity hidden && hidden.isFromEnergy()) {
            return Collections.emptyList();
        }
        return Collections.singletonList(new ItemStack(Blocks.GLOWSTONE));
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos,
                                       Player player) {
        return new ItemStack(this); // shows as "Hidden Light (Lantern)" in Jade/pick-block
    }

    /** Minimaps (Xaero's etc.) color the map from this - report the disguise, not stone gray. */
    @Override
    public MapColor getMapColor(BlockState state, BlockGetter level, BlockPos pos, MapColor defaultColor) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof HiddenLightTileEntity hidden) {
            BlockState mimic = hidden.getMimic();
            if (mimic != null && !(mimic.getBlock() instanceof HiddenLightBlock)) {
                try {
                    return mimic.getMapColor(level, pos);
                } catch (Throwable t) {
                    // fall through to stone
                }
            }
        }
        return MapColor.STONE;
    }

    /** Walking on the light sounds like the block it mimics. */
    @Override
    public SoundType getSoundType(BlockState state, LevelReader level, BlockPos pos, Entity entity) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof HiddenLightTileEntity hidden) {
            BlockState mimic = hidden.getMimic();
            if (mimic != null && !(mimic.getBlock() instanceof HiddenLightBlock)) {
                try {
                    return mimic.getBlock().getSoundType(mimic, level, pos, entity);
                } catch (Throwable t) {
                    // fall through to stone
                }
            }
        }
        return SoundType.STONE;
    }
}

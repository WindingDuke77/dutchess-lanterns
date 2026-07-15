package com.dutchess77.lantern;

import java.util.List;

import com.dutchess77.lantern.block.DarknessWardBlock;
import com.dutchess77.lantern.block.DarknessWardTileEntity;
import com.dutchess77.lantern.block.HiddenLightBlock;
import com.dutchess77.lantern.block.HiddenLightTileEntity;
import com.dutchess77.lantern.block.LanternBenchBlock;
import com.dutchess77.lantern.block.LanternBenchTileEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Lantern.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Lantern.MODID);

    public static final DeferredBlock<HiddenLightBlock> HIDDEN_LIGHT =
        BLOCKS.register("hidden_light", HiddenLightBlock::new);
    public static final DeferredBlock<LanternBenchBlock> LANTERN_BENCH =
        BLOCKS.register("lantern_bench", LanternBenchBlock::new);
    public static final DeferredBlock<DarknessWardBlock> DARKNESS_WARD =
        BLOCKS.register("darkness_ward", DarknessWardBlock::new);

    // own item form so pick-block/Jade say "Hidden Light (Lantern)", not vanilla glowstone
    public static final DeferredItem<BlockItem> HIDDEN_LIGHT_ITEM =
        ModItems.ITEMS.register("hidden_light", () -> new BlockItem(HIDDEN_LIGHT.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> LANTERN_BENCH_ITEM =
        ModItems.ITEMS.register("lantern_bench", () -> new BlockItem(LANTERN_BENCH.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> DARKNESS_WARD_ITEM =
        ModItems.ITEMS.register("darkness_ward", () -> new BlockItem(DARKNESS_WARD.get(), new Item.Properties()) {
            @Override
            public void appendHoverText(ItemStack stack, TooltipContext context,
                                        List<Component> tooltip, TooltipFlag flag) {
                tooltip.add(Component.translatable("tooltip.lantern.ward").withStyle(ChatFormatting.DARK_PURPLE));
                tooltip.add(Component.translatable("tooltip.lantern.ward2")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            }
        });

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HiddenLightTileEntity>> HIDDEN_LIGHT_BE =
        BLOCK_ENTITIES.register("hidden_light", () ->
            BlockEntityType.Builder.of(HiddenLightTileEntity::new, HIDDEN_LIGHT.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LanternBenchTileEntity>> LANTERN_BENCH_BE =
        BLOCK_ENTITIES.register("lantern_bench", () ->
            BlockEntityType.Builder.of(LanternBenchTileEntity::new, LANTERN_BENCH.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DarknessWardTileEntity>> DARKNESS_WARD_BE =
        BLOCK_ENTITIES.register("darkness_ward", () ->
            BlockEntityType.Builder.of(DarknessWardTileEntity::new, DARKNESS_WARD.get()).build(null));

    private ModBlocks() {
    }
}

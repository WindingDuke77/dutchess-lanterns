package com.dutchess77.lantern;

import com.dutchess77.lantern.block.HiddenLightBlock;
import com.dutchess77.lantern.block.HiddenLightTileEntity;
import com.dutchess77.lantern.client.HiddenLightBakedModel;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.color.IBlockColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = Lantern.MODID)
public class ModBlocks {

    public static final HiddenLightBlock HIDDEN_LIGHT = new HiddenLightBlock();

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        event.getRegistry().register(HIDDEN_LIGHT);
        GameRegistry.registerTileEntity(HiddenLightTileEntity.class, Lantern.MODID + ":hidden_light");
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        // own item form so pick-block/Waila say "Hidden Light (Lantern)", not vanilla glowstone
        event.getRegistry().register(
            new ItemBlock(HIDDEN_LIGHT).setRegistryName(HIDDEN_LIGHT.getRegistryName()));
    }

    @Mod.EventBusSubscriber(value = Side.CLIENT, modid = Lantern.MODID)
    public static class ClientBake {
        @SubscribeEvent
        public static void onModelRegistry(ModelRegistryEvent event) {
            Item item = Item.getItemFromBlock(HIDDEN_LIGHT);
            ModelLoader.setCustomModelResourceLocation(item, 0,
                new ModelResourceLocation(HIDDEN_LIGHT.getRegistryName(), "inventory"));
        }

        @SubscribeEvent
        public static void onModelBake(ModelBakeEvent event) {
            ModelResourceLocation location =
                new ModelResourceLocation(HIDDEN_LIGHT.getRegistryName(), "normal");
            IBakedModel base = event.getModelRegistry().getObject(location);
            if (base != null) {
                event.getModelRegistry().putObject(location, new HiddenLightBakedModel(base));
            }
        }

        /** Delegate tint lookups (grass green, foliage, water...) to the mimicked block. */
        @SubscribeEvent
        public static void onBlockColors(ColorHandlerEvent.Block event) {
            final net.minecraft.client.renderer.color.BlockColors colors = event.getBlockColors();
            colors.registerBlockColorHandler(new IBlockColor() {
                @Override
                public int colorMultiplier(IBlockState state, net.minecraft.world.IBlockAccess world,
                                           net.minecraft.util.math.BlockPos pos, int tintIndex) {
                    if (world != null && pos != null) {
                        TileEntity te = world.getTileEntity(pos);
                        if (te instanceof HiddenLightTileEntity) {
                            IBlockState mimic = ((HiddenLightTileEntity) te).getMimic();
                            if (mimic != null && !(mimic.getBlock() instanceof HiddenLightBlock)) {
                                try {
                                    return colors.colorMultiplier(mimic, world, pos, tintIndex);
                                } catch (Throwable t) {
                                    return -1;
                                }
                            }
                        }
                    }
                    return -1;
                }
            }, HIDDEN_LIGHT);
        }
    }
}

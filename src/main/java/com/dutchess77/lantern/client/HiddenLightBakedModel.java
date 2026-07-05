package com.dutchess77.lantern.client;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import com.dutchess77.lantern.block.HiddenLightBlock;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Renders the hidden light as the block it replaced; falls back to glowstone. */
@SideOnly(Side.CLIENT)
public class HiddenLightBakedModel implements IBakedModel {

    private final IBakedModel fallback;

    public HiddenLightBakedModel(IBakedModel fallback) {
        this.fallback = fallback;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
        IBlockState mimic = null;
        if (state instanceof IExtendedBlockState) {
            mimic = ((IExtendedBlockState) state).getValue(HiddenLightBlock.MIMIC);
        }
        BlockRenderLayer layer = MinecraftForgeClient.getRenderLayer();
        if (mimic == null || mimic.getBlock() instanceof HiddenLightBlock) {
            if (layer != null && layer != BlockRenderLayer.SOLID) {
                return Collections.emptyList();
            }
            return fallback.getQuads(state, side, rand);
        }
        if (layer != null && !mimic.getBlock().canRenderInLayer(mimic, layer)) {
            return Collections.emptyList();
        }
        try {
            IBakedModel model = Minecraft.getMinecraft().getBlockRendererDispatcher()
                .getBlockModelShapes().getModelForState(mimic);
            return model.getQuads(mimic, side, rand);
        } catch (Throwable t) {
            return fallback.getQuads(state, side, rand);
        }
    }

    @Override
    public boolean isAmbientOcclusion() {
        return true;
    }

    @Override
    public boolean isGui3d() {
        return fallback.isGui3d();
    }

    @Override
    public boolean isBuiltInRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleTexture() {
        return fallback.getParticleTexture();
    }

    @Override
    public ItemCameraTransforms getItemCameraTransforms() {
        return fallback.getItemCameraTransforms();
    }

    @Override
    public ItemOverrideList getOverrides() {
        return ItemOverrideList.NONE;
    }
}

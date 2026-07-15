package com.dutchess77.lantern.client;

import java.util.Collections;
import java.util.List;

import com.dutchess77.lantern.block.HiddenLightBlock;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;

/** Renders the hidden light as the block it replaced; falls back to glowstone. */
public class HiddenLightBakedModel implements IDynamicBakedModel {

    /** The disguised block state, published by HiddenLightTileEntity.getModelData(). */
    public static final ModelProperty<BlockState> MIMIC = new ModelProperty<>();

    private static final ChunkRenderTypeSet SOLID_ONLY = ChunkRenderTypeSet.of(RenderType.solid());

    private final BakedModel base;

    public HiddenLightBakedModel(BakedModel base) {
        this.base = base;
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand, ModelData data,
                                    RenderType renderType) {
        BlockState mimic = data.get(MIMIC);
        if (mimic == null || mimic.getBlock() instanceof HiddenLightBlock) {
            if (renderType != null && renderType != RenderType.solid()) {
                return Collections.emptyList();
            }
            return base.getQuads(state, side, rand, ModelData.EMPTY, renderType);
        }
        try {
            BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(mimic);
            return model.getQuads(mimic, side, rand, ModelData.EMPTY, renderType);
        } catch (Throwable t) {
            return base.getQuads(state, side, rand, ModelData.EMPTY, renderType);
        }
    }

    /** Render in whatever chunk layers the mimic uses (grass cutout overlays etc.). */
    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        BlockState mimic = data.get(MIMIC);
        if (mimic != null && !(mimic.getBlock() instanceof HiddenLightBlock)) {
            try {
                return ItemBlockRenderTypes.getRenderLayers(mimic);
            } catch (Throwable t) {
                return SOLID_ONLY;
            }
        }
        return SOLID_ONLY;
    }

    @Override
    public TextureAtlasSprite getParticleIcon(ModelData data) {
        BlockState mimic = data.get(MIMIC);
        if (mimic != null && !(mimic.getBlock() instanceof HiddenLightBlock)) {
            try {
                return Minecraft.getInstance().getBlockRenderer().getBlockModel(mimic)
                    .getParticleIcon(ModelData.EMPTY);
            } catch (Throwable t) {
                // fall through to base
            }
        }
        return base.getParticleIcon();
    }

    @Override
    public boolean useAmbientOcclusion() {
        return true;
    }

    @Override
    public boolean isGui3d() {
        return base.isGui3d();
    }

    @Override
    public boolean usesBlockLight() {
        return base.usesBlockLight();
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return base.getParticleIcon();
    }

    @Override
    public ItemTransforms getTransforms() {
        return base.getTransforms();
    }

    @Override
    public ItemOverrides getOverrides() {
        return base.getOverrides();
    }
}

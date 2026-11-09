package com.carpentersblocks.forge.client;

import com.carpentersblocks.blockentity.RenderData;
import com.carpentersblocks.client.render.CoverModelBuilder;
import com.carpentersblocks.forge.ForgeCoverableBlockEntity;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.IDynamicBakedModel;
import net.minecraftforge.client.model.data.IModelData;

import java.util.List;
import java.util.Random;

/** Wraps the JSON model of a coverable block; gets its look from the block entity model data. */
public final class ForgeCoverableModel implements IDynamicBakedModel {
    private final BakedModel base;

    public ForgeCoverableModel(BakedModel base) {
        this.base = base;
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, Random rand, IModelData extraData) {
        RenderData data = extraData.getData(ForgeCoverableBlockEntity.RENDER_DATA);
        return CoverModelBuilder.quads(state, side, rand, data, base);
    }

    @Override
    public boolean useAmbientOcclusion() {
        return base.useAmbientOcclusion();
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

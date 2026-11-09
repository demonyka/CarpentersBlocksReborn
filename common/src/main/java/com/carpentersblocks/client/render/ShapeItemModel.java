package com.carpentersblocks.client.render;

import com.carpentersblocks.blockentity.RenderData;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Random;

/**
 * Item model showing a fixed shape (e.g. a wedge for the wedge slope item) with the uncovered
 * texture. Transforms and texture come from the JSON model it wraps.
 */
public final class ShapeItemModel implements BakedModel {
    private final BakedModel base;
    private final BlockState state;

    /** @param state block state whose shape and uncovered textures the item shows */
    public ShapeItemModel(BakedModel base, BlockState state) {
        this.base = base;
        this.state = state;
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, Random rand) {
        return CoverModelBuilder.quads(this.state, side, rand, RenderData.EMPTY, base);
    }

    @Override
    public boolean useAmbientOcclusion() {
        return true;
    }

    @Override
    public boolean isGui3d() {
        return true;
    }

    @Override
    public boolean usesBlockLight() {
        return true;
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
        return ItemOverrides.EMPTY;
    }
}

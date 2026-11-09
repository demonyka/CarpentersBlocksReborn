package com.carpentersblocks.fabric.client;

import com.carpentersblocks.blockentity.RenderData;
import com.carpentersblocks.client.render.BoundCoverModel;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.fabricmc.fabric.api.rendering.data.v1.RenderAttachedBlockView;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

/**
 * Wraps the JSON model of a coverable block. Reads the block entity render attachment and
 * hands a vanilla-style model to the renderer (Indigo, or Indium with Sodium), so lighting
 * and ambient occlusion are the vanilla ones.
 */
public final class FabricCoverableModel implements BakedModel, FabricBakedModel {
    private final BakedModel base;

    public FabricCoverableModel(BakedModel base) {
        this.base = base;
    }

    @Override
    public boolean isVanillaAdapter() {
        return false;
    }

    @Override
    public void emitBlockQuads(BlockAndTintGetter view, BlockState state, BlockPos pos, Supplier<Random> random, RenderContext context) {
        Object attachment = ((RenderAttachedBlockView) view).getBlockEntityRenderAttachment(pos);
        // Geometry always comes from the block (slopes have no JSON geometry); covers are optional.
        RenderData data = attachment instanceof RenderData ? (RenderData) attachment : RenderData.EMPTY;
        context.fallbackConsumer().accept(new BoundCoverModel(base, data));
    }

    @Override
    public void emitItemQuads(ItemStack stack, Supplier<Random> random, RenderContext context) {
        context.fallbackConsumer().accept(base);
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, Random rand) {
        return base.getQuads(state, side, rand);
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

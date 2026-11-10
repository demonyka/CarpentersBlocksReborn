package com.carpentersblocks.forge.client;

import com.carpentersblocks.block.CoverableBlock;
import com.carpentersblocks.blockentity.RenderData;
import com.carpentersblocks.core.attribute.AttributeLocation;
import com.carpentersblocks.forge.ForgeCoverableBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.model.data.IDynamicBakedModel;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.data.ModelDataMap;
import net.minecraftforge.client.model.data.ModelProperty;

import java.util.List;
import java.util.Random;

/**
 * Wraps the JSON model of a coverable block. The look comes from the block entity's model
 * data; {@link #getModelData} adds the surroundings (see {@link ForgeRenderContext}).
 */
public final class ForgeCoverableModel implements IDynamicBakedModel {
    static final ModelProperty<ForgeRenderContext> CONTEXT = new ModelProperty<>();

    private final BakedModel base;

    public ForgeCoverableModel(BakedModel base) {
        this.base = base;
    }

    /** Called on the chunk-building thread once per render layer, right before {@link #getQuads}. */
    @Override
    public IModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, IModelData tileData) {
        if (!(state.getBlock() instanceof CoverableBlock)) {
            return tileData;
        }
        RenderData data = tileData.getData(ForgeCoverableBlockEntity.RENDER_DATA);
        return new ModelDataMap.Builder()
            .withInitial(ForgeCoverableBlockEntity.RENDER_DATA, data)
            .withInitial(CONTEXT, new ForgeRenderContext(level, pos.immutable(), state, data, base))
            .build();
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, Random rand, IModelData extraData) {
        if (state == null || !(state.getBlock() instanceof CoverableBlock)) {
            return base.getQuads(state, side, rand);
        }
        ForgeRenderContext context = extraData.getData(CONTEXT);
        if (context == null || context.state != state) {
            // No surroundings (item-like rendering, block breaking overlay, other renderers).
            context = new ForgeRenderContext(null, BlockPos.ZERO, state, extraData.getData(ForgeCoverableBlockEntity.RENDER_DATA), base);
        }
        return context.quads(side, MinecraftForgeClient.getRenderLayer());
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

    /** Break, run and landing particles show the host cover. */
    @Override
    public TextureAtlasSprite getParticleTexture(IModelData data) {
        RenderData render = data.getData(ForgeCoverableBlockEntity.RENDER_DATA);
        BlockState cover = render == null ? null : render.cover(AttributeLocation.HOST);
        if (cover != null) {
            return Minecraft.getInstance().getBlockRenderer().getBlockModel(cover).getParticleIcon();
        }
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

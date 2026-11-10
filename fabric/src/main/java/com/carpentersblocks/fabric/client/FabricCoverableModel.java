package com.carpentersblocks.fabric.client;

import com.carpentersblocks.block.CoverableBlock;
import com.carpentersblocks.blockentity.RenderData;
import com.carpentersblocks.client.render.BoundCoverModel;
import com.carpentersblocks.client.render.CoverModelBuilder;
import com.carpentersblocks.client.render.FaceCulling;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.fabricmc.fabric.api.rendering.data.v1.RenderAttachedBlockView;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
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
 * emits its quads through the Fabric Rendering API (Indigo, or Indium with Sodium), each in
 * the render layer of its cover, so stained glass and ice covers are translucent.
 */
public final class FabricCoverableModel implements BakedModel, FabricBakedModel {
    private final BakedModel base;
    /** Materials by layer: translucent, cutout, cut-out mipped, solid. Looked up on first use. */
    private static volatile RenderMaterial[] materials;

    public FabricCoverableModel(BakedModel base) {
        this.base = base;
    }

    @Override
    public boolean isVanillaAdapter() {
        return false;
    }

    @Override
    public void emitBlockQuads(BlockAndTintGetter view, BlockState state, BlockPos pos, Supplier<Random> random, RenderContext context) {
        Object attachment = view instanceof RenderAttachedBlockView
            ? ((RenderAttachedBlockView) view).getBlockEntityRenderAttachment(pos) : null;
        // Geometry always comes from the block (slopes have no JSON geometry); covers are optional.
        RenderData data = attachment instanceof RenderData ? (RenderData) attachment : RenderData.EMPTY;
        RenderMaterial[] byLayer = materials();
        if (byLayer == null || !(state.getBlock() instanceof CoverableBlock)) {
            context.fallbackConsumer().accept(new BoundCoverModel(base, data));
            return;
        }
        QuadEmitter emitter = context.getEmitter();
        int hidden = FaceCulling.hiddenSides(view, pos, state, data);
        CoverModelBuilder.build(state, data, base, CoverModelBuilder.PLAIN, hidden, (quad, cull, cover, detail) -> {
            emitter.fromVanilla(quad, byLayer[detail ? 1 : layerIndex(cover)], cull);
            emitter.emit();
        });
    }

    private static int layerIndex(BlockState cover) {
        if (cover == null) {
            return 2; // the frame has cut-out holes
        }
        RenderType layer = ItemBlockRenderTypes.getChunkRenderType(cover);
        return layer == RenderType.translucent() ? 0 : layer == RenderType.cutout() ? 1 : layer == RenderType.cutoutMipped() ? 2 : 3;
    }

    private static RenderMaterial[] materials() {
        RenderMaterial[] found = materials;
        if (found == null) {
            Renderer renderer = RendererAccess.INSTANCE.getRenderer();
            if (renderer == null) {
                return null;
            }
            BlendMode[] modes = { BlendMode.TRANSLUCENT, BlendMode.CUTOUT, BlendMode.CUTOUT_MIPPED, BlendMode.SOLID };
            found = new RenderMaterial[modes.length];
            for (int i = 0; i < modes.length; i++) {
                found[i] = renderer.materialFinder().blendMode(0, modes[i]).find();
            }
            materials = found;
        }
        return found;
    }

    @Override
    public void emitItemQuads(ItemStack stack, Supplier<Random> random, RenderContext context) {
        context.fallbackConsumer().accept(base);
    }

    /**
     * Used by renderers without Fabric Rendering API support (e.g. Sodium without Indium):
     * they cannot pass the block entity data, so the shape is drawn without covers rather
     * than not at all.
     */
    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, Random rand) {
        return CoverModelBuilder.quads(state, side, rand, RenderData.EMPTY, base);
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

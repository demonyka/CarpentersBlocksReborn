package com.carpentersblocks.forge.client;

import com.carpentersblocks.blockentity.RenderData;
import com.carpentersblocks.client.render.CoverModelBuilder;
import com.carpentersblocks.client.render.FaceCulling;
import com.carpentersblocks.client.render.FacadeLevel;
import com.carpentersblocks.core.attribute.AttributeLocation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.EmptyModelData;
import net.minecraftforge.client.model.data.IModelData;

import java.util.ArrayList;
import java.util.List;

/**
 * One block being drawn in one render layer: its look plus the level around it. The level
 * is what makes hidden-face culling and connected textures possible:
 * <ul>
 * <li>covers are asked for their quads through Forge's model data, with a level in which
 *     covered Carpenter's blocks report their cover ({@link FacadeLevel}), so models of the
 *     CTM mod (Chisel and others) connect across them;</li>
 * <li>with OptiFine installed, each cover quad also goes through OptiFine's connected
 *     textures the same way (see {@link OptiFineConnectedTextures}).</li>
 * </ul>
 * Lives for one {@code getModelData} / {@code getQuads} round on a chunk-building thread.
 */
final class ForgeRenderContext implements CoverModelBuilder.CoverSource {
    /** Layer of the frame (its texture has cut-out holes) and of small fixed parts (no mipmaps). */
    private static final RenderType FRAME_LAYER = RenderType.cutoutMipped(), DETAIL_LAYER = RenderType.cutout();

    final BlockState state;
    private final BlockAndTintGetter level;
    private final BlockPos pos;
    private final RenderData data;
    private final BakedModel base;

    private List<BakedQuad>[] quads;
    private RenderType quadsLayer;
    private final IModelData[] coverData = new IModelData[AttributeLocation.values().length];
    private final FacadeLevel[] facades = new FacadeLevel[AttributeLocation.values().length];

    /** @param level the level being drawn, or null when the surroundings are unknown */
    ForgeRenderContext(BlockAndTintGetter level, BlockPos pos, BlockState state, RenderData data, BakedModel base) {
        this.level = level;
        this.pos = pos;
        this.state = state;
        this.data = data == null ? RenderData.EMPTY : data;
        this.base = base;
    }

    /** @param layer the layer being drawn, or null for all layers at once (items, breaking overlay) */
    List<BakedQuad> quads(Direction side, RenderType layer) {
        if (quads == null || quadsLayer != layer) {
            int hidden = level == null ? 0 : FaceCulling.hiddenSides(level, pos, state, data);
            quads = CoverModelBuilder.collect(state, data, base, this, hidden,
                cover -> layer == null || ItemBlockRenderTypes.canRenderInLayer(cover, layer),
                layer == null || layer == FRAME_LAYER, layer == null || layer == DETAIL_LAYER);
            quadsLayer = layer;
        }
        return quads[side == null ? 6 : side.ordinal()];
    }

    @Override
    public List<BakedQuad> quads(AttributeLocation location, BlockState cover, Direction dir) {
        BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(cover);
        if (level == null) {
            return CoverModelBuilder.facing(model, cover, dir);
        }
        int index = location.ordinal();
        if (facades[index] == null) {
            facades[index] = new FacadeLevel(level, pos, cover);
            coverData[index] = model.getModelData(facades[index], pos, cover, EmptyModelData.INSTANCE);
        }
        IModelData modelData = coverData[index];
        List<BakedQuad> result = model.getQuads(cover, dir, CoverModelBuilder.coverRandom(), modelData);
        if (result.isEmpty()) {
            result = new ArrayList<>(2);
            for (BakedQuad quad : model.getQuads(cover, null, CoverModelBuilder.coverRandom(), modelData)) {
                if (quad.getDirection() == dir) {
                    result.add(quad);
                }
            }
        }
        return OptiFineConnectedTextures.apply(facades[index], cover, pos, result);
    }
}

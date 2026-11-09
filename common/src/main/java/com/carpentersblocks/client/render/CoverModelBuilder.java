package com.carpentersblocks.client.render;

import com.carpentersblocks.block.CoverableBlock;
import com.carpentersblocks.blockentity.RenderData;
import com.carpentersblocks.core.attribute.AttributeLocation;
import com.carpentersblocks.core.geom.Polygon;
import com.carpentersblocks.core.geom.Vec3d;
import com.carpentersblocks.util.Sides;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

/**
 * Builds the quads of a coverable block. Shared by the Forge and Fabric models.
 * <p>
 * Geometry comes from {@link CoverableBlock#getRenderPolygons} (core module), already in
 * vanilla vertex order, so vanilla ambient occlusion, shading and face culling apply.
 * Textures and tint layers come from the cover block's own model for the polygon's direction,
 * so grass, oriented logs and tinted leaves look right, including blocks from other mods.
 * Without a cover, the base model's particle texture (the uncovered frame) is used.
 */
public final class CoverModelBuilder {
    /** Covers are rendered with a fixed seed so random-rotation blocks (stone, sand) do not flicker. */
    private static final long COVER_SEED = 42L;

    private CoverModelBuilder() {
    }

    /**
     * Quads for one cull side ({@code null} = faces that are never culled).
     *
     * @param data the block entity snapshot, or null when there is none (e.g. item rendering)
     * @param base the JSON model of the block, for its uncovered texture
     */
    public static List<BakedQuad> quads(BlockState state, Direction cullSide, Random rand, RenderData data, BakedModel base) {
        if (state == null || !(state.getBlock() instanceof CoverableBlock)) {
            return base.getQuads(state, cullSide, rand);
        }
        CoverableBlock block = (CoverableBlock) state.getBlock();
        RenderData snapshot = data == null ? RenderData.EMPTY : data;
        return quads(block.getRenderPolygons(state, snapshot), cullSide, snapshot,
            uncoveredSprites(base, polygon -> block.uncoveredTexture(state, polygon)),
            polygon -> block.fixedTexture(state, polygon));
    }

    /**
     * Sprite for uncovered faces: the texture chosen by the block, else the base model's
     * particle texture (the uncovered frame).
     */
    public static Function<Polygon, TextureAtlasSprite> uncoveredSprites(BakedModel base, Function<Polygon, ResourceLocation> textures) {
        return polygon -> {
            ResourceLocation texture = textures.apply(polygon);
            if (texture == null) {
                return base.getParticleIcon();
            }
            return sprite(texture);
        };
    }

    private static TextureAtlasSprite sprite(ResourceLocation texture) {
        return Minecraft.getInstance().getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS).getSprite(texture);
    }

    /** Quads for explicit geometry (also used for item models). */
    public static List<BakedQuad> quads(List<Polygon> polygons, Direction cullSide, RenderData data,
                                        Function<Polygon, TextureAtlasSprite> uncovered,
                                        Function<Polygon, ResourceLocation> fixed) {
        List<BakedQuad> out = new ArrayList<>();
        for (Polygon polygon : polygons) {
            Direction cull = polygon.cullFace == null ? null : Sides.toMc(polygon.cullFace);
            if (cull != cullSide) {
                continue;
            }
            ResourceLocation fixedTexture = fixed.apply(polygon);
            if (fixedTexture != null) {
                // Parts that never take a cover: torch flame, lever handle, glass panes...
                out.add(bake(polygon, sprite(fixedTexture), -1));
                continue;
            }
            AttributeLocation location = polygon.coverSide == null
                ? AttributeLocation.HOST : data.faceLocation(AttributeLocation.of(polygon.coverSide));
            BlockState cover = data.cover(location);
            boolean dyed = data.dye(location) != -1;
            if (cover == null) {
                out.add(bake(polygon, uncovered.apply(polygon), dyed ? TintIndex.of(location, -1) : -1));
                continue;
            }
            for (Layer layer : coverLayers(cover, Sides.toMc(polygon.direction))) {
                int tint = layer.tint >= 0 || dyed ? TintIndex.of(location, layer.tint) : -1;
                out.add(bake(polygon, layer.sprite, tint));
            }
        }
        return out;
    }

    /** Vertex layout of Minecraft's BLOCK format: position, colour, uv, light, normal (8 ints). */
    private static BakedQuad bake(Polygon polygon, TextureAtlasSprite sprite, int tint) {
        int[] data = new int[32];
        Vec3d n = polygon.normal;
        int normal = (((int) (n.x * 127)) & 0xFF) | ((((int) (n.y * 127)) & 0xFF) << 8) | ((((int) (n.z * 127)) & 0xFF) << 16);
        for (int i = 0; i < 4; i++) {
            Vec3d v = polygon.vertices[i];
            int o = i * 8;
            data[o] = Float.floatToRawIntBits((float) v.x);
            data[o + 1] = Float.floatToRawIntBits((float) v.y);
            data[o + 2] = Float.floatToRawIntBits((float) v.z);
            data[o + 3] = -1;
            data[o + 4] = Float.floatToRawIntBits(sprite.getU(polygon.uvs[i * 2]));
            data[o + 5] = Float.floatToRawIntBits(sprite.getV(polygon.uvs[i * 2 + 1]));
            data[o + 6] = 0;
            data[o + 7] = normal;
        }
        return new BakedQuad(data, tint, Sides.toMc(polygon.direction), sprite, true);
    }

    /** Texture layers of the cover block on one face (several for grass: dirt + tinted overlay). */
    private static List<Layer> coverLayers(BlockState cover, Direction dir) {
        BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(cover);
        List<Layer> layers = new ArrayList<>(2);
        for (BakedQuad quad : model.getQuads(cover, dir, new Random(COVER_SEED))) {
            layers.add(new Layer(quad.sprite, quad.isTinted() ? quad.getTintIndex() : -1));
        }
        if (layers.isEmpty()) {
            // Models with only inner elements (no cull faces): use quads facing this way.
            for (BakedQuad quad : model.getQuads(cover, null, new Random(COVER_SEED))) {
                if (quad.getDirection() == dir) {
                    layers.add(new Layer(quad.sprite, quad.isTinted() ? quad.getTintIndex() : -1));
                }
            }
        }
        if (layers.isEmpty()) {
            return Collections.singletonList(new Layer(model.getParticleIcon(), -1));
        }
        return layers;
    }

    private static final class Layer {
        final TextureAtlasSprite sprite;
        final int tint;

        Layer(TextureAtlasSprite sprite, int tint) {
            this.sprite = sprite;
            this.tint = tint;
        }
    }
}

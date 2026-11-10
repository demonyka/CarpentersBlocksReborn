package com.carpentersblocks.client.render;

import com.carpentersblocks.block.CoverableBlock;
import com.carpentersblocks.blockentity.RenderData;
import com.carpentersblocks.core.attribute.AttributeLocation;
import com.carpentersblocks.core.geom.Polygon;
import com.carpentersblocks.core.geom.Polygons;
import com.carpentersblocks.core.geom.Vec3d;
import com.carpentersblocks.util.Sides;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Builds the quads of a coverable block. Shared by the Forge and Fabric models.
 * <p>
 * Geometry comes from {@link CoverableBlock#getRenderPolygons} (core module), already in
 * vanilla vertex order, so vanilla ambient occlusion and shading apply. The look comes from
 * the cover block's own quads for the face's direction: each cover quad is projected onto
 * our geometry with its own texture coordinates and tint. So grass, oriented logs, rotated
 * textures and connected textures (which pick another sprite or split a side into quarters)
 * look as they do on the real block, including blocks from other mods.
 * Without a cover, the base model's particle texture (the uncovered frame) is used.
 */
public final class CoverModelBuilder {
    /** Covers are rendered with a fixed seed so random-rotation blocks (stone, sand) do not flicker. */
    private static final long COVER_SEED = 42L;
    private static final float FULL = 16.0F, EPS = 1.0E-3F;

    /** Supplies the quads a cover block shows on one side. */
    public interface CoverSource {
        /**
         * @param location which cover of the block this is (host or a side cover)
         * @return the cover's quads facing {@code dir}; never null
         */
        List<BakedQuad> quads(AttributeLocation location, BlockState cover, Direction dir);
    }

    /** Receives the finished quads. */
    public interface Sink {
        /**
         * @param cullFace side on which a neighbour hides the quad, or null
         * @param cover    the cover block the quad shows, or null for the frame and fixed parts
         */
        void accept(BakedQuad quad, Direction cullFace, BlockState cover);
    }

    /** The cover's plain model quads, with no knowledge of the world around the block. */
    public static final CoverSource PLAIN = (location, cover, dir) ->
        facing(Minecraft.getInstance().getBlockRenderer().getBlockModel(cover), cover, dir);

    private CoverModelBuilder() {
    }

    /**
     * Emits every quad of the block.
     *
     * @param data        the block entity snapshot, or null when there is none (item rendering)
     * @param base        the JSON model of the block, for its uncovered texture
     * @param hiddenSides bit per {@link Direction} ordinal: a neighbour hides that side
     *                    (see {@link FaceCulling}); only used for blocks that are not opaque
     */
    public static void build(BlockState state, RenderData data, BakedModel base, CoverSource source, int hiddenSides, Sink sink) {
        CoverableBlock block = (CoverableBlock) state.getBlock();
        RenderData snapshot = data == null ? RenderData.EMPTY : data;
        // An opaque block has a real occlusion shape, so vanilla culls its faces correctly.
        // Otherwise the shape is empty and vanilla would hide our faces next to any solid
        // neighbour, even a slab: those faces are emitted unculled and hidden by FaceCulling.
        boolean opaque = FaceCulling.isOpaque(state);
        for (Polygon polygon : block.getRenderPolygons(state, snapshot)) {
            Direction cull = polygon.cullFace == null ? null : Sides.toMc(polygon.cullFace);
            if (!opaque && cull != null) {
                if ((hiddenSides & 1 << cull.ordinal()) != 0) {
                    continue;
                }
                cull = null;
            }
            ResourceLocation fixedTexture = block.fixedTexture(state, polygon);
            if (fixedTexture != null) {
                // Parts that never take a cover: torch flame, lever handle, glass panes...
                sink.accept(bake(polygon, sprite(fixedTexture), TintIndex.NONE), cull, null);
                continue;
            }
            AttributeLocation location = polygon.coverSide == null
                ? AttributeLocation.HOST : snapshot.faceLocation(AttributeLocation.of(polygon.coverSide));
            BlockState cover = snapshot.cover(location);
            boolean dyed = snapshot.dye(location) != -1;
            if (cover == null) {
                ResourceLocation texture = block.uncoveredTexture(state, polygon);
                TextureAtlasSprite sprite = texture == null ? base.getParticleIcon() : sprite(texture);
                sink.accept(bake(polygon, sprite, dyed ? TintIndex.of(location, -1) : TintIndex.NONE), cull, null);
                continue;
            }
            Direction dir = Sides.toMc(polygon.direction);
            List<BakedQuad> coverQuads;
            try {
                coverQuads = source.quads(location, cover, dir);
            } catch (RuntimeException e) {
                coverQuads = PLAIN.quads(location, cover, dir); // a foreign model that needs its own block around it
            }
            if (coverQuads.isEmpty()) {
                TextureAtlasSprite particle = Minecraft.getInstance().getBlockRenderer().getBlockModel(cover).getParticleIcon();
                sink.accept(bake(polygon, particle, dyed ? TintIndex.of(location, -1) : TintIndex.NONE), cull, cover);
                continue;
            }
            for (BakedQuad coverQuad : coverQuads) {
                int coverTint = coverQuad.isTinted() ? coverQuad.getTintIndex() : -1;
                int tint = coverTint >= 0 || dyed ? TintIndex.of(location, coverTint) : TintIndex.NONE;
                project(polygon, coverQuad, tint, cull, cover, sink);
            }
        }
    }

    /** All quads split by cull side: index = {@link Direction} ordinal, 6 = never culled. */
    public static List<BakedQuad>[] collect(BlockState state, RenderData data, BakedModel base, CoverSource source, int hiddenSides,
                                            java.util.function.Predicate<BlockState> coverFilter, boolean ownParts) {
        @SuppressWarnings("unchecked")
        List<BakedQuad>[] lists = new List[7];
        for (int i = 0; i < lists.length; i++) {
            lists[i] = new ArrayList<>();
        }
        build(state, data, base, source, hiddenSides, (quad, cull, cover) -> {
            if (cover == null ? ownParts : coverFilter.test(cover)) {
                lists[cull == null ? 6 : cull.ordinal()].add(quad);
            }
        });
        return lists;
    }

    /** Quads for one cull side with plain covers and no culling context (fallback renderers, items). */
    public static List<BakedQuad> quads(BlockState state, Direction cullSide, Random rand, RenderData data, BakedModel base) {
        if (state == null || !(state.getBlock() instanceof CoverableBlock)) {
            return base.getQuads(state, cullSide, rand);
        }
        return collect(state, data, base, PLAIN, 0, cover -> true, true)[cullSide == null ? 6 : cullSide.ordinal()];
    }

    // ---- cover quads ----------------------------------------------------------------------

    /** The quads of a model that face {@code dir}: the culled list, or matching unculled ones. */
    public static List<BakedQuad> facing(BakedModel model, BlockState state, Direction dir) {
        List<BakedQuad> quads = model.getQuads(state, dir, new Random(COVER_SEED));
        if (!quads.isEmpty()) {
            return quads;
        }
        // Models with only inner elements (no cull faces): use quads facing this way.
        List<BakedQuad> inner = new ArrayList<>(2);
        for (BakedQuad quad : model.getQuads(state, null, new Random(COVER_SEED))) {
            if (quad.getDirection() == dir) {
                inner.add(quad);
            }
        }
        return inner.isEmpty() ? Collections.emptyList() : inner;
    }

    public static Random coverRandom() {
        return new Random(COVER_SEED);
    }

    /**
     * Draws the part of {@code polygon} that the cover quad covers, with the cover quad's
     * texture mapping carried over: both are expressed in the world-aligned texture space of
     * the face ({@link Polygons#uv}), where the cover quad's mapping to the atlas is affine.
     */
    private static void project(Polygon polygon, BakedQuad coverQuad, int tint, Direction cull, BlockState cover, Sink sink) {
        int[] v = coverQuad.getVertices();
        int stride = v.length / 4;
        float[] s = new float[4], t = new float[4], au = new float[4], av = new float[4];
        float minS = Float.MAX_VALUE, minT = Float.MAX_VALUE, maxS = -Float.MAX_VALUE, maxT = -Float.MAX_VALUE;
        for (int i = 0; i < 4; i++) {
            int o = i * stride;
            float[] st = Polygons.uv(polygon.direction, new Vec3d(Float.intBitsToFloat(v[o]),
                Float.intBitsToFloat(v[o + 1]), Float.intBitsToFloat(v[o + 2])));
            s[i] = st[0];
            t[i] = st[1];
            au[i] = Float.intBitsToFloat(v[o + 4]);
            av[i] = Float.intBitsToFloat(v[o + 5]);
            minS = Math.min(minS, s[i]);
            maxS = Math.max(maxS, s[i]);
            minT = Math.min(minT, t[i]);
            maxT = Math.max(maxT, t[i]);
        }
        // Affine map (s, t) -> atlas (u, v) from vertex 0 and the two edges with the largest determinant.
        float[] map = null;
        float bestDet = 1.0E-4F;
        for (int a = 1; a < 4; a++) {
            for (int b = a + 1; b < 4; b++) {
                float s1 = s[a] - s[0], t1 = t[a] - t[0], s2 = s[b] - s[0], t2 = t[b] - t[0];
                float det = s1 * t2 - s2 * t1;
                if (Math.abs(det) > bestDet) {
                    bestDet = Math.abs(det);
                    float u1 = au[a] - au[0], u2 = au[b] - au[0], w1 = av[a] - av[0], w2 = av[b] - av[0];
                    map = new float[] {
                        (u1 * t2 - u2 * t1) / det, (u2 * s1 - u1 * s2) / det,
                        (w1 * t2 - w2 * t1) / det, (w2 * s1 - w1 * s2) / det,
                    };
                }
            }
        }
        TextureAtlasSprite sprite = coverQuad.sprite;
        if (map == null) {
            sink.accept(bake(polygon, sprite, tint), cull, cover); // degenerate cover quad: whole sprite
            return;
        }
        boolean wholeSide = minS <= EPS && minT <= EPS && maxS >= FULL - EPS && maxT >= FULL - EPS;
        List<Polygon> pieces = wholeSide || !Polygons.hasWorldUvs(polygon)
            ? Collections.singletonList(polygon) : Polygons.clip(polygon, minS, minT, maxS, maxT);
        for (Polygon piece : pieces) {
            float[] uvs = wholeSide ? wrapped(piece.uvs) : piece.uvs;
            float[] atlas = new float[8];
            for (int i = 0; i < 4; i++) {
                float ds = uvs[i * 2] - s[0], dt = uvs[i * 2 + 1] - t[0];
                atlas[i * 2] = au[0] + map[0] * ds + map[1] * dt;
                atlas[i * 2 + 1] = av[0] + map[2] * ds + map[3] * dt;
            }
            sink.accept(bake(piece, atlas, sprite, tint), cull, cover);
        }
    }

    /**
     * Texture coordinates moved by whole blocks into 0..16, for parts that stick out of the
     * block (door handles): otherwise they would sample the neighbouring atlas sprite.
     */
    private static float[] wrapped(float[] uvs) {
        float minU = Float.MAX_VALUE, minV = Float.MAX_VALUE, maxU = -Float.MAX_VALUE, maxV = -Float.MAX_VALUE;
        for (int i = 0; i < 4; i++) {
            minU = Math.min(minU, uvs[i * 2]);
            maxU = Math.max(maxU, uvs[i * 2]);
            minV = Math.min(minV, uvs[i * 2 + 1]);
            maxV = Math.max(maxV, uvs[i * 2 + 1]);
        }
        if (minU >= -EPS && minV >= -EPS && maxU <= FULL + EPS && maxV <= FULL + EPS) {
            return uvs;
        }
        float shiftU = (float) Math.floor((minU + EPS) / FULL) * FULL, shiftV = (float) Math.floor((minV + EPS) / FULL) * FULL;
        float[] out = new float[8];
        for (int i = 0; i < 4; i++) {
            out[i * 2] = Math.max(0, Math.min(FULL, uvs[i * 2] - shiftU));
            out[i * 2 + 1] = Math.max(0, Math.min(FULL, uvs[i * 2 + 1] - shiftV));
        }
        return out;
    }

    // ---- baking ---------------------------------------------------------------------------

    private static TextureAtlasSprite sprite(ResourceLocation texture) {
        return Minecraft.getInstance().getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS).getSprite(texture);
    }

    /** A face showing the whole sprite, world-aligned. */
    private static BakedQuad bake(Polygon polygon, TextureAtlasSprite sprite, int tint) {
        float[] uvs = wrapped(polygon.uvs);
        float[] atlas = new float[8];
        for (int i = 0; i < 4; i++) {
            atlas[i * 2] = sprite.getU(uvs[i * 2]);
            atlas[i * 2 + 1] = sprite.getV(uvs[i * 2 + 1]);
        }
        return bake(polygon, atlas, sprite, tint);
    }

    /** Vertex layout of Minecraft's BLOCK format: position, colour, uv, light, normal (8 ints). */
    private static BakedQuad bake(Polygon polygon, float[] atlasUvs, TextureAtlasSprite sprite, int tint) {
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
            data[o + 4] = Float.floatToRawIntBits(atlasUvs[i * 2]);
            data[o + 5] = Float.floatToRawIntBits(atlasUvs[i * 2 + 1]);
            data[o + 6] = 0;
            data[o + 7] = normal;
        }
        return new BakedQuad(data, tint, Sides.toMc(polygon.direction), sprite, true);
    }
}

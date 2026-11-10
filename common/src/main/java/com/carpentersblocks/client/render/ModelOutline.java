package com.carpentersblocks.client.render;

import com.carpentersblocks.block.CoverableBlock;
import com.carpentersblocks.blockentity.CoverableBlockEntity;
import com.carpentersblocks.blockentity.RenderData;
import com.carpentersblocks.core.geom.Polygon;
import com.carpentersblocks.core.geom.Vec3d;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The selection outline of blocks whose click shape is only an approximation of what is drawn:
 * a slope's shape is a staircase of small boxes, and outlining that looks like stairs. For
 * those blocks the outline follows the edges of the drawn faces instead.
 */
public final class ModelOutline {
    private ModelOutline() {
    }

    /**
     * Draws the outline if the block wants one (see {@link CoverableBlock#hasModelOutline}).
     *
     * @param lines buffer of the vanilla lines render type
     * @return false if the vanilla outline should be drawn instead
     */
    public static boolean render(PoseStack pose, VertexConsumer lines, BlockGetter level, BlockPos pos, BlockState state,
                                 double cameraX, double cameraY, double cameraZ) {
        if (!(state.getBlock() instanceof CoverableBlock) || !((CoverableBlock) state.getBlock()).hasModelOutline(state)) {
            return false;
        }
        BlockEntity be = level.getBlockEntity(pos);
        RenderData data = be instanceof CoverableBlockEntity ? ((CoverableBlockEntity) be).getRenderData() : RenderData.EMPTY;
        Matrix4f matrix = pose.last().pose();
        float x = (float) (pos.getX() - cameraX), y = (float) (pos.getY() - cameraY), z = (float) (pos.getZ() - cameraZ);
        for (Vec3d[] edge : edges(((CoverableBlock) state.getBlock()).getRenderPolygons(state, data))) {
            for (Vec3d end : edge) {
                lines.vertex(matrix, x + (float) end.x, y + (float) end.y, z + (float) end.z).color(0.0F, 0.0F, 0.0F, 0.4F).endVertex();
            }
        }
        return true;
    }

    /** Edges of the faces, once each, without the seams between faces lying in one plane. */
    static List<Vec3d[]> edges(List<Polygon> polygons) {
        Map<String, Vec3d[]> ends = new HashMap<>();
        Map<String, List<Vec3d>> normals = new HashMap<>();
        for (Polygon polygon : polygons) {
            for (int i = 0; i < 4; i++) {
                Vec3d a = polygon.vertices[i], b = polygon.vertices[(i + 1) % 4];
                if (a.sub(b).length() < 1e-6) {
                    continue; // a triangle repeats one corner
                }
                String ka = key(a), kb = key(b);
                String key = ka.compareTo(kb) < 0 ? ka + "|" + kb : kb + "|" + ka;
                ends.putIfAbsent(key, new Vec3d[] { a, b });
                normals.computeIfAbsent(key, k -> new ArrayList<>(2)).add(polygon.normal);
            }
        }
        List<Vec3d[]> out = new ArrayList<>(ends.size());
        for (Map.Entry<String, Vec3d[]> entry : ends.entrySet()) {
            List<Vec3d> around = normals.get(entry.getKey());
            boolean flat = around.size() == 2 && around.get(0).dot(around.get(1)) > 0.999;
            if (!flat) {
                out.add(entry.getValue());
            }
        }
        return out;
    }

    private static String key(Vec3d v) {
        return Math.round(v.x * 4096) + "," + Math.round(v.y * 4096) + "," + Math.round(v.z * 4096);
    }
}

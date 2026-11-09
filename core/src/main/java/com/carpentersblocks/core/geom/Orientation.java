package com.carpentersblocks.core.geom;

import com.carpentersblocks.core.Side;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * Rotations used to re-orient models designed in one canonical frame. All are proper
 * rotations (no mirroring), so face winding and normals stay correct.
 */
public final class Orientation {
    private Orientation() {
    }

    /** Quarter turns clockwise seen from above (north goes to east). */
    public static Vec3d rotateY(Vec3d p, int quarterTurns) {
        for (int i = 0; i < ((quarterTurns % 4) + 4) % 4; i++) {
            p = new Vec3d(1 - p.z, p.y, p.x);
        }
        return p;
    }

    /**
     * Maps a point of a model designed as if attached to the floor (support below) to a model
     * attached to {@code support}. On a wall, the canonical north (low z) points up.
     */
    public static Vec3d attach(Vec3d p, Side support) {
        switch (support) {
            case DOWN:  return p;
            case UP:    return new Vec3d(p.x, 1 - p.y, 1 - p.z);   // half turn around x
            case NORTH: return new Vec3d(p.x, 1 - p.z, p.y);       // quarter turn around x
            case EAST:  return rotateY(attach(p, Side.NORTH), 1);
            case SOUTH: return rotateY(attach(p, Side.NORTH), 2);
            case WEST:  return rotateY(attach(p, Side.NORTH), 3);
            default: throw new IllegalArgumentException(String.valueOf(support));
        }
    }

    /** Quarter turns that bring a model facing north to face {@code facing} (horizontal). */
    public static int turnsFromNorth(Side facing) {
        switch (facing) {
            case NORTH: return 0;
            case EAST:  return 1;
            case SOUTH: return 2;
            case WEST:  return 3;
            default: throw new IllegalArgumentException("Not horizontal: " + facing);
        }
    }

    /** Applies a rotation to a box (its two corners, re-sorted). */
    public static Box box(Box b, UnaryOperator<Vec3d> rotation) {
        Vec3d p = rotation.apply(new Vec3d(b.minX, b.minY, b.minZ));
        Vec3d q = rotation.apply(new Vec3d(b.maxX, b.maxY, b.maxZ));
        return new Box(Math.min(p.x, q.x), Math.min(p.y, q.y), Math.min(p.z, q.z),
            Math.max(p.x, q.x), Math.max(p.y, q.y), Math.max(p.z, q.z));
    }

    /**
     * Applies a rotation to faces built in another frame, keeping their texture coordinates
     * (the texture turns with the part). Cull and cover sides are dropped: rotated
     * free-standing parts are never culled.
     */
    public static List<Polygon> polygons(List<Polygon> in, UnaryOperator<Vec3d> rotation) {
        List<Polygon> out = new ArrayList<>(in.size());
        for (Polygon p : in) {
            Vec3d[] turned = new Vec3d[4];
            for (int i = 0; i < 4; i++) {
                turned[i] = rotation.apply(p.vertices[i]);
            }
            Vec3d origin = rotation.apply(p.vertices[0]);
            Vec3d normal = rotation.apply(p.vertices[0].add(p.normal)).sub(origin).normalize();
            out.add(new Polygon(turned, normal, Polygons.dominant(normal), null, null, p.uvs, p.part));
        }
        return out;
    }
}

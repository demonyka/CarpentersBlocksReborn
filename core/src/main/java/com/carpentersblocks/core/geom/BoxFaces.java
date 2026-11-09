package com.carpentersblocks.core.geom;

import com.carpentersblocks.core.Side;

/**
 * Face data for box-shaped parts, matching Minecraft's JSON block model rules so covers line
 * up with neighbouring vanilla blocks and with each other.
 */
public final class BoxFaces {
    private BoxFaces() {
    }

    /**
     * Texture coordinates of a box face in pixels (0..16), as {u1, v1, u2, v2}.
     * Same table as Minecraft's default element UVs: world-aligned, so a half slab shows
     * the matching half of the texture.
     */
    public static float[] uv(Box box, Side side) {
        float x1 = px(box.minX), y1 = px(box.minY), z1 = px(box.minZ);
        float x2 = px(box.maxX), y2 = px(box.maxY), z2 = px(box.maxZ);
        switch (side) {
            case DOWN:  return new float[] { x1, 16 - z2, x2, 16 - z1 };
            case UP:    return new float[] { x1, z1, x2, z2 };
            case NORTH: return new float[] { 16 - x2, 16 - y2, 16 - x1, 16 - y1 };
            case SOUTH: return new float[] { x1, 16 - y2, x2, 16 - y1 };
            case WEST:  return new float[] { z1, 16 - y2, z2, 16 - y1 };
            case EAST:  return new float[] { 16 - z2, 16 - y2, 16 - z1, 16 - y1 };
            default: throw new IllegalArgumentException(String.valueOf(side));
        }
    }

    /** Whether this face lies on the block boundary, so a solid neighbour can hide (cull) it. */
    public static boolean onBoundary(Box box, Side side) {
        switch (side) {
            case DOWN:  return box.minY == 0;
            case UP:    return box.maxY == 1;
            case NORTH: return box.minZ == 0;
            case SOUTH: return box.maxZ == 1;
            case WEST:  return box.minX == 0;
            case EAST:  return box.maxX == 1;
            default: throw new IllegalArgumentException(String.valueOf(side));
        }
    }

    /** Whether the face has any area (a zero-thickness box has two empty faces per axis). */
    public static boolean hasArea(Box box, Side side) {
        switch (side) {
            case DOWN: case UP:       return box.maxX > box.minX && box.maxZ > box.minZ;
            case NORTH: case SOUTH:   return box.maxX > box.minX && box.maxY > box.minY;
            case WEST: case EAST:     return box.maxZ > box.minZ && box.maxY > box.minY;
            default: throw new IllegalArgumentException(String.valueOf(side));
        }
    }

    private static float px(double v) {
        return (float) (v * 16.0);
    }
}

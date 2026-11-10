package com.carpentersblocks.core.geom;

import com.carpentersblocks.core.Side;

/**
 * One renderable face, ready to become a Minecraft quad.
 * <p>
 * Always 4 vertices (a triangle repeats one vertex), ordered like Minecraft's own model faces
 * for {@link #direction}, because vanilla ambient occlusion assigns corner values by vertex
 * index. Build instances with {@link Polygons#build}.
 */
public final class Polygon {
    /** Exactly 4 vertices in vanilla slot order for {@link #direction}. */
    public final Vec3d[] vertices;
    /** Outward unit normal. */
    public final Vec3d normal;
    /** Axis direction used for shading, ambient occlusion and choosing the cover texture. */
    public final Side direction;
    /** Side a solid neighbour hides this face on, or {@code null} if it is never culled. */
    public final Side cullFace;
    /**
     * Block side this face belongs to for side covers, or {@code null} for faces that always
     * use the host cover (sloped faces).
     */
    public final Side coverSide;
    /** Texture coordinates in pixels (0..16): u0, v0, u1, v1, ... */
    public final float[] uvs;
    /**
     * Which part of the block this face belongs to, for blocks whose parts use fixed textures
     * (torch flame, lever handle, glass panes). 0 = the coverable body.
     */
    public final int part;

    Polygon(Vec3d[] vertices, Vec3d normal, Side direction, Side cullFace, Side coverSide, float[] uvs, int part) {
        this.part = part;
        this.vertices = vertices;
        this.normal = normal;
        this.direction = direction;
        this.cullFace = cullFace;
        this.coverSide = coverSide;
        this.uvs = uvs;
    }

    /** The same face with other texture coordinates (pixels: u0, v0, u1, v1, ...). */
    public Polygon withUvs(float[] newUvs) {
        return new Polygon(vertices, normal, direction, cullFace, coverSide, newUvs, part);
    }

    /** Area of the face (block face = 1). */
    public double area() {
        Vec3d a = vertices[0], b = vertices[1], c = vertices[2], d = vertices[3];
        return 0.5 * (b.sub(a).cross(c.sub(a)).length() + c.sub(a).cross(d.sub(a)).length());
    }
}

package com.carpentersblocks.core.geom;

import com.carpentersblocks.core.Side;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Builds {@link Polygon}s: vertex ordering, direction choice and texture coordinates. */
public final class Polygons {
    /**
     * Corner of the face's bounding rectangle each vanilla vertex slot sits on, per direction,
     * as {sMax?, tMax?} in the 2D projection used by {@link #project}. Taken from Minecraft's
     * FaceInfo table (the order block model faces are baked in).
     */
    private static final boolean[][][] SLOTS = {
        /* DOWN  (x,z) */ { { false, true }, { false, false }, { true, false }, { true, true } },
        /* UP    (x,z) */ { { false, false }, { false, true }, { true, true }, { true, false } },
        /* NORTH (x,y) */ { { true, true }, { true, false }, { false, false }, { false, true } },
        /* SOUTH (x,y) */ { { false, true }, { false, false }, { true, false }, { true, true } },
        /* WEST  (z,y) */ { { false, true }, { false, false }, { true, false }, { true, true } },
        /* EAST  (z,y) */ { { true, true }, { true, false }, { false, false }, { false, true } },
    };

    private Polygons() {
    }

    /** The six faces of a box, culled on the block boundary like a JSON model element. */
    public static List<Polygon> ofBox(Box box) {
        return ofBox(box, 0);
    }

    /** Box faces tagged with a part id (see {@link Polygon#part}). */
    public static List<Polygon> ofBox(Box box, int part) {
        List<Polygon> out = new ArrayList<>(6);
        for (Side side : Side.values()) {
            if (!BoxFaces.hasArea(box, side)) {
                continue;
            }
            Vec3d[] corners = boxFace(box, side);
            Side cull = BoxFaces.onBoundary(box, side) ? side : null;
            out.add(build(Arrays.asList(corners), cull, side, part));
        }
        return out;
    }

    /**
     * Faces of a box rotated by {@code degrees} around an axis ('x', 'y' or 'z') through
     * {@code pivot} (lever handles, wall torches). Never culled.
     */
    public static List<Polygon> ofRotatedBox(Box box, char axis, double degrees, Vec3d pivot, int part) {
        double rad = Math.toRadians(degrees), cos = Math.cos(rad), sin = Math.sin(rad);
        List<Polygon> out = new ArrayList<>(6);
        for (Side side : Side.values()) {
            if (!BoxFaces.hasArea(box, side)) {
                continue;
            }
            // Texture coordinates come from the unrotated box, so the texture follows the part.
            Polygon flat = build(Arrays.asList(boxFace(box, side)), null, null, part);
            Vec3d[] turned = new Vec3d[4];
            for (int i = 0; i < 4; i++) {
                turned[i] = rotate(flat.vertices[i].sub(pivot), axis, cos, sin).add(pivot);
            }
            Vec3d normal = rotate(flat.normal, axis, cos, sin);
            out.add(new Polygon(turned, normal, dominant(normal), null, null, flat.uvs, part));
        }
        return out;
    }

    private static Vec3d rotate(Vec3d v, char axis, double cos, double sin) {
        switch (axis) {
            case 'x': return new Vec3d(v.x, v.y * cos - v.z * sin, v.y * sin + v.z * cos);
            case 'y': return new Vec3d(v.x * cos + v.z * sin, v.y, -v.x * sin + v.z * cos);
            default:  return new Vec3d(v.x * cos - v.y * sin, v.x * sin + v.y * cos, v.z);
        }
    }

    /**
     * @param ccw       3 or 4 vertices of a convex face, counter-clockwise seen from outside
     * @param cullFace  side that hides the face, or null
     * @param coverSide side whose cover it shows, or null for the host cover
     */
    public static Polygon build(List<Vec3d> ccw, Side cullFace, Side coverSide) {
        return build(ccw, cullFace, coverSide, 0);
    }

    public static Polygon build(List<Vec3d> ccw, Side cullFace, Side coverSide, int part) {
        if (ccw.size() < 3 || ccw.size() > 4) {
            throw new IllegalArgumentException("Faces must have 3 or 4 vertices: " + ccw.size());
        }
        Vec3d normal = normalOf(ccw);
        Side direction = cullFace != null ? cullFace : dominant(normal);
        Vec3d[] ordered = orderForSlots(ccw, direction);
        float[] uvs = new float[8];
        for (int i = 0; i < 4; i++) {
            float[] uv = uv(direction, ordered[i]);
            uvs[i * 2] = uv[0];
            uvs[i * 2 + 1] = uv[1];
        }
        return new Polygon(ordered, normal, direction, cullFace, coverSide, uvs, part);
    }

    /** Outward normal of a counter-clockwise polygon (Newell's method, robust for any vertex count). */
    public static Vec3d normalOf(List<Vec3d> ccw) {
        double nx = 0, ny = 0, nz = 0;
        for (int i = 0; i < ccw.size(); i++) {
            Vec3d a = ccw.get(i), b = ccw.get((i + 1) % ccw.size());
            nx += (a.y - b.y) * (a.z + b.z);
            ny += (a.z - b.z) * (a.x + b.x);
            nz += (a.x - b.x) * (a.y + b.y);
        }
        return new Vec3d(nx, ny, nz).normalize();
    }

    /**
     * Axis direction closest to a normal. Ties between vertical and horizontal go to vertical,
     * so a 45-degree roof shows the cover's top texture (grass, log rings), as 1.7.10 did for
     * grass; ties between X and Z go to X (1.7.10 used east/west for diagonal side wedges).
     */
    public static Side dominant(Vec3d n) {
        double ax = Math.abs(n.x), ay = Math.abs(n.y), az = Math.abs(n.z);
        if (ay >= Math.max(ax, az) - Vec3d.EPS) {
            return n.y > 0 ? Side.UP : Side.DOWN;
        }
        if (ax >= az - Vec3d.EPS) {
            return n.x > 0 ? Side.EAST : Side.WEST;
        }
        return n.z > 0 ? Side.SOUTH : Side.NORTH;
    }

    /**
     * World-aligned texture coordinates of a point, in pixels, for a face looking along
     * {@code side}. Same mapping as Minecraft's default element UVs, so textures continue
     * seamlessly across neighbouring blocks and onto sloped faces.
     */
    public static float[] uv(Side side, Vec3d p) {
        switch (side) {
            case DOWN:  return px(p.x, 1 - p.z);
            case UP:    return px(p.x, p.z);
            case NORTH: return px(1 - p.x, 1 - p.y);
            case SOUTH: return px(p.x, 1 - p.y);
            case WEST:  return px(p.z, 1 - p.y);
            case EAST:  return px(1 - p.z, 1 - p.y);
            default: throw new IllegalArgumentException(String.valueOf(side));
        }
    }

    /** 2D coordinates of a point in the plane of {@code side}: (x,z), (x,y) or (z,y). */
    public static double[] project(Side side, Vec3d p) {
        switch (side) {
            case DOWN: case UP:       return new double[] { p.x, p.z };
            case NORTH: case SOUTH:   return new double[] { p.x, p.y };
            default:                  return new double[] { p.z, p.y };
        }
    }

    /**
     * Puts the vertices into vanilla slot order: each slot gets the vertex closest to its
     * corner of the bounding rectangle, keeping the counter-clockwise order. Triangles repeat
     * one vertex.
     */
    static Vec3d[] orderForSlots(List<Vec3d> ccw, Side direction) {
        List<Vec3d[]> candidates = new ArrayList<>();
        if (ccw.size() == 4) {
            candidates.add(ccw.toArray(new Vec3d[0]));
        } else {
            Vec3d a = ccw.get(0), b = ccw.get(1), c = ccw.get(2);
            candidates.add(new Vec3d[] { a, a, b, c });
            candidates.add(new Vec3d[] { a, b, b, c });
            candidates.add(new Vec3d[] { a, b, c, c });
        }
        double minS = Double.MAX_VALUE, minT = Double.MAX_VALUE, maxS = -Double.MAX_VALUE, maxT = -Double.MAX_VALUE;
        for (Vec3d v : ccw) {
            double[] st = project(direction, v);
            minS = Math.min(minS, st[0]);
            maxS = Math.max(maxS, st[0]);
            minT = Math.min(minT, st[1]);
            maxT = Math.max(maxT, st[1]);
        }
        boolean[][] slots = SLOTS[direction.ordinal()];
        Vec3d[] best = null;
        double bestCost = Double.MAX_VALUE;
        for (Vec3d[] candidate : candidates) {
            for (int rotation = 0; rotation < 4; rotation++) {
                double cost = 0;
                for (int slot = 0; slot < 4; slot++) {
                    double[] st = project(direction, candidate[(slot + rotation) % 4]);
                    double ds = st[0] - (slots[slot][0] ? maxS : minS);
                    double dt = st[1] - (slots[slot][1] ? maxT : minT);
                    cost += ds * ds + dt * dt;
                }
                if (cost < bestCost - Vec3d.EPS) {
                    bestCost = cost;
                    best = new Vec3d[4];
                    for (int slot = 0; slot < 4; slot++) {
                        best[slot] = candidate[(slot + rotation) % 4];
                    }
                }
            }
        }
        return best;
    }

    private static Vec3d[] boxFace(Box b, Side side) {
        double x1 = b.minX, y1 = b.minY, z1 = b.minZ, x2 = b.maxX, y2 = b.maxY, z2 = b.maxZ;
        switch (side) {
            case DOWN:  return new Vec3d[] { v(x1, y1, z2), v(x1, y1, z1), v(x2, y1, z1), v(x2, y1, z2) };
            case UP:    return new Vec3d[] { v(x1, y2, z1), v(x1, y2, z2), v(x2, y2, z2), v(x2, y2, z1) };
            case NORTH: return new Vec3d[] { v(x2, y2, z1), v(x2, y1, z1), v(x1, y1, z1), v(x1, y2, z1) };
            case SOUTH: return new Vec3d[] { v(x1, y2, z2), v(x1, y1, z2), v(x2, y1, z2), v(x2, y2, z2) };
            case WEST:  return new Vec3d[] { v(x1, y2, z1), v(x1, y1, z1), v(x1, y1, z2), v(x1, y2, z2) };
            case EAST:  return new Vec3d[] { v(x2, y2, z2), v(x2, y1, z2), v(x2, y1, z1), v(x2, y2, z1) };
            default: throw new IllegalArgumentException(String.valueOf(side));
        }
    }

    private static Vec3d v(double x, double y, double z) {
        return new Vec3d(x, y, z);
    }

    private static float[] px(double u, double v) {
        return new float[] { (float) (u * 16), (float) (v * 16) };
    }
}

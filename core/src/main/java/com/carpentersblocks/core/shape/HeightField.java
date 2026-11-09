package com.carpentersblocks.core.shape;

import com.carpentersblocks.core.Side;
import com.carpentersblocks.core.geom.Box;
import com.carpentersblocks.core.geom.Polygon;
import com.carpentersblocks.core.geom.Polygons;
import com.carpentersblocks.core.geom.Vec3d;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;

/**
 * A solid that rests on one block face ({@link #base}) and rises to a piecewise-linear
 * height over a 3x3 grid of sample points (corners, edge midpoints, centre). Every
 * Carpenter's slope is one of these: wedges, corners, oblique corners and prisms rise from
 * the bottom or top face, side wedges rise from a vertical face.
 * <p>
 * Between samples the surface is linear on 8 triangles (each quadrant split along the
 * corner-to-centre diagonal), which is exactly where the creases of all slope shapes lie.
 */
public final class HeightField {
    private final Side base;
    /** Heights (0..1, measured from the base) at grid point (i, j), index i * 3 + j. */
    private final double[] h = new double[9];

    private HeightField(Side base) {
        this.base = base;
    }

    /**
     * @param f height (0..1) as a function of a point on the base face; only evaluated at the
     *          9 grid points
     */
    public static HeightField sample(Side base, ToDoubleFunction<Vec3d> f) {
        HeightField field = new HeightField(base);
        for (int i = 0; i <= 2; i++) {
            for (int j = 0; j <= 2; j++) {
                double value = f.applyAsDouble(field.map(i / 2.0, j / 2.0, 0));
                if (value < -Vec3d.EPS || value > 1 + Vec3d.EPS) {
                    throw new IllegalArgumentException("Height out of range: " + value);
                }
                field.h[i * 3 + j] = Math.max(0, Math.min(1, value));
            }
        }
        return field;
    }

    public Side base() {
        return base;
    }

    /** Block-local point for grid coordinates (a, b) on the base and height t above it. */
    public Vec3d map(double a, double b, double t) {
        switch (base) {
            case DOWN:  return new Vec3d(a, t, b);
            case UP:    return new Vec3d(a, 1 - t, b);
            case NORTH: return new Vec3d(a, b, t);
            case SOUTH: return new Vec3d(a, b, 1 - t);
            case WEST:  return new Vec3d(t, a, b);
            case EAST:  return new Vec3d(1 - t, a, b);
            default: throw new IllegalStateException();
        }
    }

    private double h(int i, int j) {
        return h[i * 3 + j];
    }

    private Vec3d grid(int i, int j) {
        return map(i / 2.0, j / 2.0, h(i, j));
    }

    private Vec3d away() {
        Side top = base.opposite();
        return new Vec3d(top.dx, top.dy, top.dz);
    }

    // ---- rendering --------------------------------------------------------------------

    /** All faces, merged into as few convex polygons as possible. */
    public List<Polygon> polygons() {
        Map<String, Group> groups = new LinkedHashMap<>();
        Vec3d away = away();

        // Top surface and base: 8 triangles. The base only exists under triangles that have
        // some height (an oblique exterior corner has a triangular base).
        for (int ci = 0; ci <= 2; ci += 2) {
            for (int cj = 0; cj <= 2; cj += 2) {
                addTop(groups, ci, cj, 1, cj, away);
                addTop(groups, ci, cj, ci, 1, away);
            }
        }

        // Walls: each block side next to the base, in two halves (corner-mid, mid-corner).
        int[][] edges = { { 0, -1 }, { 2, -1 }, { -1, 0 }, { -1, 2 } };
        Vec3d centre = map(0.5, 0.5, 0);
        for (int[] edge : edges) {
            for (int s = 0; s < 2; s++) {
                int i0 = edge[0] >= 0 ? edge[0] : s, j0 = edge[1] >= 0 ? edge[1] : s;
                int i1 = edge[0] >= 0 ? edge[0] : s + 1, j1 = edge[1] >= 0 ? edge[1] : s + 1;
                double h0 = h(i0, j0), h1 = h(i1, j1);
                if (h0 < Vec3d.EPS && h1 < Vec3d.EPS) {
                    continue;
                }
                List<Vec3d> wall = new ArrayList<>(4);
                wall.add(map(i0 / 2.0, j0 / 2.0, 0));
                wall.add(map(i1 / 2.0, j1 / 2.0, 0));
                if (h1 > Vec3d.EPS) {
                    wall.add(grid(i1, j1));
                }
                if (h0 > Vec3d.EPS) {
                    wall.add(grid(i0, j0));
                }
                Vec3d outward = map(i0 / 2.0, j0 / 2.0, 0).add(map(i1 / 2.0, j1 / 2.0, 0)).scale(0.5).sub(centre);
                List<Vec3d> oriented = oriented(wall, outward);
                Side side = Polygons.dominant(Polygons.normalOf(oriented));
                add(groups, oriented, side, side);
            }
        }

        List<Polygon> out = new ArrayList<>();
        for (Group group : groups.values()) {
            group.emit(out);
        }
        return out;
    }

    private void addTop(Map<String, Group> groups, int ci, int cj, int mi, int mj, Vec3d away) {
        double ha = h(ci, cj), hb = h(mi, mj), hc = h(1, 1);
        if (ha < Vec3d.EPS && hb < Vec3d.EPS && hc < Vec3d.EPS) {
            return;
        }
        List<Vec3d> under = oriented(Arrays.asList(map(ci / 2.0, cj / 2.0, 0), map(mi / 2.0, mj / 2.0, 0), map(0.5, 0.5, 0)), away.scale(-1));
        add(groups, under, base, base);
        List<Vec3d> tri = oriented(Arrays.asList(grid(ci, cj), grid(mi, mj), grid(1, 1)), away);
        if (Polygons.normalOf(tri).length() < Vec3d.EPS) {
            return; // degenerate
        }
        boolean onOppositeFace = ha > 1 - Vec3d.EPS && hb > 1 - Vec3d.EPS && hc > 1 - Vec3d.EPS;
        Side top = base.opposite();
        add(groups, tri, onOppositeFace ? top : null, onOppositeFace ? top : null);
    }

    private static List<Vec3d> oriented(List<Vec3d> verts, Vec3d wantedNormal) {
        if (Polygons.normalOf(verts).dot(wantedNormal) < 0) {
            List<Vec3d> reversed = new ArrayList<>(verts);
            Collections.reverse(reversed);
            return reversed;
        }
        return verts;
    }

    private static void add(Map<String, Group> groups, List<Vec3d> ccw, Side cull, Side coverSide) {
        Vec3d n = Polygons.normalOf(ccw);
        double d = n.dot(ccw.get(0));
        String key = Math.round(n.x * 1e4) + "," + Math.round(n.y * 1e4) + "," + Math.round(n.z * 1e4) + "," + Math.round(d * 1e4);
        groups.computeIfAbsent(key, k -> new Group(n, cull, coverSide)).parts.add(ccw);
    }

    /** Coplanar pieces; emitted as one polygon when their union is convex with at most 4 corners. */
    private static final class Group {
        final Vec3d normal;
        final Side cull;
        final Side coverSide;
        final List<List<Vec3d>> parts = new ArrayList<>();

        Group(Vec3d normal, Side cull, Side coverSide) {
            this.normal = normal;
            this.cull = cull;
            this.coverSide = coverSide;
        }

        void emit(List<Polygon> out) {
            List<Vec3d> hull = hull();
            if (hull != null) {
                out.add(Polygons.build(hull, cull, coverSide));
                return;
            }
            for (List<Vec3d> part : parts) {
                out.add(Polygons.build(part, cull, coverSide));
            }
        }

        private List<Vec3d> hull() {
            if (parts.size() == 1) {
                return parts.get(0);
            }
            List<Vec3d> points = new ArrayList<>();
            double area = 0;
            for (List<Vec3d> part : parts) {
                points.addAll(part);
                area += area(part);
            }
            Side axis = Polygons.dominant(normal);
            List<Vec3d> hull = ConvexHull.of(points, axis);
            if (hull.size() < 3 || hull.size() > 4 || Math.abs(area(hull) - area) > 1e-4) {
                return null;
            }
            return oriented(hull, normal);
        }

        private static double area(List<Vec3d> poly) {
            Vec3d a = poly.get(0);
            double sum = 0;
            for (int i = 1; i + 1 < poly.size(); i++) {
                sum += poly.get(i).sub(a).cross(poly.get(i + 1).sub(a)).length() / 2;
            }
            return sum;
        }
    }

    // ---- collision ----------------------------------------------------------------------

    /** Height at grid coordinates (a, b), interpolated linearly on the 8 triangles. */
    public double heightAt(double a, double b) {
        int ci = a < 0.5 ? 0 : 2, cj = b < 0.5 ? 0 : 2;
        double ca = ci / 2.0, cb = cj / 2.0;
        // Distance from the corner along each axis, as a fraction of the half cell.
        double da = Math.abs(a - ca) * 2, db = Math.abs(b - cb) * 2;
        double hc = h(ci, cj), hCentre = h(1, 1);
        if (da >= db) {
            // Triangle (corner, mid on the a-axis side, centre).
            double hm = h(1, cj);
            return hc + (hm - hc) * (da - db) + (hCentre - hc) * db;
        }
        double hm = h(ci, 1);
        return hc + (hm - hc) * (db - da) + (hCentre - hc) * da;
    }

    /**
     * Collision approximation: columns on an {@code steps} x {@code steps} grid, each as tall
     * as the highest point of the surface above it. Never lower than the visible surface, so
     * full faces stay full (torches attach, neighbours cull) and nothing sinks in.
     */
    public List<Box> boxes(int steps) {
        List<Box> boxes = new ArrayList<>();
        for (int i = 0; i < steps; i++) {
            for (int j = 0; j < steps; j++) {
                double a0 = (double) i / steps, a1 = (double) (i + 1) / steps;
                double b0 = (double) j / steps, b1 = (double) (j + 1) / steps;
                double top = Math.max(Math.max(heightAt(a0, b0), heightAt(a1, b0)), Math.max(heightAt(a0, b1), heightAt(a1, b1)));
                double t = Math.ceil(top * 16 - 1e-4) / 16;
                if (t <= 0) {
                    continue;
                }
                Vec3d p = map(a0, b0, 0), q = map(a1, b1, t);
                boxes.add(new Box(Math.min(p.x, q.x), Math.min(p.y, q.y), Math.min(p.z, q.z),
                    Math.max(p.x, q.x), Math.max(p.y, q.y), Math.max(p.z, q.z)));
            }
        }
        return boxes;
    }
}

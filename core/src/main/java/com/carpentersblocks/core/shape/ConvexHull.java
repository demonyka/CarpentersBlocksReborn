package com.carpentersblocks.core.shape;

import com.carpentersblocks.core.Side;
import com.carpentersblocks.core.geom.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** 2D convex hull (monotone chain) of coplanar 3D points, dropping collinear points. */
final class ConvexHull {
    private ConvexHull() {
    }

    static List<Vec3d> of(List<Vec3d> points, Side axis) {
        List<Vec3d> unique = new ArrayList<>();
        for (Vec3d p : points) {
            if (unique.stream().noneMatch(p::near)) {
                unique.add(p);
            }
        }
        unique.sort(Comparator.<Vec3d>comparingDouble(p -> s(axis, p)).thenComparingDouble(p -> t(axis, p)));
        if (unique.size() < 3) {
            return unique;
        }
        List<Vec3d> hull = new ArrayList<>();
        for (int pass = 0; pass < 2; pass++) {
            int start = hull.size();
            for (int k = 0; k < unique.size(); k++) {
                Vec3d p = unique.get(pass == 0 ? k : unique.size() - 1 - k);
                while (hull.size() >= start + 2 && cross(axis, hull.get(hull.size() - 2), hull.get(hull.size() - 1), p) <= 1e-9) {
                    hull.remove(hull.size() - 1);
                }
                hull.add(p);
            }
            hull.remove(hull.size() - 1);
        }
        return hull;
    }

    private static double s(Side axis, Vec3d p) {
        switch (axis) {
            case DOWN: case UP:     return p.x;
            case NORTH: case SOUTH: return p.x;
            default:                return p.z;
        }
    }

    private static double t(Side axis, Vec3d p) {
        switch (axis) {
            case DOWN: case UP: return p.z;
            default:            return p.y;
        }
    }

    private static double cross(Side axis, Vec3d o, Vec3d a, Vec3d b) {
        return (s(axis, a) - s(axis, o)) * (t(axis, b) - t(axis, o)) - (t(axis, a) - t(axis, o)) * (s(axis, b) - s(axis, o));
    }
}

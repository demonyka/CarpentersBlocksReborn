package com.carpentersblocks.core.geom;

/** Minimal immutable 3D vector in block-local coordinates. */
public final class Vec3d {
    public static final double EPS = 1e-6;

    public final double x, y, z;

    public Vec3d(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vec3d sub(Vec3d o) {
        return new Vec3d(x - o.x, y - o.y, z - o.z);
    }

    public Vec3d add(Vec3d o) {
        return new Vec3d(x + o.x, y + o.y, z + o.z);
    }

    public Vec3d scale(double s) {
        return new Vec3d(x * s, y * s, z * s);
    }

    public Vec3d cross(Vec3d o) {
        return new Vec3d(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x);
    }

    public double dot(Vec3d o) {
        return x * o.x + y * o.y + z * o.z;
    }

    public double length() {
        return Math.sqrt(x * x + y * y + z * z);
    }

    public Vec3d normalize() {
        double len = length();
        return len < EPS ? this : scale(1.0 / len);
    }

    public boolean near(Vec3d o) {
        return Math.abs(x - o.x) < EPS && Math.abs(y - o.y) < EPS && Math.abs(z - o.z) < EPS;
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ", " + z + ")";
    }
}

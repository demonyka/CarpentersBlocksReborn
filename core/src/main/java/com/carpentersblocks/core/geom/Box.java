package com.carpentersblocks.core.geom;

/** Axis-aligned box in block-local coordinates (0..1). Immutable. */
public final class Box {
    public static final Box FULL = new Box(0, 0, 0, 1, 1, 1);

    public final double minX, minY, minZ, maxX, maxY, maxZ;

    public Box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        if (minX > maxX || minY > maxY || minZ > maxZ) {
            throw new IllegalArgumentException("Inverted box");
        }
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }

    public boolean isFullCube() {
        return minX == 0 && minY == 0 && minZ == 0 && maxX == 1 && maxY == 1 && maxZ == 1;
    }

    /** Box in pixels (0..16), the way model JSON and 1.7.10 code write them. */
    public static Box px(double x1, double y1, double z1, double x2, double y2, double z2) {
        return new Box(x1 / 16, y1 / 16, z1 / 16, x2 / 16, y2 / 16, z2 / 16);
    }

    /** Quarter turns clockwise seen from above (north goes to east), around the block centre. */
    public Box rotateY(int quarterTurns) {
        Box b = this;
        for (int i = 0; i < ((quarterTurns % 4) + 4) % 4; i++) {
            // (x, z) -> (1 - z, x)
            b = new Box(1 - b.maxZ, b.minY, b.minX, 1 - b.minZ, b.maxY, b.maxX);
        }
        return b;
    }

    /** The box moved by an offset (may leave the unit block, e.g. an open door's handle). */
    public Box move(double dx, double dy, double dz) {
        return new Box(minX + dx, minY + dy, minZ + dz, maxX + dx, maxY + dy, maxZ + dz);
    }

    /** Mirror top to bottom. */
    public Box flipY() {
        return new Box(minX, 1 - maxY, minZ, maxX, 1 - minY, maxZ);
    }

    /** Mirror west to east. */
    public Box flipX() {
        return new Box(1 - maxX, minY, minZ, 1 - minX, maxY, maxZ);
    }

    /**
     * Re-orients a box designed as if attached to the floor (support below, "up" pointing away
     * from it, "north" at the top of a wall view) so it is attached to {@code support} instead.
     */
    public Box attachedTo(com.carpentersblocks.core.Side support) {
        return Orientation.box(this, p -> Orientation.attach(p, support));
    }

    /**
     * Rotates a box designed facing north (front towards -z) to face {@code facing}
     * (a horizontal side).
     */
    public Box facing(com.carpentersblocks.core.Side facing) {
        switch (facing) {
            case NORTH: return this;
            case EAST:  return rotateY(1);
            case SOUTH: return rotateY(2);
            case WEST:  return rotateY(3);
            default: throw new IllegalArgumentException("Not horizontal: " + facing);
        }
    }

    public boolean intersects(Box o) {
        return minX < o.maxX && maxX > o.minX && minY < o.maxY && maxY > o.minY && minZ < o.maxZ && maxZ > o.minZ;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Box)) {
            return false;
        }
        Box b = (Box) o;
        return minX == b.minX && minY == b.minY && minZ == b.minZ
            && maxX == b.maxX && maxY == b.maxY && maxZ == b.maxZ;
    }

    @Override
    public int hashCode() {
        return java.util.Arrays.hashCode(new double[] { minX, minY, minZ, maxX, maxY, maxZ });
    }

    @Override
    public String toString() {
        return "Box[" + minX + "," + minY + "," + minZ + " -> " + maxX + "," + maxY + "," + maxZ + "]";
    }
}

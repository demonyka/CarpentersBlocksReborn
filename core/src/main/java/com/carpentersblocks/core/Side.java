package com.carpentersblocks.core;

/**
 * A face of a block. Order matches Minecraft's {@code Direction} / {@code EnumFacing}
 * (down, up, north, south, west, east) so adapters can convert by ordinal.
 */
public enum Side {
    DOWN(0, -1, 0),
    UP(0, 1, 0),
    NORTH(0, 0, -1),
    SOUTH(0, 0, 1),
    WEST(-1, 0, 0),
    EAST(1, 0, 0);

    private static final Side[] VALUES = values();

    public final int dx;
    public final int dy;
    public final int dz;

    Side(int dx, int dy, int dz) {
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
    }

    public Side opposite() {
        return VALUES[ordinal() ^ 1];
    }

    public static Side byOrdinal(int ordinal) {
        return VALUES[ordinal];
    }

    public String id() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}

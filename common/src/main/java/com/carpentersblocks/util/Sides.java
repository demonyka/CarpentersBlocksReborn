package com.carpentersblocks.util;

import com.carpentersblocks.core.Side;
import com.carpentersblocks.core.geom.Box;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Conversions between core types and Minecraft 1.16.5 types. */
public final class Sides {
    private Sides() {
    }

    public static Side toCore(Direction direction) {
        // Both enums use the order down, up, north, south, west, east.
        return Side.byOrdinal(direction.ordinal());
    }

    public static Direction toMc(Side side) {
        return Direction.from3DDataValue(side.ordinal());
    }

    public static VoxelShape toShape(Box box) {
        return Shapes.box(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
    }
}

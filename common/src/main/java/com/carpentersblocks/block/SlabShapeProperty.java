package com.carpentersblocks.block;

import com.carpentersblocks.core.shape.SlabShape;
import net.minecraft.util.StringRepresentable;

/** {@link SlabShape} as a block state property value. Same order as the core enum. */
public enum SlabShapeProperty implements StringRepresentable {
    FULL, DOWN, UP, NORTH, SOUTH, WEST, EAST;

    private static final SlabShapeProperty[] VALUES = values();

    public SlabShape core() {
        return SlabShape.values()[ordinal()];
    }

    public static SlabShapeProperty of(SlabShape shape) {
        return VALUES[shape.ordinal()];
    }

    @Override
    public String getSerializedName() {
        return core().id();
    }
}

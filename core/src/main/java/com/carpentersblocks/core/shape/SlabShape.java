package com.carpentersblocks.core.shape;

import com.carpentersblocks.core.Side;
import com.carpentersblocks.core.geom.Box;

/**
 * Shape of the Carpenter's Block: a full cube or a half slab resting against one side.
 * The hammer toggles between them (1.7.10 behaviour).
 */
public enum SlabShape {
    FULL(null, Box.FULL),
    DOWN(Side.DOWN, new Box(0, 0, 0, 1, 0.5, 1)),
    UP(Side.UP, new Box(0, 0.5, 0, 1, 1, 1)),
    NORTH(Side.NORTH, new Box(0, 0, 0, 1, 1, 0.5)),
    SOUTH(Side.SOUTH, new Box(0, 0, 0.5, 1, 1, 1)),
    WEST(Side.WEST, new Box(0, 0, 0, 0.5, 1, 1)),
    EAST(Side.EAST, new Box(0.5, 0, 0, 1, 1, 1));

    /** Side the slab rests against, or {@code null} for a full block. */
    public final Side side;
    public final Box bounds;

    SlabShape(Side side, Box bounds) {
        this.side = side;
        this.bounds = bounds;
    }

    public static SlabShape against(Side side) {
        return values()[side.ordinal() + 1];
    }

    /**
     * Hammer action: a full block becomes a slab against the side opposite the clicked face
     * (clicking the top makes a bottom slab); a slab becomes a full block again.
     */
    public SlabShape hammered(Side clickedFace) {
        return this == FULL ? against(clickedFace.opposite()) : FULL;
    }

    /** Whether the given face of this shape fully covers the block face (used for culling and side solidity). */
    public boolean isFaceFull(Side face) {
        return this == FULL || face == side;
    }

    public String id() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}

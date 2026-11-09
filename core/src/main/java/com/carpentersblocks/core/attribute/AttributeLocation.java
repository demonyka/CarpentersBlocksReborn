package com.carpentersblocks.core.attribute;

import com.carpentersblocks.core.Side;

/**
 * Where an attribute lives: on the host (the whole block) or on one side.
 * In 1.7.10 these were indices 0-5 (sides) and 6 (host).
 */
public enum AttributeLocation {
    DOWN, UP, NORTH, SOUTH, WEST, EAST, HOST;

    private static final AttributeLocation[] VALUES = values();

    public boolean isHost() {
        return this == HOST;
    }

    /** @return the side, or {@code null} for {@link #HOST}. */
    public Side side() {
        return this == HOST ? null : Side.byOrdinal(ordinal());
    }

    public static AttributeLocation of(Side side) {
        return VALUES[side.ordinal()];
    }

    public String id() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}

package com.carpentersblocks.client.render;

import com.carpentersblocks.core.attribute.AttributeLocation;

/**
 * Packs "which location" and "which tint layer of the cover" into a quad tint index, so our
 * {@link CoverBlockColor} can find the dye or delegate to the cover block's own colour
 * (grass, leaves, redstone, ...).
 */
public final class TintIndex {
    private static final int NO_COVER_TINT = 7;

    private TintIndex() {
    }

    /** @param coverTint the cover quad's tint index, or -1 if it has none */
    public static int of(AttributeLocation location, int coverTint) {
        int tint = coverTint < 0 || coverTint >= NO_COVER_TINT ? NO_COVER_TINT : coverTint;
        return location.ordinal() * 8 + tint;
    }

    public static AttributeLocation location(int tintIndex) {
        return AttributeLocation.values()[(tintIndex / 8) % AttributeLocation.values().length];
    }

    /** @return the cover tint layer, or -1 if the quad was only tinted for a dye */
    public static int coverTint(int tintIndex) {
        int tint = tintIndex % 8;
        return tint == NO_COVER_TINT ? -1 : tint;
    }
}

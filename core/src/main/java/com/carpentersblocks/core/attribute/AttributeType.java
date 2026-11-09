package com.carpentersblocks.core.attribute;

/** Kinds of decoration a Carpenter's block can hold. */
public enum AttributeType {
    /** A block used as the texture source (host cover or a side cover). */
    COVER("cover", true),
    /** Dye tint applied over a cover. */
    DYE("dye", true),
    /** Overlay such as grass, snow, hay, web, vines or mycelium. */
    OVERLAY("overlay", true),
    /** Chisel design carved into a cover. Stored as a design name, not an item. */
    DESIGN("design", true),
    /** Makes the block emit full light (glowstone dust in 1.7.10). Host only. */
    ILLUMINATOR("illuminator", false);

    public final String id;
    /** Whether this attribute can also exist on individual sides, not just on the host. */
    public final boolean perSide;

    AttributeType(String id, boolean perSide) {
        this.id = id;
        this.perSide = perSide;
    }

    /** Whether removing this attribute gives back an item (designs do not). */
    public boolean dropsItem() {
        return this != DESIGN;
    }
}

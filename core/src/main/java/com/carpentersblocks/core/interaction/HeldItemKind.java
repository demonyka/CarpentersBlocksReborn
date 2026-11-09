package com.carpentersblocks.core.interaction;

/** What the player is holding, as far as Carpenter's blocks care. Classified by the adapter. */
public enum HeldItemKind {
    HAMMER,
    CHISEL,
    /** A block item that may be used as a cover. */
    COVER,
    ILLUMINATOR,
    OVERLAY,
    DYE,
    OTHER
}

package com.carpentersblocks.util;

/** Flags for Level#setBlock / sendBlockUpdated. 1.16.5 mappings do not name them. */
public final class BlockFlags {
    public static final int NEIGHBORS = 1;
    public static final int CLIENTS = 2;
    public static final int ALL = NEIGHBORS | CLIENTS;
    /** Client side: re-render the section on the main thread right away. */
    public static final int RERENDER_NOW = 8;

    private BlockFlags() {
    }
}

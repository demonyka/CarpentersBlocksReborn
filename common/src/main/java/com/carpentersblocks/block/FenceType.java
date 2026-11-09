package com.carpentersblocks.block;

import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/** Styles shared by the Carpenter's Barrier and Gate (1.7.10 types 0-6). */
public enum FenceType implements StringRepresentable {
    VANILLA, VANILLA_X1, VANILLA_X2, VANILLA_X3, PICKET, SHADOWBOX, WALL;

    public boolean isVanilla() {
        return ordinal() <= VANILLA_X3.ordinal();
    }

    /** Rail offset of the vanilla sub-styles, in pixels (1.7.10 lowered the rails by the type). */
    public int railDrop() {
        return isVanilla() && this != VANILLA_X3 ? ordinal() : 0;
    }

    /** Hammer right-click: vanilla family, picket, shadowbox, wall, back to vanilla. */
    public FenceType next() {
        switch (this) {
            case PICKET:    return SHADOWBOX;
            case SHADOWBOX: return WALL;
            case WALL:      return VANILLA;
            default:        return PICKET;
        }
    }

    /** Sneak + hammer right-click: next vanilla sub-style (only within the vanilla family). */
    public FenceType nextVanilla() {
        return isVanilla() ? values()[(ordinal() + 1) % 4] : this;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}

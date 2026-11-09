package com.carpentersblocks.core.attribute;

import java.util.EnumMap;
import java.util.Map;

/**
 * Identifies one attribute slot, e.g. "cover on the host" or "dye on the north side".
 * Instances are interned, so {@code ==} comparison is safe.
 */
public final class AttributeKey {
    private static final Map<AttributeType, AttributeKey[]> KEYS = new EnumMap<>(AttributeType.class);

    static {
        for (AttributeType type : AttributeType.values()) {
            AttributeKey[] keys = new AttributeKey[AttributeLocation.values().length];
            for (AttributeLocation location : AttributeLocation.values()) {
                if (type.perSide || location.isHost()) {
                    keys[location.ordinal()] = new AttributeKey(type, location);
                }
            }
            KEYS.put(type, keys);
        }
    }

    public final AttributeType type;
    public final AttributeLocation location;
    private final String id;

    private AttributeKey(AttributeType type, AttributeLocation location) {
        this.type = type;
        this.location = location;
        this.id = type.id + "." + location.id();
    }

    /**
     * @throws IllegalArgumentException if the type cannot live at that location
     *         (e.g. an illuminator on a side).
     */
    public static AttributeKey of(AttributeType type, AttributeLocation location) {
        AttributeKey key = KEYS.get(type)[location.ordinal()];
        if (key == null) {
            throw new IllegalArgumentException(type + " cannot be placed at " + location);
        }
        return key;
    }

    /** Parses an id produced by {@link #id()}; returns {@code null} for unknown ids. */
    public static AttributeKey parse(String id) {
        int dot = id.indexOf('.');
        if (dot < 0) {
            return null;
        }
        String typeId = id.substring(0, dot);
        String locationId = id.substring(dot + 1);
        for (AttributeType type : AttributeType.values()) {
            if (!type.id.equals(typeId)) {
                continue;
            }
            for (AttributeLocation location : AttributeLocation.values()) {
                if (location.id().equals(locationId)) {
                    return KEYS.get(type)[location.ordinal()];
                }
            }
        }
        return null;
    }

    /** Stable id used as the NBT key, e.g. {@code "cover.host"}. Never change these. */
    public String id() {
        return id;
    }

    @Override
    public String toString() {
        return id;
    }
}

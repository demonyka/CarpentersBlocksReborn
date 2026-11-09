package com.carpentersblocks.core.attribute;

/** Read-only view of which attribute slots are filled. */
@FunctionalInterface
public interface AttributeView {
    boolean has(AttributeKey key);

    default boolean has(AttributeType type, AttributeLocation location) {
        return has(AttributeKey.of(type, location));
    }
}

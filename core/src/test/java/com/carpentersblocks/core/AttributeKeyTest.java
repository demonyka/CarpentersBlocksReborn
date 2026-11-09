package com.carpentersblocks.core;

import com.carpentersblocks.core.attribute.AttributeKey;
import com.carpentersblocks.core.attribute.AttributeLocation;
import com.carpentersblocks.core.attribute.AttributeType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AttributeKeyTest {
    @Test
    void idsRoundTrip() {
        for (AttributeType type : AttributeType.values()) {
            for (AttributeLocation location : AttributeLocation.values()) {
                if (!type.perSide && !location.isHost()) {
                    assertThrows(IllegalArgumentException.class, () -> AttributeKey.of(type, location));
                    continue;
                }
                AttributeKey key = AttributeKey.of(type, location);
                assertSame(key, AttributeKey.parse(key.id()));
            }
        }
    }

    @Test
    void idsAreStable() {
        // These strings are written to world saves; changing them loses data.
        assertEquals("cover.host", AttributeKey.of(AttributeType.COVER, AttributeLocation.HOST).id());
        assertEquals("dye.north", AttributeKey.of(AttributeType.DYE, AttributeLocation.NORTH).id());
        assertEquals("illuminator.host", AttributeKey.of(AttributeType.ILLUMINATOR, AttributeLocation.HOST).id());
    }

    @Test
    void unknownIdsParseToNull() {
        assertNull(AttributeKey.parse("cover"));
        assertNull(AttributeKey.parse("paint.host"));
        assertNull(AttributeKey.parse("illuminator.north"));
    }

    @Test
    void sidesMatchMinecraftOrder() {
        assertEquals(Side.UP, Side.DOWN.opposite());
        assertEquals(Side.SOUTH, Side.NORTH.opposite());
        assertEquals(Side.EAST, Side.WEST.opposite());
        assertEquals(Side.NORTH, AttributeLocation.NORTH.side());
        assertNull(AttributeLocation.HOST.side());
    }
}

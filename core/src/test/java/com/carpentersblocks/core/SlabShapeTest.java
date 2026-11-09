package com.carpentersblocks.core;

import com.carpentersblocks.core.shape.SlabShape;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SlabShapeTest {
    @Test
    void hammerOnTopMakesBottomSlabAndBack() {
        SlabShape slab = SlabShape.FULL.hammered(Side.UP);
        assertEquals(SlabShape.DOWN, slab);
        assertEquals(0.5, slab.bounds.maxY);
        assertEquals(SlabShape.FULL, slab.hammered(Side.NORTH));
    }

    @Test
    void everySideHasItsSlab() {
        for (Side side : Side.values()) {
            SlabShape slab = SlabShape.against(side);
            assertEquals(side, slab.side);
            assertTrue(slab.isFaceFull(side));
            assertFalse(slab.isFaceFull(side.opposite()));
            assertFalse(slab.bounds.isFullCube());
        }
        assertTrue(SlabShape.FULL.bounds.isFullCube());
    }
}

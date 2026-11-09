package com.carpentersblocks.core;

import com.carpentersblocks.core.geom.Box;
import com.carpentersblocks.core.geom.Polygon;
import com.carpentersblocks.core.shape.CollapsibleShape;
import com.carpentersblocks.core.shape.SlopeType;
import com.carpentersblocks.core.shape.StairsShape;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class StairsAndCollapsibleTest {
    private static double volume(List<Box> boxes) {
        return boxes.stream().mapToDouble(b -> (b.maxX - b.minX) * (b.maxY - b.minY) * (b.maxZ - b.minZ)).sum();
    }

    private static Set<Box> set(Box... boxes) {
        return new HashSet<>(Arrays.asList(boxes));
    }

    /** Boxes from 1.7.10 {@code StairsUtil}, as reported for a few shapes. */
    @Test
    void geometryMatches1710() {
        // POS_N: back half full height + bottom slab on the open (north) half.
        assertEquals(0.75, volume(StairsShape.boxes(SlopeType.WEDGE_POS_N.ordinal())), 1e-9);
        assertTrue(StairsShape.boxes(SlopeType.WEDGE_POS_N.ordinal()).stream().anyMatch(b ->
            b.equals(new Box(0, 0, 0, 1, 0.5, 1)) || b.equals(new Box(0, 0, 0.5, 1, 1, 1))));
        // Side SE: missing the x > .5, z > .5 column.
        assertEquals(0.75, volume(StairsShape.boxes(SlopeType.WEDGE_SE.ordinal())), 1e-9);
        assertFalse(StairsShape.boxes(SlopeType.WEDGE_SE.ordinal()).stream().anyMatch(b -> b.intersects(new Box(0.5, 0, 0.5, 1, 1, 1))));
        // Inner corner 7/8, outer corner 5/8.
        assertEquals(0.875, volume(StairsShape.boxes(SlopeType.WEDGE_INT_POS_NW.ordinal())), 1e-9);
        assertEquals(0.625, volume(StairsShape.boxes(SlopeType.WEDGE_EXT_POS_NW.ordinal())), 1e-9);
        // EXT_POS_NW: the top quarter sits at the south-east corner.
        assertTrue(StairsShape.boxes(SlopeType.WEDGE_EXT_POS_NW.ordinal()).stream().anyMatch(b -> b.intersects(new Box(0.5, 0.5, 0.5, 1, 1, 1))));
        assertFalse(StairsShape.boxes(SlopeType.WEDGE_EXT_POS_NW.ordinal()).stream().anyMatch(b -> b.intersects(new Box(0, 0.5, 0, 0.5, 1, 0.5))));
        // Merging keeps it to at most three boxes per shape.
        for (int id = 0; id < StairsShape.COUNT; id++) {
            assertTrue(StairsShape.boxes(id).size() <= 3, "shape " + id);
        }
    }

    @Test
    void hammerCycleAndRotation() {
        assertEquals(SlopeType.WEDGE_POS_N.ordinal(), StairsShape.next(SlopeType.WEDGE_SE.ordinal()));
        assertEquals(0, StairsShape.next(SlopeType.WEDGE_EXT_NEG_NE.ordinal()));
        for (int id = 0; id < StairsShape.COUNT; id++) {
            int t = id;
            for (int i = 0; i < 4; i++) {
                t = StairsShape.rotate(t);
                assertTrue(t < StairsShape.COUNT);
            }
            assertEquals(id, t);
            assertTrue(StairsShape.next(id) < StairsShape.COUNT);
        }
    }

    @Test
    void collapsiblePacksCornersAndBuildsClosedShapes() {
        int packed = CollapsibleShape.pack(0, 16, 8, 4);
        assertEquals(0, CollapsibleShape.corner(packed, CollapsibleShape.NN));
        assertEquals(16, CollapsibleShape.corner(packed, CollapsibleShape.PN));
        assertEquals(8, CollapsibleShape.corner(packed, CollapsibleShape.NP));
        assertEquals(4, CollapsibleShape.corner(packed, CollapsibleShape.PP));
        assertEquals(12, CollapsibleShape.corner(CollapsibleShape.withCorner(packed, CollapsibleShape.PP, 12), CollapsibleShape.PP));
        assertEquals(16, CollapsibleShape.corner(CollapsibleShape.withCorner(packed, CollapsibleShape.NN, 40), CollapsibleShape.NN));

        for (boolean hanging : new boolean[] { false, true }) {
            double volume = 0;
            for (Polygon p : CollapsibleShape.polygons(packed, hanging)) {
                volume += p.area() * p.normal.dot(p.vertices[0]) / 3;
            }
            assertTrue(volume > 0.2 && volume < 0.8, "volume " + volume);
        }
        assertTrue(CollapsibleShape.isFaceFull(CollapsibleShape.FULL, false, Side.UP));
        assertTrue(CollapsibleShape.isFaceFull(packed, false, Side.EAST) == false);
        assertTrue(CollapsibleShape.isFaceFull(CollapsibleShape.pack(0, 16, 0, 16), false, Side.EAST));
        assertTrue(CollapsibleShape.isFaceFull(packed, true, Side.UP), "a hanging block is full on top");
        assertFalse(CollapsibleShape.isFaceFull(packed, true, Side.DOWN));
    }

    @Test
    void autoSlopeInterpolatesBetweenEnds() {
        // Ramp along x: a full block 2 cells west, open ground 1 cell east.
        int packed = CollapsibleShape.autoSlope(2, new int[] { 16, 16 }, 1, new int[] { 0, 0 },
            1, new int[] { 0, 0 }, 1, new int[] { 0, 0 });
        assertTrue(CollapsibleShape.corner(packed, CollapsibleShape.NN) > CollapsibleShape.corner(packed, CollapsibleShape.PN));
        assertEquals(CollapsibleShape.corner(packed, CollapsibleShape.NN), CollapsibleShape.corner(packed, CollapsibleShape.NP));
        // Flat ground everywhere: a full block instead of an invisible sheet.
        assertEquals(CollapsibleShape.FULL, CollapsibleShape.autoSlope(1, new int[2], 1, new int[2], 1, new int[2], 1, new int[2]));
    }
}

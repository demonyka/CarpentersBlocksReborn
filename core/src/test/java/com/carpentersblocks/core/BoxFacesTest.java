package com.carpentersblocks.core;

import com.carpentersblocks.core.geom.Box;
import com.carpentersblocks.core.geom.BoxFaces;
import com.carpentersblocks.core.shape.SlabShape;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BoxFacesTest {
    @Test
    void fullCubeUsesWholeTexture() {
        for (Side side : Side.values()) {
            assertArrayEquals(new float[] { 0, 0, 16, 16 }, BoxFaces.uv(Box.FULL, side));
            assertTrue(BoxFaces.onBoundary(Box.FULL, side));
        }
    }

    @Test
    void bottomSlabShowsLowerHalfOnSides() {
        Box slab = SlabShape.DOWN.bounds;
        // v grows downwards in textures, so the lower half of the block is v 8..16.
        assertArrayEquals(new float[] { 0, 8, 16, 16 }, BoxFaces.uv(slab, Side.NORTH));
        assertArrayEquals(new float[] { 0, 8, 16, 16 }, BoxFaces.uv(slab, Side.EAST));
        assertArrayEquals(new float[] { 0, 0, 16, 16 }, BoxFaces.uv(slab, Side.UP));
        assertFalse(BoxFaces.onBoundary(slab, Side.UP));
        assertTrue(BoxFaces.onBoundary(slab, Side.DOWN));
    }

    @Test
    void matchesVanillaElementUvForOffCentreBox() {
        // Element from [2,3,4] to [10,12,14], values from Minecraft's BlockElement defaults.
        Box box = new Box(2 / 16.0, 3 / 16.0, 4 / 16.0, 10 / 16.0, 12 / 16.0, 14 / 16.0);
        assertArrayEquals(new float[] { 2, 2, 10, 12 }, BoxFaces.uv(box, Side.DOWN));
        assertArrayEquals(new float[] { 2, 4, 10, 14 }, BoxFaces.uv(box, Side.UP));
        assertArrayEquals(new float[] { 6, 4, 14, 13 }, BoxFaces.uv(box, Side.NORTH));
        assertArrayEquals(new float[] { 2, 4, 10, 13 }, BoxFaces.uv(box, Side.SOUTH));
        assertArrayEquals(new float[] { 4, 4, 14, 13 }, BoxFaces.uv(box, Side.WEST));
        assertArrayEquals(new float[] { 2, 4, 12, 13 }, BoxFaces.uv(box, Side.EAST));
    }
}

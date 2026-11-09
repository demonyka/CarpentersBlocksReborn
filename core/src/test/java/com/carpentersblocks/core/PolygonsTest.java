package com.carpentersblocks.core;

import com.carpentersblocks.core.geom.Box;
import com.carpentersblocks.core.geom.BoxFaces;
import com.carpentersblocks.core.geom.Polygon;
import com.carpentersblocks.core.geom.Polygons;
import com.carpentersblocks.core.geom.Vec3d;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class PolygonsTest {
    @Test
    void boxFacesKeepVanillaVertexOrderAndUvs() {
        Box box = new Box(2 / 16.0, 3 / 16.0, 4 / 16.0, 10 / 16.0, 12 / 16.0, 14 / 16.0);
        for (Polygon p : Polygons.ofBox(box)) {
            // Vanilla FaceBakery: vertex i gets u from uvs[0] for i = 0, 1 and v from uvs[1] for i = 0, 3.
            float[] uv = BoxFaces.uv(box, p.direction);
            float[] expected = { uv[0], uv[1], uv[0], uv[3], uv[2], uv[3], uv[2], uv[1] };
            assertArrayEquals(expected, p.uvs, 1e-4f, p.direction.name());
            assertEquals(p.direction, p.coverSide);
            assertEquals(new Vec3d(p.direction.dx, p.direction.dy, p.direction.dz).dot(p.normal), 1.0, 1e-9);
        }
    }

    @Test
    void triangleRepeatsOneVertexAndKeepsWinding() {
        // Upward-facing triangle; any input rotation gives the same slot order.
        Vec3d a = new Vec3d(0, 1, 0), b = new Vec3d(0, 1, 1), c = new Vec3d(1, 1, 1);
        for (Polygon p : new Polygon[] {
            Polygons.build(Arrays.asList(a, b, c), Side.UP, Side.UP),
            Polygons.build(Arrays.asList(b, c, a), Side.UP, Side.UP) }) {
            // Slot 0 is (minX, minZ), slot 1 (minX, maxZ), slot 2 (maxX, maxZ); slot 3 (maxX, minZ)
            // is empty and repeats a neighbour.
            assertSame(a, p.vertices[0]);
            assertSame(b, p.vertices[1]);
            assertSame(c, p.vertices[2]);
            assertTrue(p.vertices[3] == a || p.vertices[3] == c);
            assertEquals(0.5, p.area(), 1e-9);
        }
    }

    @Test
    void roofTieGoesToTopTexture() {
        assertEquals(Side.UP, Polygons.dominant(new Vec3d(0, 1, -1).normalize()));
        assertEquals(Side.EAST, Polygons.dominant(new Vec3d(1, 0, 1).normalize()));
        assertEquals(Side.NORTH, Polygons.dominant(new Vec3d(0.2, 0.5, -1).normalize()));
    }
}

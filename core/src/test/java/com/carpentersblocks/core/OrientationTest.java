package com.carpentersblocks.core;

import com.carpentersblocks.core.geom.Box;
import com.carpentersblocks.core.geom.Orientation;
import com.carpentersblocks.core.geom.Polygon;
import com.carpentersblocks.core.geom.Polygons;
import com.carpentersblocks.core.geom.Vec3d;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrientationTest {
    private static double volume(List<Polygon> polygons) {
        double v = 0;
        for (Polygon p : polygons) {
            v += p.area() * p.normal.dot(p.vertices[0]) / 3;
        }
        return v;
    }

    @Test
    void attachKeepsShapesClosedAndOutward() {
        Box button = Box.px(5, 0, 6, 11, 2, 10);
        for (Side support : Side.values()) {
            List<Polygon> rotated = Orientation.polygons(Polygons.ofBox(button), p -> Orientation.attach(p, support));
            assertEquals(6 * 2 * 4 / 4096.0, volume(rotated), 1e-9, support.name());
            // The face that touched the floor now touches the support.
            Box moved = button.attachedTo(support);
            double gap;
            switch (support) {
                case DOWN: gap = moved.minY; break;
                case UP: gap = 1 - moved.maxY; break;
                case NORTH: gap = moved.minZ; break;
                case SOUTH: gap = 1 - moved.maxZ; break;
                case WEST: gap = moved.minX; break;
                default: gap = 1 - moved.maxX; break;
            }
            assertEquals(0, gap, 1e-9, support.name());
        }
    }

    @Test
    void rotatedBoxIsStillClosed() {
        Box stick = Box.px(7, 1, 7, 9, 11, 9);
        List<Polygon> tilted = Polygons.ofRotatedBox(stick, 'x', 40, new Vec3d(0.5, 1 / 16.0, 0.5), 1);
        assertEquals(2 * 2 * 10 / 4096.0, volume(tilted), 1e-9);
        assertTrue(tilted.stream().allMatch(p -> p.part == 1 && p.cullFace == null));
    }
}

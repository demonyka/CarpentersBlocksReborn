package com.carpentersblocks.core;

import com.carpentersblocks.core.geom.Polygon;
import com.carpentersblocks.core.geom.Vec3d;
import com.carpentersblocks.core.shape.HeightField;
import com.carpentersblocks.core.shape.SlopePlacement;
import com.carpentersblocks.core.shape.SlopeType;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SlopeTypeTest {
    /**
     * Face shapes from 1.7.10 {@code data/Slope.java}, per slope id, in side order
     * DOWN UP NORTH SOUTH WEST EAST. F = full, W = wedge (half), T = prism triangle, N = none.
     */
    private static final String[] FACES_1710 = {
        "WWFNFN", "WWNFNF", "WWNFFN", "WWFNNF",
        "NFNFWW", "NFFNWW", "NFWWNF", "NFWWFN",
        "FNNFWW", "FNFNWW", "FNWWNF", "FNWWFN",
        "NFFWFW", "NFWFWF", "NFWFFW", "NFFWWF",
        "FNFWFW", "FNWFWF", "FNWFFW", "FNFWWF",
        "NFWNWN", "NFNWNW", "NFNWWN", "NFWNNW",
        "FNWNWN", "FNNWNW", "FNNWWN", "FNWNNW",
        "WFFWFW", "WFWFWF", "WFWFFW", "WFFWWF",
        "FWFWFW", "FWWFWF", "FWWFFW", "FWFWWF",
        "NWWNWN", "NWNWNW", "NWNWWN", "NWWNNW",
        "WNWNWN", "WNNWNW", "WNNWWN", "WNWNNW",
        "NFNNNN", "FNNNNN",
        "FNTNNN", "FNNTNN", "FNNNTN", "FNNNNT",
        "FNTTNN", "FNNNTT", "FNNTNT", "FNTNTN", "FNTNNT", "FNNTTN",
        "FNTNTT", "FNNTTT", "FNTTTN", "FNTTNT",
        "FNTTTT",
        "FNTFWW", "FNFTWW", "FNWWTF", "FNWWFT",
    };

    /** 1.7.10 {@code ROTATION_Y[UP]}: slope id after a hammer left-click. */
    private static final int[] ROTATION_1710 = {
        3, 2, 0, 1, 7, 6, 4, 5, 11, 10, 8, 9,
        15, 14, 12, 13, 19, 18, 16, 17, 23, 22, 20, 21, 27, 26, 24, 25,
        31, 30, 28, 29, 35, 34, 32, 33, 39, 38, 36, 37, 43, 42, 40, 41,
        44, 45, 49, 48, 46, 47, 51, 50, 55, 54, 52, 53, 59, 58, 56, 57, 60,
        64, 63, 61, 62,
    };

    @Test
    void faceShapesMatch1710() {
        assertEquals(65, SlopeType.values().length);
        for (SlopeType type : SlopeType.values()) {
            String expected = FACES_1710[type.ordinal()];
            StringBuilder actual = new StringBuilder();
            for (Side side : Side.values()) {
                actual.append(type.faceShape(side).name().charAt(0));
            }
            assertEquals(expected, actual.toString(), type.name());
        }
    }

    @Test
    void rotationMatches1710() {
        for (SlopeType type : SlopeType.values()) {
            assertEquals(SlopeType.byId(ROTATION_1710[type.ordinal()]), type.rotateY(), type.name());
            assertEquals(type, type.rotateY().rotateY().rotateY().rotateY(), type.name());
        }
    }

    @Test
    void hammerCycleAlwaysReturnsToStart() {
        for (SlopeType type : SlopeType.values()) {
            SlopeType t = type;
            int steps = 0;
            while (t != SlopeType.WEDGE_SE && steps < 40) {
                t = t.next();
                steps++;
            }
            assertEquals(SlopeType.WEDGE_SE, t, "cycle from " + type);
        }
    }

    @Test
    void shapesAreClosedSolidsWithOutwardFaces() {
        for (SlopeType type : SlopeType.values()) {
            // Divergence theorem: sum(area * n.p) / 3 is the volume only if the surface is
            // closed and every normal points outwards.
            double volume = 0;
            for (Polygon p : type.polygons()) {
                volume += p.area() * p.normal.dot(p.vertices[0]) / 3;
                for (float uv : p.uvs) {
                    assertTrue(uv >= -1e-4 && uv <= 16 + 1e-4, type + " uv " + uv);
                }
            }
            assertEquals(integrate(type.field()), volume, 1e-3, type.name());
            assertTrue(volume > 0.1, type.name());
        }
    }

    @Test
    void knownVolumes() {
        assertEquals(0.5, volume(SlopeType.WEDGE_POS_N), 1e-6);
        assertEquals(0.5, volume(SlopeType.WEDGE_SE), 1e-6);
        assertEquals(2 / 3.0, volume(SlopeType.WEDGE_INT_NEG_SE), 1e-6);
        assertEquals(1 / 3.0, volume(SlopeType.WEDGE_EXT_POS_NW), 1e-6);
        assertEquals(5 / 6.0, volume(SlopeType.OBL_INT_POS_NE), 1e-6);
        assertEquals(1 / 6.0, volume(SlopeType.OBL_EXT_NEG_SW), 1e-6);
        assertEquals(1 / 6.0, volume(SlopeType.PRISM_POS), 1e-6);
    }

    @Test
    void wedgeIsOneSlopedQuadPlusFourFaces() {
        // Merging should give exactly: bottom, back, two triangles, one sloped quad.
        assertEquals(5, SlopeType.WEDGE_POS_N.polygons().size());
        long sloped = SlopeType.WEDGE_POS_N.polygons().stream().filter(p -> p.cullFace == null).count();
        assertEquals(1, sloped);
    }

    @Test
    void collisionReachesTheTop() {
        double maxY = SlopeType.WEDGE_POS_N.collisionBoxes().stream().mapToDouble(b -> b.maxY).max().orElse(0);
        assertEquals(1.0, maxY, 1e-9);
        assertFalse(SlopeType.PRISM_POS.collisionBoxes().stream().anyMatch(b -> b.maxY > 0.5 + 1e-9));
    }

    @Test
    void placementMatches1710() {
        // Placing on top of a block while looking north: rises away from the player like stairs.
        assertEquals(SlopeType.WEDGE_POS_S,
            SlopePlacement.place(SlopePlacement.Kind.WEDGE, Side.UP, 0.5, 1, 0.5, Side.NORTH, 180));
        // Clicking the upper middle of a block side gives a positive... no: upper half = NEG wedge (hangs).
        assertEquals(SlopeType.WEDGE_NEG_S,
            SlopePlacement.place(SlopePlacement.Kind.WEDGE, Side.SOUTH, 0.5, 0.8, 1, Side.NORTH, 180));
        assertEquals(SlopeType.WEDGE_POS_S,
            SlopePlacement.place(SlopePlacement.Kind.WEDGE, Side.SOUTH, 0.5, 0.2, 1, Side.NORTH, 180));
        assertEquals(SlopeType.PRISM_POS,
            SlopePlacement.place(SlopePlacement.Kind.PRISM, Side.UP, 0.5, 1, 0.5, Side.NORTH, 180));
        Set<SlopeType> corners = new HashSet<>();
        for (int yaw = 0; yaw < 360; yaw += 90) {
            corners.add(SlopePlacement.place(SlopePlacement.Kind.OBLIQUE_INTERIOR, Side.UP, 0.5, 1, 0.5, Side.NORTH, yaw + 45));
        }
        assertEquals(4, corners.size());
        assertTrue(corners.stream().allMatch(t -> t.family == SlopeType.Family.OBLIQUE_INT && t.positive));
    }

    private static double volume(SlopeType type) {
        double volume = 0;
        for (Polygon p : type.polygons()) {
            volume += p.area() * p.normal.dot(p.vertices[0]) / 3;
        }
        return volume;
    }

    private static double integrate(HeightField field) {
        int n = 400;
        double sum = 0;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                sum += field.heightAt((i + 0.5) / n, (j + 0.5) / n);
            }
        }
        return sum / (n * n);
    }

    @SuppressWarnings("unused")
    private static Vec3d unused() {
        return null;
    }
}

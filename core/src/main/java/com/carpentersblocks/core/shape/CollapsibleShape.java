package com.carpentersblocks.core.shape;

import com.carpentersblocks.core.Side;
import com.carpentersblocks.core.geom.Box;
import com.carpentersblocks.core.geom.Polygon;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Collapsible Block: a block whose surface passes through four corner heights (0..16 pixels),
 * resting on the floor or hanging from the ceiling. The four heights are packed into one int
 * (5 bits each) stored in the block entity.
 * <p>
 * The surface is four triangles meeting at a centre point; 1.7.10 put the centre at the
 * average of the diagonal pair whose heights differ least, which is the same as splitting
 * along the steeper diagonal.
 */
public final class CollapsibleShape {
    /** Corner indices: x and z at 0 (N = negative) or 1 (P = positive). */
    public static final int NN = 0, PN = 1, NP = 2, PP = 3;
    public static final int MAX = 16;
    public static final int FULL = pack(MAX, MAX, MAX, MAX);

    private static final Map<Integer, List<Polygon>> POLYGONS = new ConcurrentHashMap<>();
    private static final Map<Integer, List<Box>> BOXES = new ConcurrentHashMap<>();

    /** Shapes are cached per corner combination; the caches are emptied beyond this many entries. */
    public static final int CACHE_LIMIT = 4096;

    private CollapsibleShape() {
    }

    /** The same shape with every corner in 0..16 (stored data may hold larger raw values). */
    public static int normalize(int packed) {
        return pack(corner(packed, NN), corner(packed, PN), corner(packed, NP), corner(packed, PP));
    }

    private static <T> Map<Integer, T> bounded(Map<Integer, T> cache) {
        if (cache.size() > CACHE_LIMIT) {
            cache.clear();
        }
        return cache;
    }

    public static int pack(int nn, int pn, int np, int pp) {
        return clamp(nn) | clamp(pn) << 5 | clamp(np) << 10 | clamp(pp) << 15;
    }

    public static int corner(int packed, int index) {
        return Math.min(MAX, (packed >> (index * 5)) & 31);
    }

    public static int withCorner(int packed, int index, int height) {
        return (packed & ~(31 << (index * 5))) | clamp(height) << (index * 5);
    }

    public static int cornerIndex(int x, int z) {
        return x | z << 1;
    }

    /** Corner nearest to a hit point (block-local x/z). */
    public static int nearestCorner(double hitX, double hitZ) {
        return cornerIndex(hitX >= 0.5 ? 1 : 0, hitZ >= 0.5 ? 1 : 0);
    }

    private static int clamp(int h) {
        return Math.max(0, Math.min(MAX, h));
    }

    public static HeightField field(int packed, boolean hanging) {
        double nn = corner(packed, NN) / 16.0, pn = corner(packed, PN) / 16.0;
        double np = corner(packed, NP) / 16.0, pp = corner(packed, PP) / 16.0;
        if (nn + pn + np + pp == 0) {
            // A completely flat block would have no volume at all; keep a hairline sheet.
            nn = pn = np = pp = 1 / 1024.0;
        }
        double centre = Math.abs(nn - pp) < Math.abs(pn - np) ? (pn + np) / 2 : (nn + pp) / 2;
        double cnn = nn, cpn = pn, cnp = np, cpp = pp, c = centre;
        return HeightField.sample(hanging ? Side.UP : Side.DOWN, p -> {
            boolean xMid = Math.abs(p.x - 0.5) < 1e-6, zMid = Math.abs(p.z - 0.5) < 1e-6;
            if (xMid && zMid) {
                return c;
            }
            double west = p.z < 0.25 ? cnn : p.z > 0.75 ? cnp : (cnn + cnp) / 2;
            double east = p.z < 0.25 ? cpn : p.z > 0.75 ? cpp : (cpn + cpp) / 2;
            return p.x < 0.25 ? west : p.x > 0.75 ? east : (west + east) / 2;
        });
    }

    public static List<Polygon> polygons(int packed, boolean hanging) {
        int shape = normalize(packed);
        return bounded(POLYGONS).computeIfAbsent(shape << 1 | (hanging ? 1 : 0),
            k -> Collections.unmodifiableList(field(shape, hanging).polygons()));
    }

    /** Collision columns (8 per axis), never below the visible surface. */
    public static List<Box> collisionBoxes(int packed, boolean hanging) {
        int shape = normalize(packed);
        return bounded(BOXES).computeIfAbsent(shape << 1 | (hanging ? 1 : 0),
            k -> Collections.unmodifiableList(field(shape, hanging).boxes(8)));
    }

    /** Whether a block side is a full face (fixes 1.7.10, which ignored hanging blocks). */
    public static boolean isFaceFull(int packed, boolean hanging, Side side) {
        Side base = hanging ? Side.UP : Side.DOWN;
        if (side == base) {
            return true;
        }
        if (side == base.opposite()) {
            return packed == FULL;
        }
        int x0 = side == Side.EAST ? 1 : 0, x1 = side == Side.WEST ? 0 : 1;
        int z0 = side == Side.SOUTH ? 1 : 0, z1 = side == Side.NORTH ? 0 : 1;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                if (corner(packed, cornerIndex(x, z)) < MAX) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 1.7.10 auto-slope: corner heights interpolated between the ends found along x and z.
     *
     * @param xnDist distance (>= 1) to the end on the west; {@code xnEast} its two east corner
     *               heights (north, south). Likewise for the other three directions.
     * @return packed heights, or {@link #FULL} when there is no slope to follow
     */
    public static int autoSlope(int xnDist, int[] xnEast, int xpDist, int[] xpWest,
                                int znDist, int[] znSouth, int zpDist, int[] zpNorth) {
        // Along x, for the north row (z = 0) and the south row (z = 1).
        double spanX = xnDist + xpDist - 1;
        double dx1 = (xpWest[0] - xnEast[0]) / spanX, dx2 = (xpWest[1] - xnEast[1]) / spanX;
        double xNN = xnEast[0] + dx1 * (xnDist - 1), xPN = xnEast[0] + dx1 * xnDist;
        double xNP = xnEast[1] + dx2 * (xnDist - 1), xPP = xnEast[1] + dx2 * xnDist;
        // Along z, for the west column (x = 0) and the east column (x = 1).
        double spanZ = znDist + zpDist - 1;
        double dz1 = (zpNorth[0] - znSouth[0]) / spanZ, dz2 = (zpNorth[1] - znSouth[1]) / spanZ;
        double zNN = znSouth[0] + dz1 * (znDist - 1), zNP = znSouth[0] + dz1 * znDist;
        double zPN = znSouth[1] + dz2 * (znDist - 1), zPP = znSouth[1] + dz2 * znDist;
        int nn = (int) Math.max(0, Math.max(xNN, zNN)), pn = (int) Math.max(0, Math.max(xPN, zPN));
        int np = (int) Math.max(0, Math.max(xNP, zNP)), pp = (int) Math.max(0, Math.max(xPP, zPP));
        if (nn == 0 && pn == 0 && np == 0 && pp == 0) {
            // 1.7.10 left an almost invisible sheet on flat ground; a full block is more useful.
            return FULL;
        }
        return pack(nn, pn, np, pp);
    }
}

package com.carpentersblocks.core.shape;

import com.carpentersblocks.core.Side;
import com.carpentersblocks.core.geom.Box;
import com.carpentersblocks.core.geom.Polygon;
import com.carpentersblocks.core.geom.Polygons;
import com.carpentersblocks.core.geom.Vec3d;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static com.carpentersblocks.core.Side.*;

/**
 * All 65 slope shapes of 1.7.10, in the same order (ordinal = 1.7.10 slope id).
 * <p>
 * Each shape is defined by its family and the directions it faces; geometry, collision,
 * rotation and the hammer cycle are derived from that.
 */
public enum SlopeType {
    WEDGE_SE(Family.WEDGE_SIDE, SOUTH, EAST),
    WEDGE_NW(Family.WEDGE_SIDE, NORTH, WEST),
    WEDGE_NE(Family.WEDGE_SIDE, NORTH, EAST),
    WEDGE_SW(Family.WEDGE_SIDE, SOUTH, WEST),
    WEDGE_NEG_N(Family.WEDGE, DOWN, NORTH),
    WEDGE_NEG_S(Family.WEDGE, DOWN, SOUTH),
    WEDGE_NEG_W(Family.WEDGE, DOWN, WEST),
    WEDGE_NEG_E(Family.WEDGE, DOWN, EAST),
    WEDGE_POS_N(Family.WEDGE, UP, NORTH),
    WEDGE_POS_S(Family.WEDGE, UP, SOUTH),
    WEDGE_POS_W(Family.WEDGE, UP, WEST),
    WEDGE_POS_E(Family.WEDGE, UP, EAST),
    WEDGE_INT_NEG_SE(Family.WEDGE_INT, DOWN, SOUTH, EAST),
    WEDGE_INT_NEG_NW(Family.WEDGE_INT, DOWN, NORTH, WEST),
    WEDGE_INT_NEG_NE(Family.WEDGE_INT, DOWN, NORTH, EAST),
    WEDGE_INT_NEG_SW(Family.WEDGE_INT, DOWN, SOUTH, WEST),
    WEDGE_INT_POS_SE(Family.WEDGE_INT, UP, SOUTH, EAST),
    WEDGE_INT_POS_NW(Family.WEDGE_INT, UP, NORTH, WEST),
    WEDGE_INT_POS_NE(Family.WEDGE_INT, UP, NORTH, EAST),
    WEDGE_INT_POS_SW(Family.WEDGE_INT, UP, SOUTH, WEST),
    WEDGE_EXT_NEG_SE(Family.WEDGE_EXT, DOWN, SOUTH, EAST),
    WEDGE_EXT_NEG_NW(Family.WEDGE_EXT, DOWN, NORTH, WEST),
    WEDGE_EXT_NEG_NE(Family.WEDGE_EXT, DOWN, NORTH, EAST),
    WEDGE_EXT_NEG_SW(Family.WEDGE_EXT, DOWN, SOUTH, WEST),
    WEDGE_EXT_POS_SE(Family.WEDGE_EXT, UP, SOUTH, EAST),
    WEDGE_EXT_POS_NW(Family.WEDGE_EXT, UP, NORTH, WEST),
    WEDGE_EXT_POS_NE(Family.WEDGE_EXT, UP, NORTH, EAST),
    WEDGE_EXT_POS_SW(Family.WEDGE_EXT, UP, SOUTH, WEST),
    OBL_INT_NEG_SE(Family.OBLIQUE_INT, DOWN, SOUTH, EAST),
    OBL_INT_NEG_NW(Family.OBLIQUE_INT, DOWN, NORTH, WEST),
    OBL_INT_NEG_NE(Family.OBLIQUE_INT, DOWN, NORTH, EAST),
    OBL_INT_NEG_SW(Family.OBLIQUE_INT, DOWN, SOUTH, WEST),
    OBL_INT_POS_SE(Family.OBLIQUE_INT, UP, SOUTH, EAST),
    OBL_INT_POS_NW(Family.OBLIQUE_INT, UP, NORTH, WEST),
    OBL_INT_POS_NE(Family.OBLIQUE_INT, UP, NORTH, EAST),
    OBL_INT_POS_SW(Family.OBLIQUE_INT, UP, SOUTH, WEST),
    OBL_EXT_NEG_SE(Family.OBLIQUE_EXT, DOWN, SOUTH, EAST),
    OBL_EXT_NEG_NW(Family.OBLIQUE_EXT, DOWN, NORTH, WEST),
    OBL_EXT_NEG_NE(Family.OBLIQUE_EXT, DOWN, NORTH, EAST),
    OBL_EXT_NEG_SW(Family.OBLIQUE_EXT, DOWN, SOUTH, WEST),
    OBL_EXT_POS_SE(Family.OBLIQUE_EXT, UP, SOUTH, EAST),
    OBL_EXT_POS_NW(Family.OBLIQUE_EXT, UP, NORTH, WEST),
    OBL_EXT_POS_NE(Family.OBLIQUE_EXT, UP, NORTH, EAST),
    OBL_EXT_POS_SW(Family.OBLIQUE_EXT, UP, SOUTH, WEST),
    PRISM_NEG(Family.PRISM, DOWN),
    PRISM_POS(Family.PRISM, UP),
    PRISM_1P_POS_N(Family.PRISM_1P, UP, NORTH),
    PRISM_1P_POS_S(Family.PRISM_1P, UP, SOUTH),
    PRISM_1P_POS_W(Family.PRISM_1P, UP, WEST),
    PRISM_1P_POS_E(Family.PRISM_1P, UP, EAST),
    PRISM_2P_POS_NS(Family.PRISM_2P, UP, NORTH, SOUTH),
    PRISM_2P_POS_WE(Family.PRISM_2P, UP, WEST, EAST),
    PRISM_2P_POS_SE(Family.PRISM_2P, UP, SOUTH, EAST),
    PRISM_2P_POS_NW(Family.PRISM_2P, UP, NORTH, WEST),
    PRISM_2P_POS_NE(Family.PRISM_2P, UP, NORTH, EAST),
    PRISM_2P_POS_SW(Family.PRISM_2P, UP, SOUTH, WEST),
    PRISM_3P_POS_NWE(Family.PRISM_3P, UP, NORTH, WEST, EAST),
    PRISM_3P_POS_SWE(Family.PRISM_3P, UP, SOUTH, WEST, EAST),
    PRISM_3P_POS_NSW(Family.PRISM_3P, UP, NORTH, SOUTH, WEST),
    PRISM_3P_POS_NSE(Family.PRISM_3P, UP, NORTH, SOUTH, EAST),
    PRISM_4P_POS(Family.PRISM_4P, UP, NORTH, SOUTH, WEST, EAST),
    PRISM_WEDGE_POS_N(Family.PRISM_WEDGE, UP, NORTH),
    PRISM_WEDGE_POS_S(Family.PRISM_WEDGE, UP, SOUTH),
    PRISM_WEDGE_POS_W(Family.PRISM_WEDGE, UP, WEST),
    PRISM_WEDGE_POS_E(Family.PRISM_WEDGE, UP, EAST);

    /** Shape families. Prisms are roof pieces: a pyramid with 0-4 ridges ("points"). */
    public enum Family {
        WEDGE_SIDE, WEDGE, WEDGE_INT, WEDGE_EXT, OBLIQUE_INT, OBLIQUE_EXT,
        PRISM, PRISM_1P, PRISM_2P, PRISM_3P, PRISM_4P, PRISM_WEDGE;

        public boolean isPrism() {
            return this == PRISM || this == PRISM_1P || this == PRISM_2P || this == PRISM_3P || this == PRISM_4P;
        }
    }

    /** Face shape on a block side, as 1.7.10 classified it (used for culling and side covers). */
    public enum FaceShape { NONE, TRIANGLE, WEDGE, FULL }

    /** Collision columns per axis: 16 = one-pixel steps, walkable without jumping. */
    public static final int COLLISION_STEPS = 16;

    private static final SlopeType[] VALUES = values();

    public final Family family;
    /** Directions the slope faces (1.7.10 "facings"). Contains UP or DOWN for non-side wedges. */
    public final Set<Side> facings;
    public final boolean positive;

    private HeightField field;
    private List<Polygon> polygons;
    private List<Box> boxes;
    private FaceShape[] faceShapes;

    SlopeType(Family family, Side... facings) {
        this.family = family;
        this.facings = Collections.unmodifiableSet(EnumSet.copyOf(Arrays.asList(facings)));
        this.positive = this.facings.contains(UP);
    }

    public static SlopeType byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : WEDGE_SE;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    // ---- geometry -------------------------------------------------------------------------

    public synchronized HeightField field() {
        if (field == null) {
            field = buildField();
        }
        return field;
    }

    public synchronized List<Polygon> polygons() {
        if (polygons == null) {
            polygons = Collections.unmodifiableList(field().polygons());
        }
        return polygons;
    }

    public synchronized List<Box> collisionBoxes() {
        if (boxes == null) {
            boxes = Collections.unmodifiableList(field().boxes(COLLISION_STEPS));
        }
        return boxes;
    }

    /** Shape of the face this slope shows on a block side. */
    public synchronized FaceShape faceShape(Side side) {
        if (faceShapes == null) {
            double[] area = new double[6];
            for (Polygon polygon : polygons()) {
                if (polygon.cullFace != null) {
                    area[polygon.cullFace.ordinal()] += polygon.area();
                }
            }
            faceShapes = new FaceShape[6];
            for (Side s : Side.values()) {
                double a = area[s.ordinal()];
                faceShapes[s.ordinal()] = a > 0.99 ? FaceShape.FULL : a > 0.49 ? FaceShape.WEDGE : a > 0.24 ? FaceShape.TRIANGLE : FaceShape.NONE;
            }
        }
        return faceShapes[side.ordinal()];
    }

    /**
     * For a {@link FaceShape#WEDGE} face: the corner of the block face where the half-triangle
     * has its right angle (1.7.10 "face bias"), as (s is max ? 1 : 0) | (t is max ? 2 : 0) in
     * {@link Polygons#project} coordinates. -1 for other face shapes.
     */
    public int wedgeCorner(Side side) {
        if (faceShape(side) != FaceShape.WEDGE) {
            return -1;
        }
        boolean[] covered = new boolean[4];
        for (Polygon polygon : polygons()) {
            if (polygon.cullFace != side) {
                continue;
            }
            for (Vec3d v : polygon.vertices) {
                double[] st = Polygons.project(side, v);
                boolean sEdge = st[0] < Vec3d.EPS || st[0] > 1 - Vec3d.EPS;
                boolean tEdge = st[1] < Vec3d.EPS || st[1] > 1 - Vec3d.EPS;
                if (sEdge && tEdge) {
                    covered[(st[0] > 0.5 ? 1 : 0) | (st[1] > 0.5 ? 2 : 0)] = true;
                }
            }
        }
        // The right angle sits at the covered corner whose diagonal opposite is not covered.
        for (int corner = 0; corner < 4; corner++) {
            if (covered[corner] && !covered[3 - corner]) {
                return corner;
            }
        }
        return -1;
    }

    public boolean isFaceFull(Side side) {
        return faceShape(side) == FaceShape.FULL;
    }

    private HeightField buildField() {
        Side vertical = positive ? UP : facings.contains(DOWN) ? DOWN : null;
        Side[] horizontal = facings.stream().filter(s -> s.dy == 0).toArray(Side[]::new);
        Side base = vertical == null ? null : vertical.opposite();
        switch (family) {
            case WEDGE_SIDE: {
                // Vertical slope: rests on the full face opposite its east/west facing,
                // depth grows away from its north/south facing.
                Side x = horizontal[0].dx != 0 ? horizontal[0] : horizontal[1];
                Side z = horizontal[0].dz != 0 ? horizontal[0] : horizontal[1];
                return HeightField.sample(x.opposite(), p -> rise(z, p));
            }
            case WEDGE:
                return HeightField.sample(base, p -> rise(horizontal[0], p));
            case WEDGE_INT:
                return HeightField.sample(base, p -> Math.max(rise(horizontal[0], p), rise(horizontal[1], p)));
            case WEDGE_EXT:
                return HeightField.sample(base, p -> Math.min(rise(horizontal[0], p), rise(horizontal[1], p)));
            case OBLIQUE_INT:
                return HeightField.sample(base, p -> Math.min(1, rise(horizontal[0], p) + rise(horizontal[1], p)));
            case OBLIQUE_EXT:
                return HeightField.sample(base, p -> Math.max(0, rise(horizontal[0], p) + rise(horizontal[1], p) - 1));
            case PRISM_WEDGE:
                return HeightField.sample(base, p -> Math.max(rise(horizontal[0], p), ridge(p, horizontal)));
            default: // prisms
                return HeightField.sample(base, p -> ridge(p, horizontal));
        }
    }

    /** 0 on the edge the slope faces, rising to 1 on the opposite edge. */
    private static double rise(Side facing, Vec3d p) {
        switch (facing) {
            case NORTH: return p.z;
            case SOUTH: return 1 - p.z;
            case WEST:  return p.x;
            case EAST:  return 1 - p.x;
            default: throw new IllegalArgumentException(String.valueOf(facing));
        }
    }

    /** Prism roof: half-height peak in the centre, plus a ridge to the middle of each "point" side. */
    private static double ridge(Vec3d p, Side[] points) {
        double cx = Math.abs(p.x - 0.5), cz = Math.abs(p.z - 0.5);
        if (cx < Vec3d.EPS && cz < Vec3d.EPS) {
            return 0.5;
        }
        for (Side point : points) {
            boolean isMid = point.dx != 0
                ? cz < Vec3d.EPS && Math.abs(p.x - (point.dx > 0 ? 1 : 0)) < Vec3d.EPS
                : cx < Vec3d.EPS && Math.abs(p.z - (point.dz > 0 ? 1 : 0)) < Vec3d.EPS;
            if (isMid) {
                return 0.5;
            }
        }
        return 0;
    }

    // ---- hammer -------------------------------------------------------------------------

    /** Hammer left-click: quarter turn clockwise seen from above (north to east), as 1.7.10. */
    public SlopeType rotateY() {
        EnumSet<Side> rotated = EnumSet.noneOf(Side.class);
        for (Side side : facings) {
            rotated.add(rotateClockwise(side));
        }
        for (SlopeType type : VALUES) {
            if (type.family == family && type.facings.equals(rotated)) {
                return type;
            }
        }
        return this;
    }

    private static Side rotateClockwise(Side side) {
        switch (side) {
            case NORTH: return EAST;
            case EAST:  return SOUTH;
            case SOUTH: return WEST;
            case WEST:  return NORTH;
            default:    return side;
        }
    }

    /** Hammer right-click: next shape in the single 1.7.10 cycle through every family. */
    public SlopeType next() {
        int id = ordinal();
        switch (family) {
            case WEDGE_SIDE:
                return byId(id + 8);
            case WEDGE:
            case WEDGE_INT:
            case WEDGE_EXT:
            case OBLIQUE_INT:
                return byId(positive ? id - 4 : id + 12);
            case OBLIQUE_EXT:
                return positive ? byId(id - 4) : PRISM_POS;
            case PRISM:
                return positive ? PRISM_NEG : PRISM_1P_POS_N;
            case PRISM_1P:
                return PRISM_2P_POS_NS;
            case PRISM_2P:
                return this == PRISM_2P_POS_NS || this == PRISM_2P_POS_WE ? PRISM_2P_POS_SE : PRISM_3P_POS_NWE;
            case PRISM_3P:
                return PRISM_4P_POS;
            case PRISM_4P:
                return PRISM_WEDGE_POS_N;
            case PRISM_WEDGE:
            default:
                return WEDGE_SE;
        }
    }
}

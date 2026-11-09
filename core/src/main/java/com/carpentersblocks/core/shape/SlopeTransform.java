package com.carpentersblocks.core.shape;

import com.carpentersblocks.core.Side;
import com.carpentersblocks.core.shape.SlopeType.Family;

import java.util.EnumMap;
import java.util.Map;

import static com.carpentersblocks.core.Side.*;
import static com.carpentersblocks.core.shape.SlopeType.*;

/**
 * Automatic joining of slopes on placement, ported from 1.7.10 {@code SlopeTransform}.
 * Skipped when the player sneaks.
 * <ul>
 *   <li>A wedge placed under or over a slope with a half-triangle face becomes a matching side wedge.</li>
 *   <li>A wedge placed next to wedges becomes an interior or exterior corner, and turns
 *       neighbouring plain wedges into corners.</li>
 *   <li>Upward prisms grow ridges ("points") towards neighbouring prisms.</li>
 * </ul>
 * Pure functions of the neighbours, so they can be tested without a world.
 */
public final class SlopeTransform {
    /** Slope shape of the neighbour on a side, or {@code null} if it is not a slope. */
    @FunctionalInterface
    public interface Neighbors {
        SlopeType at(Side side);
    }

    private SlopeTransform() {
    }

    private static boolean has(SlopeType t, Side s) {
        return t.facings.contains(s);
    }

    private static SlopeType noSide(SlopeType t) {
        return t != null && t.family == Family.WEDGE_SIDE ? null : t;
    }

    private static SlopeType pick(boolean positive, SlopeType pos, SlopeType neg) {
        return positive ? pos : neg;
    }

    /** Shape for a wedge being placed, given its neighbours. */
    public static SlopeType transformWedge(SlopeType placed, Neighbors n) {
        SlopeType xn = n.at(WEST), xp = n.at(EAST), zn = n.at(NORTH), zp = n.at(SOUTH), yn = n.at(DOWN), yp = n.at(UP);

        // Under or over a half-triangle face: become the side wedge with the same triangle.
        int corner = -1;
        if (yp != null && yp.faceShape(DOWN) == FaceShape.WEDGE) {
            corner = yp.wedgeCorner(DOWN);
        } else if (yn != null && yn.faceShape(UP) == FaceShape.WEDGE) {
            corner = yn.wedgeCorner(UP);
        }
        if (corner >= 0) {
            for (SlopeType side : new SlopeType[] { WEDGE_SE, WEDGE_NW, WEDGE_NE, WEDGE_SW }) {
                if (side.wedgeCorner(UP) == corner) {
                    return side;
                }
            }
        }

        if (placed.family != Family.WEDGE) {
            return placed;
        }
        // 1.7.10 also let vertical side wedges (which face no up/down direction) take part in
        // the corner rules, turning straight pieces into odd corners; they are ignored here.
        xn = noSide(xn);
        xp = noSide(xp);
        zn = noSide(zn);
        zp = noSide(zp);
        // Between two neighbours: corner pieces.
        if (zn != null) {
            if (xn != null) {
                if (has(zn, WEST) && has(xn, NORTH)) return pick(xn.positive && zn.positive, WEDGE_INT_POS_NW, WEDGE_INT_NEG_NW);
                if (has(zn, EAST) && has(xn, SOUTH)) return pick(xn.positive && zn.positive, WEDGE_EXT_POS_SE, WEDGE_EXT_NEG_SE);
            }
            if (xp != null) {
                if (has(zn, EAST) && has(xp, NORTH)) return pick(xp.positive && zn.positive, WEDGE_INT_POS_NE, WEDGE_INT_NEG_NE);
                if (has(zn, WEST) && has(xp, SOUTH)) return pick(xp.positive && zn.positive, WEDGE_EXT_POS_SW, WEDGE_EXT_NEG_SW);
            }
        }
        if (zp != null) {
            if (xn != null) {
                if (has(zp, WEST) && has(xn, SOUTH)) return pick(xn.positive && zp.positive, WEDGE_INT_POS_SW, WEDGE_INT_NEG_SW);
                if (has(zp, EAST) && has(xn, NORTH)) return pick(xn.positive && zp.positive, WEDGE_EXT_POS_NE, WEDGE_EXT_NEG_NE);
            }
            if (xp != null) {
                if (has(zp, EAST) && has(xp, SOUTH)) return pick(xp.positive && zp.positive, WEDGE_INT_POS_SE, WEDGE_INT_NEG_SE);
                if (has(zp, WEST) && has(xp, NORTH)) return pick(xp.positive && zp.positive, WEDGE_EXT_POS_NW, WEDGE_EXT_NEG_NW);
            }
        }
        // Next to one neighbour that slopes across our slope.
        if (xn != null) {
            if (has(placed, WEST)) {
                if (has(xn, SOUTH) && !has(xn, EAST)) return pick(xn.positive, WEDGE_INT_POS_SW, WEDGE_INT_NEG_SW);
                if (has(xn, NORTH) && !has(xn, EAST)) return pick(xn.positive, WEDGE_INT_POS_NW, WEDGE_INT_NEG_NW);
            }
            if (has(placed, EAST)) {
                if (has(xn, SOUTH) && !has(xn, EAST)) return pick(xn.positive, WEDGE_EXT_POS_SE, WEDGE_EXT_NEG_SE);
                if (has(xn, NORTH) && !has(xn, EAST)) return pick(xn.positive, WEDGE_EXT_POS_NE, WEDGE_EXT_NEG_NE);
            }
        }
        if (xp != null) {
            if (has(placed, WEST)) {
                if (has(xp, SOUTH) && !has(xp, WEST)) return pick(xp.positive, WEDGE_EXT_POS_SW, WEDGE_EXT_NEG_SW);
                if (has(xp, NORTH) && !has(xp, WEST)) return pick(xp.positive, WEDGE_EXT_POS_NW, WEDGE_EXT_NEG_NW);
            }
            if (has(placed, EAST)) {
                if (has(xp, SOUTH) && !has(xp, WEST)) return pick(xp.positive, WEDGE_INT_POS_SE, WEDGE_INT_NEG_SE);
                if (has(xp, NORTH) && !has(xp, WEST)) return pick(xp.positive, WEDGE_INT_POS_NE, WEDGE_INT_NEG_NE);
            }
        }
        if (zn != null) {
            if (has(placed, NORTH)) {
                if (has(zn, EAST) && !has(zn, SOUTH)) return pick(zn.positive, WEDGE_INT_POS_NE, WEDGE_INT_NEG_NE);
                if (has(zn, WEST) && !has(zn, SOUTH)) return pick(zn.positive, WEDGE_INT_POS_NW, WEDGE_INT_NEG_NW);
            }
            if (has(placed, SOUTH)) {
                if (has(zn, EAST) && !has(zn, SOUTH)) return pick(zn.positive, WEDGE_EXT_POS_SE, WEDGE_EXT_NEG_SE);
                if (has(zn, WEST) && !has(zn, SOUTH)) return pick(zn.positive, WEDGE_EXT_POS_SW, WEDGE_EXT_NEG_SW);
            }
        }
        if (zp != null) {
            if (has(placed, NORTH)) {
                if (has(zp, EAST) && !has(zp, NORTH)) return pick(zp.positive, WEDGE_EXT_POS_NE, WEDGE_EXT_NEG_NE);
                if (has(zp, WEST) && !has(zp, NORTH)) return pick(zp.positive, WEDGE_EXT_POS_NW, WEDGE_EXT_NEG_NW);
            }
            if (has(placed, SOUTH)) {
                if (has(zp, EAST) && !has(zp, NORTH)) return pick(zp.positive, WEDGE_INT_POS_SE, WEDGE_INT_NEG_SE);
                if (has(zp, WEST) && !has(zp, NORTH)) return pick(zp.positive, WEDGE_INT_POS_SW, WEDGE_INT_NEG_SW);
            }
        }
        return placed;
    }

    /**
     * New shapes for neighbouring plain wedges after a wedge was placed (as {@code placed},
     * already transformed). Only neighbours of the same direction (up/down) change.
     */
    public static Map<Side, SlopeType> adjacentWedges(SlopeType placed, Neighbors n) {
        Map<Side, SlopeType> changes = new EnumMap<>(Side.class);
        if (placed.family != Family.WEDGE) {
            return changes;
        }
        boolean pos = placed.positive;
        if (has(placed, WEST)) {
            change(changes, n, NORTH, pos, NORTH, pick(pos, WEDGE_EXT_POS_NW, WEDGE_EXT_NEG_NW), SOUTH, pick(pos, WEDGE_INT_POS_SW, WEDGE_INT_NEG_SW));
            change(changes, n, SOUTH, pos, SOUTH, pick(pos, WEDGE_EXT_POS_SW, WEDGE_EXT_NEG_SW), NORTH, pick(pos, WEDGE_INT_POS_NW, WEDGE_INT_NEG_NW));
        }
        if (has(placed, EAST)) {
            change(changes, n, NORTH, pos, NORTH, pick(pos, WEDGE_EXT_POS_NE, WEDGE_EXT_NEG_NE), SOUTH, pick(pos, WEDGE_INT_POS_SE, WEDGE_INT_NEG_SE));
            change(changes, n, SOUTH, pos, SOUTH, pick(pos, WEDGE_EXT_POS_SE, WEDGE_EXT_NEG_SE), NORTH, pick(pos, WEDGE_INT_POS_NE, WEDGE_INT_NEG_NE));
        }
        if (has(placed, NORTH)) {
            change(changes, n, WEST, pos, WEST, pick(pos, WEDGE_EXT_POS_NW, WEDGE_EXT_NEG_NW), EAST, pick(pos, WEDGE_INT_POS_NE, WEDGE_INT_NEG_NE));
            change(changes, n, EAST, pos, EAST, pick(pos, WEDGE_EXT_POS_NE, WEDGE_EXT_NEG_NE), WEST, pick(pos, WEDGE_INT_POS_NW, WEDGE_INT_NEG_NW));
        }
        if (has(placed, SOUTH)) {
            change(changes, n, WEST, pos, WEST, pick(pos, WEDGE_EXT_POS_SW, WEDGE_EXT_NEG_SW), EAST, pick(pos, WEDGE_INT_POS_SE, WEDGE_INT_NEG_SE));
            change(changes, n, EAST, pos, EAST, pick(pos, WEDGE_EXT_POS_SE, WEDGE_EXT_NEG_SE), WEST, pick(pos, WEDGE_INT_POS_SW, WEDGE_INT_NEG_SW));
        }
        return changes;
    }

    /**
     * A neighbour that is a plain wedge of the same direction becomes {@code ifA} when it faces
     * {@code a}, or {@code ifB} when it faces {@code b}. As in 1.7.10, the second rule wins
     * when both apply.
     */
    private static void change(Map<Side, SlopeType> changes, Neighbors n, Side where, boolean positive,
                               Side a, SlopeType ifA, Side b, SlopeType ifB) {
        SlopeType neighbour = n.at(where);
        if (neighbour == null || neighbour.family != Family.WEDGE || neighbour.positive != positive) {
            return;
        }
        if (has(neighbour, a)) {
            changes.put(where, ifA);
        }
        if (has(neighbour, b)) {
            changes.put(where, ifB);
        }
    }

    /** Whether the neighbour on {@code side} gives an upward prism a ridge towards it. */
    private static boolean prismPoint(Neighbors n, Side side) {
        SlopeType t = n.at(side);
        if (t == null) {
            return false;
        }
        if (t.family.isPrism()) {
            // 1.7.10 also counted downward prisms, turning them upward; only upward ones join.
            return t.positive;
        }
        return t.family == Family.PRISM_WEDGE && has(t, side.opposite());
    }

    /** Ridges of an upward prism from its neighbours; unchanged if it has none or points down. */
    public static SlopeType transformPrism(SlopeType current, Neighbors n) {
        if (!current.family.isPrism() || !current.positive) {
            return current;
        }
        boolean w = prismPoint(n, WEST), e = prismPoint(n, EAST), no = prismPoint(n, NORTH), s = prismPoint(n, SOUTH);
        int count = (w ? 1 : 0) + (e ? 1 : 0) + (no ? 1 : 0) + (s ? 1 : 0);
        if (count == 0) {
            return current;
        }
        for (SlopeType t : SlopeType.values()) {
            if (t.family.isPrism() && t.positive && t.family != Family.PRISM
                && has(t, WEST) == w && has(t, EAST) == e && has(t, NORTH) == no && has(t, SOUTH) == s) {
                return t;
            }
        }
        return current;
    }

    /** Sides around a block where a placed prism may change its neighbours. */
    public static final Side[] HORIZONTAL = { NORTH, SOUTH, WEST, EAST };
}

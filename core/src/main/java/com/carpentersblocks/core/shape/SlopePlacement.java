package com.carpentersblocks.core.shape;

import com.carpentersblocks.core.Side;

/**
 * Which slope shape a placement creates, ported from 1.7.10 {@code BlockCarpentersSlope}.
 * Inputs: the face that was clicked, where on it (0..1 within the clicked block), and the
 * player's view.
 */
public final class SlopePlacement {
    /** The five slope items of 1.7.10 (block metadata 0-4). */
    public enum Kind { WEDGE, OBLIQUE_INTERIOR, OBLIQUE_EXTERIOR, PRISM, PRISM_WEDGE }

    private SlopePlacement() {
    }

    /**
     * @param clicked  face of the block that was clicked
     * @param hitX     click position inside the clicked block, 0..1
     * @param look     horizontal direction the player looks
     * @param yaw      player yaw in degrees (Minecraft convention: 0 = south)
     */
    public static SlopeType place(Kind kind, Side clicked, double hitX, double hitY, double hitZ, Side look, float yaw) {
        boolean positive = clicked == Side.UP || clicked.dy == 0 && hitY < 0.5;
        switch (kind) {
            case WEDGE:
                return wedge(look, clicked, hitX, hitY, hitZ);
            case OBLIQUE_INTERIOR:
                return corner(positive ? SlopeType.OBL_INT_POS_SE : SlopeType.OBL_INT_NEG_SE, yaw);
            case OBLIQUE_EXTERIOR:
                return corner(positive ? SlopeType.OBL_EXT_POS_SE : SlopeType.OBL_EXT_NEG_SE, yaw);
            case PRISM:
                return positive ? SlopeType.PRISM_POS : SlopeType.PRISM_NEG;
            case PRISM_WEDGE:
            default:
                switch (look) {
                    case NORTH: return SlopeType.PRISM_WEDGE_POS_S;
                    case SOUTH: return SlopeType.PRISM_WEDGE_POS_N;
                    case WEST:  return SlopeType.PRISM_WEDGE_POS_E;
                    default:    return SlopeType.PRISM_WEDGE_POS_W;
                }
        }
    }

    /** 1.7.10 {@code getWedgeOrientation}: side faces pick by hit position, top/bottom by view. */
    static SlopeType wedge(Side look, Side clicked, double hitX, double hitY, double hitZ) {
        int side = clicked.ordinal();
        switch (side) {
            case 2: hitX = 1 - hitX; break;
            case 4: hitX = hitZ; break;
            case 5: hitX = 1 - hitZ; break;
            default: break;
        }
        int id;
        if (side > 1) {
            if (hitY > 0.5 && hitX > 1 - hitY && hitX < hitY) {
                id = side + 2;
            } else if (hitY < 0.5 && hitX < 1 - hitY && hitX > hitY) {
                id = side + 6;
            } else if (hitX < 0.2) {
                id = side == 2 ? 1 : side == 3 ? 0 : side == 4 ? 3 : 2;
            } else if (hitX > 0.8) {
                id = side == 2 ? 2 : side == 3 ? 3 : side == 4 ? 1 : 0;
            } else if (hitY > 0.5) {
                id = side + 2;
            } else {
                id = side + 6;
            }
            return SlopeType.byId(id);
        }
        boolean bottom = side == 0;
        switch (look) {
            case NORTH: return bottom ? SlopeType.WEDGE_NEG_S : SlopeType.WEDGE_POS_S;
            case SOUTH: return bottom ? SlopeType.WEDGE_NEG_N : SlopeType.WEDGE_POS_N;
            case WEST:  return bottom ? SlopeType.WEDGE_NEG_E : SlopeType.WEDGE_POS_E;
            default:    return bottom ? SlopeType.WEDGE_NEG_W : SlopeType.WEDGE_POS_W;
        }
    }

    /**
     * Oblique corners face the corner the player looks towards. {@code se} is the south-east
     * variant of the wanted kind; ids of a group of four are ordered SE, NW, NE, SW.
     */
    private static SlopeType corner(SlopeType se, float yaw) {
        int quadrant = ((int) Math.floor(yaw * 4.0F / 360.0F) & 3) % 4;
        int offset;
        switch (quadrant) {
            case 0:  offset = 2; break; // NE
            case 1:  offset = 0; break; // SE
            case 2:  offset = 3; break; // SW
            default: offset = 1; break; // NW
        }
        return SlopeType.byId(se.ordinal() + offset);
    }
}

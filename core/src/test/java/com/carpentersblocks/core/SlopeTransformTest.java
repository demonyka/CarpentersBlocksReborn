package com.carpentersblocks.core;

import com.carpentersblocks.core.shape.SlopeTransform;
import com.carpentersblocks.core.shape.SlopeType;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static com.carpentersblocks.core.shape.SlopeType.*;
import static org.junit.jupiter.api.Assertions.*;

class SlopeTransformTest {
    private static SlopeTransform.Neighbors around(Object... sideAndType) {
        Map<Side, SlopeType> map = new EnumMap<>(Side.class);
        for (int i = 0; i < sideAndType.length; i += 2) {
            map.put((Side) sideAndType[i], (SlopeType) sideAndType[i + 1]);
        }
        return map::get;
    }

    /** 1.7.10 face bias XYNN/XYNP/XYPN/XYPP = corner 0/2/1/3 on the up and down faces. */
    @Test
    void wedgeCornersMatch1710FaceBias() {
        Object[][] table = {
            { WEDGE_SE, 0 }, { WEDGE_NW, 3 }, { WEDGE_NE, 2 }, { WEDGE_SW, 1 },
            { OBL_INT_NEG_NW, 3 }, { OBL_INT_NEG_NE, 2 }, { OBL_INT_NEG_SW, 1 }, { OBL_INT_NEG_SE, 0 },
            { OBL_EXT_POS_NW, 3 }, { OBL_EXT_POS_NE, 2 }, { OBL_EXT_POS_SW, 1 }, { OBL_EXT_POS_SE, 0 },
        };
        for (Object[] row : table) {
            assertEquals(row[1], ((SlopeType) row[0]).wedgeCorner(Side.DOWN), row[0] + " down");
        }
        assertEquals(3, OBL_INT_POS_NW.wedgeCorner(Side.UP));
        assertEquals(0, OBL_EXT_NEG_SE.wedgeCorner(Side.UP));
        assertEquals(-1, WEDGE_POS_N.wedgeCorner(Side.DOWN));
    }

    @Test
    void wedgeUnderSideWedgeBecomesSideWedge() {
        assertEquals(WEDGE_NE, SlopeTransform.transformWedge(WEDGE_POS_N, around(Side.UP, WEDGE_NE)));
        assertEquals(WEDGE_SW, SlopeTransform.transformWedge(WEDGE_POS_N, around(Side.DOWN, OBL_INT_POS_SW)));
    }

    @Test
    void wedgeJoinsNeighboursIntoCorners() {
        assertEquals(WEDGE_INT_POS_NE, SlopeTransform.transformWedge(WEDGE_POS_N, around(Side.NORTH, WEDGE_POS_E)));
        assertEquals(WEDGE_EXT_NEG_SE, SlopeTransform.transformWedge(WEDGE_NEG_S, around(Side.NORTH, WEDGE_NEG_E)));
        // Two neighbours forming an inner corner.
        assertEquals(WEDGE_INT_POS_NW, SlopeTransform.transformWedge(WEDGE_POS_N, around(Side.NORTH, WEDGE_POS_W, Side.WEST, WEDGE_POS_N)));
        // Alone or sneaking (not called): unchanged.
        assertEquals(WEDGE_POS_N, SlopeTransform.transformWedge(WEDGE_POS_N, around()));
        // Non-wedges are never turned into corners.
        assertEquals(PRISM_POS, SlopeTransform.transformWedge(PRISM_POS, around(Side.NORTH, WEDGE_POS_E)));
    }

    @Test
    void placedWedgeTurnsNeighbouringWedgesIntoCorners() {
        Map<Side, SlopeType> changes = SlopeTransform.adjacentWedges(WEDGE_POS_W, around(Side.NORTH, WEDGE_POS_N, Side.SOUTH, WEDGE_NEG_N));
        assertEquals(WEDGE_EXT_POS_NW, changes.get(Side.NORTH));
        assertFalse(changes.containsKey(Side.SOUTH), "a downward wedge does not join an upward one");
    }

    @Test
    void prismsGrowRidgesTowardsPrisms() {
        assertEquals(PRISM_2P_POS_WE, SlopeTransform.transformPrism(PRISM_POS, around(Side.WEST, PRISM_POS, Side.EAST, PRISM_1P_POS_W)));
        assertEquals(PRISM_1P_POS_N, SlopeTransform.transformPrism(PRISM_POS, around(Side.NORTH, PRISM_WEDGE_POS_S)));
        assertEquals(PRISM_4P_POS, SlopeTransform.transformPrism(PRISM_POS,
            around(Side.WEST, PRISM_POS, Side.EAST, PRISM_POS, Side.NORTH, PRISM_POS, Side.SOUTH, PRISM_POS)));
        // A prism wedge that does not point at us, or a downward prism, adds no ridge.
        assertEquals(PRISM_POS, SlopeTransform.transformPrism(PRISM_POS, around(Side.NORTH, PRISM_WEDGE_POS_N, Side.WEST, PRISM_NEG)));
        // Downward prisms never change.
        assertEquals(PRISM_NEG, SlopeTransform.transformPrism(PRISM_NEG, around(Side.WEST, PRISM_POS)));
    }
}

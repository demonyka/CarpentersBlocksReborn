package com.carpentersblocks.block;

import com.carpentersblocks.core.shape.SlopeType;
import net.minecraft.util.StringRepresentable;

/**
 * {@link SlopeType} as a block state value. Same order as the core enum (and as 1.7.10 slope ids);
 * checked at class load so the two can never drift apart.
 */
public enum SlopeTypeProperty implements StringRepresentable {
    WEDGE_SE, WEDGE_NW, WEDGE_NE, WEDGE_SW, WEDGE_NEG_N, WEDGE_NEG_S, WEDGE_NEG_W, WEDGE_NEG_E,
    WEDGE_POS_N, WEDGE_POS_S, WEDGE_POS_W, WEDGE_POS_E, WEDGE_INT_NEG_SE, WEDGE_INT_NEG_NW,
    WEDGE_INT_NEG_NE, WEDGE_INT_NEG_SW, WEDGE_INT_POS_SE, WEDGE_INT_POS_NW, WEDGE_INT_POS_NE,
    WEDGE_INT_POS_SW, WEDGE_EXT_NEG_SE, WEDGE_EXT_NEG_NW, WEDGE_EXT_NEG_NE, WEDGE_EXT_NEG_SW,
    WEDGE_EXT_POS_SE, WEDGE_EXT_POS_NW, WEDGE_EXT_POS_NE, WEDGE_EXT_POS_SW, OBL_INT_NEG_SE,
    OBL_INT_NEG_NW, OBL_INT_NEG_NE, OBL_INT_NEG_SW, OBL_INT_POS_SE, OBL_INT_POS_NW, OBL_INT_POS_NE,
    OBL_INT_POS_SW, OBL_EXT_NEG_SE, OBL_EXT_NEG_NW, OBL_EXT_NEG_NE, OBL_EXT_NEG_SW, OBL_EXT_POS_SE,
    OBL_EXT_POS_NW, OBL_EXT_POS_NE, OBL_EXT_POS_SW, PRISM_NEG, PRISM_POS, PRISM_1P_POS_N,
    PRISM_1P_POS_S, PRISM_1P_POS_W, PRISM_1P_POS_E, PRISM_2P_POS_NS, PRISM_2P_POS_WE,
    PRISM_2P_POS_SE, PRISM_2P_POS_NW, PRISM_2P_POS_NE, PRISM_2P_POS_SW, PRISM_3P_POS_NWE,
    PRISM_3P_POS_SWE, PRISM_3P_POS_NSW, PRISM_3P_POS_NSE, PRISM_4P_POS, PRISM_WEDGE_POS_N,
    PRISM_WEDGE_POS_S, PRISM_WEDGE_POS_W, PRISM_WEDGE_POS_E;

    private static final SlopeTypeProperty[] VALUES = values();

    static {
        SlopeType[] core = SlopeType.values();
        if (core.length != VALUES.length) {
            throw new IllegalStateException("SlopeTypeProperty out of sync with SlopeType");
        }
        for (int i = 0; i < core.length; i++) {
            if (!core[i].name().equals(VALUES[i].name())) {
                throw new IllegalStateException("SlopeTypeProperty out of sync at " + core[i]);
            }
        }
    }

    public SlopeType core() {
        return SlopeType.byId(ordinal());
    }

    public static SlopeTypeProperty of(SlopeType type) {
        return VALUES[type.ordinal()];
    }

    @Override
    public String getSerializedName() {
        return core().id();
    }
}

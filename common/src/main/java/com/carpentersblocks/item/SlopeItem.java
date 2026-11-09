package com.carpentersblocks.item;

import com.carpentersblocks.core.shape.SlopePlacement;
import com.carpentersblocks.core.shape.SlopeType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;

import java.util.Locale;

/**
 * One of the five slope items of 1.7.10. They all place the same block; the kind decides how
 * the placed shape is chosen ({@link SlopePlacement}).
 */
public class SlopeItem extends BlockItem {
    public final SlopePlacement.Kind kind;

    public SlopeItem(Block block, SlopePlacement.Kind kind, Properties properties) {
        super(block, properties);
        this.kind = kind;
    }

    /** Registry path of the item for a kind, e.g. {@code slope_oblique_interior}. */
    public static String idOf(SlopePlacement.Kind kind) {
        return "slope_" + kind.name().toLowerCase(Locale.ROOT);
    }

    /** Which item a broken slope drops: the kind whose placement produces that shape family. */
    public static SlopePlacement.Kind kindOf(SlopeType type) {
        switch (type.family) {
            case OBLIQUE_INT: return SlopePlacement.Kind.OBLIQUE_INTERIOR;
            case OBLIQUE_EXT: return SlopePlacement.Kind.OBLIQUE_EXTERIOR;
            case PRISM_WEDGE: return SlopePlacement.Kind.PRISM_WEDGE;
            default: return type.family.isPrism() ? SlopePlacement.Kind.PRISM : SlopePlacement.Kind.WEDGE;
        }
    }

    /** Shape shown for the item in inventories. */
    public static SlopeType displayType(SlopePlacement.Kind kind) {
        switch (kind) {
            case OBLIQUE_INTERIOR: return SlopeType.OBL_INT_POS_NW;
            case OBLIQUE_EXTERIOR: return SlopeType.OBL_EXT_POS_SE;
            case PRISM:            return SlopeType.PRISM_2P_POS_WE;
            case PRISM_WEDGE:      return SlopeType.PRISM_WEDGE_POS_N;
            default:               return SlopeType.WEDGE_POS_N;
        }
    }

    @Override
    public String getDescriptionId() {
        return "item.carpentersblocks." + idOf(kind);
    }
}

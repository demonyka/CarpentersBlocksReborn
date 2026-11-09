package com.carpentersblocks.core.interaction;

import com.carpentersblocks.core.Side;
import com.carpentersblocks.core.attribute.AttributeKey;
import com.carpentersblocks.core.attribute.AttributeLocation;
import com.carpentersblocks.core.attribute.AttributeType;
import com.carpentersblocks.core.attribute.AttributeView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/**
 * Decides which attribute slot a click adds to or removes from. Pure logic, ported
 * from 1.7.10 {@code BlockCoverable#onBlockActivated} and {@code #popAttribute}.
 */
public final class AttributeRules {
    private AttributeRules() {
    }

    /**
     * The location a click on {@code clicked} affects: that side if it already has a side
     * cover, otherwise the host.
     */
    public static AttributeLocation effectiveLocation(AttributeView view, Side clicked) {
        AttributeLocation side = AttributeLocation.of(clicked);
        return view.has(AttributeType.COVER, side) ? side : AttributeLocation.HOST;
    }

    /**
     * Slot that right-clicking with an item should fill, or {@code null} if nothing applies.
     *
     * @param canCover whether the block allows a cover at a location (host or a specific side)
     */
    public static AttributeKey addTarget(HeldItemKind kind, boolean sneaking, Side clicked,
                                         AttributeView view, Predicate<AttributeLocation> canCover) {
        AttributeLocation location = effectiveLocation(view, clicked);
        switch (kind) {
            case COVER: {
                // The host is tried first; once it is covered (or cannot be), the clicked side gets a side cover.
                if (location.isHost() && (!canCover.test(location) || view.has(AttributeType.COVER, location))) {
                    location = AttributeLocation.of(clicked);
                }
                if (canCover.test(location) && !view.has(AttributeType.COVER, location)) {
                    return AttributeKey.of(AttributeType.COVER, location);
                }
                return null;
            }
            case ILLUMINATOR:
                return sneaking && !view.has(AttributeType.ILLUMINATOR, AttributeLocation.HOST)
                    ? AttributeKey.of(AttributeType.ILLUMINATOR, AttributeLocation.HOST) : null;
            case OVERLAY:
                return sneaking && !view.has(AttributeType.OVERLAY, location)
                    ? AttributeKey.of(AttributeType.OVERLAY, location) : null;
            case DYE:
                return sneaking && !view.has(AttributeType.DYE, location)
                    ? AttributeKey.of(AttributeType.DYE, location) : null;
            default:
                return null;
        }
    }

    /**
     * Slots removed by sneak + left-click with the hammer, in 1.7.10 priority order:
     * illuminator, overlay, dye, then cover (a cover takes its chisel design with it).
     *
     * @return slots to clear; empty if there is nothing to remove
     */
    public static List<AttributeKey> popTargets(AttributeView view, Side clicked) {
        AttributeLocation location = effectiveLocation(view, clicked);
        AttributeKey illuminator = AttributeKey.of(AttributeType.ILLUMINATOR, AttributeLocation.HOST);
        if (view.has(illuminator)) {
            return Collections.singletonList(illuminator);
        }
        for (AttributeType type : new AttributeType[] { AttributeType.OVERLAY, AttributeType.DYE }) {
            AttributeKey key = AttributeKey.of(type, location);
            if (view.has(key)) {
                return Collections.singletonList(key);
            }
        }
        AttributeKey cover = AttributeKey.of(AttributeType.COVER, location);
        if (view.has(cover)) {
            List<AttributeKey> keys = new ArrayList<>(2);
            keys.add(cover);
            AttributeKey design = AttributeKey.of(AttributeType.DESIGN, location);
            if (view.has(design)) {
                keys.add(design);
            }
            return keys;
        }
        return Collections.emptyList();
    }
}
